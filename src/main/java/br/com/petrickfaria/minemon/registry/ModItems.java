package br.com.petrickfaria.minemon.registry;
import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.SpeciesDefinitions;
import java.util.*;
import net.minecraft.world.item.*;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.*;
public final class ModItems {
 public static final CreativeModeTab MINEMON_TAB=new CreativeModeTab(Minemon.MOD_ID){@Override public ItemStack makeIcon(){return BULBASAUR_SPAWN_EGG.get().getDefaultInstance();}};
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,Minemon.MOD_ID);
 public static final Map<String,RegistryObject<Item>> EGGS=register();
 public static final RegistryObject<Item> BULBASAUR_SPAWN_EGG=EGGS.get("bulbasaur");
 private static Map<String,RegistryObject<Item>> register(){
  Map<String,RegistryObject<Item>> map=new LinkedHashMap<>();
  for(var e:ModEntities.SPECIES.entrySet()) {var d=SpeciesDefinitions.get(e.getKey());map.put(e.getKey(),ITEMS.register(e.getKey()+"_spawn_egg",()->new ForgeSpawnEggItem(e.getValue(),d.eggBase(),d.eggSpots(),new Item.Properties().tab(MINEMON_TAB))));}
  return Collections.unmodifiableMap(map);
 }
 private ModItems(){}
}
