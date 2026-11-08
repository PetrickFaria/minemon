package br.com.petrickfaria.minemon.pokec;

import net.minecraft.nbt.CompoundTag;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SpawnChanceProgressionTest {
    @Test public void chanceGrowsLinearlyFromTheOriginalBase() {
        double base = 0.015D;
        assertEquals(0.015D, SpawnChanceProgression.chance(base, 0), 1.0E-12D);
        assertEquals(0.030D, SpawnChanceProgression.chance(base, SpawnChanceProgression.PERIOD_TICKS), 1.0E-12D);
        assertEquals(0.045D, SpawnChanceProgression.chance(base, 2 * SpawnChanceProgression.PERIOD_TICKS), 1.0E-12D);
        assertEquals(0.060D, SpawnChanceProgression.chance(base, 3 * SpawnChanceProgression.PERIOD_TICKS), 1.0E-12D);
    }

    @Test public void chanceCapsAtFiftyPercent() {
        assertEquals(0.50D, SpawnChanceProgression.chance(0.015D, 100 * SpawnChanceProgression.PERIOD_TICKS), 1.0E-12D);
        assertEquals(0.50D, SpawnChanceProgression.chance(0.75D, 0), 1.0E-12D);
    }

    @Test public void speciesAndPlayersKeepIndependentPersistentTimers() {
        CompoundTag firstPlayer = new CompoundTag();
        CompoundTag secondPlayer = new CompoundTag();
        SpawnChanceProgression.tick(firstPlayer, "bulbasaur");
        SpawnChanceProgression.tick(firstPlayer, "charmander");
        SpawnChanceProgression.tick(firstPlayer, "bulbasaur");
        SpawnChanceProgression.tick(secondPlayer, "bulbasaur");
        assertEquals(2L, SpawnChanceProgression.elapsedTicks(firstPlayer, "bulbasaur"));
        assertEquals(1L, SpawnChanceProgression.elapsedTicks(firstPlayer, "charmander"));
        assertEquals(1L, SpawnChanceProgression.elapsedTicks(secondPlayer, "bulbasaur"));
        SpawnChanceProgression.reset(firstPlayer, "bulbasaur");
        assertEquals(0L, SpawnChanceProgression.elapsedTicks(firstPlayer, "bulbasaur"));
        assertEquals(1L, SpawnChanceProgression.elapsedTicks(firstPlayer, "charmander"));
    }
}
