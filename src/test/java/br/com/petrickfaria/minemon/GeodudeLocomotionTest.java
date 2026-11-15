package br.com.petrickfaria.minemon;

import br.com.petrickfaria.minemon.entity.GeodudeLocomotion;
import com.google.gson.*;
import java.nio.file.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class GeodudeLocomotionTest {
    @Test public void restAndOrdinaryMovementHoverWhileOnlyRunningRolls() throws Exception {
        assertEquals("ground_idle", GeodudeLocomotion.clipKey(false,false));
        assertEquals("ground_idle", GeodudeLocomotion.clipKey(true,false));
        assertEquals("ground_run", GeodudeLocomotion.clipKey(true,true));
        assertEquals("ground_idle", GeodudeLocomotion.clipKey(false,true));
        JsonObject clips=JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/minemon/animations/geodude_cobblemon_1_8_2/geodude.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
        for(String key:new String[]{"ground_walk","ground_run"}) {
            String rotation=clips.getAsJsonObject("animation.geodude."+key).getAsJsonObject("bones").getAsJsonObject("torso").get("rotation").toString();
            assertTrue("Source gait really rolls: "+key,rotation.contains("360"));
        }
        assertFalse(clips.getAsJsonObject("animation.geodude.ground_idle").getAsJsonObject("bones").toString().contains("360"));
    }
}
