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

public class Batch10AnimationCompatibilityTest {
    private static final List<String> SPECIES=List.of(
        "cloyster","gastly","haunter","gengar","onix","drowzee","hypno","krabby","kingler","voltorb");
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

    private static Set<String> strings(JsonArray values) {
        var result=new java.util.LinkedHashSet<String>();
        for(var value:values)result.add(value.getAsString());
        return result;
    }

    @Test public void exactBatchTenSpeciesAndNativeRunAvailabilityAreDeclared() throws Exception {
        var defs=jsonArray("data/minemon_gen1/runtime_species.json");
        var profiles=json("data/minemon_gen1/animation_validation_profiles.json");
        Set<String> nativeRuns=Set.of("drowzee","hypno");
        for(int number=91;number<=100;number++) {
            var def=defs.get(number-1).getAsJsonObject();
            String slug=def.get("slug").getAsString();
            assertEquals(number+" species order",SPECIES.get(number-91),slug);
            assertEquals("1.8.2",def.get("source_cobblemon_version").getAsString());
            assertTrue(def.get("model").getAsString(),def.get("model").getAsString().contains(slug+"_cobblemon_1_8_2/"));
            assertTrue(def.get("texture").getAsString(),def.get("texture").getAsString().contains(slug+"_cobblemon_1_8_2/"));
            assertTrue(def.get("animation").getAsString(),def.get("animation").getAsString().contains(slug+"_cobblemon_1_8_2/"));
            assertEquals("Only source-native ground_run clips may be selected for "+slug,nativeRuns.contains(slug),
                def.getAsJsonObject("clips").has("ground_run"));
            assertTrue("Missing source-grounded joint profile for "+slug,profiles.has(slug));
        }
        var haunter=defs.get(92).getAsJsonObject();
        assertEquals("flying",haunter.get("mode").getAsString());
        assertEquals("idle",haunter.get("movement_fallback").getAsString());
        assertTrue("Haunter source idle is explicitly static",haunter.get("source_static_idle").getAsBoolean());
        var gengar=defs.get(93).getAsJsonObject();
        assertEquals("animation.gengar.air_idle",gengar.get("idle").getAsString());
        assertEquals("animation.gengar.air_fly",gengar.get("walk").getAsString());
    }

    @Test public void activeIdleWalkAndNativeRunClipsParseAndLoadInGeckoLib31() throws Exception {
        var defs=jsonArray("data/minemon_gen1/runtime_species.json");
        for(int number=91;number<=100;number++) {
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
                var parser=new MolangParser();parser.setValue("query.anim_time",0.0);
                checkExpressions(clip.get("bones"),parser,slug+"/"+name);
                var loaded=software.bernie.geckolib3.util.json.JsonAnimationUtils.deserializeJsonToAnimation(
                    new java.util.AbstractMap.SimpleImmutableEntry<String,JsonElement>(name,clip),parser);
                assertTrue(slug+"/"+name,Double.isFinite(loaded.animationLength));
            }
        }
    }

    @Test public void characteristicAnatomyAndSpecialClipsRemainPresent() throws Exception {
        Map<String,List<String>> anatomy=Map.ofEntries(
            Map.entry("cloyster",List.of("shell_left","shell_right","inner_shell_left","inner_shell_right","inner_spike")),
            Map.entry("gastly",List.of("body","tongue")),
            Map.entry("haunter",List.of("hand_left1","hand_right1","hand_left2","hand_right2","tail1","tail2","tail3")),
            Map.entry("gengar",List.of("arm_left","arm_right","leg_left","leg_right")),
            Map.entry("onix",java.util.stream.IntStream.rangeClosed(1,14).mapToObj(i->"segment"+i).toList()),
            Map.entry("drowzee",List.of("arm_left","arm_right","leg_left","leg_right")),
            Map.entry("hypno",List.of("pendulum","string_1","string_2","string_3","weight")),
            Map.entry("krabby",List.of("claw_left","claw_right","leg_left_front","leg_right_front")),
            Map.entry("kingler",List.of("claw_left","claw_right","leg_left_front","leg_right_front")),
            Map.entry("voltorb",List.of("body")));
        for(var entry:anatomy.entrySet()) {
            String slug=entry.getKey();
            var geometry=json("assets/minemon_gen1/geo/"+slug+"_cobblemon_1_8_2/"+slug+".geo.json")
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            var bones=new java.util.HashSet<String>();
            for(var bone:geometry.getAsJsonArray("bones"))bones.add(bone.getAsJsonObject().get("name").getAsString());
            assertTrue(slug,bones.containsAll(entry.getValue()));
        }
        assertTrue(animations("cloyster").has("animation.cloyster.closed"));
        assertTrue(animations("cloyster").has("animation.cloyster.water_swim"));
        assertTrue(animations("onix").has("animation.onix.boulder_rotation"));
        assertTrue(animations("krabby").has("animation.krabby.snip_left"));
        assertTrue(animations("krabby").has("animation.krabby.snip_right"));
    }

    @Test public void gengarNativeFlightClipsAreStableDistinctPoses() throws Exception {
        var profiles=json("data/minemon_gen1/animation_validation_profiles.json").getAsJsonObject("gengar");
        var joints=strings(profiles.getAsJsonArray("pose_transition"));
        assertEquals(Set.of("arm_left","arm_right","body","leg_left","leg_right"),joints);
        var clips=animations("gengar");
        var idle=clips.getAsJsonObject("animation.gengar.air_idle");
        var flight=clips.getAsJsonObject("animation.gengar.air_fly");
        for(String joint:joints) {
            var idleRotation=idle.getAsJsonObject("bones").getAsJsonObject(joint).getAsJsonArray("rotation");
            var flightRotation=flight.getAsJsonObject("bones").getAsJsonObject(joint).getAsJsonArray("rotation");
            assertEquals("Native Gengar idle rotation must remain a fixed pose",3,idleRotation.size());
            assertEquals("Native Gengar flight rotation must remain a fixed pose",3,flightRotation.size());
            assertNotEquals("Gengar's native movement pose must articulate "+joint,
                idle.getAsJsonObject("bones").getAsJsonObject(joint).get("rotation"),
                flight.getAsJsonObject("bones").getAsJsonObject(joint).get("rotation"));
        }
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
