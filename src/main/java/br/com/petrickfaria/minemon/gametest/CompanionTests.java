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
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
@GameTestHolder("minemon")
@PrefixGameTestTemplate(false)
public final class CompanionTests {
 private static void check(GameTestHelper h,boolean condition,String message) { if (!condition) h.fail(message); }
 private static String state(MinemonCreature b, net.minecraft.world.entity.LivingEntity target) { return " target="+b.getTarget()+" canAttack="+b.canAttack(target)+" dist="+b.distanceTo(target)+" pos="+b.position()+" targetPos="+target.position()+" alive="+target.isAlive()+" health="+target.getHealth()+" owner="+b.getOwner()+" sit="+b.isInSittingPose()+" navDone="+b.getNavigation().isDone(); }
 private static MinemonCreature spawn(GameTestHelper h) {
  return h.spawn(ModEntities.BULBASAUR.get(),new BlockPos(5,1,5));
 }
 private static FakePlayer player(GameTestHelper h) {
  FakePlayer p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"MinemonTester")) {
   @Override public boolean isInvulnerableTo(DamageSource source) { return false; }
  };
  h.getLevel().getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
  p.setGameMode(GameType.SURVIVAL);p.tickCount=100;
  BlockPos pos=h.absolutePos(new BlockPos(7,1,5));
  p.moveTo(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,0,0);
  h.getLevel().addNewPlayer(p);
  return p;
 }
 @GameTest(template="test_arena",timeoutTicks=180)
 public static void wildRetaliatesAgainstPlayer(GameTestHelper h) {
  MinemonCreature b=spawn(h); FakePlayer p=player(h);float health=p.getHealth();
  check(h,b.canAttackType(EntityType.PLAYER),"Animal player target restriction must be overridden");
  check(h,b.hurt(DamageSource.playerAttack(p),1),"Hit must be accepted");
  check(h,b.getTarget()==p,"Player aggressor must become target");
  h.runAfterDelay(100,()->{check(h,p.getHealth()<health,"Melee AI must actually damage the player"+state(b,p));p.discard();h.succeed();});
 }
 @GameTest(template="test_arena",timeoutTicks=180)
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
 @GameTest(template="test_arena",timeoutTicks=180)
 public static void ownerProtectedAndAssisted(GameTestHelper h) {
  MinemonCreature b=spawn(h);FakePlayer p=player(h);b.tame(p);b.setOrderedToSit(false);
  b.hurt(DamageSource.playerAttack(p),1);check(h,b.getTarget()!=p,"Never retaliate against owner");
  Cow c=h.spawn(EntityType.COW,new BlockPos(8,1,5));c.setNoAi(true);p.attack(c);float health=c.getHealth();
  h.runAfterDelay(100,()->{check(h,c.getHealth()<health,"Owner attack assistance must damage target"+state(b,c));p.discard();h.succeed();});
 }
 @GameTest(template="test_arena",timeoutTicks=180)
 public static void ownerDefense(GameTestHelper h) {
  MinemonCreature b=spawn(h);FakePlayer p=player(h);b.tame(p);b.setOrderedToSit(false);
  Cow c=h.spawn(EntityType.COW,new BlockPos(8,1,5));c.setNoAi(true);float health=c.getHealth();p.hurt(DamageSource.mobAttack(c),1);
  h.runAfterDelay(100,()->{check(h,c.getHealth()<health,"Companion must defend attacked owner"+state(b,c));p.discard();h.succeed();});
 }
 @GameTest(template="test_arena",timeoutTicks=100)
 public static void sitAndPersistence(GameTestHelper h) {
  MinemonCreature b=spawn(h);FakePlayer p=player(h);b.tame(p);b.setOrderedToSit(true);
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

}
