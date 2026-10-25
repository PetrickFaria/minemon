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
            for(var clip:def.getAsJsonObject("clips").entrySet()) {
                String value=clip.getValue().getAsString();
                if(value.contains(".minemon_")) {
                    String expected="animation."+slug+".minemon_"+(clip.getKey().equals("air_fly")?"air_fly":clip.getKey().equals("air_idle")?"air_idle":"");
                    assertTrue("Only the three legendary birds may use derived air clips: "+slug+"/"+clip.getKey(),
                        List.of("articuno","zapdos","moltres").contains(slug)
                            &&List.of("air_idle","air_fly").contains(clip.getKey())
                            &&value.equals(expected));
                } else assertFalse("No invented action "+slug+"/"+clip.getKey(),value.contains(".minemon_"));
            }
        }
        assertEquals("ground",definitions.get(140).getAsJsonObject().get("mode").getAsString());
        assertEquals("ground",definitions.get(141).getAsJsonObject().get("mode").getAsString());
        assertEquals("ground",definitions.get(142).getAsJsonObject().get("mode").getAsString());
        for(int i=143;i<=145;i++) assertEquals("flying",definitions.get(i).getAsJsonObject().get("mode").getAsString());
        for(int i=146;i<=148;i++) assertEquals("ground",definitions.get(i).getAsJsonObject().get("mode").getAsString());
        assertEquals("flying",definitions.get(149).getAsJsonObject().get("mode").getAsString());
        for(String slug:List.of("aerodactyl","dratini","dragonair")) assertTrue(slug,definitions.get(SPECIES.indexOf(slug)+140).getAsJsonObject().getAsJsonObject("clips").has("ground_run"));
        for(String slug:List.of("kabutops","snorlax","articuno","zapdos","moltres","dragonite","mewtwo")) assertFalse(slug,definitions.get(SPECIES.indexOf(slug)+140).getAsJsonObject().getAsJsonObject("clips").has("ground_run"));
        for(String slug:List.of("articuno","zapdos","moltres","mewtwo")) {
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
                var expectedProfileJoints=new HashSet<String>(); for(JsonElement joint:profile.getAsJsonArray(state)) expectedProfileJoints.add(joint.getAsString());
                assertTrue(slug+" "+state+" profile joints must be animated by selected clip",channelBones.containsAll(expectedProfileJoints));
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

    @Test public void zapdosUsesItsNativeAerialPoseTransitionInsteadOfGroundGait() throws Exception {
        var definitions=defs(); var zapdos=definitions.get(144).getAsJsonObject();
        assertEquals("flying",zapdos.get("mode").getAsString());
        assertEquals("animation.zapdos.air_idle",zapdos.get("idle").getAsString());
        assertEquals("animation.zapdos.air_fly",zapdos.get("walk").getAsString());
        assertFalse("Zapdos has no native ground run",zapdos.getAsJsonObject("clips").has("ground_run"));

        var profiles=json("data/minemon_gen1/animation_validation_profiles.json").getAsJsonObject("zapdos");
        var expected=List.of("head","wing_open_left","wing_open_right");
        for(String profile:List.of("idle","idle_static","pose_transition")) {
            var joints=new java.util.ArrayList<String>();
            for(JsonElement joint:profiles.getAsJsonArray(profile))joints.add(joint.getAsString());
            assertEquals(profile,expected,joints);
        }
        var clips=animations("zapdos");
        for(String bone:expected) {
            var idleTrack=clips.getAsJsonObject("animation.zapdos.air_idle").getAsJsonObject("bones").getAsJsonObject(bone).getAsJsonArray("rotation");
            var flyTrack=clips.getAsJsonObject("animation.zapdos.air_fly").getAsJsonObject("bones").getAsJsonObject(bone).getAsJsonArray("rotation");
            assertEquals("source pose vector for "+bone,3,idleTrack.size());
            assertEquals("source pose vector for "+bone,3,flyTrack.size());
            boolean changed=false;
            for(int axis=0;axis<3;axis++) if(!idleTrack.get(axis).equals(flyTrack.get(axis))) changed=true;
            assertTrue("native aerial transition changes "+bone,idleTrack.size()==3&&changed);
        }
    }

    @Test public void legendaryBirdsUseNativeAerialPoseTransitionsInsteadOfGroundGaits() throws Exception {
        var definitions=defs();
        var profiles=json("data/minemon_gen1/animation_validation_profiles.json").getAsJsonObject();
        var expected=Map.of(
            "articuno",List.of("head","wing_open_left","wing_open_right"),
            "zapdos",List.of("head","wing_open_left","wing_open_right"),
            "moltres",List.of("head","wing_open_left","wing_open_right")
        );
        for(var entry:expected.entrySet()) {
            String slug=entry.getKey();
            var definition=definitions.get(SPECIES.indexOf(slug)+140).getAsJsonObject();
            assertEquals(slug,"flying",definition.get("mode").getAsString());
            assertEquals(slug+" native idle", "animation."+slug+".air_idle",definition.get("idle").getAsString());
            assertEquals(slug+" native flight", "animation."+slug+".air_fly",definition.get("walk").getAsString());
            assertFalse(slug+" has no native ground run",definition.getAsJsonObject("clips").has("ground_run"));

            var speciesProfiles=profiles.getAsJsonObject(slug);
            var transition=new java.util.ArrayList<String>();
            for(JsonElement joint:speciesProfiles.getAsJsonArray("pose_transition"))transition.add(joint.getAsString());
            assertEquals(slug+" uses source aerial joints",entry.getValue(),transition);
            var movementProfile=new HashSet<String>();
            for(JsonElement joint:speciesProfiles.getAsJsonArray("walk"))movementProfile.add(joint.getAsString());
            assertTrue(slug+" movement profile samples every aerial transition joint",movementProfile.containsAll(transition));
            assertEquals(slug+" stable idle profile",speciesProfiles.getAsJsonArray("idle").toString(),speciesProfiles.getAsJsonArray("idle_static").toString());

            var clips=animations(slug);
            for(String bone:entry.getValue()) {
                var idleBone=clips.getAsJsonObject("animation."+slug+".air_idle").getAsJsonObject("bones").getAsJsonObject(bone);
                var flightBone=clips.getAsJsonObject("animation."+slug+".air_fly").getAsJsonObject("bones").getAsJsonObject(bone);
                assertTrue(slug+" "+bone+" idle rotation is a native vector",idleBone.get("rotation").isJsonArray());
                assertTrue(slug+" "+bone+" flight rotation is a native vector",flightBone.get("rotation").isJsonArray());
                var idleVector=idleBone.getAsJsonArray("rotation");var flightVector=flightBone.getAsJsonArray("rotation");
                assertEquals(slug+" "+bone+" idle rotation",3,idleVector.size());
                assertEquals(slug+" "+bone+" flight rotation",3,flightVector.size());
                boolean changed=false;
                for(int axis=0;axis<3;axis++)if(!idleVector.get(axis).equals(flightVector.get(axis)))changed=true;
                assertTrue(slug+" native aerial poses rotate "+bone,changed);
            }
        }
    }

    @Test public void mewtwoUsesItsNativeFlyingPosesAndSourceTransition() throws Exception {
        var definition=defs().get(149).getAsJsonObject();
        assertEquals("flying",definition.get("mode").getAsString());
        assertEquals("animation.mewtwo.air_idle",definition.get("idle").getAsString());
        assertEquals("animation.mewtwo.air_fly",definition.get("walk").getAsString());
        assertFalse("Mewtwo has no native ground run",definition.getAsJsonObject("clips").has("ground_run"));

        var profiles=json("data/minemon_gen1/animation_validation_profiles.json").getAsJsonObject().getAsJsonObject("mewtwo");
        assertEquals("Mewtwo visible idle pose is static",profiles.getAsJsonArray("idle").toString(),profiles.getAsJsonArray("idle_static").toString());
        var transition=List.of("arm_left","arm_right","tail","tail9");
        var actual=new java.util.ArrayList<String>();
        for(JsonElement joint:profiles.getAsJsonArray("pose_transition"))actual.add(joint.getAsString());
        assertEquals("Mewtwo uses real source flight joints",transition,actual);

        var clips=animations("mewtwo");
        for(String bone:transition) {
            var idle=clips.getAsJsonObject("animation.mewtwo.air_idle").getAsJsonObject("bones").getAsJsonArray("rotation");
            var fly=clips.getAsJsonObject("animation.mewtwo.air_fly").getAsJsonObject("bones").getAsJsonArray("rotation");
            assertEquals(bone+" idle rotation",3,idle.size()); assertEquals(bone+" flight rotation",3,fly.size());
            boolean changed=false;
            for(int axis=0;axis<3;axis++)if(Math.abs(idle.get(axis).getAsDouble()-fly.get(axis).getAsDouble())>0.05)changed=true;
            assertTrue("native Mewtwo pose changes "+bone,changed);
        }
        var poser=definition.getAsJsonObject("source_procedural_pose");
        assertEquals(List.of("q.pitch_tilt('body')"),stringArray(poser.getAsJsonArray("pitch_tilt")));
        assertEquals(List.of("q.look('head', 1, 1, 20, -90, 60, -37)"),stringArray(poser.getAsJsonArray("look")));
    }

    private static List<String> stringArray(JsonArray array) {
        var result=new java.util.ArrayList<String>(); for(JsonElement value:array)result.add(value.getAsString()); return result;
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
