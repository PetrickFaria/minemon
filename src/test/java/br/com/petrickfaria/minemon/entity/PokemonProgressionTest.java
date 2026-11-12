package br.com.petrickfaria.minemon.entity;

import org.junit.Test;
import static org.junit.Assert.*;

public final class PokemonProgressionTest {
    @Test public void levelCostsMatchTheZombieReferenceAndEveryRarityStep() {
        String[] rarities = {"common", "uncommon", "rare", "legendary", "mythic"};
        int[] bases = {10, 11, 12, 13, 15};
        int[] totals = {0, 0, 0, 0, 0};
        for (int rarity = 0; rarity < rarities.length; rarity++) {
            for (int level = 1; level <= 9; level++) {
                int expected = (bases[rarity] + level - 1) * 5;
                assertEquals(rarities[rarity] + " level " + level, expected,
                        PokemonProgression.xpForNextLevelForRarity(level, rarities[rarity], 5, 10));
                totals[rarity] += expected;
            }
        }
        assertArrayEquals(new int[] {630, 675, 720, 765, 855}, totals);
        assertEquals(0, PokemonProgression.xpForNextLevelForRarity(10, "common", 5, 10));
        assertEquals(20, PokemonProgression.xpForNextLevelForRarity(1, "common", 2, 10));
        assertEquals(22, PokemonProgression.xpForNextLevelForRarity(1, "uncommon", 2, 10));
        assertEquals(24, PokemonProgression.xpForNextLevelForRarity(1, "rare", 2, 10));
        assertEquals(26, PokemonProgression.xpForNextLevelForRarity(1, "legendary", 2, 10));
        assertEquals(30, PokemonProgression.xpForNextLevelForRarity(1, "mythic", 2, 10));
    }

    @Test public void wildPokemonRewardsUseHalfXpUnitsWithoutRounding() {
        assertEquals(5, PokemonProgression.xpRewardHalfUnitsForWildRarity("common", 5));
        assertEquals(10, PokemonProgression.xpRewardHalfUnitsForWildRarity("uncommon", 5));
        assertEquals(20, PokemonProgression.xpRewardHalfUnitsForWildRarity("rare", 5));
        assertEquals(100, PokemonProgression.xpRewardHalfUnitsForWildRarity("legendary", 5));
        assertEquals(250, PokemonProgression.xpRewardHalfUnitsForWildRarity("mythic", 5));
    }

    @Test public void attributesKeepTheirConfiguredStartingValuesAndLevelGrowth() {
        assertEquals(10.0D, PokemonProgression.healthForRarity("common", 1), 0.0D);
        assertEquals(14.0D, PokemonProgression.healthForRarity("uncommon", 1), 0.0D);
        assertEquals(18.0D, PokemonProgression.healthForRarity("rare", 1), 0.0D);
        assertEquals(22.0D, PokemonProgression.healthForRarity("legendary", 1), 0.0D);
        assertEquals(26.0D, PokemonProgression.healthForRarity("mythic", 1), 0.0D);
        assertEquals(4.0D, PokemonProgression.healthForRarity("common", 2)
                - PokemonProgression.healthForRarity("common", 1), 0.0D);
        assertEquals(1.0D, PokemonProgression.damageForRarity("common", 2)
                - PokemonProgression.damageForRarity("common", 1), 0.0D);
    }

    @Test public void uncommonBulbasaurAndCharmanderDamageIsFixedInHalfHeartSteps() {
        double[] expectedPoints={4.0D,5.0D,6.0D,7.0D,8.0D};
        for (int level=1;level<=expectedPoints.length;level++)
            assertEquals("level "+level,expectedPoints[level-1],PokemonProgression.damageForRarity("uncommon",level),0.0D);
    }

    @Test public void chanseyCooldownUsesItsCurrentLevelBand() {
        assertEquals(6000, PokemonProgression.chanseyCooldownTicks(1));
        assertEquals(4800, PokemonProgression.chanseyCooldownTicks(4));
        assertEquals(3600, PokemonProgression.chanseyCooldownTicks(6));
        assertEquals(2400, PokemonProgression.chanseyCooldownTicks(8));
        assertEquals(1200, PokemonProgression.chanseyCooldownTicks(10));
    }
}
