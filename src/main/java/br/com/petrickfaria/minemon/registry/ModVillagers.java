package br.com.petrickfaria.minemon.registry;

import br.com.petrickfaria.minemon.Minemon;
import com.google.common.collect.ImmutableSet;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Vanilla villager job-site and profession registries for Minemon. */
public final class ModVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, Minemon.MOD_ID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, Minemon.MOD_ID);

    public static final RegistryObject<PoiType> ROCKET_POI = POI_TYPES.register("rocket", () ->
            new PoiType(ImmutableSet.copyOf(ModBlocks.ROCKET_BLOCK.get().getStateDefinition().getPossibleStates()), 1, 1));

    public static final RegistryObject<VillagerProfession> ROCKET = PROFESSIONS.register("rocket", () ->
            new VillagerProfession("rocket",
                    poi -> poi.is(ROCKET_POI.getKey()),
                    poi -> poi.is(ROCKET_POI.getKey()),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_CARTOGRAPHER));

    public static void register(IEventBus modEventBus) {
        POI_TYPES.register(modEventBus);
        PROFESSIONS.register(modEventBus);
    }

    private ModVillagers() {}
}
