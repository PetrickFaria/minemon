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
 public BehaviorCycle(Profile p){profile=p;phase=p==Profile.WATER_VISITOR?Phase.SEEK_WATER:Phase.LAND;}
 public Phase phase(){return phase;}
 public int ticks(){return ticks;}
 public int stage(){return stage;}
 public double minimumAltitude(){return profile==Profile.ALTITUDE_CYCLE?new double[]{3,6,9}[stage]:3;}
 public double maximumAltitude(){return profile==Profile.ALTITUDE_CYCLE?new double[]{5,8,10}[stage]:10;}
 public boolean flying(){return profile==Profile.ALTITUDE_CYCLE||profile==Profile.GROUND_FLIGHT&&phase!=Phase.LAND;}
 public boolean landing(){return profile==Profile.GROUND_FLIGHT&&phase==Phase.LANDING;}
 private void enter(Phase next){phase=next;ticks=0;}
 public void tick(Observation o){
  if(!o.active())return;
  if(profile==Profile.ALTITUDE_CYCLE){
   if(o.flightBand()&&++ticks>=new int[]{6000,1800,1200}[stage]){stage=(stage+1)%3;ticks=0;}
  }else if(profile==Profile.GROUND_FLIGHT){
   switch(phase){
    case LAND -> {if(o.ground()&&++ticks>=6000)enter(Phase.TAKEOFF);}
    case TAKEOFF -> {if(o.flightBand())enter(Phase.AIR);}
    case AIR -> {if(o.flightBand()&&++ticks>=1200)enter(Phase.LANDING);}
    case LANDING -> {if(o.dryGround())enter(Phase.LAND);}
    default -> enter(Phase.LAND);
   }
  }else if(profile==Profile.WATER_VISITOR){
   switch(phase){
    case SEEK_WATER -> {if(o.wet())enter(o.canDive()?Phase.SUBMERGED:Phase.EXIT_WATER);}
    case SUBMERGED -> {if(o.submerged()&&++ticks>=600)enter(Phase.SURFACE);}
    case SURFACE -> {if(o.surface()&&++ticks>=600)enter(Phase.EXIT_WATER);}
    case EXIT_WATER -> {if(o.dryGround())enter(Phase.COOLDOWN);}
    case COOLDOWN -> {if(o.wet())enter(Phase.EXIT_WATER);else if(o.dryGround()&&++ticks>=6000)enter(Phase.SEEK_WATER);}
    default -> enter(Phase.SEEK_WATER);
   }
  }
 }
 public void restore(String state,int elapsed,int altitudeStage){
  try{phase=Phase.valueOf(state);}catch(IllegalArgumentException e){phase=profile==Profile.WATER_VISITOR?Phase.SEEK_WATER:Phase.LAND;}
  stage=Math.max(0,Math.min(2,altitudeStage));
  int limit=profile==Profile.ALTITUDE_CYCLE?new int[]{6000,1800,1200}[stage]
      :phase==Phase.SUBMERGED||phase==Phase.SURFACE?600:phase==Phase.AIR?1200:6000;
  ticks=Math.max(0,Math.min(limit-1,elapsed));
  if(profile==Profile.GROUND_FLIGHT&&phase!=Phase.LAND&&phase!=Phase.TAKEOFF&&phase!=Phase.AIR&&phase!=Phase.LANDING)enter(Phase.LAND);
  if(profile==Profile.WATER_VISITOR&&phase!=Phase.SEEK_WATER&&phase!=Phase.SUBMERGED&&phase!=Phase.SURFACE&&phase!=Phase.EXIT_WATER&&phase!=Phase.COOLDOWN)enter(Phase.SEEK_WATER);
 }
}
