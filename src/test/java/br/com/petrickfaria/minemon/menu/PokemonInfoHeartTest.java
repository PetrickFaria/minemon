package br.com.petrickfaria.minemon.menu;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PokemonInfoHeartTest {
    @Test public void mapsFullHalfAndEmptyHeartSpritesPrecisely() {
        assertEquals(52, PokemonInfoScreen.heartSpriteU(2.0D));
        assertEquals(61, PokemonInfoScreen.heartSpriteU(1.0D));
        assertEquals(16, PokemonInfoScreen.heartSpriteU(0.0D));
    }

    @Test public void preservesFractionalDamageAcrossMultipleHearts() {
        assertEquals(52, PokemonInfoScreen.heartSpriteU(3.0D));
        assertEquals(61, PokemonInfoScreen.heartSpriteU(1.0D));
    }

    @Test public void statusScreenUsesTheRequestedCompactRangeLabels() {
        assertEquals("Perc. Alcance", PokemonInfoScreen.PERCEPTION_RANGE_LABEL);
        assertEquals("Perc. Ataque", PokemonInfoScreen.ATTACK_RANGE_LABEL);
    }
}
