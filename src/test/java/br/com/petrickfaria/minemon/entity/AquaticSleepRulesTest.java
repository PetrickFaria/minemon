package br.com.petrickfaria.minemon.entity;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Focused rules for the requested aquatic sleep behavior. */
public final class AquaticSleepRulesTest {
    @Test public void shallowSleeperMovesOnlyAsFarAsNeededToReachThreeBlocks() {
        assertEquals(96.8D, WaterMovementGoal.shallowAquaticSleepY(99.0D, 100.0D), 0.0D);
        assertEquals(96.8D, WaterMovementGoal.shallowAquaticSleepY(98.0D, 100.0D), 0.0D);
        assertEquals(96.8D, WaterMovementGoal.shallowAquaticSleepY(97.0D, 100.0D), 0.0D);
    }

    @Test public void alreadySubmergedSleeperKeepsItsCurrentDepth() {
        assertEquals(96.0D, WaterMovementGoal.shallowAquaticSleepY(96.0D, 100.0D), 0.0D);
        assertEquals(95.0D, WaterMovementGoal.shallowAquaticSleepY(95.0D, 100.0D), 0.0D);
    }

    @Test public void surfaceRuleExcludesGyaradosLaprasAndWaterVisitors() {
        assertTrue(SpeciesBehaviorProfiles.usesSwimmingSleepAnimation("magikarp"));
        assertTrue(SpeciesBehaviorProfiles.locksPositionDuringWaterSleep("gyarados"));
        assertFalse(SpeciesBehaviorProfiles.locksPositionDuringWaterSleep("lapras"));
        assertFalse(SpeciesBehaviorProfiles.locksPositionDuringWaterSleep("krabby"));
        assertFalse(SpeciesBehaviorProfiles.usesSwimmingSleepAnimation("gyarados"));
        assertFalse(SpeciesBehaviorProfiles.usesSwimmingSleepAnimation("lapras"));
        assertFalse(SpeciesBehaviorProfiles.usesSwimmingSleepAnimation("krabby"));
    }
}
