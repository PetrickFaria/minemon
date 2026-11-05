package br.com.petrickfaria.minemon;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import static org.junit.Assert.*;

public class Minemon175AssetsTest {
    @Test public void pokeballUsesSpriteNormallyAndSeparateThreeDimensionalCaptureModel() throws Exception {
        Path root = Path.of(System.getProperty("minemon.testResources"));
        var normal = JsonParser.parseString(Files.readString(root.resolve("assets/minemon/models/item/pokeball.json"))).getAsJsonObject();
        assertEquals("minecraft:item/generated", normal.get("parent").getAsString());
        assertEquals("minemon:item/pokeball", normal.getAsJsonObject("textures").get("layer0").getAsString());
        assertEquals("minemon_gen1:capture_animation", normal.getAsJsonArray("overrides").get(0).getAsJsonObject()
                .getAsJsonObject("predicate").entrySet().iterator().next().getKey());
        assertEquals("minemon:item/pokeball_3d", normal.getAsJsonArray("overrides").get(0).getAsJsonObject()
                .get("model").getAsString());
        var capture = JsonParser.parseString(Files.readString(root.resolve("assets/minemon/models/item/pokeball_3d.json"))).getAsJsonObject();
        assertTrue(capture.has("elements"));
        assertTrue(capture.getAsJsonArray("elements").size() >= 6);
        assertEquals("minecraft:item/generated", JsonParser.parseString(Files.readString(
                root.resolve("assets/minemon/models/item/master_ball.json"))).getAsJsonObject().get("parent").getAsString());
    }
}
