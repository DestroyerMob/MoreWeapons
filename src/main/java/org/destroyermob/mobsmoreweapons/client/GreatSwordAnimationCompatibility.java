package org.destroyermob.mobsmoreweapons.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.destroyermob.mobsmoreweapons.MoreWeapons;
import org.destroyermob.mobsmoreweapons.item.GreatSwordItem;

@EventBusSubscriber(modid = MoreWeapons.MOD_ID, value = Dist.CLIENT)
public final class GreatSwordAnimationCompatibility {
    private static final int MAX_WAIT_TICKS = 10;
    private static int pendingSweepTicks;

    private GreatSwordAnimationCompatibility() {
    }

    public static void triggerSweep() {
        pendingSweepTicks = MAX_WAIT_TICKS;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (pendingSweepTicks <= 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || --pendingSweepTicks <= 0) {
            pendingSweepTicks = 0;
            return;
        }
        if (minecraft.player.isUsingItem()
                || !(minecraft.player.getMainHandItem().getItem() instanceof GreatSwordItem)) {
            return;
        }

        BetterCombatUseAnimationCompatibility.playGreatSwordSweep(minecraft.player);
        pendingSweepTicks = 0;
    }
}
