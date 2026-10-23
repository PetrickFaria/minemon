package br.com.petrickfaria.minemon;

import br.com.petrickfaria.minemon.entity.SpeciesBehaviorProfiles;
import br.com.petrickfaria.minemon.entity.SpeciesDefinitions;
import org.junit.Test;
import static org.junit.Assert.*;

public final class NaturalBehaviorProfileTest {
    @Test public void electabuzzIsExactlySeventyFivePercentOfItsPreviousScale() {
        var electabuzz = SpeciesDefinitions.get("electabuzz");
        assertEquals(1.35D, electabuzz.scale(), 0.0001D);
        assertEquals(1.05D, electabuzz.width(), 0.0001D);
        assertEquals(2.25D, electabuzz.height(), 0.0001D);
        assertEquals(0.75D, electabuzz.scale() / 1.8D, 0.0001D);
    }

    @Test public void airborneAndAquaticGroupsAreExplicitAndDoNotUseTypeLabels() {
        assertTrue(SpeciesBehaviorProfiles.isAirborne("gastly"));
        assertTrue(SpeciesBehaviorProfiles.isAirborne("charizard"));
        assertTrue(SpeciesBehaviorProfiles.isAirborne("mew"));
        assertFalse(SpeciesBehaviorProfiles.isAirborne("gyarados"));
        assertTrue(SpeciesBehaviorProfiles.isAquatic("gyarados"));
        assertTrue(SpeciesBehaviorProfiles.isAquatic("magikarp"));
        assertFalse(SpeciesBehaviorProfiles.isAquatic("squirtle"));
        assertFalse(SpeciesBehaviorProfiles.isAquatic("psyduck"));
        assertEquals(151, SpeciesDefinitions.ALL.size());
    }

    @Test public void everyProfileEntryHasARegisteredSpeciesDefinition() {
        SpeciesBehaviorProfiles.airborneSpecies().forEach(slug -> assertNotNull(slug, SpeciesDefinitions.get(slug)));
        SpeciesBehaviorProfiles.aquaticSpecies().forEach(slug -> assertNotNull(slug, SpeciesDefinitions.get(slug)));
        for(String slug:SpeciesBehaviorProfiles.aquaticSpecies()) {
            var clips=SpeciesDefinitions.get(slug).clips();
            assertTrue(slug+" needs a native swim clip", clips.containsKey("water_swim")
                    || clips.containsKey("water_surface_swim") || clips.containsKey("surface_water_swim")
                    || clips.containsKey("surfacewater_swim"));
            assertTrue(slug+" needs a native water idle clip", clips.containsKey("water_idle")
                    || clips.containsKey("water_surface_idle") || clips.containsKey("surface_water_idle")
                    || clips.containsKey("surfacewater_idle"));
        }
        for(var definition:SpeciesDefinitions.ALL.values()) {
            if(definition.clips().containsKey("ground_run")
                    && !SpeciesBehaviorProfiles.isAirborne(definition.slug())
                    && !SpeciesBehaviorProfiles.isAquatic(definition.slug()))
                assertNotNull(definition.slug()+" run clip",definition.clips().get("ground_run"));
        }
    }
}
