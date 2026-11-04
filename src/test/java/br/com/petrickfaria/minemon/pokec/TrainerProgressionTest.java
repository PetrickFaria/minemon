package br.com.petrickfaria.minemon.pokec;

import org.junit.Test;
import static org.junit.Assert.*;

public class TrainerProgressionTest {
    @Test public void trainerThresholdsMatchRequestedCurve() {
        assertEquals(5.0D, TrainerProgression.xpToNext(1), 0.0D);
        assertEquals(15.0D, TrainerProgression.xpToNext(2), 0.0D);
        assertEquals(30.0D, TrainerProgression.xpToNext(3), 0.0D);
        assertEquals(135.0D, TrainerProgression.xpToNext(10), 0.0D);
    }

    @Test public void progressionPreservesRemainderAndSupportsMultipleLevels() {
        var first = TrainerProgression.add(new TrainerProgression.Progress(1, 0.0D), 6.5D);
        assertEquals(2, first.level()); assertEquals(1.5D, first.xpTowardNext(), 0.0D);
        var multiple = TrainerProgression.add(new TrainerProgression.Progress(1, 0.0D), 50.0D);
        assertEquals(4, multiple.level()); assertEquals(0.0D, multiple.xpTowardNext(), 0.0D);
    }

    @Test public void pokemonXpUsesRarityStageAndIndividualLevelBonuses() {
        assertEquals(7.575D, TrainerProgression.pokemonXp("bulbasaur", 1), 1.0E-9D);
        assertEquals(9.0D, TrainerProgression.pokemonXp("ivysaur", 5), 1.0E-9D);
        assertEquals(10.5D, TrainerProgression.pokemonXp("venusaur", 10), 1.0E-9D);
        assertEquals(0.0D, TrainerProgression.pokemonXp("not_a_species", 10), 0.0D);
    }

    @Test public void levelCapConsumesNoFurtherExperience() {
        var capped = TrainerProgression.add(new TrainerProgression.Progress(1000, 12.0D), 1000.0D);
        assertEquals(1000, capped.level()); assertEquals(0.0D, capped.xpTowardNext(), 0.0D);
    }
}
