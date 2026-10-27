package br.com.petrickfaria.minemon.client;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.registry.ModEntities;
import br.com.petrickfaria.minemon.registry.ModParticles;
import br.com.petrickfaria.minemon.menu.ModMenus;
import br.com.petrickfaria.minemon.menu.PokecScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.gui.screens.MenuScreens;

@Mod.EventBusSubscriber(modid = Minemon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (var type : ModEntities.SPECIES.values()) event.registerEntityRenderer(type.get(), MinemonRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.register(ModParticles.GASTLY_GHOSTLY_SMOKE.get(), GastlySmokeParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event){
        event.enqueueWork(() -> MenuScreens.register(ModMenus.POKEC.get(), PokecScreen::new));
    }

    private ClientEvents() {}
}
