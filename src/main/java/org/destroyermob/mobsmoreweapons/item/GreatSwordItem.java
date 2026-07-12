package org.destroyermob.mobsmoreweapons.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.destroyermob.mobsmoreweapons.MoreWeapons;
import org.destroyermob.mobsmoreweapons.combat.GreatSwordSweepSystem;

public class GreatSwordItem extends SwordItem {
    public static final ResourceLocation SWEEP_DAMAGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            MoreWeapons.MOD_ID,
            "great_sword_sweep_damage"
    );
    public static final ResourceLocation REACH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            MoreWeapons.MOD_ID,
            "great_sword_reach"
    );

    public GreatSwordItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return GreatSwordSweepSystem.PREPARATION_TICKS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND
                || stack.getDamageValue() >= stack.getMaxDamage() - 1
                || player.getAttackStrengthScale(0.0F) < 1.0F
                || player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide && !GreatSwordSweepSystem.beginCharge(player, stack)) {
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!level.isClientSide && entity instanceof Player player) {
            GreatSwordSweepSystem.cancelCharge(player, stack);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            GreatSwordSweepSystem.completeCharge(player, stack);
        }
        return stack;
    }

    @Override
    public AABB getSweepHitBox(ItemStack stack, Player player, Entity target) {
        return target.getBoundingBox().inflate(1.5D, 0.25D, 1.5D);
    }
}
