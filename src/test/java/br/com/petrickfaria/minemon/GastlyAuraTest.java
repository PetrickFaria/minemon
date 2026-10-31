package br.com.petrickfaria.minemon;
import org.junit.Test;
import static org.junit.Assert.*;
import br.com.petrickfaria.minemon.entity.GastlyAura;
public class GastlyAuraTest {
 @Test public void restoredEmitterCreatesOneParticleEveryTwoTicks(){
  for(int tick=0;tick<200;tick++)assertEquals(tick%2==0,GastlyAura.emitsAt(tick));
  int emissions=0;for(int tick=0;tick<20;tick++)if(GastlyAura.emitsAt(tick))emissions++;
  assertEquals(10,emissions);
 }
}
