package br.com.petrickfaria.minemon.gametest;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
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
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="minemon")
@GameTestHolder("minemon")
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
 private static void floor(GameTestHelper h) { for(int x=0;x<16;x++)for(int z=0;z<16;z++)h.setBlock(new BlockPos(x,0,z),net.minecraft.world.level.block.Blocks.STONE); }
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
  System.out.println("MINEMON DEFENSE AFTER HIT "+state(b,c)+" goalCanUse="+new net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal(b).canUse()+" sight="+b.getSensing().hasLineOfSight(c)+" wants="+b.wantsToAttack(c,p));
  h.runAfterDelay(5,()->System.out.println("MINEMON DEFENSE AFTER 5 TICKS "+state(b,c)));
  h.runAfterDelay(100,()->{check(h,c.getHealth()<health,"Companion must defend attacked owner"+state(b,c));p.discard();h.succeed();});});
 }
 @GameTest(template="test_arena",timeoutTicks=100)
 public static void sitAndPersistence(GameTestHelper h) {
  MinemonCreature b=spawn(h);ServerPlayer p=player(h);b.tame(p);b.setOrderedToSit(true);
  CompoundTag tag=new CompoundTag();b.saveWithoutId(tag);MinemonCreature restored=ModEntities.BULBASAUR.get().create(h.getLevel());restored.load(tag);
  check(h,restored.isTame()&&p.getUUID().equals(restored.getOwnerUUID())&&restored.isOrderedToSit(),"Taming UUID and sit order must survive NBT reload");
  h.runAfterDelay(20,()->{var start=b.position();h.runAfterDelay(30,()->{double dx=b.getX()-start.x,dz=b.getZ()-start.z;check(h,dx*dx+dz*dz<0.25,"Sitting creature must stay in place");p.discard();h.succeed();});});
 }
 @GameTest(template="test_arena",timeoutTicks=100)
 public static void registeredSpeciesSpawnPersistAndHaveEggs(GameTestHelper h) {
  for(var entry:ModEntities.SPECIES.entrySet()) {
   MinemonCreature b=entry.getValue().get().create(h.getLevel());
   check(h,b!=null,"Entity creation failed: "+entry.getKey());
   var egg=(net.minecraft.world.item.SpawnEggItem)br.com.petrickfaria.minemon.registry.ModItems.EGGS.get(entry.getKey()).get();
   check(h,egg.getType(null)==entry.getValue().get(),"Wrong spawn egg mapping: "+entry.getKey());
   b.setOwnerUUID(UUID.randomUUID());b.setTame(true);b.setOrderedToSit(true);CompoundTag tag=new CompoundTag();b.saveWithoutId(tag);
   MinemonCreature restored=entry.getValue().get().create(h.getLevel());restored.load(tag);
   check(h,restored.isTame()&&restored.isOrderedToSit()&&b.getOwnerUUID().equals(restored.getOwnerUUID()),"Persistence failed: "+entry.getKey());
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
  h.runAfterDelay(430,()->{check(h,b.getTarget()==null,"Expired aggression must not be reacquired by HurtByTargetGoal");h.succeed();});
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

}
