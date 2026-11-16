package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.entity.*;
import br.com.petrickfaria.minemon.registry.ModEntities;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("minemon_followup")
@PrefixGameTestTemplate(false)
public final class MovementSleepFollowupTests {
    private static void check(boolean ok,String message){if(!ok)throw new GameTestAssertException(message);}
    private static void arena(GameTestHelper h,boolean water){
        for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=0;y<=22;y++)
            h.setBlock(new BlockPos(x,y,z),y==0||water&&y<=18&&(x==0||z==0||x==15||z==15)
                    ?Blocks.STONE:water&&y<=18?Blocks.WATER:Blocks.AIR);
        var bounds=new net.minecraft.world.phys.AABB(h.absolutePos(BlockPos.ZERO),h.absolutePos(new BlockPos(16,23,16)));
        h.onEachTick(()->h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,bounds,
                m->m.getType().getCategory()==net.minecraft.world.entity.MobCategory.MONSTER)
                .forEach(net.minecraft.world.entity.Entity::discard));
    }
    private static ServerPlayer owner(GameTestHelper h,int x,int y,int z){
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"FollowupOwner"),null);
        p.connection=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"FollowupConnection")).connection;
        var at=h.absolutePos(new BlockPos(x,y,z));p.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
        p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.setNoGravity(true);h.getLevel().addNewPlayer(p);return p;
    }
    private static MinemonCreature spawn(GameTestHelper h,String slug,int x,int y,int z){
        return h.spawn(ModEntities.SPECIES.get(slug).get(),new BlockPos(x,y,z));
    }
    private static void await(GameTestHelper h,BooleanSupplier condition,Runnable done,int remaining,String reason){
        if(condition.getAsBoolean()){done.run();return;}
        if(remaining<=0){h.fail(reason);return;}
        h.runAfterDelay(2,()->await(h,condition,done,remaining-2,reason));
    }
    @GameTest(template="test_arena",templateNamespace="minemon_followup",batch="followup_free_depth",timeoutTicks=240)
    public static void freeGoldeenBelowNearbyTrainerNeverAcquiresFollowGoal(GameTestHelper h){
        arena(h,true);h.getLevel().setDayTime(1000);var p=owner(h,8,19,8);
        var fish=spawn(h,"goldeen",5,4,5);fish.tame(p);fish.applyManagedBehavior("free");
        int[] following={0};double[] travel={0};var previous=new net.minecraft.world.phys.Vec3[]{fish.position()};
        h.onEachTick(()->{if(fish.isFollowingOwner())following[0]++;travel[0]+=fish.position().distanceTo(previous[0]);previous[0]=fish.position();});
        h.runAfterDelay(180,()->{
            check(following[0]==0,"Nearby shore owner must not steal a deep Goldeen's natural route; followTicks="+following[0]);
            check(travel[0]>1,"A free Goldeen must actually explore its pond, not simply have following disabled");
            p.discard();fish.discard();h.succeed();
        });
    }
    @GameTest(template="test_arena",templateNamespace="minemon_followup",batch="followup_free_repeat",timeoutTicks=500)
    public static void repeatedFreeCommandPreservesActiveReturnAndReleasesInsideBand(GameTestHelper h){
        arena(h,true);h.getLevel().setDayTime(1000);var p=owner(h,14,19,8);
        var fish=spawn(h,"goldeen",2,4,8);fish.tame(p);fish.applyManagedBehavior("free");
        boolean[] observed={false};
        h.onEachTick(()->{
            if(fish.isRemoved())return;
            boolean following=fish.isFollowingOwner();if(following)observed[0]=true;
            fish.applyManagedBehavior("free");
            check(!following||fish.isFollowingOwner(),"Duplicate Livre command cleared the active return owner");
        });
        await(h,()->observed[0]&&!fish.isFollowingOwner()&&fish.freeOwnerDistanceSqr(p)<=81,()->{
            check(fish.isFreeMode(),"Return changed managed mode");p.discard();fish.discard();h.succeed();
        },450,"Goldeen must return without repeated mode commands interrupting it");
    }
    @GameTest(template="test_arena",templateNamespace="minemon_followup",batch="followup_free_sleep",timeoutTicks=400)
    public static void deepFreeGoldeenSleepsWithoutOwnerReacquisition(GameTestHelper h){
        arena(h,true);h.getLevel().setDayTime(18000);var p=owner(h,8,19,8);
        var fish=spawn(h,"goldeen",6,4,8);fish.tame(p);fish.applyManagedBehavior("free");
        boolean[] observing={false};int[] wakes={0};
        h.onEachTick(()->{if(observing[0]&&!fish.isRemoved()&&(!fish.isSleeping()||fish.isFollowingOwner()))wakes[0]++;});
        await(h,fish::isSleeping,()->{
            var at=fish.position();observing[0]=true;
            h.runAfterDelay(100,()->{
                check(wakes[0]==0,"Deep sleep repeatedly woke to follow the nearby trainer");
                check(at.distanceTo(fish.position())<.001,"Sleeping Goldeen drifted at depth");
                p.discard();fish.discard();h.getLevel().setDayTime(1000);h.succeed();
            });
        },250,"Deep free Goldeen never entered sleep");
    }
    @GameTest(template="test_arena",templateNamespace="minemon_followup",batch="followup_pidgey_geodude",timeoutTicks=700)
    public static void seatedPidgeySleepsBesideWildGeodudeAndSurvivesDuplicateSit(GameTestHelper h){
        arena(h,false);h.getLevel().setDayTime(18000);var p=owner(h,8,1,8);
        var bird=spawn(h,"pidgey",6,3,8);bird.tame(p);bird.setOrderedToSit(true);
        var rock=spawn(h,"geodude",10,1,8);
        int[] samples={0};
        h.onEachTick(()->{
            if(++samples[0]%40==0&&!bird.isRemoved())System.out.println("[FOLLOWUP PIDGEY] tick="+samples[0]
                +" bird="+bird.position()+" sleep="+bird.isSleeping()+" night="+bird.isNightResting()
                +" target="+bird.getTarget()+" grounded="+bird.isOnGround()+" gravity="+!bird.isNoGravity()
                +" motion="+bird.getDeltaMovement()+" rockSleep="+rock.isSleeping()+" rock="+rock.position());
        });
        await(h,()->bird.isSleeping()&&rock.isSleeping(),()->{
            var at=bird.position();bird.applyManagedBehavior("sit");
            check(bird.isSleeping(),"Reapplying Sentar must not lift and wake a landed sleeper");
            h.runAfterDelay(100,()->{
                check(bird.isSleeping()&&bird.getTarget()==null,"Sleeping wild Geodude was treated as an unprovoked threat");
                check(bird.position().distanceTo(at)<.01,"Seated sleeping Pidgey bounced");
                p.discard();bird.discard();rock.discard();h.getLevel().setDayTime(1000);h.succeed();
            });
        },500,"Seated Pidgey and wild Geodude did not reach night rest");
    }
    @GameTest(template="test_arena",templateNamespace="minemon_followup",batch="followup_gyarados_free",timeoutTicks=1000)
    public static void freeGyaradosReachesSafeBottomBelowShoreOwner(GameTestHelper h){
        arena(h,true);h.getLevel().setDayTime(18000);var p=owner(h,8,19,8);
        var fish=spawn(h,"gyarados",8,13,8);fish.tame(p);fish.applyManagedBehavior("free");
        await(h,fish::isSleeping,()->{
            double floor=h.absolutePos(BlockPos.ZERO).getY()+1;
            check(Math.abs(fish.getY()-floor)<.1,"Gyarados slept above the safe submerged floor: "+fish.position());
            check(h.getLevel().noCollision(fish,fish.getBoundingBox()),"Gyarados penetrated the floor");
            p.discard();fish.discard();h.getLevel().setDayTime(1000);h.succeed();
        },900,"Free Gyarados never reached stable bottom sleep; position="+fish.position());
    }
}
