package br.com.petrickfaria.minemon.network;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.menu.PokecMenu;
import br.com.petrickfaria.minemon.menu.PokemonInfoMenu;
import br.com.petrickfaria.minemon.pokec.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.core.BlockPos;

import java.util.*;
import java.util.function.Supplier;

/** Sends only a playback cue; user audio remains local to each client. */
public final class MinemonNetwork {
    private static final String VERSION = "4";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Minemon.MOD_ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        CHANNEL.messageBuilder(PlayCreatureSound.class, 0)
                .encoder(PlayCreatureSound::encode).decoder(PlayCreatureSound::decode)
                .consumerMainThread(PlayCreatureSound::handle).add();
        CHANNEL.messageBuilder(PokecRequest.class,1).encoder(PokecRequest::encode).decoder(PokecRequest::decode).consumerMainThread(PokecRequest::handle).add();
        CHANNEL.messageBuilder(PokecAction.class,2).encoder(PokecAction::encode).decoder(PokecAction::decode).consumerMainThread(PokecAction::handle).add();
        CHANNEL.messageBuilder(PokecState.class,3).encoder(PokecState::encode).decoder(PokecState::decode).consumerMainThread(PokecState::handle).add();
        CHANNEL.messageBuilder(BlackListAction.class,4).encoder(BlackListAction::encode).decoder(BlackListAction::decode).consumerMainThread(BlackListAction::handle).add();
        CHANNEL.messageBuilder(BlackListState.class,5).encoder(BlackListState::encode).decoder(BlackListState::decode).consumerMainThread(BlackListState::handle).add();
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
        PokecMenu menu=(PokecMenu)player.containerMenu;
        var preview = menu.conversionPreview();
        CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new PokecState(PokecPlayerData.coins(player),free,unlocked,data,prices,requirements,menu.selectedSlug(),menu.stagedCoins(),menu.stagedSouls(),menu.conversionBall(),menu.conversionOutput(),menu.conversionResult(), preview == null ? "" : preview.species(), preview == null ? "" : preview.rarity(), preview == null ? "" : preview.stage().name(), preview == null || preview.soulType() == null ? "" : preview.soulType().name(), preview == null ? 0 : preview.level(), preview == null ? 0 : preview.base(), preview == null ? 0 : preview.minimum(), preview == null ? 0 : preview.maximum(), menu.isConversionConfirming(player.tickCount)));
    }
    public static void refreshPokec(ServerPlayer player){if(player.containerMenu instanceof PokecMenu)syncPokec(player);}

    public static void openBlackList(ServerPlayer player, MinemonCreature pokemon) {
        if (player == null || pokemon == null || !pokemon.isOwnedBy(player) || !pokemon.isAlive()) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new BlackListState(
                pokemon.getId(), pokemon.getUUID(), pokemon.getBlackListOverrides(), true));
    }
    public static void setBlackListTarget(int entityId, UUID uuid, ResourceLocation type, boolean selected) {
        CHANNEL.sendToServer(new BlackListAction(entityId, uuid, type, selected));
    }
    private static boolean validPokemonMenu(ServerPlayer player, int entityId, UUID uuid) {
        if (!(player.containerMenu instanceof PokemonInfoMenu menu) || !menu.stillValid(player)) return false;
        MinemonCreature pokemon = menu.pokemon();
        return pokemon != null && pokemon.getId() == entityId && pokemon.getUUID().equals(uuid)
                && pokemon.isOwnedBy(player);
    }
    private static void syncBlackList(ServerPlayer player, MinemonCreature pokemon, boolean open) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new BlackListState(
                pokemon.getId(), pokemon.getUUID(), pokemon.getBlackListOverrides(), open));
    }

    private record BlackListAction(int entityId, UUID pokemon, ResourceLocation type, boolean selected) {
        static void encode(BlackListAction m, FriendlyByteBuf b) { b.writeVarInt(m.entityId); b.writeUUID(m.pokemon); b.writeResourceLocation(m.type); b.writeBoolean(m.selected); }
        static BlackListAction decode(FriendlyByteBuf b) { return new BlackListAction(b.readVarInt(), b.readUUID(), b.readResourceLocation(), b.readBoolean()); }
        static void handle(BlackListAction m, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get(); ServerPlayer player = context.getSender();
            context.enqueueWork(() -> {
                if (player == null || !validPokemonMenu(player, m.entityId, m.pokemon)) return;
                MinemonCreature pokemon = ((PokemonInfoMenu) player.containerMenu).pokemon();
                var type = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(m.type);
                if (type == null || type.getCategory() == net.minecraft.world.entity.MobCategory.MISC) return;
                pokemon.setBlackListOverride(m.type, m.selected);
                syncBlackList(player, pokemon, false);
            });
            context.setPacketHandled(true);
        }
    }

    private record BlackListState(int entityId, UUID pokemon, Map<String, Boolean> overrides, boolean open) {
        static void encode(BlackListState m, FriendlyByteBuf b) {
            b.writeVarInt(m.entityId); b.writeUUID(m.pokemon);
            int size = Math.min(4096, m.overrides.size()); b.writeVarInt(size);
            int written = 0;
            for (var entry : new TreeMap<>(m.overrides).entrySet()) {
                if (written++ >= size) break;
                b.writeUtf(entry.getKey(), 256); b.writeBoolean(entry.getValue());
            }
            b.writeBoolean(m.open);
        }
        static BlackListState decode(FriendlyByteBuf b) {
            int entityId = b.readVarInt(); UUID pokemon = b.readUUID(); int size = Math.min(4096, b.readVarInt());
            Map<String, Boolean> state = new TreeMap<>();
            for (int i = 0; i < size; i++) state.put(b.readUtf(256), b.readBoolean());
            return new BlackListState(entityId, pokemon, state, b.readBoolean());
        }
        static void handle(BlackListState m, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
                        var minecraft = net.minecraft.client.Minecraft.getInstance();
                        if (m.open && minecraft.screen instanceof br.com.petrickfaria.minemon.menu.PokemonInfoScreen info)
                            info.openBlackList(m.entityId, m.pokemon, m.overrides);
                        else if (minecraft.screen instanceof br.com.petrickfaria.minemon.menu.BlackListScreen list)
                            list.replaceOverrides(m.overrides);
                    }));
            context.setPacketHandled(true);
        }
    }

    private record PokecRequest(BlockPos pos){
        static void encode(PokecRequest m,FriendlyByteBuf b){b.writeBlockPos(m.pos);}static PokecRequest decode(FriendlyByteBuf b){return new PokecRequest(b.readBlockPos());}
        static void handle(PokecRequest m,Supplier<NetworkEvent.Context> s){var c=s.get();ServerPlayer p=c.getSender();c.enqueueWork(()->{if(p!=null&&p.containerMenu instanceof PokecMenu menu&&menu.pos().equals(m.pos)&&menu.stillValid(p))syncPokec(p);});c.setPacketHandled(true);}
    }
    private record PokecAction(BlockPos pos,String slug,int action,long id){
        static void encode(PokecAction m,FriendlyByteBuf b){b.writeBlockPos(m.pos);b.writeUtf(m.slug,64);b.writeByte(m.action);b.writeLong(m.id);}
        static PokecAction decode(FriendlyByteBuf b){return new PokecAction(b.readBlockPos(),b.readUtf(64),b.readUnsignedByte(),b.readLong());}
        static void handle(PokecAction m,Supplier<NetworkEvent.Context> s){var c=s.get();ServerPlayer p=c.getSender();c.enqueueWork(()->{if(p==null||m.action<0||m.action>=PokecTransactions.Action.values().length)return;PokecTransactions.perform(p,m.pos,m.slug,PokecTransactions.Action.values()[m.action],m.id);if(p.containerMenu instanceof PokecMenu menu&&menu.pos().equals(m.pos))syncPokec(p);});c.setPacketHandled(true);}
    }
    private record PokecState(int coins,boolean freeAvailable,Set<String> unlocked,Map<String,Integer> deposits,Map<String,Integer> prices,Map<String,Integer> requirements,String selectedSlug,int stagedCoins,int stagedSouls,net.minecraft.world.item.ItemStack conversionBall,net.minecraft.world.item.ItemStack conversionOutput,int conversionResult,String conversionSpecies,String conversionRarity,String conversionStage,String conversionType,int conversionLevel,int conversionBase,int conversionMinimum,int conversionMaximum,boolean conversionConfirming){
        static void encode(PokecState m,FriendlyByteBuf b){b.writeVarInt(m.coins);b.writeBoolean(m.freeAvailable);b.writeVarInt(m.deposits.size());m.deposits.forEach((slug,count)->{b.writeUtf(slug,64);b.writeBoolean(m.unlocked.contains(slug));b.writeVarInt(count);b.writeVarInt(m.prices.getOrDefault(slug,0));b.writeVarInt(m.requirements.getOrDefault(slug,0));});b.writeUtf(m.selectedSlug,64);b.writeVarInt(m.stagedCoins);b.writeVarInt(m.stagedSouls);b.writeItem(m.conversionBall);b.writeItem(m.conversionOutput);b.writeInt(m.conversionResult);b.writeUtf(m.conversionSpecies,64);b.writeUtf(m.conversionRarity,32);b.writeUtf(m.conversionStage,32);b.writeUtf(m.conversionType,32);b.writeVarInt(m.conversionLevel);b.writeVarInt(m.conversionBase);b.writeVarInt(m.conversionMinimum);b.writeVarInt(m.conversionMaximum);b.writeBoolean(m.conversionConfirming);}
        static PokecState decode(FriendlyByteBuf b){int coins=b.readVarInt();boolean free=b.readBoolean();int n=Math.min(151,b.readVarInt());Set<String> unlocked=new HashSet<>();Map<String,Integer> deposits=new LinkedHashMap<>(),prices=new LinkedHashMap<>(),requirements=new LinkedHashMap<>();for(int i=0;i<n;i++){String slug=b.readUtf(64);if(b.readBoolean())unlocked.add(slug);deposits.put(slug,b.readVarInt());prices.put(slug,b.readVarInt());requirements.put(slug,b.readVarInt());}return new PokecState(coins,free,unlocked,deposits,prices,requirements,b.readUtf(64),b.readVarInt(),b.readVarInt(),b.readItem(),b.readItem(),b.readInt(),b.readUtf(64),b.readUtf(32),b.readUtf(32),b.readUtf(32),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readBoolean());}
        static void handle(PokecState m,Supplier<NetworkEvent.Context> s){var c=s.get();c.enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->br.com.petrickfaria.minemon.menu.PokecScreen.updateState(m.coins,m.freeAvailable,m.unlocked,m.deposits,m.prices,m.requirements,m.selectedSlug,m.stagedCoins,m.stagedSouls,m.conversionBall,m.conversionOutput,m.conversionResult,m.conversionSpecies,m.conversionRarity,m.conversionStage,m.conversionType,m.conversionLevel,m.conversionBase,m.conversionMinimum,m.conversionMaximum,m.conversionConfirming)));c.setPacketHandled(true);}
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
