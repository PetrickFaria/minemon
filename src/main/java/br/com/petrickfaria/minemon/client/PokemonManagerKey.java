package br.com.petrickfaria.minemon.client;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.network.MinemonNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=Minemon.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class PokemonManagerKey {
    public static final KeyMapping OPEN = new KeyMapping("key.minemon.pokemon_manager", GLFW.GLFW_KEY_P, "key.categories.minemon");
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.register(OPEN); }

    @Mod.EventBusSubscriber(modid=Minemon.MOD_ID,value=Dist.CLIENT)
    public static final class Ticks {
        @SubscribeEvent public static void clientTick(TickEvent.ClientTickEvent event) {
            if(event.phase!=TickEvent.Phase.END || !OPEN.consumeClick())return;
            var minecraft=net.minecraft.client.Minecraft.getInstance();
            if(minecraft.player!=null && minecraft.level!=null && minecraft.screen==null)MinemonNetwork.requestPokemonManagement();
        }
    }
    private PokemonManagerKey() { }
}
