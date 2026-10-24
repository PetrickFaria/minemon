package br.com.petrickfaria.minemon.entity;

import br.com.petrickfaria.minemon.entity.SpeciesBehaviorProfiles.Profile;

/** Tick-exact state machine; observation, navigation and persistence stay in the entity. */
public final class BehaviorCycle {
 public enum Phase { LAND, TAKEOFF, AIR, LANDING, SEEK_WATER, SUBMERGED, SURFACE, EXIT_WATER, COOLDOWN }
 public record Observation(boolean active,boolean ground,boolean flightBand,boolean wet,
                           boolean submerged,boolean surface,boolean dryGround,boolean canDive) {}
 private final Profile profile;
 private Phase phase;
 private int ticks,stage;
 public BehaviorCycle(Profile p){profile=p;phase=p==Profile.WATER_VISITOR?Phase.COOLDOWN:Phase.LAND;}
 public Phase phase(){return phase;}
 public int ticks(){return ticks;}
 public int stage(){return stage;}
 public double minimumAltitude(){return profile==Profile.ALTITUDE_CYCLE||profile==Profile.GROUND_FLIGHT?new double[]{3,8}[Math.min(stage,1)]:3;}
 public double maximumAltitude(){return profile==Profile.ALTITUDE_CYCLE||profile==Profile.GROUND_FLIGHT?new double[]{7,15}[Math.min(stage,1)]:15;}
 public boolean flying(){return profile==Profile.ALTITUDE_CYCLE||profile==Profile.GROUND_FLIGHT&&phase!=Phase.LAND;}
 public boolean landing(){return profile==Profile.GROUND_FLIGHT&&phase==Phase.LANDING;}
 private void enter(Phase next){phase=next;ticks=0;}
 public void tick(Observation o){
  if(!o.active())return;
  if(profile==Profile.ALTITUDE_CYCLE){
   if(o.flightBand()&&++ticks>=(stage==0?6000:3000)){stage=1-stage;ticks=0;}
  }else if(profile==Profile.GROUND_FLIGHT){
   switch(phase){
    case LAND -> {if(o.ground()&&++ticks>=6000)enter(Phase.TAKEOFF);}
    case TAKEOFF -> {if(o.flightBand())enter(Phase.AIR);}
    case AIR -> {
     if(o.flightBand()&&++ticks>=(stage==0||profile==Profile.GROUND_FLIGHT?6000:3000)){
      if(stage==0){stage=1;ticks=0;}else enter(Phase.LANDING);
     }
    }
    case LANDING -> {if(o.dryGround()){stage=0;enter(Phase.LAND);}}
    default -> enter(Phase.LAND);
   }
  }else if(profile==Profile.WATER_VISITOR){
   switch(phase){
    case SEEK_WATER -> {if(o.wet())enter(o.canDive()?Phase.SUBMERGED:Phase.SURFACE);}
    case SUBMERGED, SURFACE -> {if(o.wet()&&++ticks>=12000)enter(Phase.EXIT_WATER);}
    case EXIT_WATER -> {if(o.dryGround())enter(Phase.COOLDOWN);}
    case COOLDOWN -> {if(o.wet())enter(Phase.EXIT_WATER);else if(o.dryGround()&&++ticks>=12000)enter(Phase.SEEK_WATER);}
    default -> enter(Phase.SEEK_WATER);
   }
  }
 }
 public void restore(String state,int elapsed,int altitudeStage){
  try{phase=Phase.valueOf(state);}catch(IllegalArgumentException e){phase=profile==Profile.WATER_VISITOR?Phase.COOLDOWN:Phase.LAND;}
  stage=Math.max(0,Math.min(profile==Profile.ALTITUDE_CYCLE||profile==Profile.GROUND_FLIGHT?1:0,altitudeStage));
  int limit=profile==Profile.ALTITUDE_CYCLE?(stage==0?6000:3000)
      :phase==Phase.SUBMERGED||phase==Phase.SURFACE?12000:phase==Phase.AIR?(stage==0||profile==Profile.GROUND_FLIGHT?6000:3000):12000;
  ticks=Math.max(0,Math.min(limit-1,elapsed));
  if(profile==Profile.GROUND_FLIGHT&&phase!=Phase.LAND&&phase!=Phase.TAKEOFF&&phase!=Phase.AIR&&phase!=Phase.LANDING)enter(Phase.LAND);
  if(profile==Profile.WATER_VISITOR&&phase!=Phase.SEEK_WATER&&phase!=Phase.SUBMERGED&&phase!=Phase.SURFACE&&phase!=Phase.EXIT_WATER&&phase!=Phase.COOLDOWN)enter(Phase.COOLDOWN);
 }
}
