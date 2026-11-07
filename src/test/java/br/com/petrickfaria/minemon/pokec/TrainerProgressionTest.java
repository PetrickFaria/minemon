package br.com.petrickfaria.minemon.pokec;

import org.junit.Test;
import static org.junit.Assert.*;

public class TrainerProgressionTest {
    @Test public void trainerThresholdsMatchTenZombiesThenFiveMoreEachLevel() {
        double[] expected = {50, 75, 100, 125, 150, 175, 200, 225, 250};
        for (int level = 1; level <= expected.length; level++) {
            assertEquals(expected[level - 1], TrainerProgression.xpToNext(level, 5), 0.0D);
        }
        assertEquals(275.0D, TrainerProgression.xpToNext(10, 5), 0.0D);
        assertEquals(100.0D, TrainerProgression.xpToNext(1, 10), 0.0D);
    }

    @Test public void trainerGetsAnAdditionalTenPercentOfTheCompanionAward() {
        assertEquals(10.0D, TrainerProgression.trainerShareForCompanionXpHalfUnits(200), 0.0D);
        assertEquals(5.0D, TrainerProgression.trainerShareForCompanionXpHalfUnits(100), 0.0D);
        assertEquals(0.25D, TrainerProgression.trainerShareForCompanionXpHalfUnits(5), 0.0D);
        assertEquals(0.0D, TrainerProgression.trainerShareForCompanionXpHalfUnits(-1), 0.0D);
    }

    @Test public void progressionPreservesFractionalRemainderAndSupportsMultipleLevels() {
        var first = TrainerProgression.add(new TrainerProgression.Progress(1, 0.0D), 56.5D, 5);
        assertEquals(2, first.level()); assertEquals(6.5D, first.xpTowardNext(), 0.0D);
        var multiple = TrainerProgression.add(new TrainerProgression.Progress(1, 0.0D), 235.0D, 5);
        assertEquals(4, multiple.level()); assertEquals(10.0D, multiple.xpTowardNext(), 0.0D);
        var exact = TrainerProgression.add(new TrainerProgression.Progress(1, 0.0D), 125.0D, 5);
        assertEquals(3, exact.level()); assertEquals(0.0D, exact.xpTowardNext(), 0.0D);
    }

    @Test public void levelCapConsumesNoFurtherExperience() {
        var capped = TrainerProgression.add(new TrainerProgression.Progress(1000, 12.0D), 1000.0D, 5);
        assertEquals(1000, capped.level()); assertEquals(0.0D, capped.xpTowardNext(), 0.0D);
    }
}
