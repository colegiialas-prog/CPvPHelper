package com.cpvphelper.feature;

public final class ClientTicks {
	private static long ticks;

	private ClientTicks() {
	}

	public static void increment() {
		ticks++;
	}

	public static long get() {
		return ticks;
	}
}
