package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.*;
import br.com.petrickfaria.minemon.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.function.BooleanSupplier;
import static br.com.petrickfaria.minemon.entity.BehaviorCycle.Phase.*;

@GameTestHolder(Minemon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IndividualBehaviorTests {
 private static void arena(GameTestHelper h,boolean pond){
  for(int x=0;x<16;x++)for(int z=0;z<16;z++){
   boolean water=pond&&x>=4&&x<=13&&z>=3&&z<=13;
   h.setBlock(new BlockPos(x,3,z),Blocks.STONE);
   for(int y=4;y<=8;y++)h.setBlock(new BlockPos(x,y,z),water?Blocks.WATER:Blocks.STONE);
   for(int y=9;y<=25;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
  }
 }
 private static MinemonCreature spawn(GameTestHelper h,String slug,int x,int y,int z){return h.spawn(ModEntities.SPECIES.get(slug).get(),new BlockPos(x,y+8,z));}
 private static void check(GameTestHelper h,boolean ok,String reason){if(!ok)h.fail(reason);}
 private static void restore(MinemonCreature mob,String phase,int ticks,int stage){
  CompoundTag tag=new CompoundTag();mob.addAdditionalSaveData(tag);
  tag.putInt("NaturalBehaviorStartDelay",0);tag.putString("NaturalBehaviorPhase",phase);tag.putInt("NaturalBehaviorTicks",ticks);tag.putInt("NaturalAltitudeStage",stage);mob.readAdditionalSaveData(tag);
 }
 private static String state(MinemonCreature m){return " species="+m.getSpeciesSlug()+" phase="+m.naturalCycle().phase()+" ticks="+m.naturalCycle().ticks()+" pos="+m.position()+" wet="+m.isInWater()+" grounded="+m.isOnGround()+" target="+m.getTarget()+" nav="+m.getNavigation().getTargetPos();}
 private static void await(GameTestHelper h,BooleanSupplier condition,Runnable next,int remaining,String error){
  if(condition.getAsBoolean()){next.run();return;}
  if(remaining<=0){
   var origin=h.absolutePos(BlockPos.ZERO);
   for(var m:h.getLevel().getEntitiesOfClass(MinemonCreature.class,new net.minecraft.world.phys.AABB(origin).inflate(22)))
    com.mojang.logging.LogUtils.getLogger().error("INDIVIDUAL BEHAVIOR FAILURE"+state(m)+" velocity="+m.getDeltaMovement()+" gravity="+!m.isNoGravity()+" navigation="+m.getNavigation().getClass().getSimpleName());
   h.fail(error);return;
  }
  h.runAfterDelay(5,()->await(h,condition,next,remaining-5,error));
 }
 @GameTest(template="test_arena",timeoutTicks=700)
 public static void flyerAlternatesTwoAltitudeBandsAndPausesForSleep(GameTestHelper h){
  arena(h,false);var bird=spawn(h,"pidgey",5,3,5);
  restore(bird,"LAND",5999,0);
  await(h,()->bird.naturalCycle().stage()==0&&band(bird,3,7),()->{
   restore(bird,"AIR",5999,0);
   await(h,()->bird.naturalCycle().stage()==1&&band(bird,8,15),()->{
    restore(bird,"AIR",2999,1);
    await(h,()->bird.naturalCycle().stage()==0&&band(bird,1,3),()->{
     int ticks=bird.naturalCycle().ticks();bird.setSleeping(true);
     h.runAfterDelay(20,()->{
      check(h,bird.naturalCycle().ticks()==ticks,"Sleep must pause the exact altitude clock");
      bird.setSleeping(false);h.succeed();
     });
    },200,"Flight must return smoothly to the 3..7 band"+state(bird));
   },200,"Flight must reach the 8..15 band"+state(bird));
  },200,"Flight must enter its first 3..7 band"+state(bird));
 }
 private static boolean band(MinemonCreature m,double low,double high){double a=m.getY()-m.naturalSurface();return a>=low&&a<=high;}
 @GameTest(template="test_arena",timeoutTicks=700)
 public static void charizardAndScytherSwitchNavigationAndLandWithoutTeleport(GameTestHelper h){
  arena(h,false);var first=spawn(h,"charizard",3,1,5);var second=spawn(h,"scyther",10,1,10);
  await(h,()->first.isOnGround()&&second.isOnGround(),()->{
  for(var mob:java.util.List.of(first,second)){
   check(h,!mob.isFlyingSpecies(),"Mixed species must begin on land"+state(mob));
   restore(mob,"LAND",5999,0);
   check(h,mob.getNavigation().moveTo(mob.getX()+2,mob.getY(),mob.getZ(),0.8D),"Grounded mixed species must accept the real walk route"+state(mob));
  }
  await(h,()->first.naturalCycle().phase()==AIR&&second.naturalCycle().phase()==AIR,()->{
   for(var mob:java.util.List.of(first,second)){
    check(h,!mob.isOnGround()&&mob.isFlyingSpecies(),"Takeoff must physically occur"+state(mob));restore(mob,"AIR",5999,1);
   }
   await(h,()->first.naturalCycle().phase()==LAND&&second.naturalCycle().phase()==LAND,()->{
    for(var mob:java.util.List.of(first,second))check(h,mob.isOnGround()&&!mob.isFlyingSpecies()&&!mob.isNoGravity(),"Real dry landing must restore land control"+state(mob));
    h.succeed();
   },400,"Mixed species must complete safe landing");
  },250,"Both mixed species must enter their real flight phase");
  },30,"Mixed species must first settle on their physical ground support");
 }
 @GameTest(template="test_arena",timeoutTicks=1200)
 public static void visitorDivesSurfacesExitsAndHonorsLandCooldown(GameTestHelper h){
  arena(h,true);var turtle=spawn(h,"squirtle",2,1,7);
  // Start directly at the seek phase to keep this test focused on real
  // navigation; the full 10-minute land interval is covered by unit tests.
  restore(turtle,"SEEK_WATER",0,0);
  await(h,()->turtle.naturalCycle().phase()==SUBMERGED&&underwater(turtle),()->{
   restore(turtle,"SUBMERGED",11999,0);
   await(h,()->turtle.naturalCycle().phase()==COOLDOWN&&!turtle.isInWater(),()->{
    check(h,turtle.isOnGround(),"Cooldown begins only after effective return to land");
    h.runAfterDelay(100,()->{
     check(h,turtle.naturalCycle().phase()==COOLDOWN&&!turtle.isInWater(),"Visitor must not immediately reenter water"+state(turtle));h.succeed();
    });
   },400,"Visitor must exit real water and return to land after its water interval"+state(turtle));
  },350,"Visitor must find, enter and submerge in reachable water"+state(turtle));
 }
 private static boolean underwater(MinemonCreature m){return m.level.getFluidState(new BlockPos(m.getX(),m.getY()+m.getBbHeight()+.1D,m.getZ())).is(net.minecraft.tags.FluidTags.WATER);}
 private static boolean atSurface(MinemonCreature m){return m instanceof MinemonAquaticCreature a&&Math.abs(m.getY()-(a.waterSurface()-WaterMovementGoal.preferredSurfaceDepth(m)))<.3D;}
 @GameTest(template="test_arena",timeoutTicks=320)
 public static void permanentSwimmersMoveUnderwaterAndSurfaceSpeciesFloat(GameTestHelper h){
  arena(h,true);var fish=spawn(h,"magikarp",4,-2,3);fish.setDeltaMovement(-0.12D,0,-0.12D);var lapras=spawn(h,"lapras",8,0,9);var gyarados=spawn(h,"gyarados",11,0,5);
  Vec3 start=fish.position();double[] farthest={0};
  sample(h,fish,start,farthest,20);
  h.runAfterDelay(240,()->{
   BlockPos head=new BlockPos(fish.getX(),fish.getY()+fish.getBbHeight()+.1D,fish.getZ());
   check(h,fish.isInWater(),"Permanent swimmer must stay in water"+state(fish));
   check(h,underwater(fish),"Permanent swimmer must keep its full hitbox submerged; headBlock="+head
       +" headFluid="+h.getLevel().getFluidState(head)+" bbHeight="+fish.getBbHeight()
       +" waterSurface="+(fish instanceof MinemonAquaticCreature aquatic?aquatic.waterSurface():Double.NaN)
       +" velocity="+fish.getDeltaMovement()+state(fish));
   check(h,farthest[0]>.5D,"Permanent swimmer must travel at least 0.5 blocks during the observation window; maxDistanceSquared="+farthest[0]
       +" start="+start+" current="+fish.position()+" target="+fish.getNavigation().getTargetPos()+" velocity="+fish.getDeltaMovement());
   for(var m:java.util.List.of(lapras,gyarados))check(h,m.isInWater()&&atSurface(m),"Surface species must keep their model-sized surface position"+state(m));
   h.succeed();
  });
 }
 private static void sample(GameTestHelper h,MinemonCreature m,Vec3 start,double[] farthest,int n){
  farthest[0]=Math.max(farthest[0],m.position().distanceToSqr(start));if(n>0)h.runAfterDelay(10,()->sample(h,m,start,farthest,n-1));
 }
 @GameTest(template="test_arena",timeoutTicks=300)
 public static void aquaticControllerActuallyAscendsFromDeepWater(GameTestHelper h){
  arena(h,true);var fish=spawn(h,"goldeen",7,-3,7);
  fish.goalSelector.removeAllGoals();fish.targetSelector.removeAllGoals();
  await(h,fish::isInWater,()->{
   double start=fish.getY();
   // moveTo(x,y,z) permits a path ending one block short; combined
   // with the normal waypoint tolerance that can finish without ascending.
   // Require an exact higher endpoint so this probe exercises real 3D travel.
   var destination=new BlockPos(fish.getX(),start+2.0D,fish.getZ());
   var path=fish.getNavigation().createPath(destination,0);
   check(h,path!=null&&path.canReach()&&path.getEndNode().y==destination.getY(),"Deep water must have an exact reachable higher node"+state(fish));
   check(h,fish.getNavigation().moveTo(path,1.0D),"Deep water ascent path must start"+state(fish));
   traceAscent(h,fish,0);
   h.runAfterDelay(200,()->{
    check(h,fish.isInWater()&&fish.getY()>start+1.0D,"Swimming controller must actually ascend, not continually sink"+state(fish));
    h.succeed();
   });
  },40,"Ascent fixture must first enter its real water volume");
 }
 private static void traceAscent(GameTestHelper h,MinemonCreature fish,int elapsed){
  var path=fish.getNavigation().getPath();
  com.mojang.logging.LogUtils.getLogger().info("MINEMON ASCENT TRACE sample="+elapsed+state(fish)
   +" velocity="+fish.getDeltaMovement()+" input="+fish.yya+","+fish.zza+" pitch="+fish.getXRot()
   +" speed="+fish.getSpeed()+" gravity="+!fish.isNoGravity()+" wanted="+fish.getMoveControl().getWantedX()+","+fish.getMoveControl().getWantedY()+","+fish.getMoveControl().getWantedZ()
   +" navDone="+fish.getNavigation().isDone()+" path="+(path==null?"null":path.getNextNodeIndex()+"/"+path.getNodeCount()+" end="+path.getEndNode()));
  if(elapsed<180)h.runAfterDelay(20,()->traceAscent(h,fish,elapsed+20));
 }
 @GameTest(template="test_arena",timeoutTicks=60)
 public static void seakingScaleAndNaturalCycleStatePersist(GameTestHelper h){
  arena(h,false);var fish=spawn(h,"seaking",5,1,5);
  check(h,Math.abs(fish.getBbWidth()-.525D)<.0001D&&Math.abs(fish.getBbHeight()-.45D)<.0001D,"Seaking hitbox must be 75 percent of the approved dimensions");
  var dragon=spawn(h,"charizard",9,1,9);restore(dragon,"AIR",437,0);
  CompoundTag tag=new CompoundTag();dragon.addAdditionalSaveData(tag);var copy=ModEntities.SPECIES.get("charizard").get().create(h.getLevel());copy.readAdditionalSaveData(tag);
  check(h,copy.naturalCycle().phase()==AIR&&copy.naturalCycle().ticks()==437&&copy.isFlyingSpecies(),"NBT must preserve phase, elapsed ticks and synchronized flight mode");h.succeed();
 }
}
