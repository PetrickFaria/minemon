package br.com.petrickfaria.minemon.entity;

import net.minecraft.world.phys.AABB;

/** Pure geometry used by night shore searches and their regression tests. */
final class NightShoreLandingRules {
    private NightShoreLandingRules() {}

    static boolean insideSearchVolume(int dx, int dy, int dz) {
        return dx >= -25 && dx <= 24 && dy >= -25 && dy <= 24 && dz >= -25 && dz <= 24;
    }

    static boolean waterIsTooClose(AABB landingBox, int waterX, int waterZ, double minimumDistance) {
        double dx = Math.max(0.0D, Math.max(landingBox.minX - (waterX + 1.0D), waterX - landingBox.maxX));
        double dz = Math.max(0.0D, Math.max(landingBox.minZ - (waterZ + 1.0D), waterZ - landingBox.maxZ));
        return dx * dx + dz * dz < minimumDistance * minimumDistance;
    }
}
