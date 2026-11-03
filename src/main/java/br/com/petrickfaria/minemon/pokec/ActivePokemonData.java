package br.com.petrickfaria.minemon.pokec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

/** Durable owner-to-entity index; unloaded chunks continue to occupy a trainer slot. */
public final class ActivePokemonData extends SavedData {
    private static final String STORAGE_KEY = "minemon_active_pokemon";
    private final CompoundTag owners = new CompoundTag();

    public static ActivePokemonData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(ActivePokemonData::load,
                ActivePokemonData::new, STORAGE_KEY);
    }

    private static ActivePokemonData load(CompoundTag tag) {
        ActivePokemonData data = new ActivePokemonData();
        data.owners.merge(tag.getCompound("Owners"));
        return data;
    }

    public int count(UUID owner) { return owner == null ? 0 : owners.getCompound(owner.toString()).getAllKeys().size(); }

    public static void register(net.minecraft.world.entity.Entity entity) {
        if (entity instanceof br.com.petrickfaria.minemon.entity.MinemonCreature pokemon
                && pokemon.level instanceof ServerLevel level && pokemon.isTame() && pokemon.getOwnerUUID() != null)
            get(level).add(pokemon.getOwnerUUID(), pokemon.getUUID());
    }

    public static void unregister(net.minecraft.world.entity.Entity entity) {
        if (entity instanceof br.com.petrickfaria.minemon.entity.MinemonCreature pokemon
                && pokemon.level instanceof ServerLevel level && pokemon.getOwnerUUID() != null)
            get(level).remove(pokemon.getOwnerUUID(), pokemon.getUUID());
    }

    public void add(UUID owner, UUID pokemon) {
        if (owner == null || pokemon == null) return;
        CompoundTag entities = owners.getCompound(owner.toString());
        if (entities.getBoolean(pokemon.toString())) return;
        entities.putBoolean(pokemon.toString(), true);
        owners.put(owner.toString(), entities);
        setDirty();
    }

    public void remove(UUID owner, UUID pokemon) {
        if (owner == null || pokemon == null) return;
        CompoundTag entities = owners.getCompound(owner.toString());
        if (!entities.contains(pokemon.toString())) return;
        entities.remove(pokemon.toString());
        if (entities.isEmpty()) owners.remove(owner.toString());
        else owners.put(owner.toString(), entities);
        setDirty();
    }

    @Override public CompoundTag save(CompoundTag tag) { tag.put("Owners", owners.copy()); return tag; }
}
