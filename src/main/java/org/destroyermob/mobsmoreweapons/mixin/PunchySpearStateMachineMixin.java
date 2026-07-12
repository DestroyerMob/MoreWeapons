package org.destroyermob.mobsmoreweapons.mixin;

import net.minecraft.world.item.ItemStack;
import org.destroyermob.mobsmoreweapons.item.KnifeItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps Punchy's spear charger from claiming knives before its throw state machine can. */
@Pseudo
@Mixin(targets = "punchy.client.state.SpearStateMachine", remap = false)
public abstract class PunchySpearStateMachineMixin {
    @Inject(
            method = "isSpearStack(Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private static void mobsmoreweapons$excludeThrowableKnives(
            ItemStack stack,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (stack.getItem() instanceof KnifeItem) {
            callback.setReturnValue(false);
        }
    }
}
