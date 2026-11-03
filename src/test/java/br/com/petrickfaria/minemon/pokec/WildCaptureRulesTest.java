package br.com.petrickfaria.minemon.pokec;

import org.junit.Test;
import static org.junit.Assert.*;

public class WildCaptureRulesTest {
    @Test public void rarityChancesAndMaximumTreatBonusMatchSpecification() {
        assertEquals(0.50D, WildCaptureRules.chance("common", 0), 0.0D);
        assertEquals(1.00D, WildCaptureRules.chance("common", 15), 0.0D);
        assertEquals(0.50D, WildCaptureRules.chance("uncommon", 15), 0.0D);
        assertEquals(0.25D, WildCaptureRules.chance("rare", 15), 0.0D);
        assertEquals(0.10D, WildCaptureRules.chance("legendary", 15), 1.0E-12D);
        assertEquals(0.05D, WildCaptureRules.chance("mythic", 15), 1.0E-12D);
    }

    @Test public void treatThresholdsAreCumulativeAndCapped() {
        assertEquals(0, WildCaptureRules.treatStages(0));
        assertEquals(1, WildCaptureRules.treatStages(1));
        assertEquals(1, WildCaptureRules.treatStages(2));
        assertEquals(2, WildCaptureRules.treatStages(3));
        assertEquals(3, WildCaptureRules.treatStages(6));
        assertEquals(4, WildCaptureRules.treatStages(10));
        assertEquals(5, WildCaptureRules.treatStages(15));
        assertEquals(5, WildCaptureRules.treatStages(100));
    }

    @Test public void soulRewardsUseDocumentedHalfUpRounding() {
        assertEquals(1, WildCaptureRules.souls(1, 50));
        assertEquals(2, WildCaptureRules.souls(2, 75));
        assertEquals(0, WildCaptureRules.souls(1, 25));
    }
}
