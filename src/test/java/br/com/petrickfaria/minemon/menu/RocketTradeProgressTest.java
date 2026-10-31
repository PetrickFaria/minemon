package br.com.petrickfaria.minemon.menu;

import br.com.petrickfaria.minemon.registry.RocketChallengeManager;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public final class RocketTradeProgressTest {
    @Test public void tradeProgressTracksCurrentLevelAndCapsAtLevelTen() {
        int levelOneTrades = RocketChallengeManager.requiredTradesForNextLevel(1);
        assertEquals(0.0F, RocketChallengeManager.tradeProgress(1, 0), 0.0F);
        assertEquals(0.5F, RocketChallengeManager.tradeProgress(1, levelOneTrades / 2), 0.001F);
        assertEquals(1.0F, RocketChallengeManager.tradeProgress(1, levelOneTrades), 0.0F);
        assertEquals(1.0F, RocketChallengeManager.tradeProgress(1, levelOneTrades + 20), 0.0F);

        int levelTwoTrades = RocketChallengeManager.requiredTradesForNextLevel(2);
        assertEquals((float) (levelTwoTrades / 3) / levelTwoTrades,
                RocketChallengeManager.tradeProgress(2, levelTwoTrades / 3), 0.001F);
        assertEquals(1.0F, RocketChallengeManager.tradeProgress(10, 0), 0.0F);
        assertEquals(Math.round(RocketChallengeManager.tradeProgress(1, levelOneTrades / 2) * 102),
                RocketTradeScreen.progressPixels(1, levelOneTrades / 2, 102));
        assertEquals(102, RocketTradeScreen.progressPixels(10, 0, 102));
    }
}
