package org.destroyermob.mobsmoreweapons.client;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.destroyermob.mobsmoreweapons.MoreWeapons;
import org.destroyermob.mobsmoreweapons.config.MoreWeaponsConfig;
import org.destroyermob.mobsmoreweapons.item.BattleAxeItem;
import org.slf4j.Logger;

/** Routes secondary attacks through Better Combat's own timed attack controller. */
@EventBusSubscriber(modid = MoreWeapons.MOD_ID, value = Dist.CLIENT)
public final class BetterCombatUseAnimationCompatibility {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BATTLE_AXE_HOOK = "bettercombat:two_handed_slam";
    private static final String GREAT_SWORD_SWEEP = "bettercombat:two_handed_spin";

    private static boolean usingBattleAxe;
    private static int battleAxeUseTicks;
    private static Method playAttackAnimation;
    private static Method getAttackCooldownTicks;
    private static Object twoHanded;
    private static boolean unavailable;

    private BetterCombatUseAnimationCompatibility() {
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            usingBattleAxe = false;
            battleAxeUseTicks = 0;
            return;
        }

        boolean current = player.isUsingItem() && player.getUseItem().getItem() instanceof BattleAxeItem;
        if (current) {
            battleAxeUseTicks = player.getTicksUsingItem();
        } else if (usingBattleAxe) {
            if (battleAxeUseTicks >= MoreWeaponsConfig.BATTLE_AXE_HOOK_PREPARATION_TICKS.get()) {
                play(player, BATTLE_AXE_HOOK, 0.5F);
            }
            battleAxeUseTicks = 0;
        }
        usingBattleAxe = current;
    }

    public static void playGreatSwordSweep(LocalPlayer player) {
        play(player, GREAT_SWORD_SWEEP, 0.5F);
    }

    private static void play(LocalPlayer player, String animation, float upswingRate) {
        if (unavailable || !ModList.get().isLoaded("bettercombat")) {
            return;
        }
        try {
            resolveApi();
            float swingDuration = ((Number) getAttackCooldownTicks.invoke(null, player)).floatValue();
            playAttackAnimation.invoke(player, animation, twoHanded, swingDuration, upswingRate);
        } catch (ReflectiveOperationException exception) {
            unavailable = true;
            LOGGER.warn("Could not use Better Combat's attack controller; disabling compatibility.", exception);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void resolveApi() throws ReflectiveOperationException {
        if (playAttackAnimation != null) {
            return;
        }
        Class<?> animatable = Class.forName("net.bettercombat.client.animation.PlayerAttackAnimatable");
        Class<? extends Enum> animatedHand = (Class<? extends Enum>) Class.forName("net.bettercombat.logic.AnimatedHand");
        Class<?> attackHelper = Class.forName("net.bettercombat.logic.PlayerAttackHelper");
        twoHanded = Enum.valueOf(animatedHand, "TWO_HANDED");
        playAttackAnimation = animatable.getMethod(
                "playAttackAnimation",
                String.class,
                animatedHand,
                float.class,
                float.class
        );
        getAttackCooldownTicks = attackHelper.getMethod("getAttackCooldownTicksCapped", net.minecraft.world.entity.player.Player.class);
    }
}
