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
  if(phase.equals("SUBMERGED")||phase.equals("SURFACE"))tag.putInt("NaturalAquaticTicks",ticks);
  tag.putInt("NaturalBehaviorStartDelay",0);tag.putString("NaturalBehaviorPhase",phase);tag.putInt("NaturalBehaviorTicks",ticks);tag.putInt("NaturalAltitudeStage",stage);mob.readAdditionalSaveData(tag);
 }
 private static String state(MinemonCreature m){return " species="+m.getSpeciesSlug()+" phase="+m.naturalCycle().phase()+" ticks="+m.naturalCycle().ticks()+" pos="+m.position()+" wet="+m.isInWater()+" sleep="+m.isSleeping()+" night="+m.isNightResting()+" grounded="+m.isOnGround()+" target="+m.getTarget()+" nav="+m.getNavigation().getTargetPos();}
 private static void await(GameTestHelper h,BooleanSupplier condition,Runnable next,int remaining,String error){
  if(condition.getAsBoolean()){next.run();return;}
  if(remaining<=0){
   var origin=h.absolutePos(BlockPos.ZERO);
   for(var m:h.getLevel().getEntitiesOfClass(MinemonCreature.class,new net.minecraft.world.phys.AABB(origin).inflate(160)))
    com.mojang.logging.LogUtils.getLogger().error("INDIVIDUAL BEHAVIOR FAILURE"+state(m)+" velocity="+m.getDeltaMovement()+" gravity="+!m.isNoGravity()+" navigation="+m.getNavigation().getClass().getSimpleName());
   h.fail(error);return;
  }
  h.runAfterDelay(5,()->await(h,condition,next,remaining-5,error));
 }
 @GameTest(template="test_arena",timeoutTicks=700)
 public static void flyerAlternatesTwoAltitudeBandsAndPausesForSleep(GameTestHelper h){
  arena(h,false);var bird=spawn(h,"spearow",5,3,5);
  // Spearow has no compatible native ground gait, so it remains in its
  // source-backed continuous flight profile and alternates both altitude bands.
  restore(bird,"AIR",0,0);
  await(h,()->bird.naturalCycle().stage()==0&&band(bird,3,5),()->{
   restore(bird,"AIR",0,1);
   await(h,()->bird.naturalCycle().stage()==1&&band(bird,6,9),()->{
    restore(bird,"AIR",0,0);
    await(h,()->bird.naturalCycle().stage()==0&&band(bird,3,5),()->{
     int ticks=bird.naturalCycle().ticks();bird.setSleeping(true);
     h.runAfterDelay(20,()->{
      check(h,bird.naturalCycle().ticks()==ticks,"Sleep must pause the exact altitude clock");
      bird.setSleeping(false);h.succeed();
     });
   },200,"Flight must return smoothly to the 3..5 band stage="+bird.naturalCycle().stage()+" altitude="+(bird.getY()-bird.naturalSurface())+state(bird));
  },200,"Flight must reach the 6..9 band stage="+bird.naturalCycle().stage()+" altitude="+(bird.getY()-bird.naturalSurface())+state(bird));
  },200,"Continuous flyer must patrol at 3..5 blocks above terrain"+state(bird));
 }
 private static boolean band(MinemonCreature m,double low,double high){double a=m.getY()-m.naturalSurface();return a>=low-0.1D&&a<=high+0.1D;}
 @GameTest(template="test_arena",timeoutTicks=1000)
 public static void allMixedFliersTakeOffChangeAltitudeAndLandWithoutTeleport(GameTestHelper h){
  arena(h,false);
  var mobs=java.util.List.of(spawn(h,"charizard",3,1,5),spawn(h,"scyther",10,1,10),spawn(h,"dragonite",8,1,4));
  await(h,()->mobs.stream().allMatch(MinemonCreature::isOnGround),()->{
   for(var mob:mobs){
    check(h,!mob.isFlyingSpecies(),"Mixed species must begin on land"+state(mob));
    restore(mob,"LAND",5999,0);
   }
   await(h,()->mobs.stream().allMatch(m->m.naturalCycle().phase()==AIR&&band(m,3,5)),()->{
    for(var mob:mobs){
     check(h,!mob.isOnGround()&&mob.isFlyingSpecies(),"Takeoff must physically occur"+state(mob));
     restore(mob,"AIR",3599,0);
    }
    await(h,()->mobs.stream().allMatch(m->m.naturalCycle().stage()==1&&band(m,6,9)),()->{
     for(var mob:mobs)restore(mob,"AIR",2399,1);
     await(h,()->mobs.stream().allMatch(m->m.naturalCycle().phase()==LAND),()->{
      for(var mob:mobs)check(h,mob.isOnGround()&&!mob.isFlyingSpecies()&&!mob.isNoGravity(),"Real dry landing must restore land control"+state(mob));
      h.succeed();
     },400,"All three mixed species must complete safe landing");
    },250,"All three mixed species must physically reach the 6..9 high flight band");
    },250,"All three mixed species must physically enter the 3..5 low flight band");
  },30,"Mixed species must first settle on their physical ground support");
 }
 @GameTest(template="test_arena",timeoutTicks=250)
 public static void closeNeighborsActuallySeparateAndThenStop(GameTestHelper h){
  arena(h,false);var mob=spawn(h,"bulbasaur",6,1,6);var neighbor=spawn(h,"bulbasaur",7,1,6);
  neighbor.setNoAi(true);
  var separation=mob.goalSelector.getAvailableGoals().stream().map(g->g.getGoal())
      .filter(g->g.getClass().getSimpleName().equals("NaturalSeparationGoal")).findFirst().orElseThrow();
  mob.goalSelector.removeAllGoals();mob.goalSelector.addGoal(0,separation);
  h.runAfterDelay(160,()->{
   check(h,mob.distanceToSqr(neighbor)>=7.84D,"Close neighbors must physically reach approximately three blocks of separation"+state(mob));
   Vec3 settled=mob.position();
   h.runAfterDelay(60,()->{
    check(h,mob.position().distanceToSqr(settled)<0.25D,"Spacing must settle rather than oscillate indefinitely"+state(mob));h.succeed();
   });
  });
 }
 @GameTest(template="test_arena",timeoutTicks=1200)
 public static void visitorDivesSurfacesExitsAndHonorsLandCooldown(GameTestHelper h){
  arena(h,true);var turtle=spawn(h,"squirtle",2,1,7);
  // Start directly at the seek phase to keep this test focused on real
  // navigation; the full five-minute land interval is covered by unit tests.
  restore(turtle,"SEEK_WATER",0,0);
  await(h,()->turtle.naturalCycle().phase()==SUBMERGED&&underwater(turtle),()->{
   restore(turtle,"SUBMERGED",5999,0);
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
 @GameTest(template="test_arena",batch="daytime_swimming",timeoutTicks=320)
 public static void permanentSwimmersMoveUnderwaterAndSurfaceSpeciesFloat(GameTestHelper h){
  // This probe checks ordinary daytime swimming; separate night tests own the sleep transition.
  h.getLevel().setDayTime(1000);
  arena(h,true);
  // The model-sized Gyarados needs a deeper basin than the old small-box fixture.
  for(int x=3;x<=14;x++)for(int z=2;z<=14;z++)for(int y=4;y<=22;y++)
   h.setBlock(new BlockPos(x,y,z),x==3||x==14||z==2||z==14?Blocks.STONE:y<=18?Blocks.WATER:Blocks.AIR);
  var fish=spawn(h,"magikarp",5,-2,4);fish.setDeltaMovement(-0.12D,0,-0.12D);var lapras=spawn(h,"lapras",8,0,9);var gyarados=spawn(h,"gyarados",11,0,5);
  Vec3 start=fish.position();double[] farthest={0};
  traceAscent(h,fish,0);
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
 @GameTest(template="test_arena",batch="deep_water_ascent",timeoutTicks=300)
 public static void aquaticControllerActuallyAscendsFromDeepWater(GameTestHelper h){
  // Other tests in defaultBatch share this world and change it to night; keep
  // this navigation-only probe independent of the sleep routine's time gate.
  h.getLevel().setDayTime(1000);
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
 @GameTest(template="test_arena",batch="long_range_water",timeoutTicks=2600)
 public static void aquaticSeekerPhysicallyReachesWaterBeyondNinetyBlocks(GameTestHelper h){
  // A separate batch and elevated corridor keep the long route independent
  // of neighboring behavior arenas and their water fixtures.
  for(int x=0;x<=112;x+=16){
   var chunk=new net.minecraft.world.level.ChunkPos(h.absolutePos(new BlockPos(x,18,2)));
   h.getLevel().setChunkForced(chunk.x,chunk.z,true);
  }
  for(int x=0;x<=105;x++)for(int z=0;z<=4;z++){
   h.setBlock(new BlockPos(x,18,z),Blocks.STONE);
   for(int y=19;y<=23;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
  }
  // Keep the pond beyond the test's 90-block proof while leaving a small
  // margin inside the 100-block search radius if land wandering shifts the
  // seeker's block position by one block before its long-range scan begins.
  for(int x=96;x<=100;x++)for(int z=0;z<=4;z++){
   h.setBlock(new BlockPos(x,15,z),Blocks.STONE);
   for(int y=16;y<=18;y++)h.setBlock(new BlockPos(x,y,z),Blocks.WATER);
  }
  var fish=h.spawn(ModEntities.SPECIES.get("goldeen").get(),new BlockPos(2,19,2));
  Vec3 start=fish.position();
  for(int sample=200;sample<=2200;sample+=200)traceLongRoute(h,fish,sample);
  h.runAfterDelay(20,()->{
   for(int x=2;x<96;x++){
    var floor=h.absolutePos(new BlockPos(x,18,2));
    check(h,h.getLevel().getBlockState(floor).is(Blocks.STONE),"Long-range fixture lost corridor floor at "+floor);
    check(h,h.getLevel().getBlockState(floor.above()).isAir(),"Long-range fixture has an unexpected obstacle at "+floor.above());
   }
   var target=h.absolutePos(new BlockPos(98,17,2));
   var path=fish.getNavigation().createPath(target,0);
   com.mojang.logging.LogUtils.getLogger().info("LONG WATER PATH target="+target+" loaded="+h.getLevel().hasChunkAt(target)
       +" region="+WaterMovementGoal.isNavigableWaterRegion(target,p->h.getLevel().getFluidState(p).is(net.minecraft.tags.FluidTags.WATER))
       +" range="+fish.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE)
       +" path="+(path==null?"null":path.canReach()+" nodes="+path.getNodeCount()+" end="+path.getEndNode())+state(fish));
  });
  check(h,fish.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE)>=100,
      "Navigation must support the full advertised water-search radius");
  await(h,()->fish.isInWater()&&fish.position().distanceToSqr(start)>8100&&fish.getX()>=h.absolutePos(new BlockPos(96,0,0)).getX(),()->{
   check(h,fish.getX()>=h.absolutePos(new BlockPos(96,0,0)).getX(),"Water seeker must physically reach the distant pond"+state(fish));
   h.succeed();
  },2400,"Water search and real navigation must reach a valid pond beyond ninety blocks"+state(fish));
 }
 private static void traceLongRoute(GameTestHelper h,MinemonCreature fish,int ticks){
  h.runAfterDelay(ticks,()->{
   var path=fish.getNavigation().getPath();
   var control=fish.getMoveControl();
   com.mojang.logging.LogUtils.getLogger().info("LONG WATER PROGRESS elapsed="+ticks+state(fish)
       +" velocity="+fish.getDeltaMovement()+" input="+fish.yya+","+fish.zza+" speed="+fish.getSpeed()
       +" navDone="+fish.getNavigation().isDone()+" wanted="+control.getWantedX()+","+control.getWantedY()+","+control.getWantedZ()
       +" path="+(path==null?"null":path.getNextNodeIndex()+"/"+path.getNodeCount()+" next="+(path.isDone()?"done":path.getNextNode()))
       +" goals="+fish.goalSelector.getAvailableGoals().stream().filter(g->g.isRunning()).map(g->g.getGoal().getClass().getSimpleName()).toList());
  });
 }
 @GameTest(template="test_arena",timeoutTicks=60)
 public static void seakingScaleAndNaturalCycleStatePersist(GameTestHelper h){
  arena(h,false);var fish=spawn(h,"seaking",5,1,5);
  check(h,Math.abs(fish.getBbWidth()-br.com.petrickfaria.minemon.entity.ModelMetrics.width("seaking",br.com.petrickfaria.minemon.entity.SpeciesDefinitions.get("seaking").scale()))<.0001D&&Math.abs(fish.getBbHeight()-br.com.petrickfaria.minemon.entity.ModelMetrics.height("seaking",br.com.petrickfaria.minemon.entity.SpeciesDefinitions.get("seaking").scale()))<.0001D,"Seaking hitbox must be 75 percent of the approved dimensions");
  var dragon=spawn(h,"charizard",9,1,9);restore(dragon,"AIR",437,0);
  CompoundTag tag=new CompoundTag();dragon.addAdditionalSaveData(tag);var copy=ModEntities.SPECIES.get("charizard").get().create(h.getLevel());copy.readAdditionalSaveData(tag);
  check(h,copy.naturalCycle().phase()==AIR&&copy.naturalCycle().ticks()==437&&copy.isFlyingSpecies(),"NBT must preserve phase, elapsed ticks and synchronized flight mode");h.succeed();
 }
 @GameTest(template="test_arena",timeoutTicks=60)
 public static void requestedLargeSpeciesRenderScalesAndSelectionBoxesMatch(GameTestHelper h){
  arena(h,false);
  var victreebel=spawn(h,"victreebel",5,1,5);var venusaur=spawn(h,"venusaur",10,1,5);
  var snorlax=spawn(h,"snorlax",15,1,5);
  check(h,Math.abs(victreebel.getBbWidth()-br.com.petrickfaria.minemon.entity.ModelMetrics.width("victreebel",br.com.petrickfaria.minemon.entity.SpeciesDefinitions.get("victreebel").scale()))<.001D&&Math.abs(victreebel.getBbHeight()-br.com.petrickfaria.minemon.entity.ModelMetrics.height("victreebel",br.com.petrickfaria.minemon.entity.SpeciesDefinitions.get("victreebel").scale()))<.001D,
      "Victreebel's additional 10 percent reduction must also resize its selection box");
  check(h,Math.abs(venusaur.getBbWidth()-br.com.petrickfaria.minemon.entity.ModelMetrics.width("venusaur",br.com.petrickfaria.minemon.entity.SpeciesDefinitions.get("venusaur").scale()))<.001D&&Math.abs(venusaur.getBbHeight()-br.com.petrickfaria.minemon.entity.ModelMetrics.height("venusaur",br.com.petrickfaria.minemon.entity.SpeciesDefinitions.get("venusaur").scale()))<.001D,
      "Venusaur's additional 10 percent reduction must also resize its selection box");
  check(h,Math.abs(snorlax.getBbWidth()-br.com.petrickfaria.minemon.entity.ModelMetrics.width("snorlax",br.com.petrickfaria.minemon.entity.SpeciesDefinitions.get("snorlax").scale()))<.001D&&Math.abs(snorlax.getBbHeight()-br.com.petrickfaria.minemon.entity.ModelMetrics.height("snorlax",br.com.petrickfaria.minemon.entity.SpeciesDefinitions.get("snorlax").scale()))<.001D,
      "Snorlax's previously approved proportional selection box must remain intact");
  check(h,Math.abs(SpeciesDefinitions.get("victreebel").scale()-1.1169D)<.001D
      &&Math.abs(SpeciesDefinitions.get("venusaur").scale()-1.35D)<.001D,
      "Only the requested render scales must receive the new 10 percent factor");h.succeed();
 }
}
