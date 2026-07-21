package org.destroyermob.mobsmoreweapons.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.destroyermob.mobsmoreweapons.client.SpearChargeAnimation;
import org.destroyermob.mobsmoreweapons.MoreWeapons;
import org.destroyermob.mobsmoreweapons.item.SpearItem;
import org.destroyermob.mobsmoreweapons.item.SpearUser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** First-person vanilla spear hold, sway, recoil, and stab transforms. */
@Mixin(ItemInHandRenderer.class)
public abstract class SpearItemInHandRendererMixin {
    @Unique
    private int mobsmoreweapons$lastChargeAnimationLogTick = Integer.MIN_VALUE;

    @WrapOperation(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V",
                    ordinal = 12
            )
    )
    private void mobsmoreweapons$suppressNormalSpearSwing(
            PoseStack poseStack,
            float x,
            float y,
            float z,
            Operation<Void> original,
            @Local(argsOnly = true) ItemStack stack
    ) {
        if (!(stack.getItem() instanceof SpearItem)) {
            original.call(poseStack, x, y, z);
        }
    }

    @WrapOperation(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V",
                    ordinal = 8
            )
    )
    private void mobsmoreweapons$suppressSpearSwapDuringStab(
            ItemInHandRenderer renderer,
            PoseStack poseStack,
            HumanoidArm arm,
            float equipProgress,
            Operation<Void> original,
            @Local(argsOnly = true) AbstractClientPlayer player,
            @Local(argsOnly = true) ItemStack stack
    ) {
        original.call(renderer, poseStack, arm,
                stack.getItem() instanceof SpearItem && player.attackAnim > 0.0F ? 0.0F : equipProgress);
    }

    @WrapOperation(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmAttackTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V"
            )
    )
    private void mobsmoreweapons$applySpearStab(
            ItemInHandRenderer renderer,
            PoseStack poseStack,
            HumanoidArm arm,
            float swingProgress,
            Operation<Void> original,
            @Local(argsOnly = true) ItemStack stack
    ) {
        if (!(stack.getItem() instanceof SpearItem)) {
            original.call(renderer, poseStack, arm, swingProgress);
            return;
        }

        float thrustStart = -(Mth.cos((float) Math.PI * clampedInverseLerp(swingProgress, 0.0F, 0.05F)) - 1.0F) / 2.0F;
        float thrust = clampedInverseLerp(swingProgress, 0.05F, 0.2F);
        thrust *= thrust;
        float recovery = exponentialEase(clampedInverseLerp(swingProgress, 0.4F, 1.0F));
        poseStack.translate(
                recovery * 0.1F * (thrustStart - thrust),
                -0.075F * (thrustStart - recovery),
                0.65F * (thrustStart - thrust)
        );
        poseStack.mulPose(Axis.XP.rotationDegrees(-70.0F * (thrustStart - recovery)));
        poseStack.translate(0.0F, 0.0F, -0.25F * (recovery - thrust));
    }

    @Inject(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getUseAnimation()Lnet/minecraft/world/item/UseAnim;"
            )
    )
    private void mobsmoreweapons$applySpearCharge(
            AbstractClientPlayer player,
            float partialTick,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equipProgress,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int light,
            CallbackInfo callback,
            @Share("mobsmoreweapons$suppressSpearEquip") LocalBooleanRef suppressEquip
    ) {
        if (player.getUsedItemHand() != hand || !(stack.getItem() instanceof SpearItem spear)) {
            return;
        }

        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate(side * 0.56F, -0.52F, -0.72F);
        float elapsed = stack.getUseDuration(player) - (player.getUseItemRemainingTicks() - partialTick + 1.0F);
        SpearChargeAnimation animation = SpearChargeAnimation.play(spear, elapsed);
        if (player.tickCount % 20 == 0 && mobsmoreweapons$lastChargeAnimationLogTick != player.tickCount) {
            mobsmoreweapons$lastChargeAnimationLogTick = player.tickCount;
            MoreWeapons.LOGGER.debug(
                    "Spear first-person animation: elapsed={}, raise={}, sway={}, lower={}",
                    elapsed, animation.raiseProgress(), animation.swayProgress(), animation.lowerProgress()
            );
        }
        poseStack.translate(
                side * (animation.raiseProgress() * 0.15F
                        - animation.raiseProgressEnd() * 0.05F
                        - animation.swayProgress() * 0.1F
                        + animation.swayScaleSlow() * 0.005F),
                -animation.raiseProgress() * 0.075F
                        + animation.raiseProgressMiddle() * 0.075F
                        + animation.swayScaleFast() * 0.01F,
                animation.raiseProgressStart() * 0.05F
                        - animation.raiseProgressEnd() * 0.05F
                        + animation.swayScaleSlow() * 0.005F
        );

        float easedRaise = animation.raiseProgress();
        if (easedRaise < 0.5F) {
            easedRaise = 4.0F * easedRaise * easedRaise * (7.189819F * easedRaise - 2.5949094F) / 2.0F;
        } else {
            float shifted = 2.0F * easedRaise - 2.0F;
            easedRaise = (shifted * shifted * (3.5949094F * shifted + 2.5949094F) + 2.0F) / 2.0F;
        }

        poseStack.rotateAround(
                Axis.XP.rotationDegrees(
                        -65.0F * easedRaise
                                - 35.0F * (1.0F - animation.lowerProgress())
                                + 100.0F * animation.raiseBackProgress()
                                - 0.5F * animation.swayScaleFast()
                ),
                0.0F,
                0.1F,
                0.0F
        );
        poseStack.rotateAround(
                Axis.YN.rotationDegrees(side * (
                        -90.0F * clampedInverseLerp(animation.raiseProgress(), 0.5F, 0.55F)
                                + 90.0F * animation.swayProgress()
                                + 2.0F * animation.swayScaleSlow()
                )),
                side * 0.15F,
                0.0F,
                0.0F
        );

        float recoil = recoil(player, partialTick) * 0.4F;
        poseStack.translate(0.0F, -recoil, 0.0F);
        suppressEquip.set(true);
    }

    @WrapOperation(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V",
                    ordinal = 2
            )
    )
    private void mobsmoreweapons$suppressSpearEquipOffset(
            ItemInHandRenderer renderer,
            PoseStack poseStack,
            HumanoidArm arm,
            float equipProgress,
            Operation<Void> original,
            @Share("mobsmoreweapons$suppressSpearEquip") LocalBooleanRef suppressEquip
    ) {
        if (!suppressEquip.get()) {
            original.call(renderer, poseStack, arm, equipProgress);
        }
    }

    private static float recoil(AbstractClientPlayer player, float partialTick) {
        float elapsed = ((SpearUser) player).mobsmoreweapons$timeSinceLastSpearImpact(partialTick);
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
