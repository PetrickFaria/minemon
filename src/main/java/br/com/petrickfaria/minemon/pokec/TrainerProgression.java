package br.com.petrickfaria.minemon.pokec;

import br.com.petrickfaria.minemon.config.MinemonConfig;

/** Pure rules for the separate Trainer Pokémon experience track. */
public final class TrainerProgression {
    public static final int MAX_LEVEL = 1000;

    public record Progress(int level, double xpTowardNext) { }

    /** XP for a Pokémon conversion or capture action before applying its action percentage. */
    public static double pokemonXp(String slug, int pokemonLevel) {
        PokecSpecies.Entry species = PokecSpecies.get(slug);
        if (species == null) return 0.0D;
        double base = switch (PokecEconomy.resolveRarity(species.rarity(), "common")) {
            case "uncommon" -> 7.5D;
            case "rare" -> 37.5D;
            case "legendary" -> 375.0D;
            case "mythic" -> 9375.0D;
            default -> 2.5D;
        };
        int stage = evolutionStage(species);
        double stageBonus = stage <= 1 ? 0.0D : stage == 2 ? 0.15D : 0.30D;
        return base * (1.0D + stageBonus + 0.01D * Math.max(1, pokemonLevel));
    }

    public static int evolutionStage(PokecSpecies.Entry species) {
        int stage = 1;
        String parent = species.evolvesFrom();
        while (parent != null && !parent.isBlank() && stage < 3) {
            stage++;
            PokecSpecies.Entry previous = PokecSpecies.get(parent);
            parent = previous == null ? null : previous.evolvesFrom();
        }
        return stage;
    }

    /** XP needed for the next trainer level, in adult-zombie XP equivalents. */
    public static double xpToNext(int level) {
        return xpToNext(level, MinemonConfig.POKEMON_XP_PER_ZOMBIE.get());
    }

    public static double xpToNext(int level, int zombieXp) {
        if (level >= MAX_LEVEL) return Double.POSITIVE_INFINITY;
        long zombieEquivalent = 10L + 5L * (Math.max(1, level) - 1L);
        return zombieEquivalent * Math.max(0, zombieXp);
    }

    /** Trainer receives an additional 10% of the companion's award, expressed in half-XP units. */
    public static double trainerShareForCompanionXpHalfUnits(int companionXpHalfUnits) {
        return Math.max(0, companionXpHalfUnits) * 0.05D;
    }

    public static Progress add(Progress current, double award) {
        return add(current, award, MinemonConfig.POKEMON_XP_PER_ZOMBIE.get());
    }

    public static Progress add(Progress current, double award, int zombieXp) {
        int level = Math.max(1, Math.min(MAX_LEVEL, current.level()));
        double xp = Double.isFinite(current.xpTowardNext()) ? Math.max(0.0D, current.xpTowardNext()) : 0.0D;
        if (!Double.isFinite(award) || award <= 0.0D || level >= MAX_LEVEL)
            return new Progress(level, level >= MAX_LEVEL ? 0.0D : xp);
        xp += award;
        while (level < MAX_LEVEL && xp + 1.0E-9D >= xpToNext(level, zombieXp)) {
            xp -= xpToNext(level, zombieXp);
            level++;
        }
        return new Progress(level, level >= MAX_LEVEL ? 0.0D : Math.max(0.0D, xp));
    }

    private TrainerProgression() { }
}
