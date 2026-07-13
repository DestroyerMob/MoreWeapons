package org.destroyermob.mobsmoreweapons.client;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.destroyermob.mobsmoreweapons.MoreWeapons;
import org.destroyermob.mobsmoreweapons.item.GreatSwordItem;
import org.slf4j.Logger;

@EventBusSubscriber(modid = MoreWeapons.MOD_ID, value = Dist.CLIENT)
public final class GreatSwordAnimationCompatibility {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_WAIT_TICKS = 10;
    private static final String CHARGED_SWEEP_CLIP = "mobsmoreweapons_greatsword_charged_sweep";
    private static int pendingSweepTicks;
    private static Method resolveCustomClip;
    private static Method setSourceHand;
    private static Method playClip;
    private static Field poseHandler;
    private static boolean punchyUnavailable;

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

        playChargedSweep(minecraft);
        pendingSweepTicks = 0;
    }

    private static void playChargedSweep(Minecraft minecraft) {
        if (punchyUnavailable || !ModList.get().isLoaded("punchy")) {
            return;
        }
        try {
            if (resolveCustomClip == null) {
                Class<?> manager = Class.forName("punchy.client.animation.PunchyAnimationManager");
                Class<?> clip = Class.forName("punchy.client.animation.data.AnimationClip");
                Class<?> handler = Class.forName("punchy.client.animation.PoseHandler");
                resolveCustomClip = manager.getMethod("resolveCustomClip", String.class);
                setSourceHand = manager.getMethod("setSourceHand", Minecraft.class, InteractionHand.class);
                poseHandler = manager.getField("POSE_HANDLER");
                playClip = handler.getMethod("play", clip);
            }
            Object clip = resolveCustomClip.invoke(null, CHARGED_SWEEP_CLIP);
            Object handler = poseHandler.get(null);
            if (clip == null || handler == null) {
                return;
            }
            setSourceHand.invoke(null, minecraft, InteractionHand.MAIN_HAND);
            playClip.invoke(handler, clip);
        } catch (ReflectiveOperationException exception) {
            punchyUnavailable = true;
            LOGGER.warn("Could not trigger Punchy's greatsword sweep animation; disabling compatibility.", exception);
        }
    }
}
