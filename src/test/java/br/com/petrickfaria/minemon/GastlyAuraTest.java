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
 @Test public void originalAndEnlargedSizesAndTransparencyArePreserved(){
  assertEquals(2.5F,GastlyAura.LARGE_SIZE/GastlyAura.SMALL_SIZE,.0001F);assertEquals(.75F,GastlyAura.OPACITY,0);
 }
 @Test public void cloudSlotsSurroundTheHeadWithUnevenDepthHeightAndScale(){
  var xs=new java.util.HashSet<Float>();var ys=new java.util.HashSet<Float>();var zs=new java.util.HashSet<Float>();
  var sizes=new java.util.HashSet<Float>();
  for(int slot=0;slot<12;slot++){
   xs.add(GastlyAura.cloudX(slot));ys.add(GastlyAura.cloudY(slot));zs.add(GastlyAura.cloudZ(slot));sizes.add(GastlyAura.sizeVariation(slot));
   assertTrue(GastlyAura.cloudY(slot)>-.5F&&GastlyAura.cloudY(slot)<1.0F);
   assertTrue(GastlyAura.sizeVariation(slot)>=.82F&&GastlyAura.sizeVariation(slot)<=1.18F);
  }
  assertTrue(xs.size()>8);assertTrue(ys.size()>8);assertTrue(zs.size()>8);assertTrue(sizes.size()>6);
 }
}
