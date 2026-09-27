package com.cpvphelper;

import com.cpvphelper.config.CPvPConfig;
import com.cpvphelper.feature.SpearSwap;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CPvPHelper implements ClientModInitializer {
	public static final String MOD_ID = "cpvphelper";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static KeyMapping toggleAutoTotem;
	private static KeyMapping toggleAnchorPlace;
	private static KeyMapping toggleStunSlam;

	@Override
	public void onInitializeClient() {
		CPvPConfig.load();

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		toggleAutoTotem = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.cpvphelper.toggle_autototem", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category)
		);
		toggleAnchorPlace = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.cpvphelper.toggle_anchorplace", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category)
		);
		toggleStunSlam = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.cpvphelper.toggle_stunslam", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category)
		);
		// Handled in handleKeybinds, next to the vanilla attack key. Mouse buttons can be bound too.
		SpearSwap.init(KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.cpvphelper.spear_swap", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category)
		));

		ClientTickEvents.END_CLIENT_TICK.register(CPvPHelper::handleToggleKeys);
		LOGGER.info("CPvPHelper loaded");
	}

	private static void handleToggleKeys(Minecraft mc) {
		CPvPConfig config = CPvPConfig.get();
		while (toggleAutoTotem.consumeClick()) {
			config.autoTotemEnabled = !config.autoTotemEnabled;
			CPvPConfig.save();
			showToggle(mc, "cpvphelper.feature.autototem", config.autoTotemEnabled);
		}

		while (toggleAnchorPlace.consumeClick()) {
			config.anchorEnabled = !config.anchorEnabled;
			CPvPConfig.save();
			showToggle(mc, "cpvphelper.feature.anchorplace", config.anchorEnabled);
		}

		while (toggleStunSlam.consumeClick()) {
			config.stunSlamEnabled = !config.stunSlamEnabled;
			CPvPConfig.save();
			showToggle(mc, "cpvphelper.feature.stunslam", config.stunSlamEnabled);
		}
	}

	private static void showToggle(Minecraft mc, String featureKey, boolean enabled) {
		if (mc.player != null) {
			mc.player.sendOverlayMessage(
				Component.translatable("cpvphelper.message.toggled", Component.translatable(featureKey), enabled ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF)
			);
		}
	}
}
