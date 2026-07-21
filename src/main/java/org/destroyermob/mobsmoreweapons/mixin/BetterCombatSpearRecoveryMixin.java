package org.destroyermob.mobsmoreweapons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.destroyermob.mobsmoreweapons.item.SpearItem;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents Better Combat's completed attack recovery from slowing an active
 * spear charge. Its client movement hook reads this optional mixed-in method;
 * returning a completed swing affects only that recovery multiplier and leaves
 * vanilla sprint eligibility untouched.
 */
@Mixin(value = Minecraft.class, priority = 900)
public abstract class BetterCombatSpearRecoveryMixin {
    @Dynamic("getSwingProgress is supplied by Better Combat's Minecraft mixin")
    @SuppressWarnings("target")
    @Inject(
            method = "getSwingProgress()F",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void mobsmoreweapons$restoreSpearChargeMovement(CallbackInfoReturnable<Float> callback) {
        Minecraft minecraft = (Minecraft) (Object) this;
        LocalPlayer player = minecraft.player;
        if (player != null && player.isUsingItem() && SpearItem.isSpear(player.getUseItem())) {
            callback.setReturnValue(1.0F);
        }
    }
}
