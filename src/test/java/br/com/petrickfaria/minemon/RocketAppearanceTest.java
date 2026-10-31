package br.com.petrickfaria.minemon;

import br.com.petrickfaria.minemon.client.RocketVisuals;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public final class RocketAppearanceTest {
    private static final List<String> ROCKET_SPECIES = List.of(
            "zubat", "rattata", "ekans", "koffing", "golbat", "raticate", "arbok", "weezing", "kangaskhan", "mewtwo");

    @Test public void rocketSequenceKeepsWeezingAtLevelEight() {
        assertEquals("weezing", ROCKET_SPECIES.get(7));
    }

    @Test public void eachRocketSpeciesHasItsOwnRegisteredModelAndTexture() throws Exception {
        var resource = RocketAppearanceTest.class.getResourceAsStream("/data/minemon_gen1/runtime_species.json");
        assertNotNull(resource);
        var definitions = JsonParser.parseReader(new InputStreamReader(resource, StandardCharsets.UTF_8)).getAsJsonArray();
        for (String slug : ROCKET_SPECIES) {
            var definition = java.util.stream.StreamSupport.stream(definitions.spliterator(), false)
                    .map(element -> element.getAsJsonObject())
                    .filter(element -> slug.equals(element.get("slug").getAsString()))
                    .findFirst().orElseThrow();
            String model = definition.get("model").getAsString();
            String texture = definition.get("texture").getAsString();
            assertTrue(slug + " model must remain species-specific", model.contains("/" + slug + ".geo.json"));
            assertTrue(slug + " texture must remain species-specific", texture.contains("/" + slug + ".png"));
            assertFalse("Rocket texture must not be shared from Rattata", texture.endsWith("rattata_rocket.png"));
            var geometry = JsonParser.parseReader(new InputStreamReader(
                    RocketAppearanceTest.class.getResourceAsStream("/" + model.replace("assets/minemon/", "assets/minemon_gen1/")),
                    StandardCharsets.UTF_8)).getAsJsonObject();
            boolean hasEyeBone = false;
            for (var geometryEntry : geometry.getAsJsonArray("minecraft:geometry")) {
                for (var boneEntry : geometryEntry.getAsJsonObject().getAsJsonArray("bones")) {
                    var bone = boneEntry.getAsJsonObject();
                    if (RocketVisuals.isEyeCubeBone(bone.get("name").getAsString())
                            && bone.has("cubes") && !bone.getAsJsonArray("cubes").isEmpty()) hasEyeBone = true;
                }
            }
            assertTrue(slug + " needs eye mesh bones for the Rocket-only red overlay", hasEyeBone);
        }
    }

    @Test public void overlaySelectsEyeMeshesAndRejectsFaceAndLidBones() {
        for (String eye : List.of("eye_left", "pupil_right", "iris_left", "small_eye_right", "small_pupil_left",
                "kid_eye_angry_right", "eye_emote_left", "eyes_ditto", "eyes"))
            assertTrue(eye, RocketVisuals.isEyeCubeBone(eye));
        for (String nonEye : List.of("head", "face", "eyelid_left", "eyelid_happy_right", "eyebrow_left", "body"))
            assertFalse(nonEye, RocketVisuals.isEyeCubeBone(nonEye));
    }
}
