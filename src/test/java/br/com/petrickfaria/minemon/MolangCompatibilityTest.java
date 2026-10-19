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

}
