package com.cpvphelper;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CPvPHelper implements ClientModInitializer {
	public static final String MOD_ID = "cpvphelper";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("CPvPHelper loaded");
	}
}
