package br.com.petrickfaria.minemon.registry;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Minemon.MOD_ID);

    // Initial representative entry. The registry is structured so species can be added from generated data later.
    public static final RegistryObject<EntityType<MinemonCreature>> BULBASAUR = ENTITIES.register("bulbasaur",
            () -> EntityType.Builder.<MinemonCreature>of(MinemonCreature::new, MobCategory.CREATURE)
                    .sized(0.8F, 0.8F).clientTrackingRange(10).build("minemon:bulbasaur"));

    @Mod.EventBusSubscriber(modid = Minemon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Attributes {
        @SubscribeEvent
        public static void register(EntityAttributeCreationEvent event) {
            event.put(BULBASAUR.get(), MinemonCreature.createAttributes().build());
        }
    }

    private ModEntities() {}
}
