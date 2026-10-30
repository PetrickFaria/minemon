package br.com.petrickfaria.minemon;
import org.junit.Test;
import static org.junit.Assert.*;
import br.com.petrickfaria.minemon.entity.*;
import net.minecraft.nbt.CompoundTag;
public class GastlyCyclePersistenceTest {
 @Test public void reloadKeepsLowPhaseBeyondOldThreeMinuteClamp(){
  var c=new BehaviorCycle(SpeciesBehaviorProfiles.profile("gastly"),"gastly");
  var active=new BehaviorCycle.Observation(true,false,false,false,false,false,false,false);
  for(int i=0;i<5500;i++)c.tick(active);
  var tag=new CompoundTag();c.save(tag);
  var restored=new BehaviorCycle(SpeciesBehaviorProfiles.profile("gastly"),"gastly");restored.restore(tag);
  assertEquals(5500,restored.ticks());assertEquals(0,restored.stage());
  for(int i=0;i<500;i++)restored.tick(active);
  assertEquals(1,restored.stage());assertEquals(2,restored.minimumAltitude(),0);assertEquals(5,restored.maximumAltitude(),0);
  var paused=new BehaviorCycle.Observation(false,false,false,false,false,false,false,false);
  for(int i=0;i<200;i++)restored.tick(paused);
  assertEquals(0,restored.ticks());
  for(int i=0;i<3600;i++)restored.tick(active);
  assertEquals(0,restored.stage());assertEquals(1,restored.minimumAltitude(),0);assertEquals(1,restored.maximumAltitude(),0);
 }
}
