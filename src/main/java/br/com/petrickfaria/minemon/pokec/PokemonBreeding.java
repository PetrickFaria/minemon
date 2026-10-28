package br.com.petrickfaria.minemon.pokec;

import br.com.petrickfaria.minemon.config.MinemonConfig;
import java.util.HashSet;
import java.util.Set;
import java.util.function.DoubleSupplier;

/** Server-side lineage and inheritance rules for owned Minemon breeding. */
public final class PokemonBreeding {
    public record ParentProfile(String initialForm, int evolutionStage, int rarityLevel) { }

    private PokemonBreeding() { }

    /** Builds a profile from the species catalog and the currently loaded PokéC rarity config. */
    public static ParentProfile profile(String speciesSlug) {
        PokecSpecies.Entry current = PokecSpecies.get(speciesSlug);
        if (current == null) throw new IllegalArgumentException("Unknown breeding species: " + speciesSlug);
        Set<String> visited = new HashSet<>();
        visited.add(current.slug());
        int stage = 0;
        while (current.evolvesFrom() != null && !current.evolvesFrom().isBlank()) {
            String parentSlug = current.evolvesFrom();
            if (!visited.add(parentSlug)) throw new IllegalStateException("Evolution cycle in Pokédex at " + parentSlug);
            current = PokecSpecies.get(parentSlug);
            if (current == null) throw new IllegalStateException("Unknown evolution parent " + parentSlug + " for " + speciesSlug);
            stage++;
        }
        String rarity = PokecEconomy.get().rarity(speciesSlug);
        return new ParentProfile(current.slug(), Math.min(2, stage), rarityLevel(rarity));
    }

    public static int rarityLevel(String rarity) {
        return switch (rarity) {
            case "common" -> 0;
            case "uncommon" -> 1;
            case "rare" -> 2;
            case "very_rare" -> 3;
            case "legendary" -> 4;
            default -> 0;
        };
    }

    /** Chance in percent that a cross-line baby belongs to the first parent's line. */
    public static int chanceForFirstLine(ParentProfile first, ParentProfile second,
                                         int stageBonusPercent, int rarityBonusPercent,
                                         int minimumPercent, int maximumPercent) {
        if (first.initialForm().equals(second.initialForm())) return 100;
        int lower = clamp(minimumPercent, 0, 100);
        int upper = clamp(maximumPercent, 0, 100);
        if (lower > upper) {
            int swap = lower;
            lower = upper;
            upper = swap;
        }
        int raw = 50
                + (first.evolutionStage() - second.evolutionStage()) * Math.max(0, stageBonusPercent)
                + (first.rarityLevel() - second.rarityLevel()) * Math.max(0, rarityBonusPercent);
        return clamp(raw, lower, upper);
    }

    /** Selects only the initial form of one parent's line using one normalized server roll. */
    public static String chooseInitialForm(String firstSlug, String secondSlug, double roll) {
        ParentProfile first = profile(firstSlug);
        ParentProfile second = profile(secondSlug);
        return chooseInitialForm(first, second, roll,
                MinemonConfig.BREEDING_STAGE_BONUS_PERCENT.get(),
                MinemonConfig.BREEDING_RARITY_BONUS_PERCENT.get(),
                MinemonConfig.BREEDING_MIN_CHANCE_PERCENT.get(),
                MinemonConfig.BREEDING_MAX_CHANCE_PERCENT.get());
    }

    /** Uses exactly one server random draw only when the parents belong to different lines. */
    public static String chooseInitialForm(String firstSlug, String secondSlug, DoubleSupplier serverRoll) {
        ParentProfile first = profile(firstSlug);
        ParentProfile second = profile(secondSlug);
        return chooseInitialForm(first, second, serverRoll,
                MinemonConfig.BREEDING_STAGE_BONUS_PERCENT.get(),
                MinemonConfig.BREEDING_RARITY_BONUS_PERCENT.get(),
                MinemonConfig.BREEDING_MIN_CHANCE_PERCENT.get(),
                MinemonConfig.BREEDING_MAX_CHANCE_PERCENT.get());
    }

    /** Deterministic supplier overload; the supplier is not invoked for same-line parents. */
    public static String chooseInitialForm(ParentProfile first, ParentProfile second, DoubleSupplier serverRoll,
                                           int stageBonusPercent, int rarityBonusPercent,
                                           int minimumPercent, int maximumPercent) {
        if (first.initialForm().equals(second.initialForm())) return first.initialForm();
        return chooseInitialForm(first, second, serverRoll.getAsDouble(), stageBonusPercent,
                rarityBonusPercent, minimumPercent, maximumPercent);
    }

    /** Deterministic overload used by tests; production calls the config-backed method above. */
    public static String chooseInitialForm(ParentProfile first, ParentProfile second, double roll,
                                           int stageBonusPercent, int rarityBonusPercent,
                                           int minimumPercent, int maximumPercent) {
        if (first.initialForm().equals(second.initialForm())) return first.initialForm();
        if (!Double.isFinite(roll)) throw new IllegalArgumentException("Breeding roll must be finite");
        int chance = chanceForFirstLine(first, second, stageBonusPercent, rarityBonusPercent, minimumPercent, maximumPercent);
        double boundedRoll = Math.max(0.0D, Math.min(Math.nextDown(1.0D), roll));
        return boundedRoll * 100.0D < chance ? first.initialForm() : second.initialForm();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
