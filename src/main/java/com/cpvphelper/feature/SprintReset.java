package com.cpvphelper.feature;

import com.cpvphelper.config.CPvPConfig;
import com.cpvphelper.input.FeatureBind;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Input;

/**
 * Sprint reset after a sprint hit, done with the movement keys only: a w-tap releases the forward key, an s-tap
 * presses the back key instead, for one to three ticks. The client stops sprinting, then sprints again with the
 * forward key, and the server gets the sprint back for the next hit.
 *
 * <p>The sprint comes back on its own only with the sprint key held or toggle sprint on.
 */
public final class SprintReset {
	private static long startTick = -1;
	private static int ticksLeft;

	private SprintReset() {
	}

	/** An attack that spent the sprint on the server. */
	public static void onSprintHit(Entity target) {
		if (!(target instanceof LivingEntity) || !FeatureBind.SPRINT_RESET.isActive())
			return;

		CPvPConfig config = CPvPConfig.get();
		startTick = ClientTicks.get() + config.sprintResetDelay;
		ticksLeft = config.sprintResetTicks;
	}

	/** Movement keys of this tick: true if the tap holds them now. */
	public static boolean consumeTick(Input keys) {
		if (ticksLeft == 0 || ClientTicks.get() < startTick)
			return false;

		if (!keys.forward()) {
			ticksLeft = 0;
			return false;
		}

		ticksLeft--;
		return true;
	}

	public static boolean isBackward() {
		return CPvPConfig.get().sprintResetBackward;
	}
}
