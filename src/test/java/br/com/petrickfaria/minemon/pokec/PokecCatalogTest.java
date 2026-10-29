package br.com.petrickfaria.minemon.pokec;

import org.junit.Test;
import java.util.Set;
import static org.junit.Assert.*;

public class PokecCatalogTest {
    private static final Set<String> TYPES=Set.of("normal","fire","water","grass","electric","ice","fighting","poison","ground","flying","psychic","bug","rock","ghost","dragon");
    private static final Set<String> RARITIES=Set.of("common","uncommon","rare","legendary","mythic");

    @Test public void completeNationalDexHasValidTypesAndRarities(){
        assertEquals(151,PokecSpecies.ordered().size());
        for(int i=0;i<151;i++){
            var species=PokecSpecies.ordered().get(i);
            assertEquals(i+1,species.number());assertNotNull(species.name());assertFalse(species.types().isEmpty());
            assertTrue("unknown rarity for "+species.slug(),RARITIES.contains(species.rarity()));
            for(String type:species.types())assertTrue("unknown Gen I type "+type+" for "+species.slug(),TYPES.contains(type));
            if(species.evolvesFrom()!=null)assertNotNull("unknown evolution parent",PokecSpecies.get(species.evolvesFrom()));
        }
    }

    @Test public void newRarityMigrationKeepsExactlyFiveRanksAndAllPricesFlat(){
        assertEquals("mythic", PokecSpecies.get("mew").rarity());
        for(var species:PokecSpecies.ordered()) assertEquals("all PokéC species have the same price",5,PokecEconomy.get().coins(species.slug()));
        for(int legacyPrice:new int[]{100,250,600,1500,4000})
            assertEquals("configured legacy species prices must never override the fixed price",5,PokecEconomy.enforceFixedCoinPrice(legacyPrice));
        assertEquals("rare", PokecEconomy.resolveRarity("very_rare","rare"));
    }

    @Test public void approvedFreeStartersAndSoulTypesArePresent(){
        assertEquals(Set.of("bulbasaur","charmander","squirtle","pikachu","eevee"),Set.of(PokecEconomy.STARTERS));
        assertEquals("grass",PokecSpecies.get("bulbasaur").primaryType());
        assertEquals("fire",PokecSpecies.get("charmander").primaryType());
        assertEquals("water",PokecSpecies.get("squirtle").primaryType());
        assertEquals("electric",PokecSpecies.get("pikachu").primaryType());
        assertEquals("normal",PokecSpecies.get("eevee").primaryType());
        assertEquals("eevee",PokecSpecies.get("vaporeon").evolvesFrom());
        assertEquals("eevee",PokecSpecies.get("jolteon").evolvesFrom());
        assertEquals("eevee",PokecSpecies.get("flareon").evolvesFrom());
    }
}
