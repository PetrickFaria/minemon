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

 @Test public void allFourRequestedFlightRigsHaveAnimatedWingsAndKeepNativeIdle() throws Exception {
  var roots=java.util.Map.of(
      "articuno",List.of("wing_open_left","wing_open_right"),
      "zapdos",List.of("wing_open_left","wing_open_right"),
      "moltres",List.of("wing_open_left","wing_open_right"),
      "fearow",List.of("wing_open_left","wing_open_right"));
  for(var entry:roots.entrySet()) {
   var animations=animations(entry.getKey());
   String flight=SpeciesAssetIndex.clip(entry.getKey(),"air_fly","air_fly");
   for(String bone:entry.getValue())assertDynamicBone(animations,flight,bone);
   assertTrue(entry.getKey()+" retains its source idle",animations.has("animation."+entry.getKey()+".air_idle"));
  }
 }
}
