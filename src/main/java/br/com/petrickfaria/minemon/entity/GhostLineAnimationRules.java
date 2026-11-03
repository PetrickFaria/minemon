package br.com.petrickfaria.minemon.entity;

import net.minecraft.util.RandomSource;
import java.util.Set;

/** Timing rules used only by the Gastly evolution line's client animation controllers. */
public final class GhostLineAnimationRules {
    private static final Set<String> NATURAL_BLINKERS=Set.of("haunter","gengar");
    public static int nextBlinkInterval(String slug,RandomSource random){
        int range=NATURAL_BLINKERS.contains(slug)?201:120;
        return blinkIntervalFromOffset(slug,random.nextInt(range));
    }
    public static int blinkIntervalFromOffset(String slug,int offset){
        int range=NATURAL_BLINKERS.contains(slug)?201:120;
        if(offset<0||offset>=range)throw new IllegalArgumentException("Blink offset outside configured range: "+offset);
        return (NATURAL_BLINKERS.contains(slug)?100:80)+offset;
    }
    private GhostLineAnimationRules(){}
}
