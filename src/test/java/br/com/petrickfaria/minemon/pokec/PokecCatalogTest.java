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

    @Test public void rarityEconomyUsesFiveRanksAndConfiguredTiers(){
        assertEquals("mythic", PokecSpecies.get("mew").rarity());
        assertEquals(10,PokecEconomy.get().coins("rattata"));
        assertEquals(20,PokecEconomy.get().coins("charmander"));
        assertEquals(20,PokecEconomy.get().coins("charizard"));
        assertEquals(50,PokecEconomy.get().coins("mewtwo"));
        assertEquals(50,PokecEconomy.get().coins("mew"));
        assertEquals(25,PokecEconomy.get().souls("rattata"));
        assertEquals(50,PokecEconomy.get().souls("charmander"));
        assertEquals(50,PokecEconomy.get().souls("charizard"));
        assertEquals(150,PokecEconomy.get().souls("mewtwo"));
        assertEquals(150,PokecEconomy.get().souls("mew"));
        assertEquals(37,PokecEconomy.enforceFixedCoinPrice(37));
        assertEquals("rare", PokecEconomy.resolveRarity("very_rare","rare"));
    }

    @Test public void exactRequestedDistributionAndConsumersAgree(){
        var counts=new java.util.HashMap<String,Integer>();
        for(var species:PokecSpecies.ordered()){
            int n=species.number();
            String expected=n>=10&&n<=22?"common":n>=147&&n<=149?"rare":n>=144&&n<=146?"legendary":n>=150?"mythic":"uncommon";
            assertEquals(species.slug(),expected,species.rarity());
            assertEquals(species.slug(),expected,PokecEconomy.get().rarity(species.slug()));
            assertEquals(species.slug(),expected,PokemonSoulConversion.preview(species.slug(),1).rarity());
            counts.merge(expected,1,Integer::sum);
        }
        assertEquals(java.util.Map.of("common",13,"uncommon",130,"rare",3,"legendary",3,"mythic",2),counts);
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
