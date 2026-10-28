package br.com.petrickfaria.minemon.pokec;

import br.com.petrickfaria.minemon.config.MinemonConfig;
import org.junit.Test;
import static org.junit.Assert.*;

public class PokemonBreedingTest {
    private static PokemonBreeding.ParentProfile parent(String line, int stage, int rarity) {
        return new PokemonBreeding.ParentProfile(line, stage, rarity);
    }
    private static String choose(String first, String second, double roll) {
        return PokemonBreeding.chooseInitialForm(PokemonBreeding.profile(first), PokemonBreeding.profile(second),
                roll, 5, 5, 5, 95);
    }

    @Test public void evolutionCatalogResolvesInitialFormsAndStages() {
        assertEquals("charmander", PokemonBreeding.profile("charmander").initialForm());
        assertEquals(0, PokemonBreeding.profile("charmander").evolutionStage());
        assertEquals("charmander", PokemonBreeding.profile("charmeleon").initialForm());
        assertEquals(1, PokemonBreeding.profile("charmeleon").evolutionStage());
        assertEquals("charmander", PokemonBreeding.profile("charizard").initialForm());
        assertEquals(2, PokemonBreeding.profile("charizard").evolutionStage());
        assertEquals("caterpie", PokemonBreeding.profile("metapod").initialForm());
        assertEquals("pikachu", PokemonBreeding.profile("raichu").initialForm());
        assertEquals("eevee", PokemonBreeding.profile("jolteon").initialForm());
        assertEquals("gastly", PokemonBreeding.profile("gengar").initialForm());
        assertEquals("caterpie", PokemonBreeding.profile("butterfree").initialForm());
        assertEquals("weedle", PokemonBreeding.profile("beedrill").initialForm());
    }

    @Test public void sameEvolutionLineAlwaysProducesItsInitialForm() {
        assertEquals("charmander", choose("charizard", "charizard", 0.99));
        assertEquals("charmander", choose("charizard", "charmeleon", 0.99));
        assertEquals("charmander", choose("charmander", "charizard", 0.01));
        var draws = new java.util.concurrent.atomic.AtomicInteger();
        assertEquals("charmander", PokemonBreeding.chooseInitialForm(PokemonBreeding.profile("charizard"), PokemonBreeding.profile("charmeleon"),
                () -> { draws.incrementAndGet(); return 0.5; }, 5, 5, 5, 95));
        assertEquals(0, draws.get());
    }

    @Test public void crossSpeciesParentsCanOnlyProduceEitherInitialLine() {
        assertEquals("charmander", choose("charizard", "metapod", 0.0));
        assertEquals("caterpie", choose("charizard", "metapod", 0.999999));
        assertEquals("pikachu", choose("raichu", "jolteon", 0.0));
        assertEquals("eevee", choose("raichu", "jolteon", 0.999999));
        assertEquals("pikachu", choose("pikachu", "eevee", 0.0));
        assertEquals("eevee", choose("pikachu", "eevee", 0.999999));
        assertEquals("gastly", choose("gengar", "dragonite", 0.0));
        assertEquals("dratini", choose("gengar", "dragonite", 0.999999));
        var draws = new java.util.concurrent.atomic.AtomicInteger();
        PokemonBreeding.chooseInitialForm(PokemonBreeding.profile("charizard"), PokemonBreeding.profile("squirtle"),
                () -> { draws.incrementAndGet(); return 0.5; }, 5, 5, 5, 95);
        assertEquals(1, draws.get());
    }

    @Test public void profileReadsTheCurrentConfiguredSpeciesRarity() {
        String effectiveRarity = PokecEconomy.get().rarity("charizard");
        assertEquals(PokemonBreeding.rarityLevel(effectiveRarity), PokemonBreeding.profile("charizard").rarityLevel());
        assertEquals("legendary", PokecEconomy.resolveRarity("legendary", "common"));
        assertEquals("common", PokecEconomy.resolveRarity("invalid", "common"));
        assertEquals("common", PokecEconomy.resolveRarity(null, "common"));
    }

    @Test public void stageAdvantagesMatchTheSpecifiedOdds() {
        var charizard = parent("charmander", 2, 0);
        var squirtle = parent("squirtle", 0, 0);
        var wartortle = parent("squirtle", 1, 0);
        assertEquals(60, PokemonBreeding.chanceForFirstLine(charizard, squirtle, 5, 5, 5, 95));
        assertEquals(55, PokemonBreeding.chanceForFirstLine(charizard, wartortle, 5, 5, 5, 95));
        assertEquals(40, PokemonBreeding.chanceForFirstLine(wartortle, charizard, 5, 5, 5, 95));
        assertEquals(50, PokemonBreeding.chanceForFirstLine(parent("charmander", 2, 0), parent("squirtle", 2, 0), 5, 5, 5, 95));
    }

    @Test public void rarityAdvantagesMatchEveryConfiguredTier() {
        assertEquals(45, PokemonBreeding.chanceForFirstLine(parent("a", 0, 0), parent("b", 0, 1), 5, 5, 5, 95));
        assertEquals(40, PokemonBreeding.chanceForFirstLine(parent("a", 0, 0), parent("b", 0, 2), 5, 5, 5, 95));
        assertEquals(40, PokemonBreeding.chanceForFirstLine(parent("a", 0, 1), parent("b", 0, 3), 5, 5, 5, 95));
        assertEquals(30, PokemonBreeding.chanceForFirstLine(parent("a", 0, 0), parent("b", 0, 4), 5, 5, 5, 95));
        assertEquals(4, PokemonBreeding.rarityLevel("legendary"));
    }

    @Test public void stageAndRarityBonusesAddAndCanCompensate() {
        assertEquals(70, PokemonBreeding.chanceForFirstLine(parent("charmander", 2, 2), parent("squirtle", 0, 0), 5, 5, 5, 95));
        assertEquals(50, PokemonBreeding.chanceForFirstLine(parent("charmander", 2, 0), parent("squirtle", 0, 2), 5, 5, 5, 95));
        assertEquals(55, PokemonBreeding.chanceForFirstLine(parent("a", 1, 3), parent("b", 2, 1), 5, 5, 5, 95));
        assertEquals(50, PokemonBreeding.chanceForFirstLine(parent("a", 1, 3), parent("b", 2, 2), 5, 5, 5, 95));
    }

    @Test public void oddsAreOrderSymmetricAndDeterministicAtTheBoundary() {
        var first = parent("charmander", 2, 0);
        var second = parent("squirtle", 0, 0);
        int forward = PokemonBreeding.chanceForFirstLine(first, second, 5, 5, 5, 95);
        int reverse = PokemonBreeding.chanceForFirstLine(second, first, 5, 5, 5, 95);
        assertEquals(100, forward + reverse);
        assertEquals("charmander", PokemonBreeding.chooseInitialForm(first, second, 0.599, 5, 5, 5, 95));
        assertEquals("squirtle", PokemonBreeding.chooseInitialForm(first, second, 0.60, 5, 5, 5, 95));
    }

    @Test public void crossLineOddsRespectConfigurableBounds() {
        var extreme = parent("a", 2, 4);
        var opposite = parent("b", 0, 0);
        assertEquals(95, PokemonBreeding.chanceForFirstLine(extreme, opposite, 100, 100, 5, 95));
        assertEquals(5, PokemonBreeding.chanceForFirstLine(opposite, extreme, 100, 100, 5, 95));
        assertEquals(95, PokemonBreeding.chanceForFirstLine(extreme, opposite, 100, 100, 95, 5));
        assertEquals(100, PokemonBreeding.chanceForFirstLine(parent("same", 0, 0), parent("same", 2, 4), 5, 5, 5, 95));
    }

    @Test public void breedingOddsHaveAdjustableDefaults() {
        assertEquals(5, MinemonConfig.DEFAULT_BREEDING_STAGE_BONUS_PERCENT);
        assertEquals(5, MinemonConfig.DEFAULT_BREEDING_RARITY_BONUS_PERCENT);
        assertEquals(5, MinemonConfig.DEFAULT_BREEDING_MIN_CHANCE_PERCENT);
        assertEquals(95, MinemonConfig.DEFAULT_BREEDING_MAX_CHANCE_PERCENT);
    }
}
