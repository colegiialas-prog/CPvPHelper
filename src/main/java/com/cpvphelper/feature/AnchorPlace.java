package com.cpvphelper.feature;

import com.cpvphelper.config.CPvPConfig;
import com.cpvphelper.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Right click with a sword on a block: place a respawn anchor, charge it with glowstone, switch back to the
 * sword and detonate it.
 *
 * <p>Every step is a regular vanilla right click on what the crosshair is pointing at, with the item picked
 * from the hotbar. Nothing is clicked that the player is not looking at and nothing is placed against air.
 */
public final class AnchorPlace {
	private enum Step {
		IDLE,
		CHARGE,
		EXPLODE
	}

	/** Ticks a step may be blocked (crosshair off the anchor, mining) before the sequence is dropped. */
	private static final int MAX_BLOCKED_TICKS = 4;
	/** handleKeybinds does not run while a screen is open; drop the sequence after such a pause. */
	private static final int MAX_TICK_GAP = 10;

	private static Step step = Step.IDLE;
	private static BlockPos anchorPos;
	private static int swordSlot = -1;
	private static int glowstoneSlot = -1;
	private static int waitTicks;
	private static int blockedTicks;
	private static long lastTick;

	private AnchorPlace() {
	}

	public static boolean isActive() {
		return step != Step.IDLE;
	}

	/**
	 * A fresh right click, called in place of the first startUseItem in handleKeybinds.
	 *
	 * @return true if the click was taken over and vanilla must not use the item
	 */
	public static boolean onUseClick(Minecraft mc) {
		if (isActive()) {
			return true;
		}

		CPvPConfig config = CPvPConfig.get();
		LocalPlayer player = mc.player;
		ClientLevel level = mc.level;
		if (!config.anchorEnabled || player == null || level == null || mc.gameMode == null) {
			return false;
		}

		if (!player.getMainHandItem().is(ItemTags.SWORDS)
			|| player.isSpectator()
			|| player.isSecondaryUseActive()
			|| player.isHandsBusy()
			|| mc.gameMode.isDestroying()
			|| config.anchorOnlyOnGround && !player.onGround()) {
			return false;
		}

		// Only a real block under the crosshair: never place against air.
		if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK || !anchorsExplode(level, hit.getBlockPos())) {
			return false;
		}

		Inventory inventory = player.getInventory();
		int glowstone = findHotbar(inventory, Items.GLOWSTONE);
		if (glowstone == -1) {
			return false;
		}

		int sword = inventory.getSelectedSlot();
		BlockState clicked = level.getBlockState(hit.getBlockPos());
		if (clicked.is(Blocks.RESPAWN_ANCHOR)) {
			if (clicked.getValue(RespawnAnchorBlock.CHARGE) > 0) {
				// The vanilla sword click already detonates it.
				return false;
			}

			// An empty anchor under the crosshair: skip placing and charge it right away.
			start(sword, glowstone, hit.getBlockPos(), 0);
			runReadySteps(mc, player, level);
			return true;
		}

		int anchor = findHotbar(inventory, Items.RESPAWN_ANCHOR);
		if (anchor == -1) {
			return false;
		}

		inventory.setSelectedSlot(anchor);
		useItemOn(mc, hit);
		BlockPos placed = findPlacedAnchor(level, hit);
		if (placed == null) {
			inventory.setSelectedSlot(sword);
			return true;
		}

		start(sword, glowstone, placed, config.anchorChargeDelay);
		runReadySteps(mc, player, level);
		return true;
	}

	/** Start of handleKeybinds: advances a running sequence once per tick. */
	public static void onKeybinds(Minecraft mc, boolean totemSwappedThisTick) {
		if (!isActive()) {
			return;
		}

		LocalPlayer player = mc.player;
		ClientLevel level = mc.level;
		if (player == null || level == null || mc.gameMode == null || !CPvPConfig.get().anchorEnabled) {
			finish();
			return;
		}

		long now = ClientTicks.get();
		boolean stalled = now - lastTick > MAX_TICK_GAP;
		lastTick = now;
		if (stalled) {
			abort(player);
			return;
		}

		if (waitTicks > 0) {
			waitTicks--;
		}

		// Never stack the anchor step on top of a totem swap in the same tick.
		if (waitTicks == 0 && !totemSwappedThisTick) {
			runReadySteps(mc, player, level);
		}
	}

	private static void runReadySteps(Minecraft mc, LocalPlayer player, ClientLevel level) {
		while (isActive() && waitTicks == 0) {
			if (!runStep(mc, player, level)) {
				return;
			}
		}
	}

	private static boolean runStep(Minecraft mc, LocalPlayer player, ClientLevel level) {
		BlockState state = level.getBlockState(anchorPos);
		if (!state.is(Blocks.RESPAWN_ANCHOR)) {
			return abort(player);
		}

		BlockHitResult hit = aimedAnchor(player);
		if (hit == null || mc.gameMode.isDestroying() || player.isUsingItem() || player.isHandsBusy()) {
			// Only click what the crosshair is on; give the aim a few ticks to come back.
			if (++blockedTicks > MAX_BLOCKED_TICKS) {
				return abort(player);
			}

			waitTicks = 1;
			return false;
		}

		blockedTicks = 0;
		Inventory inventory = player.getInventory();
		CPvPConfig config = CPvPConfig.get();
		if (step == Step.CHARGE) {
			if (!inventory.getItem(glowstoneSlot).is(Items.GLOWSTONE)) {
				glowstoneSlot = findHotbar(inventory, Items.GLOWSTONE);
				if (glowstoneSlot == -1) {
					return abort(player);
				}
			}

			inventory.setSelectedSlot(glowstoneSlot);
			useItemOn(mc, hit);
			BlockState charged = level.getBlockState(anchorPos);
			if (!charged.is(Blocks.RESPAWN_ANCHOR) || charged.getValue(RespawnAnchorBlock.CHARGE) == 0) {
				return abort(player);
			}

			if (!config.anchorExplode) {
				inventory.setSelectedSlot(swordSlot);
				finish();
				return true;
			}

			step = Step.EXPLODE;
			waitTicks = config.anchorExplodeDelay;
			return true;
		}

		if (state.getValue(RespawnAnchorBlock.CHARGE) == 0 || inventory.getItem(swordSlot).is(Items.GLOWSTONE)) {
			return abort(player);
		}

		// Back on the sword: a click with anything but glowstone detonates a charged anchor.
		inventory.setSelectedSlot(swordSlot);
		useItemOn(mc, hit);
		finish();
		return true;
	}

	private static BlockHitResult aimedAnchor(LocalPlayer player) {
		// The same ray the crosshair uses, blocks only.
		HitResult result = player.pick(player.blockInteractionRange(), 1.0F, false);
		return result instanceof BlockHitResult hit && result.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(anchorPos) ? hit : null;
	}

	private static void useItemOn(Minecraft mc, BlockHitResult hit) {
		HitResult crosshair = mc.hitResult;
		mc.hitResult = hit;
		// The regular right click, so every packet matches a vanilla click.
		((MinecraftAccessor) mc).cpvphelper$startUseItem();
		mc.hitResult = crosshair;
	}

	private static BlockPos findPlacedAnchor(ClientLevel level, BlockHitResult hit) {
		BlockPos clicked = hit.getBlockPos();
		if (level.getBlockState(clicked).is(Blocks.RESPAWN_ANCHOR)) {
			// Placed into a replaceable block such as grass.
			return clicked;
		}

		BlockPos adjacent = clicked.relative(hit.getDirection());
		return level.getBlockState(adjacent).is(Blocks.RESPAWN_ANCHOR) ? adjacent : null;
	}

	private static boolean anchorsExplode(ClientLevel level, BlockPos pos) {
		return !level.environmentAttributes().getValue(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, Vec3.atCenterOf(pos), null);
	}

	private static int findHotbar(Inventory inventory, Item item) {
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			if (inventory.getItem(i).is(item)) {
				return i;
			}
		}

		return -1;
	}

	private static void start(int sword, int glowstone, BlockPos pos, int delay) {
		step = Step.CHARGE;
		swordSlot = sword;
		glowstoneSlot = glowstone;
		anchorPos = pos;
		waitTicks = delay;
		blockedTicks = 0;
		lastTick = ClientTicks.get();
	}

	private static boolean abort(LocalPlayer player) {
		if (swordSlot != -1) {
			player.getInventory().setSelectedSlot(swordSlot);
		}

		finish();
		return false;
	}

	private static void finish() {
		step = Step.IDLE;
		anchorPos = null;
		swordSlot = -1;
		glowstoneSlot = -1;
		waitTicks = 0;
		blockedTicks = 0;
	}
}
