package br.com.petrickfaria.minemon.pokec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Durable owner-to-entity index; unloaded chunks continue to occupy a trainer slot. */
public final class ActivePokemonData extends SavedData {
    private static final String STORAGE_KEY = "minemon_active_pokemon";
    private final CompoundTag owners = new CompoundTag();
    public record Entry(UUID id, String species, String name, int level, String behavior) { }

    public static ActivePokemonData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(ActivePokemonData::load,
                ActivePokemonData::new, STORAGE_KEY);
    }

    static ActivePokemonData load(CompoundTag tag) {
        ActivePokemonData data = new ActivePokemonData();
        data.owners.merge(tag.getCompound("Owners"));
        return data;
    }

    public int count(UUID owner) { return owner == null ? 0 : owners.getCompound(owner.toString()).getAllKeys().size(); }

    public boolean hasCapacity(UUID owner, int trainerLevel) {
        return owner != null && count(owner) < Math.max(1, Math.min(TrainerProgression.MAX_LEVEL, trainerLevel));
    }

    public static boolean hasCapacity(ServerPlayer owner) {
        return owner != null && owner.level instanceof ServerLevel level
                && get(level).hasCapacity(owner.getUUID(), PokecPlayerData.trainerLevel(owner));
    }

    public static void register(net.minecraft.world.entity.Entity entity) {
        if (entity instanceof br.com.petrickfaria.minemon.entity.MinemonCreature pokemon
                && pokemon.level instanceof ServerLevel level && pokemon.isTame() && pokemon.getOwnerUUID() != null) {
            ActivePokemonData data = get(level);
            data.add(pokemon.getOwnerUUID(), pokemon.getUUID());
            data.snapshot(pokemon);
            data.applyStoredBehavior(pokemon);
        }
    }

    private void snapshot(br.com.petrickfaria.minemon.entity.MinemonCreature pokemon) {
        CompoundTag record = record(pokemon.getOwnerUUID(), pokemon.getUUID());
        record.putString("species", pokemon.getSpeciesSlug());
        record.putString("name", pokemon.getName().getString());
        record.putInt("level", pokemon.getPokemonLevel());
        if (!record.contains("behavior")) record.putString("behavior", behaviorOf(pokemon));
        putRecord(pokemon.getOwnerUUID(), pokemon.getUUID(), record);
    }

    public static void updateSnapshot(br.com.petrickfaria.minemon.entity.MinemonCreature pokemon) {
        if (pokemon == null || !(pokemon.level instanceof ServerLevel level) || !pokemon.isTame() || pokemon.getOwnerUUID() == null) return;
        ActivePokemonData data = get(level);
        data.add(pokemon.getOwnerUUID(), pokemon.getUUID());
        CompoundTag record = data.record(pokemon.getOwnerUUID(), pokemon.getUUID());
        record.putString("species", pokemon.getSpeciesSlug());
        record.putString("name", pokemon.getName().getString());
        record.putInt("level", pokemon.getPokemonLevel());
        record.putString("behavior", behaviorOf(pokemon));
        data.putRecord(pokemon.getOwnerUUID(), pokemon.getUUID(), record);
    }

    private static String behaviorOf(br.com.petrickfaria.minemon.entity.MinemonCreature pokemon) {
        return pokemon.isOrderedToSit() ? "sit" : pokemon.isFreeMode() ? "free" : "follow";
    }

    private void applyStoredBehavior(br.com.petrickfaria.minemon.entity.MinemonCreature pokemon) {
        String behavior = record(pokemon.getOwnerUUID(), pokemon.getUUID()).getString("behavior");
        if (!behavior.isEmpty()) {
            // Duplicate registration must preserve an active sleep state for
            // stay/free modes. A saved explicit follow command takes priority
            // over that sleep state and must wake the companion after reload.
            boolean preserveSleep = pokemon.isSleeping() && !behavior.equals("follow");
            pokemon.applyManagedBehavior(behavior);
            if (preserveSleep && !pokemon.isSleeping()) pokemon.setSleeping(true);
        }
    }

    public List<Entry> entries(UUID owner) {
        if (owner == null) return List.of();
        CompoundTag entities = owners.getCompound(owner.toString());
        List<Entry> result = new ArrayList<>();
        for (String idText : entities.getAllKeys()) {
            try {
                UUID id = UUID.fromString(idText);
                CompoundTag stored = entities.getCompound(idText);
                result.add(new Entry(id, stored.getString("species"), stored.getString("name"),
                        Math.max(1, stored.getInt("level")), stored.getString("behavior").isEmpty() ? "follow" : stored.getString("behavior")));
            } catch (IllegalArgumentException ignored) { }
        }
        result.sort(Comparator.comparing(Entry::species).thenComparing(Entry::name));
        return List.copyOf(result);
    }

    public boolean setBehavior(UUID owner, UUID pokemon, String behavior) {
        if (owner == null || pokemon == null || !List.of("follow", "sit", "free").contains(behavior)) return false;
        CompoundTag ownerData = owners.getCompound(owner.toString());
        if (!ownerData.contains(pokemon.toString())) return false;
        CompoundTag record = ownerData.getCompound(pokemon.toString());
        record.putString("behavior", behavior);
        ownerData.put(pokemon.toString(), record);
        owners.put(owner.toString(), ownerData);
        setDirty();
        return true;
    }

    public boolean owns(UUID owner, UUID pokemon) {
        return owner != null && pokemon != null && owners.getCompound(owner.toString()).contains(pokemon.toString());
    }

    private CompoundTag record(UUID owner, UUID pokemon) {
        CompoundTag all = owners.getCompound(owner.toString());
        CompoundTag value = all.getCompound(pokemon.toString()).copy();
        // Migrate old boolean-only entries without changing their UUID identity.
        if (value.isEmpty() && all.getBoolean(pokemon.toString())) value.putBoolean("registered", true);
        return value;
    }

    private void putRecord(UUID owner, UUID pokemon, CompoundTag value) {
        CompoundTag all = owners.getCompound(owner.toString());
        all.put(pokemon.toString(), value);
        owners.put(owner.toString(), all);
        setDirty();
    }

    public static void unregister(net.minecraft.world.entity.Entity entity) {
        if (entity instanceof br.com.petrickfaria.minemon.entity.MinemonCreature pokemon
                && pokemon.level instanceof ServerLevel level && pokemon.getOwnerUUID() != null)
            get(level).remove(pokemon.getOwnerUUID(), pokemon.getUUID());
    }

    public void add(UUID owner, UUID pokemon) {
        if (owner == null || pokemon == null) return;
        CompoundTag entities = owners.getCompound(owner.toString());
        if (entities.contains(pokemon.toString())) return;
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
