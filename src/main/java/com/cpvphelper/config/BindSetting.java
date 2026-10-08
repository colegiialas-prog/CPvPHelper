package com.cpvphelper.config;

import com.cpvphelper.input.Binds;

/** A feature bind: the key, and whether a press toggles the feature or holding the key keeps it active. */
public final class BindSetting {
	public int key = Binds.NONE;
	public boolean toggle = true;

	static BindSetting sanitize(BindSetting setting) {
		if (setting == null)
			return new BindSetting();

		if (!Binds.isValid(setting.key))
			setting.key = Binds.NONE;

		return setting;
	}
}
