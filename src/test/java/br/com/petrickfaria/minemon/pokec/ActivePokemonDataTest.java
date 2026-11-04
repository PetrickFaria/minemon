package br.com.petrickfaria.minemon.pokec;

import net.minecraft.nbt.CompoundTag;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class ActivePokemonDataTest {
    @Test public void activeLimitCountsUniquePetsAndPersistsAcrossSaves() {
        UUID owner = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ActivePokemonData data = new ActivePokemonData();

        assertTrue(data.hasCapacity(owner, 1));
        data.add(owner, first);
        data.add(owner, first);
        assertEquals(1, data.count(owner));
        assertFalse(data.hasCapacity(owner, 1));
        assertTrue(data.hasCapacity(owner, 2));

        data.add(owner, second);
        ActivePokemonData loaded = ActivePokemonData.load(data.save(new CompoundTag()));
        assertEquals(2, loaded.count(owner));
        assertFalse(loaded.hasCapacity(owner, 2));
        assertTrue(loaded.hasCapacity(owner, 3));
    }
}
