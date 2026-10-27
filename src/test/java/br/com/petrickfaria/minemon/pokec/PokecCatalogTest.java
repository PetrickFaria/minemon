package br.com.petrickfaria.minemon.pokec;

import org.junit.Test;
import java.util.Set;
import static org.junit.Assert.*;

public class PokecCatalogTest {
    private static final Set<String> TYPES=Set.of("normal","fire","water","grass","electric","ice","fighting","poison","ground","flying","psychic","bug","rock","ghost","dragon");
    private static final Set<String> RARITIES=Set.of("common","uncommon","rare","very_rare","legendary");

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
