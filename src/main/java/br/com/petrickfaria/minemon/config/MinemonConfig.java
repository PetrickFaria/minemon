package br.com.petrickfaria.minemon.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MinemonConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.IntValue TAME_CHANCE_PERCENT;
    public static final ForgeConfigSpec.BooleanValue NATURAL_SPAWNING_ENABLED;
    public static final ForgeConfigSpec.IntValue TREAT_HEAL_AMOUNT;
    public static final ForgeConfigSpec.IntValue TREAT_BREEDING_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.BooleanValue TREAT_BREEDING_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> TREAT_DISABLED_BREEDING_SPECIES;
    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("companions");
        TAME_CHANCE_PERCENT = BUILDER.comment("Chance that a bone tames a wild Minemon (0-100).")
                .defineInRange("tameChancePercent", 33, 0, 100);
        NATURAL_SPAWNING_ENABLED = BUILDER.comment("Natural spawning is disabled until species-specific spawn data is validated.")
                .define("naturalSpawningEnabled", false);
        BUILDER.pop();
        BUILDER.push("pokemon_treats");
        TREAT_HEAL_AMOUNT = BUILDER.comment("Health restored by a compatible Pokémon treat (health points).")
                .defineInRange("healAmount", 4, 0, 1000);
        TREAT_BREEDING_COOLDOWN_TICKS = BUILDER.comment("Adult breeding cooldown in game ticks (6000 = five minutes).")
                .defineInRange("breedingCooldownTicks", 6000, 0, 240000);
        TREAT_BREEDING_ENABLED = BUILDER.comment("Allow compatible treats to start breeding for eligible owned Pokémon.")
                .define("breedingEnabled", true);
        TREAT_DISABLED_BREEDING_SPECIES = BUILDER.comment("Species slugs disabled for breeding, in addition to the hard legendary and mythical exclusions.")
                .defineList("disabledBreedingSpecies", java.util.List.of(), value -> value instanceof String);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private MinemonConfig() {}
}
