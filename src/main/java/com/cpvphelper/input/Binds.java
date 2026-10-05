package com.cpvphelper.input;

import java.util.List;
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
	public static final List<Integer> CHOICES = List.of(
		NONE,
		MOUSE + GLFW.GLFW_MOUSE_BUTTON_4,
		MOUSE + GLFW.GLFW_MOUSE_BUTTON_5,
		MOUSE + GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
		GLFW.GLFW_KEY_R,
		GLFW.GLFW_KEY_G,
		GLFW.GLFW_KEY_V,
		GLFW.GLFW_KEY_C,
		GLFW.GLFW_KEY_Z,
		GLFW.GLFW_KEY_B,
		GLFW.GLFW_KEY_LEFT_ALT
	);

	private Binds() {
	}

	public static boolean isDown(Minecraft mc, int bind) {
		if (bind == NONE)
			return false;

		long window = mc.getWindow().handle();
		if (bind >= MOUSE)
			return GLFW.glfwGetMouseButton(window, bind - MOUSE) == GLFW.GLFW_PRESS;

		return GLFW.glfwGetKey(window, bind) == GLFW.GLFW_PRESS;
	}

	public static Component name(int bind) {
		if (bind == NONE)
			return Component.translatable("cpvphelper.bind.none");

		if (bind >= MOUSE)
			return Component.translatable("cpvphelper.bind.mouse", bind - MOUSE + 1);

		String key = GLFW.glfwGetKeyName(bind, 0);
		return Component.literal(key == null ? "Alt" : key.toUpperCase());
	}
}
