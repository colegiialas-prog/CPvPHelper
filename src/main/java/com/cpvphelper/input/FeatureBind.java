package com.cpvphelper.input;

import com.cpvphelper.config.BindSetting;
import com.cpvphelper.config.CPvPConfig;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * The bind of a feature. Without a key the feature works whenever it is enabled. A toggle bind switches the feature
 * on and off; a hold bind keeps an enabled feature active only while the key is held.
 */
public enum FeatureBind {
	TRIGGER_BOT("cpvphelper.feature.triggerbot", config -> config.triggerBind, config -> config.triggerBotEnabled, (config, value) -> config.triggerBotEnabled = value),
	SPRINT_RESET("cpvphelper.feature.sprintreset", config -> config.sprintResetBind, config -> config.sprintResetEnabled, (config, value) -> config.sprintResetEnabled = value),
	JUMP_RESET("cpvphelper.feature.jumpreset", config -> config.jumpResetBind, config -> config.jumpResetEnabled, (config, value) -> config.jumpResetEnabled = value),
	AUTO_CART("cpvphelper.feature.autocart", config -> config.autoCartBind, config -> config.autoCartEnabled, (config, value) -> config.autoCartEnabled = value);

	private final String nameKey;
	private final Function<CPvPConfig, BindSetting> setting;
	private final Predicate<CPvPConfig> enabled;
	private final BiConsumer<CPvPConfig, Boolean> enabledSetter;
	private boolean held;

	FeatureBind(String nameKey, Function<CPvPConfig, BindSetting> setting, Predicate<CPvPConfig> enabled, BiConsumer<CPvPConfig, Boolean> enabledSetter) {
		this.nameKey = nameKey;
		this.setting = setting;
		this.enabled = enabled;
		this.enabledSetter = enabledSetter;
	}

	/** Head of every frame: reads every bind once, so even the shortest tap is seen. */
	public static void onFrame(Minecraft mc) {
		boolean inGame = mc.gui.screen() == null && mc.player != null;
		for (FeatureBind bind : values())
			bind.update(mc, inGame);
	}

	public BindSetting setting() {
		return setting.apply(CPvPConfig.get());
	}

	public Component displayName() {
		return Component.translatable(nameKey);
	}

	public boolean isActive() {
		CPvPConfig config = CPvPConfig.get();
		if (!enabled.test(config))
			return false;

		BindSetting bind = setting.apply(config);
		return bind.key == Binds.NONE || bind.toggle || held;
	}

	private void update(Minecraft mc, boolean inGame) {
		CPvPConfig config = CPvPConfig.get();
		BindSetting bind = setting.apply(config);
		boolean down = inGame && Binds.isDown(mc, bind.key);
		boolean pressed = down && !held;
		held = down;
		if (!pressed || !bind.toggle)
			return;

		boolean value = !enabled.test(config);
		enabledSetter.accept(config, value);
		CPvPConfig.save();
		mc.player.sendOverlayMessage(Component.translatable("cpvphelper.message.toggled", displayName(), value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF));
	}
}
