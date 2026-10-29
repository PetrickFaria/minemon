package br.com.petrickfaria.minemon.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.util.Mth;

/** Holds terrestrial sleep or a collision-respecting hover one block above support. */
final class NightRestGoal extends Goal {
 private final MinemonCreature mob;
 private int unsafeLandingSearchDelay;
 NightRestGoal(MinemonCreature mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));}
 @Override public boolean canUse(){
  boolean resting=mob.isSleeping()||mob.isNightResting();
  boolean waterRest=SpeciesBehaviorProfiles.waterCapable(mob.getSpeciesSlug())&&mob.isInWaterOrBubble();
  // Let SleepLanding own MOVE while a land-resting flyer is still airborne.
  // A resting airborne species that never lands remains eligible to hover.
  return resting&&(waterRest||mob.isOnGround()||!mob.wantsNaturalLanding());
 }
 @Override public boolean canContinueToUse(){return canUse()&&mob.getTarget()==null;}
 @Override public void start(){mob.getNavigation().stop();}
 @Override public void tick(){
  if(mob.isInWaterOrBubble()&&SpeciesBehaviorProfiles.waterCapable(mob.getSpeciesSlug())){
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
 private void holdWaterRest(){
  mob.getNavigation().stop();mob.setJumping(false);mob.setSpeed(0);mob.setXxa(0);mob.setYya(0);mob.setZza(0);
  double target=WaterMovementGoal.waterRestY(mob);
  double vertical=Double.isFinite(target)
      ?net.minecraft.util.Mth.clamp((target-mob.getY())*0.12D,-0.08D,0.08D):0.0D;
  if(Math.abs(vertical)>1.0E-5D
      &&!mob.level.noCollision(mob,mob.getBoundingBox().move(0,vertical,0)))vertical=0.0D;
  mob.setNoGravity(true);mob.fallDistance=0;mob.setDeltaMovement(0,vertical,0);
 }
}
