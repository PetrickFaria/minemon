package br.com.petrickfaria.minemon.pokec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

/** Durable queue for trainer XP earned while the owner is offline. */
public final class TrainerXpLedger extends SavedData {
    private static final String STORAGE_KEY = "minemon_trainer_xp_ledger";
    private final CompoundTag pending = new CompoundTag();

    public static TrainerXpLedger get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                TrainerXpLedger::load, TrainerXpLedger::new, STORAGE_KEY);
    }

    private static TrainerXpLedger load(CompoundTag tag) {
        TrainerXpLedger data = new TrainerXpLedger();
        data.pending.merge(tag.getCompound("Pending"));
        return data;
    }

    public static void queue(ServerLevel level, UUID playerId, double xp) {
        if (playerId == null || !Double.isFinite(xp) || xp <= 0.0D) return;
        TrainerXpLedger data = get(level);
        String key = playerId.toString();
        double total = data.pending.getDouble(key);
        if (!Double.isFinite(total) || total < 0.0D) total = 0.0D;
        data.pending.putDouble(key, total + xp);
        data.setDirty();
    }

    public static double pending(ServerLevel level, UUID playerId) {
        if (playerId == null) return 0.0D;
        double value = get(level).pending.getDouble(playerId.toString());
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }

    public static void deliver(ServerPlayer player) {
        if (!(player.level instanceof ServerLevel level)) return;
        TrainerXpLedger data = get(level);
        String key = player.getUUID().toString();
        double value = data.pending.getDouble(key);
        if (!Double.isFinite(value) || value <= 0.0D) return;
        data.pending.remove(key);
        data.setDirty();
        PokecPlayerData.addTrainerXp(player, value);
    }

    @Override public CompoundTag save(CompoundTag tag) {
        tag.put("Pending", pending.copy());
        return tag;
    }
}
