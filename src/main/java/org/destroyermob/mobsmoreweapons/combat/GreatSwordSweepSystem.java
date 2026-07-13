package org.destroyermob.mobsmoreweapons.combat;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.destroyermob.mobsmoreweapons.MoreWeapons;
import org.destroyermob.mobsmoreweapons.item.GreatSwordItem;
import org.destroyermob.mobsmoreweapons.network.GreatSwordSweepAnimationPayload;

/** Server-authoritative windup, strike, and shared recovery for the greatsword sweep. */
public final class GreatSwordSweepSystem {
    public static final int PREPARATION_TICKS = 12;
    public static final float DAMAGE_MULTIPLIER = 0.60F;
    public static final int TARGET_CAP = 5;
    public static final float RECOVERY_MULTIPLIER = 1.25F;
    public static final double BRACING_KNOCKBACK_RESISTANCE = 0.75D;
    private static final double SWEEP_REACH = 3.5D;
    private static final double HALF_ARC_COSINE = Math.cos(Math.toRadians(65.0D));
    private static final float BASE_KNOCKBACK = 1.0F;
    private static final ResourceLocation BRACING_KNOCKBACK_ID = ResourceLocation.fromNamespaceAndPath(
            MoreWeapons.MOD_ID,
            "great_sword_bracing_knockback_resistance"
    );
    private static final Map<Player, SweepState> STATES = new WeakHashMap<>();

    private GreatSwordSweepSystem() {
    }

    public static boolean beginCharge(Player player, ItemStack weapon) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(weapon.getItem() instanceof GreatSwordItem)
                || isRecovering(player)
                || player.getAttackStrengthScale(0.0F) < 1.0F) {
            return false;
        }

        SweepState state = new SweepState(player.getInventory().selected, weapon);
        STATES.put(player, state);
        applyBracingResistance(serverPlayer);
        serverPlayer.setSprinting(false);
        return true;
    }

    public static void cancelCharge(Player player, ItemStack weapon) {
        SweepState state = STATES.get(player);
        if (state == null || !state.charging || state.weapon != weapon) {
            return;
        }
        removeBracingResistance(player);
        STATES.remove(player);
    }

    public static void completeCharge(Player player, ItemStack weapon) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        SweepState state = STATES.get(player);
        if (state == null
                || !state.charging
                || state.weapon != weapon
                || state.selectedSlot != player.getInventory().selected
                || player.getMainHandItem() != weapon) {
            cancelCharge(player, weapon);
            return;
        }

        removeBracingResistance(player);
        state.charging = false;
        performSweep(serverPlayer, weapon);
        PacketDistributor.sendToPlayer(serverPlayer, GreatSwordSweepAnimationPayload.INSTANCE);

        int recoveryTicks = Math.max(
                1,
                (int) Math.ceil(player.getCurrentItemAttackStrengthDelay() * RECOVERY_MULTIPLIER)
        );
        player.resetAttackStrengthTicker();
        player.getCooldowns().addCooldown(weapon.getItem(), recoveryTicks);
        state.recoveryEndTick = player.level().getGameTime() + recoveryTicks;
    }

    public static boolean blocksAttack(Player player) {
        SweepState state = STATES.get(player);
        return state != null && (state.charging || state.recoveryEndTick > player.level().getGameTime());
    }

    public static boolean isRecovering(Player player) {
        SweepState state = STATES.get(player);
        return state != null && state.recoveryEndTick > player.level().getGameTime();
    }

    public static void tickPlayer(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        SweepState state = STATES.get(player);
        if (state == null) {
            return;
        }

        if (state.charging) {
            boolean stillBracing = player.isAlive()
                    && player.getInventory().selected == state.selectedSlot
                    && player.getMainHandItem() == state.weapon
                    && player.isUsingItem()
                    && player.getUsedItemHand() == InteractionHand.MAIN_HAND
                    && player.getUseItem() == state.weapon;
            if (!stillBracing) {
                removeBracingResistance(player);
                STATES.remove(player);
            }
            return;
        }

        if (state.recoveryEndTick <= player.level().getGameTime()) {
            STATES.remove(player);
        }
    }

    private static void performSweep(ServerPlayer player, ItemStack weapon) {
        List<LivingEntity> targets = targetsInArc(player);
        DamageSource source = player.damageSources().playerAttack(player);
        float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE_MULTIPLIER;
        boolean damagedAny = false;

        for (LivingEntity target : targets) {
            float damage = EnchantmentHelper.modifyDamage(player.serverLevel(), weapon, target, source, baseDamage);
            if (!target.hurt(source, damage)) {
                continue;
            }

            damagedAny = true;
            float knockback = EnchantmentHelper.modifyKnockback(
                    player.serverLevel(),
                    weapon,
                    target,
                    source,
                    BASE_KNOCKBACK
            );
            target.knockback(knockback, player.getX() - target.getX(), player.getZ() - target.getZ());
            EnchantmentHelper.doPostAttackEffects(player.serverLevel(), target, source);
            player.setLastHurtMob(target);
        }

        if (damagedAny) {
            weapon.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        player.sweepAttack();
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS,
                1.0F,
                0.8F
        );
        player.awardStat(Stats.ITEM_USED.get(weapon.getItem()));
    }

    private static List<LivingEntity> targetsInArc(ServerPlayer player) {
        Vec3 forward = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (forward.lengthSqr() < 1.0E-7D) {
            return List.of();
        }
        forward = forward.normalize();
        Vec3 origin = player.position();
        AABB search = player.getBoundingBox().inflate(SWEEP_REACH, 1.5D, SWEEP_REACH);
        Vec3 sweepDirection = forward;

        return player.serverLevel().getEntitiesOfClass(
                        LivingEntity.class,
                        search,
                        target -> isValidTarget(player, target, origin, sweepDirection)
                ).stream()
                .sorted(Comparator.comparingDouble(player::distanceToSqr))
                .limit(TARGET_CAP)
                .toList();
    }

    private static boolean isValidTarget(Player player, LivingEntity target, Vec3 origin, Vec3 forward) {
        if (target == player
                || !target.isAlive()
                || target.isSpectator()
                || !target.isAttackable()
                || player.isAlliedTo(target)
                || !player.hasLineOfSight(target)) {
            return false;
        }

        Vec3 offset = target.getBoundingBox().getCenter().subtract(origin).multiply(1.0D, 0.0D, 1.0D);
        double allowedReach = SWEEP_REACH + target.getBbWidth() * 0.5D;
        return offset.lengthSqr() <= allowedReach * allowedReach
                && offset.lengthSqr() > 1.0E-7D
                && offset.normalize().dot(forward) >= HALF_ARC_COSINE;
    }

    private static void applyBracingResistance(Player player) {
        AttributeInstance resistance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (resistance == null) {
            return;
        }
        resistance.removeModifier(BRACING_KNOCKBACK_ID);
        double amount = Math.max(0.0D, BRACING_KNOCKBACK_RESISTANCE - resistance.getValue());
        if (amount > 0.0D) {
            resistance.addTransientModifier(new AttributeModifier(
                    BRACING_KNOCKBACK_ID,
                    amount,
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }
    }

    private static void removeBracingResistance(Player player) {
        AttributeInstance resistance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (resistance != null) {
            resistance.removeModifier(BRACING_KNOCKBACK_ID);
        }
    }

    private static final class SweepState {
        private final int selectedSlot;
        private final ItemStack weapon;
        private boolean charging = true;
        private long recoveryEndTick;

        private SweepState(int selectedSlot, ItemStack weapon) {
            this.selectedSlot = selectedSlot;
            this.weapon = weapon;
        }
    }
}
