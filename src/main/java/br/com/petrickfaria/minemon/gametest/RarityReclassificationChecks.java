package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.PokemonProgression;
import br.com.petrickfaria.minemon.pokec.*;
import br.com.petrickfaria.minemon.registry.ModEntities;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.gametest.*;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.*;

public final class RarityReclassificationChecks {
 public static void all151EntitiesUseCatalogStatsAndSavedProgress(GameTestHelper h){
  for(var s:PokecSpecies.ordered()){
   var m=ModEntities.SPECIES.get(s.slug()).get().create(h.getLevel());
   m.setPokemonLevel(5);
   if(!m.getPokemonRarity().equals(s.rarity())
     ||m.getMaxHealth()!=PokemonProgression.healthForRarity(s.rarity(),5)
     ||m.getAttributeValue(Attributes.ATTACK_DAMAGE)!=PokemonProgression.damageForRarity(s.rarity(),5))h.fail("Rarity/stat mismatch: "+s.slug());
   m.setHealth(3);var saved=m.saveWithoutId(new CompoundTag());
   var copy=ModEntities.SPECIES.get(s.slug()).get().create(h.getLevel());copy.load(saved);
   if(copy.getPokemonLevel()!=5||copy.getHealth()!=3||copy.getMaxHealth()!=m.getMaxHealth()
     ||!copy.getPokemonRarity().equals(s.rarity()))h.fail("Reload changed progress: "+s.slug());
  }
 }
 public static void reclassificationKeepsOverflowXpUntilNextAward(GameTestHelper h){
  var m=ModEntities.SPECIES.get("rattata").get().create(h.getLevel());
  var owner=java.util.UUID.randomUUID();m.setTame(true);m.setOwnerUUID(owner);m.setPokemonLevel(3);m.setHealth(3);
  var saved=m.saveWithoutId(new CompoundTag());saved.putInt("MinemonPokemonExperience",37);
  m.load(saved);m.load(m.saveWithoutId(new CompoundTag()));
  if(m.getPokemonLevel()!=3||m.getPokemonExperience()!=37||m.getHealth()!=3||!owner.equals(m.getOwnerUUID()))h.fail("Existing XP, level, health or owner was lost");
  if(m.awardPokemonExperience(1)!=1||m.getPokemonLevel()!=4||m.getPokemonExperience()!=8||m.getHealth()!=5)h.fail("Saved overflow must carry forward without duplicated growth");
 }
 public static void oldConfigCannotOverrideCatalogAndUnrelatedSettingsSurvive(GameTestHelper h) throws Exception{
  Path file=FMLPaths.CONFIGDIR.get().resolve("minemon/pokec-economy.toml");byte[] original=Files.readAllBytes(file);
  try{
   Files.writeString(file,"economyVersion = 4\nrarityVersion = 3\nspecies.bulbasaur.rarity = \"common\"\nspecies.mewtwo.rarity = \"legendary\"\nspecies.mewtwo.rarity = \"rare\"\nspecies.bulbasaur.coins = 37\nyield.stage.initial = 7\n");
   PokecEconomy.load();String migrated=Files.readString(file);
   for(var s:PokecSpecies.ordered())if(!PokecEconomy.get().rarity(s.slug()).equals(s.rarity()))h.fail("Old override retained: "+s.slug());
   if(migrated.lines().filter(l->l.startsWith("species.")&&l.contains(".rarity")).count()!=151
    ||!migrated.contains("rarityVersion = 4")||PokecEconomy.get().coins("bulbasaur")!=37
    ||PokecEconomy.get().stageSouls(PokemonSoulConversion.Stage.INITIAL)!=7)h.fail("Migration duplicated rarities or changed unrelated settings");
   PokecEconomy.load();if(!Files.readString(file).equals(migrated))h.fail("Migration is not idempotent");
  }finally{Files.write(file,original);PokecEconomy.load();}
 }
}
