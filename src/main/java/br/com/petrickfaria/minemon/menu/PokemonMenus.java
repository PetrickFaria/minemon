package br.com.petrickfaria.minemon.menu;

import br.com.petrickfaria.minemon.entity.MinemonCreature;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkHooks;

public final class PokemonMenus {
    private PokemonMenus() {}

    public static void openInfo(ServerPlayer player, MinemonCreature pokemon) {
        if (player == null || pokemon == null || !pokemon.isTame() || !pokemon.isOwnedBy(player)) return;
        NetworkHooks.openScreen(player,
                new net.minecraft.world.SimpleMenuProvider((id, inventory, owner) ->
                        new PokemonInfoMenu(id, inventory, pokemon), Component.literal("Pokémon")),
                data -> data.writeVarInt(pokemon.getId()));
    }
}
