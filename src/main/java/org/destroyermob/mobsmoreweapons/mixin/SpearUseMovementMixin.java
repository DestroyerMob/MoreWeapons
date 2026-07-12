package org.destroyermob.mobsmoreweapons.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.destroyermob.mobsmoreweapons.config.MoreWeaponsConfig;
import org.destroyermob.mobsmoreweapons.item.BattleAxeItem;
import org.destroyermob.mobsmoreweapons.item.GreatSwordItem;
import org.destroyermob.mobsmoreweapons.item.SpearItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
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

    @ModifyConstant(method = "aiStep", constant = @Constant(floatValue = 0.2F))
    private float mobsmoreweapons$weaponPreparationMovement(float original) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (!player.isUsingItem()) {
            return original;
        }
        if (player.getUseItem().getItem() instanceof GreatSwordItem) {
            return 0.4F;
        }
        if (player.getUseItem().getItem() instanceof BattleAxeItem) {
            return MoreWeaponsConfig.BATTLE_AXE_HOOK_MOVEMENT_MULTIPLIER.get().floatValue();
        }
        return original;
    }
}
