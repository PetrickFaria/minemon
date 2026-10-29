package br.com.petrickfaria.minemon.entity;

import br.com.petrickfaria.minemon.config.MinemonConfig;
import br.com.petrickfaria.minemon.pokec.PokecEconomy;

/** Shared level thresholds for companion XP and the Pokémon status screen. */
public final class PokemonProgression {
    private PokemonProgression() {}

    public static int maxLevel() { return MinemonConfig.POKEMON_MAX_LEVEL.get(); }

    /** XP needed for the next level. For level 1 this is 5; for level 2 it is 15. */
    public static int xpForNextLevel(int currentLevel, String species) {
        return xpForNextLevelForRarity(currentLevel, PokecEconomy.get().rarity(species));
    }

    public static int xpForNextLevelForRarity(int currentLevel, String rarity) {
        return xpForNextLevelForRarity(currentLevel, rarity,
                MinemonConfig.POKEMON_XP_PER_ZOMBIE.get(), MinemonConfig.POKEMON_MAX_LEVEL.get());
    }

    public static int xpForNextLevelForRarity(int currentLevel, String rarity, int baseXp, int maximumLevel) {
        if (currentLevel >= maximumLevel) return 0;
        long n = (long) currentLevel + 1L;
        long base = (long) baseXp * n * (n - 1L) / 2L;
        long needed = base * rarityMultiplier(rarity);
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, needed));
    }

    public static int rarityMultiplier(String rarity) {
        return switch (rarity == null ? "common" : rarity.toLowerCase(java.util.Locale.ROOT)) {
            case "uncommon" -> 2;
            case "rare" -> 4;
            case "very_rare" -> 8;
            case "legendary", "mythic", "mythical" -> 16;
            default -> 1;
        };
    }

    public static int chanseyCooldownTicks(int level) {
        if (level <= 2) return 6000;
        if (level <= 4) return 4800;
        if (level <= 6) return 3600;
        if (level <= 8) return 2400;
        return 1200;
    }
}
