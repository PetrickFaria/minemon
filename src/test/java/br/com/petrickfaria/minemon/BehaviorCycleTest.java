package br.com.petrickfaria.minemon;
import br.com.petrickfaria.minemon.entity.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static br.com.petrickfaria.minemon.entity.SpeciesBehaviorProfiles.Profile.ALTITUDE_CYCLE;
import static br.com.petrickfaria.minemon.entity.SpeciesBehaviorProfiles.Profile.GROUND_FLIGHT;
import static br.com.petrickfaria.minemon.entity.SpeciesBehaviorProfiles.Profile.WATER_VISITOR;
import static br.com.petrickfaria.minemon.entity.SpeciesBehaviorProfiles.Profile.GROUND;
import static br.com.petrickfaria.minemon.entity.BehaviorCycle.Phase.*;
public class BehaviorCycleTest {
 private BehaviorCycle.Observation observe(boolean active,boolean walking,boolean band,boolean wet,boolean submerged,boolean surface,boolean dry,boolean dive){
  return new BehaviorCycle.Observation(active,walking,band,wet,submerged,surface,dry,dive);
 }
 private void repeat(BehaviorCycle c,int n,BehaviorCycle.Observation o){for(int i=0;i<n;i++)c.tick(o);}
 @Test public void exact151SpeciesAndSixDisjointProfiles(){
  assertEquals(SpeciesDefinitions.ALL.keySet(),SpeciesBehaviorProfiles.all().keySet());
  int[] counts={91,23,2,24,9,2};int i=0;
  for(var p:SpeciesBehaviorProfiles.Profile.values())assertEquals(p.name(),counts[i++],SpeciesBehaviorProfiles.all().values().stream().filter(v->v==p).count());
  assertEquals(WATER_VISITOR,SpeciesBehaviorProfiles.profile("dragonite"));
  assertEquals(GROUND,SpeciesBehaviorProfiles.profile("farfetchd"));
  assertEquals(ALTITUDE_CYCLE,SpeciesBehaviorProfiles.profile("spearow"));
 }
 @Test public void altitudeCountsOnlyActualTimeInsideEachBand(){
  var c=new BehaviorCycle(ALTITUDE_CYCLE);var flight=observe(true,false,true,false,false,false,false,false);
  repeat(c,6000,observe(true,false,false,false,false,false,false,false));assertEquals(0,c.ticks());
  repeat(c,5999,flight);assertEquals(0,c.stage());c.tick(flight);assertEquals(1,c.stage());assertEquals(6,c.minimumAltitude(),0);
  repeat(c,1800,flight);assertEquals(2,c.stage());assertEquals(10,c.maximumAltitude(),0);
  repeat(c,1200,flight);assertEquals(0,c.stage());assertEquals(0,c.ticks());
 }
 @Test public void fiveMinuteWalkThenRealTakeoffOneMinuteFlightAndRealLanding(){
  var c=new BehaviorCycle(GROUND_FLIGHT);var walk=observe(true,true,false,false,false,false,true,false);
  repeat(c,5999,walk);assertEquals(LAND,c.phase());c.tick(walk);assertEquals(TAKEOFF,c.phase());
  repeat(c,200,observe(true,false,false,false,false,false,false,false));assertEquals(0,c.ticks());
  var fly=observe(true,false,true,false,false,false,false,false);c.tick(fly);assertEquals(AIR,c.phase());
  repeat(c,1199,fly);assertEquals(AIR,c.phase());c.tick(fly);assertEquals(LANDING,c.phase());
  repeat(c,200,fly);assertEquals(LANDING,c.phase());c.tick(walk);assertEquals(LAND,c.phase());
 }
 @Test public void waterVisitRequires600SubmergedAnd600SurfaceThen6000TicksActuallyOnLand(){
  var c=new BehaviorCycle(WATER_VISITOR);var dive=observe(true,false,false,true,true,false,false,true);
  c.tick(dive);assertEquals(SUBMERGED,c.phase());repeat(c,599,dive);assertEquals(SUBMERGED,c.phase());c.tick(dive);assertEquals(SURFACE,c.phase());
  repeat(c,3000,dive);assertEquals(0,c.ticks());
  var surface=observe(true,false,false,true,false,true,false,true);repeat(c,600,surface);assertEquals(EXIT_WATER,c.phase());
  repeat(c,6000,surface);assertEquals(EXIT_WATER,c.phase());
  var dry=observe(true,true,false,false,false,false,true,false);c.tick(dry);assertEquals(COOLDOWN,c.phase());
  repeat(c,5999,dry);assertEquals(COOLDOWN,c.phase());c.tick(dry);assertEquals(SEEK_WATER,c.phase());
 }
 @Test public void priorityInterruptionsFreezeEveryClockAndInstancesAreIndependent(){
  var a=new BehaviorCycle(ALTITUDE_CYCLE);var b=new BehaviorCycle(ALTITUDE_CYCLE);
  var flight=observe(true,false,true,false,false,false,false,false);repeat(a,100,flight);
  repeat(a,9000,observe(false,true,true,true,true,true,true,true));assertEquals(100,a.ticks());assertEquals(0,b.ticks());
  var restored=new BehaviorCycle(ALTITUDE_CYCLE);restored.restore(a.phase().name(),a.ticks(),a.stage());assertEquals(100,restored.ticks());
 }
 @Test public void shallowWaterIsExitedRatherThanForcedSubmersion(){
  var c=new BehaviorCycle(WATER_VISITOR);c.tick(observe(true,false,false,true,false,false,false,false));assertEquals(EXIT_WATER,c.phase());
 }
 @Test public void onlySeakingReceivesExactly25PercentReduction(){
  var s=SpeciesDefinitions.get("seaking");assertEquals(1.35,s.scale(),0.00001);assertEquals(.525,s.width(),0.00001);assertEquals(.45,s.height(),0.00001);
  assertEquals(.6,SpeciesDefinitions.get("goldeen").width(),0.00001);
 }
 @Test public void absentGaitsAreExplicitAdditionsWhileOriginalSourceClipsRemain(){
  for(String s:java.util.List.of("eevee","jolteon","flareon","vaporeon")){
   var d=SpeciesDefinitions.get(s);assertNull(d.movementFallback());
   assertEquals(CompatibleLocomotion.clip(s,"walk"),d.walk());assertTrue(d.clips().containsKey("ground_idle"));
   assertEquals(CompatibleLocomotion.clip(s,"run"),d.clips().get("ground_run"));
  }
  assertEquals(CompatibleLocomotion.clip("vaporeon","swim"),SpeciesDefinitions.get("vaporeon").clips().get("water_swim"));
 }
}
