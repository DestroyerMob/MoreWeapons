package org.destroyermob.mobsmoreweapons.combat;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.destroyermob.mobsmoreweapons.config.MoreWeaponsConfig;
import org.destroyermob.mobsmoreweapons.item.BattleAxeItem;
import org.destroyermob.mobsmoreweapons.mixin.LivingEntityAttackStrengthAccessor;

/** Server-authoritative preparation, targeting, pull, shield break, and partial recovery for Hooking Strike. */
public final class BattleAxeHookSystem {
    public static final float REQUIRED_ATTACK_STRENGTH = 0.90F;
    private static final double HOOK_REACH = 3.25D;
    private static final double TARGET_INFLATION = 0.35D;
    private static final Map<Player, HookState> STATES = new WeakHashMap<>();

    private BattleAxeHookSystem() {
    }

    public static boolean beginPreparation(Player player, ItemStack weapon) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(weapon.getItem() instanceof BattleAxeItem)
                || !player.getOffhandItem().isEmpty()
                || player.getAttackStrengthScale(0.0F) < REQUIRED_ATTACK_STRENGTH
                || STATES.containsKey(player)) {
            return false;
        }

        STATES.put(player, new HookState(player.getInventory().selected, weapon, player.level().getGameTime()));
        serverPlayer.setSprinting(false);
        return true;
    }

    public static void release(Player player, ItemStack weapon) {
        HookState state = STATES.remove(player);
        if (!(player instanceof ServerPlayer serverPlayer)
                || state == null
                || state.weapon != weapon
                || state.selectedSlot != player.getInventory().selected
                || player.getMainHandItem() != weapon) {
            return;
        }

        long heldTicks = player.level().getGameTime() - state.startedAt;
        if (heldTicks < MoreWeaponsConfig.BATTLE_AXE_HOOK_PREPARATION_TICKS.get()) {
            return;
        }

        performHook(serverPlayer, weapon);
        consumeRecovery(player);
    }

    public static boolean blocksAttack(Player player) {
        return STATES.containsKey(player);
    }

    public static void tickPlayer(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        HookState state = STATES.get(player);
        if (state == null) {
            return;
        }

        boolean stillPreparing = player.isAlive()
                && player.getInventory().selected == state.selectedSlot
                && player.getMainHandItem() == state.weapon
                && player.getOffhandItem().isEmpty()
                && player.isUsingItem()
                && player.getUsedItemHand() == InteractionHand.MAIN_HAND
                && player.getUseItem() == state.weapon;
        if (!stillPreparing) {
            STATES.remove(player);
            return;
        }

        player.setSprinting(false);
        long heldTicks = player.level().getGameTime() - state.startedAt;
        if (!state.readySoundPlayed
                && heldTicks >= MoreWeaponsConfig.BATTLE_AXE_HOOK_PREPARATION_TICKS.get()) {
            state.readySoundPlayed = true;
            player.level().playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.CROSSBOW_LOADING_END.value(),
                    SoundSource.PLAYERS,
                    0.55F,
                    1.35F
            );
        }
    }

    private static void performHook(ServerPlayer player, ItemStack weapon) {
        LivingEntity target = findTarget(player);
        boolean caughtTarget = target != null;
        boolean caughtShield = false;

        if (target != null) {
            DamageSource source = player.damageSources().playerAttack(player);
            caughtShield = target.isBlocking()
                    && target.getUseItem().canPerformAction(ItemAbilities.SHIELD_BLOCK)
                    && target.isDamageSourceBlocked(source);

            if (caughtShield) {
                disableShield(target);
            } else {
                float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                        * MoreWeaponsConfig.BATTLE_AXE_HOOK_DAMAGE_MULTIPLIER.get().floatValue();
                float damage = EnchantmentHelper.modifyDamage(
                        player.serverLevel(),
                        weapon,
                        target,
                        source,
                        baseDamage
                );
                if (target.hurt(source, damage)) {
                    EnchantmentHelper.doPostAttackEffects(player.serverLevel(), target, source);
                    player.setLastHurtMob(target);
                }
            }

            pullTowardPlayer(player, target);
            weapon.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            player.serverLevel().sendParticles(
                    ParticleTypes.CRIT,
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.55D,
                    target.getZ(),
                    caughtShield ? 8 : 4,
                    0.22D,
                    0.22D,
                    0.22D,
                    caughtShield ? 0.06D : 0.0D
            );
        }

        player.swing(InteractionHand.MAIN_HAND, true);
        player.level().playSound(
                null,
                player.blockPosition(),
                caughtShield ? SoundEvents.SHIELD_BREAK
                        : caughtTarget ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_NODAMAGE,
                SoundSource.PLAYERS,
                caughtShield ? 0.8F : 0.75F,
                caughtShield ? 1.15F : 0.8F
        );
        player.awardStat(Stats.ITEM_USED.get(weapon.getItem()));
    }

    private static LivingEntity findTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 maximumEnd = eye.add(look.scale(HOOK_REACH));
        HitResult blockHit = player.level().clip(new ClipContext(
                eye,
                maximumEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? maximumEnd : blockHit.getLocation();
        AABB search = player.getBoundingBox()
                .expandTowards(look.scale(HOOK_REACH))
                .inflate(TARGET_INFLATION);
        EntityHitResult result = ProjectileUtil.getEntityHitResult(
                player,
                eye,
                end,
                search,
                entity -> isValidTarget(player, entity),
                eye.distanceToSqr(end)
        );
        return result != null && result.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static boolean isValidTarget(Player player, Entity entity) {
        if (!(entity instanceof LivingEntity target)
                || target == player
                || !target.isAlive()
                || target.isSpectator()
                || !target.isAttackable()
                || player.isAlliedTo(target)
                || !player.hasLineOfSight(target)) {
            return false;
        }
        return !(target instanceof Player targetPlayer) || player.canHarmPlayer(targetPlayer);
    }

    private static void disableShield(LivingEntity target) {
        ItemStack shield = target.getUseItem();
        target.stopUsingItem();
        if (target instanceof Player player && !shield.isEmpty()) {
            player.getCooldowns().addCooldown(
                    shield.getItem(),
                    MoreWeaponsConfig.BATTLE_AXE_HOOK_SHIELD_DISABLE_TICKS.get()
            );
        }
    }

    private static void pullTowardPlayer(Player player, LivingEntity target) {
        Vec3 horizontal = player.position().subtract(target.position()).multiply(1.0D, 0.0D, 1.0D);
        double distance = horizontal.length();
        if (distance < 1.0E-5D) {
            return;
        }

        double resistance = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D);
        double desiredDistance = Math.min(
                MoreWeaponsConfig.BATTLE_AXE_HOOK_PULL_DISTANCE.get(),
                Math.max(0.0D, distance - 0.75D)
        );
        double velocity = Math.min(0.85D, desiredDistance * 0.45D) * (1.0D - resistance);
        Vec3 direction = horizontal.scale(1.0D / distance);
        Vec3 previous = target.getDeltaMovement();
        target.setDeltaMovement(
                direction.x * velocity,
                Mth.clamp(previous.y, -0.08D, 0.08D),
                direction.z * velocity
        );
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    private static void consumeRecovery(Player player) {
        float delay = player.getCurrentItemAttackStrengthDelay();
        double consumed = MoreWeaponsConfig.BATTLE_AXE_HOOK_RECOVERY_CONSUMED.get();
        int retainedTicks = Math.max(0, (int) Math.floor(delay * (1.0D - consumed)));
        ((LivingEntityAttackStrengthAccessor) player).mobsmoreweapons$setAttackStrengthTicker(retainedTicks);
    }

    private static final class HookState {
        private final int selectedSlot;
        private final ItemStack weapon;
        private final long startedAt;
        private boolean readySoundPlayed;

        private HookState(int selectedSlot, ItemStack weapon, long startedAt) {
            this.selectedSlot = selectedSlot;
            this.weapon = weapon;
            this.startedAt = startedAt;
        }
    }
}
