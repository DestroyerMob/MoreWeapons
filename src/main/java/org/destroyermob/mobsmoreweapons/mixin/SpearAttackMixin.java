package org.destroyermob.mobsmoreweapons.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import org.destroyermob.mobsmoreweapons.item.SpearItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class SpearAttackMixin {
    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isSprinting()Z"))
    private boolean mobsmoreweapons$disableSpearSprintKnockback(Player player) {
        return !SpearItem.isSpear(player.getMainHandItem()) && player.isSprinting();
    }

    @Redirect(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/common/CommonHooks;fireCriticalHit(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;ZF)Lnet/neoforged/neoforge/event/entity/player/CriticalHitEvent;",
                    remap = false
            )
    )
    private CriticalHitEvent mobsmoreweapons$forceSpearNonCritical(Player player, Entity target, boolean vanillaCritical, float multiplier) {
        CriticalHitEvent event = CommonHooks.fireCriticalHit(player, target, vanillaCritical, multiplier);
        if (SpearItem.isSpear(player.getMainHandItem())) {
            event.setCriticalHit(false);
            event.setDamageMultiplier(1.0F);
        }
        return event;
    }

    @Inject(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;resetAttackStrengthTicker()V",
                    shift = At.Shift.AFTER
            )
    )
    private void mobsmoreweapons$pierceWithSpear(Entity primaryTarget, CallbackInfo ci) {
        SpearItem.hitAdditionalJabTargets((Player) (Object) this, primaryTarget);
    }
}
