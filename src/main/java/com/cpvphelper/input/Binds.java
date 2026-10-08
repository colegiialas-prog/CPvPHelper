package com.cpvphelper.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Keys the mod reads straight from the window, outside the vanilla controls list. A bind is a GLFW key code,
 * {@code MOUSE + button} for a mouse button, or {@link #NONE}.
 */
public final class Binds {
	public static final int NONE = 0;
	public static final int MOUSE = 1000;

	private static final int[][] KEY_RANGES = {
		{ GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_SPACE },
		{ GLFW.GLFW_KEY_APOSTROPHE, GLFW.GLFW_KEY_APOSTROPHE },
		{ GLFW.GLFW_KEY_COMMA, GLFW.GLFW_KEY_9 },
		{ GLFW.GLFW_KEY_SEMICOLON, GLFW.GLFW_KEY_SEMICOLON },
		{ GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_EQUAL },
		{ GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_RIGHT_BRACKET },
		{ GLFW.GLFW_KEY_GRAVE_ACCENT, GLFW.GLFW_KEY_GRAVE_ACCENT },
		{ GLFW.GLFW_KEY_WORLD_1, GLFW.GLFW_KEY_WORLD_2 },
		{ GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_END },
		{ GLFW.GLFW_KEY_CAPS_LOCK, GLFW.GLFW_KEY_PAUSE },
		{ GLFW.GLFW_KEY_F1, GLFW.GLFW_KEY_F25 },
		{ GLFW.GLFW_KEY_KP_0, GLFW.GLFW_KEY_KP_EQUAL },
		{ GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_MENU }
	};

	private Binds() {
	}

	public static boolean isValid(int bind) {
		if (bind == NONE)
			return true;

		if (bind >= MOUSE)
			return bind - MOUSE <= GLFW.GLFW_MOUSE_BUTTON_LAST;

		for (int[] range : KEY_RANGES) {
			if (bind >= range[0] && bind <= range[1])
				return true;
		}

		return false;
	}

	public static boolean isDown(Minecraft mc, int bind) {
		if (bind == NONE)
			return false;

		long window = mc.getWindow().handle();
		if (bind >= MOUSE)
			return GLFW.glfwGetMouseButton(window, bind - MOUSE) == GLFW.GLFW_PRESS;

		return GLFW.glfwGetKey(window, bind) == GLFW.GLFW_PRESS;
	}

	/** The first key or mouse button held down right now, or {@link #NONE}. */
	public static int firstDown(Minecraft mc) {
		for (int button = GLFW.GLFW_MOUSE_BUTTON_1; button <= GLFW.GLFW_MOUSE_BUTTON_LAST; button++) {
			if (isDown(mc, MOUSE + button))
				return MOUSE + button;
		}

		for (int[] range : KEY_RANGES) {
			for (int key = range[0]; key <= range[1]; key++) {
				if (isDown(mc, key))
					return key;
			}
		}

		return NONE;
	}

	public static Component name(int bind) {
		if (bind == NONE)
			return Component.translatable("cpvphelper.bind.none");

		InputConstants.Key key = bind >= MOUSE ? InputConstants.Type.MOUSE.getOrCreate(bind - MOUSE) : InputConstants.Type.KEYSYM.getOrCreate(bind);
		return key.getDisplayName();
	}
}
