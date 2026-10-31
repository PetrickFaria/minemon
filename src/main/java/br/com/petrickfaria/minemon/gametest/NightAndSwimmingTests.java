package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.*;
import br.com.petrickfaria.minemon.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import java.util.*;
import java.util.function.BooleanSupplier;

@GameTestHolder(Minemon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NightAndSwimmingTests {
 private static void check(GameTestHelper h,boolean ok,String why){if(!ok){h.getLevel().setDayTime(1000);h.fail(why);}}
 private static void await(GameTestHelper h,BooleanSupplier condition,Runnable next,int ticks,String why){
  await(h,condition,next,ticks,()->why);
 }
 private static void await(GameTestHelper h,BooleanSupplier condition,Runnable next,int ticks,java.util.function.Supplier<String> why){
  if(condition.getAsBoolean()){next.run();return;}
  if(ticks<=0){h.getLevel().setDayTime(1000);h.fail(why.get());return;}
  h.runAfterDelay(5,()->await(h,condition,next,ticks-5,why));
 }
 private static void awaitFlightResume(GameTestHelper h,List<MinemonCreature> mixed,
     Map<MinemonCreature,Integer> clocks,int remaining,List<String> history){
  if(mixed.stream().allMatch(m->flightResumeComplete(m,clocks))){h.succeed();return;}
  if(remaining<=0){
   h.getLevel().setDayTime(1000);
   h.fail("Mixed flyers and fixed floaters must resume their correct daylight routine after dawn; pending="
       +mixed.stream().filter(m->!flightResumeComplete(m,clocks))
           .map(NightAndSwimmingTests::flightResumeSnapshot).toList()+",history="+history);
   return;
  }
  h.runAfterDelay(5,()->{
   for(int i:new int[]{0,15,17})if(i<mixed.size())
    history.add("time="+h.getLevel().getGameTime()+" "+flightResumeSnapshot(mixed.get(i)));
   awaitFlightResume(h,mixed,clocks,remaining-5,history);
  });
 }
 private static boolean flightResumeComplete(MinemonCreature m,Map<MinemonCreature,Integer> clocks){
  if(m.isOnGround())return false;
  double altitude=m.getY()-m.naturalSurface();
  return switch(SpeciesBehaviorProfiles.profile(m.getSpeciesSlug())){
   case FLOAT_HALF -> Math.abs(altitude-m.naturalCycle().minimumAltitude())<=0.12D;
   case FLOAT_LOW -> altitude>=0.5D&&altitude<=1.0D;
   default -> m.naturalCycle().ticks()>clocks.get(m);
  };
 }
 private static String flightResumeSnapshot(MinemonCreature m){
  var level=m.getCommandSenderWorld();
  return m.getSpeciesSlug()+"{phase="+m.naturalCycle().phase()+",ticks="+m.naturalCycle().ticks()
      +",entityTick="+m.tickCount+",stage="+m.naturalCycle().stage()+",air="+m.isFlyingSpecies()
      +",active="+m.naturalBehaviorActive()
      +",ground="+m.isOnGround()+",alt="+(m.getY()-m.naturalSurface())+",min="+m.naturalCycle().minimumAltitude()
      +",landing="+m.wantsNaturalLanding()+",sleep="+m.isSleeping()+",night="+m.isNightResting()
      +",gravity="+!m.isNoGravity()+",noAI="+m.isNoAi()+",passenger="+m.isPassenger()
      +",target="+(m.getTarget()==null?"null":m.getTarget().getType())+",pos="+m.position()+",motion="+m.getDeltaMovement()
      +",chunk="+level.hasChunkAt(m.blockPosition())+",navDone="+m.getNavigation().isDone()
      +",nav="+m.getNavigation().getTargetPos()+",recovery="+((MinemonFlyingCreature)m).flightRecoveryAttemptsForValidation()
      +"/"+((MinemonFlyingCreature)m).flightRecoveryMovesForValidation()+",samples="
      +((MinemonFlyingCreature)m).flightRecoverySamplesForValidation()+"}";
 }
 private static void awaitBottom(GameTestHelper h,MinemonCreature shellder,MinemonCreature krabby,
     double originY,int remaining,int elapsed,Runnable next){
  if(elapsed%100==0)for(var m:List.of(shellder,krabby)){
   var path=m.getNavigation().getPath();
   com.mojang.logging.LogUtils.getLogger().info("BOTTOM TRACE elapsed="+elapsed+" species="+m.getSpeciesSlug()+" pos="+m.position()+" velocity="+m.getDeltaMovement()+" sleep="+m.isSleeping()+" night="+m.isNightResting()+" pitch="+m.getXRot()+" input="+m.yya+","+m.zza+" navDone="+m.getNavigation().isDone()+" wanted="+m.getMoveControl().getWantedX()+","+m.getMoveControl().getWantedY()+","+m.getMoveControl().getWantedZ()+" path="+(path==null?"null":path.getNextNodeIndex()+"/"+path.getNodeCount()+" next="+(path.isDone()?"done":path.getNextNode())));
  }
  if(shellder.isInWater()&&krabby.isInWater()
      &&shellder.getY()-originY<2.5D&&krabby.getY()-originY<4.5D){next.run();return;}
  if(remaining<=0){
   h.getLevel().setDayTime(1000);
   h.fail("Shellder and Krabby must choose reachable water near the floor; elapsed="+elapsed
       +" originY="+originY+" shellderLocalY="+(shellder.getY()-originY)+" shellderPos="+shellder.position()
       +" shellderNav="+shellder.getNavigation().getTargetPos()+" krabbyLocalY="+(krabby.getY()-originY)
       +" krabbyPos="+krabby.position()+" krabbyNav="+krabby.getNavigation().getTargetPos());
   return;
  }
  h.runAfterDelay(5,()->awaitBottom(h,shellder,krabby,originY,remaining-5,elapsed+5,next));
 }
 private static void arena(GameTestHelper h){
  for(int x=0;x<32;x++)for(int z=0;z<24;z++){
   h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
   for(int y=1;y<20;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
  }
 }
 private static MinemonCreature spawn(GameTestHelper h,String slug,int x,int y,int z){return h.spawn(ModEntities.SPECIES.get(slug).get(),new BlockPos(x,y,z));}
 private static void traceAwakeGyarados(GameTestHelper h,MinemonCreature mob,int elapsed){
  double top=((MinemonAquaticCreature)mob).waterSurface();
  var path=mob.getNavigation().getPath();
  String next=path==null||path.isDone()?"null":path.getNextNode().toString();
  com.mojang.logging.LogUtils.getLogger().info("GYARADOS TRACE elapsed="+elapsed+" pos="+mob.position()
      +" water="+mob.isInWater()+" surfaceMode="+mob.isNaturalSurface()+" surface="+top
      +" centerDepth="+(top-(mob.getY()+mob.getBbHeight()*.5D))
      +" target="+mob.getNavigation().getTargetPos()+" navDone="+mob.getNavigation().isDone()
      +" next="+next+" wanted="+mob.getMoveControl().getWantedX()+","+mob.getMoveControl().getWantedY()+","+mob.getMoveControl().getWantedZ()
      +" motion="+mob.getDeltaMovement());
  if(elapsed<300)h.runAfterDelay(50,()->traceAwakeGyarados(h,mob,elapsed+50));
 }
 @GameTest(template="test_arena",batch="night_landing_water",timeoutTicks=700)
 public static void landRestingFlyerFindsDryGroundBeforeStartingSleepOverWater(GameTestHelper h){
  arena(h);var level=h.getLevel();
  // Keep a real water column and a three-block-safe dry landing site inside
  // the loaded test area, so this test measures shore flight rather than chunk loading.
  for(int z=2;z<=12;z++)for(int y=1;y<=8;y++)h.setBlock(new BlockPos(16,y,z),Blocks.WATER);
  var pidgeot=spawn(h,"pidgeot",16,10,7);level.setDayTime(18000);
  await(h,()->pidgeot.isSleeping()&&pidgeot.isOnGround(),()->{
   var support=pidgeot.blockPosition().below();
   var supportState=level.getBlockState(support);
   check(h,!pidgeot.isInWaterOrBubble()&&level.getFluidState(support).isEmpty()
           &&supportState.isSolidRender(level,support),
       "a land-resting flyer must reach a safe dry support before entering sleep");
   h.succeed();
  },500,()->"A flyer over water must navigate to a reachable shoreline and sleep there; pos="+pidgeot.position()
      +",night="+pidgeot.isNightResting()+",sleep="+pidgeot.isSleeping()+",ground="+pidgeot.isOnGround()
      +",target="+pidgeot.getNavigation().getTargetPos()+",navDone="+pidgeot.getNavigation().isDone());
 }
 @GameTest(template="test_arena",batch="nocturnal_schedule",timeoutTicks=800)
 public static void nocturnalSpeciesSleepFromMiddayUntilNightAndPauseTheirRoutine(GameTestHelper h){
  arena(h);var level=h.getLevel();level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,level.getServer());
  level.setDayTime(5000);
  var nocturnal=new ArrayList<MinemonCreature>();
  var slugs=List.of("gastly","haunter","gengar","zubat","golbat");
  for(int i=0;i<slugs.size();i++)nocturnal.add(spawn(h,slugs.get(i),3+i*5,1,5));
  await(h,()->nocturnal.stream().allMatch(m->!m.isSleeping()),()->{
   level.setDayTime(6000);
   await(h,()->nocturnal.stream().allMatch(MinemonCreature::isSleeping),()->{
    check(h,nocturnal.get(0).getY()-nocturnal.get(0).naturalSurface()>.8D,"Gastly must sleep at its one-block hover height");
    check(h,nocturnal.get(1).getY()-nocturnal.get(1).naturalSurface()>.8D,"Haunter must sleep at its one-block hover height");
    for(int i=2;i<nocturnal.size();i++)check(h,nocturnal.get(i).isOnGround(),nocturnal.get(i).getSpeciesSlug()+" must land for daytime sleep");
    var paused=new HashMap<MinemonCreature,Integer>();nocturnal.forEach(m->paused.put(m,m.naturalCycle().ticks()));
    h.runAfterDelay(30,()->{
     nocturnal.forEach(m->check(h,m.naturalCycle().ticks()==paused.get(m),m.getSpeciesSlug()+" must pause its routine while asleep"));
     level.setDayTime(12999);
     h.runAfterDelay(5,()->{
      check(h,nocturnal.stream().allMatch(MinemonCreature::isSleeping),"Nocturnal species must remain asleep through world time 12,999");
      level.setDayTime(13000);
      await(h,()->nocturnal.stream().noneMatch(MinemonCreature::isSleeping),()->h.succeed(),40,
          "Nocturnal species must wake at the start of night (world time 13,000)");
     });
    });
   },250,"Gastly, Haunter, Gengar, Zubat, and Golbat must begin daytime sleep at world time 6,000");
  },40,"Nocturnal species must remain awake during the first half of daylight");
 }
 @GameTest(template="test_arena",batch="night_aquatic",timeoutTicks=1500)
 public static void aquaticNightRestKeepsSurfaceAndBottomRoutinesSafe(GameTestHelper h){
  arena(h);var level=h.getLevel();
  for(int x=2;x<=15;x++)for(int z=3;z<=10;z++)for(int y=1;y<=14;y++)h.setBlock(new BlockPos(x,y,z),Blocks.WATER);
  level.setDayTime(18000);
  var gyarados=spawn(h,"gyarados",4,3,6);var magikarp=spawn(h,"magikarp",8,4,6);var dratini=spawn(h,"dratini",12,6,6);var shellder=spawn(h,"shellder",5,8,9);var vaporeon=spawn(h,"vaporeon",14,7,6);var cloyster=spawn(h,"cloyster",10,7,9);
  await(h,()->gyarados.isSleeping()&&magikarp.isSleeping()&&dratini.isSleeping()&&shellder.isSleeping()&&vaporeon.isSleeping()&&cloyster.isSleeping(),()->{
   check(h,gyarados.isInWater()&&magikarp.isInWater()&&dratini.isInWater()&&shellder.isInWater()&&vaporeon.isInWater()&&cloyster.isInWater(),"Aquatic and visitor sleepers must stay wet");
   double gyaradosY=WaterMovementGoal.waterRestY(gyarados),fishY=WaterMovementGoal.waterRestY(magikarp),visitorY=WaterMovementGoal.waterRestY(dratini),cloysterY=WaterMovementGoal.waterRestY(cloyster);
   double shellderY=WaterMovementGoal.waterRestY(shellder),originY=h.absolutePos(BlockPos.ZERO).getY();
   check(h,Math.abs(gyarados.getY()-gyaradosY)<.2D&&gyarados.getY()-originY<2.5D,"Gyarados must rest at the safe water bottom at night");
   check(h,Math.abs(cloyster.getY()-cloysterY)<.2D&&Math.abs(cloyster.getY()-originY-7)<.2D,"Exclusively aquatic Cloyster must preserve its submerged starting depth");
   check(h,Math.abs(magikarp.getY()-fishY)<.2D,"Permanent submerged species must float at a safe, fully wet depth");
   check(h,Math.abs(dratini.getY()-visitorY)<.2D&&dratini.getY()-originY<2.5D,"A water visitor caught in water at night must settle at the bottom");
   check(h,Math.abs(shellder.getY()-shellderY)<.2D&&Math.abs(shellder.getY()-originY-8)<.2D,
       "Exclusively aquatic Shellder must preserve its submerged starting depth");
   // Keep the aggressor north of Dratini, clear of the sleeping Vaporeon body.
   var attacker=h.spawn(EntityType.COW,new BlockPos(12,1,5));attacker.setNoAi(true);
   float health=attacker.getHealth();
   check(h,dratini.hurt(DamageSource.mobAttack(attacker),1)&&!dratini.isSleeping()&&dratini.getTarget()==attacker,
       "An aquatic night sleeper must wake and retain its attacker");
   await(h,()->attacker.getHealth()<health,()->{
    attacker.discard();await(h,dratini::isSleeping,()->{
     level.setDayTime(1000);
      await(h,()->!gyarados.isSleeping()&&!magikarp.isSleeping()&&!dratini.isSleeping()&&!shellder.isSleeping()&&!vaporeon.isSleeping()&&!cloyster.isSleeping(),()->{
      traceAwakeGyarados(h,gyarados,0);
      h.runAfterDelay(300,()->{
       double top=((MinemonAquaticCreature)gyarados).waterSurface();
       double depth=top-(gyarados.getY()+gyarados.getBbHeight()*.5);
       check(h,gyarados.isInWater()&&Double.isFinite(top)&&Math.abs(depth-1)<.3,"Awake Gyarados must remain in water at body-center depth one block: "+depth
           +" pos="+gyarados.position()+" inWater="+gyarados.isInWater()+" sleep="+gyarados.isSleeping()
           +" surface="+top+" target="+gyarados.getNavigation().getTargetPos()+" navDone="+gyarados.getNavigation().isDone()
           +" motion="+gyarados.getDeltaMovement()+" phase="+gyarados.naturalCycle().phase()
           +" active="+gyarados.naturalBehaviorActive()+" block="+gyarados.blockPosition()
           +" blockFluid="+level.getFluidState(gyarados.blockPosition())+" time="+level.getDayTime());
       h.succeed();
      });
     },40,"Aquatic sleepers must wake at dawn");
    },220,()->"A water visitor must return to sleep after combat while it is still night; sleeping="+dratini.isSleeping()+" night="+dratini.isNightResting()+" target="+dratini.getTarget()+" pos="+dratini.position()+" velocity="+dratini.getDeltaMovement()+" restY="+WaterMovementGoal.waterRestY(dratini)+" time="+h.getLevel().getDayTime());
   },180,()->"The woken water visitor must physically retaliate in water; pos="+dratini.position()+" target="+dratini.getTarget()+" sleep="+dratini.isSleeping()+" attacker="+attacker.position()+" health="+attacker.getHealth());
  },500,"Permanent swimmers must rest in water and water visitors must settle at the bottom");
 }
 @GameTest(template="test_arena",batch="water_bottom",timeoutTicks=900)
 public static void bottomPreferringSpeciesNavigateCloseToTheChangingLakeFloor(GameTestHelper h){
  h.getLevel().setDayTime(1000);
  arena(h);
  for(int x=2;x<=27;x++)for(int z=2;z<=21;z++){
   int floor=(x>=13&&z>=9&&z<=15)?2:0;
   for(int y=1;y<=10;y++)h.setBlock(new BlockPos(x,y,z),Blocks.WATER);
   if(floor==0)h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
   else {h.setBlock(new BlockPos(x,1,z),Blocks.STONE);h.setBlock(new BlockPos(x,2,z),Blocks.STONE);}
  }
  var shellder=spawn(h,"shellder",7,8,10);var krabby=spawn(h,"krabby",22,8,10);
  // Krabby is also a land/water visitor and starts in its land cooldown.
  // This probe is specifically for bottom navigation, so place it in its
  // established submerged phase instead of exercising the separate exit rule.
  var cycle=new CompoundTag();krabby.addAdditionalSaveData(cycle);
  cycle.putInt("NaturalBehaviorStartDelay",0);cycle.putString("NaturalBehaviorPhase","SUBMERGED");
  cycle.putInt("NaturalBehaviorTicks",0);krabby.readAdditionalSaveData(cycle);
  double originY=h.absolutePos(BlockPos.ZERO).getY();
  awaitBottom(h,shellder,krabby,originY,650,0,()->{
   check(h,h.getLevel().noCollision(shellder,shellder.getBoundingBox()),"Shellder must avoid the solid bottom while seeking it");
   check(h,h.getLevel().noCollision(krabby,krabby.getBoundingBox()),"Krabby must avoid the solid bottom while seeking it");
   double shellderLocalY=shellder.getY()-originY,krabbyLocalY=krabby.getY()-originY;
   check(h,shellderLocalY>=1.0D&&krabbyLocalY>=3.0D,"Bottom-preferring swimmers must stay above the lake bed; shellderLocalY="+shellderLocalY+" krabbyLocalY="+krabbyLocalY);
   h.succeed();
  });
 }
 @GameTest(template="test_arena",batch="night_mixed",timeoutTicks=1000)
 public static void realNightLandingHoverWakeRetaliationReturnAndDawn(GameTestHelper h){
  arena(h);var level=h.getLevel();level.setDayTime(1000);
  // Keep sleeping hoverers clear of the wide grounded sleepers: collision
  // pushing is physical displacement, not autonomous sleep travel.
  for(int ax=0;ax<32;ax++)for(int az=26;az<44;az++){
   h.setBlock(new BlockPos(ax,0,az),Blocks.STONE);
   for(int ay=1;ay<20;ay++)h.setBlock(new BlockPos(ax,ay,az),Blocks.AIR);
  }
  var ground=spawn(h,"bulbasaur",3,1,3);
  var mixed=new ArrayList<MinemonCreature>();var air=new ArrayList<MinemonCreature>();
  int x=5;
  // Land-resting flyers include ground/air cycle species and species with an approved night landing pose.
  var mixedSpecies=List.of("charizard","scyther","dragonite","pidgey","pidgeotto","pidgeot","venomoth","aerodactyl","butterfree","beedrill","spearow","fearow","articuno","zapdos","moltres","metapod","kakuna","geodude","kadabra","alakazam");
  for(int i=0;i<mixedSpecies.size();i++){
   String slug=mixedSpecies.get(i);x=3+(i%5)*6;int z=5+(i/5)*5;
   var m=spawn(h,slug,x,7,z);
   var tag=new CompoundTag();m.addAdditionalSaveData(tag);tag.putInt("NaturalBehaviorStartDelay",0);
   tag.putString("NaturalBehaviorPhase","AIR");tag.putInt("NaturalBehaviorTicks",1234);m.readAdditionalSaveData(tag);mixed.add(m);
  }
  x=3;
  // These airborne species keep their one-block hover while asleep.
  for(String slug:List.of("koffing","weezing","magnemite","magneton","mewtwo","mew")){
   int airX=3+(air.size()%5)*6,airZ=29+(air.size()/5)*8;air.add(spawn(h,slug,airX,7,airZ));
  }
  h.runAfterDelay(5,()->{
   level.setDayTime(18000);
   await(h,()->ground.isSleeping()&&mixed.stream().allMatch(m->m.isSleeping()&&m.isOnGround())
       &&air.stream().allMatch(m->m.isSleeping()&&!m.isOnGround()&&Math.abs(m.getY()-m.naturalSurface()-1)<.2),()->{
    var clocks=new HashMap<MinemonCreature,Integer>();mixed.forEach(m->clocks.put(m,m.naturalCycle().ticks()));
    var positions=new HashMap<MinemonCreature,net.minecraft.world.phys.Vec3>();air.forEach(m->positions.put(m,m.position()));
    h.runAfterDelay(30,()->{
     mixed.forEach(m->check(h,m.naturalCycle().ticks()==clocks.get(m),"Night must preserve cycle progress: "+m.getSpeciesSlug()));
     air.forEach(m->check(h,m.position().subtract(positions.get(m)).horizontalDistanceSqr()<.001,"Sleeping hover must stop horizontal travel"));
     var attacker=h.spawn(EntityType.COW,new BlockPos(4,1,3));attacker.setNoAi(true);float health=attacker.getHealth();
     check(h,ground.hurt(DamageSource.mobAttack(attacker),1),"Attack must damage sleeping creature");
     check(h,!ground.isSleeping()&&ground.getTarget()==attacker,"Attack must wake and retain aggressor immediately");
     await(h,()->attacker.getHealth()<health,()->{
      attacker.discard();
      await(h,ground::isSleeping,()->{
       level.setDayTime(1000);
       await(h,()->!ground.isSleeping()&&mixed.stream().noneMatch(MinemonCreature::isSleeping)&&air.stream().noneMatch(MinemonCreature::isSleeping),()->{
        check(h,ground.getTarget()==null,"Dawn must preserve neutrality");
        mixed.forEach(m->check(h,m.naturalCycle().ticks()>=clocks.get(m),"Dawn must resume saved clock"));
        awaitFlightResume(h,mixed,clocks,200,new ArrayList<>());
       },30,"All night sleepers must wake at dawn");
      },180,"After combat, a creature must return to sleep while still night");
     },150,"Woken creature must physically retaliate using melee AI");
    });
   },350,()->"Night must settle ground/mixed creatures on land and air-only creatures in a one-block hover; mixed="
       +mixed.stream().map(m->m.getSpeciesSlug()+"{sleep="+m.isSleeping()+",ground="+m.isOnGround()+",night="+m.isNightResting()+",pos="+m.position()+",nav="+m.getNavigation().getTargetPos()+"}").toList()
       +" air="+air.stream().map(m->m.getSpeciesSlug()+"{sleep="+m.isSleeping()+",ground="+m.isOnGround()+",night="+m.isNightResting()+",pos="+m.position()+"}").toList());
  });
 }
 @GameTest(template="test_arena",batch="night_eevee_line",timeoutTicks=2400)
 public static void eeveeLineSleepsWakesRetaliatesReturnsAndWakesAtDawn(GameTestHelper h){
  arena(h);var level=h.getLevel();level.setDayTime(18000);
  // Combat recovery includes normal roaming during its grace period; seal the arena.
  for(int x=0;x<32;x++)for(int z=0;z<24;z++)if(x==0||x==31||z==0||z==23)
   for(int y=1;y<=6;y++)h.setBlock(new BlockPos(x,y,z),Blocks.STONE);
  var sleepers=new ArrayList<MinemonCreature>();
  int[][] positions={{4,3},{10,3},{16,3},{22,3}};
  var slugs=List.of("eevee","vaporeon","jolteon","flareon");
  for(int i=0;i<slugs.size();i++)sleepers.add(spawn(h,slugs.get(i),positions[i][0],1,positions[i][1]));
  await(h,()->sleepers.stream().allMatch(m->m.isSleeping()&&m.isOnGround()),()->{
   runEeveeLineCombat(h,sleepers,0,()->{
    level.setDayTime(1000);
    await(h,()->sleepers.stream().noneMatch(MinemonCreature::isSleeping),h::succeed,40,
        "Eevee and its evolutions must resume their normal behavior at dawn");
   });
  },500,"Eevee, Vaporeon on land, Jolteon, and Flareon must settle into ground sleep");
 }
 private static void runEeveeLineCombat(GameTestHelper h,List<MinemonCreature> sleepers,int index,Runnable done){
  if(index>=sleepers.size()){done.run();return;}
  var mob=sleepers.get(index);var origin=h.absolutePos(BlockPos.ZERO);
  int x=(int)Math.floor(mob.getX()-origin.getX()),z=(int)Math.floor(mob.getZ()-origin.getZ());
  var attacker=h.spawn(EntityType.COW,new BlockPos(x+1,1,z));attacker.setNoAi(true);float health=attacker.getHealth();
  check(h,mob.hurt(DamageSource.mobAttack(attacker),1)&&!mob.isSleeping()&&mob.getTarget()==attacker,
      mob.getSpeciesSlug()+" must wake and retain its attacker");
  await(h,()->attacker.getHealth()<health,()->{
   attacker.discard();
   await(h,mob::isSleeping,()->runEeveeLineCombat(h,sleepers,index+1,done),240,
       ()->mob.getSpeciesSlug()+" must return to sleep after combat while it is still night: pos="+mob.position()+" ground="+mob.isOnGround()+" night="+mob.isNightResting()+" target="+mob.getTarget()+" time="+h.getLevel().getDayTime()+" collision="+h.getLevel().noCollision(mob,mob.getBoundingBox()));
  },180,mob.getSpeciesSlug()+" must retaliate with the existing combat AI");
 }
 @GameTest(template="test_arena",batch="water_speed",timeoutTicks=240)
 public static void nonAquaticCreatureEscapesToReachableShore(GameTestHelper h){
  arena(h);
  for(int x=4;x<=14;x++)for(int z=4;z<=18;z++){
   h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
   for(int y=1;y<=4;y++)h.setBlock(new BlockPos(x,y,z),Blocks.WATER);
  }
  // Shore is at the water surface, with a dry step the amphibious path can reach.
  for(int z=0;z<24;z++)for(int x=0;x<32;x++)if(x<4||x>14||z<4||z>18)
   for(int y=1;y<=4;y++)h.setBlock(new BlockPos(x,y,z),Blocks.STONE);
  var mob=spawn(h,"bulbasaur",7,3,10);
  h.runAfterDelay(5,()->{
   check(h,mob.isInWater(),"Escape fixture must begin immersed");
   for(int tick=20;tick<=180;tick+=20){final int sample=tick;h.runAfterDelay(tick,()->com.mojang.logging.LogUtils.getLogger().info("ESCAPE TRACE tick={} pos={} velocity={} water={} nav={} done={} control={} wanted={}",sample,mob.position(),mob.getDeltaMovement(),mob.isInWater(),mob.getNavigation().getTargetPos(),mob.getNavigation().isDone(),mob.getMoveControl().getClass(),mob.getMoveControl().hasWanted()));}
   await(h,()->!mob.isInWater()&&mob.isOnGround(),()->{
    check(h,h.getLevel().noCollision(mob,mob.getBoundingBox()),"Escape must respect solid shore collisions");h.succeed();
   },200,"Non-aquatic creature must physically leave the pool for its reachable shore");
  });
 }
 @GameTest(template="test_arena",batch="water_speed_reference",timeoutTicks=60)
 public static void realPlayerTravelMatchesSwimAndQuarterFasterEscape(GameTestHelper h){
  arena(h);
  for(int x=1;x<30;x++)for(int z=1;z<23;z++)for(int y=1;y<=6;y++)h.setBlock(new BlockPos(x,y,z),Blocks.WATER);
  var player=new net.minecraftforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"SwimReference"));
  var p=h.absolutePos(new BlockPos(4,3,4));player.moveTo(p.getX()+.5,p.getY(),p.getZ()+.5,0,0);
  player.setNoGravity(true);player.setSprinting(false);
  var swimmer=spawn(h,"goldeen",10,3,4);var visitor=spawn(h,"vaporeon",16,3,4);var escape=spawn(h,"bulbasaur",22,3,4);
  for(var m:List.of(swimmer,visitor,escape)){m.goalSelector.removeAllGoals();m.targetSelector.removeAllGoals();}
  h.runAfterDelay(3,()->{
   double[] measured=new double[4];var entities=List.<net.minecraft.world.entity.LivingEntity>of(player,swimmer,visitor,escape);
   for(int i=0;i<entities.size();i++){
    var e=entities.get(i);e.setNoGravity(true);e.setYRot(0);e.setXRot(0);e.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    double start=0;
    for(int tick=0;tick<80;tick++){
     e.baseTick();check(h,e.isInWater(),"Reference must remain fully immersed: "+i);
     if(e instanceof MinemonCreature m){
      var target=new BlockPos(m.getX(),m.getY(),m.getZ()+10);
      m.getNavigation().moveTo(target.getX()+.5,target.getY(),target.getZ()+.5,.85);
      m.getNavigation().tick();
      m.getMoveControl().setWantedPosition(m.getX(),m.getY(),m.getZ()+10,.85);
      m.getMoveControl().tick();
      m.travel(new net.minecraft.world.phys.Vec3(m.xxa*.98,m.yya*.98,m.zza*.98));
     }else e.travel(new net.minecraft.world.phys.Vec3(0,0,.98));
     if(tick==39)start=e.getZ();
    }
    measured[i]=(e.getZ()-start)/40.0D;
   }
   com.mojang.logging.LogUtils.getLogger().info("SWIM REFERENCE blocks/tick player={} goldeen={} vaporeon={} escape={} ratio={}",measured[0],measured[1],measured[2],measured[3],measured[3]/measured[0]);
   check(h,measured[0]>.09&&measured[0]<.105,"Player baseline must be ordinary non-sprint swimming");
   check(h,Math.abs(measured[1]/measured[0]-1)<.03&&Math.abs(measured[2]/measured[0]-1)<.03,"Aquatic and visitor controllers must match actual player travel");
   check(h,Math.abs(measured[3]/measured[0]-1.25)<.03,"Escape must physically move 25% faster than actual player travel");
   h.succeed();
  });
 }

}
