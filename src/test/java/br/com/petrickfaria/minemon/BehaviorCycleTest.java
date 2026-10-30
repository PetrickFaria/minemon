package br.com.petrickfaria.minemon;

import br.com.petrickfaria.minemon.entity.BehaviorCycle;
import br.com.petrickfaria.minemon.entity.SpeciesBehaviorProfiles;
import br.com.petrickfaria.minemon.entity.SpeciesDefinitions;
import net.minecraft.nbt.CompoundTag;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static br.com.petrickfaria.minemon.entity.BehaviorCycle.Phase.*;
import static br.com.petrickfaria.minemon.entity.SpeciesBehaviorProfiles.Profile.*;
import static org.junit.Assert.*;

public final class BehaviorCycleTest {
    private static BehaviorCycle.Observation o(boolean active, boolean ground, boolean band, boolean wet,
                                               boolean submerged, boolean surface, boolean dry, boolean dive) {
        return new BehaviorCycle.Observation(active, ground, band, wet, submerged, surface, dry, dive);
    }
    private static void repeat(BehaviorCycle cycle, int ticks, BehaviorCycle.Observation observation) {
        for (int i = 0; i < ticks; i++) cycle.tick(observation);
    }

    @Test public void all151SpeciesHaveOneApprovedRoutineCategory() {
        assertEquals(SpeciesDefinitions.ALL.keySet(), SpeciesBehaviorProfiles.all().keySet());
        Map<SpeciesBehaviorProfiles.Profile, Long> expected = Map.ofEntries(
                Map.entry(GROUND, 84L), Map.entry(WATER_VISITOR, 23L), Map.entry(GROUND_FLIGHT, 3L),
                Map.entry(FLYER, 14L), Map.entry(FLOAT_HALF, 3L), Map.entry(FLOAT_LOW, 2L),
                Map.entry(FLOAT_CYCLE, 8L), Map.entry(GROUND_FLOAT_CYCLE, 3L),
                Map.entry(AQUATIC_CYCLE, 7L), Map.entry(AQUATIC_BOTTOM, 2L),
                Map.entry(AQUATIC_NEAR_SURFACE, 1L), Map.entry(AQUATIC_SURFACE, 1L));
        for (var profile : SpeciesBehaviorProfiles.Profile.values())
            assertEquals(profile.name(), expected.get(profile).longValue(),
                    SpeciesBehaviorProfiles.all().values().stream().filter(profile::equals).count());
    }

    @Test public void landRoutineUsesIndependentThreeToFiveSecondWalkAndThreeToSevenSecondPause() {
        var a = new BehaviorCycle(GROUND, "bulbasaur", new Random(11));
        var b = new BehaviorCycle(GROUND, "pikachu", new Random(29));
        assertTrue(a.walking()); assertTrue(b.walking());
        assertTrue(a.gaitTicks() >= 60 && a.gaitTicks() <= 100);
        assertTrue(b.gaitTicks() >= 60 && b.gaitTicks() <= 100);
        assertNotEquals("individuals draw their own first gait duration", a.gaitTicks(), b.gaitTicks());
        int firstWalk = a.gaitTicks();
        repeat(a, firstWalk - 1, o(true, true, false, false, false, false, true, false));
        assertTrue(a.walking());
        a.tick(o(true, true, false, false, false, false, true, false));
        assertFalse(a.walking());
        assertTrue(a.gaitTicks() >= 60 && a.gaitTicks() <= 140);
        int pause = a.gaitTicks();
        repeat(a, pause, o(true, true, false, false, false, false, true, false));
        assertTrue(a.walking());
        assertTrue(a.gaitTicks() >= 60 && a.gaitTicks() <= 100);
    }

    @Test public void combatSleepAndAirborneTicksPauseGroundGait() {
        var cycle = new BehaviorCycle(GROUND, "bulbasaur", new Random(4));
        int initial = cycle.gaitTicks();
        repeat(cycle, 80, o(false, true, false, false, false, false, true, false));
        repeat(cycle, 80, o(true, false, false, false, false, false, false, false));
        assertEquals(initial, cycle.gaitTicks());
    }

    @Test public void allFourteenFlyersUseThreeMinutesAtEachApprovedHeight() {
        var flyers = List.of("butterfree", "beedrill", "pidgey", "pidgeotto", "pidgeot", "spearow",
                "fearow", "zubat", "golbat", "venomoth", "aerodactyl", "articuno", "zapdos", "moltres");
        var band = o(true, false, true, false, false, false, false, false);
        for (String slug : flyers) {
            var cycle = new BehaviorCycle(FLYER, slug, new Random(3));
            assertEquals(slug, 3.0D, cycle.minimumAltitude(), 0.0D);
            assertEquals(slug, 5.0D, cycle.maximumAltitude(), 0.0D);
            repeat(cycle, 3_599, band); assertEquals(slug, 0, cycle.stage());
            cycle.tick(band); assertEquals(slug, 1, cycle.stage());
            assertEquals(slug, 6.0D, cycle.minimumAltitude(), 0.0D);
            assertEquals(slug, 9.0D, cycle.maximumAltitude(), 0.0D);
            repeat(cycle, 3_599, band); cycle.tick(band);
            assertEquals(slug, 0, cycle.stage());
        }
    }

    @Test public void floatersUseTheirSeparateHeightBandsAndDurations() {
        for (String slug : List.of("metapod", "kakuna", "abra")) {
            var cycle = new BehaviorCycle(FLOAT_HALF, slug, new Random(1));
            double expected=1.0D/3.0D;
            assertEquals(expected, cycle.minimumAltitude(), 0.0D);
            assertEquals(expected, cycle.maximumAltitude(), 0.0D);
        }
        for (String slug : List.of("geodude", "porygon")) {
            var cycle = new BehaviorCycle(FLOAT_LOW, slug, new Random(2));
            assertEquals(slug.equals("porygon")?1.0D/3.0D:1.0D, cycle.minimumAltitude(), 0.0D);
            assertEquals(slug.equals("porygon")?1.0D/3.0D:1.0D, cycle.maximumAltitude(), 0.0D);
        }
        var observation = o(true, false, true, false, false, false, false, false);
        var gastly = new BehaviorCycle(FLOAT_CYCLE, "gastly", new Random(5));
        assertEquals(1.0D, gastly.minimumAltitude(), 0.0D); assertEquals(1.0D, gastly.maximumAltitude(), 0.0D);
        repeat(gastly, 5_999, observation); assertEquals(0, gastly.stage());
        gastly.tick(observation); assertEquals(1, gastly.stage());
        assertEquals(2.0D, gastly.minimumAltitude(), 0.0D); assertEquals(5.0D, gastly.maximumAltitude(), 0.0D);
        repeat(gastly, 3_600, observation); assertEquals(0, gastly.stage());
    }

    @Test public void mixedFlyersWalkFiveMinutesThenFlyThreeAndTwoMinutes() {
        var ground = o(true, true, false, false, false, false, true, false);
        var takeoff = o(true, false, true, false, false, false, false, false);
        for (String slug : List.of("charizard", "scyther", "dragonite")) {
            var cycle = new BehaviorCycle(GROUND_FLIGHT, slug, new Random(7));
            repeat(cycle, 5_999, ground); assertEquals(LAND, cycle.phase());
            cycle.tick(ground); assertEquals(TAKEOFF, cycle.phase());
            cycle.tick(takeoff); assertEquals(AIR, cycle.phase());
            assertEquals(3.0D, cycle.minimumAltitude(), 0.0D); assertEquals(5.0D, cycle.maximumAltitude(), 0.0D);
            repeat(cycle, 3_599, takeoff); cycle.tick(takeoff); assertEquals(1, cycle.stage());
            assertEquals(6.0D, cycle.minimumAltitude(), 0.0D); assertEquals(9.0D, cycle.maximumAltitude(), 0.0D);
            repeat(cycle, 2_399, takeoff); cycle.tick(takeoff); assertEquals(LANDING, cycle.phase());
            cycle.tick(ground); assertEquals(LAND, cycle.phase());
        }
    }

    @Test public void kadabraAlakazamAndGengarHaveTheirApprovedLandFloatCycles() {
        var ground = o(true, true, false, false, false, false, true, false);
        var band = o(true, false, true, false, false, false, false, false);
        for (String slug : List.of("kadabra", "alakazam")) {
            var cycle = new BehaviorCycle(GROUND_FLOAT_CYCLE, slug, new Random(8));
            repeat(cycle, 6_000, ground); assertEquals(TAKEOFF, cycle.phase());
            cycle.tick(band); repeat(cycle, 3_600, band); assertEquals(LANDING, cycle.phase());
            cycle.tick(ground); assertEquals(LAND, cycle.phase());
        }
        var gengar = new BehaviorCycle(GROUND_FLOAT_CYCLE, "gengar", new Random(9));
        repeat(gengar, 6_000, ground); gengar.tick(band);
        repeat(gengar, 3_600, band); assertEquals(1, gengar.stage());
        assertEquals(4.0D, gengar.minimumAltitude(), 0.0D); assertEquals(6.0D, gengar.maximumAltitude(), 0.0D);
        repeat(gengar, 2_400, band); assertEquals(LANDING, gengar.phase());
    }

    @Test public void amphibiansWaitFiveMinutesOnLandAndCountOnlyActualWaterTime() {
        var cycle = new BehaviorCycle(WATER_VISITOR, "staryu", new Random(2));
        var dry = o(true, true, false, false, false, false, true, false);
        repeat(cycle, 5_999, dry); assertEquals(COOLDOWN, cycle.phase());
        cycle.tick(dry); assertEquals(SEEK_WATER, cycle.phase());
        repeat(cycle, 2_000, o(true, true, false, false, false, false, true, false));
        assertEquals(SEEK_WATER, cycle.phase());
        var water = o(true, false, false, true, true, false, false, true);
        cycle.tick(water); assertTrue(cycle.phase() == SUBMERGED || cycle.phase() == SURFACE);
        for (int i = 0; i < 6_000 && cycle.phase() != EXIT_WATER; i++) cycle.tick(water);
        assertEquals(EXIT_WATER, cycle.phase());
        assertTrue("at most two surface visits per five-minute water cycle", cycle.surfaceVisits() <= 2);
        assertTrue("surface residence never exceeds 30 seconds", cycle.surfaceTicks() <= 600);
        cycle.tick(dry); assertEquals(COOLDOWN, cycle.phase());
    }

    @Test public void staryuAndStarmieMaySurfaceAtMostTwiceAndEachVisitIsCappedAtThirtySeconds() {
        for (String slug : List.of("staryu", "starmie")) {
            var cycle = new BehaviorCycle(WATER_VISITOR, slug, new Random(2));
            cycle.restore("SEEK_WATER", 0, 0);
            var water = o(true, false, false, true, true, false, false, true);
            cycle.tick(water);
            for (int i = 0; i < 6_000 && cycle.phase() != EXIT_WATER; i++) cycle.tick(
                    o(true, false, true, true, true, cycle.phase() == SURFACE, false, true));
            assertTrue(cycle.surfaceVisits() <= 2);
            assertTrue(cycle.surfaceTicks() <= 600);
        }
    }

    @Test public void shellderAndCloysterNeverEnterASurfacePhase() {
        for (String slug : List.of("shellder", "cloyster")) {
            var cycle = new BehaviorCycle(AQUATIC_BOTTOM, slug, new Random(3));
            var wet = o(true, false, false, true, true, false, false, true);
            repeat(cycle, 20_000, wet);
            assertEquals(SUBMERGED, cycle.phase());
            assertTrue(SpeciesBehaviorProfiles.prefersBottom(slug));
            assertFalse(SpeciesBehaviorProfiles.restsAtWaterBottom(slug));
        }
    }

    @Test public void sevenAquaticSpeciesSurfaceOnlyAfterFiveSubmergedMinutesAndReturnWithinThirtySeconds() {
        for (String slug : List.of("tentacool", "tentacruel", "horsea", "seadra", "goldeen", "seaking", "magikarp")) {
            var cycle = new BehaviorCycle(AQUATIC_CYCLE, slug, new Random(4));
            var submerged = o(true, false, false, true, true, false, false, true);
            repeat(cycle, 5_999, submerged); assertEquals(slug, SUBMERGED, cycle.phase());
            cycle.tick(submerged); assertEquals(slug, SURFACE, cycle.phase());
            repeat(cycle, 599, o(true, false, false, true, false, true, false, true));
            assertEquals(slug, SURFACE, cycle.phase());
            cycle.tick(o(true, false, false, true, false, true, false, true));
            assertEquals(slug, SUBMERGED, cycle.phase());
        }
    }

    @Test public void saveLoadPreservesStageGaitAndWaterVisitCounters() {
        var cycle = new BehaviorCycle(WATER_VISITOR, "staryu", new Random(5));
        cycle.restore("SEEK_WATER", 0, 0);
        var water = o(true, false, false, true, true, false, false, true);
        cycle.tick(water);
        repeat(cycle, 2_300, water);
        var tag = new CompoundTag(); cycle.save(tag);
        var copy = new BehaviorCycle(WATER_VISITOR, "staryu", new Random(6)); copy.restore(tag);
        assertEquals(cycle.phase(), copy.phase());
        assertEquals(cycle.ticks(), copy.ticks());
        assertEquals(cycle.aquaticTicks(), copy.aquaticTicks());
        assertEquals(cycle.surfaceVisits(), copy.surfaceVisits());
        assertEquals(cycle.surfaceVisitLimit(), copy.surfaceVisitLimit());
        assertEquals(cycle.surfaceTicks(), copy.surfaceTicks());
    }
}
