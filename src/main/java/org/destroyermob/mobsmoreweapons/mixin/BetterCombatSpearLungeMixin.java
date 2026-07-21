package org.destroyermob.mobsmoreweapons.mixin;

import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.fml.ModList;
import org.destroyermob.mobsmoreweapons.MoreWeapons;
import org.destroyermob.mobsmoreweapons.item.SpearItem;
import org.destroyermob.mobsmoreweapons.network.ModNetworking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Restores Lunge when Better Combat owns the spear's normal attack path. */
@Mixin(LocalPlayer.class)
public abstract class BetterCombatSpearLungeMixin {
    @Unique
    private int mobsmoreweapons$previousBetterCombatUpswingTicks;
    @Unique
    private static volatile Method mobsmoreweapons$getUpswingTicks;
    @Unique
    private static volatile boolean mobsmoreweapons$betterCombatApiUnavailable;

    @Inject(method = "tick", at = @At("TAIL"))
    private void mobsmoreweapons$triggerLungeAtUpswingStart(CallbackInfo callback) {
        if (!ModList.get().isLoaded("bettercombat") || mobsmoreweapons$betterCombatApiUnavailable) {
            return;
        }

        LocalPlayer player = (LocalPlayer) (Object) this;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != player) {
            mobsmoreweapons$previousBetterCombatUpswingTicks = 0;
            return;
        }

        int upswingTicks = mobsmoreweapons$readUpswingTicks(minecraft);
        if (upswingTicks > mobsmoreweapons$previousBetterCombatUpswingTicks
                && player.getMainHandItem().getItem() instanceof SpearItem
                && !player.isUsingItem()
                && !player.isHandsBusy()) {
            ModNetworking.sendBetterCombatSpearLunge();
        }
        mobsmoreweapons$previousBetterCombatUpswingTicks = upswingTicks;
    }

    @Unique
    private static int mobsmoreweapons$readUpswingTicks(Minecraft minecraft) {
        try {
            Method method = mobsmoreweapons$getUpswingTicks;
            if (method == null) {
                method = minecraft.getClass().getMethod("getUpswingTicks");
                mobsmoreweapons$getUpswingTicks = method;
            }
            return Math.max(0, ((Number) method.invoke(minecraft)).intValue());
        } catch (ReflectiveOperationException | LinkageError exception) {
            mobsmoreweapons$betterCombatApiUnavailable = true;
            MoreWeapons.LOGGER.warn(
                    "Could not observe Better Combat's upswing; spear Lunge will retain vanilla-only behavior.",
                    exception
            );
            return 0;
        }
    }
}
