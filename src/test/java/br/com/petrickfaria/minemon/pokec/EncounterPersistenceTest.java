package br.com.petrickfaria.minemon.pokec;

import net.minecraft.nbt.CompoundTag;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class EncounterPersistenceTest {
    @Test public void activePokemonCountIncludesEntitiesAcrossUnloadsAndSurvivesSave() {
        UUID owner = UUID.randomUUID(), first = UUID.randomUUID(), second = UUID.randomUUID();
        ActivePokemonData before = new ActivePokemonData();
        before.add(owner, first); before.add(owner, second); before.add(owner, first);
        ActivePokemonData after = ActivePokemonData.load(before.save(new CompoundTag()));
        assertEquals(2, after.count(owner));
        after.remove(owner, first);
        assertEquals(1, after.count(owner));
    }

    @Test public void encounterSlotsRemainIndependentAndSurviveSave() {
        UUID owner = UUID.randomUUID(), first = UUID.randomUUID(), second = UUID.randomUUID();
        WildEncounterData before = new WildEncounterData();
        before.put(owner, first, "active"); before.put(owner, second, "downed");
        WildEncounterData after = WildEncounterData.load(before.save(new CompoundTag()));
        assertEquals(1, after.count(owner, "active"));
        assertEquals(1, after.count(owner, "downed"));
        after.remove(owner, second);
        assertEquals(0, after.count(owner, "downed"));
    }
}
