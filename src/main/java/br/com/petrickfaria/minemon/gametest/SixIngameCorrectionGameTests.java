package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.pokec.*;
import br.com.petrickfaria.minemon.registry.ModEntities;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder(Minemon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SixIngameCorrectionGameTests {
    private static void check(boolean ok,String message) { if(!ok)throw new GameTestAssertException(message); }
    private static ServerPlayer player(GameTestHelper h,String name) {
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),name),null);
        p.connection=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"Connection")).connection;
        return p;
    }
    @GameTest(template="test_arena",batch="six_spawn_progression",timeoutTicks=40)
    public static void spawnClockStartsAtUnlockPausesWhenDisabledAndIsIndependentPerPlayer(GameTestHelper h) {
        var first=player(h,"FirstTrainer");var second=player(h,"SecondTrainer");
        String slug="oddish";
        first.getPersistentData().putLong("MinemonWildSpawnAttempt",h.getLevel().getGameTime());
        second.getPersistentData().putLong("MinemonWildSpawnAttempt",h.getLevel().getGameTime());
        for(int i=0;i<1200;i++)WildEncounterManager.tickPlayer(first);
        check(SpawnChanceProgression.elapsedTicks(first.getPersistentData(),slug)==0,"Locked species cannot accumulate retroactive time");
        PokecPlayerData.unlock(first,slug);PokecPlayerData.unlock(second,slug);
        for(int i=0;i<1200;i++)WildEncounterManager.tickPlayer(first);
        check(SpawnChanceProgression.incrementCount(first.getPersistentData(),slug)==1,"First eligible minute adds exactly one increment");
        check(SpawnChanceProgression.elapsedTicks(second.getPersistentData(),slug)==0,"Another trainer has an independent clock");
        PokecPlayerData.toggleSpawn(first,slug);
        for(int i=0;i<2400;i++)WildEncounterManager.tickPlayer(first);
        check(SpawnChanceProgression.elapsedTicks(first.getPersistentData(),slug)==1200,"Disabled time must not accumulate");
        var saved=first.getPersistentData().copy();first.getPersistentData().merge(saved);
        PokecPlayerData.toggleSpawn(first,slug);WildEncounterManager.tickPlayer(first);
        check(SpawnChanceProgression.elapsedTicks(first.getPersistentData(),slug)==1201,"Reactivation retains progress without disabled-time catchup");
        SpawnChanceProgression.reset(first.getPersistentData(),slug);
        check(SpawnChanceProgression.elapsedTicks(first.getPersistentData(),slug)==0,"Success reset removes this species' progress");
        first.discard();second.discard();h.succeed();
    }
    @GameTest(template="test_arena",batch="six_free_return_land",timeoutTicks=650)
    public static void freeLandCompanionReturnsThenReleasesNaturalMovement(GameTestHelper h){freeReturn(h,"bulbasaur",false);}
    @GameTest(template="test_arena",batch="six_free_return_water",timeoutTicks=650)
    public static void freeSwimmerReturnsThroughWaterWithoutTeleport(GameTestHelper h){freeReturn(h,"goldeen",true);}
    @GameTest(template="test_arena",batch="six_free_return_air",timeoutTicks=650)
    public static void freeFlyerReturnsThroughAirWithoutTeleport(GameTestHelper h){freeReturn(h,"pidgey",false);}
    @GameTest(template="test_arena",batch="six_free_return_night",timeoutTicks=900)
    public static void sleepingFreeSwimmerReturnsOutsideBandThenResumesNightRest(GameTestHelper h){freeReturn(h,"goldeen",true,true);}
    private static void freeReturn(GameTestHelper h,String slug,boolean water) {
        freeReturn(h,slug,water,false);
    }
    private static void freeReturn(GameTestHelper h,String slug,boolean water,boolean night) {
        h.getLevel().setDayTime(night?13000:1000);
        for(int x=0;x<=36;x++)for(int z=0;z<=12;z++)for(int y=0;y<=12;y++)
            h.setBlock(new BlockPos(x,y,z),y==0||water&&y<=5&&(x==0||x==36||z==0||z==12)
                ?Blocks.STONE:water&&y<=5?Blocks.WATER:Blocks.AIR);
        var owner=player(h,"FreeReturnOwner");
        var end=h.absolutePos(new BlockPos(28,water?6:1,6));
        owner.moveTo(end.getX()+.5D,end.getY(),end.getZ()+.5D,0,0);h.getLevel().addNewPlayer(owner);
        var mob=h.spawn(ModEntities.SPECIES.get(slug).get(),new BlockPos(5,water?3:4,6));
        mob.tame(owner);mob.applyManagedBehavior("free");
        if(night)mob.setSleeping(true);
        Vec3[] previous={mob.position()};boolean[] returned={false};
        h.runAfterDelay(600,()->{
            if(returned[0])return;
            String state="Free return stalled: "+slug+" pos="+mob.position()+" owner="+owner.position()
                +" following="+mob.isFollowingOwner()+" combatTarget="+mob.getTarget()+" wet="+mob.isInWater()
                +" sleeping="+mob.isSleeping()+" target="+mob.getNavigation().getTargetPos()+" navDone="+mob.getNavigation().isDone();
            owner.discard();mob.discard();check(false,state);
        });
        var movementArena=new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(0,0,0)),h.absolutePos(new BlockPos(37,14,13))).inflate(8);
        h.onEachTick(()->{
            // Blacklisted monsters correctly preempt following. This test
            // isolates movement; the existing combat suite tests that priority.
            h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,movementArena,
                    candidate->candidate.getType().getCategory()==net.minecraft.world.entity.MobCategory.MONSTER)
                .forEach(net.minecraft.world.entity.Entity::discard);
            double step=mob.position().distanceTo(previous[0]);previous[0]=mob.position();
            check(step<1.0D,"Free return must not relocate "+slug+" step="+step);
            if(water&&mob.tickCount>2)check(mob.isInWaterOrBubble(),"A return in the contained pond must stay aquatic; "+mob.position());
            if(mob.distanceToSqr(owner)<=100.0D)returned[0]=true;
            if(returned[0]&&!mob.isFollowingOwner()){
                if(night&&(!mob.isSleeping()||!mob.isNightResting()))return;
                check(mob.isFreeMode(),"Return must preserve Livre mode");
                check(mob.navigationRange()==(water?128.0D:24.0D),"Natural pathfinding range must survive return");
                System.out.println("[MINEMON FREE RETURN] PASS "+slug+" distance="+mob.distanceTo(owner));
                if(night)h.getLevel().setDayTime(1000);
                owner.discard();mob.discard();h.succeed();
            }
        });
    }
    @GameTest(template="test_arena",batch="six_shore",timeoutTicks=650)
    public static void amphibianCrossesFlushBankInBothDirections(GameTestHelper h){shore(h,0,false,"squirtle");}
    @GameTest(template="test_arena",batch="six_shore",timeoutTicks=650)
    public static void amphibianCrossesRaisedBankInBothDirections(GameTestHelper h){shore(h,1,false,"psyduck");}
    @GameTest(template="test_arena",batch="six_shore",timeoutTicks=650)
    public static void amphibianCrossesSlabBankInBothDirections(GameTestHelper h){shore(h,1,true,"vaporeon");}
    private static void shore(GameTestHelper h,int raised,boolean slab,String slug) {
        h.getLevel().setDayTime(1000);
        for(int x=0;x<=20;x++)for(int z=0;z<=12;z++)for(int y=0;y<=10;y++) {
            boolean pond=x>=2&&x<=8&&z>=2&&z<=10;
            h.setBlock(new BlockPos(x,y,z),y==0?Blocks.STONE:pond&&y<=4?Blocks.WATER:
                !pond&&y<=4+raised?Blocks.STONE:Blocks.AIR);
        }
        if(slab)for(int x=9;x<=11;x++)for(int z=2;z<=10;z++)h.setBlock(new BlockPos(x,5,z),Blocks.STONE_SLAB);
        var mob=h.spawn(ModEntities.SPECIES.get(slug).get(),new BlockPos(5,3,6));
        mob.goalSelector.removeAllGoals();mob.targetSelector.removeAllGoals();
        Vec3[] previous={mob.position()};int[] stage={0};double[] maxStep={0};
        BlockPos[] routeTarget={null};
        h.runAfterDelay(600,()->System.out.println("[MINEMON SHORE DIAGNOSTIC] "+slug+" stage="+stage[0]
            +" pos="+mob.position()+" phase="+mob.naturalCycle().phase()+" wet="+mob.isInWater()
            +" grounded="+mob.isOnGround()+" navDone="+mob.getNavigation().isDone()+" target="+mob.getNavigation().getTargetPos()));
        h.onEachTick(()->{
            maxStep[0]=Math.max(maxStep[0],mob.position().distanceTo(previous[0]));previous[0]=mob.position();
            check(maxStep[0]<1.0D,"Shore traversal must use continuous physics, not teleport: "+slug+" step="+maxStep[0]);
            if(stage[0]==0&&mob.isInWater()) {
                var target=h.absolutePos(new BlockPos(15,5+raised,6));var route=mob.getNavigation().createPath(target,0);
                check(route!=null&&route.canReach(),"A reachable shore route must be found: "+slug+" target="+target
                    +" position="+mob.position()+" width="+mob.getBbWidth()+" height="+mob.getBbHeight()
                    +" path="+(route==null?"null":route.getNodeCount()+" nodes, end="+route.getEndNode()));
                mob.getNavigation().moveTo(route,.85D);routeTarget[0]=target;stage[0]=1;
            } else if(stage[0]==1&&mob.getX()>h.absolutePos(new BlockPos(12,0,0)).getX()&&mob.isOnGround()&&!mob.isInWater()) {
                var target=h.absolutePos(new BlockPos(4,3,6));var route=mob.getNavigation().createPath(target,0);
                check(route!=null&&route.canReach(),"Dry amphibian must find the return water route: "+slug);
                mob.getNavigation().moveTo(route,.85D);routeTarget[0]=target;stage[0]=2;
            } else if(stage[0]==2&&mob.isInWater()&&mob.getX()<h.absolutePos(new BlockPos(7,0,0)).getX()) {
                System.out.println("[MINEMON SHORE] PASS "+slug+" raised="+raised+" slab="+slab+" maxStep="+maxStep[0]);
                mob.discard();h.succeed();
            }
            // This fixture removes autonomous goals to measure native paths.
            // The species clock still ends phases and stops navigation; as a
            // real MOVE goal would, retain ownership of the requested route.
            if(stage[0]>0&&!mob.isRemoved()&&mob.getNavigation().isDone()&&routeTarget[0]!=null)
                mob.getNavigation().moveTo(routeTarget[0].getX()+.5D,routeTarget[0].getY(),routeTarget[0].getZ()+.5D,.85D);
        });
    }
}
