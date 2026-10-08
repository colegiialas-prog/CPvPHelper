package com.cpvphelper.config;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends OptionsSubScreen {
	public ConfigScreen(Screen parent) {
		super(parent, Minecraft.getInstance().options, Component.translatable("cpvphelper.config.title"));
	}

	@Override
	protected void addOptions() {
		CPvPConfig config = CPvPConfig.get();

		this.list.addSmall(openBinds());

		this.list.addHeader(Component.translatable("cpvphelper.feature.autototem"));
		this.list.addSmall(
			toggle("cpvphelper.option.enabled", "cpvphelper.option.totem.enabled.tooltip", config.autoTotemEnabled, value -> config.autoTotemEnabled = value),
			toggle("cpvphelper.option.totem.replace", "cpvphelper.option.totem.replace.tooltip", config.totemReplaceItems, value -> config.totemReplaceItems = value),
			toggle("cpvphelper.option.totem.hotbar", "cpvphelper.option.totem.hotbar.tooltip", config.totemHotbarSwap, value -> config.totemHotbarSwap = value),
			toggle("cpvphelper.option.totem.inventory", "cpvphelper.option.totem.inventory.tooltip", config.totemInventorySwap, value -> config.totemInventorySwap = value),
			toggle("cpvphelper.option.totem.in_air", "cpvphelper.option.totem.in_air.tooltip", config.totemInventoryInAir, value -> config.totemInventoryInAir = value),
			ticks(
				"cpvphelper.option.totem.stop_ticks",
				"cpvphelper.option.totem.stop_ticks.tooltip",
				CPvPConfig.STOP_TICKS_MIN,
				CPvPConfig.STOP_TICKS_MAX,
				config.totemStopTicks,
				value -> config.totemStopTicks = value
			),
			ticks("cpvphelper.option.totem.delay", "cpvphelper.option.totem.delay.tooltip", 0, CPvPConfig.TOTEM_DELAY_MAX, config.totemDelay, value -> config.totemDelay = value),
			ticks(
				"cpvphelper.option.totem.random_delay",
				"cpvphelper.option.totem.random_delay.tooltip",
				0,
				CPvPConfig.TOTEM_RANDOM_DELAY_MAX,
				config.totemRandomDelay,
				value -> config.totemRandomDelay = value
			)
		);

		this.list.addHeader(Component.translatable("cpvphelper.feature.anchorplace"));
		this.list.addSmall(
			toggle("cpvphelper.option.enabled", "cpvphelper.option.anchor.enabled.tooltip", config.anchorEnabled, value -> config.anchorEnabled = value),
			toggle("cpvphelper.option.anchor.only_ground", "cpvphelper.option.anchor.only_ground.tooltip", config.anchorOnlyOnGround, value -> config.anchorOnlyOnGround = value),
			ticks(
				"cpvphelper.option.anchor.charge_delay",
				"cpvphelper.option.anchor.charge_delay.tooltip",
				0,
				CPvPConfig.ANCHOR_DELAY_MAX,
				config.anchorChargeDelay,
				value -> config.anchorChargeDelay = value
			),
			ticks(
				"cpvphelper.option.anchor.explode_delay",
				"cpvphelper.option.anchor.explode_delay.tooltip",
				0,
				CPvPConfig.ANCHOR_DELAY_MAX,
				config.anchorExplodeDelay,
				value -> config.anchorExplodeDelay = value
			),
			toggle("cpvphelper.option.anchor.explode", "cpvphelper.option.anchor.explode.tooltip", config.anchorExplode, value -> config.anchorExplode = value)
		);

		this.list.addHeader(Component.translatable("cpvphelper.feature.stunslam"));
		this.list.addSmall(
			toggle("cpvphelper.option.enabled", "cpvphelper.option.stun_slam.enabled.tooltip", config.stunSlamEnabled, value -> config.stunSlamEnabled = value),
			toggle(
				"cpvphelper.option.stun_slam.only_falling",
				"cpvphelper.option.stun_slam.only_falling.tooltip",
				config.stunSlamOnlyFalling,
				value -> config.stunSlamOnlyFalling = value
			),
			toggle("cpvphelper.option.stun_slam.only_shield", "cpvphelper.option.stun_slam.only_shield.tooltip", config.stunSlamOnlyShield, value -> config.stunSlamOnlyShield = value),
			ticks(
				"cpvphelper.option.stun_slam.delay",
				"cpvphelper.option.stun_slam.delay.tooltip",
				0,
				CPvPConfig.STUN_SLAM_DELAY_MAX,
				config.stunSlamDelay,
				value -> config.stunSlamDelay = value
			)
		);

		this.list.addHeader(Component.translatable("cpvphelper.feature.spearswap"));
		this.list.addSmall(
			toggle("cpvphelper.option.enabled", "cpvphelper.option.spear_swap.enabled.tooltip", config.spearSwapEnabled, value -> config.spearSwapEnabled = value),
			toggle("cpvphelper.option.spear_swap.hold", "cpvphelper.option.spear_swap.hold.tooltip", config.spearSwapHold, value -> config.spearSwapHold = value),
			ticks(
				"cpvphelper.option.spear_swap.repeat_delay",
				"cpvphelper.option.spear_swap.repeat_delay.tooltip",
				0,
				CPvPConfig.SPEAR_REPEAT_DELAY_MAX,
				config.spearSwapRepeatDelay,
				value -> config.spearSwapRepeatDelay = value
			)
		);

		this.list.addHeader(Component.translatable("cpvphelper.feature.triggerbot"));
		this.list.addSmall(
			toggle("cpvphelper.option.enabled", "cpvphelper.option.trigger.enabled.tooltip", config.triggerBotEnabled, value -> config.triggerBotEnabled = value),
			toggle("cpvphelper.option.trigger.players_only", "cpvphelper.option.trigger.players_only.tooltip", config.triggerPlayersOnly, value -> config.triggerPlayersOnly = value),
			toggle("cpvphelper.option.trigger.weapon_only", "cpvphelper.option.trigger.weapon_only.tooltip", config.triggerWeaponOnly, value -> config.triggerWeaponOnly = value),
			distance(
				"cpvphelper.option.trigger.reach_min",
				"cpvphelper.option.trigger.reach_min.tooltip",
				0,
				CPvPConfig.TRIGGER_REACH_MAX,
				config.triggerReachMin,
				value -> config.triggerReachMin = value
			),
			distance(
				"cpvphelper.option.trigger.reach",
				"cpvphelper.option.trigger.reach.tooltip",
				CPvPConfig.TRIGGER_REACH_MIN,
				CPvPConfig.TRIGGER_REACH_MAX,
				config.triggerReach,
				value -> config.triggerReach = value
			),
			slider(
				"cpvphelper.option.trigger.charge",
				"cpvphelper.option.trigger.charge.tooltip",
				"cpvphelper.value.percent",
				CPvPConfig.TRIGGER_CHARGE_MIN,
				100,
				config.triggerCharge,
				value -> config.triggerCharge = value
			),
			slider(
				"cpvphelper.option.trigger.delay_min",
				"cpvphelper.option.trigger.delay_min.tooltip",
				"cpvphelper.value.ms",
				0,
				CPvPConfig.TRIGGER_DELAY_MAX,
				config.triggerDelayMin,
				value -> config.triggerDelayMin = value
			),
			slider(
				"cpvphelper.option.trigger.delay_max",
				"cpvphelper.option.trigger.delay_max.tooltip",
				"cpvphelper.value.ms",
				0,
				CPvPConfig.TRIGGER_DELAY_MAX,
				config.triggerDelayMax,
				value -> config.triggerDelayMax = value
			),
			ticks(
				"cpvphelper.option.trigger.crit_wait",
				"cpvphelper.option.trigger.crit_wait.tooltip",
				0,
				CPvPConfig.TRIGGER_WAIT_MAX,
				config.triggerCritWait,
				value -> config.triggerCritWait = value
			),
			ticks(
				"cpvphelper.option.trigger.sprint_wait",
				"cpvphelper.option.trigger.sprint_wait.tooltip",
				0,
				CPvPConfig.TRIGGER_WAIT_MAX,
				config.triggerSprintWait,
				value -> config.triggerSprintWait = value
			)
		);

		this.list.addHeader(Component.translatable("cpvphelper.feature.sprintreset"));
		this.list.addSmall(
			toggle("cpvphelper.option.enabled", "cpvphelper.option.sprint_reset.enabled.tooltip", config.sprintResetEnabled, value -> config.sprintResetEnabled = value),
			toggle("cpvphelper.option.sprint_reset.backward", "cpvphelper.option.sprint_reset.backward.tooltip", config.sprintResetBackward, value -> config.sprintResetBackward = value),
			ticks(
				"cpvphelper.option.sprint_reset.delay",
				"cpvphelper.option.sprint_reset.delay.tooltip",
				0,
				CPvPConfig.SPRINT_RESET_DELAY_MAX,
				config.sprintResetDelay,
				value -> config.sprintResetDelay = value
			),
			ticks(
				"cpvphelper.option.sprint_reset.ticks",
				"cpvphelper.option.sprint_reset.ticks.tooltip",
				CPvPConfig.SPRINT_RESET_TICKS_MIN,
				CPvPConfig.SPRINT_RESET_TICKS_MAX,
				config.sprintResetTicks,
				value -> config.sprintResetTicks = value
			)
		);

		this.list.addHeader(Component.translatable("cpvphelper.feature.jumpreset"));
		this.list.addSmall(
			toggle("cpvphelper.option.enabled", "cpvphelper.option.jump_reset.enabled.tooltip", config.jumpResetEnabled, value -> config.jumpResetEnabled = value),
			toggle("cpvphelper.option.jump_reset.while_using", "cpvphelper.option.jump_reset.while_using.tooltip", config.jumpResetWhileUsing, value -> config.jumpResetWhileUsing = value),
			slider("cpvphelper.option.jump_reset.chance", "cpvphelper.option.jump_reset.chance.tooltip", "cpvphelper.value.percent", 0, 100, config.jumpResetChance, value -> config.jumpResetChance = value)
		);

		this.list.addHeader(Component.translatable("cpvphelper.feature.autocart"));
		this.list.addSmall(
			toggle("cpvphelper.option.enabled", "cpvphelper.option.auto_cart.enabled.tooltip", config.autoCartEnabled, value -> config.autoCartEnabled = value),
			toggle("cpvphelper.option.auto_cart.powered_rail", "cpvphelper.option.auto_cart.powered_rail.tooltip", config.autoCartPoweredRail, value -> config.autoCartPoweredRail = value),
			toggle("cpvphelper.option.auto_cart.ignite", "cpvphelper.option.auto_cart.ignite.tooltip", config.autoCartIgnite, value -> config.autoCartIgnite = value),
			toggle("cpvphelper.option.auto_cart.restock", "cpvphelper.option.auto_cart.restock.tooltip", config.autoCartRestock, value -> config.autoCartRestock = value),
			distance(
				"cpvphelper.option.auto_cart.range",
				"cpvphelper.option.auto_cart.range.tooltip",
				CPvPConfig.AUTO_CART_RANGE_MIN,
				CPvPConfig.AUTO_CART_RANGE_MAX,
				config.autoCartRange,
				value -> config.autoCartRange = value
			),
			ticks("cpvphelper.option.auto_cart.delay", "cpvphelper.option.auto_cart.delay.tooltip", 0, CPvPConfig.AUTO_CART_DELAY_MAX, config.autoCartDelay, value -> config.autoCartDelay = value),
			ticks(
				"cpvphelper.option.auto_cart.bow_charge",
				"cpvphelper.option.auto_cart.bow_charge.tooltip",
				CPvPConfig.AUTO_CART_BOW_CHARGE_MIN,
				CPvPConfig.AUTO_CART_BOW_CHARGE_MAX,
				config.autoCartBowCharge,
				value -> config.autoCartBowCharge = value
			)
		);
	}

	@Override
	public void removed() {
		CPvPConfig.save();
		super.removed();
	}

	private static OptionInstance<Boolean> toggle(String key, String tooltipKey, boolean value, Consumer<Boolean> setter) {
		return OptionInstance.createBoolean(key, OptionInstance.cachedConstantTooltip(Component.translatable(tooltipKey)), value, setter::accept);
	}

	private OptionInstance<Boolean> openBinds() {
		return new OptionInstance<>(
			"cpvphelper.option.binds",
			OptionInstance.cachedConstantTooltip(Component.translatable("cpvphelper.option.binds.tooltip")),
			(caption, value) -> Options.genericValueLabel(caption, Component.translatable("cpvphelper.option.binds.open")),
			new OptionInstance.Enum<>(List.of(Boolean.FALSE, Boolean.TRUE), Codec.BOOL),
			Boolean.FALSE,
			value -> this.minecraft.gui.setScreen(new BindScreen(this))
		);
	}

	private static OptionInstance<Integer> distance(String key, String tooltipKey, int min, int max, int value, Consumer<Integer> setter) {
		return new OptionInstance<>(
			key,
			OptionInstance.cachedConstantTooltip(Component.translatable(tooltipKey)),
			(caption, hundredths) -> Options.genericValueLabel(caption, Component.translatable("cpvphelper.value.blocks", String.format(Locale.ROOT, "%.2f", hundredths / 100.0))),
			new OptionInstance.IntRange(min, max),
			value,
			setter::accept
		);
	}

	private static OptionInstance<Integer> ticks(String key, String tooltipKey, int min, int max, int value, Consumer<Integer> setter) {
		return slider(key, tooltipKey, "cpvphelper.value.ticks", min, max, value, setter);
	}

	private static OptionInstance<Integer> slider(String key, String tooltipKey, String valueKey, int min, int max, int value, Consumer<Integer> setter) {
		return new OptionInstance<>(
			key,
			OptionInstance.cachedConstantTooltip(Component.translatable(tooltipKey)),
			(caption, number) -> Options.genericValueLabel(caption, Component.translatable(valueKey, number)),
			new OptionInstance.IntRange(min, max),
			value,
			setter::accept
		);
	}
}
