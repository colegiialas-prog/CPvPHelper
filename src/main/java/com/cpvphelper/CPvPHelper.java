package com.cpvphelper;

import com.cpvphelper.combat.SprintState;
import com.cpvphelper.config.CPvPConfig;
import com.cpvphelper.input.MenuCombo;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CPvPHelper implements ClientModInitializer {
	public static final String MOD_ID = "cpvphelper";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		CPvPConfig.load();
		ClientTickEvents.END_CLIENT_TICK.register(MenuCombo::onTick);
		ClientTickEvents.END_CLIENT_TICK.register(SprintState::onTickEnd);
		LOGGER.info("CPvPHelper loaded");
	}
}
