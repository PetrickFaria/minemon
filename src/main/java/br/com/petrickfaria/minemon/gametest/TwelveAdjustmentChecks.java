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
  var owner=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"RecoveryTester"),null);
  owner.connection=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"RecoveryConnection")).connection;
  var ownerPos=h.absolutePos(new BlockPos(7,1,7));owner.moveTo(ownerPos.getX()+.5,ownerPos.getY(),ownerPos.getZ()+.5,0,0);
  level.addNewPlayer(owner);
  try{
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
  verifyRecipes(h,owner);
  verifyExactHatching(h);
  var pos=new BlockPos(0,1,0);var state=ModBlocks.BREEDING_EGG.get().defaultBlockState();
  var egg=new BreedingEggBlockEntity(pos,state);egg.initialize("bulbasaur",owner.getUUID(),6000);
  for(int n=0;n<1234;n++)BreedingEggBlockEntity.serverTick(level,pos,state,egg);
  var restored=new BreedingEggBlockEntity(pos,state);restored.load(egg.saveWithoutMetadata());
  if(restored.remainingTicks()!=4766)h.fail("Egg timer lost effective tick progress");
  var legacy=new BreedingEggBlockEntity(pos,state);legacy.initialize("bulbasaur",owner.getUUID(),12000);
  restored.load(legacy.saveWithoutMetadata());if(restored.remainingTicks()!=12000)h.fail("Existing egg progress was reset");
  }finally{owner.discard();}
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

 private static void verifyRecipes(GameTestHelper h,net.minecraft.server.level.ServerPlayer owner){
  for(var type:br.com.petrickfaria.minemon.item.PokemonTreatType.values()){
   var id=new net.minecraft.resources.ResourceLocation("minemon",type.itemId());
   var found=h.getLevel().getRecipeManager().byKey(id).orElse(null);
   if(!(found instanceof net.minecraft.world.item.crafting.ShapedRecipe))h.fail("Missing shaped treat recipe: "+id);
   var recipe=(net.minecraft.world.item.crafting.ShapedRecipe)found;
   var grid=new net.minecraft.world.inventory.CraftingContainer(owner.inventoryMenu,3,3);
   var soul=ModItems.SOULS.get(type.pokemonType()).get();
   grid.setItem(4,new ItemStack(net.minecraft.world.item.Items.BONE));
   for(int slot:new int[]{1,3,5,7})grid.setItem(slot,new ItemStack(soul));
   if(!recipe.matches(grid,h.getLevel()) || recipe.getResultItem().getItem()!=ModItems.TREATS.get(type).get() || recipe.getResultItem().getCount()!=1)
    h.fail("Invalid cross-shaped treat recipe: "+id);
   var other=type.pokemonType().equals("fire")?"water":"fire";
   grid.setItem(1,new ItemStack(ModItems.SOULS.get(other).get()));
   if(recipe.matches(grid,h.getLevel()))h.fail("Mixed elemental souls accepted: "+id);
  }
 }
 private static void verifyExactHatching(GameTestHelper h){
  var level=h.getLevel();var pos=h.absolutePos(new BlockPos(12,1,12));
  var previousFloor=level.getBlockState(pos.below());var previous=level.getBlockState(pos);
  var state=ModBlocks.BREEDING_EGG.get().defaultBlockState();var owner=UUID.randomUUID();
  level.setBlock(pos.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
  level.setBlock(pos,state,3);
  var egg=(BreedingEggBlockEntity)level.getBlockEntity(pos);egg.initialize("caterpie",owner,6000);
  try{
   for(int tick=0;tick<5999;tick++)BreedingEggBlockEntity.serverTick(level,pos,state,egg);
   if(egg.remainingTicks()!=1 || level.getBlockEntity(pos)!=egg)h.fail("Egg hatched before 6000 ticks");
   egg.load(egg.saveWithoutMetadata());
   BreedingEggBlockEntity.serverTick(level,pos,state,egg);
   var hatchlings=level.getEntitiesOfClass(MinemonCreature.class,new net.minecraft.world.phys.AABB(pos).inflate(4),m->owner.equals(m.getOwnerUUID()));
   if(hatchlings.size()!=1 || !level.getBlockState(pos).isAir())h.fail("Egg did not hatch exactly once at tick 6000");
   BreedingEggBlockEntity.serverTick(level,pos,state,egg);
   if(level.getEntitiesOfClass(MinemonCreature.class,new net.minecraft.world.phys.AABB(pos).inflate(4),m->owner.equals(m.getOwnerUUID())).size()!=1)h.fail("Egg duplicated hatchling");
  }finally{
   for(var child:level.getEntitiesOfClass(MinemonCreature.class,new net.minecraft.world.phys.AABB(pos).inflate(4),m->owner.equals(m.getOwnerUUID())))child.discard();
   level.setBlock(pos,previous,3);level.setBlock(pos.below(),previousFloor,3);
  }
 }

}
