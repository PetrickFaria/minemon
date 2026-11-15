package br.com.petrickfaria.minemon.pokec;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.assertEquals;

public class PokemonTeamAverageTest {
    @Test public void averagesEveryPokemonAndRoundsDown(){
        assertEquals(5,PokemonTeamAverage.floorAverage(List.of(1,10),1));
        assertEquals(8,PokemonTeamAverage.floorAverage(List.of(1,10,10,10,10,10),1));
    }
    @Test public void emptyOrInvalidRosterUsesTheConfiguredMinimum(){
        assertEquals(3,PokemonTeamAverage.floorAverage(List.of(),3));
        assertEquals(1,PokemonTeamAverage.floorAverage(List.of(-1,0),0));
    }
}
