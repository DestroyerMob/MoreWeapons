package org.destroyermob.mobsmoreweapons.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.destroyermob.mobsmoreweapons.entity.ThrownKnife;

public class KnifeItem extends SwordItem {
    private static final int MIN_THROW_TICKS = 10;
    private static final int FULL_CHARGE_TICKS = 20;

    public KnifeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getDamageValue() >= stack.getMaxDamage() - 1) {
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }

        int chargeTicks = getUseDuration(stack, entity) - timeLeft;
        if (chargeTicks < MIN_THROW_TICKS) {
            return;
        }

        float charge = throwCharge(chargeTicks);
        if (!level.isClientSide) {
            InteractionHand hand = entity.getUsedItemHand();
            EquipmentSlot slot = LivingEntity.getSlotForHand(hand);
            stack.hurtAndBreak(1, player, slot);

            ThrownKnife thrownKnife = new ThrownKnife(level, player, stack, thrownDamage(charge));
            thrownKnife.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, throwVelocity(charge), 1.0F);
            if (player.hasInfiniteMaterials()) {
                thrownKnife.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY;
            }

            level.addFreshEntity(thrownKnife);
            level.playSound(null, thrownKnife, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.8F, 1.35F);
            if (!player.hasInfiniteMaterials()) {
                player.setItemInHand(hand, ItemStack.EMPTY);
            }
        }

        player.awardStat(Stats.ITEM_USED.get(this));
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        return itemAbility != ItemAbilities.SWORD_SWEEP && super.canPerformAction(stack, itemAbility);
    }

    private float thrownDamage(float charge) {
        float fullDamage = 2.0F + getTier().getAttackDamageBonus();
        return fullDamage * (0.5F + 0.5F * charge);
    }

    private static float throwVelocity(float charge) {
        return 1.5F + 1.5F * charge;
    }

    private static float throwCharge(int chargeTicks) {
        float progress = Mth.clamp((float) chargeTicks / FULL_CHARGE_TICKS, 0.0F, 1.0F);
        return (progress * progress + progress * 2.0F) / 3.0F;
    }
}
