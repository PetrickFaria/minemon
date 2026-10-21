package br.com.petrickfaria.minemon;

import com.google.gson.*;
import org.junit.Test;
import software.bernie.geckolib3.core.molang.MolangParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class Batch7AnimationCompatibilityTest {
    private static final List<String> SPECIES=List.of(
        "poliwhirl","poliwrath","abra","kadabra","alakazam",
        "machop","machoke","machamp","bellsprout","weepinbell");

    private static Path animationPath(String slug) {
        return Path.of(System.getProperty("minemon.testResources"),
            "assets/minemon_gen1/animations/"+slug+"_cobblemon_1_8_2/"+slug+".animation.json");
    }

    private static JsonObject animations(String slug) throws Exception {
        try(var reader=Files.newBufferedReader(animationPath(slug))) {
            return JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");
        }
    }

    private static void checkExpressions(JsonElement value,MolangParser parser) throws Exception {
        if(value.isJsonArray()) {
            for(var item:value.getAsJsonArray())checkExpressions(item,parser);
        } else if(value.isJsonObject()) {
            for(var item:value.getAsJsonObject().entrySet())checkExpressions(item.getValue(),parser);
        } else if(value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString()) {
            String text=value.getAsString();
            assertFalse("Bedrock alias was not adapted: "+text,text.matches("(?s).*\\bq\\.[A-Za-z_].*"));
            var expression=parser.parseExpression(text);
            for(int i=0;i<=20;i++) {
                parser.setValue("query.anim_time",i/20.0);
                assertTrue(text,Double.isFinite(expression.get()));
            }
        }
    }

    @Test public void sourceAnimationsParseAndLoadInGeckoLib3() throws Exception {
        for(String slug:SPECIES) {
            var clips=animations(slug);
            assertFalse("No imported clips for "+slug,clips.entrySet().isEmpty());
            assertTrue("Native idle missing for "+slug,clips.has("animation."+slug+".ground_idle"));
            assertTrue("Native walk missing for "+slug,clips.has("animation."+slug+".ground_walk"));
            for(var entry:clips.entrySet()) {
                var clip=entry.getValue().getAsJsonObject();
                assertTrue(entry.getKey(),clip.has("animation_length"));
                assertTrue(entry.getKey(),clip.get("animation_length").getAsDouble()>0);
                if(clip.has("bones"))checkExpressions(clip.get("bones"),new MolangParser());
                var loaded=software.bernie.geckolib3.util.json.JsonAnimationUtils.deserializeJsonToAnimation(
                    new java.util.AbstractMap.SimpleImmutableEntry<String,JsonElement>(entry.getKey(),clip),new MolangParser());
                assertTrue(entry.getKey(),Double.isFinite(loaded.animationLength)&&loaded.animationLength>0);
            }
        }
    }

    @Test public void nativeRunsAndSourceStaticIdlesAreDeclaredAccurately() throws Exception {
        var root=Path.of(System.getProperty("minemon.testResources"),"data/minemon_gen1/runtime_species.json");
        JsonArray defs;
        try(var reader=Files.newBufferedReader(root)) { defs=JsonParser.parseReader(reader).getAsJsonArray(); }
        var profilePath=Path.of(System.getProperty("minemon.testResources"),"data/minemon_gen1/animation_validation_profiles.json");
        JsonObject profiles;
        try(var reader=Files.newBufferedReader(profilePath)) { profiles=JsonParser.parseReader(reader).getAsJsonObject(); }
        for(int number=61;number<=70;number++) {
            JsonObject def=defs.get(number-1).getAsJsonObject();
            String slug=def.get("slug").getAsString();
            assertTrue(def.get("model").getAsString(),def.get("model").getAsString().contains(slug+"_cobblemon_1_8_2"));
            assertTrue(def.get("texture").getAsString(),def.get("texture").getAsString().contains(slug+"_cobblemon_1_8_2"));
            assertTrue(def.get("animation").getAsString(),def.get("animation").getAsString().contains(slug+"_cobblemon_1_8_2"));
            assertTrue("Cobblemon has a fixed native idle for "+slug,def.get("source_static_idle").getAsBoolean());
            assertEquals(profiles.getAsJsonObject(slug).getAsJsonArray("idle"),profiles.getAsJsonObject(slug).getAsJsonArray("idle_static"));
            boolean hasNativeRun=List.of("abra","kadabra","alakazam").contains(slug);
            assertEquals("Only source-backed run clips are selected for "+slug,hasNativeRun,
                def.getAsJsonObject("clips").has("ground_run"));
            if(hasNativeRun)assertEquals("animation."+slug+".ground_run",def.getAsJsonObject("clips").get("ground_run").getAsString());
        }
    }

    @Test public void genderVariantModelsUseTheirMatchingAnimationRig() throws Exception {
        var root=Path.of(System.getProperty("minemon.testResources"),"data/minemon_gen1/runtime_species.json");
        JsonArray defs;
        try(var reader=Files.newBufferedReader(root)) { defs=JsonParser.parseReader(reader).getAsJsonArray(); }
        for(String slug:List.of("kadabra","alakazam")) {
            JsonObject def=defs.get(slug.equals("kadabra")?63:64).getAsJsonObject();
            assertTrue(def.get("source_model_variant").getAsString().contains("_male.geo.json"));
            assertTrue(def.get("source_molang_adaptations").getAsJsonObject().has("q."));
        }
    }
}
