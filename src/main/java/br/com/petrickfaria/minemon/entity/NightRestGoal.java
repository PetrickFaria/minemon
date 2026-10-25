package br.com.petrickfaria.minemon.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.util.Mth;

/** Holds terrestrial sleep or a collision-respecting hover one block above support. */
final class NightRestGoal extends Goal {
 private final MinemonCreature mob;
 NightRestGoal(MinemonCreature mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));}
 @Override public boolean canUse(){return (mob.isSleeping()||mob.isNightResting())
     &&(SpeciesBehaviorProfiles.isAirborne(mob.getSpeciesSlug())||mob.isOnGround());}
 @Override public boolean canContinueToUse(){return canUse()&&mob.getTarget()==null;}
 @Override public void start(){mob.getNavigation().stop();}
 @Override public void tick(){
  mob.getNavigation().stop();mob.setJumping(false);mob.setSpeed(0);mob.setXxa(0);mob.setYya(0);mob.setZza(0);
  boolean hover=SpeciesBehaviorProfiles.isAirborne(mob.getSpeciesSlug());
  double vertical=hover?Mth.clamp((mob.naturalSurface()+1.0D-mob.getY())*0.12D,-0.10D,0.10D):Math.min(0,mob.getDeltaMovement().y);
  mob.setDeltaMovement(0,vertical,0);
  if(hover){mob.setNoGravity(true);mob.fallDistance=0;}
 }
}
