package br.com.petrickfaria.minemon;
import org.junit.Test;
import static org.junit.Assert.*;
import com.google.gson.*;
import software.bernie.geckolib3.core.molang.MolangParser;
public class Batch5AnimationCompatibilityTest {
 private static JsonObject animations(String slug) throws Exception {
  var p=java.nio.file.Path.of(System.getProperty("minemon.testResources"),
   "assets/minemon_gen1/animations/"+slug+"_cobblemon_1_8_2/"+slug+".animation.json");
  try(var r=java.nio.file.Files.newBufferedReader(p)){return JsonParser.parseReader(r).getAsJsonObject().getAsJsonObject("animations");}
 }
 private static void checkExpressions(JsonElement value,MolangParser parser) throws Exception {
  if(value.isJsonArray()){for(var item:value.getAsJsonArray())checkExpressions(item,parser);}
  else if(value.isJsonObject()){for(var item:value.getAsJsonObject().entrySet())checkExpressions(item.getValue(),parser);}
  else if(value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString()) {
   String text=value.getAsString();assertFalse(text.contains("q."));assertFalse(text.contains("query.r."));
   var expression=parser.parseExpression(text);
   for(int i=0;i<=20;i++){parser.setValue("query.anim_time",i/20.0);assertTrue(text,Double.isFinite(expression.get()));}
  }
 }
 @Test public void everyBatch5BoneExpressionAndClipLoadsInGecko3() throws Exception {
  for(String slug:java.util.List.of("zubat","golbat","oddish","gloom","vileplume","paras","parasect","venonat","venomoth","diglett")) {
   var parser=new MolangParser();
   for(var entry:animations(slug).entrySet()){
    var clip=entry.getValue().getAsJsonObject();
    if(clip.has("bones"))checkExpressions(clip.get("bones"),parser);
    var loaded=software.bernie.geckolib3.util.json.JsonAnimationUtils.deserializeJsonToAnimation(
     new java.util.AbstractMap.SimpleImmutableEntry<String,JsonElement>(entry.getKey(),clip),parser);
    assertTrue(entry.getKey(),Double.isFinite(loaded.animationLength)&&loaded.animationLength>0);
   }
  }
 }
 @Test public void zubatPoserVisibilitySurvivesRuntimeConversion() throws Exception {
  for(var entry:animations("zubat").entrySet()){
   boolean shoulder=entry.getKey().endsWith(".shoulder_left")||entry.getKey().endsWith(".shoulder_right");
   var bones=entry.getValue().getAsJsonObject().getAsJsonObject("bones");
   assertEquals(shoulder?1.0:0.0,bones.getAsJsonObject("wings_folded").getAsJsonArray("scale").get(0).getAsDouble(),0);
   assertEquals(shoulder?0.0:1.0,bones.getAsJsonObject("wings_open").getAsJsonArray("scale").get(0).getAsDouble(),0);
  }
 }
 @Test public void golbatNativeWingGroupsKeepOriginalPivots() throws Exception {
  var p=java.nio.file.Path.of(System.getProperty("minemon.testResources"),
   "assets/minemon_gen1/geo/golbat_cobblemon_1_8_2/golbat.geo.json");
  JsonObject root;try(var r=java.nio.file.Files.newBufferedReader(p)){root=JsonParser.parseReader(r).getAsJsonObject();}
  var map=new java.util.HashMap<String,JsonObject>();
  for(var b:root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")){
   var bone=b.getAsJsonObject();map.put(bone.get("name").getAsString(),bone);
  }
  for(String side:java.util.List.of("left","right")){
   var wing=map.get("wing_"+side);var group=map.get("wing_"+side+"_rotation");
   assertNotNull(group);assertEquals("wings",group.get("parent").getAsString());
   assertEquals("wing_"+side+"_rotation",wing.get("parent").getAsString());
   assertEquals(wing.get("pivot"),group.get("pivot"));
   assertTrue(animations("golbat").getAsJsonObject("animation.golbat.air_idle").getAsJsonObject("bones").has("wing_"+side+"_rotation"));
  }
 }
}
