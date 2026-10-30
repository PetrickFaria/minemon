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
}
