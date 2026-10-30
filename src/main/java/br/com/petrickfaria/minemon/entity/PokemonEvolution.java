package br.com.petrickfaria.minemon.entity;

import br.com.petrickfaria.minemon.pokec.PokemonEvolutionRules;
import br.com.petrickfaria.minemon.pokec.PokecSpecies;
import br.com.petrickfaria.minemon.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

/** Server-only replacement of an owned entity with a valid next species form. */
public final class PokemonEvolution {
    private PokemonEvolution() {}

    public static MinemonCreature evolve(ServerPlayer player, MinemonCreature source, String targetSlug) {
        if (player == null || source == null || !(source.level instanceof ServerLevel level)
                || source.isDefeated() || !source.isAlive() || !source.isTame() || !source.isOwnedBy(player)
                || !PokemonEvolutionRules.isValidEdge(source.getSpeciesSlug(), targetSlug)
                || source.getPokemonLevel() < PokemonEvolutionRules.requiredLevel(source.getSpeciesSlug())) return null;
        PokecSpecies.Entry targetSpecies = PokecSpecies.get(targetSlug);
        var targetType = ModEntities.SPECIES.get(targetSlug);
        if (targetSpecies == null || targetType == null) return null;
        MinemonCreature evolved = targetType.get().create(level);
        if (evolved == null) return null;

        float healthRatio = source.getHealth() / Math.max(1.0F, source.getMaxHealth());
        CompoundTag saved = source.saveWithoutId(new CompoundTag());
        saved.remove("UUID");
        saved.remove("UUIDMost");
        saved.remove("UUIDLeast");
        saved.putBoolean("MinemonSleeping", false);
        saved.putBoolean("NightRoutine", false);
        evolved.load(saved);
        evolved.moveTo(source.getX(), source.getY(), source.getZ(), source.getYRot(), source.getXRot());
        evolved.setPokemonProgress(source.getPokemonLevel(), source.getPokemonExperience());
        evolved.setOrderedToSit(source.isOrderedToSit());
        evolved.setTarget(null);
        evolved.getNavigation().stop();
        evolved.setHealth(Math.max(1.0F, Math.min(evolved.getMaxHealth(), evolved.getMaxHealth() * healthRatio)));
        if (!level.addFreshEntity(evolved)) return null;

        source.notifyPokecEvolution(targetSlug);
        source.discard();
        return evolved;
    }
}
