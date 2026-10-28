package br.com.petrickfaria.minemon;

import br.com.petrickfaria.minemon.config.MinemonConfig;
import br.com.petrickfaria.minemon.registry.ModEntities;
import br.com.petrickfaria.minemon.registry.ModItems;
import br.com.petrickfaria.minemon.registry.ModParticles;
import br.com.petrickfaria.minemon.registry.ModBlocks;
import br.com.petrickfaria.minemon.registry.ModVillagers;
import br.com.petrickfaria.minemon.menu.ModMenus;
import br.com.petrickfaria.minemon.sound.CustomSoundFolders;
import br.com.petrickfaria.minemon.network.MinemonNetwork;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import software.bernie.geckolib3.GeckoLib;

@Mod(Minemon.MOD_ID)
public final class Minemon {
    public static final String MOD_ID = "minemon_gen1";

    public Minemon() {
        // Registers GeckoLib's resource reload listener and model cache. Without this,
        // AnimatedGeoModel can resolve the resource location but finds no parsed geo model.
        GeckoLib.initialize();
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITIES.register(bus);
        ModParticles.PARTICLE_TYPES.register(bus);
        ModBlocks.BLOCKS.register(bus);
        ModVillagers.register(bus);
        ModItems.ITEMS.register(bus);
        ModItems.TREAT_ITEMS.register(bus);
        ModMenus.MENUS.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MinemonConfig.SPEC);
        MinemonNetwork.register();
        CustomSoundFolders.createAll();
    }
}
