package br.com.petrickfaria.minemon.entity;

import net.minecraft.util.RandomSource;
import java.util.Set;

/** Timing rules used only by the Gastly evolution line's client animation controllers. */
public final class GhostLineAnimationRules {
    public static final int HAUNTER_HAND_LAG_TICKS=50;
    public static final int GHOST_BLINK_DURATION_TICKS=8;
    public static final double GENGAR_SLEEP_SINK_FRACTION=0.35D;

    public static boolean shouldShowClosedGhostEyes(String slug,boolean sleeping,boolean blinking){
        return (slug.equals("haunter")||slug.equals("gengar"))&&(sleeping||blinking);
    }
    public static boolean isGengarTeethMouthBoneVisible(String bone){return bone.equals("mouth_closed");}
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
