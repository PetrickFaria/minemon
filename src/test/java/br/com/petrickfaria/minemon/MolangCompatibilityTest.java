package br.com.petrickfaria.minemon;
import org.junit.Test;
import static org.junit.Assert.*;
import software.bernie.geckolib3.core.molang.MolangParser;
public class MolangCompatibilityTest {
 @Test public void nativeQueryAnimatesButBedrockAliasIsNotBound() throws Exception {
  MolangParser parser = new MolangParser();
  var alias = parser.parseExpression("math.sin(q.anim_time*360)");
  var nativeExpr = parser.parseExpression("math.sin(query.anim_time*360)");
  parser.setValue("query.anim_time", 0.0);
  double start = nativeExpr.get();
  parser.setValue("query.anim_time", 0.25);
  assertEquals("Unbound q alias stays zero",0.0,alias.get(),0.0001);
  assertTrue("Canonical animation clock must move the bone", Math.abs(nativeExpr.get()-start)>0.5);
 }
 @Test public void preparedBulbasaurBodyAndLegChannelsAdvance() throws Exception {
  var stream=java.nio.file.Files.newInputStream(java.nio.file.Path.of(System.getProperty("minemon.testResources"),"assets/minemon_gen1/animations/bulbasaur/bulbasaur.animation.json"));
  assertNotNull(stream);
  var root=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
  var walk=root.getAsJsonObject("animations").getAsJsonObject("animation.bulbasaur.ground_walk");
  assertEquals("GeckoLib 3.1.40 requires a finite duration in every clip",4.0,walk.get("animation_length").getAsDouble(),0.0);
  var loaded=software.bernie.geckolib3.util.json.JsonAnimationUtils.deserializeJsonToAnimation(new java.util.AbstractMap.SimpleImmutableEntry<String,com.google.gson.JsonElement>("animation.bulbasaur.ground_walk",walk),new MolangParser());
  assertEquals("GeckoLib stores the four-second runtime fallback as ticks",80.0,loaded.animationLength,0.0);
  var bones=walk.getAsJsonObject("bones");
  String leg=bones.getAsJsonObject("leg_front_right").getAsJsonArray("rotation").get(0).getAsString();
  assertFalse(leg.contains("q.anim_time"));
  MolangParser parser=new MolangParser();var expression=parser.parseExpression(leg);
  parser.setValue("query.anim_time",0.0);double first=expression.get();
  parser.setValue("query.anim_time",0.125);assertTrue("Leg channel must visibly change",Math.abs(expression.get()-first)>10.0);
  var body=parser.parseExpression(bones.getAsJsonObject("body").getAsJsonArray("position").get(1).getAsString());
  parser.setValue("query.anim_time",0.0);first=body.get();
  parser.setValue("query.anim_time",0.125);assertTrue("Body channel must change",Math.abs(body.get()-first)>0.1);
 }

 @Test public void cobblemon182CharmanderIdleAndWalkLoadAndAnimate() throws Exception {
  var definition=br.com.petrickfaria.minemon.entity.SpeciesDefinitions.get("charmander");
  assertTrue("Pilot must load the Cobblemon 1.8.2 model path",definition.model().contains("charmander_cobblemon_1_8_2"));
  assertTrue("Pilot must load the Cobblemon 1.8.2 texture path",definition.texture().contains("charmander_cobblemon_1_8_2"));
  var path=java.nio.file.Path.of(System.getProperty("minemon.testResources"),"assets/minemon_gen1/animations/charmander_cobblemon_1_8_2/charmander.animation.json");
  var animations=com.google.gson.JsonParser.parseReader(java.nio.file.Files.newBufferedReader(path)).getAsJsonObject().getAsJsonObject("animations");
  var idle=animations.getAsJsonObject("animation.charmander.ground_idle");
  var walk=animations.getAsJsonObject("animation.charmander.ground_walk");
  assertNotNull("Cobblemon idle clip is present",idle);
  assertNotNull("Cobblemon walk clip is present",walk);
  assertTrue("Idle loops",idle.get("loop").getAsBoolean());
  assertTrue("Walk loops",walk.get("loop").getAsBoolean());
  var parser=new MolangParser();
  var loadedIdle=software.bernie.geckolib3.util.json.JsonAnimationUtils.deserializeJsonToAnimation(
    new java.util.AbstractMap.SimpleImmutableEntry<String,com.google.gson.JsonElement>("animation.charmander.ground_idle",idle),parser);
  var loadedWalk=software.bernie.geckolib3.util.json.JsonAnimationUtils.deserializeJsonToAnimation(
    new java.util.AbstractMap.SimpleImmutableEntry<String,com.google.gson.JsonElement>("animation.charmander.ground_walk",walk),parser);
  assertEquals("Idle duration uses Cobblemon's four second cycle",80.0,loadedIdle.animationLength,0.0);
  assertEquals("Walk duration uses Cobblemon's one second cycle",20.0,loadedWalk.animationLength,0.0);
  var torso=idle.getAsJsonObject("bones").getAsJsonObject("torso").getAsJsonArray("rotation").get(0).getAsString();
  assertFalse("Runtime conversion binds Bedrock q aliases",torso.contains("q.anim_time"));
  var breath=parser.parseExpression(torso);
  parser.setValue("query.anim_time",0.0);double first=breath.get();
  parser.setValue("query.anim_time",0.25);
  assertTrue("Idle torso channel changes over time",Math.abs(breath.get()-first)>0.1);
  assertTrue("Walk clip actuates leg joints",walk.getAsJsonObject("bones").has("leg_left")&&walk.getAsJsonObject("bones").has("leg_right"));
 }

}
