package com.cpvphelper.input;

import com.cpvphelper.config.ConfigScreen;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Ctrl + Shift + X opens the settings screen, and closes it again. */
public final class MenuCombo {
	private static boolean wasDown;

	private MenuCombo() {
	}

	/** End of every client tick. */
	public static void onTick(Minecraft mc) {
		boolean down = isComboDown(mc);
		boolean pressed = down && !wasDown;
		wasDown = down;
		if (!pressed)
			return;

		if (mc.gui.screen() instanceof ConfigScreen screen) {
			screen.onClose();
			return;
		}

		if (mc.gui.screen() == null)
			mc.setScreen(new ConfigScreen(null));
	}

	private static boolean isComboDown(Minecraft mc) {
		long window = mc.getWindow().handle();
		boolean control = isPressed(window, GLFW.GLFW_KEY_LEFT_CONTROL) || isPressed(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
		boolean shift = isPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT) || isPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
		return control && shift && isPressed(window, GLFW.GLFW_KEY_X);
	}

	private static boolean isPressed(long window, int key) {
		return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
	}
}
