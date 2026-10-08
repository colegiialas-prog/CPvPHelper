package com.cpvphelper.feature;

import com.cpvphelper.config.CPvPConfig;
import com.cpvphelper.input.FeatureBind;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Jump reset: the jump key is pressed in the very tick the knockback velocity from the server is applied, while the
 * player still stands on the ground. The jump replaces the vertical knockback and the player keeps their ground.
 *
 * <p>The velocity packet is handled before the player tick, so the jump goes out with the movement of that same tick,
 * exactly like a perfectly timed press of the jump key.
 */
public final class JumpReset {
	private static Vec3 velocityBefore;
	private static boolean pending;

	private JumpReset() {
	}

	/** Head of the velocity packet handler on the main thread. */
	public static void beforeVelocity(Minecraft mc) {
		velocityBefore = mc.player == null ? null : mc.player.getDeltaMovement();
	}

	/** End of the velocity packet handler: the server pushed the local player. */
	public static void afterVelocity(Minecraft mc) {
		LocalPlayer player = mc.player;
		Vec3 before = velocityBefore;
		velocityBefore = null;
		if (player == null || before == null || !FeatureBind.JUMP_RESET.isActive())
			return;

		Vec3 velocity = player.getDeltaMovement();
		if (velocity.equals(before) || velocity.horizontalDistanceSqr() < 1.0E-4)
			return;

		if (ThreadLocalRandom.current().nextInt(100) < CPvPConfig.get().jumpResetChance)
			pending = true;
	}

	/** Player input of this tick: true if the jump key has to be pressed. */
	public static boolean consumeJump(LocalPlayer player) {
		if (!pending)
			return false;

		pending = false;
		Minecraft mc = Minecraft.getInstance();
		if (mc.gui.screen() != null || !player.onGround() || player.isInWater() || player.isInLava() || player.isPassenger())
			return false;

		return !player.isUsingItem() || CPvPConfig.get().jumpResetWhileUsing;
	}
}
