package com.cpvphelper.config;

import com.cpvphelper.CPvPHelper;
import com.cpvphelper.input.Binds;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;

public final class CPvPConfig {
	public static final int STOP_TICKS_MIN = 1;
	public static final int STOP_TICKS_MAX = 3;
	public static final int TOTEM_DELAY_MAX = 10;
	public static final int TOTEM_RANDOM_DELAY_MAX = 5;
	public static final int ANCHOR_DELAY_MAX = 5;
	public static final int STUN_SLAM_DELAY_MAX = 5;
	public static final int SPEAR_REPEAT_DELAY_MAX = 10;
	public static final int TRIGGER_CHARGE_MIN = 50;
	public static final int TRIGGER_DELAY_MAX = 250;
	public static final int TRIGGER_WAIT_MAX = 10;
	public static final int TRIGGER_REACH_MIN = 150;
	public static final int TRIGGER_REACH_MAX = 300;
	public static final int SPRINT_RESET_DELAY_MAX = 3;
	public static final int SPRINT_RESET_TICKS_MIN = 1;
	public static final int SPRINT_RESET_TICKS_MAX = 3;
	public static final int AUTO_CART_RANGE_MIN = 100;
	public static final int AUTO_CART_RANGE_MAX = 450;
	public static final int AUTO_CART_DELAY_MAX = 5;
	public static final int AUTO_CART_BOW_CHARGE_MIN = 3;
	public static final int AUTO_CART_BOW_CHARGE_MAX = 20;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve(CPvPHelper.MOD_ID + ".json");
	private static CPvPConfig instance = new CPvPConfig();

	// AutoTotem
	public boolean autoTotemEnabled = true;
	/** Swap a hotbar totem with the vanilla "swap offhand" action: no inventory clicks at all. */
	public boolean totemHotbarSwap = true;
	/** Without a hotbar totem: release movement keys, click the totem into the offhand and close the inventory. */
	public boolean totemInventorySwap = true;
	/** Allow the inventory swap while airborne. Keys are released, momentum is left untouched. */
	public boolean totemInventoryInAir = true;
	/** Ticks with released movement keys before the inventory click. */
	public int totemStopTicks = 1;
	/** Reaction delay in ticks after the offhand becomes empty. */
	public int totemDelay = 0;
	/** Extra random delay in ticks, 0..value. */
	public int totemRandomDelay = 0;
	/** Also replace a non-totem item in the offhand. */
	public boolean totemReplaceItems = false;

	// AnchorPlace
	public boolean anchorEnabled = true;
	/** Ticks between placing the anchor and charging it. */
	public int anchorChargeDelay = 1;
	/** Ticks between charging the anchor and detonating it. */
	public int anchorExplodeDelay = 1;
	public boolean anchorExplode = true;
	public boolean anchorOnlyOnGround = false;

	// StunSlam
	public boolean stunSlamEnabled = true;
	/** Only when the mace hit is a smash attack: falling more than 1.5 blocks and not gliding. */
	public boolean stunSlamOnlyFalling = true;
	/** Only when the target is blocking with a shield. */
	public boolean stunSlamOnlyShield = false;
	/** Ticks between the axe hit and the mace hit. */
	public int stunSlamDelay = 0;

	// SpearSwap
	public boolean spearSwapEnabled = true;
	/** Keep swapping while the key is held, every time the attack is charged. */
	public boolean spearSwapHold = true;
	/** Extra ticks between two swaps while the key is held. */
	public int spearSwapRepeatDelay = 0;
	/** The swap key, see Binds. */
	public int spearSwapBind = Binds.NONE;

	// TriggerBot
	public boolean triggerBotEnabled = false;
	/** Attack players only; otherwise mobs as well. */
	public boolean triggerPlayersOnly = true;
	/** Only with a sword, axe, mace, spear or trident in the main hand. */
	public boolean triggerWeaponOnly = true;
	/** Lowest attack charge in percent; every hit rolls its threshold between this value and 100. */
	public int triggerCharge = 100;
	/** Reaction delay range in milliseconds, rolled again for every hit. */
	public int triggerDelayMin = 0;
	public int triggerDelayMax = 30;
	/** Ticks a ready hit may wait in a jump for the fall that makes it critical. */
	public int triggerCritWait = 8;
	/** Ticks a ready hit may wait for the sprint to come back during a w-tap or s-tap. */
	public int triggerSprintWait = 3;
	/** Shortest hit distance in hundredths of a block: closer targets are left alone. */
	public int triggerReachMin = 0;
	/** Longest hit distance in hundredths of a block, never more than the vanilla reach. */
	public int triggerReach = 300;
	public BindSetting triggerBind = new BindSetting();

	// SprintReset
	public boolean sprintResetEnabled = false;
	/** S-tap: press the back key instead of only releasing the forward key. */
	public boolean sprintResetBackward = false;
	/** Ticks between the sprint hit and the tap. */
	public int sprintResetDelay = 0;
	/** Ticks the tap lasts. */
	public int sprintResetTicks = 1;
	public BindSetting sprintResetBind = new BindSetting();

	// JumpReset
	public boolean jumpResetEnabled = false;
	/** Chance in percent that a knockback gets a jump reset. */
	public int jumpResetChance = 100;
	/** Also jump while eating or blocking; the jump does not stop the item use. */
	public boolean jumpResetWhileUsing = true;
	public BindSetting jumpResetBind = new BindSetting();

	// AutoCart
	public boolean autoCartEnabled = false;
	/** Longest distance to the rail in hundredths of a block, never more than the vanilla block reach. */
	public int autoCartRange = 450;
	/** Powered rail instead of a regular rail. */
	public boolean autoCartPoweredRail = false;
	/** Light the cart with flint and steel from the hotbar. */
	public boolean autoCartIgnite = true;
	/** Refill the TNT minecart hotbar slot from the inventory once it runs out. */
	public boolean autoCartRestock = true;
	/** Ticks between two steps. */
	public int autoCartDelay = 1;
	/** Ticks the bow of the shoot bind is drawn. */
	public int autoCartBowCharge = 10;
	/** The bow shoot key, see Binds. */
	public int autoCartBowBind = Binds.NONE;
	public BindSetting autoCartBind = new BindSetting();

	public static CPvPConfig get() {
		return instance;
	}

	public static void load() {
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH)) {
				CPvPConfig loaded = GSON.fromJson(reader, CPvPConfig.class);
				if (loaded != null) {
					instance = loaded;
				}
			} catch (Exception e) {
				CPvPHelper.LOGGER.warn("Failed to read {}, using defaults", PATH, e);
			}
		}

		instance.clamp();
		save();
	}

	public static void save() {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			CPvPHelper.LOGGER.warn("Failed to write {}", PATH, e);
		}
	}

	private void clamp() {
		totemStopTicks = Mth.clamp(totemStopTicks, STOP_TICKS_MIN, STOP_TICKS_MAX);
		totemDelay = Mth.clamp(totemDelay, 0, TOTEM_DELAY_MAX);
		totemRandomDelay = Mth.clamp(totemRandomDelay, 0, TOTEM_RANDOM_DELAY_MAX);
		anchorChargeDelay = Mth.clamp(anchorChargeDelay, 0, ANCHOR_DELAY_MAX);
		anchorExplodeDelay = Mth.clamp(anchorExplodeDelay, 0, ANCHOR_DELAY_MAX);
		stunSlamDelay = Mth.clamp(stunSlamDelay, 0, STUN_SLAM_DELAY_MAX);
		spearSwapRepeatDelay = Mth.clamp(spearSwapRepeatDelay, 0, SPEAR_REPEAT_DELAY_MAX);
		triggerCharge = Mth.clamp(triggerCharge, TRIGGER_CHARGE_MIN, 100);
		triggerDelayMin = Mth.clamp(triggerDelayMin, 0, TRIGGER_DELAY_MAX);
		triggerDelayMax = Mth.clamp(triggerDelayMax, 0, TRIGGER_DELAY_MAX);
		triggerCritWait = Mth.clamp(triggerCritWait, 0, TRIGGER_WAIT_MAX);
		triggerSprintWait = Mth.clamp(triggerSprintWait, 0, TRIGGER_WAIT_MAX);
		triggerReachMin = Mth.clamp(triggerReachMin, 0, TRIGGER_REACH_MAX);
		triggerReach = Mth.clamp(triggerReach, TRIGGER_REACH_MIN, TRIGGER_REACH_MAX);
		sprintResetDelay = Mth.clamp(sprintResetDelay, 0, SPRINT_RESET_DELAY_MAX);
		sprintResetTicks = Mth.clamp(sprintResetTicks, SPRINT_RESET_TICKS_MIN, SPRINT_RESET_TICKS_MAX);
		spearSwapBind = Binds.isValid(spearSwapBind) ? spearSwapBind : Binds.NONE;
		triggerBind = BindSetting.sanitize(triggerBind);
		sprintResetBind = BindSetting.sanitize(sprintResetBind);
		jumpResetBind = BindSetting.sanitize(jumpResetBind);
		autoCartBind = BindSetting.sanitize(autoCartBind);
		autoCartBowBind = Binds.isValid(autoCartBowBind) ? autoCartBowBind : Binds.NONE;
		autoCartRange = Mth.clamp(autoCartRange, AUTO_CART_RANGE_MIN, AUTO_CART_RANGE_MAX);
		autoCartDelay = Mth.clamp(autoCartDelay, 0, AUTO_CART_DELAY_MAX);
		autoCartBowCharge = Mth.clamp(autoCartBowCharge, AUTO_CART_BOW_CHARGE_MIN, AUTO_CART_BOW_CHARGE_MAX);
		jumpResetChance = Mth.clamp(jumpResetChance, 0, 100);
	}
}
