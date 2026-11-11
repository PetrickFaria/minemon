package br.com.petrickfaria.minemon.client;

import br.com.petrickfaria.minemon.menu.PokemonManagerScreen;
import br.com.petrickfaria.minemon.pokec.ActivePokemonData;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/** Client-only entry point for opening the Pokémon roster screen from a sync packet. */
@OnlyIn(Dist.CLIENT)
public final class PokemonManagementClient {
    private PokemonManagementClient() { }

    public static void open(List<ActivePokemonData.Entry> entries) {
        Minecraft.getInstance().setScreen(new PokemonManagerScreen(entries));
    }
}
