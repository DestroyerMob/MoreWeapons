package org.destroyermob.mobsmoreweapons.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.destroyermob.mobsmoreweapons.MoreWeapons;
import org.destroyermob.mobsmoreweapons.combat.BattleAxeHookSystem;

public class BattleAxeItem extends SwordItem {
    public static final ResourceLocation KNOCKBACK_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            MoreWeapons.MOD_ID,
            "battle_axe_knockback"
    );

    public BattleAxeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        // The held pose and release swing are supplied by Better Combat's
        // Player Animator clips; reporting SPEAR here lets Punchy replace them.
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72_000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !player.getOffhandItem().isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }
        if (stack.getDamageValue() >= stack.getMaxDamage() - 1
                || player.getAttackStrengthScale(0.0F) < BattleAxeHookSystem.REQUIRED_ATTACK_STRENGTH
                || player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide && !BattleAxeHookSystem.beginPreparation(player, stack)) {
            return InteractionResultHolder.fail(stack);
        }

        player.setSprinting(false);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!level.isClientSide && entity instanceof Player player) {
            BattleAxeHookSystem.release(player, stack);
        }
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        if (itemAbility == ItemAbilities.SWORD_SWEEP) {
            return false;
        }
        return itemAbility == ItemAbilities.AXE_DIG || super.canPerformAction(stack, itemAbility);
    }

    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.mobsmoreweapons.battle_axe_hook").withStyle(ChatFormatting.GRAY));
    }
}
