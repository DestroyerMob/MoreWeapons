package org.destroyermob.mobsmoreweapons.event;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import org.destroyermob.mobsmoreweapons.item.KatanaItem;

public final class KatanaBlockingEvents {
    private KatanaBlockingEvents() {
    }

    public static void onLivingShieldBlock(LivingShieldBlockEvent event) {
        if (!event.getOriginalBlock() || !event.getBlocked() || !KatanaItem.isBlockingWithKatana(event.getEntity())) {
            return;
        }

        float finalDamage = KatanaItem.getOldBlockingDamage(event.getOriginalBlockedDamage());
        event.setBlockedDamage(event.getOriginalBlockedDamage() - finalDamage);
        event.setShieldDamage(0.0F);
        resetAttackTimer(event.getEntity());
    }

    private static void resetAttackTimer(LivingEntity entity) {
        if (entity.level().isClientSide || !(entity instanceof Player player)) {
            return;
        }

        InteractionHand hand = player.getUsedItemHand();
        player.swing(hand, true);
        player.resetAttackStrengthTicker();
    }
}
