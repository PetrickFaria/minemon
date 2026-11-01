package br.com.petrickfaria.minemon.menu;

import br.com.petrickfaria.minemon.registry.RocketChallengeManager;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public final class RocketTradeProgressTest {
    @Test public void tradeProgressTracksAllRocketLevelThresholdsAndCapsAtTen() {
        int[] required = {5, 15, 25, 35, 45, 55, 65, 75, 85};
        for (int level = 1; level <= 9; level++) {
            assertEquals("level " + level + " empty", 0.0F,
                    RocketChallengeManager.tradeProgress(level, 0), 0.0F);
            assertEquals("level " + level + " threshold",
                    1.0F, RocketChallengeManager.tradeProgress(level, required[level - 1]), 0.0F);
            assertEquals("level " + level + " clamps",
                    1.0F, RocketChallengeManager.tradeProgress(level, required[level - 1] + 20), 0.0F);
        }
        assertEquals(0.0F, RocketChallengeManager.tradeProgress(1, 0), 0.0F);
        assertEquals(0.4F, RocketChallengeManager.tradeProgress(1, 2), 0.001F);
        assertEquals(0.8F, RocketChallengeManager.tradeProgress(1, 4), 0.001F);
        assertEquals(0.0F, RocketChallengeManager.tradeProgress(2, 0), 0.0F);
        assertEquals(1.0F, RocketChallengeManager.tradeProgress(10, 0), 0.0F);
        assertEquals(41, RocketTradeScreen.progressPixels(1, 2, 102));
        assertEquals(82, RocketTradeScreen.progressPixels(1, 4, 102));
        assertEquals(0, RocketTradeScreen.progressPixels(2, 0, 102));
        assertEquals(102, RocketTradeScreen.progressPixels(10, 0, 102));
        assertEquals(0.4F, RocketChallengeManager.experienceProgress(1, 2), 0.001F);
        assertEquals(0.0F, RocketChallengeManager.experienceProgress(2, 0), 0.0F);
        assertEquals(1.0F, RocketChallengeManager.experienceProgress(10, 0), 0.0F);
    }
}
