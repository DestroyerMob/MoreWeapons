package org.destroyermob.mobsmoreweapons.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MoreWeaponsConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue PLAYER_BOW_ACCURACY_FIX;
    public static final ModConfigSpec.IntValue BOW_STRAIN_GRACE_TICKS;
    public static final ModConfigSpec.IntValue IAI_CHARGE_TICKS;
    public static final ModConfigSpec.IntValue IAI_PRIMED_TICKS;
    public static final ModConfigSpec.DoubleValue IAI_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue IAI_LUNGE_FORCE;
    public static final ModConfigSpec.IntValue BATTLE_AXE_HOOK_PREPARATION_TICKS;
    public static final ModConfigSpec.DoubleValue BATTLE_AXE_HOOK_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue BATTLE_AXE_HOOK_PULL_DISTANCE;
    public static final ModConfigSpec.IntValue BATTLE_AXE_HOOK_SHIELD_DISABLE_TICKS;
    public static final ModConfigSpec.DoubleValue BATTLE_AXE_HOOK_RECOVERY_CONSUMED;
    public static final ModConfigSpec.DoubleValue BATTLE_AXE_HOOK_MOVEMENT_MULTIPLIER;
    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("ranged_combat");
        PLAYER_BOW_ACCURACY_FIX = BUILDER
                .comment("When true, player-fired bow arrows have no random spread until the bow has been held at full draw for longer than bow_strain_grace_ticks.")
                .define("player_bow_accuracy_fix", false);
        BOW_STRAIN_GRACE_TICKS = BUILDER
                .comment("Ticks after full draw before bows regain vanilla random spread. 20 ticks = 1 second.")
                .defineInRange("bow_strain_grace_ticks", 60, 0, 72000);
        BUILDER.pop();

        BUILDER.push("katana");
        IAI_CHARGE_TICKS = BUILDER
                .comment("Ticks the Iaijutsu stance must charge after the normal attack cooldown is full.")
                .defineInRange("iai_charge_ticks", 40, 1, 72000);
        IAI_PRIMED_TICKS = BUILDER
                .comment("Ticks after releasing the stance during which the next katana attack is a quickdraw strike.")
                .defineInRange("iai_primed_ticks", 20, 1, 200);
        IAI_DAMAGE_MULTIPLIER = BUILDER
                .comment("Direct-hit damage multiplier applied by a fully charged base Iaijutsu strike.")
                .defineInRange("iai_damage_multiplier", 1.5D, 1.0D, 100.0D);
        IAI_LUNGE_FORCE = BUILDER
                .comment("Horizontal velocity added toward the target by a fully charged quickdraw strike.")
                .defineInRange("iai_lunge_force", 0.65D, 0.0D, 3.0D);
        BUILDER.pop();

        BUILDER.push("battle_axe");
        BATTLE_AXE_HOOK_PREPARATION_TICKS = BUILDER
                .comment("Ticks the battle axe must be prepared before releasing a Hooking Strike.")
                .defineInRange("hook_preparation_ticks", 8, 1, 200);
        BATTLE_AXE_HOOK_DAMAGE_MULTIPLIER = BUILDER
                .comment("Hooking Strike damage as a fraction of the wielder's normal attack damage.")
                .defineInRange("hook_damage_multiplier", 0.35D, 0.0D, 1.0D);
        BATTLE_AXE_HOOK_PULL_DISTANCE = BUILDER
                .comment("Approximate maximum horizontal pull distance before knockback resistance is applied.")
                .defineInRange("hook_pull_distance", 1.5D, 0.0D, 4.0D);
        BATTLE_AXE_HOOK_SHIELD_DISABLE_TICKS = BUILDER
                .comment("Ticks a shield is disabled when caught by Hooking Strike.")
                .defineInRange("hook_shield_disable_ticks", 30, 1, 200);
        BATTLE_AXE_HOOK_RECOVERY_CONSUMED = BUILDER
                .comment("Fraction of the normal attack recovery consumed by Hooking Strike, including misses.")
                .defineInRange("hook_recovery_consumed", 0.70D, 0.0D, 1.0D);
        BATTLE_AXE_HOOK_MOVEMENT_MULTIPLIER = BUILDER
                .comment("Movement multiplier while preparing Hooking Strike.")
                .defineInRange("hook_movement_multiplier", 0.65D, 0.0D, 1.0D);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private MoreWeaponsConfig() {
    }
}
