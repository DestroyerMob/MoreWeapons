package org.destroyermob.mobsmoreweapons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.destroyermob.mobsmoreweapons.MoreWeapons;
import org.destroyermob.mobsmoreweapons.item.SpearItem;
import org.destroyermob.mobsmoreweapons.network.ModNetworking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives spears vanilla's dedicated stab input when another combat system has
 * not already claimed the attack. Better Combat injects at the method head;
 * placing this after the held-stack lookup intentionally lets it win first.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftSpearAttackMixin {
    @Shadow
    public LocalPlayer player;

    @Shadow
    private int missTime;

    @Inject(
            method = "startAttack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;",
                    shift = Shift.AFTER
            ),
            cancellable = true
    )
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
        MoreWeapons.LOGGER.debug("Sent spear jab input for {}", player.getScoreboardName());
        player.resetAttackStrengthTicker();
        player.swing(InteractionHand.MAIN_HAND);
        callback.setReturnValue(true);
    }
}
