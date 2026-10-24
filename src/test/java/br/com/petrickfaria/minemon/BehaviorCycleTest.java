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
  int[] counts={91,23,3,23,9,2};int i=0;
  for(var p:SpeciesBehaviorProfiles.Profile.values())assertEquals(p.name(),counts[i++],SpeciesBehaviorProfiles.all().values().stream().filter(v->v==p).count());
  assertEquals(GROUND_FLIGHT,SpeciesBehaviorProfiles.profile("dragonite"));
  assertEquals(GROUND,SpeciesBehaviorProfiles.profile("farfetchd"));
  assertEquals(ALTITUDE_CYCLE,SpeciesBehaviorProfiles.profile("spearow"));
 }
 @Test public void altitudeCountsOnlyActualTimeInsideEachBand(){
  var c=new BehaviorCycle(ALTITUDE_CYCLE);var flight=observe(true,false,true,false,false,false,false,false);
  repeat(c,6000,observe(true,false,false,false,false,false,false,false));assertEquals(0,c.ticks());
  repeat(c,5999,flight);assertEquals(0,c.stage());c.tick(flight);assertEquals(1,c.stage());assertEquals(8,c.minimumAltitude(),0);
  assertEquals(15,c.maximumAltitude(),0);repeat(c,3000,flight);assertEquals(0,c.stage());assertEquals(0,c.ticks());
 }
 @Test public void twentyMinuteGroundLowFlightHighFlightLandingCycle(){
  var c=new BehaviorCycle(GROUND_FLIGHT);var walk=observe(true,true,false,false,false,false,true,false);
  repeat(c,5999,walk);assertEquals(LAND,c.phase());c.tick(walk);assertEquals(TAKEOFF,c.phase());
  repeat(c,200,observe(true,false,false,false,false,false,false,false));assertEquals(0,c.ticks());
  var fly=observe(true,false,true,false,false,false,false,false);c.tick(fly);assertEquals(AIR,c.phase());
  repeat(c,5999,fly);assertEquals(AIR,c.phase());c.tick(fly);assertEquals(1,c.stage());assertEquals(8,c.minimumAltitude(),0);
  repeat(c,5999,fly);assertEquals(AIR,c.phase());c.tick(fly);assertEquals(LANDING,c.phase());
  repeat(c,200,fly);assertEquals(LANDING,c.phase());c.tick(walk);assertEquals(LAND,c.phase());
 }
 @Test public void ordinaryGroundIdleCountsButPriorityPausesDoNot(){
  var c=new BehaviorCycle(GROUND_FLIGHT);
  var ground=observe(true,true,false,false,false,false,true,false);
  repeat(c,100,ground);assertEquals(100,c.ticks());
  repeat(c,500,observe(false,true,false,false,false,false,true,false));assertEquals(100,c.ticks());
 }
 @Test public void waterVisitorsSpendTenMinutesOnLandAndTenInWater(){
  var c=new BehaviorCycle(WATER_VISITOR);var dry=observe(true,true,false,false,false,false,true,false);
  assertEquals(COOLDOWN,c.phase());repeat(c,11999,dry);assertEquals(COOLDOWN,c.phase());c.tick(dry);assertEquals(SEEK_WATER,c.phase());
  var dive=observe(true,false,false,true,true,false,false,true);
  c.tick(dive);assertEquals(SUBMERGED,c.phase());repeat(c,11999,dive);assertEquals(SUBMERGED,c.phase());c.tick(dive);assertEquals(EXIT_WATER,c.phase());
  var surface=observe(true,false,false,true,false,true,false,true);repeat(c,1000,surface);assertEquals(EXIT_WATER,c.phase());
  c.tick(dry);assertEquals(COOLDOWN,c.phase());
 }
 @Test public void priorityInterruptionsFreezeEveryClockAndInstancesAreIndependent(){
  var a=new BehaviorCycle(ALTITUDE_CYCLE);var b=new BehaviorCycle(ALTITUDE_CYCLE);
  var flight=observe(true,false,true,false,false,false,false,false);repeat(a,100,flight);
  repeat(a,9000,observe(false,true,true,true,true,true,true,true));assertEquals(100,a.ticks());assertEquals(0,b.ticks());
  var restored=new BehaviorCycle(ALTITUDE_CYCLE);restored.restore(a.phase().name(),a.ticks(),a.stage());assertEquals(100,restored.ticks());
 }
 @Test public void shallowWaterUsesSurfaceSwimmingInsteadOfForcedSubmersion(){
  var c=new BehaviorCycle(WATER_VISITOR);var dry=observe(true,true,false,false,false,false,true,false);repeat(c,12000,dry);
  c.tick(observe(true,false,false,true,false,false,false,false));assertEquals(SURFACE,c.phase());
 }
 @Test public void onlySeakingReceivesExactly25PercentReduction(){
  var s=SpeciesDefinitions.get("seaking");assertEquals(1.35,s.scale(),0.00001);assertEquals(.525,s.width(),0.00001);assertEquals(.45,s.height(),0.00001);
  assertEquals(.6,SpeciesDefinitions.get("goldeen").width(),0.00001);
 }
 @Test public void groundProfilesSelectExistingGroundClipsIncludingFarfetchd(){
  for(var d:SpeciesDefinitions.ALL.values()){
   var p=SpeciesBehaviorProfiles.profile(d.slug());
   if(p!=GROUND&&p!=GROUND_FLIGHT&&p!=WATER_VISITOR)continue;
   if(d.clips().containsKey("ground_idle"))assertEquals(d.slug()+" ground idle",d.clips().get("ground_idle"),d.idle());
   if(d.clips().containsKey("ground_walk"))assertEquals(d.slug()+" ground walk",d.clips().get("ground_walk"),d.walk());
  }
  assertEquals("animation.farfetchd.ground_walk",SpeciesDefinitions.get("farfetchd").walk());
 }
 @Test public void absentGaitsAreExplicitAdditionsWhileOriginalSourceClipsRemain(){
  for(String s:java.util.List.of("eevee","jolteon","flareon","vaporeon")){
   var d=SpeciesDefinitions.get(s);assertNull(d.movementFallback());
   assertEquals(CompatibleLocomotion.clip(s,"walk"),d.walk());assertTrue(d.clips().containsKey("ground_idle"));
   assertEquals(CompatibleLocomotion.clip(s,"run"),d.clips().get("ground_run"));
  }
  assertEquals(CompatibleLocomotion.clip("vaporeon","swim"),SpeciesDefinitions.get("vaporeon").clips().get("water_swim"));
  for(String s:java.util.List.of("meowth","persian")){
   var d=SpeciesDefinitions.get(s);assertNull(d.movementFallback());
   assertEquals(CompatibleLocomotion.clip(s,"walk"),d.clips().get("ground_walk"));
   assertEquals(CompatibleLocomotion.clip(s,"run"),d.clips().get("ground_run"));
  }
  for(String s:java.util.List.of("articuno","zapdos","moltres"))
   assertEquals("native air fly remains selected for "+s,"animation."+s+".air_fly",SpeciesDefinitions.get(s).clips().get("air_fly"));
  assertFalse("Fearow has no source air_fly and receives no invented mapping",SpeciesDefinitions.get("fearow").clips().containsKey("air_fly"));
  assertFalse("Spearow has no source air_fly and receives no invented mapping",SpeciesDefinitions.get("spearow").clips().containsKey("air_fly"));
 }
 @Test public void requestedScaleAndHitboxAdjustmentsAreAppliedOnlyAtRuntime(){
  assertEquals(1.0,SpeciesDefinitions.get("dewgong").scale(),.0001);
  assertEquals(.965,SpeciesDefinitions.get("jynx").scale(),.0001);
  assertEquals(1.47,SpeciesDefinitions.get("magmar").scale(),.0001);
  assertEquals(1.46,SpeciesDefinitions.get("victreebel").scale(),.0001);
  assertEquals(1.05,SpeciesDefinitions.get("jynx").width(),.0001);
  assertEquals(2.25,SpeciesDefinitions.get("magmar").height(),.0001);
  assertEquals(1.70,SpeciesDefinitions.get("victreebel").height(),.0001);
  assertEquals(1.05,SpeciesDefinitions.get("kangaskhan").scale(),.0001);
  assertEquals(1.50,SpeciesDefinitions.get("snorlax").scale(),.0001);
 }
}
