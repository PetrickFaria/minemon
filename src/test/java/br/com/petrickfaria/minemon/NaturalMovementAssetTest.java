package br.com.petrickfaria.minemon;

import br.com.petrickfaria.minemon.entity.CompatibleLocomotion;
import br.com.petrickfaria.minemon.entity.SpeciesAssetIndex;
import br.com.petrickfaria.minemon.entity.SpeciesDefinitions;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public final class NaturalMovementAssetTest {
 private static final Path RESOURCES=Path.of(System.getProperty("minemon.testResources"));

 private JsonObject animations(String slug) throws Exception {
  var definition=SpeciesDefinitions.get(slug);
  String source=definition.animation().replace("assets/minemon/","assets/minemon_gen1/");
  try(var reader=Files.newBufferedReader(RESOURCES.resolve(source))){return JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");}
 }

 private void assertDynamicBone(JsonObject animations,String clip,String bone) {
  assertTrue("missing generated clip "+clip,animations.has(clip));
  var channels=animations.getAsJsonObject(clip).getAsJsonObject("bones");
  assertNotNull("missing bone channel "+bone,channels.get(bone));
  assertTrue("bone must animate over time "+bone,channels.getAsJsonObject(bone).get("rotation").toString().contains("math.sin"));
 }

 @Test public void meowthAndPersianHaveGeneratedWalkAndRunOnTheirOwnLegRigs() throws Exception {
  for(String slug:List.of("meowth","persian")) {
   var animations=animations(slug);
   for(String key:List.of("ground_walk","ground_run")) {
    String clip=SpeciesAssetIndex.clip(slug,key,key);
    for(String joint:CompatibleLocomotion.gaitJoints(slug))assertDynamicBone(animations,clip,joint);
   }
  }
 }

 @Test public void sourceFlightPosesStayStaticAndNoWingCycleIsSynthesized() throws Exception {
  for(String slug:List.of("articuno","zapdos","moltres")) {
   var animations=animations(slug);
   assertTrue(slug+" keeps its source flight clip",animations.has("animation."+slug+".air_fly"));
   assertTrue(slug+" keeps its source air idle",animations.has("animation."+slug+".air_idle"));
   assertFalse(slug+" has no generated wing cycle",animations.has("animation."+slug+".minemon_fly"));
   for(String bone:List.of("wing_open_left","wing_open_right")) {
    var track=animations.getAsJsonObject("animation."+slug+".air_fly").getAsJsonObject("bones").getAsJsonObject(bone).get("rotation");
    assertNotNull(slug+" native pose for "+bone,track);
    assertTrue(slug+" source pose remains a static vector for "+bone,track.isJsonArray()&&track.getAsJsonArray().size()==3);
   }
   var clips=new java.util.HashMap<>(SpeciesDefinitions.get(slug).clips());
   String nativeFlight=clips.get("air_fly");CompatibleLocomotion.apply(slug,clips);
   assertEquals(slug+" still selects native air_fly",nativeFlight,clips.get("air_fly"));
  }
  var fearow=animations("fearow");
  assertTrue("Fearow keeps its native static air pose",fearow.has("animation.fearow.air_idle"));
  assertFalse("Fearow has no invented air cycle",fearow.has("animation.fearow.minemon_fly"));
  var fearowClips=new java.util.HashMap<>(SpeciesDefinitions.get("fearow").clips());
  CompatibleLocomotion.apply("fearow",fearowClips);
  assertFalse("Fearow does not get a synthetic air_fly mapping",fearowClips.containsKey("air_fly"));
  var spearow=animations("spearow");
  assertFalse("Spearow has no generated air cycle",spearow.has("animation.spearow.minemon_fly"));
  assertEquals("Spearow uses its retained source poser walk", "animation.spearow.minemon_poser_walk",SpeciesDefinitions.get("spearow").walk());
  try(var reader=Files.newBufferedReader(RESOURCES.resolve("data/minemon_gen1/animation_validation_profiles.json"))) {
   var profile=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("spearow");
   assertEquals("leg_left",profile.getAsJsonArray("walk").get(0).getAsString());
   assertEquals("leg_right",profile.getAsJsonArray("walk").get(1).getAsString());
  }
 }
}
