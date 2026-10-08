package com.cpvphelper.feature;

import com.cpvphelper.config.CPvPConfig;
import com.cpvphelper.input.Binds;
import com.cpvphelper.input.FeatureBind;
import com.cpvphelper.mixin.MinecraftAccessor;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Auto cart: while an arrow of the player flies, its landing block is predicted, and a rail and a TNT minecart are
 * placed there, then the cart is lit with flint and steel. A bind draws the hotbar bow, shoots and selects the
 * previous slot again.
 *
 * <p>Every step is a regular vanilla right click with the item picked from the hotbar, within the vanilla block reach.
 */
public final class AutoCart {
	private enum Step {
		RAIL,
		CART,
		IGNITE
	}

	private static final IntSet processedArrows = new IntOpenHashSet();

	private static Step step;
	private static BlockPos railPos;
	private static int previousSlot = -1;
	private static int waitTicks;
	private static int stalledTicks;
	private static boolean wasActive;

	private static boolean bowKeyWasDown;
	private static boolean bowCharging;
	private static int bowWaitTicks;
	private static int bowPreviousSlot = -1;

	private AutoCart() {
	}

	public static boolean isActive() {
		return step != null || bowCharging;
	}

	/** Start of handleKeybinds, after AutoTotem and AnchorPlace. */
	public static void onKeybinds(Minecraft mc, boolean totemSwappedThisTick) {
		LocalPlayer player = mc.player;
		boolean active = FeatureBind.AUTO_CART.isActive();
		if (player == null || mc.level == null || mc.gameMode == null || !active) {
			stop(mc);
			wasActive = false;
			return;
		}

		if (!wasActive)
			skipExistingArrows(mc.level, player);

		wasActive = true;
		CPvPConfig config = CPvPConfig.get();
		handleBow(mc, player, config);
		if (bowCharging || totemSwappedThisTick || AnchorPlace.isActive())
			return;

		trackArrows(mc, player, config);
		runSteps(mc, player, config);
	}

	private static void handleBow(Minecraft mc, LocalPlayer player, CPvPConfig config) {
		boolean down = Binds.isDown(mc, config.autoCartBowBind);
		boolean pressed = down && !bowKeyWasDown;
		bowKeyWasDown = down;
		if (bowCharging) {
			chargeBow(mc, player, config);
			return;
		}

		if (pressed && step == null)
			drawBow(mc, player);
	}

	private static void drawBow(Minecraft mc, LocalPlayer player) {
		Inventory inventory = player.getInventory();
		int bow = findHotbar(inventory, Items.BOW);
		if (bow == -1 || player.isUsingItem() || player.isHandsBusy())
			return;

		bowPreviousSlot = inventory.getSelectedSlot();
		inventory.setSelectedSlot(bow);
		mc.options.keyUse.setDown(true);
		bowCharging = true;
		bowWaitTicks = 0;
	}

	private static void chargeBow(Minecraft mc, LocalPlayer player, CPvPConfig config) {
		if (!player.getMainHandItem().is(Items.BOW)) {
			finishBow(mc, player, false);
			return;
		}

		if (!player.isUsingItem()) {
			if (++bowWaitTicks > 10)
				finishBow(mc, player, false);

			return;
		}

		if (player.getTicksUsingItem() >= config.autoCartBowCharge)
			finishBow(mc, player, true);
	}

	private static void finishBow(Minecraft mc, LocalPlayer player, boolean shoot) {
		mc.options.keyUse.setDown(false);
		if (shoot && player.isUsingItem())
			mc.gameMode.releaseUsingItem(player);

		if (bowPreviousSlot != -1)
			player.getInventory().setSelectedSlot(bowPreviousSlot);

		bowCharging = false;
		bowPreviousSlot = -1;
	}

	private static void trackArrows(Minecraft mc, LocalPlayer player, CPvPConfig config) {
		ClientLevel level = mc.level;
		IntIterator processed = processedArrows.iterator();
		while (processed.hasNext()) {
			if (level.getEntity(processed.nextInt()) == null)
				processed.remove();
		}

		if (step != null)
			return;

		for (Entity entity : level.entitiesForRendering()) {
			if (!isOwnArrow(entity, player) || processedArrows.contains(entity.getId()) || !isFlying(entity))
				continue;

			if (tryStart(level, player, config, entity))
				return;
		}
	}

	private static boolean tryStart(ClientLevel level, LocalPlayer player, CPvPConfig config, Entity arrow) {
		Inventory inventory = player.getInventory();
		if (findHotbar(inventory, railItem(config)) == -1 || findHotbar(inventory, Items.TNT_MINECART) == -1)
			return false;

		BlockPos landing = predictLanding(level, arrow);
		if (landing == null || !isReachable(player, landing, config))
			return false;

		processedArrows.add(arrow.getId());
		step = Step.RAIL;
		railPos = landing;
		previousSlot = inventory.getSelectedSlot();
		waitTicks = 0;
		stalledTicks = 0;
		return true;
	}

	private static void runSteps(Minecraft mc, LocalPlayer player, CPvPConfig config) {
		if (step == null || (waitTicks > 0 && --waitTicks > 0))
			return;

		while (step != null && waitTicks == 0) {
			if (runStep(mc, player, config)) {
				stalledTicks = 0;
				continue;
			}

			if (step != null && ++stalledTicks > 20)
				finish(player);

			return;
		}
	}

	private static boolean runStep(Minecraft mc, LocalPlayer player, CPvPConfig config) {
		if (player.isUsingItem() || player.isHandsBusy() || mc.gameMode.isDestroying() || !isReachable(player, railPos, config))
			return false;

		Inventory inventory = player.getInventory();
		return switch (step) {
			case RAIL -> placeRail(mc, inventory, config);
			case CART -> placeCart(mc, inventory, config);
			case IGNITE -> ignite(mc, player, inventory);
		};
	}

	private static boolean placeRail(Minecraft mc, Inventory inventory, CPvPConfig config) {
		ClientLevel level = mc.level;
		if (isRail(level, railPos))
			return advance(Step.CART, config);

		int rail = findHotbar(inventory, railItem(config));
		if (rail == -1 || !canPlaceRail(level, railPos))
			return finish(mc.player);

		BlockPos support = railPos.below();
		inventory.setSelectedSlot(rail);
		use(mc, new BlockHitResult(new Vec3(railPos.getX() + 0.5, railPos.getY(), railPos.getZ() + 0.5), Direction.UP, support, false));
		if (!isRail(level, railPos))
			return finish(mc.player);

		return advance(Step.CART, config);
	}

	private static boolean placeCart(Minecraft mc, Inventory inventory, CPvPConfig config) {
		if (findCart(mc.level) == null) {
			int cart = findHotbar(inventory, Items.TNT_MINECART);
			if (cart == -1)
				return finish(mc.player);

			inventory.setSelectedSlot(cart);
			use(mc, new BlockHitResult(new Vec3(railPos.getX() + 0.5, railPos.getY() + 0.15, railPos.getZ() + 0.5), Direction.UP, railPos, false));
			if (config.autoCartRestock)
				restock(mc, inventory, cart);
		}

		if (!config.autoCartIgnite)
			return finish(mc.player);

		return advance(Step.IGNITE, config);
	}

	private static boolean ignite(Minecraft mc, LocalPlayer player, Inventory inventory) {
		Entity cart = findCart(mc.level);
		if (cart == null)
			return false;

		Vec3 center = cart.getBoundingBox().getCenter();
		double range = player.entityInteractionRange();
		if (player.getEyePosition().distanceToSqr(center) > range * range)
			return false;

		int lighter = findHotbar(inventory, Items.FLINT_AND_STEEL);
		if (lighter != -1) {
			inventory.setSelectedSlot(lighter);
			use(mc, new EntityHitResult(cart, center));
		}

		return finish(player);
	}

	private static void restock(Minecraft mc, Inventory inventory, int hotbarSlot) {
		LocalPlayer player = mc.player;
		if (inventory.getItem(hotbarSlot).is(Items.TNT_MINECART) || player.containerMenu != player.inventoryMenu)
			return;

		for (int slot = InventoryMenu.INV_SLOT_START; slot < InventoryMenu.INV_SLOT_END; slot++) {
			if (!player.inventoryMenu.slots.get(slot).getItem().is(Items.TNT_MINECART))
				continue;

			mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, slot, hotbarSlot, ContainerInput.SWAP, player);
			return;
		}
	}

	private static void use(Minecraft mc, HitResult hit) {
		HitResult crosshair = mc.hitResult;
		mc.hitResult = hit;
		((MinecraftAccessor) mc).cpvphelper$startUseItem();
		mc.hitResult = crosshair;
	}

	private static BlockPos predictLanding(ClientLevel level, Entity arrow) {
		Vec3 position = arrow.position();
		Vec3 motion = arrow.getDeltaMovement();
		for (int tick = 0; tick < 80; tick++) {
			Vec3 next = position.add(motion);
			BlockHitResult hit = level.clip(new ClipContext(position, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, arrow));
			if (hit.getType() != HitResult.Type.MISS)
				return resolvePlacePos(level, hit);

			boolean inWater = level.getFluidState(BlockPos.containing(position)).is(FluidTags.WATER);
			motion = motion.scale(inWater ? 0.6 : 0.99).add(0.0, -0.05, 0.0);
			position = next;
		}

		return null;
	}

	private static BlockPos resolvePlacePos(ClientLevel level, BlockHitResult hit) {
		BlockPos base = hit.getBlockPos();
		if (isRail(level, base))
			return base;

		BlockPos preferred = hit.getDirection() == Direction.UP ? base.above() : base;
		if (canPlaceRail(level, preferred))
			return preferred;

		if (canPlaceRail(level, base.above()))
			return base.above();

		return canPlaceRail(level, base.below()) ? base.below() : null;
	}

	private static boolean canPlaceRail(ClientLevel level, BlockPos pos) {
		if (isRail(level, pos))
			return true;

		if (!level.getBlockState(pos).canBeReplaced())
			return false;

		BlockPos support = pos.below();
		return level.getBlockState(support).isFaceSturdy(level, support, Direction.UP);
	}

	private static boolean isRail(ClientLevel level, BlockPos pos) {
		return level.getBlockState(pos).is(BlockTags.RAILS);
	}

	private static Entity findCart(ClientLevel level) {
		List<Entity> carts = level.getEntities((Entity) null, new AABB(railPos).inflate(0.2, 1.0, 0.2), entity -> entity.getType() == EntityType.TNT_MINECART);
		return carts.isEmpty() ? null : carts.getFirst();
	}

	private static boolean isReachable(LocalPlayer player, BlockPos pos, CPvPConfig config) {
		double range = Math.min(player.blockInteractionRange(), config.autoCartRange / 100.0);
		return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) <= range * range;
	}

	private static boolean isOwnArrow(Entity entity, LocalPlayer player) {
		EntityType<?> type = entity.getType();
		if (type != EntityType.ARROW && type != EntityType.SPECTRAL_ARROW)
			return false;

		return entity instanceof TraceableEntity traceable && traceable.getOwner() == player;
	}

	private static boolean isFlying(Entity arrow) {
		return !arrow.onGround() && arrow.getDeltaMovement().lengthSqr() > 1.0E-4;
	}

	private static void skipExistingArrows(ClientLevel level, LocalPlayer player) {
		for (Entity entity : level.entitiesForRendering()) {
			if (isOwnArrow(entity, player))
				processedArrows.add(entity.getId());
		}
	}

	private static Item railItem(CPvPConfig config) {
		return config.autoCartPoweredRail ? Items.POWERED_RAIL : Items.RAIL;
	}

	private static int findHotbar(Inventory inventory, Item item) {
		for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
			if (inventory.getItem(slot).is(item))
				return slot;
		}

		return -1;
	}

	private static boolean advance(Step next, CPvPConfig config) {
		step = next;
		waitTicks = config.autoCartDelay;
		return true;
	}

	private static boolean finish(LocalPlayer player) {
		if (previousSlot != -1)
			player.getInventory().setSelectedSlot(previousSlot);

		step = null;
		railPos = null;
		previousSlot = -1;
		waitTicks = 0;
		stalledTicks = 0;
		return false;
	}

	private static void stop(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (bowCharging) {
			mc.options.keyUse.setDown(false);
			if (player != null && bowPreviousSlot != -1)
				player.getInventory().setSelectedSlot(bowPreviousSlot);

			bowCharging = false;
			bowPreviousSlot = -1;
		}

		if (step != null && player != null)
			finish(player);

		step = null;
		railPos = null;
		previousSlot = -1;
		bowKeyWasDown = false;
	}
}
