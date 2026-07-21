package org.destroyermob.mobsmoreweapons.mixin;

import net.minecraft.world.item.ItemStack;
import org.destroyermob.mobsmoreweapons.item.SpearItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hands More Weapons spears back to Minecraft's renderer so the exact
 * backported vanilla spear transforms can run when Punchy is installed.
 */
@Pseudo
@Mixin(targets = "punchy.config.PunchyConfig", remap = false)
public abstract class PunchySpearCompatibilityMixin {
    @Inject(
            method = "isItemBlacklisted(Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private static void mobsmoreweapons$useVanillaSpearRenderer(
            ItemStack stack,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (stack != null && stack.getItem() instanceof SpearItem) {
            callback.setReturnValue(true);
        }
    }
}
