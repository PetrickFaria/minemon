package br.com.petrickfaria.minemon.pokec;

import br.com.petrickfaria.minemon.config.MinemonConfig;

/** Pure rules for the separate Trainer Pokémon experience track. */
public final class TrainerProgression {
    public static final int MAX_LEVEL = 1000;

    public record Progress(int level, double xpTowardNext) { }

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
        int level = Math.max(1, Math.min(MAX_LEVEL, current.level()));
        double xp = Double.isFinite(current.xpTowardNext()) ? Math.max(0.0D, current.xpTowardNext()) : 0.0D;
        if (!Double.isFinite(award) || award <= 0.0D || level >= MAX_LEVEL)
            return new Progress(level, level >= MAX_LEVEL ? 0.0D : xp);
        xp += award;
        while (level < MAX_LEVEL && xp + 1.0E-9D >= xpToNext(level)) {
            xp -= xpToNext(level);
            level++;
        }
        return new Progress(level, level >= MAX_LEVEL ? 0.0D : Math.max(0.0D, xp));
    }

    private TrainerProgression() { }
}
