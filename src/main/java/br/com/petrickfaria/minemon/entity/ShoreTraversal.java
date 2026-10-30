package br.com.petrickfaria.minemon.entity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.Vec3;
/** Native shore nodes need vertical clearance before horizontal steering can cross the lip. */
final class ShoreTraversal {
 static boolean exiting(MinemonCreature mob){
  State state=STATES.get(mob);
  if(mob.isAquaticSpecies()&&!SpeciesBehaviorProfiles.isVisitor(mob.getSpeciesSlug())
      &&(state==null||state.bank==null))return false;
  if(!mob.isInWater())return false;
  if(state!=null&&state.bank!=null&&mob.level.getFluidState(state.bank).isEmpty())return true;
  var path=mob.getNavigation().getPath();if(path==null||path.isDone())return false;
  var point=path.getNextEntityPos(mob);
  return point.y>=mob.getY()-.2 && !mob.level.getFluidState(new BlockPos(point)).is(FluidTags.WATER);
 }
 static void tick(MinemonCreature mob){
  if(mob.isDefeated()||mob.isSleeping()||mob.isNightResting()||mob.isOrderedToSit()||!exiting(mob))return;
  State state=STATES.get(mob);
  var path=mob.getNavigation().getPath();
  Vec3 point=path!=null&&!path.isDone()?path.getNextEntityPos(mob):new Vec3(state.bank.getX()+.5D,state.bank.getY(),state.bank.getZ()+.5D);
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
  if((mob.horizontalCollision || shore.subtract(mob.position()).horizontalDistanceSqr()<20.25D)
      &&mob.level.noCollision(mob,mob.getBoundingBox().move(0,.12,0))){
   mob.setDeltaMovement(mob.getDeltaMovement().x,Math.max(.42,mob.getDeltaMovement().y),mob.getDeltaMovement().z);
   mob.getJumpControl().jump();
  }
 }
 private static final java.util.Map<MinemonCreature,State> STATES=new java.util.WeakHashMap<>();
 private static final class State {BlockPos bank;Vec3 previous;int stalled;final java.util.Map<BlockPos,Integer> blocked=new java.util.HashMap<>();}
  static boolean followWaterline(MinemonCreature mob,net.minecraft.world.entity.LivingEntity owner,double speed){
  if(!mob.isAquaticSpecies()||mob.isInWater()||!ownerNearWater(owner))return false;
  double towardX=owner.getX()-mob.getX(),towardZ=owner.getZ()-mob.getZ();
  double ownerDistance=towardX*towardX+towardZ*towardZ;
  var candidates=new java.util.ArrayList<BlockPos>();
  for(int dx=-12;dx<=12;dx++)for(int dz=-12;dz<=12;dz++){
   var column=new BlockPos(mob.blockPosition().getX()+dx,mob.getY(),mob.blockPosition().getZ()+dz);
   if(!mob.level.hasChunkAt(column))continue;
   double top=WaterMovementGoal.restSurfaceAt(mob,column);if(!Double.isFinite(top))continue;
   double feetY=top-WaterMovementGoal.preferredSurfaceDepth(mob);
   var feet=new BlockPos(column.getX(),Math.floor(feetY),column.getZ());
   if(!WaterMovementGoal.waterFits(mob,feet,false))continue;
   double x=feet.getX()+.5D-mob.getX(),z=feet.getZ()+.5D-mob.getZ();
   double distance=x*x+z*z,dot=towardX*x+towardZ*z;
   if(dot<=0.0D||dot*dot<.8D*ownerDistance*distance)continue;
   double ownerX=owner.getX()-(feet.getX()+.5D),ownerZ=owner.getZ()-(feet.getZ()+.5D);
   if(ownerX*ownerX+ownerZ*ownerZ>=ownerDistance)continue;
   candidates.add(feet);
  }
  candidates.sort(java.util.Comparator.comparingDouble(p->{
   double ox=p.getX()+.5D-owner.getX(),oz=p.getZ()+.5D-owner.getZ();
   double mx=p.getX()+.5D-mob.getX(),mz=p.getZ()+.5D-mob.getZ();
   return ox*ox+oz*oz+.1D*(mx*mx+mz*mz);
  }));
  BlockPos fallback=null;int tried=0;
  for(var feet:candidates){
   var path=mob.getNavigation().createPath(feet,0);
   if(path!=null&&path.canReach()){mob.getNavigation().moveTo(path,speed);return true;}
   if(fallback==null)fallback=feet;
   if(++tried>=12)break;
  }
  if(fallback!=null){
   mob.getNavigation().stop();
   double top=WaterMovementGoal.restSurfaceAt(mob,fallback);
   double y=Double.isFinite(top)?top-WaterMovementGoal.preferredSurfaceDepth(mob):fallback.getY();
   mob.getMoveControl().setWantedPosition(fallback.getX()+.5D,y,fallback.getZ()+.5D,speed);
   return true;
  }
  return false;
 }
 private static boolean ownerNearWater(net.minecraft.world.entity.LivingEntity owner){
  var origin=owner.blockPosition();
  for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)for(int dy=-1;dy<=2;dy++){
   var cell=origin.offset(dx,dy,dz);
   if(owner.level.hasChunkAt(cell)&&owner.level.getFluidState(cell).is(net.minecraft.tags.FluidTags.WATER))return true;
  }
  return false;
 }
static boolean followBank(MinemonCreature mob,net.minecraft.world.entity.LivingEntity owner,double speed){
  if(!mob.isAquaticSpecies()||!mob.isInWater()||ownerNearWater(owner)){STATES.remove(mob);return false;}
  var state=STATES.computeIfAbsent(mob,m->new State());state.blocked.entrySet().removeIf(e->e.getValue()<mob.tickCount);
  if(state.previous!=null && mob.position().distanceToSqr(state.previous)<.015)state.stalled+=10;else state.stalled=0;state.previous=mob.position();
  if(state.stalled>=40 && state.bank!=null){state.blocked.put(state.bank,mob.tickCount+200);state.bank=null;mob.getNavigation().stop();state.stalled=0;}
  if(state.bank!=null){
   // Amphibious path nodes can complete on the water side of a one-block
   // bank. Keep steering toward the already reachability-checked landing
   // point directly; block collision still governs movement and the stalled
   // route check below selects another bank if this line is obstructed.
   mob.getNavigation().stop();
   mob.getMoveControl().setWantedPosition(state.bank.getX()+.5D,state.bank.getY(),state.bank.getZ()+.5D,speed);
   return true;
  }
  double top=WaterMovementGoal.restSurfaceAt(mob,mob.blockPosition());if(!Double.isFinite(top))return false;
  var candidates=new java.util.ArrayList<BlockPos>();
  for(int dx=-12;dx<=12;dx++)for(int dz=-12;dz<=12;dz++){
   var column=mob.blockPosition().offset(dx,0,dz);if(!mob.level.hasChunkAt(column))continue;
   int y=mob.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,column.getX(),column.getZ());
   var feet=new BlockPos(column.getX(),y,column.getZ());
   double towardOwnerX=owner.getX()-mob.getX(),towardOwnerZ=owner.getZ()-mob.getZ();
   double bankX=feet.getX()+.5D-mob.getX(),bankZ=feet.getZ()+.5D-mob.getZ();
   double ownerDistanceSqr=towardOwnerX*towardOwnerX+towardOwnerZ*towardOwnerZ;
   double bankDistanceSqr=bankX*bankX+bankZ*bankZ;
   double towardOwner=towardOwnerX*bankX+towardOwnerZ*bankZ;
   // Keep the landing closely aligned with the owner's direction and make sure it shortens
   // the remaining distance. An angled path around the pool can otherwise
   // select a nearby, reachable bank behind or beside the trainer.
   if(towardOwner<=0.0D||towardOwner*towardOwner<.9D*ownerDistanceSqr*bankDistanceSqr
       ||feet.distSqr(owner.blockPosition())>=mob.distanceToSqr(owner))continue;
   if(y<=top+1.05 && y>=top-.5 && !state.blocked.containsKey(feet) && WaterMovementGoal.safeDry(mob,feet))candidates.add(feet);
  }
  // Reach the owner's side of the shore first. Among those banks, prefer a
  // nearby path-reachable landing; once on land the normal follow goal closes
  // the remaining distance safely.
  candidates.sort(java.util.Comparator.comparingDouble(p->p.distSqr(owner.blockPosition())+.25*p.distSqr(mob.blockPosition())));
  int tried=0;BlockPos physicalFallback=null;
  for(var feet:candidates){
   var path=mob.getNavigation().createPath(feet,0);
   if(path!=null&&path.canReach()){
    state.bank=feet;mob.getNavigation().stop();
    mob.getMoveControl().setWantedPosition(feet.getX()+.5D,feet.getY(),feet.getZ()+.5D,speed);
    return true;
   }
   // A one-block bank can be physically jumpable even when an amphibious
   // path reports it unreachable. Try one safe landing directly; collision
   // and the existing stall blacklist decide whether another bank is needed.
   if(physicalFallback==null)physicalFallback=feet;
   if(++tried>=12)break;
  }
  if(physicalFallback!=null){
   state.bank=physicalFallback;mob.getNavigation().stop();
   mob.getMoveControl().setWantedPosition(physicalFallback.getX()+.5D,physicalFallback.getY(),physicalFallback.getZ()+.5D,speed);
   return true;
  }
  return false;
 }
 private ShoreTraversal(){}
}
