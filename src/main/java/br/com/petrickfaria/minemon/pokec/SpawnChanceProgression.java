package br.com.petrickfaria.minemon.pokec;

import net.minecraft.nbt.CompoundTag;

/** Per-player, per-species additive wild encounter chance progression. */
public final class SpawnChanceProgression {
    public static final long PERIOD_TICKS = 20L * 60L;
    public static final double MAX_CHANCE = 0.5D;
    private static final String ROOT = "MinemonWildSpawnProgress";

    private SpawnChanceProgression() {}

    public static long elapsedTicks(CompoundTag playerData, String species) {
        return Math.max(0L, playerData.getCompound(ROOT).getLong(species));
    }

    public static void tick(CompoundTag playerData, String species) {
        CompoundTag progress = playerData.getCompound(ROOT);
        long current = Math.max(0L, progress.getLong(species));
        if (current < Long.MAX_VALUE) progress.putLong(species, current + 1L);
        playerData.put(ROOT, progress);
    }

    public static void reset(CompoundTag playerData, String species) {
        CompoundTag progress = playerData.getCompound(ROOT);
        progress.putLong(species, 0L);
        playerData.put(ROOT, progress);
    }
    public static long incrementCount(CompoundTag playerData,String species){
        return elapsedTicks(playerData,species)/PERIOD_TICKS;
    }

    public static double chance(double baseChance, long elapsedTicks) {
        if (!Double.isFinite(baseChance) || baseChance <= 0.0D) return 0.0D;
        double base = Math.min(MAX_CHANCE, baseChance);
        long periods = Math.max(0L, elapsedTicks) / PERIOD_TICKS;
        return Math.min(MAX_CHANCE, base * (1.0D + 0.5D * periods));
    }
}
