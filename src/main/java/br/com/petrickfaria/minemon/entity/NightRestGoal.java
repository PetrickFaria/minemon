package br.com.petrickfaria.minemon.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.util.Mth;

/** Holds terrestrial sleep or a collision-respecting hover one block above support. */
final class NightRestGoal extends Goal {
 private final MinemonCreature mob;
 private int unsafeLandingSearchDelay;
 private net.minecraft.core.BlockPos gyaradosBottomTarget;
 private int gyaradosBottomSearchDelay;
 NightRestGoal(MinemonCreature mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));}
 @Override public boolean canUse(){
  boolean resting=mob.isSleeping()||mob.isNightResting();
  boolean waterRest=SpeciesBehaviorProfiles.waterCapable(mob.getSpeciesSlug())&&mob.isInWaterOrBubble();
  // Let SleepLanding own MOVE while a land-resting flyer is still airborne.
  // A resting airborne species that never lands remains eligible to hover.
  return resting&&(waterRest||mob.isOnGround() && !mob.isInWaterOrBubble()||!mob.wantsNaturalLanding());
 }
 @Override public boolean canContinueToUse(){return canUse()&&mob.getTarget()==null;}
 @Override public void start(){mob.getNavigation().stop();mob.getMoveControl().setWantedPosition(mob.getX(),mob.getY(),mob.getZ(),0);gyaradosBottomTarget=null;gyaradosBottomSearchDelay=0;}
 @Override public void stop(){mob.getNavigation().stop();gyaradosBottomTarget=null;gyaradosBottomSearchDelay=0;}
 @Override public void tick(){
  if(mob.isInWaterOrBubble()&&SpeciesBehaviorProfiles.waterCapable(mob.getSpeciesSlug())){
   if(mob.getSpeciesSlug().equals("gyarados")&&!mob.isSleeping()){
    if(guideGyaradosToBottom())return;
   }
   holdWaterRest();return;
  }
  if(!SpeciesBehaviorProfiles.hoversDuringNightRest(mob.getSpeciesSlug())&&!mob.isSleeping()
      &&!WaterMovementGoal.safeDry(mob,mob.blockPosition())){
   if(!mob.getNavigation().isDone())return;
   if(unsafeLandingSearchDelay>0){unsafeLandingSearchDelay--;return;}
   unsafeLandingSearchDelay=40;
   for(int radius=1;radius<=8;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
    if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
    var feet=new net.minecraft.core.BlockPos(mob.getX(),mob.naturalSurface(),mob.getZ()).offset(dx,0,dz);
    for(int dy=0;dy>=-2;dy--){
     var p=feet.offset(0,dy,0);if(!WaterMovementGoal.safeDry(mob,p))continue;
     var path=mob.getNavigation().createPath(p,0);
     if(path!=null&&path.canReach()){mob.getNavigation().moveTo(path,.7);return;}
    }
   }
   return;
  }
  unsafeLandingSearchDelay=0;
  mob.getNavigation().stop();mob.setJumping(false);mob.setSpeed(0);mob.setXxa(0);mob.setYya(0);mob.setZza(0);
  boolean hover=SpeciesBehaviorProfiles.hoversDuringNightRest(mob.getSpeciesSlug());
  double restHeight=SpeciesBehaviorProfiles.nightRestHoverHeight(mob.getSpeciesSlug());
  double vertical=hover?Mth.clamp((mob.naturalSurface()+restHeight-mob.getY())*0.12D,-0.10D,0.10D):Math.min(0,mob.getDeltaMovement().y);
  mob.setDeltaMovement(0,vertical,0);
  if(hover){mob.setNoGravity(true);mob.fallDistance=0;}
 }
 /** Owns Gyarados' nighttime route until it reaches a safe submerged floor point. */
 private boolean guideGyaradosToBottom(){
  if(gyaradosBottomTarget==null){
   if(gyaradosBottomSearchDelay>0){gyaradosBottomSearchDelay--;return true;}
   gyaradosBottomSearchDelay=20;
   gyaradosBottomTarget=WaterMovementGoal.bottomRestDestination(mob);
   if(gyaradosBottomTarget==null)return true;
  }
  var target=gyaradosBottomTarget;
  double dx=target.getX()+.5D-mob.getX(),dz=target.getZ()+.5D-mob.getZ();
  double restY=WaterMovementGoal.waterRestY(mob);
  // GroundPathNavigation nodes use integer feet coordinates while the safe
  // bed anchor is 0.30 above the water block; allow that node offset, then
  // let the rest controller ease to the exact collision-safe anchor.
  if(dx*dx+dz*dz<.36D&&Double.isFinite(restY)&&Math.abs(mob.getY()-restY)<1.0D){
   mob.getNavigation().stop();gyaradosBottomTarget=null;return false;
  }
  if(!mob.getNavigation().isDone())return true;
  var path=mob.getNavigation().createPath(target,0);
  if(path!=null&&path.canReach())mob.getNavigation().moveTo(path,.65D);
  else gyaradosBottomTarget=null;
  return true;
 }
 private void holdWaterRest(){
  mob.getNavigation().stop();mob.setJumping(false);mob.setSpeed(0);mob.setXxa(0);mob.setYya(0);mob.setZza(0);
  if(mob.isSleeping()&&SpeciesBehaviorProfiles.locksPositionDuringWaterSleep(mob.getSpeciesSlug())){
   // Once the rest position is reached, keep the entity at that exact point.
   // The client animation controller continues independently of this motion.
   mob.setNoGravity(true);mob.fallDistance=0;mob.setDeltaMovement(0,0,0);return;
  }
  double target=WaterMovementGoal.waterRestY(mob);
  double vertical=Double.isFinite(target)
      ?net.minecraft.util.Mth.clamp((target-mob.getY())*0.12D,-0.08D,0.08D):0.0D;
  if(Math.abs(vertical)>1.0E-5D
      &&!mob.level.noCollision(mob,mob.getBoundingBox().move(0,vertical,0)))vertical=0.0D;
  mob.setNoGravity(true);mob.fallDistance=0;mob.setDeltaMovement(0,vertical,0);
 }
}
