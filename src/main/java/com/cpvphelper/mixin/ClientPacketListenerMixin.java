package com.cpvphelper.mixin;

import com.cpvphelper.feature.JumpReset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	/** The handler first runs on the network thread and is rescheduled; only the main thread run applies the velocity. */
	@Inject(method = "handleSetEntityMotion", at = @At("HEAD"))
	private void cpvphelper$velocityStart(CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.isSameThread())
			JumpReset.beforeVelocity(mc);
	}

	@Inject(method = "handleSetEntityMotion", at = @At("TAIL"))
	private void cpvphelper$velocityEnd(CallbackInfo ci) {
		JumpReset.afterVelocity(Minecraft.getInstance());
	}
}
