package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.entity.SpeciesDefinitions;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class SmokeSpeciesSelectionTest {
    @Test public void emptySelectionMeansCompletePokédex() {
        var all = SmokeSpeciesSelection.select(SpeciesDefinitions.ALL.values(), null);
        assertEquals(151, all.size());
        assertEquals("bulbasaur", all.get(0));
        assertEquals("mew", all.get(150));
    }

    @Test public void acceptsSlugNumberAndNationalDexPrefix() {
        var selected = SmokeSpeciesSelection.select(SpeciesDefinitions.ALL.values(), "graveler,#076,Farfetch'd");
        assertEquals(List.of("graveler", "golem", "farfetchd"), selected);
        assertEquals(List.of("golem"), SmokeSpeciesSelection.select(SpeciesDefinitions.ALL.values(), "#076"));
    }

    @Test public void unknownSpeciesFailsBeforeClientStartup() {
        try {
            SmokeSpeciesSelection.select(SpeciesDefinitions.ALL.values(), "missingmon");
            fail("Unknown species must fail fast");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("missingmon"));
        }
    }
}
