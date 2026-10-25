package br.com.petrickfaria.minemon.gametest;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
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
  b.setNoAi(true);h.runAfterDelay(80,()->{b.setNoAi(false);b.getNavigation().stop();check(h,b.hurt(DamageSource.playerAttack(p),1),"Hit must be accepted");
  check(h,b.getTarget()==p,"Player aggressor must become target");
  h.runAfterDelay(100,()->{check(h,p.getHealth()<health,"Melee AI must actually damage the player"+state(b,p));p.discard();h.succeed();});});
 }
 @GameTest(template="test_arena",timeoutTicks=300)
 public static void wildRetaliatesAgainstCreature(GameTestHelper h) {
  MinemonCreature b=spawn(h);Cow c=h.spawn(EntityType.COW,new BlockPos(8,1,5));c.setNoAi(true);float health=c.getHealth();
  b.hurt(DamageSource.mobAttack(c),1);
  h.runAfterDelay(100,()->{check(h,c.getHealth()<health,"Melee AI must damage creature aggressor");h.succeed();});
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
  Cow c=h.spawn(EntityType.COW,new BlockPos(8,1,5));c.setNoAi(true);p.attack(c);check(h,p.getLastHurtMob()==c,"Owner attack must record its victim");float health=c.getHealth();
  h.runAfterDelay(100,()->{check(h,c.getHealth()<health,"Owner attack assistance must damage target"+state(b,c));p.discard();h.succeed();});});
 }
 @GameTest(template="test_arena",timeoutTicks=300)
 public static void ownerDefense(GameTestHelper h) {
  MinemonCreature b=spawn(h);ServerPlayer p=player(h);b.tame(p);b.setOrderedToSit(false);
  Cow c=h.spawn(EntityType.COW,new BlockPos(8,1,5));c.setNoAi(true);float health=c.getHealth();b.setNoAi(true);h.runAfterDelay(80,()->{b.setNoAi(false);b.getNavigation().stop();check(h,p.hurt(DamageSource.mobAttack(c),1),"Owner must accept damage after spawn protection");
  com.mojang.logging.LogUtils.getLogger().info("MINEMON DEFENSE AFTER HIT "+state(b,c)+" goalCanUse="+new net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal(b).canUse()+" sight="+b.getSensing().hasLineOfSight(c)+" wants="+b.wantsToAttack(c,p));
  h.runAfterDelay(5,()->com.mojang.logging.LogUtils.getLogger().info("MINEMON DEFENSE AFTER 5 TICKS "+state(b,c)));
  h.runAfterDelay(100,()->{check(h,c.getHealth()<health,"Companion must defend attacked owner"+state(b,c));p.discard();h.succeed();});});
 }
 @GameTest(template="test_arena",timeoutTicks=100)
 public static void sitAndPersistence(GameTestHelper h) {
  MinemonCreature b=spawn(h);ServerPlayer p=player(h);b.tame(p);b.setOrderedToSit(true);b.setSleeping(true);
  CompoundTag tag=new CompoundTag();b.saveWithoutId(tag);MinemonCreature restored=ModEntities.BULBASAUR.get().create(h.getLevel());restored.load(tag);
  check(h,restored.isTame()&&p.getUUID().equals(restored.getOwnerUUID())&&restored.isOrderedToSit()&&restored.isSleeping(),"Taming UUID, sit order and sleep state must survive NBT reload");
  h.runAfterDelay(20,()->{var start=b.position();h.runAfterDelay(30,()->{double dx=b.getX()-start.x,dz=b.getZ()-start.z;check(h,dx*dx+dz*dz<0.25,"Sitting creature must stay in place");p.discard();h.succeed();});});
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

 @GameTest(template="test_arena",timeoutTicks=100)
 public static void boneTamingAndSitCommands(GameTestHelper h) {
  MinemonCreature b=spawn(h);ServerPlayer p=player(h);
  int previous=br.com.petrickfaria.minemon.config.MinemonConfig.TAME_CHANCE_PERCENT.get();
  try {
   br.com.petrickfaria.minemon.config.MinemonConfig.TAME_CHANCE_PERCENT.set(100);
   p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BONE,2));
   b.mobInteract(p,net.minecraft.world.InteractionHand.MAIN_HAND);
   check(h,b.isTame()&&p.getUUID().equals(b.getOwnerUUID())&&b.isOrderedToSit(),"Bone interaction must tame, assign owner and sit");
   check(h,p.getMainHandItem().getCount()==1,"Survival taming must consume one bone");
   p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
   b.mobInteract(p,net.minecraft.world.InteractionHand.MAIN_HAND);check(h,!b.isOrderedToSit(),"Owner must be able to order standing");
   b.mobInteract(p,net.minecraft.world.InteractionHand.MAIN_HAND);check(h,b.isOrderedToSit(),"Owner must be able to order sitting");
  }finally {br.com.petrickfaria.minemon.config.MinemonConfig.TAME_CHANCE_PERCENT.set(previous);p.discard();}
  h.succeed();
 }
 @GameTest(template="test_arena",timeoutTicks=300)
 public static void followsOwner(GameTestHelper h) {
  MinemonCreature b=h.spawn(ModEntities.BULBASAUR.get(),new BlockPos(2,1,5));ServerPlayer p=player(h);
  BlockPos pos=h.absolutePos(new BlockPos(14,1,5));p.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);b.tame(p);b.setOrderedToSit(false);
  float initial=b.distanceTo(p);
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
  MinemonCreature b=spawn(h);MinemonCreature attacker=h.spawn(ModEntities.BULBASAUR.get(),new BlockPos(8,1,5));attacker.setNoAi(true);float health=attacker.getHealth();
  b.hurt(DamageSource.mobAttack(attacker),1);
  h.runAfterDelay(100,()->{check(h,attacker.getHealth()<health,"Wild Minemon must retaliate against a provoking Minemon");h.succeed();});
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
   check(h,!created.isEmpty(),"Egg did not spawn its entity: "+entry.getKey());created.forEach(net.minecraft.world.entity.Entity::discard);
  }
  p.discard();h.succeed();
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

 @GameTest(template="test_arena",batch="night_landing",timeoutTicks=600)
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
    h.succeed();
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
  if(elapsed>=240){h.fail("Flying melee AI must actually hit a terrestrial target;"+state(bird,cow));return;}
  h.runAfterDelay(5,()->checkAirborneAttack(h,bird,cow,health,elapsed+5));
 }

 @GameTest(template="test_arena",batch="night_landing",timeoutTicks=520)
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
  check(h,Math.abs(electabuzz.getBbWidth()-1.05F)<0.001F,"Electabuzz selection width must be 75% of its former 1.4 blocks");
  check(h,Math.abs(electabuzz.getBbHeight()-2.25F)<0.001F,"Electabuzz selection height must be 75% of its former 3.0 blocks");
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
   float health=p.getHealth();mob.hurt(DamageSource.playerAttack(p),1);
   check(h,mob.getTarget()==p,"Pokemon must identify the player aggressor in its movement medium");
   h.runAfterDelay(200,()->{
    check(h,p.getHealth()<health,"Existing melee combat must actually damage the player aggressor: "+state(mob,p));
    p.discard();h.succeed();
   });
  });
 }

 @GameTest(template="test_arena",timeoutTicks=1000)
 public static void nativeGroundRunnerUsesSpontaneousRunOnlyDuringTravel(GameTestHelper h) {
  floor(h);
  MinemonCreature runner=h.spawn(ModEntities.SPECIES.get("geodude").get(),new BlockPos(3,1,5));
  check(h,br.com.petrickfaria.minemon.entity.SpeciesAssetIndex.clip("geodude","ground_run","run")!=null,
      "Geodude's native run clip must be selected for spontaneous running");
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
  h.runAfterDelay(10,()->awaitSpontaneousRun(h,runner,elapsed+10));
 }

}
