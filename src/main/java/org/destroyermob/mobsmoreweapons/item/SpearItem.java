package org.destroyermob.mobsmoreweapons.item;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.destroyermob.mobsmoreweapons.MoreWeapons;
import org.destroyermob.mobsmoreweapons.item.tier.ModTiers;

/** A 1.21.1 backport of the vanilla spear's jab and velocity-driven charge. */
public class SpearItem extends SwordItem {
    public static final double MINIMUM_ATTACK_RANGE = 2.0D;
    public static final double MAXIMUM_ATTACK_RANGE = 4.5D;
    public static final double HITBOX_MARGIN = 0.125D;
    private static final double MINIMUM_DAMAGE_RELATIVE_SPEED = 4.6D;
    private static final double MINIMUM_KNOCKBACK_RELATIVE_SPEED = 5.1D;
    private static final int CONTACT_COOLDOWN_TICKS = 10;
    private static final Map<UUID, Map<Integer, ChargeContact>> CHARGE_CONTACTS = new ConcurrentHashMap<>();
    public static final TagKey<net.minecraft.world.item.Item> SPEARS = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MoreWeapons.MOD_ID, "spears")
    );
    private static final ResourceLocation REACH_ID = ResourceLocation.fromNamespaceAndPath(MoreWeapons.MOD_ID, "spear_reach");
    private static final ResourceKey<Enchantment> LUNGE = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(MoreWeapons.MOD_ID, "lunge")
    );

    public SpearItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    public static ItemAttributeModifiers createAttributes(Tier tier, int attackDamage, float attackSpeed) {
        return SwordItem.createAttributes(tier, attackDamage, attackSpeed)
                .withModifierAdded(
                        Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(REACH_ID, 1.5D, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                );
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return profile().totalUseTicks();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getDamageValue() >= stack.getMaxDamage() - 1) {
            return InteractionResultHolder.fail(stack);
        }
        CHARGE_CONTACTS.remove(player.getUUID());
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (entity instanceof Player player) {
            CHARGE_CONTACTS.remove(player.getUUID());
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            CHARGE_CONTACTS.remove(player.getUUID());
        }
        return stack;
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (!(level instanceof ServerLevel serverLevel) || !(livingEntity instanceof Player player)) {
            return;
        }

        ChargeProfile profile = profile();
        int elapsed = getUseDuration(stack, player) - remainingUseDuration;
        if (elapsed < profile.startupTicks()) {
            return;
        }

        long gameTime = level.getGameTime();
        Map<Integer, ChargeContact> contacts = CHARGE_CONTACTS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        java.util.List<Entity> targets = targetsAlongSpear(player);
        Set<Integer> currentContacts = new HashSet<>();
        for (Entity target : targets) {
            currentContacts.add(target.getId());
        }
        contacts.forEach((id, contact) -> {
            if (!currentContacts.contains(id)) {
                contact.inContact = false;
                contact.hitDuringContact = false;
            }
        });

        Vec3 view = player.getLookAngle().normalize();
        Vec3 attackerVelocity = effectiveVelocity(player);
        double attackerSpeed = Math.max(0.0D, attackerVelocity.dot(view) * 20.0D);
        for (Entity target : targets) {
            ChargeContact contact = contacts.computeIfAbsent(target.getId(), ignored -> new ChargeContact());
            contact.inContact = true;
            if (gameTime - contact.lastHitTick < CONTACT_COOLDOWN_TICKS) {
                continue;
            }
            if (contact.hitDuringContact) {
                continue;
            }

            double relativeSpeed = Math.max(0.0D, attackerVelocity.subtract(effectiveVelocity(target)).dot(view) * 20.0D);
            if (relativeSpeed < MINIMUM_DAMAGE_RELATIVE_SPEED) {
                continue;
            }

            contact.hitDuringContact = true;
            contact.lastHitTick = gameTime;
            float damage = (float) Math.floor(profile.damageMultiplier() * relativeSpeed);
            DamageSource source = player.damageSources().playerAttack(player);
            damage = EnchantmentHelper.modifyDamage(serverLevel, stack, target, source, damage);
            boolean damaged = target.hurt(source, damage);
            ChargeStage stage = profile.stage(elapsed);
            if (stage != ChargeStage.DISENGAGED && relativeSpeed >= MINIMUM_KNOCKBACK_RELATIVE_SPEED) {
                knockBackChargeTarget(serverLevel, stack, player, target, source, attackerVelocity.subtract(effectiveVelocity(target)));
            }
            if (stage == ChargeStage.ENGAGED && attackerSpeed >= profile.dismountSpeed() && target.isPassenger()) {
                target.stopRiding();
            }
            if (damaged) {
                EnchantmentHelper.doPostAttackEffects(serverLevel, target, source);
                stack.hurtAndBreak(1, player, player.getUsedItemHand() == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
            }
        }
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        if (attacker instanceof Player player && !player.isUsingItem()) {
            applyLunge(stack, player);
        }
    }

    /** Called after the primary vanilla jab to apply the spear's piercing multi-hit. */
    public static void hitAdditionalJabTargets(Player player, Entity primaryTarget) {
        if (!(player.level() instanceof ServerLevel serverLevel) || !isSpear(player.getMainHandItem())) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        DamageSource source = player.damageSources().playerAttack(player);
        for (Entity target : targetsAlongSpear(player)) {
            if (target == primaryTarget) {
                continue;
            }
            float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            damage = EnchantmentHelper.modifyDamage(serverLevel, stack, target, source, damage);
            if (target.hurt(source, damage)) {
                float knockback = EnchantmentHelper.modifyKnockback(serverLevel, stack, target, source, 0.0F);
                applyHorizontalKnockback(player, target, knockback * 0.5F);
                EnchantmentHelper.doPostAttackEffects(serverLevel, target, source);
                if (target instanceof LivingEntity living) {
                    stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                    player.setLastHurtMob(living);
                }
            }
        }
    }

    public static boolean isValidJabTarget(Player player, Entity target) {
        return distanceAlongSpear(player, target)
                .stream()
                .anyMatch(distance -> distance >= MINIMUM_ATTACK_RANGE && distance <= MAXIMUM_ATTACK_RANGE);
    }

    public static boolean isSpear(ItemStack stack) {
        return stack.getItem() instanceof SpearItem || stack.is(SPEARS);
    }

    private static void knockBackChargeTarget(ServerLevel level, ItemStack stack, Player player, Entity target, DamageSource source, Vec3 relativeVelocity) {
        float enchantmentKnockback = EnchantmentHelper.modifyKnockback(level, stack, target, source, 0.0F);
        double strength = 0.75D + enchantmentKnockback * 0.5D;
        Vec3 horizontal = relativeVelocity.multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() < 1.0E-7D) {
            horizontal = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        }
        if (horizontal.lengthSqr() > 1.0E-7D) {
            horizontal = horizontal.normalize().scale(strength);
            target.push(horizontal.x, target instanceof LivingEntity ? 0.1D : 0.0D, horizontal.z);
            target.hurtMarked = true;
        }
    }

    private static void applyHorizontalKnockback(Player player, Entity target, double strength) {
        if (strength <= 0.0D) {
            return;
        }
        Vec3 direction = target.position().subtract(player.position()).multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() > 1.0E-7D) {
            direction = direction.normalize().scale(strength);
            target.push(direction.x, 0.0D, direction.z);
            target.hurtMarked = true;
        }
    }

    private static java.util.List<Entity> targetsAlongSpear(Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().normalize().scale(MAXIMUM_ATTACK_RANGE));
        AABB search = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        return player.level().getEntities(player, search, target -> isAttackableBy(player, target))
                .stream()
                .filter(target -> distanceAlongSpear(start, end, target)
                        .stream()
                        .anyMatch(distance -> distance >= MINIMUM_ATTACK_RANGE && distance <= MAXIMUM_ATTACK_RANGE))
                .sorted(Comparator.comparingDouble(target -> distanceAlongSpear(start, end, target).orElse(Double.MAX_VALUE)))
                .toList();
    }

    private static boolean isAttackableBy(Player player, Entity target) {
        return target.isAlive() && !target.isSpectator() && target.isAttackable() && !player.isAlliedTo(target);
    }

    private static OptionalDouble distanceAlongSpear(Player player, Entity target) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().normalize().scale(MAXIMUM_ATTACK_RANGE));
        return distanceAlongSpear(start, end, target);
    }

    private static OptionalDouble distanceAlongSpear(Vec3 start, Vec3 end, Entity target) {
        return target.getBoundingBox().inflate(HITBOX_MARGIN).clip(start, end)
                .map(hit -> OptionalDouble.of(start.distanceTo(hit)))
                .orElseGet(OptionalDouble::empty);
    }

    private ChargeProfile profile() {
        Tier tier = getTier();
        if (tier == Tiers.NETHERITE) return new ChargeProfile(1.2D, 8, 50, 60, 65, 9.0D);
        if (tier == Tiers.DIAMOND) return new ChargeProfile(1.075D, 10, 60, 70, 70, 10.0D);
        if (tier == Tiers.IRON) return new ChargeProfile(0.95D, 12, 50, 85, 90, 11.0D);
        if (tier == Tiers.GOLD) return new ChargeProfile(0.7D, 14, 70, 100, 105, 13.0D);
        if (tier == ModTiers.COPPER) return new ChargeProfile(0.82D, 13, 80, 85, 85, 12.0D);
        if (tier == Tiers.STONE) return new ChargeProfile(0.82D, 14, 90, 90, 95, 13.0D);
        return new ChargeProfile(0.7D, 15, 100, 100, 100, 14.0D);
    }

    private static Vec3 effectiveVelocity(Entity entity) {
        Entity vehicle = entity.getVehicle();
        return vehicle == null ? entity.getDeltaMovement() : vehicle.getDeltaMovement();
    }

    private static void applyLunge(ItemStack stack, Player player) {
        int level = player.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(LUNGE)
                .map(stack::getEnchantmentLevel).orElse(0);
        level = Math.min(level, 3);
        if (level <= 0 || player.isPassenger() || player.isFallFlying() || player.isInWater() || player.getFoodData().getFoodLevel() < 6) {
            return;
        }
        Vec3 direction = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() < 1.0E-7D) return;
        direction = direction.normalize().scale(0.458D * level);
        player.push(direction.x, 0.0D, direction.z);
        player.causeFoodExhaustion(4.0F * level);
        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        player.level().playSound(null, player, lungeSound(level).value(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static net.minecraft.core.Holder<SoundEvent> lungeSound(int level) {
        return switch (level) {
            case 1 -> SoundEvents.TRIDENT_RIPTIDE_1;
            case 2 -> SoundEvents.TRIDENT_RIPTIDE_2;
            default -> SoundEvents.TRIDENT_RIPTIDE_3;
        };
    }

    private enum ChargeStage { ENGAGED, TIRED, DISENGAGED }

    private record ChargeProfile(double damageMultiplier, int startupTicks, int engagedTicks, int tiredTicks, int disengagedTicks, double dismountSpeed) {
        ChargeStage stage(int elapsed) {
            int active = elapsed - startupTicks;
            if (active < engagedTicks) return ChargeStage.ENGAGED;
            if (active < engagedTicks + tiredTicks) return ChargeStage.TIRED;
            return ChargeStage.DISENGAGED;
        }

        int totalUseTicks() {
            return startupTicks + engagedTicks + tiredTicks + disengagedTicks;
        }
    }

    private static final class ChargeContact {
        private long lastHitTick = Long.MIN_VALUE / 2L;
        private boolean inContact;
        private boolean hitDuringContact;
    }
}
