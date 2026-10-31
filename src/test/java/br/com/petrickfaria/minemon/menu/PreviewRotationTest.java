package br.com.petrickfaria.minemon.menu;

import br.com.petrickfaria.minemon.entity.SpeciesDefinitions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class PreviewRotationTest {
    private static final Set<String> APPROVED = Set.of("bulbasaur", "pikachu", "charmander", "caterpie");

    @Test public void all151PreviewsKeepOneScaleAtEveryAngle() {
        for (var definition : SpeciesDefinitions.ALL.values()) {
            var fit = PreviewBounds.fit(definition.slug(), definition.scale());
            assertTrue(definition.slug(), fit.scale() > 0);
            for (int yaw = 0; yaw < 360; yaw += 15)
                for (int pitch = -46; pitch <= 34; pitch += 10)
                    assertEquals(definition.slug(), fit.scale(), PreviewBounds.rotationScale(
                            definition.slug(), definition.scale(), fit.scale(), yaw, pitch), 0);
        }
    }

    @Test public void previewScaleFitsEverySupportedAngleAndKeepsApprovedReferences() throws Exception {
        Path root = Path.of(System.getProperty("minemon.testResources"));
        JsonObject bounds = JsonParser.parseString(Files.readString(root.resolve("data/minemon_gen1/preview_bounds.json"))).getAsJsonObject();
        JsonObject pivots = JsonParser.parseString(Files.readString(root.resolve("data/minemon_gen1/preview_body_pivots.json"))).getAsJsonObject();
        assertEquals(151, pivots.size());
        for (var definition : SpeciesDefinitions.ALL.values()) {
            String slug = definition.slug();
            JsonArray b = bounds.getAsJsonArray(slug);
            double width = b.get(3).getAsDouble() - b.get(0).getAsDouble();
            double height = b.get(4).getAsDouble() - b.get(1).getAsDouble();
            double depth = b.get(5).getAsDouble() - b.get(2).getAsDouble();
            double projectedWidth, projectedHeight;
            if (APPROVED.contains(slug)) {
                projectedWidth = width + depth * Math.sin(Math.toRadians(4));
                projectedHeight = height * Math.cos(Math.toRadians(6)) + depth * Math.sin(Math.toRadians(6));
            } else {
                double[] extents = PreviewBounds.maximumRotatedExtents(width, height, depth);
                projectedWidth = extents[0]; projectedHeight = extents[1];
            }
            float expected = (float) Math.min(100, Math.min(
                    232 / Math.max(.2, projectedWidth * definition.scale() * 1.3),
                    88 / Math.max(.2, projectedHeight * definition.scale() * 1.3)));
            float actual = PreviewBounds.fit(slug, definition.scale()).scale();
            assertEquals(slug, expected, actual, 0);
            if (!APPROVED.contains(slug)) {
                assertTrue(slug + " width", projectedWidth * definition.scale() * actual * 1.3 <= 232.001);
                assertTrue(slug + " height", projectedHeight * definition.scale() * actual * 1.3 <= 88.001);
            }
        }
        assertEquals(JsonParser.parseString("[0.0,0.3096574673545027,0.06510470127623136]"), pivots.getAsJsonObject("bulbasaur").getAsJsonArray("center"));
        assertEquals(JsonParser.parseString("[0.0,0.3125,0.03125]"), pivots.getAsJsonObject("pikachu").getAsJsonArray("center"));
        assertEquals(JsonParser.parseString("[0.0,0.34375,0.0]"), pivots.getAsJsonObject("charmander").getAsJsonArray("center"));
        assertEquals(JsonParser.parseString("[0.0,0.28125,-0.234375]"), pivots.getAsJsonObject("caterpie").getAsJsonArray("center"));
    }

    @Test public void nonReferencePivotsUseTheirOwnMeasuredMeshCenters() throws Exception {
        Path root = Path.of(System.getProperty("minemon.testResources"));
        JsonObject bounds = JsonParser.parseString(Files.readString(root.resolve("data/minemon_gen1/preview_bounds.json"))).getAsJsonObject();
        JsonObject pivots = JsonParser.parseString(Files.readString(root.resolve("data/minemon_gen1/preview_body_pivots.json"))).getAsJsonObject();
        for (var definition : SpeciesDefinitions.ALL.values()) {
            String slug = definition.slug();
            if (APPROVED.contains(slug)) continue;
            JsonArray b = bounds.getAsJsonArray(slug);
            JsonArray center = pivots.getAsJsonObject(slug).getAsJsonArray("center");
            assertEquals(slug, "geometry_center", pivots.getAsJsonObject(slug).get("bone").getAsString());
            for (int axis = 0; axis < 3; axis++)
                assertEquals(slug + " axis " + axis,
                        (b.get(axis).getAsDouble() + b.get(axis + 3).getAsDouble()) / 2,
                        center.get(axis).getAsDouble(), 1e-9);
        }
    }
}
