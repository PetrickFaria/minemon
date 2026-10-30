package br.com.petrickfaria.minemon.entity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.Vec3;
/** Native shore nodes need vertical clearance before horizontal steering can cross the lip. */
final class ShoreTraversal {
 static boolean exiting(MinemonCreature mob){
  if(mob.isAquaticSpecies()&&!SpeciesBehaviorProfiles.isVisitor(mob.getSpeciesSlug()))return false;
  var path=mob.getNavigation().getPath();if(!mob.isInWater()||path==null||path.isDone())return false;
  var point=path.getNextEntityPos(mob);
  return point.y>mob.getY()+.15 && !mob.level.getFluidState(new BlockPos(point)).is(FluidTags.WATER);
 }
 static void tick(MinemonCreature mob){
  if(mob.isDefeated()||mob.isSleeping()||mob.isNightResting()||mob.isOrderedToSit()||!exiting(mob))return;
  var point=mob.getNavigation().getPath().getNextEntityPos(mob);
  double dy=point.y-mob.getY();
  double top=WaterMovementGoal.restSurfaceAt(mob,mob.blockPosition());
  if(!Double.isFinite(top)||point.y>top+1.05||dy<.05)return;
  if((mob.horizontalCollision || point.subtract(mob.position()).horizontalDistanceSqr()<4) && mob.level.noCollision(mob,mob.getBoundingBox().move(0,.12,0))){
   mob.setDeltaMovement(mob.getDeltaMovement().x,Math.max(.42,mob.getDeltaMovement().y),mob.getDeltaMovement().z);
   mob.getJumpControl().jump();
  }
 }
 private ShoreTraversal(){}
}
