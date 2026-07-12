package org.destroyermob.mobsmoreweapons.entity;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.destroyermob.mobsmoreweapons.item.ModItems;
import org.destroyermob.mobsmoreweapons.network.KnifePickupPayload;

public final class ThrownKnife extends AbstractArrow implements ItemSupplier {
    private static final String SOURCE_SLOT_TAG = "SourceInventorySlot";
    private static final int NO_SOURCE_SLOT = -1;
    private static final int OFF_HAND_SOURCE_SLOT = -2;
    private static final int HOTBAR_SIZE = 9;
    private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK =
            SynchedEntityData.defineId(ThrownKnife.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Byte> DATA_LOYALTY =
            SynchedEntityData.defineId(ThrownKnife.class, EntityDataSerializers.BYTE);
    private boolean dealtDamage;
    private int clientSideReturnTickCount;
    private int sourceInventorySlot = NO_SOURCE_SLOT;

    public ThrownKnife(EntityType<? extends ThrownKnife> entityType, Level level) {
        super(entityType, level);
    }

    public ThrownKnife(Level level, Player owner, ItemStack stack, float damage, InteractionHand sourceHand) {
        super(ModEntityTypes.THROWN_KNIFE.get(), owner, level, stack, stack);
        setItem(stack);
        entityData.set(DATA_LOYALTY, getLoyaltyFromItem(stack));
        setBaseDamage(damage);
        sourceInventorySlot = sourceHand == InteractionHand.OFF_HAND
                ? OFF_HAND_SOURCE_SLOT
                : owner.getInventory().selected;
        pickup = Pickup.ALLOWED;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ITEM_STACK, getDefaultPickupItem());
        builder.define(DATA_LOYALTY, (byte) 0);
    }

    @Override
    public void tick() {
        if (inGroundTime > 0) {
            dealtDamage = true;
        }

        Entity owner = getOwner();
        int loyalty = entityData.get(DATA_LOYALTY);
        if (loyalty > 0 && (dealtDamage || isNoPhysics()) && owner != null) {
            if (!isAcceptableReturnOwner(owner)) {
                if (!level().isClientSide && pickup == Pickup.ALLOWED) {
                    spawnAtLocation(getPickupItem(), 0.1F);
                }
                discard();
            } else {
                setNoPhysics(true);
                Vec3 ownerOffset = owner.getEyePosition().subtract(position());
                setPosRaw(getX(), getY() + ownerOffset.y * 0.015D * loyalty, getZ());
                if (level().isClientSide) {
                    yOld = getY();
                }
                double acceleration = 0.05D * loyalty;
                setDeltaMovement(getDeltaMovement().scale(0.95D)
                        .add(ownerOffset.normalize().scale(acceleration)));
                if (clientSideReturnTickCount == 0) {
                    playSound(SoundEvents.TRIDENT_RETURN, 10.0F, 1.25F);
                }
                clientSideReturnTickCount++;
            }
        }
        super.tick();
    }

    private static boolean isAcceptableReturnOwner(Entity owner) {
        return owner.isAlive() && (!(owner instanceof ServerPlayer player) || !player.isSpectator());
    }

    @Override
    protected void tickDespawn() {
        // A thrown knife remains in the world until it is retrieved.
    }

    @Nullable
    @Override
    protected EntityHitResult findHitEntity(Vec3 startVec, Vec3 endVec) {
        return dealtDamage ? null : super.findHitEntity(startVec, endVec);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        Entity owner = getOwner();
        DamageSource source = damageSources().arrow(this, owner == null ? this : owner);
        float damage = (float) getBaseDamage();
        ItemStack weapon = getWeaponItem();
        if (level() instanceof ServerLevel serverLevel && weapon != null) {
            damage = EnchantmentHelper.modifyDamage(serverLevel, weapon, target, source, damage);
        }

        dealtDamage = true;
        if (target.hurt(source, damage)) {
            if (level() instanceof ServerLevel serverLevel && weapon != null) {
                EnchantmentHelper.doPostAttackEffectsWithItemSource(serverLevel, target, source, weapon);
            }
            if (target instanceof LivingEntity livingTarget) {
                doKnockback(livingTarget, source);
                doPostHurtEffects(livingTarget);
            }
        }

        setDeltaMovement(getDeltaMovement().multiply(-0.05D, -0.1D, -0.05D));
        playSound(SoundEvents.TRIDENT_HIT_GROUND, 1.0F, 1.35F);
    }

    public void setItem(ItemStack stack) {
        entityData.set(DATA_ITEM_STACK, stack.copyWithCount(1));
    }

    @Override
    public ItemStack getItem() {
        return entityData.get(DATA_ITEM_STACK);
    }

    @Override
    public ItemStack getWeaponItem() {
        return getPickupItemStackOrigin();
    }

    @Override
    protected boolean tryPickup(Player player) {
        if (pickup == Pickup.ALLOWED && ownedBy(player) && tryRestoreToSourceSlot(player)) {
            return true;
        }
        return super.tryPickup(player)
                || isNoPhysics() && ownedBy(player) && player.getInventory().add(getPickupItem());
    }

    private boolean tryRestoreToSourceSlot(Player player) {
        ItemStack pickupItem = getPickupItem();
        if (pickupItem.isEmpty()) {
            return false;
        }
        if (sourceInventorySlot == OFF_HAND_SOURCE_SLOT) {
            if (!player.getOffhandItem().isEmpty()) {
                return false;
            }
            player.setItemInHand(InteractionHand.OFF_HAND, pickupItem);
            notifyPickupAnimation(player, InteractionHand.OFF_HAND);
            return true;
        }
        if (sourceInventorySlot < 0 || sourceInventorySlot >= HOTBAR_SIZE
                || !player.getInventory().getItem(sourceInventorySlot).isEmpty()) {
            return false;
        }
        player.getInventory().setItem(sourceInventorySlot, pickupItem);
        if (player.getInventory().selected == sourceInventorySlot) {
            notifyPickupAnimation(player, InteractionHand.MAIN_HAND);
        }
        return true;
    }

    private static void notifyPickupAnimation(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new KnifePickupPayload(hand));
        }
    }

    @Override
    public void playerTouch(Player player) {
        if (ownedBy(player) || getOwner() == null) {
            super.playerTouch(player);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("DealtDamage", dealtDamage);
        if (sourceInventorySlot != NO_SOURCE_SLOT) {
            compound.putInt(SOURCE_SLOT_TAG, sourceInventorySlot);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setItem(getPickupItemStackOrigin());
        dealtDamage = compound.getBoolean("DealtDamage");
        sourceInventorySlot = compound.contains(SOURCE_SLOT_TAG)
                ? compound.getInt(SOURCE_SLOT_TAG)
                : NO_SOURCE_SLOT;
        entityData.set(DATA_LOYALTY, getLoyaltyFromItem(getPickupItemStackOrigin()));
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.IRONKNIFE.get());
    }

    private byte getLoyaltyFromItem(ItemStack stack) {
        return level() instanceof ServerLevel serverLevel
                ? (byte) Mth.clamp(
                        EnchantmentHelper.getTridentReturnToOwnerAcceleration(serverLevel, stack, this),
                        0,
                        127
                )
                : 0;
    }
}
