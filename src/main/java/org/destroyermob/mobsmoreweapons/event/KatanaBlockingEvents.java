package org.destroyermob.mobsmoreweapons.event;

import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import org.destroyermob.mobsmoreweapons.item.KatanaItem;

public final class KatanaBlockingEvents {
    private KatanaBlockingEvents() {
    }

    public static void onLivingShieldBlock(LivingShieldBlockEvent event) {
        if (!event.getOriginalBlock() || !event.getBlocked() || !KatanaItem.isBlockingWithKatana(event.getEntity())) {
            return;
        }

        event.setBlockedDamage(event.getOriginalBlockedDamage() * KatanaItem.BLOCKED_DAMAGE_FRACTION);
        event.setShieldDamage(0.0F);
    }
}
