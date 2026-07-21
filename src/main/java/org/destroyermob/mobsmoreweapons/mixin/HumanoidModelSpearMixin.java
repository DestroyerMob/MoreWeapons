package org.destroyermob.mobsmoreweapons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.destroyermob.mobsmoreweapons.client.SpearChargeAnimation;
import org.destroyermob.mobsmoreweapons.item.SpearItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Third-person arm poses from the vanilla spear backport. */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelSpearMixin<T extends LivingEntity> extends AgeableListModel<T> {
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart body;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart head;
    @Shadow public ArmPose rightArmPose;
    @Shadow public ArmPose leftArmPose;

    @Shadow
    protected abstract ModelPart getArm(HumanoidArm arm);

    @Inject(method = "poseRightArm", at = @At("HEAD"), cancellable = true)
    private void mobsmoreweapons$poseRightSpearArm(T entity, CallbackInfo callback) {
        if ((!rightArmPose.isTwoHanded() || attackTime > 0.0F)
                && mobsmoreweapons$poseSpearArm(entity, HumanoidArm.RIGHT)
                && !entity.isUsingItem()
                && attackTime > 0.0F) {
            callback.cancel();
        }
    }

    @Inject(method = "poseLeftArm", at = @At("HEAD"), cancellable = true)
    private void mobsmoreweapons$poseLeftSpearArm(T entity, CallbackInfo callback) {
        if ((!leftArmPose.isTwoHanded() || attackTime > 0.0F)
                && mobsmoreweapons$poseSpearArm(entity, HumanoidArm.LEFT)
                && !entity.isUsingItem()
                && attackTime > 0.0F) {
            callback.cancel();
        }
    }

    @Unique
    private boolean mobsmoreweapons$poseSpearArm(T entity, HumanoidArm arm) {
        ItemStack stack = entity.getMainArm() == arm ? entity.getMainHandItem() : entity.getOffhandItem();
        if (!(stack.getItem() instanceof SpearItem spear)) {
            return false;
        }

        ModelPart usedArm = getArm(arm);
        usedArm.yRot = -0.1F * head.yRot;
        usedArm.xRot = (float) (-Math.PI / 2.0D) + head.xRot + 0.8F;
        if (entity.isFallFlying() || entity.getSwimAmount(1.0F) > 0.0F) {
            usedArm.xRot -= 0.9599311F;
        }

        HumanoidArm usedItemArm = entity.getUsedItemHand() == InteractionHand.MAIN_HAND
                ? entity.getMainArm()
                : entity.getMainArm().getOpposite();
        if (entity.isUsingItem() && usedItemArm == arm && entity.getUseItem().getItem() instanceof SpearItem) {
            int side = arm == HumanoidArm.RIGHT ? 1 : -1;
            float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
            SpearChargeAnimation animation = SpearChargeAnimation.play(spear, entity.getTicksUsingItem() + partialTick);
            usedArm.yRot -= side * animation.swayScaleFast() * Mth.DEG_TO_RAD * animation.swayIntensity();
            usedArm.zRot -= side * animation.swayScaleSlow() * Mth.DEG_TO_RAD * animation.swayIntensity() * 0.5F;
            usedArm.xRot += Mth.DEG_TO_RAD * (
                    -40.0F * animation.raiseProgressStart()
                            + 30.0F * animation.raiseProgressMiddle()
                            + 20.0F * animation.raiseProgressEnd()
                            - 20.0F * animation.lowerProgress()
                            + 10.0F * animation.raiseBackProgress()
                            + 0.6F * animation.swayScaleSlow() * animation.swayIntensity()
            );
        }
        return true;
    }

    @Inject(
            method = "setupAttackAnimation",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;sin(F)F", ordinal = 3),
            cancellable = true
    )
    private void mobsmoreweapons$animateSpearStab(T entity, float animationProgress, CallbackInfo callback) {
        if (!(entity.getMainHandItem().getItem() instanceof SpearItem)) {
            return;
        }

        float swing = entity.isUsingItem() ? 1.0F : attackTime;
        HumanoidArm arm = entity.getMainArm();
        rightArm.yRot -= body.yRot;
        leftArm.yRot -= body.yRot;
        leftArm.xRot -= body.yRot;
        float thrustStart = -(Mth.cos((float) Math.PI * clampedInverseLerp(swing, 0.0F, 0.05F)) - 1.0F) / 2.0F;
        float thrust = clampedInverseLerp(swing, 0.05F, 0.2F);
        thrust *= thrust;
        float recovery = exponentialEase(clampedInverseLerp(swing, 0.4F, 1.0F));
        getArm(arm).xRot += (90.0F * thrustStart - 120.0F * thrust + 30.0F * recovery) * Mth.DEG_TO_RAD;
        float yaw = normalizedYaw(head.yRot);
        getArm(arm).yRot += yaw * (thrust - recovery);
        callback.cancel();
    }

    @Inject(method = "setupAttackAnimation", at = @At("HEAD"), cancellable = true)
    private void mobsmoreweapons$animateSpearCharge(T entity, float animationProgress, CallbackInfo callback) {
        ItemStack stack = entity.isUsingItem() ? entity.getUseItem() : entity.getMainHandItem();
        if (!entity.isUsingItem() || !(stack.getItem() instanceof SpearItem spear)) {
            return;
        }

        float elapsed = entity.getTicksUsingItem() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        float delay = spear.chargeDelayTicks() == 0 ? 1.0F : spear.chargeDelayTicks();
        float progress;
        if (elapsed > spear.damageEndTicks()) {
            progress = Math.max(0.0F, (1.0F - (elapsed - spear.damageEndTicks())) / delay);
        } else {
            progress = Math.min(1.0F, elapsed / delay);
        }

        HumanoidArm arm = entity.getUsedItemHand() == InteractionHand.MAIN_HAND
                ? entity.getMainArm()
                : entity.getMainArm().getOpposite();
        ModelPart usedArm = getArm(arm);
        float yaw = normalizedYaw(head.yRot);
        usedArm.yRot = Mth.clamp(yaw, (float) (-Math.PI * 5.0D / 12.0D), (float) (Math.PI * 5.0D / 12.0D)) * progress;
        usedArm.xRot += head.xRot * 0.5F * progress
                - (entity.isFallFlying() || entity.getSwimAmount(1.0F) > 0.0F ? 55.0F : 30.0F) * Mth.DEG_TO_RAD * progress;
        callback.cancel();
    }

    private static float normalizedYaw(float yaw) {
        while (yaw >= Math.PI * 2.0D) yaw -= (float) (Math.PI * 2.0D);
        while (yaw <= -Math.PI * 2.0D) yaw += (float) (Math.PI * 2.0D);
        return yaw;
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
