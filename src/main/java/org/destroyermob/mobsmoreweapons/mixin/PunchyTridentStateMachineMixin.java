package org.destroyermob.mobsmoreweapons.mixin;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.destroyermob.mobsmoreweapons.item.KnifeItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Routes knives through Punchy's charge-and-throw animation sequence. */
@Pseudo
@Mixin(targets = "punchy.client.state.TridentStateMachine", remap = false)
public abstract class PunchyTridentStateMachineMixin {
    @Redirect(
            method = "tick(Lnet/minecraft/client/Minecraft;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;"
            ),
            remap = false,
            require = 0
    )
    private Item mobsmoreweapons$treatKnifeAsThrowable(ItemStack stack) {
        return stack.getItem() instanceof KnifeItem ? Items.TRIDENT : stack.getItem();
    }
}
