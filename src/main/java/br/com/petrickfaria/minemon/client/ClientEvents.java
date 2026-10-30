package br.com.petrickfaria.minemon.client;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.registry.ModEntities;
import br.com.petrickfaria.minemon.registry.ModParticles;
import br.com.petrickfaria.minemon.menu.ModMenus;
import br.com.petrickfaria.minemon.menu.PokecScreen;
import br.com.petrickfaria.minemon.menu.RocketTradeScreen;
import br.com.petrickfaria.minemon.menu.PokemonInfoScreen;
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
        event.registerEntityRenderer(ModEntities.ELEMENTAL_PROJECTILE.get(),ElementalProjectileRenderer::new);
        for (var type : ModEntities.SPECIES.values()) event.registerEntityRenderer(type.get(), MinemonRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.register(ModParticles.GASTLY_GHOSTLY_SMOKE.get(), GastlySmokeParticle.Provider::new);
        event.register(ModParticles.GASTLY_SMALL_SMOKE.get(),sprites->new GastlySmokeParticle.Provider(sprites,true));
    }

    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event){
        event.enqueueWork(() -> {
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(br.com.petrickfaria.minemon.registry.ModBlocks.BREEDING_EGG.get(),net.minecraft.client.renderer.RenderType.cutout());
            MenuScreens.register(ModMenus.POKEC.get(), PokecScreen::new);
            MenuScreens.register(ModMenus.ROCKET_TRADE.get(), RocketTradeScreen::new);
            MenuScreens.register(ModMenus.POKEMON_INFO.get(), PokemonInfoScreen::new);
        });
    }

    @SubscribeEvent public static void eggBlockColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Block event){
        event.register((state,world,pos,tint)->{
            if(tint!=0)return 0xFFFFFF;
            String type=world!=null&&pos!=null&&world.getBlockEntity(pos) instanceof br.com.petrickfaria.minemon.entity.BreedingEggBlockEntity egg?egg.elementType():"normal";
            return br.com.petrickfaria.minemon.entity.EggAppearance.color(type);
        },br.com.petrickfaria.minemon.registry.ModBlocks.BREEDING_EGG.get());
    }
    @SubscribeEvent public static void eggItemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event){
        event.register((stack,tint)->tint==0?br.com.petrickfaria.minemon.entity.EggAppearance.color(stack.getTagElement("BlockEntityTag")==null?"normal":stack.getTagElement("BlockEntityTag").getString("EggElement")):0xFFFFFF,
                br.com.petrickfaria.minemon.registry.ModItems.BREEDING_EGG.get());
    }
    private ClientEvents() {}
}
