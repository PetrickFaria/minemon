package br.com.petrickfaria.minemon.pokec;

import br.com.petrickfaria.minemon.config.MinemonConfig;
import java.util.function.DoubleSupplier;

/** Server-side soul yield rules for a Pokémon stored in a Pokébola. */
public final class PokemonSoulConversion {
    public enum Stage { SINGLE, INITIAL, INTERMEDIATE, FINAL }

    public record Settings(int initialBase, int intermediateBase, int finalBase,
                           int rarityPerTier, int levelsPerBonus, int maxLevelBonus,
                           int soulChancePercent) {
        public static Settings configured() {
            return new Settings(
                    MinemonConfig.POKEBALL_INITIAL_SOULS.get(),
                    MinemonConfig.POKEBALL_INTERMEDIATE_SOULS.get(),
                    MinemonConfig.POKEBALL_FINAL_SOULS.get(),
                    MinemonConfig.POKEBALL_RARITY_BONUS.get(),
                    MinemonConfig.POKEBALL_LEVELS_PER_BONUS.get(),
                    MinemonConfig.POKEBALL_MAX_LEVEL_BONUS.get(),
                    MinemonConfig.POKEBALL_SOUL_CHANCE_PERCENT.get());
        }
    }

    public record Preview(String species, int level, String rarity, Stage stage,
                          int stageBase, int rarityBonus, int levelBonus,
                          SoulType soulType, int maximum, int chancePercent) { }

    public static Stage stage(String slug) {
        PokecSpecies.Entry entry = PokecSpecies.get(slug);
        if (entry == null) return Stage.SINGLE;
        if (entry.evolvesFrom() == null || entry.evolvesFrom().isBlank()) {
            boolean hasEvolution = PokecSpecies.ALL.values().stream()
                    .anyMatch(candidate -> slug.equals(candidate.evolvesFrom()));
            return hasEvolution ? Stage.INITIAL : Stage.SINGLE;
        }
        boolean hasEvolution = PokecSpecies.ALL.values().stream()
                .anyMatch(candidate -> slug.equals(candidate.evolvesFrom()));
        if (!hasEvolution) return Stage.FINAL;
        PokecSpecies.Entry parent = PokecSpecies.get(entry.evolvesFrom());
        return parent != null && parent.evolvesFrom() != null ? Stage.FINAL : Stage.INTERMEDIATE;
    }

    public static int rarityTier(String rarity) {
        return switch (rarity == null ? "common" : rarity) {
            case "common" -> 0;
            case "uncommon" -> 1;
            case "rare" -> 2;
            case "very_rare" -> 3;
            case "legendary" -> 4;
            default -> 0;
        };
    }

    public static int levelBonus(int level, int levelsPerBonus, int maximumBonus) {
        return Math.min(Math.max(0, maximumBonus), Math.max(1, level) / Math.max(1, levelsPerBonus));
    }

    public static Preview preview(String slug, int level, String configuredRarity, Settings settings) {
        PokecSpecies.Entry species = PokecSpecies.get(slug);
        if (species == null) return null;
        Stage stage = stage(slug);
        int base = switch (stage) {
            case SINGLE, INITIAL -> settings.initialBase();
            case INTERMEDIATE -> settings.intermediateBase();
            case FINAL -> settings.finalBase();
        };
        int rarityBonus = rarityTier(configuredRarity) * Math.max(0, settings.rarityPerTier());
        int levelBonus = levelBonus(level, settings.levelsPerBonus(), settings.maxLevelBonus());
        int maximum = Math.max(0, base + rarityBonus + levelBonus);
        int chance = Math.max(0, Math.min(100, settings.soulChancePercent()));
        return new Preview(slug, Math.max(1, level), configuredRarity, stage, base,
                rarityBonus, levelBonus, SoulType.fromSpeciesType(species.primaryType()), maximum, chance);
    }

    public static Preview preview(String slug, int level) {
        PokecSpecies.Entry species = PokecSpecies.get(slug);
        if (species == null) return null;
        return preview(slug, level, PokecEconomy.get().rarity(slug), Settings.configured());
    }

    /** Draws once per possible soul; the supplier is always called maximum times. */
    public static int roll(Preview preview, DoubleSupplier serverRandom) {
        if (preview == null) return 0;
        int obtained = 0;
        double chance = preview.chancePercent() / 100.0D;
        for (int i = 0; i < preview.maximum(); i++) {
            double draw = serverRandom.getAsDouble();
            if (Double.isFinite(draw) && draw >= 0.0D && draw < 1.0D && draw < chance) obtained++;
        }
        return obtained;
    }

    private PokemonSoulConversion() { }
}
