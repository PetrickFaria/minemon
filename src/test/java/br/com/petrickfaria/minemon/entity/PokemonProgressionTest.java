package br.com.petrickfaria.minemon.entity;

import br.com.petrickfaria.minemon.pokec.PokemonEvolutionRules;
import org.junit.Test;
import static org.junit.Assert.*;

public final class PokemonProgressionTest {
    @Test public void levelCostsAndRarityMultipliersMatchTheRequestedTable() {
        assertEquals(5, cost(1, "common"));
        assertEquals(15, cost(2, "common"));
        assertEquals(30, cost(3, "common"));
        assertEquals(75, cost(5, "common"));
        assertEquals(225, cost(9, "common"));
        assertEquals(10, cost(1, "uncommon"));
        assertEquals(20, cost(1, "rare"));
        assertEquals(40, cost(1, "very_rare"));
        assertEquals(80, cost(1, "legendary"));
        assertEquals(80, cost(1, "mythic"));
        assertEquals(0, cost(10, "common"));
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

    private static int cost(int level, String rarity) {
        return PokemonProgression.xpForNextLevelForRarity(level, rarity, 5, 10);
    }
}
