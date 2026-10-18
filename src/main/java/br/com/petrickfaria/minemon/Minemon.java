package br.com.petrickfaria.minemon;

import br.com.petrickfaria.minemon.config.MinemonConfig;
import br.com.petrickfaria.minemon.registry.ModEntities;
import br.com.petrickfaria.minemon.registry.ModItems;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Minemon.MOD_ID)
public final class Minemon {
    public static final String MOD_ID = "minemon";

    public Minemon() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MinemonConfig.SPEC);
    }
}
