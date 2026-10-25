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
import net.minecraft.world.level.GameRules;
import net.minecraftforge.gametest.*;
import java.util.*;
import java.util.function.BooleanSupplier;

@GameTestHolder(Minemon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NightAndSwimmingTests {
 private static void check(GameTestHelper h,boolean ok,String why){if(!ok)h.fail(why);}
 private static void await(GameTestHelper h,BooleanSupplier condition,Runnable next,int ticks,String why){
  if(condition.getAsBoolean()){next.run();return;}
  if(ticks<=0){h.getLevel().setDayTime(1000);h.fail(why);return;}
  h.runAfterDelay(5,()->await(h,condition,next,ticks-5,why));
 }
 private static void arena(GameTestHelper h){
  for(int x=0;x<32;x++)for(int z=0;z<24;z++){
   h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
   for(int y=1;y<20;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
  }
 }
 private static MinemonCreature spawn(GameTestHelper h,String slug,int x,int y,int z){return h.spawn(ModEntities.SPECIES.get(slug).get(),new BlockPos(x,y,z));}
 @GameTest(template="test_arena",batch="night_rest",timeoutTicks=1000)
 public static void realNightLandingHoverWakeRetaliationReturnAndDawn(GameTestHelper h){
  arena(h);var level=h.getLevel();level.setDayTime(1000);
  level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,level.getServer());
  var ground=spawn(h,"bulbasaur",3,1,3);
  var mixed=new ArrayList<MinemonCreature>();var air=new ArrayList<MinemonCreature>();
  int x=5;
  for(String slug:List.of("charizard","scyther","dragonite")){
   var m=spawn(h,slug,x,7,8);x+=8;
   var tag=new CompoundTag();m.addAdditionalSaveData(tag);tag.putInt("NaturalBehaviorStartDelay",0);
   tag.putString("NaturalBehaviorPhase","AIR");tag.putInt("NaturalBehaviorTicks",1234);m.readAdditionalSaveData(tag);mixed.add(m);
  }
  x=3;
  for(String slug:List.of("pidgey","gastly","koffing","weezing")){air.add(spawn(h,slug,x,7,17));x+=7;}
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
        await(h,()->mixed.stream().allMatch(m->!m.isOnGround()&&m.naturalCycle().ticks()>clocks.get(m)),()->{
         level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(true,level.getServer());h.succeed();
        },200,"Mixed fliers must resume their paused airborne phase after dawn");
       },30,"All night sleepers must wake at dawn");
      },180,"After combat, a creature must return to sleep while still night");
     },150,"Woken creature must physically retaliate using melee AI");
    });
   },350,"Night must settle ground/mixed creatures on land and air-only creatures in a one-block hover");
  });
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
