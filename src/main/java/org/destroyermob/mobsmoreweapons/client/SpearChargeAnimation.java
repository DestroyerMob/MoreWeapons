package org.destroyermob.mobsmoreweapons.client;

import net.minecraft.util.Mth;
import org.destroyermob.mobsmoreweapons.item.SpearItem;

/** The continuous hold-up curve used by the vanilla spear backport. */
public record SpearChargeAnimation(
        float raiseProgress,
        float raiseProgressStart,
        float raiseProgressMiddle,
        float raiseProgressEnd,
        float swayProgress,
        float lowerProgress,
        float raiseBackProgress,
        float swayIntensity,
        float swayScaleSlow,
        float swayScaleFast
) {
    public static SpearChargeAnimation play(SpearItem spear, float elapsedTicks) {
        int delay = spear.chargeDelayTicks();
        int dismountEnd = spear.dismountEndTicks();
        int knockbackEnd = spear.knockbackEndTicks();
        int damageEnd = spear.damageEndTicks();

        float raise = clampedInverseLerp(elapsedTicks, 0.0F, delay);
        float raiseStart = clampedInverseLerp(raise, 0.0F, 0.5F);
        float raiseMiddle = clampedInverseLerp(raise, 0.5F, 0.8F);
        float raiseEnd = clampedInverseLerp(raise, 0.8F, 1.0F);
        float sway = clampedInverseLerp(elapsedTicks, dismountEnd, knockbackEnd);
        float lower = clampedInverseLerp(elapsedTicks, knockbackEnd, damageEnd - 5.0F);
        if (lower > 0.0F && lower < 1.0F) {
            double wave = Math.sin((20.0D * lower - 11.125D) * Math.PI * 4.0D / 9.0D);
            lower = lower < 0.5F
                    ? (float) (-(Math.pow(2.0D, 20.0D * lower - 10.0D) * wave) / 2.0D)
                    : (float) (Math.pow(2.0D, -20.0D * lower + 10.0D) * wave / 2.0D + 1.0D);
        }
        lower = 1.0F - lower * lower * lower;

        float raiseBack = clampedInverseLerp(elapsedTicks, damageEnd - 5.0F, damageEnd);
        float swayIntensity = 2.0F * (float) Math.sqrt(1.0D + Mth.square(1.0F - sway))
                - 2.0F * ((float) -Math.sqrt(1.0D - sway * sway) + 1.0F);
        float slowWave = Mth.sin(elapsedTicks * 19.0F * Mth.DEG_TO_RAD);
        float fastWave = Mth.sin(elapsedTicks * 30.0F * Mth.DEG_TO_RAD);
        float durationFade = Mth.clamp(
                (1.0F - elapsedTicks / Math.max(Math.max(dismountEnd, knockbackEnd), damageEnd)) * 20.0F,
                0.0F,
                1.0F
        ) * (2.9F - swayIntensity);

        return new SpearChargeAnimation(
                raise,
                raiseStart,
                raiseMiddle,
                raiseEnd,
                sway,
                lower,
                raiseBack,
                swayIntensity,
                slowWave * durationFade,
                fastWave * durationFade
        );
    }

    private static float clampedInverseLerp(float value, float start, float end) {
        return Mth.clamp(Mth.inverseLerp(value, start, end), 0.0F, 1.0F);
    }
}
