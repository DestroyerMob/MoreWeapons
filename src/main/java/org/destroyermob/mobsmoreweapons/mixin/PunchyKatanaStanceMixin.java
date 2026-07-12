package org.destroyermob.mobsmoreweapons.mixin;

import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import org.destroyermob.mobsmoreweapons.item.KatanaItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Uses Punchy's held sword-ready animation while a katana is in its Iaijutsu stance. */
@Pseudo
@Mixin(targets = "punchy.client.state.UseItemStateMachine", remap = false)
public abstract class PunchyKatanaStanceMixin {
    private static Method mobsmoreweapons$originalBlockingCheck;

    @Redirect(
            method = "resolveUseSelection(Lnet/minecraft/client/Minecraft;Lnet/minecraft/world/InteractionHand;Lpunchy/client/state/UseActionTracker$TriggerType;Lnet/minecraft/world/level/block/state/BlockState;Z)Lpunchy/client/state/UseItemStateMachine$UseSelection;",
            at = @At(
                    value = "INVOKE",
                    target = "Lpunchy/compat/SwordBlockingMechanicsCompat;shouldUseBlockingAnimation(Lnet/minecraft/client/Minecraft;Lnet/minecraft/world/InteractionHand;)Z"
            ),
            remap = false,
            require = 0
    )
    private boolean mobsmoreweapons$animateKatanaStance(Minecraft minecraft, InteractionHand hand) {
        if (minecraft.player != null
                && minecraft.player.getItemInHand(hand).getItem() instanceof KatanaItem
                && minecraft.player.isUsingItem()
                && minecraft.player.getUsedItemHand() == hand) {
            return true;
        }
        try {
            if (mobsmoreweapons$originalBlockingCheck == null) {
                Class<?> compat = Class.forName("punchy.compat.SwordBlockingMechanicsCompat");
                mobsmoreweapons$originalBlockingCheck = compat.getMethod(
                        "shouldUseBlockingAnimation",
                        Minecraft.class,
                        InteractionHand.class
                );
            }
            return (boolean) mobsmoreweapons$originalBlockingCheck.invoke(null, minecraft, hand);
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }
}
