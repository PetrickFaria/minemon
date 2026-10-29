package br.com.petrickfaria.minemon.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MinemonConfig {
    public static final int DEFAULT_BREEDING_STAGE_BONUS_PERCENT = 5;
    public static final int DEFAULT_BREEDING_RARITY_BONUS_PERCENT = 5;
    public static final int DEFAULT_BREEDING_MIN_CHANCE_PERCENT = 5;
    public static final int DEFAULT_BREEDING_MAX_CHANCE_PERCENT = 95;
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.IntValue TAME_CHANCE_PERCENT;
    public static final ForgeConfigSpec.IntValue POKEMON_MAX_LEVEL;
    public static final ForgeConfigSpec.IntValue POKEMON_XP_PER_ZOMBIE;
    public static final ForgeConfigSpec.BooleanValue NATURAL_SPAWNING_ENABLED;
    public static final ForgeConfigSpec.IntValue TREAT_HEAL_AMOUNT;
    public static final ForgeConfigSpec.IntValue TREAT_BREEDING_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue BREEDING_STAGE_BONUS_PERCENT;
    public static final ForgeConfigSpec.IntValue BREEDING_RARITY_BONUS_PERCENT;
    public static final ForgeConfigSpec.IntValue BREEDING_MIN_CHANCE_PERCENT;
    public static final ForgeConfigSpec.IntValue BREEDING_MAX_CHANCE_PERCENT;
    public static final ForgeConfigSpec.BooleanValue TREAT_BREEDING_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> TREAT_DISABLED_BREEDING_SPECIES;
    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("companions");
        POKEMON_MAX_LEVEL = BUILDER.comment("Maximum level for domesticated Pokémon.")
                .defineInRange("pokemonMaxLevel", 10, 1, 1000);
        POKEMON_XP_PER_ZOMBIE = BUILDER.comment("Base companion XP equivalent to one adult zombie.")
                .defineInRange("pokemonXpPerZombie", 5, 1, 1000000);
        TAME_CHANCE_PERCENT = BUILDER.comment("Chance that a compatible elemental treat tames a wild Minemon (0-100).")
                .defineInRange("tameChancePercent", 33, 0, 100);
        NATURAL_SPAWNING_ENABLED = BUILDER.comment("Natural spawning is disabled until species-specific spawn data is validated.")
                .define("naturalSpawningEnabled", false);
        BUILDER.pop();
        BUILDER.push("pokemon_treats");
        TREAT_HEAL_AMOUNT = BUILDER.comment("Health restored by a compatible Pokémon treat (health points).")
                .defineInRange("healAmount", 4, 0, 1000);
        TREAT_BREEDING_COOLDOWN_TICKS = BUILDER.comment("Adult breeding cooldown in game ticks (6000 = five minutes).")
                .defineInRange("breedingCooldownTicks", 6000, 0, 240000);
        BREEDING_STAGE_BONUS_PERCENT = BUILDER.comment("Percentage points added per evolution-stage advantage when two different lines breed.")
                .defineInRange("breedingStageBonusPercent", DEFAULT_BREEDING_STAGE_BONUS_PERCENT, 0, 100);
        BREEDING_RARITY_BONUS_PERCENT = BUILDER.comment("Percentage points added per configured rarity-level advantage when two different lines breed.")
                .defineInRange("breedingRarityBonusPercent", DEFAULT_BREEDING_RARITY_BONUS_PERCENT, 0, 100);
        BREEDING_MIN_CHANCE_PERCENT = BUILDER.comment("Minimum parent-line chance for cross-line offspring (same-line pairs always produce their shared initial form).")
                .defineInRange("breedingMinChancePercent", DEFAULT_BREEDING_MIN_CHANCE_PERCENT, 0, 100);
        BREEDING_MAX_CHANCE_PERCENT = BUILDER.comment("Maximum parent-line chance for cross-line offspring (same-line pairs always produce their shared initial form).")
                .defineInRange("breedingMaxChancePercent", DEFAULT_BREEDING_MAX_CHANCE_PERCENT, 0, 100);
        TREAT_BREEDING_ENABLED = BUILDER.comment("Allow compatible treats to start breeding for eligible owned Pokémon.")
                .define("breedingEnabled", true);
        TREAT_DISABLED_BREEDING_SPECIES = BUILDER.comment("Species slugs disabled for breeding, in addition to the hard legendary and mythical exclusions.")
                .defineList("disabledBreedingSpecies", java.util.List.of(), value -> value instanceof String);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private MinemonConfig() {}
}
