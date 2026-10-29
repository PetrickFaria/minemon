package br.com.petrickfaria.minemon.entity;

import br.com.petrickfaria.minemon.registry.ModBlockEntities;
import br.com.petrickfaria.minemon.registry.ModBlocks;
import br.com.petrickfaria.minemon.registry.ModEntities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Stationary, saved incubation timer. The hatchling is created directly as an adult. */
public final class BreedingEggBlockEntity extends BlockEntity {
    private String species = "";
    private UUID owner;
    private int remainingTicks = 24_000;
    private boolean hatched;

    public BreedingEggBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BREEDING_EGG.get(), pos, state);
    }
    public void initialize(String species, UUID owner, int incubationTicks) {
        this.species = species;
        this.owner = owner;
        this.remainingTicks = Math.max(1, incubationTicks);
        this.hatched = false;
        setChanged();
    }
    public String species() { return species; }
    public UUID owner() { return owner; }
    public int remainingTicks() { return remainingTicks; }
    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, BreedingEggBlockEntity egg) {
        if (!(level instanceof ServerLevel server) || egg.hatched || egg.species.isBlank()) return;
        if (egg.remainingTicks > 0) {
            egg.remainingTicks--;
            if (egg.remainingTicks % 100 == 0) egg.setChanged();
            return;
        }
        egg.tryHatch(server, pos);
    }
    private void tryHatch(ServerLevel level, BlockPos pos) {
        var registryType = ModEntities.SPECIES.get(species);
        if (registryType == null || owner == null) return;
        MinemonCreature pokemon = registryType.get().create(level);
        if (pokemon == null) return;
        BlockPos safe = safePosition(level, pokemon, pos);
        if (safe == null) { pokemon.discard(); remainingTicks = 20; setChanged(); return; }

        hatched = true;
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        pokemon.moveTo(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D, 0, 0);
        pokemon.setAge(0);
        pokemon.setPokemonLevel(1);
        pokemon.setOwnerUUID(owner);
        pokemon.setTame(true);
        pokemon.setOrderedToSit(false);
        if (!level.addFreshEntity(pokemon)) {
            pokemon.discard();
            level.setBlock(pos, ModBlocks.BREEDING_EGG.get().defaultBlockState(), 3);
            BlockEntity restored = level.getBlockEntity(pos);
            if (restored instanceof BreedingEggBlockEntity replacement) replacement.initialize(species, owner, 20);
        }
    }
    private static BlockPos safePosition(ServerLevel level, Entity entity, BlockPos origin) {
        for (int radius = 0; radius <= 2; radius++)
            for (int dx = -radius; dx <= radius; dx++)
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    BlockPos feet = origin.offset(dx, 0, dz);
                    BlockPos floor = feet.below();
                    if (!level.getWorldBorder().isWithinBounds(feet)
                            || !level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                            || level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()
                            || !level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) continue;
                    double x = feet.getX() + 0.5D, y = feet.getY(), z = feet.getZ() + 0.5D;
                    AABB box = entity.getBoundingBox().move(x - entity.getX(), y - entity.getY(), z - entity.getZ());
                    if (level.noCollision(entity, box)) return feet;
                }
        return null;
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Species", species);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putInt("RemainingTicks", remainingTicks);
        tag.putBoolean("Hatched", hatched);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        species = tag.getString("Species");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        remainingTicks = Math.max(0, tag.getInt("RemainingTicks"));
        hatched = tag.getBoolean("Hatched");
    }
}
