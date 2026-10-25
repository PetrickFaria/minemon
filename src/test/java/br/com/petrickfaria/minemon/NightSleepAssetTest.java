package br.com.petrickfaria.minemon;
import br.com.petrickfaria.minemon.entity.*;
import com.google.gson.*;
import java.nio.file.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class NightSleepAssetTest {
 private JsonObject animations(SpeciesDefinitions.Definition d)throws Exception{
  var root=Path.of(System.getProperty("minemon.testResources"));
  var file=root.resolve(d.animation().replace("assets/minemon/","assets/minemon_gen1/"));
  return JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonObject("animations");
 }
 @Test public void allSpeciesHaveLoopingSleepWithOriginalBlinkBones() throws Exception {
  var root=Path.of(System.getProperty("minemon.testResources"));
  for(var d:SpeciesDefinitions.ALL.values()){
   var file=root.resolve(d.animation().replace("assets/minemon/","assets/minemon_gen1/"));
   var animations=JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonObject("animations");
   var sleep=animations.getAsJsonObject(d.clips().get("night_sleep"));
   assertNotNull(d.slug(),sleep);
   if(d.slug().equals("growlithe")){
    assertEquals(.1,sleep.getAsJsonObject("bones").getAsJsonObject("eyelid_left").getAsJsonArray("position").get(0).getAsDouble(),.0001);
    assertEquals(-.1,sleep.getAsJsonObject("bones").getAsJsonObject("eyelid_right").getAsJsonArray("position").get(0).getAsDouble(),.0001);
   }
   if(d.slug().equals("haunter"))for(String side:java.util.List.of("left","right")){
    assertEquals(-.15,sleep.getAsJsonObject("bones").getAsJsonObject("closed_eyelid_"+side).getAsJsonArray("position").get(2).getAsDouble(),.0001);
    assertEquals(.2,sleep.getAsJsonObject("bones").getAsJsonObject("pupil_"+side).getAsJsonArray("position").get(2).getAsDouble(),.0001);
   }assertTrue(d.slug(),sleep.get("loop").getAsBoolean());
   assertFalse(d.slug(),sleep.toString().matches("(?s).*\\b(?:q|query)\\.r\\..*"));
   String blinkName=d.clips().get("blink");
   if(blinkName!=null&&animations.has(blinkName)){
    var blink=animations.getAsJsonObject(blinkName).getAsJsonObject("bones");
    if(blink!=null)for(var bone:blink.keySet()){
     assertTrue(d.slug()+" holds native eye bone "+bone,sleep.getAsJsonObject("bones").has(bone));
     for(var channel:sleep.getAsJsonObject("bones").getAsJsonObject(bone).entrySet())
      assertFalse(d.slug()+" closed eye must hold rather than reopen",channel.getValue().isJsonObject());
    }
   }
  }
 }
 @Test public void gengarNightRestUsesASeatedPoseAndPidgeLineSleepsWithFoldedWings() throws Exception {
  var gengar=animations(SpeciesDefinitions.get("gengar"));
  var sleep=gengar.getAsJsonObject(SpeciesDefinitions.get("gengar").clips().get("night_sleep")).getAsJsonObject("bones");
  assertEquals(-1.0,sleep.getAsJsonObject("body").getAsJsonArray("position").get(1).getAsDouble(),.0001);
  assertEquals(24.0,sleep.getAsJsonObject("leg_left").getAsJsonArray("rotation").get(0).getAsDouble(),.0001);
  assertEquals(24.0,sleep.getAsJsonObject("leg_right").getAsJsonArray("rotation").get(0).getAsDouble(),.0001);
  for(String slug:java.util.List.of("pidgey","pidgeotto","pidgeot")){
   var d=SpeciesDefinitions.get(slug);var source=animations(d);
   var sleepBones=source.getAsJsonObject(d.clips().get("night_sleep")).getAsJsonObject("bones");
   assertTrue(slug+" night sleep must retain its source folded-wing pose",sleepBones.has("wing_closed_left"));
   assertTrue(slug+" night sleep must keep the open wings in their ground pose",sleepBones.has("wing_open_left"));
   if(!slug.equals("pidgey")){
    assertEquals(java.util.List.of(0.0,0.0,0.0),asNumbers(sleepBones.getAsJsonObject("wings_open").getAsJsonArray("scale")));
    assertEquals(java.util.List.of(1.0,1.0,1.0),asNumbers(sleepBones.getAsJsonObject("wings_closed").getAsJsonArray("scale")));
   }
  }
 }
 @Test public void eeveeLineHasGroundSleepAndVaporeonHasSeparateWaterRestPose() throws Exception {
  for(String slug:java.util.List.of("eevee","jolteon","flareon","vaporeon")){
   var d=SpeciesDefinitions.get(slug);var source=animations(d);
   var sleep=source.getAsJsonObject(d.clips().get("night_sleep"));
   assertNotNull(slug+" generated night sleep",sleep);
   assertTrue(slug+" sleep loops",sleep.get("loop").getAsBoolean());
   assertEquals(slug,4.0,sleep.get("animation_length").getAsDouble(),.0001);
   var bones=sleep.getAsJsonObject("bones");
   assertTrue(slug+" lowers the body",bones.getAsJsonObject("body").getAsJsonArray("position").get(1).getAsDouble()<0);
   String blinkName=d.clips().get("blink");var blink=source.getAsJsonObject(blinkName).getAsJsonObject("bones");
   for(String eyeBone:blink.keySet()){
    assertTrue(slug+" holds closed native eye bone "+eyeBone,bones.has(eyeBone));
    var blinkPosition=blink.getAsJsonObject(eyeBone).getAsJsonObject("position").getAsJsonArray("0.0");
    assertEquals(slug+" keeps its closed blink offset",asNumbers(blinkPosition),asNumbers(bones.getAsJsonObject(eyeBone).getAsJsonArray("position")));
   }
  }
  var vaporeon=SpeciesDefinitions.get("vaporeon");
  assertNotNull(vaporeon.clips().get("night_sleep_land"));assertNotNull(vaporeon.clips().get("night_sleep_water"));
  var animations=animations(vaporeon);
  var land=animations.getAsJsonObject(vaporeon.clips().get("night_sleep_land")).getAsJsonObject("bones");
  var water=animations.getAsJsonObject(vaporeon.clips().get("night_sleep_water")).getAsJsonObject("bones");
  assertEquals(-1.1,land.getAsJsonObject("body").getAsJsonArray("position").get(1).getAsDouble(),.0001);
  assertEquals(-.2,water.getAsJsonObject("body").getAsJsonArray("position").get(1).getAsDouble(),.0001);
  assertEquals("animation.vaporeon.minemon_night_sleep_land",SpeciesAssetIndex.clip("vaporeon","night_sleep_land","night_sleep"));
  assertEquals("animation.vaporeon.minemon_night_sleep_water",SpeciesAssetIndex.clip("vaporeon","night_sleep_water","night_sleep"));
 }
 @Test public void spearowAndFearowFlightClipsUseModelWingsWithoutGroundGaits() throws Exception {
  for(String slug:java.util.List.of("spearow","fearow")){
   var d=SpeciesDefinitions.get(slug);var source=animations(d);
   assertEquals("animation."+slug+".minemon_air_fly",d.clips().get("air_fly"));
   assertEquals("animation."+slug+".minemon_air_idle",d.clips().get("air_idle"));
   var fly=source.getAsJsonObject(d.clips().get("air_fly"));assertTrue(slug,fly.get("loop").getAsBoolean());
   var bones=fly.getAsJsonObject("bones");
   for(String wing:slug.equals("spearow")
       ?java.util.List.of("wing_open_left","wing_open_left2","wing_open_left3","wing_open_right","wing_open_right2","wing_open_right3")
       :java.util.List.of("wing_open_left","wing_open_right"))
    assertTrue(slug+" must animate retained wing joint "+wing,bones.has(wing));
   if(slug.equals("fearow")){
    assertEquals(-22,bones.getAsJsonObject("body").getAsJsonArray("rotation").get(0).getAsInt());
    assertEquals(5,bones.getAsJsonObject("head").getAsJsonArray("rotation").get(0).getAsInt());
    assertEquals(-5,bones.getAsJsonObject("leg_left").getAsJsonArray("rotation").get(2).getAsInt());
    assertEquals(5,bones.getAsJsonObject("leg_right").getAsJsonArray("rotation").get(2).getAsInt());
   }
  }
 }
 @Test public void fiveRequestedFlyersUseStaticFoldedGroundPosesForNightSleep() throws Exception {
  for(String slug:java.util.List.of("spearow","fearow","articuno","zapdos","moltres")){
   var d=SpeciesDefinitions.get(slug);var source=animations(d);
   var sleep=source.getAsJsonObject(d.clips().get("night_sleep")).getAsJsonObject("bones");
   var ground=source.getAsJsonObject(d.clips().get("ground_idle")).getAsJsonObject("bones");
   boolean explicitFoldedWings=ground.keySet().stream().anyMatch(n->n.startsWith("wing_closed")||n.equals("wings_closed"));
   boolean closedRigVisibleByDefault=slug.equals("spearow")&&ground.has("wings_open")
       &&ground.getAsJsonObject("wings_open").get("scale").getAsDouble()==0.0;
   assertTrue(slug+" retains a static folded ground wing pose",explicitFoldedWings||closedRigVisibleByDefault);
   for(String bone:ground.keySet())if(bone.startsWith("wing_")||bone.equals("wings_open")||bone.equals("wings_closed"))
    assertEquals(slug+" holds ground wing pose while asleep: "+bone,ground.get(bone),sleep.get(bone));
   assertFalse(slug+" sleep cannot contain a flap oscillator",sleep.toString().contains("math.sin"));
  }
 }
 @Test public void legendaryFlightSelectsAnimatedOriginalRigAndSpearowShowsOpenWings() throws Exception {
  for(String slug:java.util.List.of("articuno","zapdos","moltres")){
   var d=SpeciesDefinitions.get(slug);var source=animations(d);
   assertEquals("animation."+slug+".minemon_air_fly",d.clips().get("air_fly"));
   assertEquals("animation."+slug+".minemon_air_idle",d.clips().get("air_idle"));
   for(String side:java.util.List.of("left","right")){
    var clip=source.getAsJsonObject(d.clips().get("air_fly"));
    var wings=clip.getAsJsonObject("bones");
    String root="wing_open_"+side;
    assertTrue(slug+" animates its real wing hinge",wings.getAsJsonObject(root).get("rotation").toString().contains("math.sin"));
    for(String name:wings.keySet())if(name.startsWith(root+"_")||name.matches(root+"\\d+"))
     assertTrue(slug+" articulates wing segment "+name,wings.getAsJsonObject(name).get("rotation").toString().contains("math.sin"));
   }
  }
  var spearow=SpeciesDefinitions.get("spearow");var air=animations(spearow).getAsJsonObject(spearow.clips().get("air_fly")).getAsJsonObject("bones");
  assertEquals(java.util.List.of(1.0,1.0,1.0),asNumbers(air.getAsJsonObject("wings_open").getAsJsonArray("scale")));
  assertEquals(java.util.List.of(0.0,0.0,0.0),asNumbers(air.getAsJsonObject("wings_closed").getAsJsonArray("scale")));
 }
 private static java.util.List<Double> asNumbers(com.google.gson.JsonArray values){
  var result=new java.util.ArrayList<Double>();for(var value:values)result.add(value.getAsDouble());return result;
 }
}
