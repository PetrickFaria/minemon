package br.com.petrickfaria.minemon.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Small read-only index generated from the source JAR's species and resource files. */
public final class SpeciesAssetIndex {
    private static final Map<String, Entry> ENTRIES = load();

    public static String resource(String slug, String category, String fallback) {
        Entry entry = ENTRIES.get(slug);
        if (entry == null) return fallback;
        String path = switch (category) {
            case "models" -> entry.model;
            case "textures" -> entry.texture;
            case "animations" -> entry.animation;
            default -> null;
        };
        if (path == null || !path.startsWith("assets/minemon/")) return fallback;
        return path.substring("assets/minemon/".length());
    }

    public static String clip(String slug, String preferred, String fallback) {
        Entry entry = ENTRIES.get(slug);
        if (entry == null) return null;
        if (entry.clips.contains(preferred)) return "animation." + slug + "." + preferred;
        if (entry.clips.contains(fallback)) return "animation." + slug + "." + fallback;
        return null;
    }

    private static Map<String, Entry> load() {
        Map<String, Entry> result = new HashMap<>();
        try (InputStream input = SpeciesAssetIndex.class.getResourceAsStream("/data/minemon/species_catalog.json")) {
            if (input == null) return Collections.emptyMap();
            JsonArray root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement element : root) {
                JsonObject species = element.getAsJsonObject();
                String slug = species.get("slug").getAsString();
                JsonObject resources = species.getAsJsonObject("resources");
                Set<String> clips = new HashSet<>();
                if (species.has("animationClips")) {
                    for (JsonElement clip : species.getAsJsonArray("animationClips")) clips.add(clip.getAsString());
                }
                result.put(slug, new Entry(first(resources, "models"), first(resources, "textures"),
                        first(resources, "animations"), clips));
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load Minemon species resource catalog", exception);
        }
        return Collections.unmodifiableMap(result);
    }

    private static String first(JsonObject resources, String category) {
        JsonArray paths = resources.getAsJsonArray(category);
        return paths == null || paths.isEmpty() ? null : paths.get(0).getAsString();
    }

    private record Entry(String model, String texture, String animation, Set<String> clips) {}

    private SpeciesAssetIndex() {}
}
