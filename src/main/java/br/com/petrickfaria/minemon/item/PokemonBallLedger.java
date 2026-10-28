package br.com.petrickfaria.minemon.item;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent one-use tokens prevent cloned occupied balls from releasing/converting twice. */
public final class PokemonBallLedger extends SavedData {
    private static final String DATA_NAME = "minemon_pokeball_ledger";
    private static final String TOKENS_KEY = "ConsumedTokens";
    private final Set<UUID> consumed = new HashSet<>();

    public static PokemonBallLedger get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(PokemonBallLedger::load, PokemonBallLedger::new, DATA_NAME);
    }

    public synchronized boolean isConsumed(UUID token) { return consumed.contains(token); }

    public synchronized boolean consume(UUID token) {
        if (token == null || !consumed.add(token)) return false;
        setDirty();
        return true;
    }

    public static PokemonBallLedger load(CompoundTag tag) {
        PokemonBallLedger data = new PokemonBallLedger();
        ListTag tokens = tag.getList(TOKENS_KEY, 8);
        for (int i = 0; i < tokens.size(); i++) {
            try { data.consumed.add(UUID.fromString(tokens.getString(i))); }
            catch (IllegalArgumentException ignored) { }
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        ListTag tokens = new ListTag();
        for (UUID token : consumed) tokens.add(StringTag.valueOf(token.toString()));
        tag.put(TOKENS_KEY, tokens);
        return tag;
    }
}
