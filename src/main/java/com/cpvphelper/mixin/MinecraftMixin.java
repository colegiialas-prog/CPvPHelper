package com.cpvphelper.mixin;

import com.cpvphelper.feature.AnchorPlace;
import com.cpvphelper.feature.AutoTotem;
import com.cpvphelper.feature.ClientTicks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void cpvphelper$tickStart(CallbackInfo ci) {
		ClientTicks.increment();
		AutoTotem.onTickStart((Minecraft) (Object) this);
	}

	@Inject(method = "handleKeybinds", at = @At("HEAD"))
	private void cpvphelper$keybindsStart(CallbackInfo ci) {
		Minecraft mc = (Minecraft) (Object) this;
		boolean totemSwapped = AutoTotem.onKeybinds(mc);
		AnchorPlace.onKeybinds(mc, totemSwapped);
	}

	/** The right click handled from {@code keyUse.consumeClick()}. */
	@WrapOperation(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;startUseItem()V", ordinal = 0))
	private void cpvphelper$useClick(Minecraft mc, Operation<Void> original) {
		if (!AnchorPlace.onUseClick(mc)) {
			original.call(mc);
		}
	}

	/** The repeated use while the key is held down: paused while an anchor sequence runs. */
	@WrapOperation(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;startUseItem()V", ordinal = 1))
	private void cpvphelper$useHeld(Minecraft mc, Operation<Void> original) {
		if (!AnchorPlace.isActive()) {
			original.call(mc);
		}
	}
}
