package br.com.petrickfaria.minemon;

import br.com.petrickfaria.minemon.entity.*;
import com.google.gson.*;
import java.nio.file.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RequestedBehaviorAssetsTest {
    @Test public void fixedHoverSpeciesHaveIndependentRequestedHeights() {
        for(String slug:java.util.List.of("abra","porygon","kakuna","metapod")) {
            var cycle=new BehaviorCycle(SpeciesBehaviorProfiles.profile(slug),slug);
            double expected=1.0/3.0;
            assertEquals(slug,expected,cycle.minimumAltitude(),1e-9);
            assertEquals(slug,expected,cycle.maximumAltitude(),1e-9);
        }
        assertEquals(1.0,new BehaviorCycle(SpeciesBehaviorProfiles.profile("geodude"),"geodude").minimumAltitude(),1e-9);
    }
    @Test public void everyAwakeRestPreservesSleepBodyAndRemovesSleepEvents() throws Exception {
        Path root=Path.of(System.getProperty("minemon.testResources"));
        for(var d:SpeciesDefinitions.ALL.values()) {
            var a=JsonParser.parseString(Files.readString(root.resolve(d.animation().replace("assets/minemon/","assets/minemon_gen1/"))))
                    .getAsJsonObject().getAsJsonObject("animations");
            var sleep=a.getAsJsonObject(d.clips().get("night_sleep"));
            var awake=a.getAsJsonObject(d.clips().get("awake_rest"));
            assertNotNull(d.slug(),awake);
            for(String event:java.util.List.of("sound_effects","particle_effects","timeline"))assertFalse(d.slug(),awake.has(event));
            var blink=a.getAsJsonObject(d.clips().getOrDefault("blink","missing"));
            var eyeTracks=blink==null?null:blink.getAsJsonObject("bones");
            for(var entry:sleep.getAsJsonObject("bones").entrySet()) {
                String name=entry.getKey();
                if(name.matches("(?i).*(eye|lid|pupil).*") || eyeTracks!=null && eyeTracks.has(name))continue;
                assertEquals(d.slug()+" "+name,entry.getValue(),awake.getAsJsonObject("bones").get(name));
            }
        }
    }
    @Test public void alakazamHoverCombinesSleepLegsWithGroundIdleUpperBody() throws Exception {
        var definition=SpeciesDefinitions.get("alakazam");
        var root=Path.of(System.getProperty("minemon.testResources"));
        var animations=JsonParser.parseString(Files.readString(root.resolve(definition.animation().replace("assets/minemon/","assets/minemon_gen1/"))))
                .getAsJsonObject().getAsJsonObject("animations");
        var meditation=animations.getAsJsonObject("animation.alakazam.meditate").getAsJsonObject("bones");
        var sleep=animations.getAsJsonObject("animation.alakazam.sleep").getAsJsonObject("bones");
        var idle=animations.getAsJsonObject("animation.alakazam.ground_idle").getAsJsonObject("bones");
        assertNotNull(meditation);
        var sleepLegBones=java.util.List.of("left_upper_leg","left_lower_leg","foot_left","toe_back_left","toe_inner_left","toe_outer_left",
                "right_upper_leg","right_lower_leg","foot_right","toe_back_right","toe_inner_right","toe_outer_right");
        for(String bone:sleepLegBones) {
            if(bone.equals("left_upper_leg") || bone.equals("right_upper_leg")) {
                JsonObject expected=sleep.getAsJsonObject(bone).deepCopy();
                JsonArray expectedPosition=expected.getAsJsonArray("position");
                expectedPosition.set(1,new JsonPrimitive(expectedPosition.get(1).getAsDouble()+12.5));
                assertEquals("preserve sleep-leg pose with only vertical hip-fit offset "+bone,expected,meditation.get(bone));
                assertEquals("sleep-leg rotation remains unchanged "+bone,sleep.getAsJsonObject(bone).get("rotation"),meditation.getAsJsonObject(bone).get("rotation"));
            } else {
                assertEquals("reuse exact sleep-reference leg/foot pose "+bone,sleep.get(bone),meditation.get(bone));
            }
        }
        var idleUpperBodyBones=java.util.List.of("hips","lower_torso","upper_torso","head","jaw",
                "left_mustache","left_mustache2","left_mustache3","left_mustache4",
                "right_mustache","right_mustache2","right_mustache3","right_mustache4",
                "left_shoulder","left_upper_arm","left_lower_arm","hand_left",
                "finger_index_left","finger_index_left2","finger_index_left3","finger_pinky_left","finger_pinky_left2","finger_pinky_left3","thumb_left","thumb_left2","spoon_left","spoon_left2",
                "right_shoulder","right_upper_arm","right_lower_arm","hand_right",
                "finger_index_right","finger_index_right2","finger_index_right3","finger_pinky_right","finger_pinky_right2","finger_pinky_right3","thumb_right","thumb_right2","spoon_right","spoon_right2");
        for(String bone:idleUpperBodyBones) assertEquals("reuse exact ground-idle upper-body pose "+bone,idle.get(bone),meditation.get(bone));
        assertEquals("awake ground-idle head pose",idle.get("head"),meditation.get("head"));
        assertTrue("sleep pose must not be copied onto the head",!sleep.get("head").equals(meditation.get("head")));
        assertFalse("hover bob remains independent from the pose clip",meditation.has("body"));
        assertTrue(SpeciesDefinitions.get("alakazam").clips().containsKey("meditate"));
        assertFalse(SpeciesDefinitions.get("kadabra").clips().containsKey("meditate"));
    }

    @Test public void previewBoundsCoverAllSpeciesAndTreatsHaveEighteenTypes() throws Exception {
        var root=Path.of(System.getProperty("minemon.testResources"));
        var bounds=JsonParser.parseString(Files.readString(root.resolve("data/minemon_gen1/preview_bounds.json"))).getAsJsonObject();
        assertEquals(151,bounds.size());
        for(String slug:SpeciesDefinitions.ALL.keySet()) {
            var box=bounds.getAsJsonArray(slug);assertNotNull(slug,box);assertEquals(6,box.size());
            for(int i=0;i<3;i++)assertTrue(slug,box.get(i+3).getAsDouble()>=box.get(i).getAsDouble());
        }
        assertEquals(18,br.com.petrickfaria.minemon.item.PokemonTreatType.values().length);
    }

    @Test public void ghostLineUsesAuthoredMouthsAndGroundGaitWithNaturalBlinkRanges() throws Exception {
        Path root=Path.of(System.getProperty("minemon.testResources"));
        var gengar=SpeciesDefinitions.get("gengar");
        var animations=JsonParser.parseString(Files.readString(root.resolve(gengar.animation().replace("assets/minemon/","assets/minemon_gen1/"))))
                .getAsJsonObject().getAsJsonObject("animations");
        var walk=animations.getAsJsonObject("animation.gengar.ground_walk").getAsJsonObject("bones");
        assertTrue(walk.has("leg_left"));
        assertTrue(walk.has("leg_right"));
        var geo=JsonParser.parseString(Files.readString(root.resolve(gengar.model().replace("assets/minemon/","assets/minemon_gen1/"))))
                .getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
        var names=new java.util.HashSet<String>();
        for(JsonElement bone:geo)names.add(bone.getAsJsonObject().get("name").getAsString());
        assertTrue("authored teeth expression is present",names.contains("mouth_closed"));
        assertTrue("authored open-mouth expression is present",names.contains("mouth_open"));
        for(String slug:java.util.List.of("haunter","gengar")) {
            for(int offset=0;offset<=200;offset++) {
                int interval=GhostLineAnimationRules.blinkIntervalFromOffset(slug,offset);
                assertTrue(slug+" blink interval below 5 seconds: "+interval,interval>=100);
                assertTrue(slug+" blink interval above 15 seconds: "+interval,interval<=300);
            }
        }
    }
}
