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
  var pos=new BlockPos(0,1,0);var state=ModBlocks.BREEDING_EGG.get().defaultBlockState();
  var egg=new BreedingEggBlockEntity(pos,state);egg.initialize("bulbasaur",owner.getUUID(),6000);
  for(int n=0;n<1234;n++)BreedingEggBlockEntity.serverTick(level,pos,state,egg);
  var restored=new BreedingEggBlockEntity(pos,state);restored.load(egg.saveWithoutMetadata());
  if(restored.remainingTicks()!=4766)h.fail("Egg timer lost effective tick progress");
  var legacy=new BreedingEggBlockEntity(pos,state);legacy.initialize("bulbasaur",owner.getUUID(),12000);
  restored.load(legacy.saveWithoutMetadata());if(restored.remainingTicks()!=12000)h.fail("Existing egg progress was reset");
 }
}
