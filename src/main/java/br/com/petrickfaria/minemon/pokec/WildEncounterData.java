package br.com.petrickfaria.minemon.pokec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

/** Durable index lets per-player encounter limits include entities in unloaded chunks. */
public final class WildEncounterData extends SavedData {
    private static final String STORAGE_KEY = "minemon_wild_encounters";
    private final CompoundTag owners = new CompoundTag();

    public static WildEncounterData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(WildEncounterData::load,
                WildEncounterData::new, STORAGE_KEY);
    }

    static WildEncounterData load(CompoundTag tag) {
        WildEncounterData data = new WildEncounterData();
        data.owners.merge(tag.getCompound("Owners"));
        return data;
    }

    public int count(UUID owner, String state) {
        if (owner == null) return 0;
        CompoundTag encounters = owners.getCompound(owner.toString());
        int count = 0;
        for (String key : encounters.getAllKeys()) if (state.equals(encounters.getString(key))) count++;
        return count;
    }

    public void put(UUID owner, UUID encounter, String state) {
        if (owner == null || encounter == null) return;
        CompoundTag encounters = owners.getCompound(owner.toString());
        encounters.putString(encounter.toString(), state);
        owners.put(owner.toString(), encounters);
        setDirty();
    }

    public void remove(UUID owner, UUID encounter) {
        if (owner == null || encounter == null) return;
        CompoundTag encounters = owners.getCompound(owner.toString());
        if (!encounters.contains(encounter.toString())) return;
        encounters.remove(encounter.toString());
        if (encounters.isEmpty()) owners.remove(owner.toString());
        else owners.put(owner.toString(), encounters);
        setDirty();
    }

    @Override public CompoundTag save(CompoundTag tag) {
        tag.put("Owners", owners.copy());
        return tag;
    }
}
