package com.cpvphelper.mixin;

import com.cpvphelper.combat.SprintState;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
	/** Every attack packet, from a click or from a feature, before the attack charge is reset. */
	@Inject(method = "attack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
	private void cpvphelper$attack(Player player, Entity target, CallbackInfo ci) {
		SprintState.onAttack(player);
	}
}
