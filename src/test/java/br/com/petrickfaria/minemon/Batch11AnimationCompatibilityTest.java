package br.com.petrickfaria.minemon;

import com.google.gson.*;
import org.junit.Test;
import software.bernie.geckolib3.core.molang.MolangParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class Batch11AnimationCompatibilityTest {
    private static final List<String> SPECIES=List.of(
        "electrode","exeggcute","exeggutor","cubone","marowak",
        "hitmonlee","hitmonchan","lickitung","koffing","weezing");
    private static final Path RESOURCES=Path.of(System.getProperty("minemon.testResources"));

    private static JsonObject json(String path) throws Exception {
        try(var reader=Files.newBufferedReader(RESOURCES.resolve(path))) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static JsonArray jsonArray(String path) throws Exception {
        try(var reader=Files.newBufferedReader(RESOURCES.resolve(path))) {
            return JsonParser.parseReader(reader).getAsJsonArray();
        }
    }

    private static JsonObject animations(String slug) throws Exception {
        return json("assets/minemon_gen1/animations/"+slug+"_cobblemon_1_8_2/"+slug+".animation.json")
            .getAsJsonObject("animations");
    }

    @Test public void exactBatchElevenSpeciesModesAndNativeRunAreDeclared() throws Exception {
        var defs=jsonArray("data/minemon_gen1/runtime_species.json");
        var profiles=json("data/minemon_gen1/animation_validation_profiles.json");
        for(int number=101;number<=110;number++) {
            var def=defs.get(number-1).getAsJsonObject();
            String slug=def.get("slug").getAsString();
            assertEquals("Pokédex order #"+number,SPECIES.get(number-101),slug);
            assertEquals("1.8.2",def.get("source_cobblemon_version").getAsString());
            assertEquals(slug+".geo.json",def.get("source_model_variant").getAsString());
            for(String key:List.of("model","texture","animation"))
                assertTrue(def.get(key).getAsString(),def.get(key).getAsString().contains(slug+"_cobblemon_1_8_2/"));
            assertTrue("Missing source articulation profile for "+slug,profiles.has(slug));
            assertFalse("No generated test-only gait for "+slug,def.getAsJsonObject("clips").has("ground_run")
                && !slug.equals("lickitung"));
            for(var entry:def.getAsJsonObject("clips").entrySet())
                assertFalse("No synthetic placeholder action for "+slug+"/"+entry.getKey(),
                    entry.getValue().getAsString().contains(".minemon_"));
        }
        for(String slug:List.of("cubone","marowak")) {
            var def=defs.get(SPECIES.indexOf(slug)+100).getAsJsonObject();
            assertEquals("idle",def.get("movement_fallback").getAsString());
            assertTrue(def.get("source_static_idle").getAsBoolean());
        }
        for(String slug:List.of("koffing","weezing"))
            assertEquals("Kanto source behavior selects flying mode", "flying",
                defs.get(SPECIES.indexOf(slug)+100).getAsJsonObject().get("mode").getAsString());
        assertEquals("animation.lickitung.ground_run",defs.get(107).getAsJsonObject()
            .getAsJsonObject("clips").get("ground_run").getAsString());
        var hitmonchan=animations("hitmonchan");
        assertTrue("Retain the source WIP run without selecting it",hitmonchan.has("animation.hitmonchan.ground_run_WIP"));
        assertFalse("WIP run must not be active",defs.get(106).getAsJsonObject().getAsJsonObject("clips").has("ground_run"));
    }

    @Test public void selectedNativeClipsParseAndLoadInGeckoLib31() throws Exception {
        var defs=jsonArray("data/minemon_gen1/runtime_species.json");
        for(int number=101;number<=110;number++) {
            var def=defs.get(number-1).getAsJsonObject();
            String slug=def.get("slug").getAsString();
            var clips=animations(slug);
            var selected=new LinkedHashSet<String>();
            selected.add(def.get("idle").getAsString());
            selected.add(def.get("walk").getAsString());
            if(def.getAsJsonObject("clips").has("ground_run"))
                selected.add(def.getAsJsonObject("clips").get("ground_run").getAsString());
            for(String name:selected) {
                assertTrue(slug+" missing active clip "+name,clips.has(name));
                JsonObject clip=clips.getAsJsonObject(name);
                var parser=new MolangParser();
                parser.setValue("query.anim_time",0.0);
                checkExpressions(clip.get("bones"),parser,slug+"/"+name);
                var loaded=software.bernie.geckolib3.util.json.JsonAnimationUtils.deserializeJsonToAnimation(
                    new java.util.AbstractMap.SimpleImmutableEntry<String,JsonElement>(name,clip),parser);
                assertTrue(slug+"/"+name,Double.isFinite(loaded.animationLength));
            }
        }
    }

    @Test public void sourceAnatomyAndCharacteristicClipsRemainIntact() throws Exception {
        Map<String,List<String>> anatomy=Map.ofEntries(
            Map.entry("electrode",List.of("body")),
            Map.entry("exeggcute",List.of("egg1","egg2","egg3","egg4","egg5","egg6")),
            Map.entry("exeggutor",List.of("head","head2","head3")),
            Map.entry("cubone",List.of("head","bone","left_upper_arm","right_upper_arm")),
            Map.entry("marowak",List.of("head","bone_right_hand","right_hand","right_upper_arm")),
            Map.entry("hitmonlee",List.of("leg_left","leg_right","foot_left","foot_right")),
            Map.entry("hitmonchan",List.of("arm_left","arm_right","hand_left","hand_right")),
            Map.entry("lickitung",List.of("tongue","tongue2","tongue3","tongue4")),
            Map.entry("koffing",List.of("body","body_inflate","craters")),
            Map.entry("weezing",List.of("main_body","middle_body","small_body")));
        for(var entry:anatomy.entrySet()) {
            String slug=entry.getKey();
            var geometry=json("assets/minemon_gen1/geo/"+slug+"_cobblemon_1_8_2/"+slug+".geo.json")
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            var bones=new java.util.HashSet<String>();
            for(var bone:geometry.getAsJsonArray("bones"))bones.add(bone.getAsJsonObject().get("name").getAsString());
            assertTrue(slug,bones.containsAll(entry.getValue()));
        }
        assertTrue(animations("exeggcute").has("animation.exeggcute.blink6"));
        assertTrue(animations("exeggutor").has("animation.exeggutor.ground_walk"));
        assertTrue(animations("cubone").has("animation.cubone.test"));
        assertTrue(animations("marowak").has("animation.marowak.cry"));
        assertTrue(animations("lickitung").has("animation.lickitung.ground_run"));
        assertTrue(animations("koffing").has("animation.koffing.air_fly"));
        assertTrue(animations("weezing").has("animation.weezing.air_fly"));
    }

    private static void checkExpressions(JsonElement value,MolangParser parser,String source) throws Exception {
        if(value==null||value.isJsonNull())return;
        if(value.isJsonArray()) {
            for(var child:value.getAsJsonArray())checkExpressions(child,parser,source);
        } else if(value.isJsonObject()) {
            for(var entry:value.getAsJsonObject().entrySet())checkExpressions(entry.getValue(),parser,source);
        } else if(value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString()) {
            String expression=value.getAsString();
            assertFalse(source+" retains Bedrock q. syntax: "+expression,expression.matches("(?s).*\\bq\\.[A-Za-z_].*"));
            assertFalse(source+" uses unsupported GeckoLib 3.1.40 query.r: "+expression,expression.matches("(?s).*\\bquery\\.r\\.[A-Za-z_][A-Za-z0-9_]*\\s*\\(.*"));
            var parsed=parser.parseExpression(expression);
            for(int tick=0;tick<=20;tick++) {
                parser.setValue("query.anim_time",tick/20.0);
                assertTrue(source+" non-finite Molang "+expression,Double.isFinite(parsed.get()));
            }
        }
    }
}
