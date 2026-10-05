package com.cpvphelper.mixin;

import com.cpvphelper.feature.JumpReset;
import com.cpvphelper.feature.MovementStop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
	@Inject(method = "tick", at = @At("TAIL"))
	private void cpvphelper$releaseKeys(CallbackInfo ci) {
		if (MovementStop.consumeTick()) {
			// Same state as with the inventory screen open: every key released, velocity untouched.
			this.keyPresses = Input.EMPTY;
			this.moveVector = Vec2.ZERO;
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && JumpReset.consumeJump(mc.player)) {
			Input keys = this.keyPresses;
			this.keyPresses = new Input(keys.forward(), keys.backward(), keys.left(), keys.right(), true, keys.shift(), keys.sprint());
		}
	}
}
