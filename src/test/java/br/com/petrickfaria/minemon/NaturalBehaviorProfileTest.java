package br.com.petrickfaria.minemon;

import br.com.petrickfaria.minemon.entity.SpeciesBehaviorProfiles;
import br.com.petrickfaria.minemon.entity.SpeciesDefinitions;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public final class NaturalBehaviorProfileTest {
    @Test public void all151RosterEntriesHaveExactCategoryAndExpectedSpecificAssignments() {
        assertEquals(151, SpeciesDefinitions.ALL.size());
        assertEquals(SpeciesDefinitions.ALL.keySet(), SpeciesBehaviorProfiles.all().keySet());
        Set<String> land = SpeciesBehaviorProfiles.all().entrySet().stream()
                .filter(e -> e.getValue() == SpeciesBehaviorProfiles.Profile.GROUND)
                .map(java.util.Map.Entry::getKey).collect(java.util.stream.Collectors.toSet());
        assertEquals(84, land.size());
        for (String slug : Set.of("charizard", "scyther", "dragonite"))
            assertEquals(SpeciesBehaviorProfiles.Profile.GROUND_FLIGHT, SpeciesBehaviorProfiles.profile(slug));
        for (String slug : Set.of("pidgey", "pidgeotto", "pidgeot", "spearow", "fearow", "aerodactyl"))
            assertEquals(SpeciesBehaviorProfiles.Profile.FLYER, SpeciesBehaviorProfiles.profile(slug));
        for (String slug : Set.of("gastly", "haunter", "magnemite", "magneton", "koffing", "weezing", "mewtwo", "mew"))
            assertEquals(SpeciesBehaviorProfiles.Profile.FLOAT_CYCLE, SpeciesBehaviorProfiles.profile(slug));
        for (String slug : Set.of("kadabra", "alakazam", "gengar"))
            assertEquals(SpeciesBehaviorProfiles.Profile.GROUND_FLOAT_CYCLE, SpeciesBehaviorProfiles.profile(slug));
    }

    @Test public void timeWindowsUseMinecraftDayTimeForTheFiveNocturnalSpecies() {
        for (String slug : Set.of("gastly", "haunter", "gengar", "zubat", "golbat")) {
            assertFalse(slug, SpeciesBehaviorProfiles.shouldSleep(slug, 5_999));
            assertTrue(slug, SpeciesBehaviorProfiles.shouldSleep(slug, 6_000));
            assertTrue(slug, SpeciesBehaviorProfiles.shouldSleep(slug, 12_999));
            assertFalse(slug, SpeciesBehaviorProfiles.shouldSleep(slug, 13_000));
            assertFalse(slug, SpeciesBehaviorProfiles.shouldSleep(slug, 18_000));
            assertFalse(slug, SpeciesBehaviorProfiles.shouldSleep(slug, 24_000 + 5_999));
            assertTrue(slug, SpeciesBehaviorProfiles.shouldSleep(slug, 24_000 + 6_000));
        }
        for (String slug : SpeciesDefinitions.ALL.keySet()) {
            if (SpeciesBehaviorProfiles.isNocturnal(slug)) continue;
            assertFalse(slug, SpeciesBehaviorProfiles.shouldSleep(slug, 6_000));
            assertTrue(slug, SpeciesBehaviorProfiles.shouldSleep(slug, 13_000));
            assertFalse(slug, SpeciesBehaviorProfiles.shouldSleep(slug, 23_000));
        }
    }

    @Test public void bottomSwimmingAndRestOverridesMatchTheApprovedLists() {
        assertEquals(Set.of("shellder", "cloyster", "krabby", "kingler", "staryu", "starmie",
                        "omanyte", "omastar", "kabuto", "kabutops"),
                SpeciesBehaviorProfiles.all().keySet().stream().filter(SpeciesBehaviorProfiles::prefersBottom)
                        .collect(java.util.stream.Collectors.toSet()));
        assertEquals(Set.of("gyarados", "shellder", "cloyster"),
                SpeciesBehaviorProfiles.all().keySet().stream().filter(SpeciesBehaviorProfiles::restsAtWaterBottom)
                        .collect(java.util.stream.Collectors.toSet()));
        assertEquals(Set.of("charizard", "scyther", "dragonite", "butterfree", "beedrill", "pidgey", "pidgeotto", "pidgeot",
                        "spearow", "fearow", "zubat", "golbat", "venomoth", "aerodactyl", "articuno", "zapdos", "moltres",
                        "metapod", "kakuna", "geodude", "kadabra", "alakazam", "gengar"),
                SpeciesBehaviorProfiles.all().keySet().stream().filter(SpeciesBehaviorProfiles::landsForNightRest)
                        .collect(java.util.stream.Collectors.toSet()));
        assertEquals(0.5D, SpeciesBehaviorProfiles.nightRestHoverHeight("abra"), 0.0D);
        assertEquals(1.0D, SpeciesBehaviorProfiles.nightRestHoverHeight("gastly"), 0.0D);
    }

    @Test public void movementClassificationsKeepGroundAndWaterNativeClips() {
        for (var definition : SpeciesDefinitions.ALL.values()) {
            var profile = SpeciesBehaviorProfiles.profile(definition.slug());
            if (profile == SpeciesBehaviorProfiles.Profile.GROUND || profile == SpeciesBehaviorProfiles.Profile.GROUND_FLIGHT
                    || profile == SpeciesBehaviorProfiles.Profile.GROUND_FLOAT_CYCLE || profile == SpeciesBehaviorProfiles.Profile.WATER_VISITOR) {
                if (definition.clips().containsKey("ground_idle"))
                    assertEquals(definition.slug(), definition.clips().get("ground_idle"), definition.idle());
                if (definition.clips().containsKey("ground_walk"))
                    assertEquals(definition.slug(), definition.clips().get("ground_walk"), definition.walk());
            }
            if (SpeciesBehaviorProfiles.isAquatic(definition.slug())) {
                var clips = definition.clips();
                assertTrue(definition.slug(), clips.containsKey("water_swim") || clips.containsKey("water_surface_swim")
                        || clips.containsKey("surface_water_swim") || clips.containsKey("surfacewater_swim"));
                assertTrue(definition.slug(), clips.containsKey("water_idle") || clips.containsKey("water_surface_idle")
                        || clips.containsKey("surface_water_idle") || clips.containsKey("surfacewater_idle"));
            }
        }
        assertTrue(SpeciesBehaviorProfiles.canFly("mewtwo"));
        assertTrue(SpeciesBehaviorProfiles.isAirborne("mewtwo"));
        assertTrue(SpeciesBehaviorProfiles.canFly("kadabra"));
        assertFalse(SpeciesBehaviorProfiles.isAirborne("kadabra"));
        assertTrue(SpeciesBehaviorProfiles.canFly("charizard"));
        assertFalse(SpeciesBehaviorProfiles.isAirborne("charizard"));
        assertTrue(SpeciesBehaviorProfiles.isVisitor("vaporeon"));
        assertTrue(SpeciesBehaviorProfiles.isAquatic("shellder"));
    }
}
