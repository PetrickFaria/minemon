package br.com.petrickfaria.minemon;

import br.com.petrickfaria.minemon.entity.ModelMetrics;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class GyaradosSurfaceTest {
    @Test public void renderAnchorPlacesTenPercentOfMeasuredBodyBelowWater() {
        double surface = 64.0D;
        double bottom = ModelMetrics.bodyBottom("gyarados");
        double height = ModelMetrics.bodyHeight("gyarados");
        double scale = 1.0D;
        double anchor = ModelMetrics.anchorForSubmergedFraction(surface, bottom, height, scale, 0.10D);
        double submerged = surface - (anchor + bottom * scale);
        assertEquals(height * scale * 0.10D, submerged, 1.0E-9D);
    }
}
