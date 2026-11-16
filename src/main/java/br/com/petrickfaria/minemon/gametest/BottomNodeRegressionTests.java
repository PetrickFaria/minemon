package br.com.petrickfaria.minemon.gametest;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.*;
@GameTestHolder("minemon_bottom") @PrefixGameTestTemplate(false)
public final class BottomNodeRegressionTests {
 @GameTest(template="test_arena",templateNamespace="minemon_bottom",batch="followup_fractional_bottom",timeoutTicks=1000)
 public static void gyaradosFinishesRestBelowAnAlreadyReachedIntegerWaterNode(GameTestHelper h){
  MovementSleepFollowupTests.bottomGyarados(h,"wild",2.25D);
 }
 @GameTest(template="test_arena",templateNamespace="minemon_bottom",batch="followup_bottom_headings",timeoutTicks=1000)
 public static void wildGyaradosDescendFromAllFourInitialHeadings(GameTestHelper h){
  var level=h.getLevel();level.setDayTime(18000);
  var fish=new java.util.ArrayList<br.com.petrickfaria.minemon.entity.MinemonCreature>();
  var owners=new java.util.ArrayList<net.minecraft.server.level.ServerPlayer>();
  for(int i=0;i<4;i++){
   int offset=i*20;
   for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=0;y<=22;y++)
    h.setBlock(new net.minecraft.core.BlockPos(offset+x,y,z),y==0||y<=18&&(x==0||x==15||z==0||z==15)
      ?net.minecraft.world.level.block.Blocks.STONE:y<=18?net.minecraft.world.level.block.Blocks.WATER:net.minecraft.world.level.block.Blocks.AIR);
   var at=h.absolutePos(new net.minecraft.core.BlockPos(offset+8,13,8));
   var chunk=new net.minecraft.world.level.ChunkPos(at);level.setChunkForced(chunk.x,chunk.z,true);
   var owner=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"BottomObserver"+i),null);
   owner.connection=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"BottomConnection"+i)).connection;
   owner.moveTo(at.getX()+.5,at.getY()+6,at.getZ()+.5,0,0);owner.setGameMode(net.minecraft.world.level.GameType.CREATIVE);owner.setNoGravity(true);level.addNewPlayer(owner);owners.add(owner);
   var g=br.com.petrickfaria.minemon.registry.ModEntities.SPECIES.get("gyarados").get().create(level);
   g.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,i*90,0);level.addFreshEntity(g);fish.add(g);
   boolean oldOverlap=g.getBoundingBox().intersects(owner.getBoundingBox());
   owner.moveTo(owner.getX(),Math.max(owner.getY(),h.absolutePos(new net.minecraft.core.BlockPos(0,19,0)).getY()+g.getBbHeight()+1),owner.getZ(),0,0);
   System.out.println("[BOTTOM OBSERVER] i="+i+" entityHeight="+g.getBbHeight()+" previousOverlap="+oldOverlap+" currentOverlap="+g.getBoundingBox().intersects(owner.getBoundingBox()));
  }
  var previous=fish.stream().map(net.minecraft.world.entity.Entity::position).toArray(net.minecraft.world.phys.Vec3[]::new);
  int[] ticks={0};double[] maxStep={0};
  h.onEachTick(()->{
   int tick=++ticks[0];
   for(int i=0;i<fish.size();i++){
    var g=fish.get(i);if(g.isRemoved())continue;
    var area=g.getBoundingBox().inflate(32);
    level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,area,m->m.getType().getCategory()==net.minecraft.world.entity.MobCategory.MONSTER).forEach(net.minecraft.world.entity.Entity::discard);
    if(g.getBoundingBox().intersects(owners.get(i).getBoundingBox()))h.fail("The observer became a physical obstacle for heading "+i);
    double step=g.position().distanceTo(previous[i]);previous[i]=g.position();maxStep[0]=Math.max(maxStep[0],step);
    if(step>=.6||!level.noCollision(g,g.getBoundingBox()))h.fail("Bottom heading "+i+" crossed a block or moved discontinuously: "+step);
    if(tick%100==0&&!g.isSleeping())System.out.println("[BOTTOM HEADING] i="+i+" tick="+tick+" pos="+g.position()+" rest="+g.isNightResting()+" target="+g.getTarget()+" yaw="+g.getYRot()+" pitch="+g.getXRot()+" speed="+g.getSpeed()+" input="+g.yya+","+g.zza+" motion="+g.getDeltaMovement()+" next="+(g.getNavigation().getPath()==null||g.getNavigation().getPath().isDone()?null:g.getNavigation().getPath().getNextEntityPos(g))+" goals="+g.goalSelector.getAvailableGoals().stream().filter(a->a.isRunning()).map(a->a.getGoal().getClass().getSimpleName()).toList());
   }
   if(fish.stream().allMatch(br.com.petrickfaria.minemon.entity.MinemonCreature::isSleeping)){
    for(var g:fish)if(Math.abs(g.getY()-(h.absolutePos(net.minecraft.core.BlockPos.ZERO).getY()+1))>.1)h.fail("A heading slept above the bottom");
    System.out.println("[BOTTOM HEADINGS PASS] tick="+tick+" headings=4 maxStep="+maxStep[0]+" observersOutsideCollision=true");
    fish.forEach(net.minecraft.world.entity.Entity::discard);owners.forEach(net.minecraft.world.entity.Entity::discard);level.setDayTime(1000);h.succeed();
   }else if(tick==900)h.fail("Not all initial headings reached bottom sleep; positions="+fish.stream().map(g->g.position()+" sleep="+g.isSleeping()).toList());
  });
 }
 @GameTest(template="test_arena",templateNamespace="minemon_bottom",batch="followup_occupied_bottom",timeoutTicks=1000)
 public static void gyaradosRoutesAroundAnOccupiedSleepColumn(GameTestHelper h){occupiedBottom(h,false);}
 @GameTest(template="test_arena",templateNamespace="minemon_bottom",batch="followup_occupied_bottom_sit",timeoutTicks=1000)
 public static void seatedGyaradosKeepsSafeRestPositionAfterDawn(GameTestHelper h){occupiedBottom(h,true);}
 private static void occupiedBottom(GameTestHelper h,boolean seated){
  var level=h.getLevel();level.setDayTime(18000);
  for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=0;y<=22;y++)
   h.setBlock(new net.minecraft.core.BlockPos(x,y,z),y==0||y<=18&&(x==0||x==15||z==0||z==15)
    ?net.minecraft.world.level.block.Blocks.STONE:y<=18?net.minecraft.world.level.block.Blocks.WATER:net.minecraft.world.level.block.Blocks.AIR);
  var at=h.absolutePos(new net.minecraft.core.BlockPos(8,0,8));
  var blocker=br.com.petrickfaria.minemon.registry.ModEntities.SPECIES.get("goldeen").get().create(level);
  blocker.moveTo(at.getX()+.5,at.getY()+5,at.getZ()+.5,0,0);blocker.setNoAi(true);blocker.setNoGravity(true);blocker.setSleeping(true);level.addFreshEntity(blocker);
  var g=br.com.petrickfaria.minemon.registry.ModEntities.SPECIES.get("gyarados").get().create(level);
  g.moveTo(at.getX()+.5,at.getY()+13,at.getZ()+.5,270,0);
  final net.minecraft.server.level.ServerPlayer owner;
  if(seated){
   owner=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"SeatedBottomOwner"),null);
   owner.connection=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"SeatedBottomConnection")).connection;
   owner.moveTo(at.getX()+.5,at.getY()+28,at.getZ()+.5,0,0);owner.setGameMode(net.minecraft.world.level.GameType.CREATIVE);owner.setNoGravity(true);level.addNewPlayer(owner);
   g.tame(owner);g.applyManagedBehavior("sit");
  }else owner=null;
  level.addFreshEntity(g);
  int[] ticks={0},dawnAt={-1};double[] maxStep={0};var previous=new net.minecraft.world.phys.Vec3[]{g.position()};
  var restPosition=new net.minecraft.world.phys.Vec3[]{null};
  h.onEachTick(()->{
   int tick=++ticks[0];double step=g.position().distanceTo(previous[0]);previous[0]=g.position();maxStep[0]=Math.max(maxStep[0],step);
   if(step>=.6||!level.noCollision(g,g.getBoundingBox()))h.fail("Occupied-bottom recovery moved discontinuously or crossed a solid block: "+step);
   level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,g.getBoundingBox().inflate(32),m->m.getType().getCategory()==net.minecraft.world.entity.MobCategory.MONSTER).forEach(net.minecraft.world.entity.Entity::discard);
   if(tick%100==0)System.out.println("[OCCUPIED BOTTOM] tick="+tick+" pos="+g.position()+" sleep="+g.isSleeping()+" nav="+g.getNavigation().getTargetPos()+" next="+(g.getNavigation().getPath()==null||g.getNavigation().getPath().isDone()?null:g.getNavigation().getPath().getNextEntityPos(g))+" rest="+g.isNightResting()+" time="+level.getDayTime()+" target="+g.getTarget()+" goals="+g.goalSelector.getAvailableGoals().stream().filter(a->a.isRunning()).map(a->a.getGoal().getClass().getSimpleName()).toList()+" overlap="+g.getBoundingBox().intersects(blocker.getBoundingBox()));
   if(dawnAt[0]<0&&g.isSleeping()){
    if(Math.abs(g.getY()-(at.getY()+1))>.1||g.getBoundingBox().intersects(blocker.getBoundingBox()))h.fail("Gyarados accepted an occupied or elevated rest position");
    if(seated){restPosition[0]=g.position();dawnAt[0]=tick;level.setDayTime(1000);return;}
    System.out.println("[OCCUPIED BOTTOM PASS] tick="+tick+" maxStep="+maxStep[0]+" bottom="+g.position());g.discard();blocker.discard();level.setDayTime(1000);h.succeed();
   }else if(dawnAt[0]>=0&&tick-dawnAt[0]>=40){
    if(g.isSleeping()||!g.isOrderedToSit()||g.position().distanceTo(restPosition[0])>.02)h.fail("Daytime sitting did not preserve the safe night-rest anchor");
    System.out.println("[OCCUPIED SITTING DAWN PASS] tick="+tick+" maxStep="+maxStep[0]+" anchor="+g.position());g.discard();blocker.discard();owner.discard();h.succeed();
   }else if(tick==900&&dawnAt[0]<0)h.fail("Gyarados did not find the accessible floor around a sleeping Goldeen; pos="+g.position());
  });
 }

}
