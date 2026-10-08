package com.cpvphelper.config;

import com.cpvphelper.input.Binds;
import com.cpvphelper.input.FeatureBind;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Binds of the features. A click on a bind waits for the next key or mouse button; Backspace or Delete clears it.
 * Every feature bind has its own mode button: toggle or hold.
 */
public class BindScreen extends Screen {
	private record Capture(Button button, Component name, IntSupplier getter, IntConsumer setter) {
		void refresh() {
			button.setMessage(Component.translatable("cpvphelper.bind.entry", name, Binds.name(getter.getAsInt())));
		}
	}

	private final Screen parent;
	private Capture capture;
	private boolean awaitingRelease;

	public BindScreen(Screen parent) {
		super(Component.translatable("cpvphelper.binds.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int left = this.width / 2 - 155;
		int top = this.height / 6;
		FeatureBind[] features = FeatureBind.values();
		for (int row = 0; row < features.length; row++) {
			BindSetting setting = features[row].setting();
			int y = top + row * 24;
			addBindButton(left, y, 210, features[row].displayName(), () -> setting.key, key -> setting.key = key);
			this.addRenderableWidget(
				Button.builder(modeLabel(setting), button -> switchMode(button, setting))
					.bounds(left + 215, y, 95, 20)
					.tooltip(Tooltip.create(Component.translatable("cpvphelper.bind.mode.tooltip")))
					.build()
			);
		}

		CPvPConfig config = CPvPConfig.get();
		addBindButton(left, top + features.length * 24, 310, Component.translatable("cpvphelper.feature.spearswap"), () -> config.spearSwapBind, key -> config.spearSwapBind = key);
		addBindButton(left, top + (features.length + 1) * 24, 310, Component.translatable("cpvphelper.bind.auto_cart_bow"), () -> config.autoCartBowBind, key -> config.autoCartBowBind = key);
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).bounds(this.width / 2 - 100, this.height - 28, 200, 20).build());
	}

	@Override
	public void tick() {
		if (capture == null)
			return;

		int pressed = Binds.firstDown(this.minecraft);
		if (awaitingRelease) {
			awaitingRelease = pressed != Binds.NONE;
			return;
		}

		if (pressed == Binds.NONE)
			return;

		boolean clear = pressed == GLFW.GLFW_KEY_BACKSPACE || pressed == GLFW.GLFW_KEY_DELETE;
		capture.setter().accept(clear ? Binds.NONE : pressed);
		capture.refresh();
		capture = null;
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	public void removed() {
		CPvPConfig.save();
		super.removed();
	}

	private void addBindButton(int x, int y, int width, Component name, IntSupplier getter, IntConsumer setter) {
		Button button = Button.builder(Component.translatable("cpvphelper.bind.entry", name, Binds.name(getter.getAsInt())), pressed -> startCapture(pressed, name, getter, setter))
			.bounds(x, y, width, 20)
			.tooltip(Tooltip.create(Component.translatable("cpvphelper.bind.tooltip")))
			.build();
		this.addRenderableWidget(button);
	}

	private void startCapture(Button button, Component name, IntSupplier getter, IntConsumer setter) {
		if (capture != null)
			capture.refresh();

		capture = new Capture(button, name, getter, setter);
		awaitingRelease = true;
		button.setMessage(Component.translatable("cpvphelper.bind.entry", name, Component.translatable("cpvphelper.bind.listening")));
	}

	private static void switchMode(Button button, BindSetting setting) {
		setting.toggle = !setting.toggle;
		button.setMessage(modeLabel(setting));
	}

	private static Component modeLabel(BindSetting setting) {
		return Component.translatable(setting.toggle ? "cpvphelper.bind.mode.toggle" : "cpvphelper.bind.mode.hold");
	}
}
