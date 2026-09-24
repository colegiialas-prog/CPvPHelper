package com.cpvphelper.feature;

import com.cpvphelper.config.CPvPConfig;
import com.cpvphelper.mixin.MultiPlayerGameModeAccessor;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Keeps a totem in the offhand using only actions a vanilla player can do.
 *
 * <ul>
 *     <li>Hotbar totem: select its slot, send the vanilla "swap offhand" action (the F key), select the old slot
 *     again. No inventory clicks are involved.</li>
 *     <li>Inventory totem: release the movement keys for {@code totemStopTicks} ticks (what opening the inventory
 *     does), then click the totem into the offhand like pressing F over it and close the inventory.</li>
 * </ul>
 */
public final class AutoTotem {
	private enum State {
		IDLE,
		DELAY,
		STOPPING,
		WAITING
	}

	private static State state = State.IDLE;
	private static int delayLeft;
	private static int waitLeft;
	private static int waitHotbarSlot = -1;
	private static int queuedHotbarSlot = -1;

	private AutoTotem() {
	}

	/** Start of the client tick: nothing has been sent for this tick yet. */
	public static void onTickStart(Minecraft mc) {
		queuedHotbarSlot = -1;
		LocalPlayer player = mc.player;
		CPvPConfig config = CPvPConfig.get();
		if (player == null || mc.gameMode == null || !config.autoTotemEnabled || !canAct(player)) {
			reset();
			return;
		}

		if (state == State.WAITING) {
			if (!isConfirmed(player) && --waitLeft > 0) {
				return;
			}

			state = State.IDLE;
		}

		boolean needsTotem = needsTotem(player, config);
		if (state == State.STOPPING) {
			finishInventorySwap(mc, player, config, needsTotem);
			return;
		}

		if (!needsTotem) {
			state = State.IDLE;
			return;
		}

		if (state == State.IDLE) {
			delayLeft = config.totemDelay;
			if (config.totemRandomDelay > 0) {
				delayLeft += ThreadLocalRandom.current().nextInt(config.totemRandomDelay + 1);
			}

			state = State.DELAY;
		}

		if (delayLeft > 0) {
			delayLeft--;
			return;
		}

		Screen screen = mc.gui.screen();
		if (isOwnInventoryOpen(player, screen)) {
			// The player opened the inventory: keys are already released, just "press F" over the totem.
			int menuSlot = config.totemInventorySwap ? findInventoryTotem(player, true) : -1;
			if (menuSlot != -1) {
				swapFromInventory(mc, player, menuSlot, false);
				startWaiting(player, -1);
			}

			return;
		}

		if (screen != null || mc.gui.overlay() != null) {
			return;
		}

		if (config.totemHotbarSwap) {
			int hotbarSlot = findHotbarTotem(player);
			if (hotbarSlot != -1) {
				// Sent from handleKeybinds, where vanilla handles the swap offhand key.
				queuedHotbarSlot = hotbarSlot;
				return;
			}
		}

		if (config.totemInventorySwap
			&& player.containerMenu == player.inventoryMenu
			&& findInventoryTotem(player, !config.totemHotbarSwap) != -1
			&& (player.onGround() || config.totemInventoryInAir)) {
			MovementStop.request(config.totemStopTicks);
			state = State.STOPPING;
		}
	}

	/** Start of handleKeybinds. Returns true if a swap was sent this tick. */
	public static boolean onKeybinds(Minecraft mc) {
		int hotbarSlot = queuedHotbarSlot;
		queuedHotbarSlot = -1;
		LocalPlayer player = mc.player;
		if (hotbarSlot == -1 || player == null || mc.gameMode == null) {
			return false;
		}

		Inventory inventory = player.getInventory();
		if (!inventory.getItem(hotbarSlot).is(Items.TOTEM_OF_UNDYING)) {
			return false;
		}

		int selected = inventory.getSelectedSlot();
		if (hotbarSlot != selected) {
			inventory.setSelectedSlot(hotbarSlot);
			((MultiPlayerGameModeAccessor) mc.gameMode).cpvphelper$ensureHasSentCarriedItem();
		}

		// The packet the vanilla swap offhand key sends.
		player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
		if (hotbarSlot != selected) {
			// Synced by the next carried item check, like after pressing a hotbar key.
			inventory.setSelectedSlot(selected);
		}

		startWaiting(player, hotbarSlot);
		return true;
	}

	public static void reset() {
		if (state == State.STOPPING) {
			MovementStop.reset();
		}

		state = State.IDLE;
		delayLeft = 0;
		waitLeft = 0;
		waitHotbarSlot = -1;
		queuedHotbarSlot = -1;
	}

	private static void finishInventorySwap(Minecraft mc, LocalPlayer player, CPvPConfig config, boolean needsTotem) {
		Screen screen = mc.gui.screen();
		boolean ownInventory = isOwnInventoryOpen(player, screen);
		boolean noScreen = screen == null && mc.gui.overlay() == null;
		if (!needsTotem || !(noScreen || ownInventory) || player.containerMenu != player.inventoryMenu) {
			MovementStop.reset();
			state = State.IDLE;
			return;
		}

		if (noScreen && !MovementStop.isDone()) {
			// The released keys have not reached the server yet.
			return;
		}

		MovementStop.reset();
		int menuSlot = findInventoryTotem(player, ownInventory || !config.totemHotbarSwap);
		if (menuSlot == -1) {
			state = State.IDLE;
			return;
		}

		swapFromInventory(mc, player, menuSlot, noScreen);
		startWaiting(player, -1);
	}

	private static void swapFromInventory(Minecraft mc, LocalPlayer player, int menuSlot, boolean closeInventory) {
		int containerId = player.inventoryMenu.containerId;
		// The click the inventory screen sends when F is pressed over a slot.
		mc.gameMode.handleContainerInput(containerId, menuSlot, Inventory.SLOT_OFFHAND, ContainerInput.SWAP, player);
		if (closeInventory) {
			// What vanilla sends when the inventory screen is closed.
			player.connection.send(new ServerboundContainerClosePacket(containerId));
		}
	}

	private static void startWaiting(LocalPlayer player, int hotbarSlot) {
		state = State.WAITING;
		waitHotbarSlot = hotbarSlot;
		int pingTicks = pingTicks(player);
		// Hotbar swaps are not predicted client side, so never repeat one before the server answered.
		waitLeft = hotbarSlot == -1 ? pingTicks + 2 : Math.max(20, pingTicks * 3 + 5);
	}

	private static boolean isConfirmed(LocalPlayer player) {
		// Inventory clicks are predicted by the client, for them only the timer counts.
		if (waitHotbarSlot == -1) {
			return false;
		}

		return player.getOffhandItem().is(Items.TOTEM_OF_UNDYING) || !player.getInventory().getItem(waitHotbarSlot).is(Items.TOTEM_OF_UNDYING);
	}

	private static boolean canAct(LocalPlayer player) {
		return player.isAlive() && !player.isSpectator() && !player.hasInfiniteMaterials();
	}

	private static boolean needsTotem(LocalPlayer player, CPvPConfig config) {
		ItemStack offhand = player.getOffhandItem();
		return !offhand.is(Items.TOTEM_OF_UNDYING) && (offhand.isEmpty() || config.totemReplaceItems);
	}

	private static boolean isOwnInventoryOpen(LocalPlayer player, Screen screen) {
		return screen instanceof InventoryScreen
			&& player.containerMenu == player.inventoryMenu
			&& player.containerMenu.getCarried().isEmpty();
	}

	private static int findHotbarTotem(LocalPlayer player) {
		Inventory inventory = player.getInventory();
		int selected = inventory.getSelectedSlot();
		if (inventory.getItem(selected).is(Items.TOTEM_OF_UNDYING)) {
			return selected;
		}

		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			if (inventory.getItem(i).is(Items.TOTEM_OF_UNDYING)) {
				return i;
			}
		}

		return -1;
	}

	/** Returns the inventory menu slot of a totem, main inventory first. */
	private static int findInventoryTotem(LocalPlayer player, boolean includeHotbar) {
		List<Slot> slots = player.inventoryMenu.slots;
		for (int i = InventoryMenu.INV_SLOT_START; i < InventoryMenu.INV_SLOT_END; i++) {
			if (slots.get(i).getItem().is(Items.TOTEM_OF_UNDYING)) {
				return i;
			}
		}

		if (includeHotbar) {
			for (int i = InventoryMenu.USE_ROW_SLOT_START; i < InventoryMenu.USE_ROW_SLOT_END; i++) {
				if (slots.get(i).getItem().is(Items.TOTEM_OF_UNDYING)) {
					return i;
				}
			}
		}

		return -1;
	}

	private static int pingTicks(LocalPlayer player) {
		PlayerInfo info = player.connection.getPlayerInfo(player.getUUID());
		int latency = info == null ? 100 : info.getLatency();
		return Mth.clamp(latency / 50 + 1, 1, 20);
	}
}
