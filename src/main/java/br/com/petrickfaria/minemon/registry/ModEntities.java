package br.com.petrickfaria.minemon.registry;
import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.*;
import java.util.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;
public final class ModEntities {
 public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,Minemon.MOD_ID);
 public static final Map<String,RegistryObject<EntityType<MinemonCreature>>> SPECIES=register();
 public static final RegistryObject<EntityType<MinemonCreature>> BULBASAUR=SPECIES.get("bulbasaur");
 private static Map<String,RegistryObject<EntityType<MinemonCreature>>> register(){
  Map<String,RegistryObject<EntityType<MinemonCreature>>> map=new LinkedHashMap<>();
  for(var d:SpeciesDefinitions.ALL.values())map.put(d.slug(),ENTITIES.register(d.slug(),()->{
   EntityType.EntityFactory<MinemonCreature> factory=d.mode().equals("flying")?MinemonFlyingCreature::new:MinemonCreature::new;
   return EntityType.Builder.of(factory,MobCategory.CREATURE).sized(d.width(),d.height()).clientTrackingRange(10).build("minemon:"+d.slug());
  }));
  return Collections.unmodifiableMap(map);
 }
 @Mod.EventBusSubscriber(modid=Minemon.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD)
 public static final class Attributes {
  @SubscribeEvent public static void register(EntityAttributeCreationEvent event){for(var e:SPECIES.entrySet())event.put(e.getValue().get(),MinemonCreature.createAttributes(SpeciesDefinitions.get(e.getKey())).build());}
 }
 private ModEntities(){}
}
