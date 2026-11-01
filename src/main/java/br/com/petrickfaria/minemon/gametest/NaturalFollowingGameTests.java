package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.registry.ModEntities;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Minemon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NaturalFollowingGameTests {
    @GameTest(template="test_arena", batch="natural_following", timeoutTicks=650)
    public static void followsWithContinuousMovementThenObeysSit(GameTestHelper h) {
        h.getLevel().setDayTime(1000);
        var arenaBox=new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(0,0,0)),h.absolutePos(new BlockPos(40,14,10)));
        h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,arenaBox).forEach(net.minecraft.world.entity.Entity::discard);
        for(int x=0;x<40;x++)for(int z=0;z<10;z++) {
            h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
            for(int y=1;y<=12;y++)h.setBlock(new BlockPos(x,y,z),
                    x==0||x==39||z==0||z==9?Blocks.STONE:Blocks.AIR);
        }
        ServerPlayer owner=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"FollowOwner"),null);
        owner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        owner.connection=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"FollowConnection")).connection;
        BlockPos end=h.absolutePos(new BlockPos(30,1,5));
        owner.moveTo(end.getX()+.5,end.getY(),end.getZ()+.5,0,0);
        h.getLevel().addNewPlayer(owner);
        MinemonCreature mob=ModEntities.SPECIES.get("bulbasaur").get().create(h.getLevel());
        BlockPos start=h.absolutePos(new BlockPos(5,1,5));
        mob.moveTo(start.getX()+.5,start.getY(),start.getZ()+.5,0,0);
        mob.setTame(true);mob.setOwnerUUID(owner.getUUID());mob.setOrderedToSit(false);
        h.getLevel().addFreshEntity(mob);
        Vec3[] previous={mob.position()},sitting={null};
        String[] pendingFailure={null};
        boolean[] completed={false},reported={false};
        NineBehaviorChecks.FollowingAltitudeFixture[] altitudeFixture={null};
        h.runAtTickTime(540,()->{
            if(pendingFailure[0]!=null)return;
            if(altitudeFixture[0]==null){pendingFailure[0]="Follower altitude phase was not initialized";return;}
            pendingFailure[0]=NineBehaviorChecks.checkFollowingAltitudes(h,altitudeFixture[0]);
        });
        h.runAtTickTime(545,()->{
            if(pendingFailure[0]!=null)return;
            if(altitudeFixture[0]!=null) {
                pendingFailure[0]=NineBehaviorChecks.finishFollowingAltitudes(h,altitudeFixture[0]);
                completed[0]=pendingFailure[0]==null;
            }
        });
        h.onEachTick(()->{
            if(!reported[0]&&pendingFailure[0]!=null) {
                reported[0]=true;
                owner.discard();mob.discard();
                if(altitudeFixture[0]!=null) {
                    altitudeFixture[0].owner.discard();
                    altitudeFixture[0].mobs.forEach(net.minecraft.world.entity.Entity::discard);
                }
                h.fail(pendingFailure[0]);
                return;
            }
            if(completed[0]) {completed[0]=false;h.succeed();return;}
            if(mob.position().distanceTo(previous[0])>1.5) {
                pendingFailure[0]="Following relocated the Pokémon discontinuously";
                previous[0]=mob.position();
                return;
            }
            previous[0]=mob.position();
        });
        h.runAtTickTime(20,()->{
            if(!mob.isFollowingRun())pendingFailure[0]="Distant companion did not enter running state: pos="+mob.position()+" owner="+owner.position()+" water="+mob.isInWater()+" sleep="+mob.isSleeping()+" target="+mob.getTarget();
        });
        h.runAtTickTime(80,()->{
            if(pendingFailure[0]!=null)return;
            if(mob.getX()<start.getX()+1.5) {pendingFailure[0]="Companion did not walk toward owner";return;}
            mob.setOrderedToSit(true);sitting[0]=mob.position();
        });
        h.runAtTickTime(140,()->{
            if(pendingFailure[0]!=null)return;
            if(mob.position().distanceTo(sitting[0])>.5) {
                pendingFailure[0]="Sitting companion abandoned its order: start="+sitting[0]+" end="+mob.position()+" velocity="+mob.getDeltaMovement()+" target="+mob.getTarget();
                return;
            }
            if(mob.isSleeping()||mob.isFollowingRun()) {
                pendingFailure[0]="Sitting must remain awake without following run";
                return;
            }
            owner.discard();mob.discard();
            altitudeFixture[0]=NineBehaviorChecks.followingAltitudes(h);
        });
    }
}
