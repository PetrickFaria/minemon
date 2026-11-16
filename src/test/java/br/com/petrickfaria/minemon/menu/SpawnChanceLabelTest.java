package br.com.petrickfaria.minemon.menu;

import java.util.Locale;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpawnChanceLabelTest {
    @Test public void portugueseExamplesHaveOnlyCurrentChanceAndCounter() {
        Locale pt=Locale.forLanguageTag("pt-BR");
        assertEquals("1% (0)",SpawnChanceLabel.format(.01D,0,true,pt));
        assertEquals("1,5% (1)",SpawnChanceLabel.format(.015D,1,true,pt));
        assertEquals("2% (2)",SpawnChanceLabel.format(.02D,2,true,pt));
        assertEquals("2,5% (3)",SpawnChanceLabel.format(.025D,3,true,pt));
        assertEquals("0%",SpawnChanceLabel.format(.025D,3,false,pt));
        assertEquals("50% (99)",SpawnChanceLabel.format(.9D,99,true,pt));
        assertEquals("1.5% (1)",SpawnChanceLabel.format(.015D,1,true,Locale.US));
    }
}
