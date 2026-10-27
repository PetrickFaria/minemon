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
    public static void registerScreens(net.minecraftforge.client.event.RegisterMenuScreensEvent event){
        event.register(ModMenus.POKEC.get(),PokecScreen::new);
    }

    private ClientEvents() {}
}
