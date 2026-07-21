package org.destroyermob.mobsmoreweapons.item;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.stats.Stats;
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
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.border.WorldBorder;
import org.destroyermob.mobsmoreweapons.MoreWeapons;
import org.destroyermob.mobsmoreweapons.item.tier.ModTiers;
import org.destroyermob.mobsmoreweapons.registry.ModSoundEvents;

/** A 1.21.1 backport of the vanilla spear's jab and velocity-driven charge. */
public class SpearItem extends SwordItem {
    public static final double MINIMUM_ATTACK_RANGE = 2.0D;
    public static final double MAXIMUM_ATTACK_RANGE = 4.5D;
    public static final double KINETIC_HITBOX_MARGIN = 0.125D;
    public static final double JAB_HITBOX_MARGIN = 0.25D;
    // Exact thresholds used by Spear Backport 1.8.0 for Minecraft 1.21.1.
    private static final double MINIMUM_DAMAGE_RELATIVE_SPEED = 4.6D;
    private static final double MINIMUM_KNOCKBACK_SPEED = 5.1D;
    private static final float AERIAL_DIVE_FALL_DISTANCE = 2.0F;
    private static final int CONTACT_COOLDOWN_TICKS = 10;
    private static final float BETTER_COMBAT_MINIMUM_ATTACK_STRENGTH = 0.5F;
    private static final Map<ServerPlayer, Long> BETTER_COMBAT_LUNGE_TIMES = new WeakHashMap<>();
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
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        // Vanilla kinetic weapons remain in use after their damage window
        // expires; the later animation stages are not the item-use duration.
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        boolean started = player.isUsingItem()
                && player.getUsedItemHand() == hand
                && player.getUseItem() == stack;
        if (started && !level.isClientSide) {
            level.playSound(null, player, useSound(stack), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        if (started) {
            MoreWeapons.LOGGER.debug(
                    "Started spear charge for {} on {}",
                    player.getScoreboardName(),
                    level.isClientSide ? "client" : "server"
            );
        }
        return started ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // SwordItem hard-codes MAINHAND here. Resolve the actual held stack so
        // offhand charges damage the right item and fire the right break event.
        EquipmentSlot slot = attacker.getItemBySlot(EquipmentSlot.OFFHAND) == stack
                ? EquipmentSlot.OFFHAND
                : EquipmentSlot.MAINHAND;
        stack.hurtAndBreak(1, attacker, slot);
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

        int activeTicks = elapsed - profile.startupTicks();
        if (activeTicks > profile.maximumEffectTicks()) {
            // Keep the item-use lifecycle running for its animation, but stop
            // ray casts and entity scans once no charge effect can still fire.
            return;
        }

        Vec3 view = player.getLookAngle().normalize();
        Vec3 chargeDirection = chargeDirection(player, view);
        Vec3 attackerMovement = amplifiedKineticMovement(player);
        double attackerSpeed = chargeDirection.dot(attackerMovement);
        double baseAttackDamage = player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        EquipmentSlot slot = player.getUsedItemHand() == InteractionHand.OFF_HAND
                ? EquipmentSlot.OFFHAND
                : EquipmentSlot.MAINHAND;
        SpearUser spearUser = (SpearUser) player;
        Set<UUID> processedTargets = new HashSet<>();
        SoundEvent contactSound = hitSound(stack);

        boolean hit = false;
        for (Entity target : targetsAlongSpear(player, MINIMUM_ATTACK_RANGE, KINETIC_HITBOX_MARGIN)) {
            Entity contactTarget = canonicalContactTarget(target);
            if (!processedTargets.add(contactTarget.getUUID())) {
                continue;
            }
            if (spearUser.mobsmoreweapons$maintainSpearContactCooldown(contactTarget, CONTACT_COOLDOWN_TICKS)) {
                continue;
            }

            double targetSpeed = chargeDirection.dot(amplifiedKineticMovement(contactTarget));
            double relativeSpeed = Math.max(0.0D, attackerSpeed - targetSpeed);
            boolean dismount = activeTicks <= profile.dismountMaximumTicks()
                    && attackerSpeed >= profile.dismountSpeed();
            // Vanilla gates knockback on the attacker's forward speed. Only
            // damage uses attacker/target relative speed.
            boolean knockback = activeTicks <= profile.knockbackMaximumTicks()
                    && attackerSpeed >= MINIMUM_KNOCKBACK_SPEED;
            boolean dealDamage = activeTicks <= profile.damageMaximumTicks()
                    && relativeSpeed >= MINIMUM_DAMAGE_RELATIVE_SPEED;
            MoreWeapons.LOGGER.debug(
                    "Spear charge contact: player={}, target={}({}), elapsed={}, sprinting={}, pitch={}, horizontalSpeed={}, attackerSpeed={}, targetSpeed={}, damage={}, knockback={}, dismount={}",
                    player.getScoreboardName(), target.getStringUUID(), target.getType(), elapsed, player.isSprinting(),
                    player.getXRot(), attackerMovement.horizontalDistance(), attackerSpeed, targetSpeed,
                    dealDamage, knockback, dismount
            );
            if (!dismount && !knockback && !dealDamage) {
                continue;
            }

            float damage = (float) baseAttackDamage + Mth.floor(relativeSpeed * profile.damageMultiplier());
            boolean targetHit = stabTarget(serverLevel, stack, player, target, damage, dealDamage, knockback, dismount, slot);
            if (targetHit) {
                spearUser.mobsmoreweapons$startSpearContactCooldown(contactTarget, CONTACT_COOLDOWN_TICKS);
                hit = true;
            }
            if (stack.isEmpty()) {
                break;
            }
        }
        if (hit) {
            serverLevel.playSound(null, player, contactSound, SoundSource.PLAYERS, 1.0F, 1.0F);
            serverLevel.broadcastEntityEvent(player, (byte) 2);
        }
    }

    /** Server-side equivalent of vanilla's dedicated STAB player action. */
    public static void jab(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!player.isAlive() || player.isSpectator() || player.isUsingItem()
                || !isSpear(stack) || player.getAttackStrengthScale(0.0F) < 1.0F) {
            MoreWeapons.LOGGER.debug(
                    "Rejected spear jab: player={}, alive={}, spectator={}, using={}, spear={}, strength={}",
                    player.getScoreboardName(), player.isAlive(), player.isSpectator(), player.isUsingItem(),
                    isSpear(stack), player.getAttackStrengthScale(0.0F)
            );
            return;
        }

        ServerLevel level = player.serverLevel();
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        java.util.List<Entity> targets = targetsAlongSpear(player, MINIMUM_ATTACK_RANGE, JAB_HITBOX_MARGIN);
        MoreWeapons.LOGGER.debug("Processing spear jab: player={}, targets={}, damage={}", player.getScoreboardName(), targets.size(), damage);
        level.playSound(null, player, attackSound(stack), SoundSource.PLAYERS, 1.0F, 1.0F);
        SoundEvent contactSound = hitSound(stack);
        boolean hit = false;
        Set<UUID> processedTargets = new HashSet<>();
        for (Entity target : targets) {
            if (!processedTargets.add(canonicalContactTarget(target).getUUID())) {
                continue;
            }
            hit |= stabTarget(level, stack, player, target, damage, true, true, false, EquipmentSlot.MAINHAND);
            if (stack.isEmpty()) {
                break;
            }
        }
        if (hit) {
            level.playSound(null, player, contactSound, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        player.resetAttackStrengthTicker();
        applyLunge(stack, player);
        player.swing(InteractionHand.MAIN_HAND, false);
    }

    /**
     * Applies only Lunge for a Better Combat upswing. Better Combat remains
     * responsible for the attack and damage; this method exists solely to
     * restore the post-stab enchantment effect that its attack path bypasses.
     */
    public static boolean tryApplyBetterCombatLunge(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!player.isAlive()
                || player.isSpectator()
                || player.isUsingItem()
                || !(stack.getItem() instanceof SpearItem spear)
                || player.getAttackStrengthScale(0.0F) < BETTER_COMBAT_MINIMUM_ATTACK_STRENGTH) {
            return false;
        }

        long gameTime = player.serverLevel().getGameTime();
        Long lastLungeTime = BETTER_COMBAT_LUNGE_TIMES.get(player);
        int minimumInterval = Math.max(4, spear.swingAnimationTicks() - 1);
        if (lastLungeTime != null && gameTime - lastLungeTime < minimumInterval) {
            return false;
        }

        if (!applyLunge(stack, player)) {
            return false;
        }
        BETTER_COMBAT_LUNGE_TIMES.put(player, gameTime);
        MoreWeapons.LOGGER.debug("Applied Better Combat spear lunge for {}", player.getScoreboardName());
        return true;
    }

    public static boolean isSpear(ItemStack stack) {
        return stack.getItem() instanceof SpearItem || stack.is(SPEARS);
    }

    private static boolean stabTarget(
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
        float attackStrength = player.isUsingItem() ? 1.0F : player.getAttackStrengthScale(0.5F);
        float enchantmentDamage = attackStrength
                * (EnchantmentHelper.modifyDamage(level, stack, target, source, baseDamage) - baseDamage);
        if (!player.isUsingItem()) {
            baseDamage *= 0.2F + attackStrength * attackStrength * 0.8F;
        }

        if (knockback
                && target.getType().is(EntityTypeTags.REDIRECTABLE_PROJECTILE)
                && target instanceof Projectile projectile
                && projectile.deflect(ProjectileDeflection.AIM_DEFLECT, player, player, true)) {
            return true;
        }

        float previousHealth = target instanceof LivingEntity living ? living.getHealth() : 0.0F;
        boolean damaged = dealDamage && target.hurt(source, baseDamage + enchantmentDamage);
        if (knockback) {
            knockBackTarget(level, stack, player, target, source, slot);
        }
        boolean dismounted = dismount && target.isPassenger();
        if (dismounted) {
            target.stopRiding();
        }
        if (!damaged && !knockback && !dismounted) {
            return false;
        }

        if (EnchantmentHelper.modifyDamage(level, stack, target, source, enchantmentDamage) > enchantmentDamage) {
            player.magicCrit(target);
        }
        player.setLastHurtMob(target);

        Entity durabilityTarget = target instanceof EnderDragonPart dragonPart ? dragonPart.parentMob : target;
        boolean hurtEnemy = false;
        if (durabilityTarget instanceof LivingEntity living) {
            hurtEnemy = stack.hurtEnemy(living, player);
        }
        if (hurtEnemy) {
            doPostAttackEffectsWithItemSource(level, target, source, stack, slot);
        }
        if (!stack.isEmpty() && durabilityTarget instanceof LivingEntity living) {
            if (hurtEnemy) {
                stack.postHurtEnemy(living, player);
            }
            if (stack.isEmpty()) {
                player.setItemInHand(slot == EquipmentSlot.OFFHAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
        }

        if (target instanceof LivingEntity living) {
            float damageDealt = previousHealth - living.getHealth();
            player.awardStat(Stats.DAMAGE_DEALT, Math.round(damageDealt * 10.0F));
            if (damageDealt > 2.0F) {
                int particles = (int) (damageDealt * 0.5F);
                level.sendParticles(
                        ParticleTypes.DAMAGE_INDICATOR,
                        target.getX(),
                        target.getY(0.5D),
                        target.getZ(),
                        particles,
                        0.1D, 0.0D, 0.1D,
                        0.2D
                );
            }
        }
        player.causeFoodExhaustion(0.1F);
        return true;
    }

    private static void doPostAttackEffectsWithItemSource(
            ServerLevel level,
            Entity target,
            DamageSource source,
            ItemStack stack,
            EquipmentSlot slot
    ) {
        if (target instanceof LivingEntity victim) {
            EnchantmentHelper.runIterationOnEquipment(victim, (enchantment, enchantmentLevel, enchantedItem) ->
                    enchantment.value().doPostAttack(
                            level,
                            enchantmentLevel,
                            enchantedItem,
                            EnchantmentTarget.VICTIM,
                            target,
                            source
                    ));
        }
        Entity sourceEntity = source.getEntity();
        if (sourceEntity instanceof LivingEntity attacker) {
            EnchantmentHelper.runIterationOnItem(stack, slot, attacker, (enchantment, enchantmentLevel, enchantedItem) ->
                    enchantment.value().doPostAttack(
                            level,
                            enchantmentLevel,
                            enchantedItem,
                            EnchantmentTarget.ATTACKER,
                            target,
                            source
                    ));
        }
    }

    private static void knockBackTarget(
            ServerLevel level,
            ItemStack stack,
            Player player,
            Entity target,
            DamageSource source,
            EquipmentSlot slot
    ) {
        double baseKnockback = slot == EquipmentSlot.OFFHAND
                ? player.getAttributeBaseValue(Attributes.ATTACK_KNOCKBACK)
                : player.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
        float knockback = EnchantmentHelper.modifyKnockback(
                level,
                stack,
                target,
                source,
                (float) baseKnockback + 0.4F
        );
        if (target instanceof LivingEntity living) {
            living.knockback(knockback, player.getX() - target.getX(), player.getZ() - target.getZ());
            if (living instanceof ServerPlayer) {
                // hasImpulse only updates tracking clients. The struck player
                // must also receive its own authoritative velocity packet.
                living.hurtMarked = true;
            }
            return;
        }

        double x = player.getX() - target.getX();
        double z = player.getZ() - target.getZ();
        target.hasImpulse = true;
        while (x * x + z * z < 1.0E-5F) {
            x = (Math.random() - Math.random()) * 0.01D;
            z = (Math.random() - Math.random()) * 0.01D;
        }
        Vec3 movement = target.getDeltaMovement();
        Vec3 impulse = new Vec3(x, 0.0D, z).normalize().scale(knockback);
        target.setDeltaMovement(
                movement.x / 2.0D - impulse.x,
                target.onGround() ? Math.min(0.4D, movement.y / 2.0D + knockback) : movement.y,
                movement.z / 2.0D - impulse.z
        );
    }

    private static List<Entity> targetsAlongSpear(Player player, double minimumAttackRange, double hitboxMargin) {
        Vec3 look = player.calculateViewVector(player.getXRot(), player.getYHeadRot());
        Vec3 eye = player.getEyePosition();
        Vec3 start = eye.add(look.scale(minimumAttackRange));
        double movementExtension = Math.max(0.0D, player.getKnownMovement().dot(look));
        Vec3 maximumEnd = eye.add(look.scale(MAXIMUM_ATTACK_RANGE + movementExtension));
        BlockHitResult blockHit = collisionsIncludingWorldBorder(player.level(), new ClipContext(
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

        AABB search = AABB.ofSize(start, hitboxMargin, hitboxMargin, hitboxMargin)
                .expandTowards(end.subtract(start))
                .inflate(1.0D);
        List<Entity> hits = new ArrayList<>();
        for (Entity target : player.level().getEntities(player, search, candidate -> isAttackableBy(player, candidate))) {
            AABB targetBox = target.getBoundingBox();
            if (targetBox.contains(start) || targetBox.clip(start, end).isPresent()) {
                hits.add(target);
                continue;
            }

            Optional<Vec3> marginHit = targetBox.inflate(hitboxMargin).clip(start, end);
            if (marginHit.isEmpty()) {
                continue;
            }

            Vec3 marginPosition = marginHit.get();
            Vec3 targetCenter = targetBox.getCenter();
            BlockHitResult obstruction = collisionsIncludingWorldBorder(player.level(), new ClipContext(
                    marginPosition,
                    targetCenter,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
            ));
            if (obstruction.getType() != HitResult.Type.MISS) {
                targetCenter = obstruction.getLocation();
            }
            if (targetBox.clip(marginPosition, targetCenter).isPresent()) {
                hits.add(target);
            }
        }
        return hits;
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

    private static Entity canonicalContactTarget(Entity target) {
        return target instanceof EnderDragonPart dragonPart ? dragonPart.parentMob : target;
    }

    private static BlockHitResult collisionsIncludingWorldBorder(Level level, ClipContext context) {
        BlockHitResult blockHit = level.clip(context);
        WorldBorder border = level.getWorldBorder();
        if (!border.isWithinBounds(context.getFrom()) || border.isWithinBounds(blockHit.getLocation())) {
            return blockHit;
        }

        Vec3 directionVector = blockHit.getLocation().subtract(context.getFrom());
        Direction direction = Direction.getNearest(directionVector.x, directionVector.y, directionVector.z);
        Vec3 location = blockHit.getLocation();
        Vec3 clamped = new Vec3(
                Mth.clamp(location.x, border.getMinX(), border.getMaxX() - 1.0E-5F),
                location.y,
                Mth.clamp(location.z, border.getMinZ(), border.getMaxZ() - 1.0E-5F)
        );
        return new BlockHitResult(clamped, direction, BlockPos.containing(clamped), false);
    }

    private ChargeProfile profile() {
        Tier tier = getTier();
        if (tier == Tiers.NETHERITE) return new ChargeProfile(1.2D, 8, 50, 70, 175, 7.0D);
        if (tier == Tiers.DIAMOND) return new ChargeProfile(1.075D, 10, 60, 80, 200, 7.5D);
        if (tier == Tiers.IRON) return new ChargeProfile(0.95D, 12, 50, 90, 225, 8.0D);
        if (tier == Tiers.GOLD) return new ChargeProfile(0.7D, 14, 70, 110, 275, 10.0D);
        if (tier == ModTiers.COPPER) return new ChargeProfile(0.82D, 13, 80, 100, 250, 9.0D);
        if (tier == Tiers.STONE) return new ChargeProfile(0.82D, 14, 90, 110, 275, 10.0D);
        return new ChargeProfile(0.7D, 15, 100, 120, 300, 14.0D);
    }

    public int chargeDelayTicks() {
        return profile().startupTicks();
    }

    public int dismountEndTicks() {
        ChargeProfile profile = profile();
        return profile.startupTicks() + profile.dismountMaximumTicks();
    }

    public int knockbackEndTicks() {
        ChargeProfile profile = profile();
        return profile.startupTicks() + profile.knockbackMaximumTicks();
    }

    public int damageEndTicks() {
        return profile().totalUseTicks();
    }

    public float chargeForwardMovement() {
        return 0.38F;
    }

    public int swingAnimationTicks() {
        Tier tier = getTier();
        if (tier == Tiers.NETHERITE) return 23;
        if (tier == Tiers.DIAMOND) return 21;
        if (tier == Tiers.IRON || tier == Tiers.GOLD) return 19;
        if (tier == ModTiers.COPPER) return 17;
        if (tier == Tiers.STONE) return 15;
        return 13;
    }

    private static Vec3 amplifiedKineticMovement(Entity entity) {
        if (!(entity instanceof Player) && entity.isPassenger()) {
            entity = entity.getRootVehicle();
        }
        Vec3 movement = entity instanceof Player player
                ? player.getKnownMovement()
                : entity.position().subtract(entity.xo, entity.yo, entity.zo);
        return movement.scale(20.0D);
    }

    /**
     * Ordinary on-foot movement is a horizontal charge, including sprint
     * jumps, stairs, and short ledges. Mounted, swimming, flying, and genuine
     * falling dives retain the reference mod's full 3-D projection.
     */
    private static Vec3 chargeDirection(Player player, Vec3 view) {
        boolean aerialDive = !player.onGround()
                && player.fallDistance > AERIAL_DIVE_FALL_DISTANCE
                && player.getKnownMovement().y < 0.0D;
        boolean threeDimensionalMovement = player.isPassenger()
                || player.isFallFlying()
                || player.getAbilities().flying
                || player.isSwimming()
                || player.isInWaterOrBubble()
                || player.isInLava()
                || aerialDive;
        if (!threeDimensionalMovement) {
            Vec3 horizontal = new Vec3(view.x, 0.0D, view.z);
            if (horizontal.lengthSqr() > 1.0E-7D) {
                return horizontal.normalize();
            }
        }
        return view;
    }

    private static boolean applyLunge(ItemStack stack, Player player) {
        int level = player.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(LUNGE)
                .map(stack::getEnchantmentLevel).orElse(0);
        level = Math.min(level, 3);
        if (level <= 0 || player.isPassenger() || player.isFallFlying() || player.isInWater()
                || player.getFoodData().getFoodLevel() < 6) {
            return false;
        }
        Vec3 direction = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() < 1.0E-7D) return false;
        direction = direction.scale(0.458D * level);
        player.push(direction.x, 0.0D, direction.z);
        // ServerEntity sends hasImpulse movement to trackers but not back to
        // the owning ServerPlayer; hurtMarked includes the owner as well.
        player.hurtMarked = true;
        player.causeFoodExhaustion(4.0F * level);
        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        player.level().playSound(null, player, ModSoundEvents.SPEAR_LUNGE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private static net.minecraft.sounds.SoundEvent attackSound(ItemStack stack) {
        return isWoodenSpear(stack) ? ModSoundEvents.SPEAR_WOOD_ATTACK.get() : ModSoundEvents.SPEAR_ATTACK.get();
    }

    private static net.minecraft.sounds.SoundEvent hitSound(ItemStack stack) {
        return isWoodenSpear(stack) ? ModSoundEvents.SPEAR_WOOD_HIT.get() : ModSoundEvents.SPEAR_HIT.get();
    }

    private static net.minecraft.sounds.SoundEvent useSound(ItemStack stack) {
        return isWoodenSpear(stack) ? ModSoundEvents.SPEAR_WOOD_USE.get() : ModSoundEvents.SPEAR_USE.get();
    }

    private static boolean isWoodenSpear(ItemStack stack) {
        return stack.getItem() instanceof SpearItem spear && spear.getTier() == Tiers.WOOD;
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

        int maximumEffectTicks() {
            return Math.max(damageMaximumTicks, Math.max(knockbackMaximumTicks, dismountMaximumTicks));
        }
    }
}
