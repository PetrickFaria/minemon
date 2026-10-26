package br.com.petrickfaria.minemon.network;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;
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
    }

    public static void playForTracking(MinemonCreature creature) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> creature),
                new PlayCreatureSound(creature.getId(), creature.getUUID()));
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
