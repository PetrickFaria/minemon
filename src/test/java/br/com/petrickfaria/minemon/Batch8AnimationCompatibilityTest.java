package br.com.petrickfaria.minemon;

import com.google.gson.*;
import org.junit.Test;
import software.bernie.geckolib3.core.molang.MolangParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class Batch8AnimationCompatibilityTest {
    private static final List<String> SPECIES=List.of(
        "victreebel","tentacool","tentacruel","geodude","graveler",
        "golem","ponyta","rapidash","slowpoke","slowbro");
    private static final Path RESOURCES=Path.of(System.getProperty("minemon.testResources"));

    private static JsonObject json(String path) throws Exception {
        try(var reader=Files.newBufferedReader(RESOURCES.resolve(path))) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static JsonObject animations(String slug) throws Exception {
        return json("assets/minemon_gen1/animations/"+slug+"_cobblemon_1_8_2/"+slug+".animation.json").getAsJsonObject("animations");
    }

    private static void checkExpressions(JsonElement value,MolangParser parser) throws Exception {
        if(value.isJsonArray()) {
            for(var item:value.getAsJsonArray())checkExpressions(item,parser);
        } else if(value.isJsonObject()) {
            for(var item:value.getAsJsonObject().entrySet())checkExpressions(item.getValue(),parser);
        } else if(value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString()) {
            String text=value.getAsString();
            assertFalse("Bedrock query alias was not adapted: "+text,text.matches("(?s).*\\bq\\.[A-Za-z_].*"));
            var expression=parser.parseExpression(text);
            for(int i=0;i<=20;i++) {
                parser.setValue("query.anim_time",i/20.0);
                assertTrue(text,Double.isFinite(expression.get()));
            }
        }
    }

    private static boolean changesWithTime(JsonElement value,MolangParser parser) throws Exception {
        boolean changed=false;
        if(value.isJsonArray()) {
            for(var item:value.getAsJsonArray())changed|=changesWithTime(item,parser);
        } else if(value.isJsonObject()) {
            for(var item:value.getAsJsonObject().entrySet())changed|=changesWithTime(item.getValue(),parser);
        } else if(value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString()) {
            var expression=parser.parseExpression(value.getAsString());
            double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
            for(int i=0;i<=160;i++) {
                parser.setValue("query.anim_time",i/40.0);
                double sample=expression.get();assertTrue(Double.isFinite(sample));
                min=Math.min(min,sample);max=Math.max(max,sample);
            }
            changed=max-min>0.000001;
        }
        return changed;
    }

    @Test public void sourceAnimationsParseAndLoadInGeckoLib3() throws Exception {
        for(String slug:SPECIES) {
            var clips=animations(slug);
            assertFalse("No imported clips for "+slug,clips.entrySet().isEmpty());
            assertTrue("Native idle missing for "+slug,clips.has("animation."+slug+".ground_idle"));
            for(var entry:clips.entrySet()) {
                var clip=entry.getValue().getAsJsonObject();
                if(clip.has("animation_length")&&!clip.get("animation_length").isJsonNull())
                    assertTrue(entry.getKey(),clip.get("animation_length").getAsDouble()>0);
                else assertTrue("Missing duration is only valid for source looping clips: "+entry.getKey(),clip.has("loop")&&clip.get("loop").getAsBoolean());
                if(clip.has("bones"))checkExpressions(clip.get("bones"),new MolangParser());
                var loaded=software.bernie.geckolib3.util.json.JsonAnimationUtils.deserializeJsonToAnimation(
                    new java.util.AbstractMap.SimpleImmutableEntry<String,JsonElement>(entry.getKey(),clip),new MolangParser());
                assertTrue(entry.getKey(),Double.isFinite(loaded.animationLength));
            }
        }
    }

    @Test public void nativeIdleProfilesRunsAndAquaticClipsAreAccuratelyDeclared() throws Exception {
        var defs=JsonParser.parseReader(Files.newBufferedReader(RESOURCES.resolve("data/minemon_gen1/runtime_species.json"))).getAsJsonArray();
        var profiles=json("data/minemon_gen1/animation_validation_profiles.json");
        Set<String> nativeRuns=Set.of("geodude","graveler","golem","rapidash");
        for(int number=71;number<=80;number++) {
            var def=defs.get(number-1).getAsJsonObject();String slug=def.get("slug").getAsString();
            assertEquals(SPECIES.get(number-71),slug);
            for(String kind:List.of("model","texture","animation"))
                assertTrue(def.get(kind).getAsString(),def.get(kind).getAsString().contains(slug+"_cobblemon_1_8_2"));
            assertTrue(profiles.has(slug));
            assertFalse("Native animated idle cannot be marked static: "+slug,
                def.has("source_static_idle")&&def.get("source_static_idle").getAsBoolean());
            var idleBones=animations(slug).getAsJsonObject(def.get("idle").getAsString()).getAsJsonObject("bones");
            boolean moved=false;
            for(var joint:profiles.getAsJsonObject(slug).getAsJsonArray("idle")) {
                var channels=idleBones.getAsJsonObject(joint.getAsString());
                if(channels!=null)moved|=changesWithTime(channels,new MolangParser());
            }
            assertTrue("Idle profile must measure native motion for "+slug,moved);
            assertEquals("Only native source ground_run clips are selected for "+slug,nativeRuns.contains(slug),
                def.getAsJsonObject("clips").has("ground_run"));
        }
        for(String slug:List.of("tentacool","tentacruel","slowpoke","slowbro"))
            assertTrue("Native aquatic animation was dropped for "+slug,animations(slug).has("animation."+slug+".water_swim"));
        var golemRunBones=animations("golem").getAsJsonObject("animation.golem.ground_run").getAsJsonObject("bones");
        var golemRunProfile=new java.util.ArrayList<String>();
        for(var joint:profiles.getAsJsonObject("golem").getAsJsonArray("run"))golemRunProfile.add(joint.getAsString());
        assertEquals("Measure Golem's actual animated torso instead of its rigid arms",List.of("body","body_roll"),golemRunProfile);
        assertTrue("Golem's native run body must change over time",
            changesWithTime(golemRunBones.getAsJsonObject("body"),new MolangParser()));
        assertTrue("Golem's native run must retain its source body-roll keyframes",golemRunBones.has("body_roll"));
        var gravelerRunBones=animations("graveler").getAsJsonObject("animation.graveler.ground_run").getAsJsonObject("bones");
        var gravelerRunProfile=new java.util.ArrayList<String>();
        for(var joint:profiles.getAsJsonObject("graveler").getAsJsonArray("run"))gravelerRunProfile.add(joint.getAsString());
        assertEquals("Measure Graveler's animated torso instead of rigid source limbs",List.of("body","body_roll"),gravelerRunProfile);
        assertTrue("Graveler's native run body must change over time",
            changesWithTime(gravelerRunBones.getAsJsonObject("body"),new MolangParser()));
        assertTrue("Graveler's native run must retain its source body-roll keyframes",gravelerRunBones.has("body_roll"));
        var ponyta=defs.get(76).getAsJsonObject().getAsJsonObject("clips");
        assertTrue("Retain Ponyta's unreferenced source clip without claiming it as a selected ground run",ponyta.has("ground_runold"));
        assertFalse(ponyta.has("ground_run"));
    }

    @Test public void sourceAnatomyAndGeodudeLookRigArePreserved() throws Exception {
        var checks=java.util.Map.of(
            "tentacool",List.of("tentacle_left_3","tentacle_right_3"),
            "tentacruel",List.of("tentacle_left_3","tentacle_right_3"),
            "ponyta",List.of("fire_mane","fire_mane_head","tail"),
            "rapidash",List.of("fire_mane_head_flame","tail"),
            "slowpoke",List.of("tail","tail4"),
            "slowbro",List.of("shell","shell_jaw","tail"));
        for(var entry:checks.entrySet()) {
            var geo=json("assets/minemon_gen1/geo/"+entry.getKey()+"_cobblemon_1_8_2/"+entry.getKey()+".geo.json")
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            var names=new java.util.HashSet<String>();
            for(var bone:geo.getAsJsonArray("bones"))names.add(bone.getAsJsonObject().get("name").getAsString());
            assertTrue(entry.getKey(),names.containsAll(entry.getValue()));
        }
        var all=defs();
        for(String slug:List.of("geodude","golem","slowpoke","slowbro")) {
            var definition=all.get(index(slug)).getAsJsonObject();
            var pose=definition.getAsJsonObject("source_procedural_pose");
            assertTrue("Retain source poser look declaration for "+slug,pose.getAsJsonArray("look").size()>0);
            assertEquals("head",pose.getAsJsonObject("adapted_geo_bones").get("head").getAsString());
            assertTrue(definition.getAsJsonObject("source_molang_adaptations").has("q."));
        }
    }

    @Test public void geodudeGaitProfileUsesNativeAnimatedBodyAndTorso() throws Exception {
        var profile=json("data/minemon_gen1/animation_validation_profiles.json").getAsJsonObject()
            .getAsJsonObject("geodude");
        assertEquals("Idle remains measured on the natively moving arms",List.of("arm_left","arm_right"),
            strings(profile.getAsJsonArray("idle")));
        for(String state:List.of("walk","run"))
            assertEquals("Geodude "+state+" must measure joints animated by its source clip",
                List.of("body","torso"),strings(profile.getAsJsonArray(state)));

        var clips=animations("geodude");
        for(String gait:List.of("ground_walk","ground_run")) {
            var bones=clips.getAsJsonObject("animation.geodude."+gait).getAsJsonObject("bones");
            var bodyPosition=bones.getAsJsonObject("body").getAsJsonObject("position");
            assertTrue("Geodude "+gait+" body position must retain native keyframes",bodyPosition.size()>1);
            assertTrue("Geodude "+gait+" torso channel must vary with animation time",
                changesWithTime(bones.getAsJsonObject("torso"),new MolangParser()));
        }
    }

    private static List<String> strings(JsonArray values) {
        var result=new java.util.ArrayList<String>();
        for(JsonElement value:values)result.add(value.getAsString());
        return result;
    }

    private static int index(String slug) { return SPECIES.indexOf(slug)+70; }

    private static JsonArray defs() throws Exception {
        try(var reader=Files.newBufferedReader(RESOURCES.resolve("data/minemon_gen1/runtime_species.json"))) {
            return JsonParser.parseReader(reader).getAsJsonArray();
        }
    }
}
