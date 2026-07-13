package org.destroyermob.mobsmoreweapons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.destroyermob.mobsmoreweapons.item.SpearItem;
import org.destroyermob.mobsmoreweapons.network.ModNetworking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gives spears vanilla's dedicated stab input instead of routing them through normal attacks. */
@Mixin(value = Minecraft.class, priority = 2000)
public abstract class MinecraftSpearAttackMixin {
    @Shadow
    public LocalPlayer player;

    @Shadow
    private int missTime;

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void mobsmoreweapons$startSpearJab(CallbackInfoReturnable<Boolean> callback) {
        LocalPlayer player = this.player;
        if (player == null || !SpearItem.isSpear(player.getMainHandItem())) {
            return;
        }
        if (missTime > 0 || player.isHandsBusy() || player.isSpectator()
                || player.getAttackStrengthScale(0.0F) < 1.0F) {
            callback.setReturnValue(false);
            return;
        }

        ModNetworking.sendSpearJab();
        player.resetAttackStrengthTicker();
        player.swing(InteractionHand.MAIN_HAND);
        callback.setReturnValue(true);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void mobsmoreweapons$preventSpearMining(boolean attacking, CallbackInfo callback) {
        if (player != null && SpearItem.isSpear(player.getMainHandItem())) {
            callback.cancel();
        }
    }
}
