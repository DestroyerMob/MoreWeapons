package org.destroyermob.mobsmoreweapons.mixin;

import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.destroyermob.mobsmoreweapons.MoreWeapons;
import org.destroyermob.mobsmoreweapons.item.SpearItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets a spear's normal right-click use interrupt Better Combat's pending
 * upswing and bypass its attack-recovery-only item-use veto.
 *
 * <p>The hook is placed on Better Combat's real attack-selection helper
 * rather than its mixin class. Returning no selected attack makes
 * Better Combat's {@code pre_doItemUse} handler leave vanilla item use alone,
 * while normal left-click selection remains unchanged.</p>
 */
@Pseudo
@Mixin(targets = "net.bettercombat.logic.PlayerAttackHelper", remap = false)
public abstract class BetterCombatSpearUseMixin {
    @Unique
    private static volatile Method mobsmoreweapons$getUpswingTicks;
    @Unique
    private static volatile Method mobsmoreweapons$cancelUpswing;
    @Unique
    private static volatile boolean mobsmoreweapons$betterCombatApiUnavailable;

    @Inject(
            method = "getCurrentAttack(Lnet/minecraft/world/entity/player/Player;I)Lnet/bettercombat/api/AttackHand;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private static void mobsmoreweapons$allowSpearUseDuringAttackRecovery(
            Player player,
            int comboCount,
            CallbackInfoReturnable<Object> callback
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(player instanceof LocalPlayer localPlayer)
                || player != minecraft.player
                || !minecraft.options.keyUse.isDown()
                || localPlayer.isUsingItem()
                || localPlayer.isHandsBusy()
                || minecraft.gameMode == null
                || minecraft.gameMode.isDestroying()
                || (!(localPlayer.getMainHandItem().getItem() instanceof SpearItem)
                && !(localPlayer.getOffhandItem().getItem() instanceof SpearItem))) {
            return;
        }

        // A pending upswing must be feinted before item use continues; merely
        // bypassing the veto would otherwise let the attack land mid-charge.
        if (mobsmoreweapons$cancelPendingUpswing(minecraft)) {
            callback.setReturnValue(null);
        }
    }

    @Unique
    private static boolean mobsmoreweapons$cancelPendingUpswing(Minecraft minecraft) {
        if (mobsmoreweapons$betterCombatApiUnavailable) {
            return false;
        }

        try {
            Method getUpswingTicks = mobsmoreweapons$getUpswingTicks;
            Method cancelUpswing = mobsmoreweapons$cancelUpswing;
            if (getUpswingTicks == null || cancelUpswing == null) {
                getUpswingTicks = minecraft.getClass().getMethod("getUpswingTicks");
                cancelUpswing = minecraft.getClass().getMethod("cancelUpswing");
                mobsmoreweapons$getUpswingTicks = getUpswingTicks;
                mobsmoreweapons$cancelUpswing = cancelUpswing;
            }

            if (((Number) getUpswingTicks.invoke(minecraft)).intValue() > 0) {
                cancelUpswing.invoke(minecraft);
            }
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            mobsmoreweapons$betterCombatApiUnavailable = true;
            MoreWeapons.LOGGER.warn(
                    "Could not access Better Combat's swing controller; retaining its normal spear-use timing.",
                    exception
            );
            return false;
        }
    }
}
