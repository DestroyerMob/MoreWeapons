package org.destroyermob.mobsmoreweapons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.destroyermob.mobsmoreweapons.item.GreatSwordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Keeps Punchy's alternating attack direction alive across a greatsword's slow recharge. */
@Pseudo
@Mixin(targets = "punchy.client.state.AttackStateMachine", remap = false)
public abstract class PunchyGreatSwordComboMixin {
    private static final long VANILLA_COMBO_RESET_TICKS = 30L;
    private static final long GREAT_SWORD_COMBO_GRACE_TICKS = 20L;

    @ModifyConstant(
            method = "nextComboIndex(JI)I",
            constant = @Constant(longValue = VANILLA_COMBO_RESET_TICKS),
            remap = false,
            require = 0
    )
    private long mobsmoreweapons$extendGreatSwordComboWindow(long original) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || (!(minecraft.player.getMainHandItem().getItem() instanceof GreatSwordItem)
                && !(minecraft.player.getOffhandItem().getItem() instanceof GreatSwordItem))) {
            return original;
        }

        double attackSpeed = minecraft.player.getAttributeValue(Attributes.ATTACK_SPEED);
        if (attackSpeed <= 0.0D) {
            return original;
        }

        long fullRechargeTicks = (long) Math.ceil(20.0D / attackSpeed);
        return Math.max(original, fullRechargeTicks + GREAT_SWORD_COMBO_GRACE_TICKS);
    }
}
