package br.com.petrickfaria.minemon.gametest;
import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.*;
import br.com.petrickfaria.minemon.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
@GameTestHolder(Minemon.MOD_ID) @PrefixGameTestTemplate(false)
public class RefinementGameTests {
 @GameTest(template="test_arena",batch="refinement_stats",timeoutTicks=40)
 public static void levelAttributesPersistWithoutHealingOrDuplicating(GameTestHelper h) throws Exception{
  var mob=ModEntities.SPECIES.get("charmander").get().create(h.getLevel());
  mob.setPokemonLevel(1);double hp=PokemonProgression.healthForRarity(mob.getPokemonRarity(),1);
  mob.setHealth(3);mob.setPokemonLevel(5);
  if(mob.getMaxHealth()!=hp+8||mob.getHealth()!=11)h.fail("Level growth must add 8 HP, without full healing");
  double damage=mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
  var data=mob.saveWithoutId(new CompoundTag());var copy=ModEntities.SPECIES.get("charmander").get().create(h.getLevel());copy.load(data);
  if(copy.getMaxHealth()!=mob.getMaxHealth()||copy.getHealth()!=11||copy.getAttributeValue(Attributes.ATTACK_DAMAGE)!=damage)h.fail("Reload duplicated stat growth or healed");
  copy.setPokemonProgress(5,copy.getPokemonExperience());if(copy.getHealth()!=11)h.fail("Same-level update must not heal");
  // Keep rarity checks in the existing persistence batch: new named batches
  // reorder shared-world legacy arenas and contaminate their water/time fixtures.
  RarityReclassificationChecks.all151EntitiesUseCatalogStatsAndSavedProgress(h);
  RarityReclassificationChecks.reclassificationKeepsOverflowXpUntilNextAward(h);
  RarityReclassificationChecks.oldConfigCannotOverrideCatalogAndUnrelatedSettingsSurvive(h);
  TwelveAdjustmentChecks.verify(h);
  h.succeed();
 }
 @GameTest(template="test_arena",batch="refinement_water_sleep",timeoutTicks=240)
 public static void submergedSleepKeepsDepthAndSurfaceSleepSinksTwoBlocks(GameTestHelper h){
  for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=0;y<=11;y++)
   h.setBlock(new BlockPos(x,y,z),y==0||x==0||x==15||z==0||z==15?Blocks.STONE:y<=8?Blocks.WATER:Blocks.AIR);
  h.getLevel().setDayTime(18000);
  var deep=h.spawn(ModEntities.SPECIES.get("magikarp").get(),new BlockPos(4,4,5));
  var surface=h.spawn(ModEntities.SPECIES.get("magikarp").get(),new BlockPos(11,8,10));
  var lapras=h.spawn(ModEntities.SPECIES.get("lapras").get(),new BlockPos(8,8,3));
  double start=deep.getY();
  h.runAfterDelay(120,()->{
   double top=WaterMovementGoal.restSurfaceAt(surface,surface.blockPosition());
   if(!deep.isSleeping()||Math.abs(deep.getY()-start)>.15)h.fail("Submerged sleeper changed depth");
   if(!surface.isSleeping()||Math.abs(surface.getY()-(top-2))>.2)h.fail("Surface sleeper did not settle two blocks below surface");
   double laprasY=WaterMovementGoal.restSurfaceAt(lapras,lapras.blockPosition())-WaterMovementGoal.preferredSurfaceDepth(lapras);
   if(!lapras.isSleeping()||Math.abs(lapras.getY()-laprasY)>.2)h.fail("Lapras must sleep at the surface");
   var saved=surface.saveWithoutId(new CompoundTag());var copy=ModEntities.SPECIES.get("magikarp").get().create(h.getLevel());copy.load(saved);
   if(Math.abs(copy.aquaticSleepY()-surface.aquaticSleepY())>.001)h.fail("Sleep depth anchor did not persist");
   deep.discard();surface.discard();lapras.discard();h.getLevel().setDayTime(1000);h.succeed();
  });
 }
 @GameTest(template="test_arena",batch="refinement_egg",timeoutTicks=40)
 public static void eggAccentIsSavedAndReloaded(GameTestHelper h){
  var pos=h.absolutePos(new BlockPos(5,1,5));h.setBlock(new BlockPos(5,1,5),ModBlocks.BREEDING_EGG.get());
  var egg=(BreedingEggBlockEntity)h.getLevel().getBlockEntity(pos);egg.initialize("charizard",java.util.UUID.randomUUID(),24000);
  String selected=egg.elementType();var data=egg.saveWithoutMetadata();
  var copy=new BreedingEggBlockEntity(pos,ModBlocks.BREEDING_EGG.get().defaultBlockState());copy.load(data);
  if(!copy.elementType().equals(selected)||!java.util.List.of("fire","flying").contains(selected))h.fail("Egg accent rerolled or used an invalid type");
  if(copy.remainingTicks()!=24000)h.fail("Appearance changed incubation");h.succeed();
 }
}
