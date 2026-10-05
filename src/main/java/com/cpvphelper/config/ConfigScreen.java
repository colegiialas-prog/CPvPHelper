package com.cpvphelper.config;

import com.cpvphelper.input.Binds;
import com.mojang.serialization.Codec;
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
			bind("cpvphelper.option.spear_swap.bind", "cpvphelper.option.spear_swap.bind.tooltip", config.spearSwapBind, value -> config.spearSwapBind = value),
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
			bind("cpvphelper.option.trigger.hold_key", "cpvphelper.option.trigger.hold_key.tooltip", config.triggerHoldBind, value -> config.triggerHoldBind = value),
			toggle("cpvphelper.option.trigger.players_only", "cpvphelper.option.trigger.players_only.tooltip", config.triggerPlayersOnly, value -> config.triggerPlayersOnly = value),
			toggle("cpvphelper.option.trigger.weapon_only", "cpvphelper.option.trigger.weapon_only.tooltip", config.triggerWeaponOnly, value -> config.triggerWeaponOnly = value),
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

		this.list.addHeader(Component.translatable("cpvphelper.feature.jumpreset"));
		this.list.addSmall(
			toggle("cpvphelper.option.enabled", "cpvphelper.option.jump_reset.enabled.tooltip", config.jumpResetEnabled, value -> config.jumpResetEnabled = value),
			toggle("cpvphelper.option.jump_reset.while_using", "cpvphelper.option.jump_reset.while_using.tooltip", config.jumpResetWhileUsing, value -> config.jumpResetWhileUsing = value),
			slider("cpvphelper.option.jump_reset.chance", "cpvphelper.option.jump_reset.chance.tooltip", "cpvphelper.value.percent", 0, 100, config.jumpResetChance, value -> config.jumpResetChance = value)
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

	private static OptionInstance<Integer> bind(String key, String tooltipKey, int value, Consumer<Integer> setter) {
		return new OptionInstance<>(
			key,
			OptionInstance.cachedConstantTooltip(Component.translatable(tooltipKey)),
			(caption, bind) -> Options.genericValueLabel(caption, Binds.name(bind)),
			new OptionInstance.Enum<>(Binds.CHOICES, Codec.INT),
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
