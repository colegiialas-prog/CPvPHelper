package com.cpvphelper.feature;

import com.cpvphelper.config.CPvPConfig;
import com.cpvphelper.input.Binds;
import com.cpvphelper.mixin.MinecraftAccessor;
import com.cpvphelper.mixin.MultiPlayerGameModeAccessor;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Spear attribute swap on a key: select the spear slot, jab (the spear left click), select the previous slot
 * again, all within one tick. Works from whatever item is held; a Lunge spear pushes the player forward.
 *
 * <p>The jab is the one vanilla sends for a spear left click, with the same client checks: no miss cooldown,
 * hands free, not using an item and the attack charged as the spear requires it.
 */
public final class SpearSwap {
	private static boolean wasDown;
	private static long nextTick;

	private SpearSwap() {
	}

	/** Start of handleKeybinds, after the other features. */
	public static void onKeybinds(Minecraft mc) {
		CPvPConfig config = CPvPConfig.get();
		boolean down = Binds.isDown(mc, config.spearSwapBind);
		boolean clicked = down && !wasDown;
		wasDown = down;

		LocalPlayer player = mc.player;
		if (!config.spearSwapEnabled || player == null || mc.level == null || mc.gameMode == null) {
			return;
		}

		long now = ClientTicks.get();
		boolean held = config.spearSwapHold && down && now >= nextTick;
		if (!clicked && !held) {
			return;
		}

		if (swap(mc, player)) {
			nextTick = now + 1 + config.spearSwapRepeatDelay;
		}
	}

	private static boolean swap(Minecraft mc, LocalPlayer player) {
		if (player.isSpectator() || player.isHandsBusy() || player.isUsingItem() || mc.gameMode.isDestroying() || AnchorPlace.isActive() || AutoCart.isActive()) {
			return false;
		}

		if (((MinecraftAccessor) mc).cpvphelper$getMissTime() > 0) {
			return false;
		}

		Inventory inventory = player.getInventory();
		int spear = findSpear(inventory);
		if (spear == -1) {
			return false;
		}

		ItemStack spearStack = inventory.getItem(spear);
		PiercingWeapon weapon = spearStack.get(DataComponents.PIERCING_WEAPON);
		// The charge check of a left click, made before the swap like when the slot key and the click land in one tick.
		if (weapon == null || !spearStack.isItemEnabled(mc.level.enabledFeatures()) || player.cannotAttackWithItem(spearStack, 0)) {
			return false;
		}

		int selected = inventory.getSelectedSlot();
		inventory.setSelectedSlot(spear);
		// The spear left click: carried item, jab packet, swing.
		mc.gameMode.piercingAttack(weapon);
		player.swing(InteractionHand.MAIN_HAND);
		if (spear != selected) {
			inventory.setSelectedSlot(selected);
			// Back to the previous slot in the same tick.
			((MultiPlayerGameModeAccessor) mc.gameMode).cpvphelper$ensureHasSentCarriedItem();
		}

		return true;
	}

	/** A hotbar spear, the highest Lunge level first. */
	private static int findSpear(Inventory inventory) {
		int best = -1;
		int bestLunge = -1;
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.is(ItemTags.SPEARS) || !stack.has(DataComponents.PIERCING_WEAPON)) {
				continue;
			}

			int lunge = lungeLevel(stack);
			if (lunge > bestLunge) {
				best = i;
				bestLunge = lunge;
			}
		}

		return best;
	}

	private static int lungeLevel(ItemStack stack) {
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getEnchantments().entrySet()) {
			if (entry.getKey().is(Enchantments.LUNGE)) {
				return entry.getIntValue();
			}
		}

		return 0;
	}
}
