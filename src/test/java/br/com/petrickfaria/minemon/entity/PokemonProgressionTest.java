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

    @Test public void rarityDamageUsesRequestedHeartsAndHalfHeartLevelGrowth() {
        String[] rarities={"common","uncommon","rare","legendary","mythic"};
        double[][] expectedHp={{0,1,2,3,4},{4,5,6,7,8},{9,10,11,12,13}};
        int[] levels={1,5,10};
        for(int row=0;row<levels.length;row++)for(int rank=0;rank<rarities.length;rank++)
            assertEquals(rarities[rank]+" L"+levels[row],expectedHp[row][rank],
                    PokemonProgression.damageForRarity(rarities[rank],levels[row]),0.0D);
    }

    @Test public void rangedCooldownScalesFromFiveToTwoAndHalfSeconds() {
        assertEquals(100, PokemonProgression.projectileCooldownTicks(1));
        assertEquals(50, PokemonProgression.projectileCooldownTicks(10));
        assertEquals(78, PokemonProgression.projectileCooldownTicks(5));
        assertEquals(56, PokemonProgression.projectileCooldownTicks(9));
        assertEquals(100, PokemonProgression.projectileCooldownTicks(0));
        assertEquals(50, PokemonProgression.projectileCooldownTicks(99));
    }

    private static int cost(int level, String rarity) {
        return PokemonProgression.xpForNextLevelForRarity(level, rarity, 5, 10);
    }
}
