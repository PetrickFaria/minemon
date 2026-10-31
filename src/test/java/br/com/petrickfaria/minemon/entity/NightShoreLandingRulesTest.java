package br.com.petrickfaria.minemon.entity;

import net.minecraft.world.phys.AABB;
import org.junit.Test;
import static org.junit.Assert.*;

public final class NightShoreLandingRulesTest {
    @Test public void searchVolumeIsExactlyFiftyBlocksOnEachAxis() {
        assertTrue(NightShoreLandingRules.insideSearchVolume(-25, -25, -25));
        assertTrue(NightShoreLandingRules.insideSearchVolume(24, 24, 24));
        assertFalse(NightShoreLandingRules.insideSearchVolume(-26, 0, 0));
        assertFalse(NightShoreLandingRules.insideSearchVolume(25, 0, 0));
        assertFalse(NightShoreLandingRules.insideSearchVolume(0, 25, 0));
        assertFalse(NightShoreLandingRules.insideSearchVolume(0, -26, 0));
    }

    @Test public void shorelineDistanceUsesTheWholeHorizontalHitbox() {
        AABB wideLandingBox = new AABB(0.0D, 64.0D, 0.0D, 3.0D, 68.0D, 2.0D);
        assertTrue(NightShoreLandingRules.waterIsTooClose(wideLandingBox, 5, 0, 3.0D));
        assertFalse(NightShoreLandingRules.waterIsTooClose(wideLandingBox, 6, 0, 3.0D));
        assertTrue(NightShoreLandingRules.waterIsTooClose(wideLandingBox, 3, 4, 3.0D));
    }
}
