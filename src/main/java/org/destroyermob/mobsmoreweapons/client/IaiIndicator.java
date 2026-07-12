package org.destroyermob.mobsmoreweapons.client;

import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.destroyermob.mobsmoreweapons.MoreWeapons;
import org.destroyermob.mobsmoreweapons.network.IaiStatePayload;

@EventBusSubscriber(modid = MoreWeapons.MOD_ID, value = Dist.CLIENT)
public final class IaiIndicator {
    private static final ResourceLocation PROGRESS_SPRITE = ResourceLocation.fromNamespaceAndPath(
            MoreWeapons.MOD_ID,
            "hud/crosshair_attack_indicator_progress"
    );
    private static final int INDICATOR_WIDTH = 18;
    private static final int INDICATOR_HEIGHT = 6;
    private static boolean active;
    private static boolean charging;
    private static boolean primed;
    private static boolean ready;
    private static int chargeTicks;
    private static int requiredTicks = 1;
    private static long receivedTick;
    private static ClientLevel currentLevel;

    private IaiIndicator() {
    }

    public static void acceptState(IaiStatePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean becameReady = payload.active() && payload.ready() && (!active || !ready);
        active = payload.active();
        charging = payload.charging();
        primed = payload.primed();
        ready = payload.ready();
        chargeTicks = Math.max(0, payload.chargeTicks());
        requiredTicks = Math.max(1, payload.requiredTicks());
        currentLevel = minecraft.level;
        receivedTick = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        if (becameReady && minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.65F, 1.7F);
        }
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != currentLevel) {
            clear(minecraft.level);
        }
        if (!active || minecraft.player == null || minecraft.screen != null || minecraft.options.hideGui) {
            return;
        }

        long elapsed = ready || primed || !charging
                ? 0L
                : Math.max(0L, minecraft.level.getGameTime() - receivedTick);
        double rawProgress = charging ? (chargeTicks + elapsed) / (double) requiredTicks : 0.0D;
        double progress = ready ? 1.0D : Math.min(0.98D, Math.max(0.0D, rawProgress));
        renderAttackIndicatorProgress(event.getGuiGraphics(), minecraft, progress);
    }

    private static void renderAttackIndicatorProgress(GuiGraphics graphics, Minecraft minecraft, double progress) {
        if (minecraft.options.attackIndicator().get() != AttackIndicatorStatus.CROSSHAIR
                || !vanillaAttackIndicatorVisible(minecraft)) {
            return;
        }

        int progressWidth = (int) Math.floor(INDICATOR_WIDTH * progress);
        if (progressWidth <= 0) {
            return;
        }
        int x = graphics.guiWidth() / 2 - 9;
        int y = graphics.guiHeight() / 2 + 8;
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blitSprite(
                PROGRESS_SPRITE,
                INDICATOR_WIDTH,
                INDICATOR_HEIGHT,
                0,
                0,
                x,
                y,
                progressWidth,
                INDICATOR_HEIGHT
        );
    }

    private static boolean vanillaAttackIndicatorVisible(Minecraft minecraft) {
        float attackStrength = minecraft.player.getAttackStrengthScale(0.0F);
        if (attackStrength < 1.0F) {
            return true;
        }
        return minecraft.crosshairPickEntity instanceof LivingEntity target
                && target.isAlive()
                && minecraft.player.getCurrentItemAttackStrengthDelay() > 5.0F;
    }

    private static void clear(ClientLevel level) {
        active = false;
        charging = false;
        primed = false;
        ready = false;
        chargeTicks = 0;
        requiredTicks = 1;
        receivedTick = 0L;
        currentLevel = level;
    }
}
