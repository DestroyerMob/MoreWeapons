package org.destroyermob.mobsmoreweapons.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

// For future custom logic
public class KatanaItem extends SwordItem {
    private static final int BLOCK_USE_DURATION_TICKS = 72000;
    private static final float OLD_BLOCKING_DAMAGE_OFFSET = 1.0F;
    private static final float OLD_BLOCKING_DAMAGE_MULTIPLIER = 0.5F;

    public KatanaItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        player.setSprinting(false);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return BLOCK_USE_DURATION_TICKS;
    }

    public static float getOldBlockingDamage(float damage) {
        return Math.min(damage, (OLD_BLOCKING_DAMAGE_OFFSET + damage) * OLD_BLOCKING_DAMAGE_MULTIPLIER);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        return itemAbility == ItemAbilities.SHIELD_BLOCK || super.canPerformAction(stack, itemAbility);
    }

    public static boolean isBlockingWithKatana(LivingEntity entity) {
        return entity.isBlocking() && entity.getUseItem().getItem() instanceof KatanaItem;
    }
}
