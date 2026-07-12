package org.destroyermob.mobsmoreweapons.client;

import com.mojang.logging.LogUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.destroyermob.mobsmoreweapons.MoreWeapons;
import org.destroyermob.mobsmoreweapons.item.KnifeItem;
import org.destroyermob.mobsmoreweapons.network.KnifePickupPayload;
import org.slf4j.Logger;

@EventBusSubscriber(modid = MoreWeapons.MOD_ID, value = Dist.CLIENT)
public final class KnifePickupAnimationCompatibility {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_WAIT_TICKS = 20;
    private static InteractionHand pendingHand;
    private static int remainingWaitTicks;
    private static Method clearSourceHand;
    private static Method triggerPickupEquipCycle;
    private static boolean punchyUnavailable;

    private KnifePickupAnimationCompatibility() {
    }

    public static void accept(KnifePickupPayload payload) {
        pendingHand = payload.hand();
        remainingWaitTicks = MAX_WAIT_TICKS;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (pendingHand == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || remainingWaitTicks-- <= 0) {
            clearPending();
            return;
        }
        ItemStack returnedKnife = minecraft.player.getItemInHand(pendingHand);
        if (!(returnedKnife.getItem() instanceof KnifeItem)) {
            return;
        }
        notifyPunchy(minecraft, pendingHand);
        clearPending();
    }

    private static void notifyPunchy(Minecraft minecraft, InteractionHand hand) {
        if (punchyUnavailable || !ModList.get().isLoaded("punchy")) {
            return;
        }
        try {
            if (clearSourceHand == null) {
                clearSourceHand = Class.forName("punchy.client.animation.PunchyAnimationManager")
                        .getMethod("clearSourceHand");
            }
            if (triggerPickupEquipCycle == null) {
                triggerPickupEquipCycle = Class.forName("punchy.client.state.HandEquipStateMachine")
                        .getMethod("triggerPickupEquipCycle", Minecraft.class, InteractionHand.class);
            }
            clearSourceHand.invoke(null);
            triggerPickupEquipCycle.invoke(null, minecraft, hand);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            punchyUnavailable = true;
            LOGGER.warn("Could not notify Punchy about a returned knife; disabling pickup animation compatibility.", exception);
        }
    }

    private static void clearPending() {
        pendingHand = null;
        remainingWaitTicks = 0;
    }
}
