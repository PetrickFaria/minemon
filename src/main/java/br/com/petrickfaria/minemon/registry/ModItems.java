package br.com.petrickfaria.minemon.registry;
import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.SpeciesDefinitions;
import br.com.petrickfaria.minemon.pokec.SoulItem;
import br.com.petrickfaria.minemon.pokec.SoulType;
import br.com.petrickfaria.minemon.item.PokemonTreatItem;
import br.com.petrickfaria.minemon.item.PokemonTreatType;
import java.util.*;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.*;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.*;
public final class ModItems {
 public static final CreativeModeTab MINEMON_TAB=new CreativeModeTab(Minemon.MOD_ID){
  @Override public ItemStack makeIcon(){return BULBASAUR_SPAWN_EGG.get().getDefaultInstance();}
  @Override public void fillItemList(NonNullList<ItemStack> items){
   EGGS.entrySet().stream()
    .sorted(Comparator.comparingInt(e->SpeciesDefinitions.get(e.getKey()).nationalPokedexNumber()))
    .forEach(e->items.add(e.getValue().get().getDefaultInstance()));
  items.add(POKEC.get().getDefaultInstance());
  items.add(ROCKET_BLOCK.get().getDefaultInstance());
  SOULS.values().forEach(s->items.add(s.get().getDefaultInstance()));
  TREATS.values().forEach(s->items.add(s.get().getDefaultInstance()));
  }
 };
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,Minemon.MOD_ID);
 public static final DeferredRegister<Item> TREAT_ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,"minemon");
 public static final Map<String,RegistryObject<Item>> EGGS=register();
 public static final RegistryObject<Item> POKEC=ITEMS.register("pokec",()->new PokecBlockItem(ModBlocks.POKEC.get(),new Item.Properties().tab(MINEMON_TAB)));
 public static final RegistryObject<Item> ROCKET_BLOCK=ITEMS.register("rocket_block",()->new BlockItem(ModBlocks.ROCKET_BLOCK.get(),new Item.Properties().tab(MINEMON_TAB)));
 public static final Map<String,RegistryObject<Item>> SOULS=registerSouls();
 public static final Map<PokemonTreatType,RegistryObject<Item>> TREATS=registerTreats();
 public static final RegistryObject<Item> BULBASAUR_SPAWN_EGG=EGGS.get("bulbasaur");
 private static Map<String,RegistryObject<Item>> register(){
  Map<String,RegistryObject<Item>> map=new LinkedHashMap<>();
  for(var e:ModEntities.SPECIES.entrySet()) {var d=SpeciesDefinitions.get(e.getKey());map.put(e.getKey(),ITEMS.register(e.getKey()+"_spawn_egg",()->new ForgeSpawnEggItem(e.getValue(),d.eggBase(),d.eggSpots(),new Item.Properties().tab(MINEMON_TAB))));}
  return Collections.unmodifiableMap(map);
 }
 private static Map<PokemonTreatType,RegistryObject<Item>> registerTreats(){
  Map<PokemonTreatType,RegistryObject<Item>> map=new LinkedHashMap<>();
  for(PokemonTreatType type:PokemonTreatType.values())map.put(type,TREAT_ITEMS.register(type.itemId(),()->new PokemonTreatItem(type,new Item.Properties().stacksTo(64).tab(MINEMON_TAB))));
  return Collections.unmodifiableMap(map);
 }
 private static Map<String,RegistryObject<Item>> registerSouls(){
  Map<String,RegistryObject<Item>> map=new LinkedHashMap<>();
  for(SoulType type:SoulType.values())map.put(type.id(),ITEMS.register(type.id()+"_soul",()->new SoulItem(type,new Item.Properties().stacksTo(64).tab(MINEMON_TAB))));
  return Collections.unmodifiableMap(map);
 }
 private ModItems(){}
}
