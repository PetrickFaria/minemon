package br.com.petrickfaria.minemon;

import com.google.gson.*;
import org.junit.Test;
import software.bernie.geckolib3.core.molang.MolangParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class Batch14AnimationCompatibilityTest {
    private static final List<String> SPECIES=List.of("lapras","ditto","eevee","vaporeon","jolteon","flareon","porygon","omanyte","omastar","kabuto");
    private static final Path RESOURCES=Path.of(System.getProperty("minemon.testResources"));
    private static JsonObject json(String path) throws Exception {
        try(var reader=Files.newBufferedReader(RESOURCES.resolve(path))) { return JsonParser.parseReader(reader).getAsJsonObject(); }
    }
    private static JsonArray defs() throws Exception {
        try(var reader=Files.newBufferedReader(RESOURCES.resolve("data/minemon_gen1/runtime_species.json"))) { return JsonParser.parseReader(reader).getAsJsonArray(); }
    }
    private static JsonObject animations(String slug) throws Exception {
        return json("assets/minemon_gen1/animations/"+slug+"_cobblemon_1_8_2/"+slug+".animation.json").getAsJsonObject("animations");
    }

    @Test public void exactBatchFourteenAndSourceBackedLocomotionAreDeclared() throws Exception {
        var definitions=defs(); var profiles=json("data/minemon_gen1/animation_validation_profiles.json");
        for(int number=131;number<=140;number++) {
            var def=definitions.get(number-1).getAsJsonObject(); String slug=def.get("slug").getAsString();
            assertEquals("National Pokédex #"+number,SPECIES.get(number-131),slug);
            assertEquals("1.8.2",def.get("source_cobblemon_version").getAsString());
            assertTrue("Source-backed model variant "+slug,def.get("source_model_variant").getAsString().startsWith(slug));
            for(String key:List.of("model","texture","animation")) assertTrue(slug+"/"+key,def.get(key).getAsString().contains(slug+"_cobblemon_1_8_2/"));
            assertTrue("Missing articulation profile "+slug,profiles.has(slug));
            for(var clip:def.getAsJsonObject("clips").entrySet()) assertFalse("No invented action "+slug+"/"+clip.getKey(),clip.getValue().getAsString().contains(".minemon_"));
        }
        assertTrue(definitions.get(136).getAsJsonObject().getAsJsonObject("clips").has("ground_run"));
        for(String slug:List.of("lapras","ditto","eevee","vaporeon","jolteon","flareon","omanyte","omastar","kabuto"))
            assertFalse("No native run exists for "+slug,definitions.get(SPECIES.indexOf(slug)+130).getAsJsonObject().getAsJsonObject("clips").has("ground_run"));
        for(String slug:List.of("eevee","vaporeon","jolteon","flareon")) {
            var d=definitions.get(SPECIES.indexOf(slug)+130).getAsJsonObject();
            assertTrue("Static source pose is explicit: "+slug,d.get("source_static_idle").getAsBoolean());
            assertEquals("static",d.get("movement_fallback").getAsString());
            assertEquals(d.get("idle").getAsString(),d.get("walk").getAsString());
        }
        for(String slug:List.of("lapras","omanyte","omastar")) {
            var clips=definitions.get(SPECIES.indexOf(slug)+130).getAsJsonObject().getAsJsonObject("clips");
            assertTrue("Keep aquatic idle "+slug,clips.has("water_idle"));
            assertTrue("Keep aquatic swim "+slug,clips.has("water_swim"));
        }
    }

    @Test public void selectedNativeClipsParseAndLoadInGeckoLib31() throws Exception {
        var definitions=defs();
        for(int number=131;number<=140;number++) {
            var def=definitions.get(number-1).getAsJsonObject(); String slug=def.get("slug").getAsString(); var clips=animations(slug);
            var selected=new LinkedHashSet<String>(); selected.add(def.get("idle").getAsString()); selected.add(def.get("walk").getAsString());
            if(def.getAsJsonObject("clips").has("ground_run")) selected.add(def.getAsJsonObject("clips").get("ground_run").getAsString());
            for(String name:selected) {
                assertTrue(slug+" missing active clip "+name,clips.has(name)); JsonObject clip=clips.getAsJsonObject(name); var parser=new MolangParser();
                parser.setValue("query.anim_time",0.0); checkExpressions(clip.get("bones"),parser,slug+"/"+name);
                var loaded=software.bernie.geckolib3.util.json.JsonAnimationUtils.deserializeJsonToAnimation(new java.util.AbstractMap.SimpleImmutableEntry<String,JsonElement>(name,clip),parser);
                assertTrue(slug+"/"+name,Double.isFinite(loaded.animationLength));
            }
        }
    }

    @Test public void signatureAnatomyAquaticModesAndPivotsRemainInSourceRigs() throws Exception {
        Map<String,List<String>> anatomy=Map.ofEntries(
            Map.entry("lapras",List.of("shell","neck2","head","tail2","leg_front_left","leg_back_right")),
            Map.entry("ditto",List.of("body","head","arm_left","arm_right","mouth")),
            Map.entry("eevee",List.of("head","tail1","tail4","front_leg_left","back_leg_right")),
            Map.entry("vaporeon",List.of("frill_main","fin","tail6","frontleg_left","back_leg_right")),
            Map.entry("jolteon",List.of("frill_main","tail_left","tail_right","front_leg_left","backleg_right")),
            Map.entry("flareon",List.of("mane_main","tail6","fur_tip_top1","front_leg_left","back_leg_right")),
            Map.entry("porygon",List.of("head","beak","tail2","leg_left","leg_right")),
            Map.entry("omanyte",List.of("shell","front_leg_middle","left_back_leg","right_hand")),
            Map.entry("omastar",List.of("shell","front_left_tentacle_pair3","mid_right_tentacle_pair2","back_left_tentacle_pair")),
            Map.entry("kabuto",List.of("body","left_front_leg","left_back_leg","right_front_leg","right_back_leg")));
        for(var entry:anatomy.entrySet()) {
            String slug=entry.getKey(); var geometry=json("assets/minemon_gen1/geo/"+slug+"_cobblemon_1_8_2/"+slug+".geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            var bones=new HashSet<String>(); for(var bone:geometry.getAsJsonArray("bones")) bones.add(bone.getAsJsonObject().get("name").getAsString());
            assertTrue(slug,bones.containsAll(entry.getValue()));
            for(var bone:geometry.getAsJsonArray("bones")) {
                var b=bone.getAsJsonObject();
                if(b.has("parent")) assertTrue(slug+" parent "+b.get("name"),bones.contains(b.get("parent").getAsString()));
                if(b.has("pivot")) for(var value:b.getAsJsonArray("pivot")) assertTrue(slug+" finite pivot",Double.isFinite(value.getAsDouble()));
            }
        }
    }

    private static void checkExpressions(JsonElement value,MolangParser parser,String source) throws Exception {
        if(value==null||value.isJsonNull())return;
        if(value.isJsonArray()) for(var child:value.getAsJsonArray()) checkExpressions(child,parser,source);
        else if(value.isJsonObject()) for(var entry:value.getAsJsonObject().entrySet()) checkExpressions(entry.getValue(),parser,source);
        else if(value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString()) {
            String expression=value.getAsString();
            assertFalse(source+" retains Bedrock q. syntax: "+expression,expression.matches("(?s).*\\bq\\.[A-Za-z_].*"));
            assertFalse(source+" uses unsupported GeckoLib 3.1.40 query.r: "+expression,expression.matches("(?s).*\\bquery\\.r\\.[A-Za-z_][A-Za-z0-9_]*\\s*\\(.*"));
            var parsed=parser.parseExpression(expression);
            for(int tick=0;tick<=20;tick++){parser.setValue("query.anim_time",tick/20.0);assertTrue(source+" non-finite Molang "+expression,Double.isFinite(parsed.get()));}
        }
    }
}
