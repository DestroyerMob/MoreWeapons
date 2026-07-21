package org.destroyermob.mobsmoreweapons.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import org.destroyermob.mobsmoreweapons.item.SpearItem;
import org.destroyermob.mobsmoreweapons.item.SpearUser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySpearMixin implements SpearUser {
    @Unique
    private long mobsmoreweapons$lastSpearImpactTime = Long.MIN_VALUE;
    @Unique
    private Map<UUID, Long> mobsmoreweapons$spearContactCooldowns;

    @Inject(
            method = "startUsingItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;gameEvent(Lnet/minecraft/core/Holder;)V")
    )
    private void mobsmoreweapons$startSpearUse(InteractionHand hand, CallbackInfo callback) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.getUseItem().getItem() instanceof SpearItem) {
            mobsmoreweapons$spearContactCooldowns = new HashMap<>();
        }
    }

    @WrapOperation(
            method = "startUsingItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;gameEvent(Lnet/minecraft/core/Holder;)V")
    )
    private void mobsmoreweapons$suppressSpearStartVibration(
            LivingEntity entity,
            Holder<GameEvent> event,
            Operation<Void> original
    ) {
        if (!(entity.getUseItem().getItem() instanceof SpearItem)) {
            original.call(entity, event);
        }
    }

    @WrapOperation(
            method = "stopUsingItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;gameEvent(Lnet/minecraft/core/Holder;)V")
    )
    private void mobsmoreweapons$suppressSpearStopVibration(
            LivingEntity entity,
            Holder<GameEvent> event,
            Operation<Void> original
    ) {
        if (!(entity.getUseItem().getItem() instanceof SpearItem)) {
            original.call(entity, event);
        }
    }

    @Inject(
            method = "stopUsingItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setLivingEntityFlag(IZ)V")
    )
    private void mobsmoreweapons$stopSpearUse(CallbackInfo callback) {
        mobsmoreweapons$spearContactCooldowns = null;
    }

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void mobsmoreweapons$recordSpearImpact(byte status, CallbackInfo callback) {
        if (status == 2) {
            LivingEntity entity = (LivingEntity) (Object) this;
            mobsmoreweapons$lastSpearImpactTime = entity.level().getGameTime();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void mobsmoreweapons$tickSpearContactCooldowns(CallbackInfo callback) {
        LivingEntity owner = (LivingEntity) (Object) this;
        if (owner.level().isClientSide || mobsmoreweapons$spearContactCooldowns == null) {
            return;
        }

        long gameTime = owner.level().getGameTime();
        Iterator<Map.Entry<UUID, Long>> iterator = mobsmoreweapons$spearContactCooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (entry.getValue() <= gameTime) {
                iterator.remove();
            }
        }
    }

    @Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
    private void mobsmoreweapons$useSpearSwingDuration(CallbackInfoReturnable<Integer> callback) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity.getMainHandItem().getItem() instanceof SpearItem spear)) {
            return;
        }

        int duration = spear.swingAnimationTicks();
        if (MobEffectUtil.hasDigSpeed(entity)) {
            duration -= 1 + MobEffectUtil.getDigSpeedAmplification(entity);
        } else if (entity.hasEffect(MobEffects.DIG_SLOWDOWN)) {
            duration += (1 + Objects.requireNonNull(entity.getEffect(MobEffects.DIG_SLOWDOWN)).getAmplifier()) * 2;
        }
        callback.setReturnValue(duration);
    }

    @Override
    public float mobsmoreweapons$timeSinceLastSpearImpact(float partialTick) {
        LivingEntity entity = (LivingEntity) (Object) this;
        return mobsmoreweapons$lastSpearImpactTime == Long.MIN_VALUE
                ? 0.0F
                : entity.level().getGameTime() - mobsmoreweapons$lastSpearImpactTime + partialTick;
    }

    @Override
    public boolean mobsmoreweapons$maintainSpearContactCooldown(Entity target, int cooldownTicks) {
        if (mobsmoreweapons$spearContactCooldowns == null) {
            return false;
        }
        LivingEntity owner = (LivingEntity) (Object) this;
        long gameTime = owner.level().getGameTime();
        UUID targetId = target.getUUID();
        Long expiresAt = mobsmoreweapons$spearContactCooldowns.get(targetId);
        if (expiresAt == null || expiresAt <= gameTime) {
            mobsmoreweapons$spearContactCooldowns.remove(targetId);
            return false;
        }

        // Only a contact that was created by a real effect is extended. This
        // turns the cooldown into a continuous-contact latch without allowing
        // a slow/failed approach to poison the later, valid part of that pass.
        mobsmoreweapons$spearContactCooldowns.put(targetId, gameTime + cooldownTicks);
        return true;
    }

    @Override
    public void mobsmoreweapons$startSpearContactCooldown(Entity target, int cooldownTicks) {
        if (mobsmoreweapons$spearContactCooldowns != null) {
            LivingEntity owner = (LivingEntity) (Object) this;
            mobsmoreweapons$spearContactCooldowns.put(
                    target.getUUID(),
                    owner.level().getGameTime() + cooldownTicks
            );
        }
    }

    @Override
    public int mobsmoreweapons$countSpearContacts() {
        return mobsmoreweapons$spearContactCooldowns == null ? 0 : mobsmoreweapons$spearContactCooldowns.size();
    }
}
