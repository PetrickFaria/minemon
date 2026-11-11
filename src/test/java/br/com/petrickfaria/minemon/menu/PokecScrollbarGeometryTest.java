package br.com.petrickfaria.minemon.menu;

import org.junit.Test;
import static org.junit.Assert.*;

public class PokecScrollbarGeometryTest {
    @Test public void visibleBarCenterIsInsideHitAreaAtCommonGuiScales() {
        double left = 37.0, top = 19.0;
        int barX = PokecScreen.LIST_SCROLLBAR_X, barY = PokecScreen.LIST_SCROLLBAR_Y;
        int barWidth = PokecScreen.LIST_SCROLLBAR_WIDTH, barHeight = 66;
        for (float scale : new float[]{0.25F, 0.5F, 0.75F, 1.0F}) {
            double centerX = left + (barX + barWidth / 2.0) * scale;
            double centerY = top + (barY + barHeight / 2.0) * scale;
            assertTrue("bar center missed at GUI scale " + scale,
                    PokecScreen.containsScaledPoint(centerX, centerY, left, top, scale,
                            barX, barY, barWidth, barHeight));
            assertFalse("point left of visible bar was captured at GUI scale " + scale,
                    PokecScreen.containsScaledPoint(left + (barX * scale) - .01, centerY, left, top, scale,
                            barX, barY, barWidth, barHeight));
            assertFalse("right edge must stay outside at GUI scale " + scale,
                    PokecScreen.containsScaledPoint(left + (barX + barWidth) * scale, centerY, left, top, scale,
                            barX, barY, barWidth, barHeight));
        }
    }
}
