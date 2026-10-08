package com.cpvphelper.mixin;

import com.cpvphelper.feature.JumpReset;
import com.cpvphelper.feature.MovementStop;
import com.cpvphelper.feature.SprintReset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
	@Inject(method = "tick", at = @At("TAIL"))
	private void cpvphelper$adjustKeys(CallbackInfo ci) {
		if (MovementStop.consumeTick()) {
			// Same state as with the inventory screen open: every key released, velocity untouched.
			this.keyPresses = Input.EMPTY;
			this.moveVector = Vec2.ZERO;
			return;
		}

		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}

		Input keys = this.keyPresses;
		boolean tap = SprintReset.consumeTick(keys);
		boolean jump = JumpReset.consumeJump(player);
		if (!tap && !jump) {
			return;
		}

		boolean forward = keys.forward() && !tap;
		boolean backward = keys.backward() || (tap && SprintReset.isBackward());
		this.keyPresses = new Input(forward, backward, keys.left(), keys.right(), keys.jump() || jump, keys.shift(), keys.sprint());
		if (tap) {
			this.moveVector = cpvphelper$moveVector(forward, backward, keys.left(), keys.right());
		}
	}

	private static Vec2 cpvphelper$moveVector(boolean forward, boolean backward, boolean left, boolean right) {
		float strafe = (left ? 1.0F : 0.0F) - (right ? 1.0F : 0.0F);
		float walk = (forward ? 1.0F : 0.0F) - (backward ? 1.0F : 0.0F);
		float length = Mth.sqrt(strafe * strafe + walk * walk);
		return length > 1.0F ? new Vec2(strafe / length, walk / length) : new Vec2(strafe, walk);
	}
}
