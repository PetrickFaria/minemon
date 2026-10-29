package br.com.petrickfaria.minemon.registry;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.BreedingEggBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Minemon.MOD_ID);
    public static final RegistryObject<BlockEntityType<BreedingEggBlockEntity>> BREEDING_EGG =
            TYPES.register("breeding_egg", () -> BlockEntityType.Builder.of(BreedingEggBlockEntity::new,
                    ModBlocks.BREEDING_EGG.get()).build(null));
    private ModBlockEntities() {}
}
