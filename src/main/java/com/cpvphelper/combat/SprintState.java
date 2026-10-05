package com.cpvphelper.combat;

import com.cpvphelper.feature.ClientTicks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The sprint state the server works with.
 *
 * <p>A charged sprint hit stops sprinting on the server, while the client keeps running and sends nothing until
 * its own sprint flag flips. Only a w-tap or s-tap sends the sprint again, so the next hit knocks back like a
 * sprint hit. Between those hits the server sees a player that is not sprinting, which is what a critical hit needs.
 */
public final class SprintState {
	private static boolean sent;
	private static boolean spent;
	private static long lastSprintTick = -1;
	private static long lastAttackTick = -1;

	private SprintState() {
	}

	/** End of the client tick: the sprint flag of this tick has been sent with the movement. */
	public static void onTickEnd(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null) {
			reset();
			return;
		}

		boolean sprinting = player.isSprinting();
		if (sprinting)
			lastSprintTick = ClientTicks.get();

		if (sprinting == sent)
			return;

		sent = sprinting;
		spent = false;
	}

	/** Head of every attack sent to the server, before it resets the attack charge. */
	public static void onAttack(Player player) {
		lastAttackTick = ClientTicks.get();
		if (sent && player.getAttackStrengthScale(0.5F) > 0.9F)
			spent = true;
	}

	public static boolean isServerSprinting() {
		return sent && !spent;
	}

	/** Sprinting stopped after the last hit and has not reached the server again yet: a w-tap or s-tap is under way. */
	public static boolean isResetting(LocalPlayer player) {
		return !isServerSprinting() && !player.isSprinting() && lastSprintTick > lastAttackTick;
	}

	private static void reset() {
		sent = false;
		spent = false;
		lastSprintTick = -1;
		lastAttackTick = -1;
	}
}
