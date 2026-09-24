package com.cpvphelper.feature;

/**
 * Releases the movement keys the way vanilla does while the inventory screen is open.
 *
 * <p>Only the key state is touched, velocity never is: on the ground the player slows down through
 * normal friction, in the air the momentum is kept exactly as vanilla physics dictates.
 */
public final class MovementStop {
	private static int ticksLeft;
	private static boolean active;

	private MovementStop() {
	}

	public static void request(int ticks) {
		ticksLeft = Math.max(ticksLeft, ticks);
		active = true;
	}

	/** True once every requested tick has been sent to the server with released keys. */
	public static boolean isDone() {
		return active && ticksLeft == 0;
	}

	public static void reset() {
		ticksLeft = 0;
		active = false;
	}

	/** Called once per player tick right after the keyboard state was read. */
	public static boolean consumeTick() {
		if (ticksLeft > 0) {
			ticksLeft--;
			return true;
		}

		return false;
	}
}
