package br.com.petrickfaria.minemon.entity;

/** Geodude's ground_walk also rolls; only an explicit run may select a rolling clip. */
public final class GeodudeLocomotion {
    private GeodudeLocomotion() { }
    public static String clipKey(boolean moving, boolean running) {
        return moving && running ? "ground_run" : "ground_idle";
    }
}
