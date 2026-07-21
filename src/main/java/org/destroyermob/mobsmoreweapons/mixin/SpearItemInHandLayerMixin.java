package org.destroyermob.mobsmoreweapons.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.destroyermob.mobsmoreweapons.client.SpearChargeAnimation;
import org.destroyermob.mobsmoreweapons.item.SpearItem;
import org.destroyermob.mobsmoreweapons.item.SpearUser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Third-person item transforms from the vanilla spear backport. */
@Mixin(ItemInHandLayer.class)
public abstract class SpearItemInHandLayerMixin {
    @Inject(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            )
    )
    private void mobsmoreweapons$animateSpearItem(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext,
            HumanoidArm arm,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int light,
            CallbackInfo callback
    ) {
        if (!(stack.getItem() instanceof SpearItem spear)) {
            return;
        }

        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        if (entity.attackAnim > 0.0F && entity.getMainArm() == arm) {
            float swing = entity.isUsingItem() ? 1.0F : entity.getAttackAnim(partialTick);
            float thrust = clampedInverseLerp(swing, 0.05F, 0.2F);
            thrust *= thrust;
            float recovery = exponentialEase(clampedInverseLerp(swing, 0.4F, 1.0F));
            poseStack.rotateAround(Axis.XN.rotationDegrees(70.0F * (thrust - recovery)), 0.0F, -0.125F, 0.125F);
            poseStack.translate(0.0F, spear.chargeForwardMovement() * (thrust - recovery), 0.0F);
        }

        HumanoidArm usedArm = entity.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                ? entity.getMainArm()
                : entity.getMainArm().getOpposite();
        if (!entity.isUsingItem() || usedArm != arm || !(entity.getUseItem().getItem() instanceof SpearItem)) {
            return;
        }

        float elapsed = stack.getUseDuration(entity) - (entity.getUseItemRemainingTicks() - partialTick + 1.0F);
        if (elapsed == 0.0F) {
            return;
        }

        SpearChargeAnimation animation = SpearChargeAnimation.play(spear, elapsed);
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float raise = -animation.raiseProgress();
        raise = 1.0F - (1.0F + 2.70158F * raise * raise * raise + 1.70158F * Mth.square(raise));
        float recoil = recoil(entity, partialTick) * 0.4F;
        poseStack.translate(
                0.0F,
                -recoil * 0.4F,
                -spear.chargeForwardMovement() * (raise - animation.raiseBackProgress()) + recoil
        );
        poseStack.rotateAround(
                Axis.XN.rotationDegrees(animation.raiseProgress() * 70.0F - animation.raiseBackProgress() * 70.0F),
                0.0F,
                -0.03125F,
                0.125F
        );
        poseStack.rotateAround(
                Axis.YP.rotationDegrees(animation.raiseProgress() * side * 90.0F - animation.swayProgress() * side * 90.0F),
                0.0F,
                0.0F,
                0.125F
        );
    }

    private static float recoil(LivingEntity entity, float partialTick) {
        float elapsed = ((SpearUser) entity).mobsmoreweapons$timeSinceLastSpearImpact(partialTick);
        float recoil = 1.0F - Mth.square(Mth.square(1.0F - clampedInverseLerp(elapsed, 1.0F, 3.0F)))
                + (Mth.cos((float) Math.PI * clampedInverseLerp(elapsed, 3.0F, 10.0F)) - 1.0F) / 2.0F;
        return recoil >= 10.0F ? 0.0F : recoil;
    }

    private static float clampedInverseLerp(float value, float start, float end) {
        return Mth.clamp(Mth.inverseLerp(value, start, end), 0.0F, 1.0F);
    }

    private static float exponentialEase(float value) {
        if (value < 0.5F) {
            return value == 0.0F ? 0.0F : (float) (Math.pow(2.0D, 20.0D * value - 10.0D) / 2.0D);
        }
        return value == 1.0F ? 1.0F : (float) ((2.0D - Math.pow(2.0D, -20.0D * value + 10.0D)) / 2.0D);
    }
}
