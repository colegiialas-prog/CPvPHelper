package com.cpvphelper.mixin;

import com.cpvphelper.feature.AnchorPlace;
import com.cpvphelper.feature.AutoTotem;
import com.cpvphelper.feature.ClientTicks;
import com.cpvphelper.feature.StunSlam;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void cpvphelper$tickStart(CallbackInfo ci) {
		ClientTicks.increment();
		AutoTotem.onTickStart((Minecraft) (Object) this);
	}

	@Inject(method = "handleKeybinds", at = @At("HEAD"))
	private void cpvphelper$keybindsStart(CallbackInfo ci) {
		Minecraft mc = (Minecraft) (Object) this;
		boolean totemSwapped = AutoTotem.onKeybinds(mc);
		AnchorPlace.onKeybinds(mc, totemSwapped);
		StunSlam.onKeybinds(mc);
	}

	/** The attack of a left click on an entity: an axe hit may be followed by a mace hit. */
	@WrapOperation(
		method = "startAttack",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;attack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V"
		)
	)
	private void cpvphelper$attack(MultiPlayerGameMode gameMode, Player player, Entity target, Operation<Void> original) {
		Minecraft mc = (Minecraft) (Object) this;
		// Checked before the axe hit, while the target is still blocking.
		boolean slam = StunSlam.shouldSlam(mc, target);
		original.call(gameMode, player, target);
		if (slam) {
			StunSlam.onAxeHit(mc, target);
		}
	}

	/** After the axe hit and its swing: the mace hit without delay goes out in the same tick. */
	@Inject(method = "startAttack", at = @At("RETURN"))
	private void cpvphelper$attackEnd(CallbackInfoReturnable<Boolean> cir) {
		StunSlam.afterAttack((Minecraft) (Object) this);
	}

	/** The right click handled from {@code keyUse.consumeClick()}. */
	@WrapOperation(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;startUseItem()V", ordinal = 0))
	private void cpvphelper$useClick(Minecraft mc, Operation<Void> original) {
		if (!AnchorPlace.onUseClick(mc)) {
			original.call(mc);
		}
	}

	/** The repeated use while the key is held down: paused while an anchor sequence runs. */
	@WrapOperation(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;startUseItem()V", ordinal = 1))
	private void cpvphelper$useHeld(Minecraft mc, Operation<Void> original) {
		if (!AnchorPlace.isActive()) {
			original.call(mc);
		}
	}
}
