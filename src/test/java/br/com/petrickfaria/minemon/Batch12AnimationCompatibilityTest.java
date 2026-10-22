package br.com.petrickfaria.minemon;

import com.google.gson.*;
import org.junit.Test;
import software.bernie.geckolib3.core.molang.MolangParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class Batch12AnimationCompatibilityTest {
    private static final List<String> SPECIES=List.of("rhyhorn","rhydon","chansey","tangela","kangaskhan","horsea","seadra","goldeen","seaking","staryu");
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

    @Test public void exactBatchTwelveSpeciesAndNativeGaitsAreDeclared() throws Exception {
        var definitions=defs(); var profiles=json("data/minemon_gen1/animation_validation_profiles.json");
        for(int number=111;number<=120;number++) {
            var def=definitions.get(number-1).getAsJsonObject(); String slug=def.get("slug").getAsString();
            assertEquals("National Pokédex #"+number,SPECIES.get(number-111),slug);
            assertEquals("1.8.2",def.get("source_cobblemon_version").getAsString());
            assertTrue(List.of(slug+".geo.json",slug+"_male.geo.json",slug+"_female.geo.json").contains(def.get("source_model_variant").getAsString()));
            for(String key:List.of("model","texture","animation")) assertTrue(slug+"/"+key,def.get(key).getAsString().contains(slug+"_cobblemon_1_8_2/"));
            assertTrue("Missing articulation profile "+slug,profiles.has(slug));
            for(var clip:def.getAsJsonObject("clips").entrySet())
                assertFalse("No generated movement/action placeholder "+slug+"/"+clip.getKey(),clip.getValue().getAsString().contains(".minemon_"));
        }
        for(String slug:List.of("rhyhorn","rhydon","chansey","staryu"))
            assertTrue("Select only native source run for "+slug,defs().get(SPECIES.indexOf(slug)+110).getAsJsonObject().getAsJsonObject("clips").has("ground_run"));
        for(String slug:List.of("tangela","kangaskhan","horsea","seadra","goldeen","seaking"))
            assertFalse("Do not invent a source run for "+slug,defs().get(SPECIES.indexOf(slug)+110).getAsJsonObject().getAsJsonObject("clips").has("ground_run"));
        var tangela=definitions.get(113).getAsJsonObject();
        assertEquals("idle",tangela.get("movement_fallback").getAsString());
        assertTrue(tangela.get("source_static_idle").getAsBoolean());
        assertEquals(tangela.get("idle").getAsString(),tangela.get("walk").getAsString());
        assertTrue(tangela.get("source_resource_limit").getAsString().contains("no idle/walk/run clip or poser"));
        for(String slug:List.of("horsea","seadra","goldeen","seaking","staryu")) {
            var clips=defs().get(SPECIES.indexOf(slug)+110).getAsJsonObject().getAsJsonObject("clips");
            assertTrue("Preserve native water idle for "+slug,clips.has("water_idle"));
            assertTrue("Preserve native water swim for "+slug,clips.has("water_swim"));
        }
    }

    @Test public void selectedNativeClipsParseAndLoadInGeckoLib31() throws Exception {
        var definitions=defs();
        for(int number=111;number<=120;number++) {
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

    @Test public void characteristicAnatomyAndArticulatedSourceClipsAreRetained() throws Exception {
        Map<String,List<String>> anatomy=Map.ofEntries(
            Map.entry("rhyhorn",List.of("left_front_leg","right_front_leg","left_back_leg","right_back_leg")),
            Map.entry("rhydon",List.of("left_upper_arm","right_upper_arm","left_upper_leg","right_upper_leg","tail","tail2","tail3","tail4")),
            Map.entry("chansey",List.of("left_tendril_front","right_tendril_front","pouch","egg","tail1","tail2")),
            Map.entry("tangela",List.of("body","leftfoot","rightfoot")),
            Map.entry("kangaskhan",List.of("kid","kid_head","pouch","kid_tail","tail","tail2","tail3","tail4","tail5")),
            Map.entry("horsea",List.of("tail","tail2","fin")), Map.entry("seadra",List.of("tail","tail2","tail3","fin_left","fin_right")),
            Map.entry("goldeen",List.of("tail_left1","tail_middle1","tail_right1","fin_left1","fin_right1")),
            Map.entry("seaking",List.of("tail_back_left1","tail_back_right1","tail_bottom_left","tail_bottom_right")),
            Map.entry("staryu",List.of("body_limb_top","body_limb_left","body_limb_right","body_limb_bottom_left","body_limb_bottom_right")));
        for(var entry:anatomy.entrySet()) {
            String slug=entry.getKey(); var geometry=json("assets/minemon_gen1/geo/"+slug+"_cobblemon_1_8_2/"+slug+".geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            var bones=new java.util.HashSet<String>(); for(var bone:geometry.getAsJsonArray("bones")) bones.add(bone.getAsJsonObject().get("name").getAsString());
            assertTrue(slug,bones.containsAll(entry.getValue()));
        }
        assertTrue(animations("kangaskhan").has("animation.kangaskhan.ground_walk"));
        assertTrue(animations("horsea").has("animation.horsea.water_swim"));
        assertTrue(animations("seadra").has("animation.seadra.water_swim"));
        assertTrue(animations("staryu").has("animation.staryu.ground_run"));
        assertTrue(animations("tangela").has("animation.tangela.cry"));
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
