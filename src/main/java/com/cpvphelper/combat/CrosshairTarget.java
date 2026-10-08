package com.cpvphelper.combat;

import java.util.Optional;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The entity under the crosshair, picked like vanilla picks it, with the rotation of the current frame and the
 * position the server knows. A block in front of the entity hides it.
 */
public final class CrosshairTarget {
	private CrosshairTarget() {
	}

	public static EntityHitResult pick(LocalPlayer player, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 reach = player.getViewVector(1.0F).scale(range);
		Vec3 end = eye.add(reach);
		HitResult block = player.pick(range, 1.0F, false);
		AABB area = player.getBoundingBox().expandTowards(reach).inflate(1.0);

		Entity closest = null;
		Vec3 closestPoint = null;
		double closestDistance = block.getType() == HitResult.Type.MISS ? range * range : eye.distanceToSqr(block.getLocation());
		for (Entity entity : player.level().getEntities(player, area, CrosshairTarget::isPickable)) {
			if (entity.getRootVehicle() == player.getRootVehicle())
				continue;

			AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
			if (box.contains(eye))
				return new EntityHitResult(entity, eye);

			Optional<Vec3> point = box.clip(eye, end);
			if (point.isEmpty())
				continue;

			double distance = eye.distanceToSqr(point.get());
			if (distance >= closestDistance)
				continue;

			closest = entity;
			closestPoint = point.get();
			closestDistance = distance;
		}

		return closest == null ? null : new EntityHitResult(closest, closestPoint);
	}

	private static boolean isPickable(Entity entity) {
		return !entity.isSpectator() && entity.isPickable();
	}
}
