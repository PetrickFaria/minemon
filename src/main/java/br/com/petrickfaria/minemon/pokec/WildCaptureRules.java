package br.com.petrickfaria.minemon.pokec;

/** Deterministic capture and soul reward rules shared by server actions and tests. */
public final class WildCaptureRules {
    public static final int MAX_ATTEMPTS = 3;
    public static final int MAX_TREATS = 15;

    public static double baseChance(String rarity) {
        return switch (PokecEconomy.resolveRarity(rarity, "common")) {
            case "uncommon" -> 0.25D;
            case "rare" -> 0.125D;
            case "legendary" -> 0.05D;
            case "mythic" -> 0.025D;
            default -> 0.50D;
        };
    }

    public static int treatStages(int treats) {
        int count = Math.max(0, Math.min(MAX_TREATS, treats));
        if (count >= 15) return 5;
        if (count >= 10) return 4;
        if (count >= 6) return 3;
        if (count >= 3) return 2;
        return count >= 1 ? 1 : 0;
    }

    public static double chance(String rarity, int treats) {
        return Math.min(1.0D, baseChance(rarity) * (1.0D + 0.20D * treatStages(treats)));
    }

    /** Evolution forms inherit the current level requirement that unlocks that form. */
    public static int minimumSpawnLevel(String slug) {
        PokecSpecies.Entry species = PokecSpecies.get(slug);
        if (species == null || !species.isEvolution()) return 1;
        return Math.max(1, PokemonEvolutionRules.requiredLevel(species.evolvesFrom()));
    }

    /** Half-up rounding keeps fractional soul rewards consistent and non-negative. */
    public static int souls(int baseSouls, int percent) {
        long bounded = Math.max(0, baseSouls);
        int pct = Math.max(0, Math.min(100, percent));
        return (int) Math.min(Integer.MAX_VALUE, (bounded * pct + 50L) / 100L);
    }

    private WildCaptureRules() { }
}
