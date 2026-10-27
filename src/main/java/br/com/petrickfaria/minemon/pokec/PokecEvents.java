package br.com.petrickfaria.minemon.pokec;

import br.com.petrickfaria.minemon.Minemon;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Minemon.MOD_ID)
public final class PokecEvents {
    @SubscribeEvent public static void serverStarting(ServerStartingEvent event){PokecEconomy.load();}
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event){PokecPlayerData.copyOnRespawn(event.getOriginal(),event.getEntity());}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){PokecTransactions.clearTransactions(event.getEntity().getUUID());}
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("pokec").requires(source->source.hasPermission(2))
            .then(Commands.literal("coins").then(Commands.argument("player",EntityArgument.player()).then(Commands.argument("amount",IntegerArgumentType.integer(0)).executes(ctx->{
                var target=EntityArgument.getPlayer(ctx,"player");int amount=IntegerArgumentType.getInteger(ctx,"amount");PokecPlayerData.setCoins(target,amount);br.com.petrickfaria.minemon.network.MinemonNetwork.refreshPokec(target);
                ctx.getSource().sendSuccess(Component.translatable("command.minemon_gen1.pokec.coins_set",target.getName(),amount),true);return 1;
            }))))
            .then(Commands.literal("reload").executes(ctx->{PokecEconomy.load();for(var p:ctx.getSource().getServer().getPlayerList().getPlayers())br.com.petrickfaria.minemon.network.MinemonNetwork.refreshPokec(p);ctx.getSource().sendSuccess(Component.translatable("command.minemon_gen1.pokec.reload"),true);return 1;})));
    }
    private PokecEvents(){}
}
