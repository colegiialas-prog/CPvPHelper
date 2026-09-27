package com.cpvphelper.feature;

import com.cpvphelper.config.CPvPConfig;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Stun slam: a regular axe hit on an entity is followed by a mace hit on the same target, then the axe is
 * selected again. The axe knocks the shield out, the mace smash lands right after it.
 *
 * <p>The mace hit is what a player does by hand: select the mace slot, left click the target under the
 * crosshair, select the axe slot again. The axe slot is synced by the next carried item check, like after
 * pressing a hotbar key.
 */
public final class StunSlam {
	/** handleKeybinds does not run while a screen is open; drop a delayed hit that is this late. */
	private static final int MAX_LATE_TICKS = 2;

	private static Entity target;
	private static int axeSlot = -1;
	private static long fireTick;
	private static long lastSlamTick = -1;

	private StunSlam() {
	}

	public static boolean isPending() {
		return target != null;
	}

	/**
	 * Called right before vanilla sends the attack of a left click on an entity.
	 *
	 * @return true if the hit is an axe hit that has to be followed by a mace hit
	 */
	public static boolean shouldSlam(Minecraft mc, Entity entity) {
		CPvPConfig config = CPvPConfig.get();
		LocalPlayer player = mc.player;
		if (!config.stunSlamEnabled || player == null || mc.gameMode == null || isPending() || lastSlamTick == ClientTicks.get()) {
			return false;
		}

		if (player.isSpectator() || !player.getMainHandItem().is(ItemTags.AXES) || !(entity instanceof LivingEntity living) || !living.isAlive()) {
			return false;
		}

		if (config.stunSlamOnlyShield && !living.isBlocking()) {
			return false;
		}

		if (config.stunSlamOnlyFalling && !MaceItem.canSmashAttack(player)) {
			return false;
		}

		return findMace(player.getInventory()) != -1;
	}

	/** The axe hit has been sent: schedule the mace hit. */
	public static void onAxeHit(Minecraft mc, Entity entity) {
		target = entity;
		axeSlot = mc.player.getInventory().getSelectedSlot();
		fireTick = ClientTicks.get() + CPvPConfig.get().stunSlamDelay;
	}

	/** End of startAttack: with no delay the mace hit follows the axe hit and its swing in the same tick. */
	public static void afterAttack(Minecraft mc) {
		if (isPending() && fireTick <= ClientTicks.get()) {
			slam(mc);
		}
	}

	/** Start of handleKeybinds: sends a delayed mace hit once its tick has come. */
	public static void onKeybinds(Minecraft mc) {
		if (!isPending()) {
			return;
		}

		long now = ClientTicks.get();
		if (!CPvPConfig.get().stunSlamEnabled || now > fireTick + MAX_LATE_TICKS) {
			reset();
			return;
		}

		if (now >= fireTick) {
			slam(mc);
		}
	}

	public static void reset() {
		target = null;
		axeSlot = -1;
		fireTick = 0;
	}

	private static void slam(Minecraft mc) {
		Entity entity = target;
		int axe = axeSlot;
		reset();

		LocalPlayer player = mc.player;
		if (player == null || mc.gameMode == null || player.isSpectator() || player.isUsingItem() || player.isHandsBusy()) {
			return;
		}

		Inventory inventory = player.getInventory();
		// The player switched away from the axe in the meantime: leave the hotbar alone.
		if (inventory.getSelectedSlot() != axe || !inventory.getItem(axe).is(ItemTags.AXES)) {
			return;
		}

		// Only hit what the crosshair is on, exactly like a left click.
		if (!(mc.hitResult instanceof EntityHitResult hit) || hit.getEntity() != entity || !entity.isAlive()) {
			return;
		}

		if (CPvPConfig.get().stunSlamOnlyFalling && !MaceItem.canSmashAttack(player)) {
			// Landed during the delay: no smash any more.
			return;
		}

		int mace = findMace(inventory);
		if (mace == -1) {
			return;
		}

		ItemStack maceStack = inventory.getItem(mace);
		AttackRange range = maceStack.get(DataComponents.ATTACK_RANGE);
		if ((range != null && !range.isInRange(player, hit.getLocation())) || player.cannotAttackWithItem(maceStack, 0)) {
			return;
		}

		inventory.setSelectedSlot(mace);
		// The regular attack of a left click: carried item, attack packet, client side attack.
		mc.gameMode.attack(player, entity);
		player.swing(InteractionHand.MAIN_HAND);
		inventory.setSelectedSlot(axe);
		lastSlamTick = ClientTicks.get();
	}

	/** A hotbar mace, Density first. */
	private static int findMace(Inventory inventory) {
		int fallback = -1;
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.is(Items.MACE)) {
				continue;
			}

			if (hasDensity(stack)) {
				return i;
			}

			if (fallback == -1) {
				fallback = i;
			}
		}

		return fallback;
	}

	private static boolean hasDensity(ItemStack stack) {
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getEnchantments().entrySet()) {
			if (entry.getKey().is(Enchantments.DENSITY)) {
				return true;
			}
		}

		return false;
	}
}
