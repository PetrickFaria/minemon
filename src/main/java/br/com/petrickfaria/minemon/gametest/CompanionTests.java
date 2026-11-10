package br.com.petrickfaria.minemon.gametest;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.entity.CombatGeometry;
import br.com.petrickfaria.minemon.entity.NaturalMovementGoals;
import br.com.petrickfaria.minemon.entity.WaterMovementGoal;
import br.com.petrickfaria.minemon.registry.ModEntities;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=br.com.petrickfaria.minemon.Minemon.MOD_ID)
@GameTestHolder(br.com.petrickfaria.minemon.Minemon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CompanionTests {
 private static final java.util.Set<ServerPlayer> TEST_PLAYERS=new java.util.HashSet<>();
 @net.minecraftforge.eventbus.api.SubscribeEvent public static void tickPlayers(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
  if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
  TEST_PLAYERS.removeIf(net.minecraft.world.entity.Entity::isRemoved);
  // Real connections call doTick; the dummy transport used by these tests does not.
  for(ServerPlayer p:TEST_PLAYERS)p.doTick();
 }
 private static void check(GameTestHelper h,boolean condition,String message) { if (!condition) h.fail(message); }
 private static void keepCombatTargetAlive(net.minecraft.world.entity.LivingEntity target) {
  target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100.0D);
  target.setHealth(100.0F);
 }
 private static String state(MinemonCreature b, net.minecraft.world.entity.LivingEntity target) { return " bAlive="+b.isAlive()+" bRemoved="+b.isRemoved()+" target="+b.getTarget()+" canAttack="+b.canAttack(target)+" dist="+b.distanceTo(target)+" pos="+b.position()+" targetPos="+target.position()+" alive="+target.isAlive()+" health="+target.getHealth()+" owner="+b.getOwner()+" ownerVictim="+(b.getOwner()==null?null:b.getOwner().getLastHurtMob())+" ownerAttacker="+(b.getOwner()==null?null:b.getOwner().getLastHurtByMob())+" ownerStamp="+(b.getOwner()==null?-1:b.getOwner().getLastHurtByMobTimestamp())+" orderedSit="+b.isOrderedToSit()+" sit="+b.isInSittingPose()+" navDone="+b.getNavigation().isDone(); }
 private static void traceMovement(GameTestHelper h,String label,MinemonCreature b,int elapsed,int until) {
  com.mojang.logging.LogUtils.getLogger().info("MINEMON MOVEMENT TRACE "+label+" sample="+elapsed
      +" entityTick="+b.tickCount+" pos="+b.position()+" velocity="+b.getDeltaMovement()
      +" alive="+b.isAlive()+" removed="+b.isRemoved()+" noAI="+b.isNoAi()+" noGravity="+b.isNoGravity()
      +" sleeping="+b.isSleeping()+" nightRest="+b.isNightResting()+" wantsLanding="+b.wantsNaturalLanding()
      +" flying="+b.isFlyingSpecies()+" naturalActive="+b.naturalBehaviorActive()
      +" grounded="+b.isOnGround()+" water="+b.isInWater()
      +" speed="+b.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
      +" health="+b.getHealth()+" target="+b.getTarget()+" navDone="+b.getNavigation().isDone()
      +" navTarget="+b.getNavigation().getTargetPos());
  if(elapsed<until)h.runAfterDelay(20,()->traceMovement(h,label,b,elapsed+20,until));
 }
 private static void floor(GameTestHelper h) { for(int x=0;x<16;x++)for(int z=0;z<16;z++){h.setBlock(new BlockPos(x,0,z),net.minecraft.world.level.block.Blocks.STONE);for(int y=1;y<6;y++)h.setBlock(new BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.AIR);} }
 private static MinemonCreature spawn(GameTestHelper h) {
  floor(h);
  return h.spawn(ModEntities.BULBASAUR.get(),new BlockPos(5,1,5));
 }
 private static ServerPlayer player(GameTestHelper h) {
  floor(h);
  ServerPlayer p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"MinemonTester"),null);
  p.connection=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"DummyConnection")).connection;
  h.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
  p.setGameMode(GameType.SURVIVAL);p.tickCount=100;
  BlockPos pos=h.absolutePos(new BlockPos(7,1,5));
  p.moveTo(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,0,0);
  h.getLevel().addNewPlayer(p);TEST_PLAYERS.add(p);
  return p;
 }
 @GameTest(template="test_arena",timeoutTicks=300)
 public static void wildRetaliatesAgainstPlayer(GameTestHelper h) {
  MinemonCreature b=spawn(h); ServerPlayer p=player(h);float health=p.getHealth();
  traceMovement(h,"player-retaliation",b,0,180);
  check(h,b.canAttackType(EntityType.PLAYER),"Player entity type must be a valid retaliation target");
  float wildHealth=b.getHealth();
  b.setNoAi(true);h.runAfterDelay(80,()->{b.setNoAi(false);b.getNavigation().stop();
   check(h,b.hurt(DamageSource.playerAttack(p),12.0F),"A direct melee hit must reach the wild Pokémon damage flow");
   check(h,b.getHealth()==wildHealth-1.0F&&b.getTarget()==null,"A player hit must deal one HP and must not make the wild Pokémon retaliate");
   check(h,p.getHealth()==health,"A wild Pokémon must not retaliate against a player hit");p.discard();h.succeed();});
 }
 @GameTest(template="test_arena",timeoutTicks=300)
 public static void wildRetaliatesAgainstCreature(GameTestHelper h) {
  MinemonCreature b=spawn(h);Cow c=h.spawn(EntityType.COW,new BlockPos(14,1,5));c.setNoAi(true);float health=c.getHealth();
  b.hurt(DamageSource.mobAttack(c),1);
  h.runAfterDelay(120,()->{check(h,c.getHealth()<health,"Wild Pokémon must retaliate with a ranged projectile while maintaining safe distance");h.succeed();});
 }
 @GameTest(template="test_arena",timeoutTicks=100)
 public static void passiveUntilProvoked(GameTestHelper h) {
  MinemonCreature b=spawn(h);h.spawn(EntityType.COW,new BlockPos(7,1,5));
  h.runAfterDelay(40,()->{check(h,b.getTarget()==null,"Unprovoked creature must remain peaceful");h.succeed();});
 }
 @GameTest(template="test_arena",timeoutTicks=300)
 public static void ownerProtectedAndAssisted(GameTestHelper h) {
  MinemonCreature b=spawn(h);ServerPlayer p=player(h);b.tame(p);b.setOrderedToSit(false);
  b.setNoAi(true);h.runAfterDelay(80,()->{b.setNoAi(false);b.getNavigation().stop();b.hurt(DamageSource.playerAttack(p),1);check(h,b.getTarget()!=p,"Never retaliate against owner");
  Cow c=h.spawn(EntityType.COW,new BlockPos(14,1,5));c.setNoAi(true);p.attack(c);check(h,p.getLastHurtMob()==c,"Owner attack must record its victim");float health=c.getHealth();
  h.runAfterDelay(100,()->{check(h,c.getHealth()<health,"Owner attack assistance must damage target"+state(b,c));p.discard();h.succeed();});});
 }
 @GameTest(template="test_arena",timeoutTicks=300)
 public static void ownerDefense(GameTestHelper h) {
  MinemonCreature b=spawn(h);ServerPlayer p=player(h);b.tame(p);b.setOrderedToSit(false);
  Cow c=h.spawn(EntityType.COW,new BlockPos(14,1,5));c.setNoAi(true);float health=c.getHealth();b.setNoAi(true);h.runAfterDelay(80,()->{b.setNoAi(false);b.getNavigation().stop();check(h,p.hurt(DamageSource.mobAttack(c),1),"Owner must accept damage after spawn protection");p.setInvulnerable(true);
  com.mojang.logging.LogUtils.getLogger().info("MINEMON DEFENSE AFTER HIT "+state(b,c)+" goalCanUse="+new net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal(b).canUse()+" sight="+b.getSensing().hasLineOfSight(c)+" wants="+b.wantsToAttack(c,p));
  h.runAfterDelay(5,()->com.mojang.logging.LogUtils.getLogger().info("MINEMON DEFENSE AFTER 5 TICKS "+state(b,c)));
  h.runAfterDelay(100,()->{check(h,c.getHealth()<health,"Companion must defend attacked owner"+state(b,c));p.discard();h.succeed();});});
 }
 @GameTest(template="test_arena",timeoutTicks=300)
 public static void ownerDefenseReachesFollowingSittingFreeAndSleepingCompanions(GameTestHelper h) {
  ServerPlayer p=player(h);Cow aggressor=h.spawn(EntityType.COW,new BlockPos(14,1,5));aggressor.setNoAi(true);
  // Several defenders can now land real shots while this fixture is measuring
  // target acquisition; keep the attacker alive for the full focus assertion.
  aggressor.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000.0D);
  aggressor.setHealth(1000.0F);
  MinemonCreature[] team=new MinemonCreature[4];
  BlockPos[] positions={new BlockPos(6,1,3),new BlockPos(10,1,5),new BlockPos(6,1,7),new BlockPos(5,1,5)};
  for(int i=0;i<team.length;i++){
   team[i]=h.spawn(ModEntities.BULBASAUR.get(),positions[i]);team[i].tame(p);
   if(i==1)team[i].setOrderedToSit(true);
   if(i==2)team[i].setFreeMode(true);
   if(i==3)team[i].setSleeping(true);
  }
  var sittingPosition=team[1].position();
  h.runAfterDelay(80,()->{
   p.setInvulnerable(false);
   p.invulnerableTime=0;
   check(h,p.hurt(DamageSource.mobAttack(aggressor),1),"Trainer must receive the fixture attack");
   for(MinemonCreature companion:team)
    check(h,companion.getTarget()==aggressor,"Every behavior mode must acquire the trainer's attacker: "+state(companion,aggressor));
   check(h,!team[3].isSleeping(),"A sleeping companion must wake for trainer defense");
   h.runAfterDelay(3,()->{
    check(h,team[1].isOrderedToSit()&&team[1].isCombatPose(),"A sitting defender must retain Sentar and use its combat pose");
    check(h,team[1].getX()==sittingPosition.x&&team[1].getZ()==sittingPosition.z,"A sitting defender must not pursue or retreat");
    h.runAfterDelay(100,()->{
     for(MinemonCreature companion:team)
      check(h,companion.getTarget()==aggressor,"Defenders must retain focus through the attack cooldown: "+state(companion,aggressor));
     p.discard();h.succeed();
    });
   });
  });
 }
 @GameTest(template="test_arena",timeoutTicks=180)
 public static void sittingCompanionAttacksAuthorizedTargetWithoutMoving(GameTestHelper h) {
  floor(h);
  ServerPlayer owner=player(h);
  BlockPos ownerPosition=h.absolutePos(new BlockPos(1,1,1));
  owner.moveTo(ownerPosition.getX()+.5D,ownerPosition.getY(),ownerPosition.getZ()+.5D,0,0);
  MinemonCreature pokemon=h.spawn(ModEntities.SPECIES.get("pikachu").get(),new BlockPos(8,1,5));
  pokemon.tame(owner);pokemon.setPokemonLevel(10);pokemon.setOrderedToSit(true);
  pokemon.setBlackListOverride(new net.minecraft.resources.ResourceLocation("minecraft","cow"),true);
  Cow target=h.spawn(EntityType.COW,new BlockPos(5,1,5));target.setNoAi(true);keepCombatTargetAlive(target);
  var startingPosition=pokemon.position();float startingHealth=target.getHealth();
  h.runAfterDelay(120,()->{
   String state=" target="+pokemon.getTarget()+" canAttack="+pokemon.canAttack(target)+" canFire="+pokemon.canFireAt(target)
       +" sleeping="+pokemon.isSleeping()+" orderedSit="+pokemon.isOrderedToSit()+" pose="+pokemon.isCombatPose()
       +" targetHealth="+target.getHealth()+" position="+pokemon.position()+" targetPosition="+target.position();
   check(h,pokemon.getTarget()==target,"A sitting Pokémon must acquire its authorized nearby target:"+state);
   check(h,target.getHealth()<startingHealth,"A sitting Pokémon must fire at the authorized target:"+state);
   check(h,pokemon.isOrderedToSit()&&pokemon.isCombatPose(),"Attacking must preserve the Sentar order and idle combat pose:"+state);
   check(h,pokemon.position().distanceTo(startingPosition)<.05D,"A sitting attacker must not chase or retreat:"+state);
   owner.discard();h.succeed();
  });
 }
 @GameTest(template="test_arena",timeoutTicks=200)
 public static void pidgeyAttacksAndDescendsToItsCombatFlightLimit(GameTestHelper h) {
  floor(h);
  ServerPlayer owner=player(h);
  BlockPos ownerPosition=h.absolutePos(new BlockPos(1,1,1));
  owner.moveTo(ownerPosition.getX()+.5D,ownerPosition.getY(),ownerPosition.getZ()+.5D,0,0);
  MinemonCreature pidgey=h.spawn(ModEntities.SPECIES.get("pidgey").get(),new BlockPos(5,4,5));
  pidgey.tame(owner);pidgey.setPokemonLevel(10);
  check(h,pidgey.isFlyingSpecies(),"Pidgey must use the flying behavior profile");
  Zombie target=h.spawn(EntityType.ZOMBIE,new BlockPos(9,1,5));target.setNoAi(true);keepCombatTargetAlive(target);
  double surface=pidgey.naturalSurface();pidgey.setPos(pidgey.getX(),surface+3.0D,pidgey.getZ());
  float startingHealth=target.getHealth();double initialDistance=CombatGeometry.horizontalCenterDistanceSquared(pidgey,target);
  awaitPidgeyCombat(h,pidgey,owner,target,startingHealth,initialDistance,0);
 }
 @GameTest(template="test_arena",timeoutTicks=150)
 public static void rangedCompanionRepositionsDuringCooldownAndFiresAgainInRange(GameTestHelper h) {
  floor(h);
  ServerPlayer owner=player(h);
  BlockPos ownerPosition=h.absolutePos(new BlockPos(1,1,1));
  owner.moveTo(ownerPosition.getX()+.5D,ownerPosition.getY(),ownerPosition.getZ()+.5D,0,0);
  MinemonCreature pokemon=h.spawn(ModEntities.SPECIES.get("pikachu").get(),new BlockPos(9,1,5));
  pokemon.tame(owner);pokemon.setPokemonLevel(10);
  Cow target=h.spawn(EntityType.COW,new BlockPos(5,1,5));target.setNoAi(true);target.setNoGravity(true);keepCombatTargetAlive(target);
  pokemon.focusTrainerDefenseTarget(target);
  check(h,pokemon.fireElementalProjectile(target),"The close-range fixture shot must fire and start the cooldown");
  var start=pokemon.position();double startingDistance=pokemon.distanceTo(target);float initialHealth=target.getHealth();
  h.runAfterDelay(30,()->{
   check(h,!pokemon.combatReady(),"The defensive reposition must still occur during the shot cooldown");
   check(h,target.getHealth()<initialHealth,"The initial fixture shot must hit before the cooldown comparison");
   double movedAway= pokemon.position().subtract(start).dot(start.subtract(target.position()).normalize());
   check(h,movedAway>1.0D&&pokemon.distanceTo(target)>startingDistance,
       "The companion must retreat while its next shot recharges; start="+startingDistance+", now="+pokemon.distanceTo(target)+", movedAway="+movedAway+", displacement="+pokemon.position().distanceTo(start)+", target="+target.position()+", companion="+pokemon.position()+", targetRef="+pokemon.getTarget()+", canAttack="+pokemon.canAttack(target)+", noAI="+pokemon.isNoAi()+", sleeping="+pokemon.isSleeping()+", navDone="+pokemon.getNavigation().isDone()+", navTarget="+pokemon.getNavigation().getTargetPos());
   float healthDuringCooldown=target.getHealth();
   h.runAfterDelay(85,()->{
    check(h,target.getHealth()<healthDuringCooldown,
        "The companion must fire again after cooldown while the target remains within maximum range");
    owner.discard();h.succeed();
   });
  });
 }
 @GameTest(template="test_arena",timeoutTicks=180)
 public static void charmanderRetreatsAndAttacksWhileAnAdvancingZombiePressuresIt(GameTestHelper h) {
  floor(h);ServerPlayer owner=player(h);owner.setGameMode(GameType.CREATIVE);
  BlockPos ownerPosition=h.absolutePos(new BlockPos(1,1,1));owner.moveTo(ownerPosition.getX()+.5D,ownerPosition.getY(),ownerPosition.getZ()+.5D,0,0);
  MinemonCreature pokemon=h.spawn(ModEntities.SPECIES.get("charmander").get(),new BlockPos(7,1,8));
  pokemon.tame(owner);pokemon.setPokemonLevel(10);pokemon.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200.0D);pokemon.setHealth(200.0F);
  pokemon.setBlackListOverride(new net.minecraft.resources.ResourceLocation("minecraft","zombie"),false);
  Zombie target=h.spawn(EntityType.ZOMBIE,new BlockPos(9,1,8));keepCombatTargetAlive(target);target.setTarget(pokemon);
  pokemon.focusTrainerDefenseTarget(target);
  var pokemonStart=pokemon.position();var targetStart=target.position();float targetHealth=target.getHealth();
  h.onEachTick(()->{
   if(h.getLevel().getGameTime()%20==0)com.mojang.logging.LogUtils.getLogger().info(
       "MINEMON CHARMANDER RETREAT TRACE distance="+pokemon.distanceTo(target)
       +" pokemon="+pokemon.position()+" velocity="+pokemon.getDeltaMovement()
       +" navDone="+pokemon.getNavigation().isDone()+" navTarget="+pokemon.getNavigation().getTargetPos()
       +" zombie="+target.position()+" velocity="+target.getDeltaMovement()
       +" speed="+pokemon.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
  });
  h.runAfterDelay(100,()->{
   double pokemonTravel=pokemon.position().subtract(pokemonStart).horizontalDistance();
   double targetTravel=target.position().subtract(targetStart).horizontalDistance();
   check(h,pokemonTravel>2.0D,"Charmander must keep moving defensively while the zombie advances; travel="+pokemonTravel+" distance="+pokemon.distanceTo(target)+" navDone="+pokemon.getNavigation().isDone());
   check(h,targetTravel>1.0D,"The escape test must use a zombie that actually advanced; travel="+targetTravel);
   check(h,target.getHealth()<targetHealth,"Charmander must continue attacking during defensive movement; health="+target.getHealth());
   owner.discard();target.discard();h.succeed();
  });
 }
 @GameTest(template="test_arena",timeoutTicks=180)
 public static void charmanderStopsAtSafetyAndResumesWhenZombieCloses(GameTestHelper h) {
  floor(h);ServerPlayer owner=player(h);owner.setGameMode(GameType.CREATIVE);
  BlockPos ownerPosition=h.absolutePos(new BlockPos(1,1,1));owner.moveTo(ownerPosition.getX()+.5D,ownerPosition.getY(),ownerPosition.getZ()+.5D,0,0);
  MinemonCreature pokemon=h.spawn(ModEntities.SPECIES.get("charmander").get(),new BlockPos(7,1,8));
  pokemon.tame(owner);pokemon.setPokemonLevel(10);pokemon.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200.0D);pokemon.setHealth(200.0F);
  pokemon.setBlackListOverride(new net.minecraft.resources.ResourceLocation("minecraft","zombie"),false);
  Zombie target=h.spawn(EntityType.ZOMBIE,new BlockPos(9,1,8));keepCombatTargetAlive(target);target.setTarget(pokemon);pokemon.focusTrainerDefenseTarget(target);
  awaitCharmanderSafety(h,pokemon,target,owner,0);
 }
 private static void awaitCharmanderSafety(GameTestHelper h,MinemonCreature pokemon,Zombie target,ServerPlayer owner,int elapsed){
  double safeDistance=Math.max(1.0D,Math.min(7.0D,pokemon.attackRange()-1.0D));
  double distance=pokemon.distanceTo(target);
  // Poll the safety condition instead of assuming the retreat ends at a fixed
  // tick. The test keeps its original 180-tick limit and reserves the final
  // 27 ticks for the stop and re-approach checks below.
  if(distance<safeDistance&&elapsed<138){
   h.runAfterDelay(1,()->awaitCharmanderSafety(h,pokemon,target,owner,elapsed+1));return;
  }
  if(distance<safeDistance){
   h.fail("Charmander did not recover a safe distance before the test deadline; elapsed="+elapsed+" distance="+distance+" safe="+safeDistance+" pokemon="+pokemon.position()+" velocity="+pokemon.getDeltaMovement()+" navDone="+pokemon.getNavigation().isDone()+" navTarget="+pokemon.getNavigation().getTargetPos()+" zombie="+target.position()+" zombieVelocity="+target.getDeltaMovement());
   owner.discard();target.discard();return;
  }
  target.setNoAi(true);target.getNavigation().stop();target.setDeltaMovement(0,0,0);
  var safePosition=pokemon.position();
  h.runAfterDelay(12,()->{
   double drift=pokemon.position().subtract(safePosition).horizontalDistance();
   check(h,pokemon.getNavigation().isDone()&&drift<.35D,"Charmander should stop retreating after safety is restored; drift="+drift+" navDone="+pokemon.getNavigation().isDone());
   double dx=pokemon.getX()-target.getX(),dz=pokemon.getZ()-target.getZ(),length=Math.sqrt(dx*dx+dz*dz);
   target.moveTo(pokemon.getX()-dx/length*2.5D,pokemon.getY(),pokemon.getZ()-dz/length*2.5D,0,0);
   target.setNoAi(false);target.setTarget(pokemon);
   var beforeResume=pokemon.position();
   h.runAfterDelay(15,()->{
    double resumed=pokemon.position().subtract(beforeResume).horizontalDistance();
    check(h,resumed>.5D,"Charmander must immediately resume fleeing when the zombie enters the danger radius; moved="+resumed+" distance="+pokemon.distanceTo(target));
    owner.discard();target.discard();h.succeed();
   });
  });
 }
 @GameTest(template="test_arena",timeoutTicks=180)
 public static void pidgeyRetreatsAfterAttackingWithoutAFlightDash(GameTestHelper h) {
  checkAirRetreatHasNoDash(h,"pidgey",3.0D);
 }
 @GameTest(template="test_arena",timeoutTicks=180)
 public static void gastlyRetreatsAfterAttackingWithoutAFlightDash(GameTestHelper h) {
  checkAirRetreatHasNoDash(h,"gastly",1.0D);
 }
 private static void checkAirRetreatHasNoDash(GameTestHelper h,String slug,double altitude) {
  floor(h);ServerPlayer owner=player(h);owner.setGameMode(GameType.CREATIVE);
  BlockPos ownerPosition=h.absolutePos(new BlockPos(1,1,1));owner.moveTo(ownerPosition.getX()+.5D,ownerPosition.getY(),ownerPosition.getZ()+.5D,0,0);
  MinemonCreature pokemon=h.spawn(ModEntities.SPECIES.get(slug).get(),new BlockPos(5,1+(int)altitude,8));
  pokemon.tame(owner);pokemon.setPokemonLevel(10);pokemon.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200.0D);pokemon.setHealth(200.0F);
  pokemon.setBlackListOverride(new net.minecraft.resources.ResourceLocation("minecraft","zombie"),false);
  Zombie target=h.spawn(EntityType.ZOMBIE,new BlockPos(8,1,8));target.setNoAi(true);keepCombatTargetAlive(target);target.setTarget(pokemon);
  pokemon.setPos(pokemon.getX(),pokemon.naturalSurface()+altitude,pokemon.getZ());
  pokemon.focusTrainerDefenseTarget(target);
  double health=target.getHealth();
  sampleAirRetreat(h,pokemon,target,owner,pokemon.position(),0.0D,0,130,health);
 }
 private static void sampleAirRetreat(GameTestHelper h,MinemonCreature pokemon,Zombie target,ServerPlayer owner,
                                     net.minecraft.world.phys.Vec3 previous,double maxStep,int elapsed,int duration,double initialHealth) {
  if(elapsed>=duration) {
   double distance=pokemon.position().subtract(previous).horizontalDistance();
   check(h,target.getHealth()<initialHealth,""+pokemon.getSpeciesSlug()+" must attack during the retreat test; health="+target.getHealth());
   check(h,maxStep<1.0D,""+pokemon.getSpeciesSlug()+" must not make an excessive one-tick flight dash; max step="+maxStep+" final step="+distance);
   owner.discard();target.discard();h.succeed();return;
  }
  h.runAfterDelay(1,()->{
   double step=pokemon.position().subtract(previous).horizontalDistance();
   sampleAirRetreat(h,pokemon,target,owner,pokemon.position(),Math.max(maxStep,step),elapsed+1,duration,initialHealth);
  });
 }
 @GameTest(template="test_arena",timeoutTicks=180)
 public static void pidgeyReturnsToItsNaturalFlightBandAfterCombat(GameTestHelper h) {
  floor(h);ServerPlayer owner=player(h);owner.setGameMode(GameType.CREATIVE);
  BlockPos ownerPosition=h.absolutePos(new BlockPos(1,1,1));owner.moveTo(ownerPosition.getX()+.5D,ownerPosition.getY(),ownerPosition.getZ()+.5D,0,0);
  MinemonCreature pidgey=h.spawn(ModEntities.SPECIES.get("pidgey").get(),new BlockPos(5,4,8));
  pidgey.tame(owner);pidgey.setPokemonLevel(10);pidgey.setBlackListOverride(new net.minecraft.resources.ResourceLocation("minecraft","zombie"),false);
  Zombie target=h.spawn(EntityType.ZOMBIE,new BlockPos(8,1,8));target.setNoAi(true);keepCombatTargetAlive(target);pidgey.focusTrainerDefenseTarget(target);
  h.runAfterDelay(45,()->{
   check(h,Math.abs(pidgey.getY()-pidgey.naturalSurface()-1.5D)<.15D,"Pidgey must reach the 1.5-block combat band before combat ends");
   target.discard();pidgey.setTarget(null);
   // The level 10 perception can reacquire hostiles from neighboring
   // GameTests. Disable only target goals while the natural-flight reset runs.
   pidgey.targetSelector.removeAllGoals();
   h.runAfterDelay(50,()->{
    double altitude=pidgey.getY()-pidgey.naturalSurface();
    check(h,pidgey.getTarget()==null&&altitude>=pidgey.flightMinimum()-.1D&&altitude<=pidgey.flightMaximum()+.1D,
        "Pidgey must resume its natural flight band after combat; altitude="+altitude+" natural="+pidgey.flightMinimum()+".."+pidgey.flightMaximum()
            +" target="+pidgey.getTarget()+" naturalActive="+pidgey.naturalBehaviorActive()+" phase="+pidgey.naturalCycle().phase()
            +" flying="+pidgey.isFlyingSpecies()+" following="+pidgey.isFollowingOwner()+" noAI="+pidgey.isNoAi()
            +" velocity="+pidgey.getDeltaMovement()+" nav="+pidgey.getNavigation().getTargetPos());
    owner.discard();h.succeed();
   });
  });
 }
 @GameTest(template="test_arena",timeoutTicks=160)
 public static void rangedCompanionAttacksAuthorizedSpiderWithinRange(GameTestHelper h) {
  floor(h);ServerPlayer owner=player(h);owner.setGameMode(GameType.CREATIVE);
  BlockPos ownerPosition=h.absolutePos(new BlockPos(1,1,1));
  owner.moveTo(ownerPosition.getX()+.5D,ownerPosition.getY(),ownerPosition.getZ()+.5D,0,0);
  MinemonCreature pokemon=h.spawn(ModEntities.SPECIES.get("pikachu").get(),new BlockPos(5,1,5));
  pokemon.tame(owner);pokemon.setPokemonLevel(10);
  pokemon.setBlackListOverride(new net.minecraft.resources.ResourceLocation("minecraft","spider"),false);
  Spider target=h.spawn(EntityType.SPIDER,new BlockPos(14,1,5));target.setNoAi(true);target.setNoGravity(true);keepCombatTargetAlive(target);
  float startingHealth=target.getHealth();pokemon.focusTrainerDefenseTarget(target);
  h.runAfterDelay(120,()->{
   check(h,pokemon.getTarget()==target&&pokemon.canAttack(target),
       "A domesticated Pokémon must retain an authorized spider target within its attack range");
   check(h,target.getHealth()<startingHealth,
       "A domesticated Pokémon must damage a spider from inside maximum range; health="+target.getHealth());
   owner.discard();h.succeed();
  });
 }
 @GameTest(template="test_arena",timeoutTicks=100)
 public static void retreatMatchesVanillaSpiderDisplacement(GameTestHelper h) {
  compareRetreatToSpider(h,false);
 }
 @GameTest(template="test_arena",timeoutTicks=100)
 public static void flyingRetreatMatchesVanillaSpiderDisplacement(GameTestHelper h) {
  compareRetreatToSpider(h,true);
 }
 private static void compareRetreatToSpider(GameTestHelper h,boolean airborne) {
  floor(h);ServerPlayer owner=player(h);
  owner.setGameMode(GameType.CREATIVE);
  BlockPos ownerPosition=h.absolutePos(new BlockPos(1,1,1));
  owner.moveTo(ownerPosition.getX()+.5D,ownerPosition.getY(),ownerPosition.getZ()+.5D,0,0);
  MinemonCreature pokemon=h.spawn(ModEntities.SPECIES.get(airborne?"pidgey":"pikachu").get(),new BlockPos(5,airborne?2:1,4));
  pokemon.tame(owner);pokemon.setPokemonLevel(10);pokemon.setNoAi(true);
  pokemon.setBlackListOverride(new net.minecraft.resources.ResourceLocation("minecraft","zombie"),false);
  Cow target=h.spawn(EntityType.COW,new BlockPos(4,1,4));
  target.setNoAi(true);target.setNoGravity(true);keepCombatTargetAlive(target);
  var reference=h.spawn(EntityType.SPIDER,new BlockPos(5,1,8));
  Cow spiderTarget=h.spawn(EntityType.COW,new BlockPos(14,1,8));
  spiderTarget.setNoAi(true);spiderTarget.setNoGravity(true);keepCombatTargetAlive(spiderTarget);
  // Retain the Spider's normal movement and melee goals. Pin its target to a
  // stationary cow on the same heading as the companion's retreat so both
  // measurements exercise ordinary, active path-following movement.
  reference.targetSelector.removeAllGoals();
  reference.setTarget(spiderTarget);
  // Explicitly request the Spider's ordinary entity path at vanilla's normal
  // navigation multiplier. A manually assigned target alone does not guarantee
  // that its melee goal has started navigation before the measurement window.
  if(!reference.getNavigation().moveTo(spiderTarget,1.0D))
   h.fail("Vanilla spider could not create a real path to the stationary reference target");
  h.runAfterDelay(5,()->{
   // Both subjects start at rest facing the measured +X direction. Without
   // this, the companion follows its owner during setup and the flying turn
   // controller spends the measurement reversing inherited momentum.
   pokemon.getNavigation().stop();pokemon.setDeltaMovement(0,0,0);
   reference.getNavigation().stop();reference.setDeltaMovement(0,0,0);
   pokemon.setYRot(-90);pokemon.yRotO=-90;pokemon.yBodyRot=pokemon.yBodyRotO=-90;
   reference.setYRot(-90);reference.yRotO=-90;reference.yBodyRot=reference.yBodyRotO=-90;
   pokemon.setNoAi(false);
   pokemon.focusTrainerDefenseTarget(target);
   // Level 10 perception spans neighboring GameTest structures. Preserve the
   // explicitly focused target while preventing fixtures from stealing it.
   pokemon.targetSelector.removeAllGoals();
   awaitRetreatPath(h,pokemon,target,reference,owner,airborne,0);
  });
 }
 private static void awaitRetreatPath(GameTestHelper h,MinemonCreature pokemon,Cow target,Spider reference,
                                      ServerPlayer owner,boolean airborne,int elapsed) {
  if((pokemon.getTarget()!=target||pokemon.getNavigation().isDone()||reference.getTarget()==null
      ||reference.getNavigation().isDone())&&elapsed<40){
   h.runAfterDelay(1,()->awaitRetreatPath(h,pokemon,target,reference,owner,airborne,elapsed+1));return;
  }
  if(pokemon.getTarget()!=target||pokemon.getNavigation().isDone()||reference.getTarget()==null||reference.getNavigation().isDone()){
   h.fail("Companion and vanilla spider must both enter active paths before speed measurement; companionTarget="+pokemon.getTarget()
       +" companionNav="+pokemon.getNavigation().getTargetPos()+" spiderTarget="+reference.getTarget()+" spiderNav="+reference.getNavigation().getTargetPos());
   owner.discard();reference.discard();return;
  }
  // The Spider's melee goal can take a few ticks to establish its path while
  // the companion's ranged goal starts sooner. Measure both from rest once
  // both paths are active so setup momentum is not counted as speed.
  pokemon.setDeltaMovement(0,0,0);reference.setDeltaMovement(0,0,0);
  pokemon.setYRot(-90);pokemon.yRotO=-90;pokemon.yBodyRot=pokemon.yBodyRotO=-90;
   var pokemonStart=pokemon.position();var spiderStart=reference.position();
   // Compare real ground-path displacement with real companion retreat; neither
   // entity receives a velocity or movement-speed modifier.
  h.runAfterDelay(20,()->{
    double retreat=pokemon.position().subtract(pokemonStart).horizontalDistance();
    double spider=reference.position().subtract(spiderStart).horizontalDistance();
    String measured="; species="+pokemon.getSpeciesSlug()+"; 20 ticks: companion="+retreat+" blocks, spider="+spider+" blocks"
        +"; pokemonTarget="+pokemon.getTarget()+" start="+pokemonStart+" end="+pokemon.position()+" velocity="+pokemon.getDeltaMovement()+" navDone="+pokemon.getNavigation().isDone()+" navTarget="+pokemon.getNavigation().getTargetPos()
        +"; spiderTarget="+reference.getTarget()+" start="+spiderStart+" end="+reference.position()+" velocity="+reference.getDeltaMovement()+" navDone="+reference.getNavigation().isDone()+" navTarget="+reference.getNavigation().getTargetPos();
    check(h,spider>1.0D,"The reference spider must actually walk"+measured+" spiderTarget="+reference.getTarget()+" start="+spiderStart+" end="+reference.position()+" velocity="+reference.getDeltaMovement()+" navDone="+reference.getNavigation().isDone()+" navTarget="+reference.getNavigation().getTargetPos());
    check(h,retreat>=spider*.85D&&retreat<=spider*1.15D,
        "Defensive retreat must match vanilla spider displacement within 15%"+measured);
    if(airborne)check(h,Math.abs(pokemon.getY()-pokemon.naturalSurface()-pokemon.combatFlightHeight())<.15D,
        "Flying retreat must retain its capped combat flight band"+measured);
    com.mojang.logging.LogUtils.getLogger().info("MINEMON SPIDER RETREAT SPEED"+measured);
    owner.discard();reference.discard();h.succeed();
  });
 }
 @GameTest(template="test_arena",timeoutTicks=120)
 public static void seatedAbraRetainsItsLowerNaturalCombatHover(GameTestHelper h){
  floor(h);ServerPlayer owner=player(h);owner.setGameMode(GameType.CREATIVE);
  var pokemon=h.spawn(ModEntities.SPECIES.get("abra").get(),new BlockPos(5,2,5));
  pokemon.tame(owner);pokemon.setPokemonLevel(10);pokemon.setOrderedToSit(true);
  pokemon.setBlackListOverride(new net.minecraft.resources.ResourceLocation("minecraft","cow"),true);
  var target=h.spawn(EntityType.COW,new BlockPos(8,1,5));target.setNoAi(true);keepCombatTargetAlive(target);
  float health=target.getHealth();var start=pokemon.position();
  pokemon.focusTrainerDefenseTarget(target);
  h.runAfterDelay(40,()->{
   double natural=Math.min(1.0D,pokemon.naturalCycle().minimumAltitude());
   check(h,natural<1.0D,"Abra fixture must have a natural hover below one block");
   check(h,Math.abs(pokemon.getY()-pokemon.naturalSurface()-natural)<.1D,
       "Seated Abra must retain its lower natural combat hover; altitude="+(pokemon.getY()-pokemon.naturalSurface())+", natural="+natural);
   check(h,pokemon.position().subtract(start).horizontalDistanceSqr()<1.0E-6D,
       "Seated Abra must not chase or retreat horizontally");
   check(h,pokemon.isCombatPose()&&pokemon.isOrderedToSit()&&target.getHealth()<health,
       "Seated Abra must attack with a stable combat pose while preserving Sentar");
   owner.discard();h.succeed();
  });
 }
 @GameTest(template="test_arena",timeoutTicks=180)
 public static void abraRetainsNaturalSubBlockHoverDuringAndAfterCombat(GameTestHelper h){
  floor(h);ServerPlayer owner=player(h);owner.setGameMode(GameType.CREATIVE);
  MinemonCreature abra=h.spawn(ModEntities.SPECIES.get("abra").get(),new BlockPos(5,2,5));
  abra.tame(owner);abra.setPokemonLevel(10);
  abra.setBlackListOverride(new net.minecraft.resources.ResourceLocation("minecraft","cow"),false);
  Cow target=h.spawn(EntityType.COW,new BlockPos(8,1,5));target.setNoAi(true);keepCombatTargetAlive(target);
  double natural=Math.min(1.0D,abra.naturalCycle().minimumAltitude());
  abra.setPos(abra.getX(),abra.naturalSurface()+natural,abra.getZ());
  float initialHealth=target.getHealth();abra.focusTrainerDefenseTarget(target);
  h.runAfterDelay(55,()->{
   double during=abra.getY()-abra.naturalSurface();
   check(h,natural<1.0D&&Math.abs(during-natural)<.12D,
       "An active Abra must preserve its natural sub-block combat hover; altitude="+during+" natural="+natural);
   check(h,target.getHealth()<initialHealth,"Abra must continue attacking while retaining its lower natural combat height");
   target.discard();abra.setTarget(null);
   // The expanded level 10 perception can see neighboring GameTest fixtures;
   // freeze target acquisition while checking the completed flight reset.
   abra.setNoAi(true);
   h.runAfterDelay(45,()->{
    double after=abra.getY()-abra.naturalSurface();
    check(h,abra.getTarget()==null&&after<=1.0D&&after>=abra.flightMinimum()-.15D,
        "Abra must return to its natural low hover after combat; altitude="+after+" naturalMinimum="+abra.flightMinimum());
    owner.discard();h.succeed();
   });
  });
 }
 private static void awaitPidgeyCombat(GameTestHelper h,MinemonCreature pidgey,ServerPlayer owner,Zombie target,float initialHealth,double initialDistance,int elapsed){
  if(pidgey.getTarget()!=target&&elapsed<100){h.runAfterDelay(5,()->awaitPidgeyCombat(h,pidgey,owner,target,initialHealth,initialDistance,elapsed+5));return;}
  if(pidgey.getTarget()!=target){h.fail("Pidgey failed to acquire its authorized target within 100 ticks; target="+pidgey.getTarget()+" canAttack="+pidgey.canAttack(target)+" distance="+pidgey.distanceTo(target));owner.discard();return;}
  h.runAfterDelay(40,()->{
   double altitude=pidgey.getY()-pidgey.naturalSurface();
   double distance=CombatGeometry.horizontalCenterDistanceSquared(pidgey,target);
   check(h,pidgey.getTarget()==target&&target.getHealth()<initialHealth,
       "Pidgey must damage a zombie while retaining focus; target="+pidgey.getTarget()+" health="+target.getHealth()+" position="+pidgey.position());
   check(h,Math.abs(altitude-1.5D)<.15D,"A flying combatant must hold 1.5 blocks above its natural surface after 40 combat ticks; altitude="+altitude+" target="+pidgey.getTarget()+" navDone="+pidgey.getNavigation().isDone());
   check(h,distance>initialDistance&&!pidgey.combatReady(),"Pidgey must retreat from the zombie while recharging; initialDistanceSquared="+initialDistance+", currentDistanceSquared="+distance);
   owner.discard();h.succeed();
  });
 }
 @GameTest(template="test_arena",timeoutTicks=100)
 public static void sitAndPersistence(GameTestHelper h) {
  MinemonCreature b=spawn(h);ServerPlayer p=player(h);b.tame(p);b.setOrderedToSit(true);b.setSleeping(true);
  CompoundTag tag=new CompoundTag();b.saveWithoutId(tag);MinemonCreature restored=ModEntities.BULBASAUR.get().create(h.getLevel());restored.load(tag);
  check(h,restored.isTame()&&p.getUUID().equals(restored.getOwnerUUID())&&restored.isOrderedToSit()&&restored.isSleeping(),"Taming UUID, sit order and sleep state must survive NBT reload");
  h.runAfterDelay(20,()->{var start=b.position();h.runAfterDelay(30,()->{double dx=b.getX()-start.x,dz=b.getZ()-start.z;check(h,dx*dx+dz*dz<0.25,"Sitting creature must stay in place");p.discard();h.succeed();});});
 }
 @GameTest(template="test_arena",timeoutTicks=120)
 public static void sittingAllowsGravityAndUsesTheLatestTeleportPosition(GameTestHelper h) {
  floor(h);h.setBlock(new BlockPos(5,4,5),net.minecraft.world.level.block.Blocks.STONE);
  MinemonCreature b=h.spawn(ModEntities.BULBASAUR.get(),new BlockPos(5,5,5));ServerPlayer p=player(h);
  b.tame(p);b.setOrderedToSit(true);h.setBlock(new BlockPos(5,4,5),net.minecraft.world.level.block.Blocks.AIR);
  h.runAfterDelay(60,()->{
   double support=NaturalMovementGoals.surfaceBelow(h.getLevel(),b.getX(),b.getY(),b.getZ());
   check(h,Math.abs(b.getY()-support)<0.1D&&b.isOnGround(),
       "A sitting grounded Pokémon must fall onto the lower floor: y="+b.getY()+", support="+support+", grounded="+b.isOnGround()+", noGravity="+b.isNoGravity()+", velocity="+b.getDeltaMovement());
   check(h,b.isOrderedToSit(),"Falling must preserve the Sentar command");
   BlockPos moved=h.absolutePos(new BlockPos(9,1,9));
   b.moveTo(moved.getX()+0.5D,moved.getY(),moved.getZ()+0.5D,0.0F,0.0F);
   h.runAfterDelay(5,()->{
    check(h,b.isOrderedToSit()&&Math.abs(b.getX()-(moved.getX()+0.5D))<0.01D
            &&Math.abs(b.getZ()-(moved.getZ()+0.5D))<0.01D,
        "A sitting Pokémon must adopt a new teleport position instead of returning to its old anchor");
    p.discard();h.succeed();
   });
  });
 }
 @GameTest(template="test_arena",timeoutTicks=120)
 public static void rangedAttackCooldownIsExactlyOneHundredTicks(GameTestHelper h) {
  floor(h);MinemonCreature b=h.spawn(ModEntities.BULBASAUR.get(),new BlockPos(5,1,5));
  ServerPlayer p=player(h);Cow target=h.spawn(EntityType.COW,new BlockPos(8,1,5));target.setNoAi(true);
  b.tame(p);b.setOrderedToSit(false);b.setNoAi(true);b.focusTrainerDefenseTarget(target);
  check(h,b.combatReady()&&b.canFireAt(target),"Fixture must begin with a valid ready shot");
  check(h,b.fireElementalProjectile(target),"A valid ranged shot must fire immediately");
  h.runAfterDelay(99,()->{
   check(h,!b.combatReady(),"The normal five-second cooldown must still be active at tick 99");
   b.getNavigation().stop();
   h.runAfterDelay(1,()->{
    check(h,b.combatReady(),"The next shot must become available at exactly tick 100");
    p.discard();h.succeed();
   });
  });
 }
 @GameTest(template="test_arena",timeoutTicks=100)
 public static void registeredSpeciesSpawnPersistAndHaveEggs(GameTestHelper h) {
  for(var entry:ModEntities.SPECIES.entrySet()) {
   MinemonCreature b=entry.getValue().get().create(h.getLevel());
   check(h,b!=null,"Entity creation failed: "+entry.getKey());
   var egg=(net.minecraft.world.item.SpawnEggItem)br.com.petrickfaria.minemon.registry.ModItems.EGGS.get(entry.getKey()).get();
   check(h,egg.getType(null)==entry.getValue().get(),"Wrong spawn egg mapping: "+entry.getKey());
   b.setOwnerUUID(UUID.randomUUID());b.setTame(true);b.setOrderedToSit(true);b.setSleeping(true);CompoundTag tag=new CompoundTag();b.saveWithoutId(tag);
   MinemonCreature restored=entry.getValue().get().create(h.getLevel());restored.load(tag);
   check(h,restored.isTame()&&restored.isOrderedToSit()&&restored.isSleeping()&&b.getOwnerUUID().equals(restored.getOwnerUUID()),"Persistence failed: "+entry.getKey());
  }
  h.succeed();
 }

 @GameTest(template="test_arena",batch="taming_bone",timeoutTicks=100)
 public static void boneTamingAndSitCommands(GameTestHelper h) {
  MinemonCreature b=spawn(h);ServerPlayer p=player(h);
  int previous=br.com.petrickfaria.minemon.config.MinemonConfig.TAME_CHANCE_PERCENT.get();
  try {
   br.com.petrickfaria.minemon.config.MinemonConfig.TAME_CHANCE_PERCENT.set(100);
   p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BONE,2));
   b.mobInteract(p,net.minecraft.world.InteractionHand.MAIN_HAND);
   check(h,!b.isTame(),"Bone interaction must never tame a Minemon");
   check(h,p.getMainHandItem().getCount()==2,"Bone interaction must preserve the bone stack");
   var treat=br.com.petrickfaria.minemon.registry.ModItems.TREATS.get(br.com.petrickfaria.minemon.item.PokemonTreatType.GRASS).get().getDefaultInstance();
   p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,treat);
   b.mobInteract(p,net.minecraft.world.InteractionHand.MAIN_HAND);
   check(h,b.isTame()&&p.getUUID().equals(b.getOwnerUUID())&&b.isOrderedToSit(),"Compatible Grass Treat must tame, assign owner and sit");
   check(h,treat.isEmpty(),"Successful survival treat taming must consume one treat");
   p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
   b.mobInteract(p,net.minecraft.world.InteractionHand.MAIN_HAND);
   check(h,p.containerMenu instanceof br.com.petrickfaria.minemon.menu.PokemonInfoMenu,"Owned Pokémon interaction must open its management interface");
   var menu=(br.com.petrickfaria.minemon.menu.PokemonInfoMenu)p.containerMenu;
   check(h,menu.clickMenuButton(p,0)&&!b.isOrderedToSit(),"Seguir must be able to order standing");
   check(h,menu.clickMenuButton(p,1)&&b.isOrderedToSit(),"Sentar must be able to order sitting");
  }finally {br.com.petrickfaria.minemon.config.MinemonConfig.TAME_CHANCE_PERCENT.set(previous);p.discard();}
  h.succeed();
 }
 @GameTest(template="test_arena",timeoutTicks=300)
 public static void followsOwner(GameTestHelper h) {
  MinemonCreature b=h.spawn(ModEntities.BULBASAUR.get(),new BlockPos(2,1,5));ServerPlayer p=player(h);
  BlockPos pos=h.absolutePos(new BlockPos(14,1,5));p.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
  // Anchor the follow fixture relative to the actual player coordinates. This
  // keeps both entities on this structure's floor and inside level-1 range.
  b.setPos(p.getX()-11.0D,p.getY(),p.getZ());b.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
  b.tame(p);b.setOrderedToSit(false);b.targetSelector.removeAllGoals();
  b.setBlackListOverride(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(net.minecraft.world.entity.EntityType.ZOMBIE),false);
  float initial=b.distanceTo(p);
  check(h,initial>10.0D&&initial<15.0D,"Owner follow fixture must start inside level-1 perception and outside its approach threshold; distance="+initial);
  h.runAfterDelay(100,()->{check(h,b.distanceTo(p)<initial-2,"Companion must approach its owner"+state(b,p));p.discard();h.succeed();});
 }
 @GameTest(template="test_arena",timeoutTicks=100)
 public static void creativePlayersRemainProtected(GameTestHelper h) {
  MinemonCreature b=spawn(h);ServerPlayer p=player(h);p.setGameMode(GameType.CREATIVE);
  check(h,!b.canAttack(p),"Creative player must retain vanilla target protection");b.hurt(DamageSource.playerAttack(p),1);
  h.runAfterDelay(40,()->{check(h,b.getTarget()==null,"Creative aggressor must not create a combat target");p.discard();h.succeed();});
 }
 @GameTest(template="test_arena",timeoutTicks=180)
 public static void wildRetaliatesAgainstAnotherMinemon(GameTestHelper h) {
  floor(h);MinemonCreature b=h.spawn(ModEntities.BULBASAUR.get(),new BlockPos(10,1,5));
  MinemonCreature attacker=h.spawn(ModEntities.BULBASAUR.get(),new BlockPos(4,1,5));attacker.setNoAi(true);float health=attacker.getHealth();
  check(h,b.hurt(DamageSource.mobAttack(attacker),1)&&b.getTarget()==attacker,
      "A damaging Minemon hit must acquire the attacker before ranged repositioning; "+state(b,attacker));
  awaitWildRangedRetaliation(h,b,attacker,health,0);
 }
 private static void awaitWildRangedRetaliation(GameTestHelper h,MinemonCreature defender,MinemonCreature attacker,float initialHealth,int elapsed){
  if(attacker.getHealth()<initialHealth){
   check(h,CombatGeometry.horizontalCenterDistanceSquared(defender,attacker)>=49.0D,
       "Wild Minemon must keep seven horizontal blocks before its ranged retaliation");
   h.succeed();return;
  }
  check(h,elapsed<160,"Wild Minemon must retreat to a safe ranged position and retaliate; "+state(defender,attacker));
  h.runAfterDelay(5,()->awaitWildRangedRetaliation(h,defender,attacker,initialHealth,elapsed+5));
 }
 @GameTest(template="test_arena",timeoutTicks=460)
 public static void wildAggressionActuallyExpires(GameTestHelper h) {
  MinemonCreature b=spawn(h);b.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0);
  Cow c=h.spawn(EntityType.COW,new BlockPos(11,1,5));c.setNoAi(true);b.hurt(DamageSource.mobAttack(c),1);
  check(h,b.getTarget()==c,"Initial provocation must acquire target");
  h.runAfterDelay(430,()->{check(h,b.getTarget()==null,"Expired aggression must not be reacquired by HurtByTargetGoal"+state(b,c));h.succeed();});
 }

 @GameTest(template="test_arena",timeoutTicks=100)
 public static void everySpawnEggCreatesItsEntity(GameTestHelper h) {
  ServerPlayer p=player(h);BlockPos floorPos=h.absolutePos(new BlockPos(8,0,8));
  for(var entry:ModEntities.SPECIES.entrySet()) {
   var egg=br.com.petrickfaria.minemon.registry.ModItems.EGGS.get(entry.getKey()).get();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(egg));
   var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floorPos),net.minecraft.core.Direction.UP,floorPos,false);
   var result=egg.useOn(new net.minecraft.world.item.context.UseOnContext(p,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
   check(h,result.consumesAction(),"Egg use failed: "+entry.getKey());
   var created=h.getLevel().getEntitiesOfClass(MinemonCreature.class,new net.minecraft.world.phys.AABB(floorPos).inflate(5),e->e.getType()==entry.getValue().get());
   check(h,!created.isEmpty(),"Egg did not spawn its entity: "+entry.getKey());
   check(h,created.stream().noneMatch(MinemonCreature::isTame),"Spawn eggs must create wild Pokémon: "+entry.getKey());
   check(h,created.stream().allMatch(pokemon->br.com.petrickfaria.minemon.pokec.WildEncounterManager.isEncounter(pokemon)
       &&p.getUUID().equals(br.com.petrickfaria.minemon.pokec.WildEncounterManager.owner(pokemon))),
       "Spawn eggs must register capture encounters without assigning Pokémon ownership: "+entry.getKey());
   created.forEach(net.minecraft.world.entity.Entity::discard);
  }
  var ownedTemplate=br.com.petrickfaria.minemon.registry.ModItems.EGGS.get("charmander").get();
  var ownedStack=br.com.petrickfaria.minemon.item.PokecOwnedSpawnEggItem.ownedStack(new net.minecraft.world.item.ItemStack(ownedTemplate),p.getUUID(),p.getGameProfile().getName());
  p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ownedStack);
  var ownedHit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floorPos),net.minecraft.core.Direction.UP,floorPos,false);
  check(h,ownedTemplate.useOn(new net.minecraft.world.item.context.UseOnContext(p,net.minecraft.world.InteractionHand.MAIN_HAND,ownedHit)).consumesAction(),"Owner-tagged eggs should still spawn without slot gating");
  var createdOwned=h.getLevel().getEntitiesOfClass(MinemonCreature.class,new net.minecraft.world.phys.AABB(floorPos).inflate(5),e->e.getSpeciesSlug().equals("charmander"));
  check(h,!createdOwned.isEmpty()&&createdOwned.stream().noneMatch(MinemonCreature::isTame),"Purchased spawn egg must not assign ownership or active slots");
  createdOwned.forEach(net.minecraft.world.entity.Entity::discard);
  p.discard();h.succeed();
 }

 @GameTest(template="test_arena",timeoutTicks=100)
 public static void eggSpawnedWildPokemonDownsAndPersistsInsteadOfDying(GameTestHelper h){
  ServerPlayer owner=player(h);var egg=br.com.petrickfaria.minemon.registry.ModItems.EGGS.get("bulbasaur").get();
  BlockPos floor=h.absolutePos(new BlockPos(8,0,8));owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
      new net.minecraft.world.item.ItemStack(egg));
  var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floor),
      net.minecraft.core.Direction.UP,floor,false);
  check(h,egg.useOn(new net.minecraft.world.item.context.UseOnContext(owner,net.minecraft.world.InteractionHand.MAIN_HAND,hit)).consumesAction(),
      "Bulbasaur spawn egg must create the test wild");
  var found=h.getLevel().getEntitiesOfClass(MinemonCreature.class,new net.minecraft.world.phys.AABB(floor).inflate(4),
      pokemon->pokemon.getSpeciesSlug().equals("bulbasaur")&&!pokemon.isTame());
  check(h,!found.isEmpty(),"Spawn-egg Bulbasaur must remain wild");
  MinemonCreature wild=found.get(0);
  check(h,br.com.petrickfaria.minemon.pokec.WildEncounterManager.isEncounter(wild)
      &&owner.getUUID().equals(br.com.petrickfaria.minemon.pokec.WildEncounterManager.owner(wild))
      &&wild.getOwnerUUID()==null,"Capture attribution must not domesticate or assign the Pokémon owner");
  check(h,br.com.petrickfaria.minemon.pokec.ActivePokemonData.get(h.getLevel()).count(owner.getUUID())==0,
      "An egg-spawned wild encounter must not occupy a domesticated Pokémon slot");
  MinemonCreature ally=h.spawn(ModEntities.SPECIES.get("charizard").get(),new BlockPos(7,1,7));
  ally.tame(owner);ally.setOwnerUUID(owner.getUUID());wild.setHealth(2.0F);
  check(h,br.com.petrickfaria.minemon.pokec.WildEncounterManager.canDefeat(wild,ally),
      "A same-level owner and its active companion must be authorized to defeat this wild encounter");
  check(h,wild.hurt(DamageSource.mobAttack(ally),20.0F),"Owned Pokémon must be able to defeat its egg-spawned wild target");
  check(h,br.com.petrickfaria.minemon.pokec.WildEncounterManager.isDowned(wild)&&wild.getHealth()==1.0F
      &&!wild.isRemoved()&&wild.isAlive(),"Defeated egg-spawned Pokémon must enter the capture-ready state instead of dying");
  CompoundTag saved=new CompoundTag();wild.saveWithoutId(saved);wild.discard();
  MinemonCreature restored=ModEntities.SPECIES.get("bulbasaur").get().create(h.getLevel());restored.load(saved);
  check(h,h.getLevel().addFreshEntity(restored),"Downed egg encounter must reload into the test world");
  check(h,br.com.petrickfaria.minemon.pokec.WildEncounterManager.isEncounter(restored)
      &&br.com.petrickfaria.minemon.pokec.WildEncounterManager.isDowned(restored)
      &&owner.getUUID().equals(br.com.petrickfaria.minemon.pokec.WildEncounterManager.owner(restored))
      &&!restored.isTame()&&!restored.isRemoved(),"Downed state and wild status must survive entity persistence");
  h.succeed();
 }

 @GameTest(template="test_arena",timeoutTicks=100)
 public static void directPlayerHitsCommandOwnedPokemonToFocusWildTarget(GameTestHelper h){
  ServerPlayer owner=player(h);var egg=br.com.petrickfaria.minemon.registry.ModItems.EGGS.get("geodude").get();
  BlockPos eggFloor=h.absolutePos(new BlockPos(8,0,8));
  owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(egg));
  var eggHit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(eggFloor),net.minecraft.core.Direction.UP,eggFloor,false);
  check(h,egg.useOn(new net.minecraft.world.item.context.UseOnContext(owner,net.minecraft.world.InteractionHand.MAIN_HAND,eggHit)).consumesAction(),"Geodude spawn egg must create the wild target");
  var spawned=h.getLevel().getEntitiesOfClass(MinemonCreature.class,new net.minecraft.world.phys.AABB(eggFloor).inflate(4),m->m.getSpeciesSlug().equals("geodude")&&!m.isTame());
  check(h,!spawned.isEmpty(),"Spawn-egg Geodude must be a wild Pokémon");
  MinemonCreature wild=spawned.get(0);
  MinemonCreature[] team=new MinemonCreature[3];String[] species={"charizard","jolteon","gengar"};
  for(int i=0;i<team.length;i++){
   team[i]=h.spawn(ModEntities.SPECIES.get(species[i]).get(),new BlockPos(7+i,1,7));team[i].tame(owner);team[i].setOwnerUUID(owner.getUUID());
  }
  team[0].setSleeping(true);team[2].setOrderedToSit(true);float hp=wild.getHealth();
  owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_SWORD));
  owner.attack(wild);
  check(h,wild.getHealth()==hp-1.0F,"A Netherite sword hit against a spawn-egg Pokémon must cause exactly one HP of damage");
  check(h,team[0].getTarget()==wild&&team[1].getTarget()==wild&&team[2].getTarget()==wild,"Every loaded companion must focus the exact wild Pokémon hit");
  check(h,!team[0].isSleeping(),"A sleeping companion must wake immediately for the focused attack");
  float companionHealth=team[1].getHealth();
  check(h,!team[1].hurt(DamageSource.playerAttack(owner),20.0F)&&team[1].getHealth()==companionHealth,
      "Direct player damage must remain blocked against the owner's domesticated Pokémon");
  wild.discard();
  h.runAfterDelay(1,()->{
   check(h,team[0].isSleeping(),"A companion that was asleep before combat must resume sleep when its focus target disappears");
   check(h,team[2].isOrderedToSit(),"The sit command must be restored after a temporary focused attack");
   owner.discard();h.succeed();
  });
 }

 @GameTest(template="test_arena",timeoutTicks=100)
 public static void playerDamageCapIgnoresWeaponAndAttributeDamage(GameTestHelper h){
  ServerPlayer owner=player(h);String[] species={"bulbasaur","charmander","squirtle","pikachu","geodude","gastly"};
  net.minecraft.world.item.Item[] weapons={net.minecraft.world.item.Items.AIR,net.minecraft.world.item.Items.WOODEN_SWORD,
      net.minecraft.world.item.Items.IRON_SWORD,net.minecraft.world.item.Items.DIAMOND_SWORD,
      net.minecraft.world.item.Items.NETHERITE_SWORD,net.minecraft.world.item.Items.NETHERITE_AXE};
  float[] incoming={1.0F,3.0F,6.0F,8.0F,15.0F,40.0F};
  for(int i=0;i<species.length;i++){
   MinemonCreature wild=h.spawn(ModEntities.SPECIES.get(species[i]).get(),new BlockPos(3+i,1,10));
   float health=wild.getHealth();owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(weapons[i]));
   check(h,wild.hurt(DamageSource.playerAttack(owner),incoming[i]),"Direct player melee must be accepted for weapon fixture "+weapons[i]);
   check(h,wild.getHealth()==health-1.0F,"Player melee must be capped at one HP for "+weapons[i]+" even when incoming damage is "+incoming[i]);
   wild.hurt(DamageSource.playerAttack(owner),incoming[i]);
   check(h,wild.getHealth()==health-1.0F,"Repeated calls inside the vanilla hurt window must not add damage for "+weapons[i]);
  }
  owner.discard();h.succeed();
 }

 @GameTest(template="test_arena",timeoutTicks=100)
 public static void playerDamageRulesDoNotChangeForVanillaMobsOrOwnedPokemon(GameTestHelper h){
  ServerPlayer owner=player(h);MinemonCreature pokemon=h.spawn(ModEntities.SPECIES.get("pikachu").get(),new BlockPos(8,1,8));
  pokemon.tame(owner);pokemon.setOwnerUUID(owner.getUUID());float pokemonHealth=pokemon.getHealth();
  check(h,!pokemon.hurt(DamageSource.playerAttack(owner),30.0F)&&pokemon.getHealth()==pokemonHealth,
      "Domestic Minemon must retain direct-player damage protection");
  Cow cow=h.spawn(EntityType.COW,new BlockPos(10,1,8));float cowHealth=cow.getHealth();
  check(h,cow.hurt(DamageSource.playerAttack(owner),7.0F)&&cow.getHealth()==cowHealth-7.0F,
      "Vanilla mobs must retain their unmodified player damage");
  owner.discard();h.succeed();
 }

 @GameTest(template="test_arena",timeoutTicks=100)
 public static void naturalEncounterPlayerHitsStayOneHpAndNeverDownTheEncounter(GameTestHelper h){
  ServerPlayer owner=player(h);MinemonCreature wild=h.spawn(ModEntities.SPECIES.get("bulbasaur").get(),new BlockPos(8,1,8));
  var data=wild.getPersistentData();data.putBoolean("MinemonWildEncounter",true);data.putUUID("MinemonEncounterOwner",owner.getUUID());data.putString("MinemonEncounterState","active");
  wild.setWildEncounter(true);wild.setWildEncounterOwner(owner.getUUID());wild.setHealth(2.0F);
  check(h,wild.hurt(DamageSource.playerAttack(owner),50.0F),"A direct hit must be accepted on a naturally managed wild encounter");
  check(h,wild.getHealth()==1.0F&&!br.com.petrickfaria.minemon.pokec.WildEncounterManager.isDowned(wild),
      "A player's capped hit must leave a natural encounter at one HP without triggering capture/downed state");
  wild.invulnerableTime=0;
  wild.hurt(DamageSource.playerAttack(owner),50.0F);
  check(h,wild.getHealth()==1.0F&&!br.com.petrickfaria.minemon.pokec.WildEncounterManager.isDowned(wild),
      "A player's next hit must not kill or down a natural encounter");
  owner.discard();h.succeed();
 }

 @GameTest(template="test_arena",timeoutTicks=1240)
 public static void domesticatedPokemonRegeneratesOneHealthAfter1200Ticks(GameTestHelper h){
  ServerPlayer owner=player(h);MinemonCreature pokemon=h.spawn(ModEntities.SPECIES.get("charmander").get(),new BlockPos(6,1,6));
  pokemon.tame(owner);pokemon.setOwnerUUID(owner.getUUID());pokemon.setNoAi(true);pokemon.setSleeping(true);pokemon.setHealth(pokemon.getMaxHealth()-3.0F);float initial=pokemon.getHealth();
  MinemonCreature wild=h.spawn(ModEntities.SPECIES.get("charmander").get(),new BlockPos(9,1,6));wild.setNoAi(true);wild.setHealth(wild.getMaxHealth()-3.0F);float wildInitial=wild.getHealth();
  h.runAfterDelay(1199,()->{
   check(h,pokemon.getHealth()==initial,"Natural healing must not occur before the full 1200-tick interval");
   check(h,wild.getHealth()==wildInitial,"The automatic recovery timer must not heal wild Pokémon");
   h.runAfterDelay(1,()->{
    check(h,pokemon.getHealth()==initial+1.0F,"A domesticated Pokémon must recover exactly one health point at 1200 ticks");
    check(h,wild.getHealth()==wildInitial,"The automatic recovery timer must exclude wild Pokémon");
    check(h,pokemon.getHealth()<=pokemon.getMaxHealth(),"Natural healing must respect species maximum health");owner.discard();h.succeed();
   });
  });
 }

 @GameTest(template="test_arena",timeoutTicks=420)
 public static void airborneSpeciesStayAboveSurfaceAndLandOnlyToSleep(GameTestHelper h) {
  floor(h);
  // Magnemite is a flyer that intentionally rests in a safe hover; Spearow,
  // Fearow and the legendary birds are covered by the grounded night-rest test.
  MinemonCreature bird=h.spawn(ModEntities.SPECIES.get("magnemite").get(),new BlockPos(5,1,5));
  traceMovement(h,"natural-flight",bird,0,300);
  h.runAfterDelay(100,()->{
   double altitude=bird.getY()-NaturalMovementGoals.surfaceBelow(h.getLevel(),bird.getX(),bird.getY(),bird.getZ());
   check(h,altitude>=0.9D&&altitude<=9.1D,"Airborne species must maintain the active 1..3 or 4..9 block altitude band; got "+altitude);
   bird.setSleeping(true);
   check(h,bird.isNoGravity(),"Air-only sleeping species must retain hover gravity control");
   h.runAfterDelay(100,()->{
    double surface=NaturalMovementGoals.surfaceBelow(h.getLevel(),bird.getX(),bird.getY(),bird.getZ());
    check(h,Math.abs(bird.getY()-surface-1.0D)<0.2D&&!bird.isOnGround(),"Air-only sleeping species must hover one block above support");
    bird.setSleeping(false);
    check(h,bird.isNoGravity(),"Awake airborne species must resume flight");
    awaitAwakeFlightBand(h,bird,180,0);
   });
  });
 }

 @GameTest(template="test_arena",batch="night_landing",timeoutTicks=1200)
 public static void requestedBirdsLandAndHoldTheirFoldedSleepPose(GameTestHelper h) {
  floor(h);h.getLevel().setDayTime(18000);
  var slugs=java.util.List.of("spearow","fearow","articuno","zapdos","moltres");
  int[][] positions={{2,3},{8,3},{14,3},{5,11},{11,11}};
  var birds=new java.util.ArrayList<MinemonCreature>();
  for(int i=0;i<slugs.size();i++){
   var bird=h.spawn(ModEntities.SPECIES.get(slugs.get(i)).get(),new BlockPos(positions[i][0],8,positions[i][1]));
   bird.setNoGravity(true);
   bird.setDeltaMovement(0,0,0);
   birds.add(bird);
  }
  awaitRequestedBirdLanding(h,birds,480);
 }

 private static void awaitRequestedBirdLanding(GameTestHelper h,java.util.List<MinemonCreature> birds,int remaining) {
  if(birds.stream().allMatch(b->b.isSleeping()&&b.isNightResting()&&b.isOnGround()&&!b.isNoGravity())){
   h.getLevel().setDayTime(1000);
   h.runAfterDelay(3,()->{
    for(var bird:birds)check(h,!bird.isSleeping()&&!bird.isNightResting(),bird.getSpeciesSlug()+" must wake at dawn");
    awaitCommandedLanding(h,birds,500,false);
   });
   return;
  }
  if(remaining<=0){
   var states=birds.stream().map(b->b.getSpeciesSlug()+"{sleep="+b.isSleeping()+",night="+b.isNightResting()+",ground="+b.isOnGround()+",gravity="+b.isNoGravity()+",pos="+b.position()+",nav="+b.getNavigation().getTargetPos()+"}").toList();
   h.fail("Requested flyers must land and sleep on dry ground before dawn: "+states);
   return;
  }
  h.runAfterDelay(5,()->awaitRequestedBirdLanding(h,birds,remaining-5));
 }

 private static void awaitCommandedLanding(GameTestHelper h,java.util.List<MinemonCreature> birds,int remaining,boolean ordered){
  if(!ordered && birds.stream().allMatch(b->!b.isOnGround() && b.getY()-b.naturalSurface()>.6)){
   // Seguir requires an actual owner. Random, unresolvable owner UUIDs leave
   // the follow command active without any follow goal able to guide takeoff.
   ServerPlayer owner=player(h);owner.setGameMode(GameType.CREATIVE);
   for(var bird:birds){bird.tame(owner);bird.setOrderedToSit(true);}
   // Establish the reference after the command selects its safe hover height.
   var anchors=birds.stream().map(MinemonCreature::position).toList();
   h.runAfterDelay(30,()->{
    for(int i=0;i<birds.size();i++){
     var bird=birds.get(i);
     check(h,bird.isOrderedToSit()&&!bird.isSleeping(),"Commanded flyer must remain awake and sitting");
     double hover=bird.getY()-NaturalMovementGoals.surfaceBelow(h.getLevel(),bird.getX(),bird.getY(),bird.getZ());
     check(h,Math.abs(hover-1.0D)<0.2D,bird.getSpeciesSlug()+" must hold an airborne flyer at one block above support; altitude="+hover+", position="+bird.position());
     check(h,bird.position().subtract(anchors.get(i)).lengthSqr()<1.0E-6D,bird.getSpeciesSlug()+" must hold the flyer's full 3D position; start="+anchors.get(i)+", now="+bird.position());
     bird.setOrderedToSit(false);
    }
    h.runAfterDelay(60,()->{
     for(var bird:birds)check(h,!bird.isOrderedToSit()&&!bird.isOnGround(),"Following order must resume natural flight after sitting");
     for(var bird:birds)bird.discard();owner.discard();NineBehaviorChecks.defeatedFloaters(h);
    });
   });return;
  }
  check(h,remaining>0,"Commanded flyers failed to remain at their sit position or resume flight");
  final boolean next=ordered;
  h.runAfterDelay(5,()->awaitCommandedLanding(h,birds,remaining-5,next));
 }

 @GameTest(template="test_arena",batch="minemon_flyer_sit_night",timeoutTicks=520)
 public static void sittingFlyerSleepsAtNightAndFollowCommandWakesIt(GameTestHelper h){
  floor(h);long originalDayTime=h.getLevel().getDayTime();h.getLevel().setDayTime(1000);
  boolean previousMobSpawning=h.getLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING);
  h.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,h.getLevel().getServer());
  MinemonCreature bird=h.spawn(ModEntities.SPECIES.get("pidgey").get(),new BlockPos(5,6,5));
  ServerPlayer owner=player(h);owner.moveTo(h.absolutePos(new BlockPos(13,1,5)).getX()+.5D,1.0D,h.absolutePos(new BlockPos(13,1,5)).getZ()+.5D,0,0);
  bird.tame(owner);bird.setOwnerUUID(owner.getUUID());bird.setOrderedToSit(true);
  // GameTests share nearby chunks; remove hostile leftovers so this fixture
  // verifies natural night rest rather than an unrelated defense response.
  for(var mob:h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,bird.getBoundingBox().inflate(32.0D)))
   if(mob!=bird&&mob instanceof net.minecraft.world.entity.monster.Enemy)mob.discard();
  var horizontal=bird.position();
  h.runAfterDelay(30,()->{
   check(h,Math.abs(bird.getY()-NaturalMovementGoals.surfaceBelow(h.getLevel(),bird.getX(),bird.getY(),bird.getZ())-1.0D)<0.2D,
       "A sitting Pidgey must hover one block above ground during daytime");
   check(h,bird.position().subtract(horizontal).horizontalDistanceSqr()<1.0E-6D,"A sitting flyer must not drift horizontally");
   h.getLevel().setDayTime(18000);
   h.runAfterDelay(300,()->{
    boolean slept=bird.isSleeping()&&bird.isOnGround();
    if(!slept){
     String state=" sleeping="+bird.isSleeping()+" nightRest="+bird.isNightResting()+" grounded="+bird.isOnGround()
         +" noGravity="+bird.isNoGravity()+" following="+bird.isFollowingOwner()+" y="+bird.getY()
         +" surface="+bird.naturalSurface()+" target="+bird.getTarget()+" nav="+bird.getNavigation().getTargetPos()
         +" landing="+bird.wantsNaturalLanding()+" sit="+bird.isOrderedToSit();
     h.getLevel().setDayTime(originalDayTime);h.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(previousMobSpawning,h.getLevel().getServer());
     h.fail("A sitting flyer must land and sleep during its natural night period;"+state);return;
    }
    ServerPlayer followOwner=player(h);
    followOwner.setInvulnerable(true);followOwner.setNoGravity(true);
    BlockPos followPos=h.absolutePos(new BlockPos(13,1,5));
    followOwner.moveTo(followPos.getX()+0.5D,followPos.getY(),followPos.getZ()+0.5D,0,0);
    // GameTests share nearby chunks; remove leftover hostile mobs (including
    // Slimes, which do not extend Monster) so this fixture measures Follow alone.
    for(var mob:h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,followOwner.getBoundingBox().inflate(32.0D)))
     if(mob!=bird&&mob instanceof net.minecraft.world.entity.monster.Enemy)mob.discard();
    bird.tame(followOwner);bird.setOwnerUUID(followOwner.getUUID());bird.setTarget(null);
    double horizontalBefore=bird.position().subtract(followOwner.position()).horizontalDistance();
    bird.setOrderedToSit(false);
    h.runAfterDelay(120,()->{
     boolean awake=!bird.isSleeping()&&!bird.isNightResting();
     double horizontalAfter=bird.position().subtract(followOwner.position()).horizontalDistance();
     boolean followed=horizontalAfter<horizontalBefore-1.0D;
     String state=" awake="+awake+" followed="+followed+" horizontalBefore="+horizontalBefore+" horizontalAfter="+horizontalAfter
         +" owner="+followOwner.position()+" ownerAlive="+followOwner.isAlive()+" ownerRemoved="+followOwner.isRemoved()
         +" ownerUUID="+followOwner.getUUID()+" mobOwner="+bird.getOwnerUUID()+" combatTarget="+bird.getTarget()
         +" orderedSit="+bird.isOrderedToSit()+" following="+bird.isFollowingOwner()+" target="+bird.getNavigation().getTargetPos()
         +" goals="+bird.goalSelector.getAvailableGoals().stream().map(g->g.getGoal().getClass().getSimpleName()+":"+g.isRunning()).toList()
         +" position="+bird.position();
     h.getLevel().setDayTime(originalDayTime);
     h.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(previousMobSpawning,h.getLevel().getServer());
     if(!awake){h.fail("The Follow order must wake the flyer and suppress automatic return to sleep;"+state);return;}
     if(!followed){h.fail("The woken Pidgey must follow its trainer;"+state);return;}
     bird.discard();owner.discard();followOwner.discard();h.succeed();
    });
   });
  });
 }

 private static void awaitAwakeFlightBand(GameTestHelper h,MinemonCreature bird,int remaining,int stable){
  double altitude=bird.getY()-NaturalMovementGoals.surfaceBelow(h.getLevel(),bird.getX(),bird.getY(),bird.getZ());
  int nextStable=altitude>=0.9D&&altitude<=9.1D&&!bird.isOnGround()?stable+5:0;
  if(nextStable>=20){h.succeed();return;}
  check(h,remaining>0,"Awakened bird must actually take off and remain in the altitude band; got "+altitude);
  h.runAfterDelay(5,()->awaitAwakeFlightBand(h,bird,remaining-5,nextStable));
 }

 @GameTest(template="test_arena",timeoutTicks=420)
 public static void airborneCombatHitsGroundTargetsWithoutLanding(GameTestHelper h) {
  floor(h);
  MinemonCreature bird=h.spawn(ModEntities.SPECIES.get("spearow").get(),new BlockPos(5,5,5));
  Cow cow=h.spawn(EntityType.COW,new BlockPos(7,1,5));cow.setNoAi(true);
  float health=cow.getHealth();
  // Start already airborne and provoke before an unrelated random patrol
  // can leave the finite arena. Takeoff/patrol are covered by the separate
  // altitude test; this fixture exercises real melee navigation and damage.
  h.runAfterDelay(1,()->{
   check(h,bird.hurt(DamageSource.mobAttack(cow),1),"Flying creature must accept provocation");
   checkAirborneAttack(h,bird,cow,health,0);
  });
 }

 private static void checkAirborneAttack(GameTestHelper h,MinemonCreature bird,Cow cow,float health,int elapsed) {
  check(h,!bird.isOnGround(),"An awake flying attacker must remain airborne");
  if(cow.getHealth()<health){h.succeed();return;}
  if(elapsed>=300){h.fail("Flying ranged AI must hit a terrestrial target while maintaining safe distance;"+state(bird,cow));return;}
  h.runAfterDelay(5,()->checkAirborneAttack(h,bird,cow,health,elapsed+5));
 }

 @GameTest(template="test_arena",batch="night_pond_landing",timeoutTicks=520)
 public static void sleepingFlyerFindsDryLandingInsteadOfDroppingIntoWater(GameTestHelper h) {
  floor(h);
  h.getLevel().setDayTime(18000);
  // Center the pond so the dry shoreline is reachable on every side of this
  // 16x16 arena, without a neighboring GameTest floor affecting path search.
  for(int x=7;x<=9;x++)for(int z=7;z<=9;z++){
   h.setBlock(new BlockPos(x,-1,z),net.minecraft.world.level.block.Blocks.STONE);
   h.setBlock(new BlockPos(x,0,z),net.minecraft.world.level.block.Blocks.WATER);
  }
  MinemonCreature bird=h.spawn(ModEntities.SPECIES.get("pidgey").get(),new BlockPos(8,8,8));
  CompoundTag flight=new CompoundTag();bird.addAdditionalSaveData(flight);flight.putString("NaturalBehaviorPhase","AIR");bird.readAdditionalSaveData(flight);
  traceMovement(h,"sleep-landing",bird,0,400);
  h.runAfterDelay(400,()->{
   boolean landed=bird.isSleeping()&&bird.isOnGround()&&!bird.isInWater();
   boolean clear=h.getLevel().noCollision(bird,bird.getBoundingBox());
   var position=bird.position();var navTarget=bird.getNavigation().getTargetPos();
   h.getLevel().setDayTime(1000);
   check(h,landed,"Night-resting mixed flyer must follow a path to dry ground; pos="+position+" nav="+navTarget
       +" sleeping="+bird.isSleeping()+" grounded="+bird.isOnGround()+" water="+bird.isInWater());
   check(h,clear,"Landing must not clip the entity into solid terrain");
   h.succeed();
  });
 }

 @GameTest(template="test_arena",timeoutTicks=40)
 public static void flightClearanceUsesWaterSurfaceAndIgnoresOverheadRoof(GameTestHelper h) {
  floor(h);
  for(int y=1;y<=3;y++)h.setBlock(new BlockPos(5,y,5),net.minecraft.world.level.block.Blocks.WATER);
  h.setBlock(new BlockPos(5,12,5),net.minecraft.world.level.block.Blocks.STONE);
  BlockPos column=h.absolutePos(new BlockPos(5,8,5));
  double surface=NaturalMovementGoals.surfaceBelow(h.getLevel(),column.getX()+0.5D,column.getY(),column.getZ()+0.5D);
  BlockPos waterTop=h.absolutePos(new BlockPos(5,3,5));
  double expected=waterTop.getY()+h.getLevel().getFluidState(waterTop).getHeight(h.getLevel(),waterTop);
  check(h,Math.abs(surface-expected)<0.001D,
      "Flight clearance must use water top below the entity, not seabed or overhead roof; got "+surface);
  h.succeed();
 }

 @GameTest(template="test_arena",timeoutTicks=40)
 public static void flightClearanceUsesPartialCollisionSurfaces(GameTestHelper h) {
  floor(h);
  h.setBlock(new BlockPos(5,0,5),net.minecraft.world.level.block.Blocks.STONE_SLAB);
  BlockPos column=h.absolutePos(new BlockPos(5,8,5));
  double slab=NaturalMovementGoals.surfaceBelow(h.getLevel(),column.getX()+0.5D,column.getY(),column.getZ()+0.5D);
  check(h,Math.abs(slab-(h.absolutePos(new BlockPos(5,0,5)).getY()+0.5D))<0.001D,
      "Flight must measure from the actual half-height slab surface");
  h.setBlock(new BlockPos(5,1,5),net.minecraft.world.level.block.Blocks.OAK_FENCE);
  double edge=NaturalMovementGoals.surfaceBelow(h.getLevel(),column.getX()+0.05D,column.getY(),column.getZ()+0.05D);
  check(h,Math.abs(edge-slab)<0.001D,"A fence must not replace the surface of an empty part of its column");
  h.setBlock(new BlockPos(5,1,5),net.minecraft.world.level.block.Blocks.OAK_FENCE.defaultBlockState()
      .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,true));
  double fence=NaturalMovementGoals.surfaceBelow(h.getLevel(),column.getX()+0.5D,column.getY(),column.getZ()+0.5D);
  check(h,Math.abs(fence-(h.absolutePos(new BlockPos(5,1,5)).getY()+1.5D))<0.001D,
      "A waterlogged fence's solid top must take precedence over its water surface");
  h.succeed();
 }

 @GameTest(template="test_arena",timeoutTicks=80)
 public static void electabuzzScaleAndSelectionBoxShrinkProportionally(GameTestHelper h) {
  floor(h);
  MinemonCreature electabuzz=h.spawn(ModEntities.SPECIES.get("electabuzz").get(),new BlockPos(5,1,5));
  check(h,Math.abs(electabuzz.getBbWidth()-br.com.petrickfaria.minemon.entity.ModelMetrics.width("electabuzz",br.com.petrickfaria.minemon.entity.SpeciesDefinitions.get("electabuzz").scale()))<0.001F,"Electabuzz selection width must be 75% of its former 1.4 blocks");
  check(h,Math.abs(electabuzz.getBbHeight()-br.com.petrickfaria.minemon.entity.ModelMetrics.height("electabuzz",br.com.petrickfaria.minemon.entity.SpeciesDefinitions.get("electabuzz").scale()))<0.001F,"Electabuzz selection height must be 75% of its former 3.0 blocks");
  check(h,Math.abs(br.com.petrickfaria.minemon.entity.SpeciesDefinitions.get("electabuzz").scale()-1.35F)<0.001F,
      "Electabuzz renderer scale must be 75% of its former 1.8x scale");
  h.succeed();
 }

 @GameTest(template="test_arena",timeoutTicks=1300)
 public static void aquaticSpeciesSeekWaterAndSurviveWithoutIt(GameTestHelper h) {
  floor(h);
  // All aquatic profiles require a real 3x3x3 volume; retain a separate
  // one-block puddle to ensure it cannot satisfy that requirement.
  for(int x=8;x<=12;x++)for(int z=3;z<=9;z++){
   h.setBlock(new BlockPos(x,-3,z),net.minecraft.world.level.block.Blocks.STONE);
   for(int y=-2;y<=0;y++)h.setBlock(new BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.WATER);
  }
  // Keep the seeker only one block from the open pond. At the old farther
  // spawn, unlucky rejected paths could consume the first search window and
  // make the assertion race the goal's blocked-destination retry.
  MinemonCreature goldeen=h.spawn(ModEntities.SPECIES.get("goldeen").get(),new BlockPos(7,1,5));
  int waterPriority=goldeen.goalSelector.getAvailableGoals().stream()
      .filter(goal->goal.getGoal() instanceof WaterMovementGoal).mapToInt(goal->goal.getPriority()).findFirst().orElse(Integer.MAX_VALUE);
  int spacingPriority=goldeen.goalSelector.getAvailableGoals().stream()
      .filter(goal->goal.getGoal().getClass().getSimpleName().equals("NaturalSeparationGoal"))
      .mapToInt(goal->goal.getPriority()).findFirst().orElse(Integer.MIN_VALUE);
  check(h,waterPriority<spacingPriority,"Species-specific water navigation must take priority over optional spacing: water="+waterPriority+" spacing="+spacingPriority);
  BlockPos pond=h.absolutePos(new BlockPos(10,-1,6));
  var directPath=goldeen.getNavigation().createPath(pond,0);
  com.mojang.logging.LogUtils.getLogger().info("MINEMON AQUATIC SEEK FIXTURE target="+pond
      +" targetWater="+h.getLevel().getFluidState(pond).is(net.minecraft.tags.FluidTags.WATER)
      +" 3x3x3="+br.com.petrickfaria.minemon.entity.WaterMovementGoal.isNavigableWaterRegion(pond,
          p->h.getLevel().getFluidState(p).is(net.minecraft.tags.FluidTags.WATER))
      +" directPath="+(directPath==null?"null":directPath.canReach()+" nodes="+directPath.getNodeCount())
      +" state="+goldeen.position()+" nav="+goldeen.getNavigation().getClass().getSimpleName());
  h.setBlock(new BlockPos(13,-1,10),net.minecraft.world.level.block.Blocks.STONE);
  h.setBlock(new BlockPos(13,0,10),net.minecraft.world.level.block.Blocks.WATER);
  MinemonCreature adjacent=h.spawn(ModEntities.SPECIES.get("goldeen").get(),new BlockPos(6,1,7));
  // NoAI does not disable physical pushing. Keep the dry-survival specimen
  // away from the two roaming water seekers, with a real dry enclosure;
  // otherwise incidental pushes can invalidate the dry-land precondition.
  for(int x=2;x<=4;x++)for(int z=11;z<=13;z++)for(int y=1;y<=3;y++)
   if(x==2||x==4||z==11||z==13)
    h.setBlock(new BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.STONE);
  MinemonCreature dryHorsea=h.spawn(ModEntities.SPECIES.get("horsea").get(),new BlockPos(3,1,12));
  dryHorsea.setNoAi(true);
  float health=dryHorsea.getHealth();
  check(h,dryHorsea.canBreatheUnderwater(),"Aquatic species must not drown when temporarily submerged");
  double initial=goldeen.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pond));
  h.runAfterDelay(400,()->{
   double current=goldeen.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pond));
   check(h,goldeen.isInWater(),"Aquatic species must actually enter reachable water; initial distance="+initial+" current="+current
       +" position="+goldeen.position()+" velocity="+goldeen.getDeltaMovement()+" navigation="+goldeen.getNavigation().getTargetPos()
       +" active="+goldeen.naturalBehaviorActive());
   check(h,adjacent.isInWater(),"Second aquatic seeker must reject the small puddle and enter valid water");
  });
  h.runAfterDelay(1200,()->{
   check(h,!dryHorsea.isInWater()&&dryHorsea.isAlive()&&dryHorsea.getHealth()==health,"Aquatic species must retain full health after 1200 ticks on dry land; position="+dryHorsea.position()
       +" water="+dryHorsea.isInWater()+" alive="+dryHorsea.isAlive()+" removed="+dryHorsea.isRemoved()
       +" health="+dryHorsea.getHealth()+" expected="+health);
   h.succeed();
  });
 }

 @GameTest(template="test_arena",timeoutTicks=300)
 public static void airbornePokemonRetaliatesAgainstRealPlayer(GameTestHelper h){
  playerRetaliationInMedium(h,false);
 }
 @GameTest(template="test_arena",timeoutTicks=300)
 public static void swimmingPokemonRetaliatesAgainstRealPlayer(GameTestHelper h){
  playerRetaliationInMedium(h,true);
 }
 private static void playerRetaliationInMedium(GameTestHelper h,boolean water){
  ServerPlayer p=player(h);
  if(water)for(int x=3;x<=12;x++)for(int z=3;z<=12;z++)for(int y=1;y<=5;y++)
   h.setBlock(new BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.WATER);
  BlockPos pos=h.absolutePos(new BlockPos(6,water?2:1,6));
  p.moveTo(pos.getX()+.5D,pos.getY(),pos.getZ()+.5D,0,0);p.setNoGravity(true);
  var mob=h.spawn(ModEntities.SPECIES.get(water?"goldeen":"spearow").get(),new BlockPos(8,3,6));
  h.runAfterDelay(20,()->{
   check(h,mob.getTarget()==null,"Unprovoked Pokemon must remain neutral toward the player");
   check(h,water?mob.isInWater():mob.isFlyingSpecies()&&!mob.isOnGround(),"Provocation must occur in the actual movement medium");
   float health=p.getHealth(),wildHealth=mob.getHealth();
   check(h,mob.hurt(DamageSource.playerAttack(p),12.0F),"A direct player hit must reach the wild Pokemon damage flow");
   check(h,mob.getHealth()==wildHealth-1.0F&&mob.getTarget()==null,"A direct player hit must deal one HP without making the wild Pokemon retaliate");
   watchForRetaliation(h,mob,p,200);
  });
 }
 private static void watchForRetaliation(GameTestHelper h,MinemonCreature mob,ServerPlayer player,int remaining){
  if(mob.getTarget()==player||player.getLastHurtByMob()==mob){
   h.fail("A wild Pokemon must not retaliate against the player's direct hit: "+state(mob,player));return;
  }
  if(remaining<=0){player.discard();h.succeed();return;}
  h.runAfterDelay(1,()->watchForRetaliation(h,mob,player,remaining-1));
 }

 @GameTest(template="test_arena",timeoutTicks=1000)
 public static void nativeGroundRunnerUsesSpontaneousRunOnlyDuringTravel(GameTestHelper h) {
  floor(h);
  MinemonCreature runner=h.spawn(ModEntities.SPECIES.get("tauros").get(),new BlockPos(3,1,5));
  check(h,br.com.petrickfaria.minemon.entity.SpeciesAssetIndex.clip("tauros","ground_run","run")!=null,
      "Tauros's native run clip must be selected for spontaneous running");
  runner.goalSelector.removeAllGoals();
  runner.targetSelector.removeAllGoals();
  runner.getRandom().setSeed(125L);
  awaitSpontaneousRun(h,runner,0);
 }

 private static void awaitSpontaneousRun(GameTestHelper h,MinemonCreature runner,int elapsed) {
  if(runner.getSpontaneousRunCountForValidation()>0) {
   check(h,runner.getTarget()==null,"Spontaneous running must not take over a combat target");
   double base=runner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getBaseValue();
   check(h,runner.isSpontaneouslyRunning(),"Observed run must still be active");
   check(h,Math.abs(runner.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)-base*1.55D)<0.0001D,
       "Native run must apply the temporary movement-speed multiplier");
   h.runAfterDelay(100,()->{
    check(h,!runner.isSpontaneouslyRunning(),"Spontaneous run must end within 90 ticks");
    check(h,Math.abs(runner.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)-base)<0.0001D,
        "Walking speed must be restored after the run");
    h.succeed();
   });return;
  }
  if(elapsed>=720) { h.fail("Eligible native ground runner did not start a spontaneous run during travel");return; }
  BlockPos near=h.absolutePos(new BlockPos(3,1,5));
  BlockPos far=h.absolutePos(new BlockPos(12,1,5));
  double middle=(near.getX()+far.getX())/2.0D;
  double targetX=runner.getX()>middle?near.getX()+0.5D:far.getX()+0.5D;
  runner.getNavigation().moveTo(targetX,near.getY(),near.getZ()+0.5D,1.0D);
  // Sample every tick: a short test path can finish between ten-tick polls,
  // stopping the otherwise valid run before the fixture observes its active state.
  h.runAfterDelay(1,()->awaitSpontaneousRun(h,runner,elapsed+1));
 }

}
