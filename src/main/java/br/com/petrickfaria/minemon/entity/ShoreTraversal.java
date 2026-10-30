package br.com.petrickfaria.minemon.entity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.Vec3;
/** Native shore nodes need vertical clearance before horizontal steering can cross the lip. */
final class ShoreTraversal {
 static boolean exiting(MinemonCreature mob){
  if(mob.isAquaticSpecies()&&!SpeciesBehaviorProfiles.isVisitor(mob.getSpeciesSlug()))return false;
  State state=STATES.get(mob);
  if(mob.isInWater()&&state!=null&&state.bank!=null&&mob.level.getFluidState(state.bank).isEmpty())return true;
  var path=mob.getNavigation().getPath();if(!mob.isInWater()||path==null||path.isDone())return false;
  var point=path.getNextEntityPos(mob);
  return point.y>=mob.getY()-.2 && !mob.level.getFluidState(new BlockPos(point)).is(FluidTags.WATER);
 }
 static void tick(MinemonCreature mob){
  if(mob.isDefeated()||mob.isSleeping()||mob.isNightResting()||mob.isOrderedToSit()||!exiting(mob))return;
  var point=mob.getNavigation().getPath().getNextEntityPos(mob);
  State state=STATES.get(mob);
  Vec3 shore=state!=null&&state.bank!=null
      ?new Vec3(state.bank.getX()+.5D,state.bank.getY(),state.bank.getZ()+.5D):point;
  double dy=point.y-mob.getY();
  double top=WaterMovementGoal.restSurfaceAt(mob,mob.blockPosition());
  if(!Double.isFinite(top)||point.y>top+1.05||dy<-.35)return;
  if(state!=null&&state.bank!=null){
   double dx=shore.x-mob.getX(),dz=shore.z-mob.getZ(),distance=Math.sqrt(dx*dx+dz*dz);
   if(distance>1.0D){
    var velocity=mob.getDeltaMovement();
    double steer=Math.min(.12D,.035D+distance*.008D);
    mob.setDeltaMovement(velocity.x*.55D+dx/distance*steer,velocity.y,velocity.z*.55D+dz/distance*steer);
   }
  }
  if((mob.horizontalCollision || shore.subtract(mob.position()).horizontalDistanceSqr()<6.25D)
      &&mob.level.noCollision(mob,mob.getBoundingBox().move(0,.12,0))){
   mob.setDeltaMovement(mob.getDeltaMovement().x,Math.max(.42,mob.getDeltaMovement().y),mob.getDeltaMovement().z);
   mob.getJumpControl().jump();
  }
 }
 private static final java.util.Map<MinemonCreature,State> STATES=new java.util.WeakHashMap<>();
 private static final class State {BlockPos bank;Vec3 previous;int stalled;final java.util.Map<BlockPos,Integer> blocked=new java.util.HashMap<>();}
 static boolean followBank(MinemonCreature mob,net.minecraft.world.entity.LivingEntity owner,double speed){
  if(!SpeciesBehaviorProfiles.isVisitor(mob.getSpeciesSlug())||!mob.isInWater()||owner.isInWaterOrBubble()){STATES.remove(mob);return false;}
  var state=STATES.computeIfAbsent(mob,m->new State());state.blocked.entrySet().removeIf(e->e.getValue()<mob.tickCount);
  if(state.previous!=null && mob.position().distanceToSqr(state.previous)<.015)state.stalled+=10;else state.stalled=0;state.previous=mob.position();
  if(state.stalled>=40 && state.bank!=null){state.blocked.put(state.bank,mob.tickCount+200);state.bank=null;mob.getNavigation().stop();state.stalled=0;}
  if(state.bank!=null && !mob.getNavigation().isDone()){mob.getNavigation().setSpeedModifier(speed);return true;}
  double top=WaterMovementGoal.restSurfaceAt(mob,mob.blockPosition());if(!Double.isFinite(top))return false;
  var candidates=new java.util.ArrayList<BlockPos>();
  for(int dx=-12;dx<=12;dx++)for(int dz=-12;dz<=12;dz++){
   var column=mob.blockPosition().offset(dx,0,dz);if(!mob.level.hasChunkAt(column))continue;
   int y=mob.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,column.getX(),column.getZ());
   var feet=new BlockPos(column.getX(),y,column.getZ());
   if(y<=top+1.05 && y>=top-.5 && !state.blocked.containsKey(feet) && WaterMovementGoal.safeDry(mob,feet))candidates.add(feet);
  }
  // Reach dry land first. Biasing the search toward the owner can select a
  // distant bank on the far side of the water, where amphibious paths often
  // fail to leave a vertical shoreline. Once on land the normal follow goal
  // can close the remaining distance safely.
  candidates.sort(java.util.Comparator.comparingDouble(p->p.distSqr(mob.blockPosition())+.15*p.distSqr(owner.blockPosition())));
  int tried=0;
  for(var feet:candidates){
   var path=mob.getNavigation().createPath(feet,0);
   if(path!=null&&path.canReach()){state.bank=feet;mob.getNavigation().moveTo(path,speed);return true;}
   state.blocked.put(feet,mob.tickCount+200);if(++tried>=12)break;
  }
  return false;
 }
 private ShoreTraversal(){}
}
