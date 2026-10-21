package br.com.petrickfaria.minemon;

import com.google.gson.*;
import org.junit.Test;
import software.bernie.geckolib3.core.molang.MolangParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class Batch6AnimationCompatibilityTest {
    private static final List<String> SPECIES=List.of(
        "dugtrio","meowth","persian","psyduck","golduck",
        "mankey","primeape","growlithe","arcanine","poliwag");

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
            assertFalse(text.contains("q."));
            assertFalse(text.contains("query.r."));
            var expression=parser.parseExpression(text);
            for(int i=0;i<=20;i++) {
                parser.setValue("query.anim_time",i/20.0);
                assertTrue(text,Double.isFinite(expression.get()));
            }
        }
    }

    @Test public void allBatch6ClipsParseAndLoadInGeckoLib3() throws Exception {
        for(String slug:SPECIES) {
            var clips=animations(slug);
            assertFalse("No imported clips for "+slug,clips.entrySet().isEmpty());
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

    @Test public void growlitheWalkUsesSourcePoserQuadrupedJointsAndCadence() throws Exception {
        var clip=animations("growlithe").getAsJsonObject("animation.growlithe.minemon_poser_walk");
        var bones=clip.getAsJsonObject("bones");
        double frequency=720.0*0.66/0.6662;
        for(var joint:List.of("leg_front_left","leg_front_right","leg_back_left","leg_back_right")) {
            var rotation=bones.getAsJsonObject(joint).getAsJsonArray("rotation").get(0).getAsString();
            assertTrue(rotation,rotation.contains("713.299310"));
            var parser=new MolangParser();var expression=parser.parseExpression(rotation);
            double sign=joint.equals("leg_front_left")||joint.equals("leg_back_right")?1.0:-1.0;
            for(int i=0;i<=80;i++) {
                double time=i/80.0;parser.setValue("query.anim_time",time);
                assertEquals(joint,sign*42.0*Math.cos(Math.toRadians(time*frequency)),expression.get(),0.0001);
            }
        }
    }

    @Test public void arcanineRideClipsUseNeutralInputsAndRetainCryEvent() throws Exception {
        var clips=animations("arcanine");
        for(String name:List.of("animation.arcanine.ride_ground_walk","animation.arcanine.ride_ground_run")) {
            String json=clips.getAsJsonObject(name).getAsJsonObject("bones").toString();
            assertFalse(json,json.contains("query.r."));
        }
        var cry=clips.getAsJsonObject("animation.arcanine.cry");
        assertEquals("pokemon.arcanine.cry",cry.getAsJsonObject("sound_effects").getAsJsonObject("0.4167").get("effect").getAsString());
    }

    @Test public void missingWalksAndPlaceholderWaterSpeciesStayExplicit() throws Exception {
        var root=Path.of(System.getProperty("minemon.testResources"),"data/minemon_gen1/runtime_species.json");
        JsonArray defs;
        try(var reader=Files.newBufferedReader(root)) { defs=JsonParser.parseReader(reader).getAsJsonArray(); }
        for(String slug:List.of("meowth","persian")) {
            JsonObject def=null;
            for(JsonElement element:defs)if(element.getAsJsonObject().get("slug").getAsString().equals(slug)) {
                def=element.getAsJsonObject();break;
            }
            assertNotNull(def);
            assertEquals("idle",def.get("movement_fallback").getAsString());
            assertEquals(def.get("idle").getAsString(),def.get("walk").getAsString());
        }
        var poliwag=animations("poliwag");
        assertTrue(poliwag.has("animation.poliwag.ground_idle_placeholder"));
        assertTrue(poliwag.has("animation.poliwag.ground_walk_placeholder"));
        assertTrue(poliwag.has("animation.poliwag.water_idle_placeholder"));
        assertTrue(poliwag.has("animation.poliwag.water_swim_placeholder"));
    }
}
