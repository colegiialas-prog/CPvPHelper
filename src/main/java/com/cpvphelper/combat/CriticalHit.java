package com.cpvphelper.combat;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** The vanilla critical hit conditions, judged with the sprint state the server has. */
public final class CriticalHit {
	private CriticalHit() {
	}

	/** A hit sent now lands as a critical hit. */
	public static boolean isReady(Player player) {
		return player.getAttackStrengthScale(0.5F) > 0.9F && player.fallDistance > 0 && isAirborne(player);
	}

	/** Not critical yet, but becomes critical in this jump: the fall has not started or the charge is still low. */
	public static boolean isComing(Player player) {
		if (isReady(player) || !isAirborne(player))
			return false;

		return !player.isFallFlying() && !player.hasEffect(MobEffects.LEVITATION) && !player.hasEffect(MobEffects.SLOW_FALLING);
	}

	private static boolean isAirborne(Player player) {
		if (player.onGround() || player.onClimbable() || player.isInWater() || player.isPassenger())
			return false;

		return !player.hasEffect(MobEffects.BLINDNESS) && !SprintState.isServerSprinting();
	}
}
