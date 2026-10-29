package br.com.petrickfaria.minemon.menu;

import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.entity.PokemonEvolution;
import br.com.petrickfaria.minemon.network.MinemonNetwork;
import br.com.petrickfaria.minemon.pokec.PokemonEvolutionRules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Owner-only, server-authoritative status and command menu for one companion. */
public final class PokemonInfoMenu extends AbstractContainerMenu {
    private final MinemonCreature pokemon;

    public PokemonInfoMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, inventory.player.level.getEntity(data.readVarInt()) instanceof MinemonCreature found ? found : null);
    }

    public PokemonInfoMenu(int id, Inventory inventory, MinemonCreature pokemon) {
        super(ModMenus.POKEMON_INFO.get(), id);
        this.pokemon = pokemon;
    }

    public MinemonCreature pokemon() { return pokemon; }
    public java.util.List<br.com.petrickfaria.minemon.pokec.PokecSpecies.Entry> evolutionOptions() {
        return pokemon == null ? java.util.List.of() : PokemonEvolutionRules.nextForms(pokemon.getSpeciesSlug());
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (Boolean.getBoolean("minemon.clientSmokeTest")) {
            System.out.println("[POKEMON MENU CLICK] side=" + (player.level.isClientSide ? "client" : "server")
                    + " button=" + button + " pokemon=" + (pokemon == null ? "null" : pokemon.getId())
                    + " sit=" + (pokemon != null && pokemon.isOrderedToSit())
                    + " valid=" + (pokemon != null && stillValid(player))
                    + " player=" + player.getUUID());
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !stillValid(player) || pokemon == null) return false;
        if (button == 0) {
            pokemon.setOrderedToSit(false);
            pokemon.setTarget(null);
            pokemon.getNavigation().stop();
            return true;
        }
        if (button == 1) {
            pokemon.setOrderedToSit(true);
            pokemon.setTarget(null);
            pokemon.getNavigation().stop();
            if (Boolean.getBoolean("minemon.clientSmokeTest"))
                System.out.println("[POKEMON MENU CLICK] server sit-after=true pokemon=" + pokemon.getId());
            return true;
        }
        if (button == 2) {
            MinemonNetwork.openBlackList(serverPlayer, pokemon);
            return true;
        }
        if (button >= 3) {
            int option = button - 3;
            var options = evolutionOptions();
            if (option < 0 || option >= options.size()
                    || pokemon.getPokemonLevel() < PokemonEvolutionRules.requiredLevel(pokemon.getSpeciesSlug())) return false;
            MinemonCreature evolved = PokemonEvolution.evolve(serverPlayer, pokemon, options.get(option).slug());
            if (evolved == null) return false;
            PokemonMenus.openInfo(serverPlayer, evolved);
            return true;
        }
        return false;
    }

    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) {
        return pokemon != null && pokemon.isAlive() && !pokemon.isRemoved() && pokemon.isTame()
                && pokemon.isOwnedBy(player) && pokemon.level == player.level && player.distanceToSqr(pokemon) <= 64.0D;
    }
}
