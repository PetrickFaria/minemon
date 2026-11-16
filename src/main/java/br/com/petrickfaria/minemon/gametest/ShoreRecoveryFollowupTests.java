package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.registry.ModEntities;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

/** Uses the complete autonomous/follow AI, never a test-owned route driver. */
@GameTestHolder("minemon_shore")
@PrefixGameTestTemplate(false)
public final class ShoreRecoveryFollowupTests {
    @GameTest(template="test_arena",templateNamespace="minemon_shore",batch="followup_shore_flat",timeoutTicks=800)
    public static void squirtleCrossesFlushBankWithItsActualFollowGoal(GameTestHelper h){run(h,"squirtle",0,false,0);}
    @GameTest(template="test_arena",templateNamespace="minemon_shore",batch="followup_shore_step",timeoutTicks=800)
    public static void psyduckCrossesRaisedBankWithItsActualFollowGoal(GameTestHelper h){run(h,"psyduck",1,false,0);}
    @GameTest(template="test_arena",templateNamespace="minemon_shore",batch="followup_shore_slab",timeoutTicks=800)
    public static void squirtleCrossesSlabBankWithItsActualFollowGoal(GameTestHelper h){run(h,"squirtle",0,true,0);}
    @GameTest(template="test_arena",templateNamespace="minemon_shore",batch="followup_shore_detour",timeoutTicks=1200)
    public static void squirtleUsesLateralOpeningInsteadOfPressingAgainstHighWall(GameTestHelper h){run(h,"squirtle",1,false,1);}
    @GameTest(template="test_arena",templateNamespace="minemon_shore",batch="followup_shore_open",timeoutTicks=1200)
    public static void psyduckRecoversWhenInitiallyBlockedPassageOpens(GameTestHelper h){run(h,"psyduck",1,false,3);}
    @GameTest(template="test_arena",templateNamespace="minemon_shore",batch="followup_shore_impossible",timeoutTicks=500)
    public static void inaccessibleWallReleasesFreeRoutineWithoutCrossingBlocks(GameTestHelper h){run(h,"squirtle",1,false,2);}
    @GameTest(template="test_arena",templateNamespace="minemon_shore",batch="followup_shore_permanent",timeoutTicks=500)
    public static void permanentSwimmerDoesNotGainLandTraversalWhenOwnerIsUnreachable(GameTestHelper h){run(h,"goldeen",1,false,2);}
    private static void check(boolean ok,String message){if(!ok)throw new GameTestAssertException(message);}
    private static void run(GameTestHelper h,String slug,int raised,boolean slab,int wall){
        var level=h.getLevel();level.setDayTime(1000);
        for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=0;y<=14;y++){
            boolean water=x>=1&&x<=7&&z>=1&&z<=14;
            h.setBlock(new BlockPos(x,y,z),y==0?Blocks.STONE:water&&y<=4?Blocks.WATER:
                    !water&&y<=4+raised?Blocks.STONE:Blocks.AIR);
        }
        if(slab)for(int x=8;x<16;x++)for(int z=0;z<16;z++)h.setBlock(new BlockPos(x,5,z),Blocks.STONE_SLAB);
        if(wall!=0)for(int z=0;z<16;z++)for(int y=5+raised;y<=12;y++){
            if(wall!=1||z<11||z>12)h.setBlock(new BlockPos(8,y,z),Blocks.STONE);
            h.setBlock(new BlockPos(0,y,z),Blocks.STONE);
            for(int x=0;x<=8;x++){h.setBlock(new BlockPos(x,y,0),Blocks.STONE);h.setBlock(new BlockPos(x,y,15),Blocks.STONE);}
        }
        var p=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"ShoreOwner"),null);
        p.connection=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"ShoreConnection")).connection;
        var target=h.absolutePos(new BlockPos(wall==2?30:14,5+raised,6));
        p.moveTo(target.getX()+.5,target.getY(),target.getZ()+.5,0,0);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.setNoGravity(true);
        if(wall==2)for(int x=29;x<=31;x++)for(int z=5;z<=7;z++)h.setBlock(new BlockPos(x,4+raised,z),Blocks.STONE);
        level.addNewPlayer(p);
        var mob=h.spawn(ModEntities.SPECIES.get(slug).get(),new BlockPos(3,3,6));mob.tame(p);
        mob.applyManagedBehavior(wall==2?"free":"follow");
        var bounds=new net.minecraft.world.phys.AABB(h.absolutePos(BlockPos.ZERO),h.absolutePos(new BlockPos(16,15,16)));
        int[] ticks={0},stage={0},released={0};boolean[] detour={false};
        double[] travelled={0},maxStep={0};var previous=new net.minecraft.world.phys.Vec3[]{mob.position()};
        if(wall==3)h.runAfterDelay(120,()->{
            for(int z=11;z<=12;z++)for(int y=6;y<=12;y++)h.setBlock(new BlockPos(8,y,z),Blocks.AIR);
        });
        h.onEachTick(()->{
            if(mob.isRemoved())return;
            level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,bounds,
                    m->m.getType().getCategory()==net.minecraft.world.entity.MobCategory.MONSTER).forEach(net.minecraft.world.entity.Entity::discard);
            int tick=++ticks[0];double step=mob.position().distanceTo(previous[0]);previous[0]=mob.position();maxStep[0]=Math.max(maxStep[0],step);
            check(step<1,"Shore recovery relocated the entity: "+slug+" step="+step);
            check(level.noCollision(mob,mob.getBoundingBox()),"Shore recovery entered a solid block: "+mob.position());
            double x=mob.getX()-h.absolutePos(BlockPos.ZERO).getX(),z=mob.getZ()-h.absolutePos(BlockPos.ZERO).getZ();
            if(x>7&&x<10&&z>10)detour[0]=true;
            if(wall==2){
                if(tick>40)check(mob.isInWaterOrBubble(),"An inaccessible wall provoked a pointless jump out of the pond");
                if(tick%20==0)System.out.println("[BLOCKED SWIM] tick="+tick+" pos="+mob.position()+" follow="+mob.isFollowingOwner()+" active="+mob.naturalBehaviorActive()+" phase="+mob.naturalCycle().phase()+" target="+mob.getTarget()+" nav="+mob.getNavigation().getTargetPos()+" done="+mob.getNavigation().isDone()+" wanted="+mob.getMoveControl().getWantedX()+","+mob.getMoveControl().getWantedY()+","+mob.getMoveControl().getWantedZ()+" motion="+mob.getDeltaMovement());
                if(tick>40){travelled[0]+=step;if(!mob.isFollowingOwner())released[0]++;}
                check(x<8,"The inaccessible wall was crossed");
                if(tick==400){
                    check(released[0]>=40&&travelled[0]>.5,"An impossible return must yield to valid natural swimming; released="+released[0]+" travel="+travelled[0]);
                    System.out.println("[FOLLOWUP SHORE] PASS inaccessible wall; natural ticks="+released[0]+" travel="+travelled[0]);
                    p.discard();mob.discard();h.succeed();
                }
                return;
            }
            if(stage[0]==0&&x>9&&!mob.isInWater()&&mob.isOnGround()){
                if(wall!=0)check(detour[0],"The high wall was bypassed without using its accessible lateral opening");
                var inside=h.absolutePos(new BlockPos(2,5,6));p.moveTo(inside.getX()+.5,inside.getY(),inside.getZ()+.5,0,0);stage[0]=1;
            }else if(stage[0]==1&&x<7&&mob.isInWater()&&mob.getBoundingBox().maxX<h.absolutePos(new BlockPos(8,0,0)).getX()){
                System.out.println("[FOLLOWUP SHORE] PASS "+slug+" raised="+raised+" slab="+slab+" wall="+wall+" ticks="+tick+" maxStep="+maxStep[0]);
                p.discard();mob.discard();h.succeed();
            }
            if(tick%100==0)System.out.println("[FOLLOWUP SHORE TRACE] "+slug+" wall="+wall+" stage="+stage[0]+" pos="+mob.position()+" wet="+mob.isInWater()+" following="+mob.isFollowingOwner()+" nav="+mob.getNavigation().getTargetPos()+" done="+mob.getNavigation().isDone()+" next="+(mob.getNavigation().getPath()==null||mob.getNavigation().getPath().isDone()?null:mob.getNavigation().getPath().getNextEntityPos(mob))+" wanted="+mob.getMoveControl().getWantedX()+","+mob.getMoveControl().getWantedY()+","+mob.getMoveControl().getWantedZ()+" motion="+mob.getDeltaMovement());
        });
    }
}
