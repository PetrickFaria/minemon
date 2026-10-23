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

public class Batch15AnimationCompatibilityTest {
    private static final List<String> SPECIES=List.of("kabutops","aerodactyl","snorlax","articuno","zapdos","moltres","dratini","dragonair","dragonite","mewtwo");
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

    @Test public void exactBatchFifteenAndNativeLocomotionAreDeclared() throws Exception {
        var definitions=defs(); var profiles=json("data/minemon_gen1/animation_validation_profiles.json");
        for(int number=141;number<=150;number++) {
            var def=definitions.get(number-1).getAsJsonObject(); String slug=def.get("slug").getAsString();
            assertEquals("National Pokédex #"+number,SPECIES.get(number-141),slug);
            assertEquals("1.8.2",def.get("source_cobblemon_version").getAsString());
            assertTrue("Source-backed model variant "+slug,def.get("source_model_variant").getAsString().startsWith(slug));
            for(String key:List.of("model","texture","animation")) assertTrue(slug+"/"+key,def.get(key).getAsString().contains(slug+"_cobblemon_1_8_2/"));
            assertTrue("Missing articulation profile "+slug,profiles.has(slug));
            for(var clip:def.getAsJsonObject("clips").entrySet()) assertFalse("No invented action "+slug+"/"+clip.getKey(),clip.getValue().getAsString().contains(".minemon_"));
        }
        assertEquals("ground",definitions.get(140).getAsJsonObject().get("mode").getAsString());
        assertEquals("ground",definitions.get(141).getAsJsonObject().get("mode").getAsString());
        assertEquals("ground",definitions.get(142).getAsJsonObject().get("mode").getAsString());
        for(int i=143;i<=145;i++) assertEquals("flying",definitions.get(i).getAsJsonObject().get("mode").getAsString());
        for(int i=146;i<=149;i++) assertEquals("ground",definitions.get(i).getAsJsonObject().get("mode").getAsString());
        for(String slug:List.of("aerodactyl","dratini","dragonair")) assertTrue(slug,definitions.get(SPECIES.indexOf(slug)+140).getAsJsonObject().getAsJsonObject("clips").has("ground_run"));
        for(String slug:List.of("kabutops","snorlax","articuno","zapdos","moltres","dragonite","mewtwo")) assertFalse(slug,definitions.get(SPECIES.indexOf(slug)+140).getAsJsonObject().getAsJsonObject("clips").has("ground_run"));
        for(String slug:List.of("articuno","zapdos","moltres")) {
            var clips=definitions.get(SPECIES.indexOf(slug)+140).getAsJsonObject().getAsJsonObject("clips");
            assertTrue(slug+" native flight idle",clips.has("air_idle")); assertTrue(slug+" native flight",clips.has("air_fly"));
        }
    }

    @Test public void selectedNativeClipsParseAndLoadInGeckoLib31() throws Exception {
        var definitions=defs();
        for(int number=141;number<=150;number++) {
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

    @Test public void sourceAnatomyAndSelectedArticulationRemainInTheRigs() throws Exception {
        Map<String,List<String>> anatomy=Map.ofEntries(
            Map.entry("kabutops",List.of("left_arm","right_arm","left_claw","right_claw","left_upper_leg","right_upper_leg","tail","fin_upper_left","fin_upper_right")),
            Map.entry("aerodactyl",List.of("wing_ground_left","wing_ground_right","wing_air_left","wing_air_right","tail_1","tail_tip","left_upper_leg","right_upper_leg")),
            Map.entry("snorlax",List.of("belly","belly2","arm_left","arm_right","leg_left","leg_right")),
            Map.entry("articuno",List.of("wing_open_left","wing_open_left3","wing_open_right","wing_open_right3","tail18","tail_base")),
            Map.entry("zapdos",List.of("wing_open_left","wing_open_left_anti","wing_open_right","wing_open_right_anti","tail_top")),
            Map.entry("moltres",List.of("wing_open_left","wing_open_right","head_fire2","head_fire3","tail_fire_puff2","tail_fire_puff5")),
            Map.entry("dratini",List.of("segment1","segment4","segment7","segment8","head")),
            Map.entry("dragonair",List.of("segment1","segment6","segment11","gem_tail1","gem_tail2","head_rotation")),
            Map.entry("dragonite",List.of("arm_left","arm_right","leg_left","leg_right","wing_left","wing_right","tail5")),
            Map.entry("mewtwo",List.of("arm_left","arm_right","leg_left","leg_right","tail","tail9","head")));
        var definitions=defs(); var profiles=json("data/minemon_gen1/animation_validation_profiles.json");
        for(var entry:anatomy.entrySet()) {
            String slug=entry.getKey(); var def=definitions.get(SPECIES.indexOf(slug)+140).getAsJsonObject();
            var geometry=json("assets/minemon_gen1/geo/"+slug+"_cobblemon_1_8_2/"+slug+".geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            var bones=new HashSet<String>(); for(var bone:geometry.getAsJsonArray("bones")) bones.add(bone.getAsJsonObject().get("name").getAsString());
            assertTrue(slug,bones.containsAll(entry.getValue()));
            var animation=animations(slug); var profile=profiles.getAsJsonObject(slug);
            for(String state:List.of("idle","walk")) {
                String clip=def.get(state).getAsString(); assertTrue(slug+"/"+clip,animation.has(clip));
                var channelBones=new HashSet<String>(); for(var bone:animation.getAsJsonObject(clip).getAsJsonObject("bones").keySet()) channelBones.add(bone);
                assertTrue(slug+" "+state+" profile joints must be animated by selected clip",channelBones.containsAll(profile.getAsJsonArray(state).asList().stream().map(JsonElement::getAsString).toList()));
            }
            for(var bone:geometry.getAsJsonArray("bones")) {
                var b=bone.getAsJsonObject(); if(b.has("parent")) assertTrue(slug+" parent "+b.get("name"),bones.contains(b.get("parent").getAsString()));
                if(b.has("pivot")) for(var value:b.getAsJsonArray("pivot")) assertTrue(slug+" finite pivot",Double.isFinite(value.getAsDouble()));
            }
        }
        var moltres=animations("moltres");
        for(String clip:List.of("animation.moltres.air_idle","animation.moltres.air_fly"))
            for(String bone:List.of("head_fire2","head_fire3","tail_fire_puff2")) assertTrue(clip+" preserves fire element "+bone,moltres.getAsJsonObject(clip).getAsJsonObject("bones").has(bone));
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
