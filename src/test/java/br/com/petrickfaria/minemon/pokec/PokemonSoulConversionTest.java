package br.com.petrickfaria.minemon.pokec;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class PokemonSoulConversionTest {
    private static final PokemonSoulConversion.Settings DEFAULTS = new PokemonSoulConversion.Settings(1, 3, 5, 1, 2, 5, 75);

    @Test public void evolutionStagesFollowCatalogIncludingTwoStageLines() {
        assertEquals(PokemonSoulConversion.Stage.INITIAL, PokemonSoulConversion.stage("charmander"));
        assertEquals(PokemonSoulConversion.Stage.INTERMEDIATE, PokemonSoulConversion.stage("charmeleon"));
        assertEquals(PokemonSoulConversion.Stage.FINAL, PokemonSoulConversion.stage("charizard"));
        assertEquals(PokemonSoulConversion.Stage.INITIAL, PokemonSoulConversion.stage("pikachu"));
        assertEquals(PokemonSoulConversion.Stage.FINAL, PokemonSoulConversion.stage("raichu"));
        assertEquals(PokemonSoulConversion.Stage.SINGLE, PokemonSoulConversion.stage("snorlax"));
    }

    @Test public void maximumCombinesStageRarityAndLevelWithLevelCap() {
        var charmander = PokemonSoulConversion.preview("charmander", 1, "common", DEFAULTS);
        assertEquals(1, charmander.maximum());
        assertEquals(SoulType.FIRE, charmander.soulType());
        assertEquals(4, PokemonSoulConversion.preview("charmeleon", 2, "common", DEFAULTS).maximum());
        assertEquals(12, PokemonSoulConversion.preview("charizard", 10, "rare", DEFAULTS).maximum());
        assertEquals(5, PokemonSoulConversion.levelBonus(100, 2, 5));
        assertEquals(0, PokemonSoulConversion.levelBonus(1, 2, 5));
    }

    @Test public void configurableBonusesAndChanceAreUsed() {
        var settings = new PokemonSoulConversion.Settings(2, 4, 6, 2, 3, 4, 40);
        var preview = PokemonSoulConversion.preview("charizard", 9, "very_rare", settings);
        assertEquals(6, preview.stageBase());
        assertEquals(6, preview.rarityBonus());
        assertEquals(3, preview.levelBonus());
        assertEquals(15, preview.maximum());
        assertEquals(40, preview.chancePercent());
    }

    @Test public void rollUsesOneIndependentDeterministicDrawPerPossibleSoul() {
        var preview = PokemonSoulConversion.preview("charmeleon", 2, "common", DEFAULTS);
        AtomicInteger draws = new AtomicInteger();
        int result = PokemonSoulConversion.roll(preview, () -> draws.getAndIncrement() % 2 == 0 ? 0.74D : 0.75D);
        assertEquals(4, draws.get());
        assertEquals(2, result);
    }

    @Test public void rollHandlesZeroMaximumAndChanceBounds() {
        var zero = PokemonSoulConversion.preview("charmander", 1, "common",
                new PokemonSoulConversion.Settings(0, 0, 0, 0, 2, 0, 75));
        AtomicInteger draws = new AtomicInteger();
        assertEquals(0, PokemonSoulConversion.roll(zero, () -> { draws.incrementAndGet(); return 0; }));
        assertEquals(0, draws.get());
        var none = PokemonSoulConversion.preview("charmander", 1, "common",
                new PokemonSoulConversion.Settings(1, 3, 5, 1, 2, 5, -20));
        assertEquals(0, PokemonSoulConversion.roll(none, () -> 0.0D));
        var guaranteed = PokemonSoulConversion.preview("charmander", 1, "common",
                new PokemonSoulConversion.Settings(1, 3, 5, 1, 2, 5, 120));
        assertEquals(1, PokemonSoulConversion.roll(guaranteed, () -> 0.999D));
    }
}
