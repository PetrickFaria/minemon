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
}
