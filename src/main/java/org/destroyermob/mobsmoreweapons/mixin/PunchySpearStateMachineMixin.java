package org.destroyermob.mobsmoreweapons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.destroyermob.mobsmoreweapons.item.KnifeItem;
import org.destroyermob.mobsmoreweapons.item.SpearItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
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

    @ModifyConstant(
            method = "resolveChargeInfo(Lnet/minecraft/client/Minecraft;Lnet/minecraft/world/item/ItemStack;)Lpunchy/client/state/SpearStateMachine$ChargeInfo;",
            constant = @Constant(intValue = 10),
            remap = false,
            require = 0
    )
    private static int mobsmoreweapons$engagedPhaseEnd(int original) {
        SpearItem spear = activeSpear();
        return spear == null ? original : spear.engagedEndTicks();
    }

    @ModifyConstant(
            method = "resolveChargeInfo(Lnet/minecraft/client/Minecraft;Lnet/minecraft/world/item/ItemStack;)Lpunchy/client/state/SpearStateMachine$ChargeInfo;",
            constant = @Constant(intValue = 20),
            remap = false,
            require = 0
    )
    private static int mobsmoreweapons$tiredPhaseEnd(int original) {
        SpearItem spear = activeSpear();
        return spear == null ? original : spear.tiredEndTicks();
    }

    @ModifyConstant(
            method = "resolveChargeInfo(Lnet/minecraft/client/Minecraft;Lnet/minecraft/world/item/ItemStack;)Lpunchy/client/state/SpearStateMachine$ChargeInfo;",
            constant = @Constant(intValue = 30),
            remap = false,
            require = 0
    )
    private static int mobsmoreweapons$disengagedPhaseEnd(int original) {
        SpearItem spear = activeSpear();
        return spear == null ? original : spear.disengagedEndTicks();
    }

    private static SpearItem activeSpear() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }
        return minecraft.player.getUseItem().getItem() instanceof SpearItem spear ? spear : null;
    }
}
