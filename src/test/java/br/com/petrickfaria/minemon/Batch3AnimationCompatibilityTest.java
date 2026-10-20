package br.com.petrickfaria.minemon;
import org.junit.Test;
import static org.junit.Assert.*;
import software.bernie.geckolib3.core.molang.MolangParser;
public class Batch3AnimationCompatibilityTest {
 @Test public void proceduralBirdWalkPreservesStanceAndMovesOpposedLegs() throws Exception {
  for(String slug:java.util.List.of("spearow","fearow")) {
   var path=java.nio.file.Path.of(System.getProperty("minemon.testResources"),
    "assets/minemon_gen1/animations/"+slug+"_cobblemon_1_8_2/"+slug+".animation.json");
   var animations=com.google.gson.JsonParser.parseReader(java.nio.file.Files.newBufferedReader(path)).getAsJsonObject().getAsJsonObject("animations");
   var idle=animations.getAsJsonObject("animation."+slug+".ground_idle");
   var walk=animations.getAsJsonObject("animation."+slug+".minemon_poser_walk");
   assertNotNull(slug+" retains native ground stance",idle);
   assertNotNull(slug+" has a compatible source-poser walking adapter",walk);
   assertTrue(walk.get("loop").getAsBoolean());
   var parser=new MolangParser();var rotations=new java.util.ArrayList<com.google.gson.JsonArray>();
   for(String side:java.util.List.of("left","right")) {
    var base=idle.getAsJsonObject("bones").getAsJsonObject("leg_"+side).getAsJsonArray("rotation");
    var leg=walk.getAsJsonObject("bones").getAsJsonObject("leg_"+side).getAsJsonArray("rotation");
    assertEquals("Native leg yaw is preserved",base.get(1),leg.get(1));
    assertEquals("Native leg roll is preserved",base.get(2),leg.get(2));
    assertFalse("GeckoLib must receive the bound clock",leg.get(0).getAsString().contains("q.anim_time"));
    rotations.add(leg);
   }
   var left=parser.parseExpression(rotations.get(0).get(0).getAsString());
   var right=parser.parseExpression(rotations.get(1).get(0).getAsString());
   double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
   for(int i=0;i<=40;i++) {
    parser.setValue("query.anim_time",i/40.0);
    double l=left.get(),r=right.get();
    assertTrue("Leg evaluations must be finite",Double.isFinite(l)&&Double.isFinite(r));
    assertEquals("Legs must alternate instead of moving together",0.0,l+r,0.0001);
    min=Math.min(min,l);max=Math.max(max,l);
   }
   assertTrue("Walking adapter must produce a visible gait",max-min>40.0);
   var loaded=software.bernie.geckolib3.util.json.JsonAnimationUtils.deserializeJsonToAnimation(
    new java.util.AbstractMap.SimpleImmutableEntry<String,com.google.gson.JsonElement>("animation."+slug+".minemon_poser_walk",walk),parser);
   assertTrue("GeckoLib must load a finite loop duration",loaded.animationLength>0);
  }
 }
}
