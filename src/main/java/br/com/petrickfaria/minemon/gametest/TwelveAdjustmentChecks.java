package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.entity.*;
import br.com.petrickfaria.minemon.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import com.mojang.authlib.GameProfile;
import java.util.UUID;

/** Isolated synchronous checks inside the existing persistence batch. */
final class TwelveAdjustmentChecks {
 static void verify(GameTestHelper h){
  var level=h.getLevel();
  var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"RecoveryTester"));
  var mob=ModEntities.SPECIES.get("bulbasaur").get().create(level);
  mob.tame(owner);mob.setPokemonProgress(3,2);
  mob.die(DamageSource.GENERIC);
  if(!mob.isDefeated() || !mob.isAlive() || mob.isRemoved() || !mob.isSleeping())h.fail("Defeat must preserve living entity identity and sleep pose");
  if(mob.awardPokemonExperience(5)!=0)h.fail("Defeated companion gained XP");
  mob.heal(999);if(mob.getHealth()!=0)h.fail("Natural healing revived defeated companion");
  var saved=mob.saveWithoutId(new CompoundTag());
  var copy=ModEntities.SPECIES.get("bulbasaur").get().create(level);copy.load(saved);
  if(!copy.isDefeated() || !copy.getUUID().equals(mob.getUUID()) || copy.getPokemonLevel()!=3 || copy.getPokemonExperience()!=2 || !owner.getUUID().equals(copy.getOwnerUUID()))h.fail("Defeat lost identity, owner or progress on reload");
  var treat=ModItems.TREATS.get(br.com.petrickfaria.minemon.item.PokemonTreatType.GRASS).get();
  owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(treat,64));
  copy.mobInteract(owner,InteractionHand.MAIN_HAND);
  if(copy.getHealth()<copy.getMaxHealth() && !copy.isDefeated())h.fail("Partial treat healing revived companion");
  for(int n=0;n<64 && copy.isDefeated();n++)copy.mobInteract(owner,InteractionHand.MAIN_HAND);
  if(copy.isDefeated() || copy.getHealth()!=copy.getMaxHealth())h.fail("Matching treats failed to fully recover companion");
  var creeper=EntityType.CREEPER.create(level);creeper.setPos(copy.getX()+2,copy.getY(),copy.getZ());
  if(copy.isBlackListTarget(creeper))h.fail("Creeper must be unselected by default");
  var id=new net.minecraft.resources.ResourceLocation("minecraft","creeper");copy.setBlackListOverride(id,true);
  saved=copy.saveWithoutId(new CompoundTag());mob.load(saved);
  if(!mob.isBlackListTarget(creeper))h.fail("Explicit creeper choice did not persist");
  creeper.setPos(mob.getX()+6,mob.getY(),mob.getZ());if(mob.canAttack(creeper))h.fail("Tamed attacker accepted target beyond five blocks");
  verifyCombat(h);
  var pos=new BlockPos(0,1,0);var state=ModBlocks.BREEDING_EGG.get().defaultBlockState();
  var egg=new BreedingEggBlockEntity(pos,state);egg.initialize("bulbasaur",owner.getUUID(),6000);
  for(int n=0;n<1234;n++)BreedingEggBlockEntity.serverTick(level,pos,state,egg);
  var restored=new BreedingEggBlockEntity(pos,state);restored.load(egg.saveWithoutMetadata());
  if(restored.remainingTicks()!=4766)h.fail("Egg timer lost effective tick progress");
  var legacy=new BreedingEggBlockEntity(pos,state);legacy.initialize("bulbasaur",owner.getUUID(),12000);
  restored.load(legacy.saveWithoutMetadata());if(restored.remainingTicks()!=12000)h.fail("Existing egg progress was reset");
 }
 private static void verifyCombat(GameTestHelper h){
  var level=h.getLevel();long originalTime=level.getGameTime();
  var attacker=ModEntities.SPECIES.get("charmander").get().create(level);
  var point=h.absolutePos(new BlockPos(5,2,5));attacker.moveTo(point.getX()+.5,point.getY(),point.getZ()+.5,0,0);
  attacker.setTame(true);attacker.setOwnerUUID(UUID.randomUUID());attacker.setNoAi(true);
  var cow=EntityType.COW.create(level);cow.moveTo(attacker.getX()+2,attacker.getY(),attacker.getZ(),0,0);
  cow.setNoAi(true);cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);cow.setHealth(200);
  level.addFreshEntity(cow);
  try{
   for(int i=0;i<4;i++){
    cow.invulnerableTime=0;
    if(!attacker.doHurtTarget(cow))h.fail("Valid melee strike was cancelled at sequence "+i);
    if(attacker.doHurtTarget(cow))h.fail("Melee cooldown permitted a strike before 50 ticks");
    level.getServer().getWorldData().overworldData().setGameTime(level.getGameTime()+50);
   }
   if(!attacker.projectileReady())h.fail("Four successful melee strikes must prepare a projectile");
   if(attacker.doHurtTarget(cow))h.fail("Fifth strike incorrectly remained melee");
   var saved=attacker.saveWithoutId(new CompoundTag());
   var restored=ModEntities.SPECIES.get("charmander").get().create(level);restored.load(saved);
   if(!restored.projectileReady())h.fail("Combat sequence did not persist");
   cow.invulnerableTime=0;float before=cow.getHealth();
   if(!attacker.fireElementalProjectile(cow) || attacker.projectileReady())h.fail("Fifth strike did not spawn exactly one projectile and reset sequence");
   var shots=level.getEntitiesOfClass(ElementalProjectile.class,attacker.getBoundingBox().inflate(5));
   if(shots.size()!=1)h.fail("Projectile was absent or duplicated");
   var shot=shots.get(0);
   if(shot.color()!=EggAppearance.color("fire"))h.fail("Projectile did not use the first elemental type");
   for(int tick=0;tick<15 && !shot.isRemoved();tick++)shot.tick();
   float expected=(float)attacker.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
   if(Math.abs(before-cow.getHealth()-expected)>.001 || cow.isOnFire())h.fail("Projectile damage must equal basic attack damage, once, without burning");
  }finally{
   for(var shot:level.getEntitiesOfClass(ElementalProjectile.class,attacker.getBoundingBox().inflate(8)))shot.discard();
   cow.discard();level.getServer().getWorldData().overworldData().setGameTime(originalTime);
  }
 }

}
