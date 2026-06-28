package org.destroyermob.mobsmoreweapons.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MoreWeaponsConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue PLAYER_BOW_ACCURACY_FIX;
    public static final ModConfigSpec.IntValue BOW_STRAIN_GRACE_TICKS;
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

        SPEC = BUILDER.build();
    }

    private MoreWeaponsConfig() {
    }
}
