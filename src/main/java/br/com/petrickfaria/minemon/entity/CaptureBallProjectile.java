package br.com.petrickfaria.minemon.entity;

import br.com.petrickfaria.minemon.registry.ModEntities;
import br.com.petrickfaria.minemon.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;
import net.minecraftforge.network.NetworkHooks;

/** Short-lived world visual for a Pokéball throw and its capture shake sequence. */
public final class CaptureBallProjectile extends Entity {
    private static final EntityDataAccessor<Float> TARGET_X = SynchedEntityData.defineId(CaptureBallProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_Y = SynchedEntityData.defineId(CaptureBallProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_Z = SynchedEntityData.defineId(CaptureBallProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> SUCCESS = SynchedEntityData.defineId(CaptureBallProjectile.class, EntityDataSerializers.BOOLEAN);
    private double startX, startY, startZ;
    private UUID pendingWild;
    private UUID pendingOwner;
    private boolean pendingExpired;
    private boolean masterBall;
    private int destinationSlot;

    public CaptureBallProjectile(EntityType<? extends CaptureBallProjectile> type, Level level) { super(type, level); }

    public static boolean launch(ServerLevel level, ServerPlayer player, MinemonCreature wild,
                                 boolean success, boolean expired, boolean masterBall,
                                 net.minecraft.world.InteractionHand hand) {
        CaptureBallProjectile ball = ModEntities.CAPTURE_BALL.get().create(level);
        if (ball == null) return false;
        ball.moveTo(player.getX(), player.getEyeY() - .12D, player.getZ(), player.getYRot(), player.getXRot());
        ball.startX = ball.getX(); ball.startY = ball.getY(); ball.startZ = ball.getZ();
        ball.entityData.set(TARGET_X, (float) wild.getX());
        ball.entityData.set(TARGET_Y, (float) wild.getY());
        ball.entityData.set(TARGET_Z, (float) wild.getZ());
        ball.entityData.set(SUCCESS, success);
        ball.pendingWild=wild.getUUID();ball.pendingOwner=player.getUUID();
        ball.pendingExpired=expired;ball.masterBall=masterBall;
        ball.destinationSlot=hand==net.minecraft.world.InteractionHand.MAIN_HAND?player.getInventory().selected:40;
        ball.setNoGravity(true);
        return level.addFreshEntity(ball);
    }

    @Override protected void defineSynchedData() {
        entityData.define(TARGET_X, 0.0F); entityData.define(TARGET_Y, 0.0F);
        entityData.define(TARGET_Z, 0.0F); entityData.define(SUCCESS, false);
    }

    @Override public void tick() {
        super.tick();
        if (tickCount == 1 && level.isClientSide) { startX = getX(); startY = getY(); startZ = getZ(); }
        Vec3 target = new Vec3(entityData.get(TARGET_X), entityData.get(TARGET_Y) + .35D, entityData.get(TARGET_Z));
        if (tickCount <= 12) {
            double t = tickCount / 12.0D, eased = t * t * (3.0D - 2.0D * t);
            setPos(startX + (target.x - startX) * eased,
                    startY + (target.y - startY) * eased + Math.sin(Math.PI * t) * 1.1D,
                    startZ + (target.z - startZ) * eased);
        } else if (tickCount <= 20) {
            double t = (tickCount - 12) / 8.0D;
            setPos(target.x, target.y + .35D * (1.0D - t), target.z);
        } else {
            setPos(target.x, target.y, target.z);
        }
        if (tickCount <= 12) setRot(getYRot() + 30.0F, getXRot() + 13.0F);
        else if (tickCount >= 20 && tickCount < 56) {
            float wobble = (float)Math.sin((tickCount - 20) * Math.PI / 4.0D) * 28.0F;
            setRot(getYRot(), wobble);
        } else setRot(getYRot(), 0.0F);

        if (!level.isClientSide && level instanceof ServerLevel server) {
            if (tickCount == 1) server.playSound(null, blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, .9F, .8F);
            if (tickCount == 13) {
                server.playSound(null, blockPosition(), SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.PLAYERS, .65F, 1.2F);
                server.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 16, .25D, .35D, .25D, .12D);
                server.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 8, .2D, .3D, .2D, .02D);
            }
            if (tickCount == 24 || tickCount == 36 || tickCount == 48)
                server.playSound(null, blockPosition(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, .65F, 1.35F);
            if (tickCount == 64 && entityData.get(SUCCESS)) {
                server.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, .7F, 1.4F);
                server.sendParticles(ParticleTypes.FIREWORK, getX(), getY() + .25D, getZ(), 12, .3D, .35D, .3D, .12D);
            }
            if (tickCount >= 64) completePending(server);
        }
    }

    private void completePending(ServerLevel server) {
        if (pendingWild == null || pendingOwner == null) { discard(); return; }
        ServerPlayer player=server.getServer().getPlayerList().getPlayer(pendingOwner);
        if(player==null)player=server.getEntitiesOfClass(ServerPlayer.class,
                getBoundingBox().inflate(128.0D),candidate->pendingOwner.equals(candidate.getUUID()))
                .stream().findFirst().orElse(null);
        if (player == null || player.level != server) return; // Keep the persisted transaction until its owner returns.
        Entity target=server.getEntity(pendingWild);
        if (!(target instanceof MinemonCreature wild)) { discard(); return; }
        if (entityData.get(SUCCESS)) {
            if (!br.com.petrickfaria.minemon.pokec.WildEncounterManager.completeCapture(wild,player)) return;
            Item item=masterBall?ModItems.MASTER_BALL.get():ModItems.POKEBALL.get();
            ItemStack occupied=new ItemStack(item);
            try {
                br.com.petrickfaria.minemon.item.PokeballItem.store(occupied,wild,player);
                if(br.com.petrickfaria.minemon.item.PokeballItem.readStoredEntity(occupied)==null)
                    throw new IllegalStateException("Captured Pokémon could not be restored from the Pokébola");
            }
            catch (RuntimeException invalid) {
                br.com.petrickfaria.minemon.pokec.WildEncounterManager.restoreCaptureAfterStorageFailure(wild,player);
                ItemStack returnedBall=new ItemStack(item);
                if(!player.getInventory().add(returnedBall))player.drop(returnedBall,false);
                discard();return;
            }
            br.com.petrickfaria.minemon.pokec.WildEncounterManager.finishCapture(wild,player);
            wild.discard();
            if(destinationSlot>=0&&destinationSlot<36&&player.getInventory().getItem(destinationSlot).isEmpty())
                player.getInventory().setItem(destinationSlot,occupied);
            else if(destinationSlot==40&&player.getOffhandItem().isEmpty())
                player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,occupied);
            else if (!player.getInventory().add(occupied)) player.drop(occupied,false);
        } else {
            br.com.petrickfaria.minemon.pokec.WildEncounterManager.cancelCapturePending(wild,pendingExpired);
        }
        discard();
    }

    public ItemStack renderStack() {
        ItemStack stack = new ItemStack(ModItems.POKEBALL.get());
        stack.getOrCreateTag().putBoolean("MinemonCaptureAnimation", true);
        return stack;
    }
    public boolean captureSucceeded() { return entityData.get(SUCCESS); }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("StartX", startX); tag.putDouble("StartY", startY); tag.putDouble("StartZ", startZ);
        tag.putFloat("TargetX", entityData.get(TARGET_X)); tag.putFloat("TargetY", entityData.get(TARGET_Y));
        tag.putFloat("TargetZ", entityData.get(TARGET_Z)); tag.putBoolean("Success", entityData.get(SUCCESS));
        if(pendingWild!=null)tag.putUUID("PendingWild",pendingWild);if(pendingOwner!=null)tag.putUUID("PendingOwner",pendingOwner);
        tag.putBoolean("PendingExpired",pendingExpired);tag.putBoolean("MasterBall",masterBall);tag.putInt("DestinationSlot",destinationSlot);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        startX=tag.getDouble("StartX"); startY=tag.getDouble("StartY"); startZ=tag.getDouble("StartZ");
        entityData.set(TARGET_X,tag.getFloat("TargetX")); entityData.set(TARGET_Y,tag.getFloat("TargetY"));
        entityData.set(TARGET_Z,tag.getFloat("TargetZ")); entityData.set(SUCCESS,tag.getBoolean("Success"));
        pendingWild=tag.hasUUID("PendingWild")?tag.getUUID("PendingWild"):null;
        pendingOwner=tag.hasUUID("PendingOwner")?tag.getUUID("PendingOwner"):null;
        pendingExpired=tag.getBoolean("PendingExpired");masterBall=tag.getBoolean("MasterBall");destinationSlot=tag.getInt("DestinationSlot");
    }
    @Override public Packet<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
