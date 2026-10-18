package br.com.petrickfaria.minemon.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MinemonConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.IntValue TAME_CHANCE_PERCENT;
    public static final ForgeConfigSpec.BooleanValue NATURAL_SPAWNING_ENABLED;
    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("companions");
        TAME_CHANCE_PERCENT = BUILDER.comment("Chance that a bone tames a wild Minemon (0-100).")
                .defineInRange("tameChancePercent", 33, 0, 100);
        NATURAL_SPAWNING_ENABLED = BUILDER.comment("Natural spawning is disabled until species-specific spawn data is validated.")
                .define("naturalSpawningEnabled", false);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private MinemonConfig() {}
}
