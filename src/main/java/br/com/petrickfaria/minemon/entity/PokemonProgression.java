package br.com.petrickfaria.minemon.entity;

import br.com.petrickfaria.minemon.config.MinemonConfig;
import br.com.petrickfaria.minemon.pokec.PokecEconomy;

/** Shared level thresholds for companion XP and the Pokémon status screen. */
public final class PokemonProgression {
    private PokemonProgression() {}

    public static String rarityKey(String rarity) {
        String key=rarity==null?"common":rarity.toLowerCase(java.util.Locale.ROOT);
        return switch(key) {case "mythical","legendary_mythical" -> "mythic";case "very_rare" -> "rare";default -> key;};
    }
    public static double healthForRarity(String rarity,int level) {
        double base=switch(rarityKey(rarity)){case "uncommon"->14;case "rare"->18;case "legendary"->22;case "mythic"->26;default->10;};
        return base+4.0D*(Math.max(1,level)-1);
    }
    public static double damageForRarity(String rarity,int level) {
        double base=switch(rarityKey(rarity)){case "uncommon"->2;case "rare"->3;case "legendary"->4;case "mythic"->5;default->1;};
        return base+Math.max(1,level)-1;
    }
    public static int maxLevel() { return MinemonConfig.POKEMON_MAX_LEVEL.get(); }

    /** XP needed to advance one level, using the configured adult-zombie XP as the base. */
    public static int xpForNextLevel(int currentLevel, String species) {
        return xpForNextLevelForRarity(currentLevel, PokecEconomy.get().rarity(species));
    }

    public static int xpForNextLevelForRarity(int currentLevel, String rarity) {
        return xpForNextLevelForRarity(currentLevel, rarity,
                MinemonConfig.POKEMON_XP_PER_ZOMBIE.get(), MinemonConfig.POKEMON_MAX_LEVEL.get());
    }

    public static int xpForNextLevelForRarity(int currentLevel, String rarity, int baseXp, int maximumLevel) {
        if (currentLevel >= maximumLevel) return 0;
        long needed = ((long) Math.max(1, currentLevel) + rarityOffset(rarity)) * Math.max(0, baseXp);
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, needed));
    }

    public static int xpRewardHalfUnitsForWildRarity(String rarity, int baseXp) {
        int multiplier = switch (rarityKey(rarity)) {
            case "uncommon" -> 2;
            case "rare" -> 4;
            case "legendary" -> 20;
            case "mythic" -> 50;
            default -> 1;
        };
        return (int) Math.min(Integer.MAX_VALUE, (long) Math.max(0, baseXp) * multiplier);
    }

    public static int rarityOffset(String rarity) {
        return switch (rarityKey(rarity)) {
            case "uncommon" -> 3;
            case "rare" -> 4;
            case "legendary" -> 5;
            case "mythic" -> 6;
            default -> 2;
        };
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
