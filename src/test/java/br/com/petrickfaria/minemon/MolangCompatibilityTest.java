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
}
