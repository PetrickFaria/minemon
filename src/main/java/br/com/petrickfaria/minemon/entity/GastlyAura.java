package br.com.petrickfaria.minemon.entity;

/** The restored Gastly smoke emitter produces one sprite every two ticks. */
public final class GastlyAura {
    public static boolean emitsAt(int tick) {
        return tick % 2 == 0;
    }

    private GastlyAura() { }
}
