package org.destroyermob.mobsmoreweapons.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import org.destroyermob.mobsmoreweapons.config.MoreWeaponsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Projectile.class)
public abstract class ProjectileAccuracyMixin {
    @ModifyVariable(method = "getMovementToShoot(DDDFF)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private float mobsmoreweapons$removePlayerArrowSpread(float inaccuracy) {
        Projectile projectile = (Projectile) (Object) this;
        Entity owner = projectile.getOwner();
        if (projectile instanceof AbstractArrow && owner instanceof Player player && shouldSteadyBowShot(player)) {
            return 0.0F;
        }
        return inaccuracy;
    }

    private static boolean shouldSteadyBowShot(Player player) {
        if (!MoreWeaponsConfig.PLAYER_BOW_ACCURACY_FIX.get() || !player.isUsingItem() || !(player.getUseItem().getItem() instanceof BowItem)) {
            return false;
        }

        int strainThreshold = BowItem.MAX_DRAW_DURATION + MoreWeaponsConfig.BOW_STRAIN_GRACE_TICKS.get();
        return player.getTicksUsingItem() <= strainThreshold;
    }
}
