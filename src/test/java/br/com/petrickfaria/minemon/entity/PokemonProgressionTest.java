package br.com.petrickfaria.minemon.entity;

import br.com.petrickfaria.minemon.pokec.PokemonEvolutionRules;
import org.junit.Test;
import static org.junit.Assert.*;

public final class PokemonProgressionTest {
    @Test public void levelCostsAndRarityMultipliersMatchTheRequestedTable() {
        assertEquals(5, PokemonProgression.xpForNextLevelForRarity(1, "common"));
        assertEquals(15, PokemonProgression.xpForNextLevelForRarity(2, "common"));
        assertEquals(30, PokemonProgression.xpForNextLevelForRarity(3, "common"));
        assertEquals(75, PokemonProgression.xpForNextLevelForRarity(5, "common"));
        assertEquals(225, PokemonProgression.xpForNextLevelForRarity(9, "common"));
        assertEquals(10, PokemonProgression.xpForNextLevelForRarity(1, "uncommon"));
        assertEquals(20, PokemonProgression.xpForNextLevelForRarity(1, "rare"));
        assertEquals(40, PokemonProgression.xpForNextLevelForRarity(1, "very_rare"));
        assertEquals(80, PokemonProgression.xpForNextLevelForRarity(1, "legendary"));
        assertEquals(80, PokemonProgression.xpForNextLevelForRarity(1, "mythic"));
        assertEquals(0, PokemonProgression.xpForNextLevelForRarity(10, "common"));
    }

    @Test public void evolutionRequirementsFollowCatalogEdgesAndBranches() {
        assertEquals(5, PokemonEvolutionRules.requiredLevel("charmander"));
        assertEquals(10, PokemonEvolutionRules.requiredLevel("charmeleon"));
        assertEquals(7, PokemonEvolutionRules.requiredLevel("rattata"));
        assertEquals(7, PokemonEvolutionRules.requiredLevel("eevee"));
        assertEquals(3, PokemonEvolutionRules.nextForms("eevee").size());
        assertFalse(PokemonEvolutionRules.isValidEdge("charmander", "charizard"));
        assertTrue(PokemonEvolutionRules.isValidEdge("charmander", "charmeleon"));
        assertEquals(Integer.MAX_VALUE, PokemonEvolutionRules.requiredLevel("mew"));
    }

    @Test public void chanseyCooldownUsesItsCurrentLevelBand() {
        assertEquals(6000, PokemonProgression.chanseyCooldownTicks(1));
        assertEquals(4800, PokemonProgression.chanseyCooldownTicks(4));
        assertEquals(3600, PokemonProgression.chanseyCooldownTicks(6));
        assertEquals(2400, PokemonProgression.chanseyCooldownTicks(8));
        assertEquals(1200, PokemonProgression.chanseyCooldownTicks(10));
    }
}
