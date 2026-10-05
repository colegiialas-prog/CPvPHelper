package com.cpvphelper.feature;

import com.cpvphelper.combat.CriticalHit;
import com.cpvphelper.combat.CrosshairTarget;
import com.cpvphelper.combat.SprintState;
import com.cpvphelper.config.CPvPConfig;
import com.cpvphelper.mixin.MinecraftAccessor;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Attacks the entity under the crosshair on the first frame the hit is worth it instead of the next client tick.
 * The rotation changes every frame, so the hit leaves the moment the aim lands on the target.
 *
 * <p>The hit is a regular vanilla left click on that entity with every client check of a click. A ready hit may wait
 * in a jump for the fall that makes it critical, or for the sprint to come back during a w-tap or s-tap. Every hit
 * rolls its own reaction delay and charge threshold.
 */
public final class TriggerBot {
	private static KeyMapping holdKey;
	private static Entity target;
	private static long readyTick = -1;
	private static long fireTime;
	private static boolean scheduled;
	private static float chargeThreshold = 1.0F;

	private TriggerBot() {
	}

	public static void init(KeyMapping mapping) {
		holdKey = mapping;
	}

	/** Head of every frame, before its client ticks. */
	public static void onFrame(Minecraft mc) {
		CPvPConfig config = CPvPConfig.get();
		LocalPlayer player = mc.player;
		if (!config.triggerBotEnabled || player == null || mc.level == null || mc.gameMode == null || !canAttack(mc, player, config)) {
			reset();
			return;
		}

		EntityHitResult hit = CrosshairTarget.pick(player);
		if (hit == null || !isTarget(hit.getEntity(), config) || !isInRange(player, hit)) {
			reset();
			return;
		}

		if (hit.getEntity() != target) {
			reset();
			target = hit.getEntity();
		}

		if (!isCharged(player)) {
			clearSchedule();
			return;
		}

		long tick = ClientTicks.get();
		if (readyTick == -1)
			readyTick = tick;

		if (shouldHold(player, config, tick - readyTick))
			return;

		long now = System.nanoTime();
		if (!scheduled) {
			fireTime = now + rollDelay(config);
			scheduled = true;
		}

		if (now < fireTime)
			return;

		attack(mc, hit);
	}

	private static boolean canAttack(Minecraft mc, LocalPlayer player, CPvPConfig config) {
		if (mc.gui.screen() != null || mc.gui.overlay() != null || !mc.isWindowActive() || mc.isPaused())
			return false;

		if (!player.isAlive() || player.isSpectator() || player.isUsingItem() || player.isHandsBusy() || mc.gameMode.isDestroying())
			return false;

		if (config.triggerHoldKey && (holdKey == null || !holdKey.isDown()))
			return false;

		if (AnchorPlace.isActive() || StunSlam.isPending() || ((MinecraftAccessor) mc).cpvphelper$getMissTime() > 0)
			return false;

		return !config.triggerWeaponOnly || isWeapon(player.getMainHandItem());
	}

	private static boolean isWeapon(ItemStack stack) {
		return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(ItemTags.SPEARS) || stack.is(Items.MACE) || stack.is(Items.TRIDENT);
	}

	private static boolean isTarget(Entity entity, CPvPConfig config) {
		if (!entity.isAlive())
			return false;

		if (entity instanceof Player)
			return true;

		return !config.triggerPlayersOnly && entity instanceof Mob;
	}

	private static boolean isInRange(LocalPlayer player, EntityHitResult hit) {
		AttackRange range = player.getMainHandItem().get(DataComponents.ATTACK_RANGE);
		return range == null || range.isInRange(player, hit.getLocation());
	}

	private static boolean isCharged(LocalPlayer player) {
		return !player.cannotAttackWithItem(player.getMainHandItem(), 0) && player.getAttackStrengthScale(0.5F) >= chargeThreshold;
	}

	private static boolean shouldHold(LocalPlayer player, CPvPConfig config, long heldTicks) {
		if (heldTicks < config.triggerCritWait && CriticalHit.isComing(player))
			return true;

		return heldTicks < config.triggerSprintWait && SprintState.isResetting(player);
	}

	private static void attack(Minecraft mc, EntityHitResult hit) {
		HitResult crosshair = mc.hitResult;
		mc.hitResult = hit;
		((MinecraftAccessor) mc).cpvphelper$startAttack();
		mc.hitResult = crosshair;
		chargeThreshold = rollCharge(CPvPConfig.get());
		clearSchedule();
	}

	private static long rollDelay(CPvPConfig config) {
		int min = Math.min(config.triggerDelayMin, config.triggerDelayMax);
		int max = Math.max(config.triggerDelayMin, config.triggerDelayMax);
		ThreadLocalRandom random = ThreadLocalRandom.current();
		double spread = (random.nextDouble() + random.nextDouble()) * 0.5;
		return Math.round((min + (max - min) * spread) * 1_000_000.0);
	}

	private static float rollCharge(CPvPConfig config) {
		float lowest = config.triggerCharge / 100.0F;
		return lowest + (1.0F - lowest) * ThreadLocalRandom.current().nextFloat();
	}

	private static void clearSchedule() {
		readyTick = -1;
		scheduled = false;
	}

	private static void reset() {
		target = null;
		clearSchedule();
	}
}
