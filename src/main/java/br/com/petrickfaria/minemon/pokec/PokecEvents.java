package br.com.petrickfaria.minemon.pokec;

import br.com.petrickfaria.minemon.Minemon;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.registry.ModItems;
import br.com.petrickfaria.minemon.config.MinemonConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Minemon.MOD_ID)
public final class PokecEvents {
    @SubscribeEvent public static void serverStarting(ServerStartingEvent event){PokecEconomy.load();}
    @SubscribeEvent public static void companionExperience(LivingDeathEvent event) {
        if (event.getEntity().level.isClientSide || !(event.getSource().getEntity() instanceof MinemonCreature attacker)
                || !attacker.isTame() || !(event.getEntity() instanceof net.minecraft.world.entity.Mob defeated)) return;
        var marker = event.getEntity().getPersistentData();
        if (marker.getBoolean("MinemonCompanionXpHandled")) return;
        marker.putBoolean("MinemonCompanionXpHandled", true);
        attacker.awardPokemonExperience(Math.max(0, defeated.getExperienceReward()));
    }
    @SubscribeEvent public static void pokemonSoulDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof MinemonCreature pokemon) || event.getEntity().level.isClientSide) return;
        var marker = pokemon.getPersistentData();
        if (marker.getBoolean("MinemonSoulDropHandled")) return;
        marker.putBoolean("MinemonSoulDropHandled", true);
        var preview = PokemonSoulConversion.preview(pokemon.getSpeciesSlug(), pokemon.getPokemonLevel());
        if (preview == null || preview.soulType() == null) return;
        int count = PokemonSoulConversion.rollDeathDrops(pokemon.getSpeciesSlug(), pokemon.getPokemonLevel(),
                MinemonConfig.POKEBALL_SOUL_CHANCE_PERCENT.get(), pokemon.getRandom()::nextDouble);
        if (count <= 0) return;
        var soul = ModItems.SOULS.get(preview.soulType().id());
        if (soul != null) { var drop = new net.minecraft.world.entity.item.ItemEntity(pokemon.level, pokemon.getX(), pokemon.getY(), pokemon.getZ(), new ItemStack(soul.get(), count)); event.getDrops().add(drop); }
    }
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event){PokecPlayerData.copyOnRespawn(event.getOriginal(),event.getEntity());if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)PokecTransactions.recoverEscrow(player);}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)PokecTransactions.recoverEscrow(player);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)PokecTransactions.recoverEscrow(player);PokecTransactions.clearTransactions(event.getEntity().getUUID());}
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("pokec").requires(source->source.hasPermission(2))
            .then(Commands.literal("coins").then(Commands.argument("player",EntityArgument.player()).then(Commands.argument("amount",IntegerArgumentType.integer(0)).executes(ctx->{
                var target=EntityArgument.getPlayer(ctx,"player");int amount=IntegerArgumentType.getInteger(ctx,"amount");PokecPlayerData.setCoins(target,amount);br.com.petrickfaria.minemon.network.MinemonNetwork.refreshPokec(target);
                ctx.getSource().sendSuccess(Component.literal("Saldo de "+target.getName().getString()+" definido para "+amount+" PokéCoins."),true);return 1;
            }))))
            .then(Commands.literal("reload").executes(ctx->{PokecEconomy.load();for(var p:ctx.getSource().getServer().getPlayerList().getPlayers())br.com.petrickfaria.minemon.network.MinemonNetwork.refreshPokec(p);ctx.getSource().sendSuccess(Component.literal("Configuração da economia PokéC recarregada."),true);return 1;})));
    }
    private PokecEvents(){}
}
