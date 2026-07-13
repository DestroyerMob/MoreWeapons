package org.destroyermob.mobsmoreweapons.item;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.destroyermob.mobsmoreweapons.MoreWeapons;
import org.destroyermob.mobsmoreweapons.item.tier.ModTiers;

/** A 1.21.1 backport of the vanilla spear's jab and velocity-driven charge. */
public class SpearItem extends SwordItem {
    public static final double MINIMUM_ATTACK_RANGE = 2.0D;
    public static final double MAXIMUM_ATTACK_RANGE = 4.5D;
    public static final double CREATIVE_MAXIMUM_ATTACK_RANGE = 6.5D;
    public static final double HITBOX_MARGIN = 0.125D;
    // Exact vanilla 1.21.11 kinetic-spear thresholds.
    private static final double MINIMUM_DAMAGE_RELATIVE_SPEED = 4.6D;
    private static final double MINIMUM_KNOCKBACK_RELATIVE_SPEED = 5.1D;
    private static final int CONTACT_COOLDOWN_TICKS = 10;
    private static final Map<UUID, Map<Integer, Long>> CHARGE_CONTACTS = new ConcurrentHashMap<>();
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
        Map<Integer, Long> contacts = CHARGE_CONTACTS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        Vec3 view = player.getLookAngle().normalize();
        Vec3 attackerMovement = amplifiedKineticMovement(player);
        double attackerSpeed = view.dot(attackerMovement);
        double baseAttackDamage = player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        int activeTicks = elapsed - profile.startupTicks();

        for (Entity target : targetsAlongSpear(player, MINIMUM_ATTACK_RANGE)) {
            long lastContact = contacts.getOrDefault(target.getId(), Long.MIN_VALUE / 2L);
            if (gameTime - lastContact < CONTACT_COOLDOWN_TICKS) {
                continue;
            }
            // Vanilla starts the cooldown before evaluating the three conditions.
            contacts.put(target.getId(), gameTime);

            double targetSpeed = view.dot(amplifiedKineticMovement(target));
            double relativeSpeed = Math.max(0.0D, attackerSpeed - targetSpeed);
            boolean dismount = activeTicks <= profile.dismountMaximumTicks()
                    && attackerSpeed >= profile.dismountSpeed();
            boolean knockback = activeTicks <= profile.knockbackMaximumTicks()
                    && relativeSpeed >= MINIMUM_KNOCKBACK_RELATIVE_SPEED;
            boolean dealDamage = activeTicks <= profile.damageMaximumTicks()
                    && relativeSpeed >= MINIMUM_DAMAGE_RELATIVE_SPEED;
            if (!dismount && !knockback && !dealDamage) {
                continue;
            }

            float damage = (float) baseAttackDamage + Mth.floor(relativeSpeed * profile.damageMultiplier());
            EquipmentSlot slot = player.getUsedItemHand() == InteractionHand.OFF_HAND
                    ? EquipmentSlot.OFFHAND
                    : EquipmentSlot.MAINHAND;
            stabTarget(serverLevel, stack, player, target, damage, dealDamage, knockback, dismount, slot);
        }
    }

    /** Server-side equivalent of vanilla's dedicated STAB player action. */
    public static void jab(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (player.isSpectator() || !isSpear(stack) || player.getAttackStrengthScale(5.0F) < 1.0F) {
            return;
        }

        ServerLevel level = player.serverLevel();
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (Entity target : targetsAlongSpear(player, MINIMUM_ATTACK_RANGE)) {
            stabTarget(level, stack, player, target, damage, true, true, false, EquipmentSlot.MAINHAND);
        }
        player.resetAttackStrengthTicker();
        applyLunge(stack, player);
        player.swing(InteractionHand.MAIN_HAND, false);
    }

    public static boolean isSpear(ItemStack stack) {
        return stack.getItem() instanceof SpearItem || stack.is(SPEARS);
    }

    private static void stabTarget(
            ServerLevel level,
            ItemStack stack,
            Player player,
            Entity target,
            float baseDamage,
            boolean dealDamage,
            boolean knockback,
            boolean dismount,
            EquipmentSlot slot
    ) {
        DamageSource source = player.damageSources().playerAttack(player);
        float damage = EnchantmentHelper.modifyDamage(level, stack, target, source, baseDamage);
        boolean damaged = dealDamage && target.hurt(source, damage);
        if (knockback) {
            knockBackTarget(level, stack, player, target, source);
        }
        if (dismount && target.isPassenger()) {
            target.stopRiding();
        }
        if (target instanceof LivingEntity living) {
            stack.hurtAndBreak(1, player, slot);
            player.setLastHurtMob(living);
        }
        if (damaged) {
            EnchantmentHelper.doPostAttackEffects(level, target, source);
        }
    }

    private static void knockBackTarget(ServerLevel level, ItemStack stack, Player player, Entity target, DamageSource source) {
        if (!(target instanceof LivingEntity living)) {
            return;
        }
        float enchantmentKnockback = EnchantmentHelper.modifyKnockback(level, stack, target, source, 0.0F);
        float yawRadians = player.getYRot() * ((float) Math.PI / 180.0F);
        living.knockback(0.4F + enchantmentKnockback, Mth.sin(yawRadians), -Mth.cos(yawRadians));
        player.setDeltaMovement(player.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
    }

    private static java.util.List<Entity> targetsAlongSpear(Player player, double minimumAttackRange) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 eye = player.getEyePosition();
        Vec3 start = eye.add(look.scale(minimumAttackRange));
        double movementExtension = Math.max(0.0D, lastTickMovement(player).dot(look));
        double maximumRange = player.getAbilities().instabuild ? CREATIVE_MAXIMUM_ATTACK_RANGE : MAXIMUM_ATTACK_RANGE;
        Vec3 maximumEnd = eye.add(look.scale(maximumRange + movementExtension));
        HitResult blockHit = player.level().clip(new ClipContext(
                eye,
                maximumEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? maximumEnd : blockHit.getLocation();
        if (eye.distanceToSqr(end) < eye.distanceToSqr(start)) {
            return java.util.List.of();
        }

        AABB search = new AABB(start, start)
                .inflate(HITBOX_MARGIN * 0.5D)
                .expandTowards(end.subtract(start))
                .inflate(1.0D);
        return player.level().getEntities(player, search, target -> isAttackableBy(player, target))
                .stream()
                .filter(target -> distanceAlongSpear(start, end, target)
                        .stream()
                        .anyMatch(distance -> distance <= start.distanceTo(end) + HITBOX_MARGIN))
                .sorted(Comparator.comparingDouble(target -> distanceAlongSpear(start, end, target).orElse(Double.MAX_VALUE)))
                .toList();
    }

    private static boolean isAttackableBy(Player player, Entity target) {
        if (!target.isAlive() || target.isInvulnerable() || !target.canBeHitByProjectile()) {
            return false;
        }
        if (target instanceof Player otherPlayer && !player.canHarmPlayer(otherPlayer)) {
            return false;
        }
        return !player.isPassengerOfSameVehicle(target);
    }

    private static OptionalDouble distanceAlongSpear(Vec3 start, Vec3 end, Entity target) {
        return target.getBoundingBox().inflate(target.getPickRadius() + HITBOX_MARGIN).clip(start, end)
                .map(hit -> OptionalDouble.of(start.distanceTo(hit)))
                .orElseGet(OptionalDouble::empty);
    }

    private ChargeProfile profile() {
        Tier tier = getTier();
        if (tier == Tiers.NETHERITE) return new ChargeProfile(1.2D, 8, 50, 110, 175, 7.0D);
        if (tier == Tiers.DIAMOND) return new ChargeProfile(1.075D, 10, 60, 130, 200, 7.5D);
        if (tier == Tiers.IRON) return new ChargeProfile(0.95D, 12, 50, 135, 225, 8.0D);
        if (tier == Tiers.GOLD) return new ChargeProfile(0.7D, 14, 70, 170, 275, 10.0D);
        if (tier == ModTiers.COPPER) return new ChargeProfile(0.82D, 13, 80, 165, 250, 9.0D);
        if (tier == Tiers.STONE) return new ChargeProfile(0.82D, 14, 90, 180, 275, 10.0D);
        return new ChargeProfile(0.7D, 15, 100, 200, 300, 14.0D);
    }

    public int engagedEndTicks() {
        ChargeProfile profile = profile();
        return profile.startupTicks() + profile.dismountMaximumTicks();
    }

    public int tiredEndTicks() {
        ChargeProfile profile = profile();
        return profile.startupTicks() + profile.knockbackMaximumTicks();
    }

    public int disengagedEndTicks() {
        return profile().totalUseTicks();
    }

    private static Vec3 amplifiedKineticMovement(Entity entity) {
        if (!(entity instanceof Player) && entity.isPassenger()) {
            entity = entity.getRootVehicle();
        }
        return lastTickMovement(entity).scale(20.0D);
    }

    private static Vec3 lastTickMovement(Entity entity) {
        return entity.position().subtract(new Vec3(entity.xo, entity.yo, entity.zo));
    }

    private static void applyLunge(ItemStack stack, Player player) {
        int level = player.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(LUNGE)
                .map(stack::getEnchantmentLevel).orElse(0);
        level = Math.min(level, 3);
        if (level <= 0 || player.isPassenger() || player.isFallFlying() || player.isInWater()
                || (player.getFoodData().getFoodLevel() < 6 && !player.getAbilities().mayfly)) {
            return;
        }
        Vec3 direction = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() < 1.0E-7D) return;
        direction = direction.scale(0.458D * level);
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

    private record ChargeProfile(
            double damageMultiplier,
            int startupTicks,
            int dismountMaximumTicks,
            int knockbackMaximumTicks,
            int damageMaximumTicks,
            double dismountSpeed
    ) {
        int totalUseTicks() {
            return startupTicks + damageMaximumTicks;
        }
    }
}
