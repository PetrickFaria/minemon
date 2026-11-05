package br.com.petrickfaria.minemon;
import org.junit.Test;
import static org.junit.Assert.*;
import br.com.petrickfaria.minemon.entity.GastlyAura;
public class GastlyAuraTest {
 @Test public void everySteadyPopulationWindowHasFiveLargeAndSevenSmall(){
  for(int end=16;end<200;end++){
   int large=0,small=0;
   for(int t=end-15;t<=end;t++){if(GastlyAura.largeSlot(t)>=0)large++;if(GastlyAura.smallSlot(t)>=0)small++;}
   assertEquals(5,large);assertEquals(7,small);
  }
 }
 @Test public void originalAuraDensityAndTransparencyArePreserved(){
  assertEquals(.55F,GastlyAura.CORE_RADIUS_SCALE,0);
  assertEquals(2.5F,GastlyAura.LARGE_SIZE/GastlyAura.SMALL_SIZE,.0001F);assertEquals(.75F,GastlyAura.OPACITY,0);
 }
 @Test public void billboardExtentKeepsAuraQuadOutsideGastlyCore(){
  double width=1.2,quad=.85;
  double radius=GastlyAura.minimumVisibleParticleRadius(width,quad);
  assertTrue(radius>=width*.5+quad*.72);
  assertTrue(radius>GastlyAura.CORE_RADIUS_SCALE*width);
  assertEquals(GastlyAura.minimumVisibleParticleRadius(width,0),Math.max(width*.5,GastlyAura.CORE_RADIUS_SCALE*width),0);
 }
 @Test public void everyParticleOrbitIsExactlyFifteenPercentCloser(){
  for(double width:new double[]{.2,.6,1.2,2.4})for(double quad:new double[]{.0,.34,.85})
   assertEquals(GastlyAura.minimumVisibleParticleRadius(width,quad)*.85,
       GastlyAura.particleOrbitRadius(width,quad),1e-12);
  assertEquals(.85,GastlyAura.RADIAL_DISTRIBUTION_SCALE,0);
 }
}
