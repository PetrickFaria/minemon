package br.com.petrickfaria.minemon.pokec;

import br.com.petrickfaria.minemon.Minemon;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Minemon.MOD_ID)
public final class PokecEvents {
    @SubscribeEvent public static void lootDownedOnAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
                || !(event.getTarget() instanceof MinemonCreature wild)
                || !WildEncounterManager.isDowned(wild)) return;
        WildEncounterManager.loot(wild,player);
        event.setCanceled(true);
    }

    @SubscribeEvent public static void playerPokemonAttack(LivingAttackEvent event) {
        if (event.getEntity().level.isClientSide
                || !(event.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
                || event.getSource().getDirectEntity() != player) return;
        if (event.getEntity() instanceof MinemonCreature target && target.isRocketSpecial()) return;
        if (event.getEntity() instanceof MinemonCreature wild && WildEncounterManager.isDowned(wild)) {
            WildEncounterManager.loot(wild, player);
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof MinemonCreature wild && (wild.isDefeated() || !wild.isAlive())) {
            event.setCanceled(true);
            return;
        }
        var area=event.getEntity().getBoundingBox().inflate(16.0D);
        for (MinemonCreature companion : event.getEntity().level.getEntitiesOfClass(MinemonCreature.class, area,
                pokemon -> pokemon.isTame() && player.getUUID().equals(pokemon.getOwnerUUID())
                        && !pokemon.isDefeated() && pokemon.isAlive())) {
            companion.focusPlayerTarget(event.getEntity());
        }
        if (!(event.getEntity() instanceof MinemonCreature wild)) return;
        // Owned Pokémon remain protected. MinemonCreature caps direct player melee
        // before delegating to Forge's damage pipeline, regardless of weapon modifiers.
        if (wild.isTame()) { event.setCanceled(true); return; }
        // Preserve the active-encounter one-HP floor; only its owned companion can
        // put it into the capture-ready downed state.
        if (br.com.petrickfaria.minemon.pokec.WildEncounterManager.isEncounter(wild)&&wild.getHealth()<=1.0F)
            event.setCanceled(true);
    }
    @SubscribeEvent public static void serverStarting(ServerStartingEvent event){
        PokecEconomy.load();
        if (event.getServer().overworld() != null) WildEncounterManager.loadConfig(event.getServer().overworld());
    }
    @SubscribeEvent public static void wildEncounters(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof net.minecraft.server.level.ServerPlayer player)
            WildEncounterManager.tickPlayer(player);
    }
    @SubscribeEvent public static void trackActivePokemon(EntityJoinLevelEvent event) {
        if (event.getEntity().level.isClientSide || !(event.getEntity() instanceof MinemonCreature pokemon)) return;
        if (WildEncounterManager.isEncounter(pokemon)) WildEncounterManager.trackLoaded(pokemon);
        else ActivePokemonData.register(pokemon);
    }
    @SubscribeEvent public static void untrackRemovedPokemon(EntityLeaveLevelEvent event) {
        if (event.getEntity().level.isClientSide || !(event.getEntity() instanceof MinemonCreature)) return;
        var reason = event.getEntity().getRemovalReason();
        if (reason == net.minecraft.world.entity.Entity.RemovalReason.DISCARDED
                || reason == net.minecraft.world.entity.Entity.RemovalReason.KILLED)
            if (WildEncounterManager.isEncounter((MinemonCreature) event.getEntity()))
                WildEncounterManager.untrackRemoved((MinemonCreature) event.getEntity());
            else ActivePokemonData.unregister(event.getEntity());
    }
    @SubscribeEvent public static void companionExperience(LivingDeathEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.Mob defeated)
            WildEncounterManager.awardCompanionExperience(defeated, event.getSource());
    }
    @SubscribeEvent public static void pokemonSoulDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof MinemonCreature pokemon) || event.getEntity().level.isClientSide) return;
        // Wild Pokémon can reward souls; a domesticated Pokémon's protection and ownership rules remain intact.
        if (pokemon.isTame()) return;
        var marker = pokemon.getPersistentData();
        if (marker.getBoolean("MinemonSoulDropHandled")) return;
        marker.putBoolean("MinemonSoulDropHandled", true);
        var preview = PokemonSoulConversion.preview(pokemon.getSpeciesSlug(), pokemon.getPokemonLevel());
        if (preview == null || preview.soulType() == null) return;
        int count = PokemonSoulConversion.rollDeathDrops(pokemon.getSpeciesSlug(), pokemon.getPokemonLevel(), pokemon.getRandom()::nextDouble);
        if (count <= 0) return;
        var soul = ModItems.SOULS.get(preview.soulType().id());
        if (soul != null) for(int remaining=count;remaining>0;remaining-=Math.min(remaining,soul.get().getMaxStackSize())) {
            int stackSize=Math.min(remaining,soul.get().getMaxStackSize());
            event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(pokemon.level,pokemon.getX(),pokemon.getY(),pokemon.getZ(),new ItemStack(soul.get(),stackSize)));
        }
    }
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event){PokecPlayerData.copyOnRespawn(event.getOriginal(),event.getEntity());if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)PokecTransactions.recoverEscrow(player);}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player){PokecTransactions.recoverEscrow(player);TrainerXpLedger.deliver(player);br.com.petrickfaria.minemon.network.MinemonNetwork.syncTrainer(player);}}
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
