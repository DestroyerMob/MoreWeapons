package org.destroyermob.mobsmoreweapons.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.destroyermob.mobsmoreweapons.item.SpearItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Backports 1.21.11 spear use_effects: full movement speed and sprinting while charging. */
@Mixin(LocalPlayer.class)
public abstract class SpearUseMovementMixin {
    @Redirect(
            method = "aiStep",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z")
    )
    private boolean mobsmoreweapons$allowMovementWhileCharging(LocalPlayer player) {
        return player.isUsingItem() && !SpearItem.isSpear(player.getUseItem());
    }
}
