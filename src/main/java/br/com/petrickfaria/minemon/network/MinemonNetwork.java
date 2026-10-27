package br.com.petrickfaria.minemon.network;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.menu.PokecMenu;
import br.com.petrickfaria.minemon.pokec.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.function.Supplier;

/** Sends only a playback cue; user audio remains local to each client. */
public final class MinemonNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Minemon.MOD_ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        CHANNEL.messageBuilder(PlayCreatureSound.class, 0)
                .encoder(PlayCreatureSound::encode).decoder(PlayCreatureSound::decode)
                .consumerMainThread(PlayCreatureSound::handle).add();
        CHANNEL.messageBuilder(PokecRequest.class,1).encoder(PokecRequest::encode).decoder(PokecRequest::decode).consumerMainThread(PokecRequest::handle).add();
        CHANNEL.messageBuilder(PokecAction.class,2).encoder(PokecAction::encode).decoder(PokecAction::decode).consumerMainThread(PokecAction::handle).add();
        CHANNEL.messageBuilder(PokecState.class,3).encoder(PokecState::encode).decoder(PokecState::decode).consumerMainThread(PokecState::handle).add();
    }

    public static void playForTracking(MinemonCreature creature) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> creature),
                new PlayCreatureSound(creature.getId(), creature.getUUID()));
    }

    public static void requestPokecState(BlockPos pos){CHANNEL.sendToServer(new PokecRequest(pos));}
    public static void performPokecAction(BlockPos pos,String slug,PokecTransactions.Action action){CHANNEL.sendToServer(new PokecAction(pos,slug,action.ordinal(),java.util.concurrent.ThreadLocalRandom.current().nextLong()));}
    private static void syncPokec(ServerPlayer player){
        var unlocked=PokecPlayerData.unlocked(player);var data=new LinkedHashMap<String,Integer>();var prices=new LinkedHashMap<String,Integer>();var requirements=new LinkedHashMap<String,Integer>();
        for(var species:PokecSpecies.ordered()){data.put(species.slug(),PokecPlayerData.deposited(player,species.slug()));prices.put(species.slug(),PokecEconomy.get().coins(species.slug()));requirements.put(species.slug(),PokecEconomy.get().souls(species.slug()));}
        boolean free=PokecEconomy.get().firstPurchaseFree()&&!PokecPlayerData.usedFreePurchase(player);
        CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new PokecState(PokecPlayerData.coins(player),free,unlocked,data,prices,requirements));
    }
    public static void refreshPokec(ServerPlayer player){if(player.containerMenu instanceof PokecMenu)syncPokec(player);}

    private record PokecRequest(BlockPos pos){
        static void encode(PokecRequest m,FriendlyByteBuf b){b.writeBlockPos(m.pos);}static PokecRequest decode(FriendlyByteBuf b){return new PokecRequest(b.readBlockPos());}
        static void handle(PokecRequest m,Supplier<NetworkEvent.Context> s){var c=s.get();ServerPlayer p=c.getSender();c.enqueueWork(()->{if(p!=null&&p.containerMenu instanceof PokecMenu menu&&menu.pos().equals(m.pos)&&menu.stillValid(p))syncPokec(p);});c.setPacketHandled(true);}
    }
    private record PokecAction(BlockPos pos,String slug,int action,long id){
        static void encode(PokecAction m,FriendlyByteBuf b){b.writeBlockPos(m.pos);b.writeUtf(m.slug,64);b.writeByte(m.action);b.writeLong(m.id);}
        static PokecAction decode(FriendlyByteBuf b){return new PokecAction(b.readBlockPos(),b.readUtf(64),b.readUnsignedByte(),b.readLong());}
        static void handle(PokecAction m,Supplier<NetworkEvent.Context> s){var c=s.get();ServerPlayer p=c.getSender();c.enqueueWork(()->{if(p==null||m.action<0||m.action>=PokecTransactions.Action.values().length)return;PokecTransactions.perform(p,m.pos,m.slug,PokecTransactions.Action.values()[m.action],m.id);if(p.containerMenu instanceof PokecMenu menu&&menu.pos().equals(m.pos))syncPokec(p);});c.setPacketHandled(true);}
    }
    private record PokecState(int coins,boolean freeAvailable,Set<String> unlocked,Map<String,Integer> deposits,Map<String,Integer> prices,Map<String,Integer> requirements){
        static void encode(PokecState m,FriendlyByteBuf b){b.writeVarInt(m.coins);b.writeBoolean(m.freeAvailable);b.writeVarInt(m.deposits.size());m.deposits.forEach((slug,count)->{b.writeUtf(slug,64);b.writeBoolean(m.unlocked.contains(slug));b.writeVarInt(count);b.writeVarInt(m.prices.getOrDefault(slug,0));b.writeVarInt(m.requirements.getOrDefault(slug,0));});}
        static PokecState decode(FriendlyByteBuf b){int coins=b.readVarInt();boolean free=b.readBoolean();int n=Math.min(151,b.readVarInt());Set<String> unlocked=new HashSet<>();Map<String,Integer> deposits=new LinkedHashMap<>(),prices=new LinkedHashMap<>(),requirements=new LinkedHashMap<>();for(int i=0;i<n;i++){String slug=b.readUtf(64);if(b.readBoolean())unlocked.add(slug);deposits.put(slug,b.readVarInt());prices.put(slug,b.readVarInt());requirements.put(slug,b.readVarInt());}return new PokecState(coins,free,unlocked,deposits,prices,requirements);}
        static void handle(PokecState m,Supplier<NetworkEvent.Context> s){var c=s.get();c.enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->br.com.petrickfaria.minemon.menu.PokecScreen.updateState(m.coins,m.freeAvailable,m.unlocked,m.deposits,m.prices,m.requirements)));c.setPacketHandled(true);}
    }

    public record PlayCreatureSound(int entityId, UUID uuid) {
        private static void encode(PlayCreatureSound message, FriendlyByteBuf buffer) {
            buffer.writeVarInt(message.entityId);
            buffer.writeUUID(message.uuid);
        }

        private static PlayCreatureSound decode(FriendlyByteBuf buffer) {
            return new PlayCreatureSound(buffer.readVarInt(), buffer.readUUID());
        }

        private static void handle(PlayCreatureSound message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
                    net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                            () -> () -> br.com.petrickfaria.minemon.client.CustomCreatureAudio.play(message.entityId, message.uuid));
                }
            });
            context.setPacketHandled(true);
        }
    }

    private MinemonNetwork() {}
}
