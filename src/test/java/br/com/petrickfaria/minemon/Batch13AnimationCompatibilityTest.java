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

public class Batch13AnimationCompatibilityTest {
    private static final List<String> SPECIES=List.of("starmie","mrmime","scyther","jynx","electabuzz","magmar","pinsir","tauros","magikarp","gyarados");
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

    @Test public void exactBatchThirteenSpeciesAndOnlyNativeRunsAreDeclared() throws Exception {
        var definitions=defs(); var profiles=json("data/minemon_gen1/animation_validation_profiles.json");
        for(int number=121;number<=130;number++) {
            var def=definitions.get(number-1).getAsJsonObject(); String slug=def.get("slug").getAsString();
            assertEquals("National Pokédex #"+number,SPECIES.get(number-121),slug);
            assertEquals("1.8.2",def.get("source_cobblemon_version").getAsString());
            List<String> variants=slug.equals("mrmime")?List.of("mr_mime.geo.json"):List.of(slug+".geo.json",slug+"_male.geo.json",slug+"_female.geo.json");
            assertTrue("Unexpected Kanto source model for "+slug,variants.contains(def.get("source_model_variant").getAsString()));
            for(String key:List.of("model","texture","animation")) assertTrue(slug+"/"+key,def.get(key).getAsString().contains(slug+"_cobblemon_1_8_2/"));
            assertTrue("Missing articulation profile "+slug,profiles.has(slug));
            for(var clip:def.getAsJsonObject("clips").entrySet()) assertFalse("No generated movement/action placeholder "+slug+"/"+clip.getKey(),clip.getValue().getAsString().contains(".minemon_"));
        }
        for(String slug:List.of("starmie","mrmime","tauros")) assertTrue("Native run missing: "+slug,defs().get(SPECIES.indexOf(slug)+120).getAsJsonObject().getAsJsonObject("clips").has("ground_run"));
        for(String slug:List.of("scyther","jynx","electabuzz","magmar","pinsir","magikarp","gyarados")) assertFalse("Do not synthesize run for "+slug,defs().get(SPECIES.indexOf(slug)+120).getAsJsonObject().getAsJsonObject("clips").has("ground_run"));
        for(String slug:List.of("scyther","jynx","magmar")) {
            var d=definitions.get(SPECIES.indexOf(slug)+120).getAsJsonObject();
            assertEquals("idle",d.get("movement_fallback").getAsString());
            assertEquals(d.get("idle").getAsString(),d.get("walk").getAsString());
        }
        for(String slug:List.of("jynx","magmar")) assertTrue("Source idle should be classified from its static keyframes: "+slug,definitions.get(SPECIES.indexOf(slug)+120).getAsJsonObject().get("source_static_idle").getAsBoolean());
        for(String slug:List.of("starmie","magikarp","gyarados")) {
            var clips=definitions.get(SPECIES.indexOf(slug)+120).getAsJsonObject().getAsJsonObject("clips");
            assertTrue("Keep native aquatic idle for "+slug,clips.has("water_idle"));
            assertTrue("Keep native aquatic swim for "+slug,clips.has("water_swim"));
        }
    }

    @Test public void selectedNativeClipsParseAndLoadInGeckoLib31() throws Exception {
        var definitions=defs();
        for(int number=121;number<=130;number++) {
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

    @Test public void characteristicAnatomyAndNativeAquaticClipsAreRetained() throws Exception {
        Map<String,List<String>> anatomy=Map.ofEntries(
            Map.entry("starmie",List.of("body_limb_top","body_limb_left","body_limb_right","body_limb_bottom_left","body_limb_bottom_right")),
            Map.entry("mrmime",List.of("arm_left","arm_right","wrist_left","wrist_right","foot_left","foot_right")),
            Map.entry("scyther",List.of("left_wing","right_wing","left_upper_wing","right_upper_wing","left_scythe","right_scythe")),
            Map.entry("jynx",List.of("hair_front_left","hair_front_right","dress_front","hand_left","hand_right")),
            Map.entry("electabuzz",List.of("arm_fur_left","arm_fur_right","leg_left","leg_right","tail")),
            Map.entry("magmar",List.of("elbow_frill_left","elbow_frill_right","tail","tail2","tail3","tail4")),
            Map.entry("pinsir",List.of("pincer_left","pincer_left2","pincer_right","pincer_right2")),
            Map.entry("tauros",List.of("tail_left","tail_middle","tail_right","leg_front_left","leg_back_right")),
            Map.entry("magikarp",List.of("fin_left","fin_right","tail","tail2","tail3")),
            Map.entry("gyarados",List.of("neck1","neck5","segment","segment9","tail1","tail2","tail3")));
        for(var entry:anatomy.entrySet()) {
            String slug=entry.getKey(); var geometry=json("assets/minemon_gen1/geo/"+slug+"_cobblemon_1_8_2/"+slug+".geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            var bones=new HashSet<String>(); for(var bone:geometry.getAsJsonArray("bones")) bones.add(bone.getAsJsonObject().get("name").getAsString());
            assertTrue(slug,bones.containsAll(entry.getValue()));
        }
        assertTrue(animations("scyther").has("animation.scyther.air_idle"));
        assertTrue(animations("tauros").has("animation.tauros.ground_run"));
        for(String slug:List.of("starmie","magikarp","gyarados")) {
            assertTrue(animations(slug).has("animation."+slug+".water_idle"));
            assertTrue(animations(slug).has("animation."+slug+".water_swim"));
        }
        assertTrue("Use Kanto Mr. Mime rig",json("assets/minemon_gen1/geo/mrmime_cobblemon_1_8_2/mrmime.geo.json").getAsJsonArray("minecraft:geometry").size()>0);
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
