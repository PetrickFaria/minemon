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
  if(!SpeciesBehaviorProfiles.isAirborne(mob.getSpeciesSlug())&&!mob.isSleeping()
      &&!WaterMovementGoal.safeDry(mob,mob.blockPosition())){
   if(!mob.getNavigation().isDone())return;
   for(int radius=1;radius<=8;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
    if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
    var feet=mob.blockPosition().offset(dx,0,dz);
    for(int dy=0;dy>=-2;dy--){
     var p=feet.offset(0,dy,0);if(!WaterMovementGoal.safeDry(mob,p))continue;
     var path=mob.getNavigation().createPath(p,0);
     if(path!=null&&path.canReach()){mob.getNavigation().moveTo(path,.7);return;}
    }
   }
   return;
  }
  mob.getNavigation().stop();mob.setJumping(false);mob.setSpeed(0);mob.setXxa(0);mob.setYya(0);mob.setZza(0);
  boolean hover=SpeciesBehaviorProfiles.isAirborne(mob.getSpeciesSlug());
  double vertical=hover?Mth.clamp((mob.naturalSurface()+1.0D-mob.getY())*0.12D,-0.10D,0.10D):Math.min(0,mob.getDeltaMovement().y);
  mob.setDeltaMovement(0,vertical,0);
  if(hover){mob.setNoGravity(true);mob.fallDistance=0;}
 }
}
