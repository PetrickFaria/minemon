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
  return point.y>=mob.getY()-.2 && WaterMovementGoal.safeDry(mob,new BlockPos(point));
 }
 static void tick(MinemonCreature mob){
  State state=STATES.get(mob);
  boolean isExiting=exiting(mob);
  if(mob.isDefeated()||mob.isSleeping()||mob.isNightResting()&&!SpeciesBehaviorProfiles.isVisitor(mob.getSpeciesSlug())||mob.isOrderedToSit()||!isExiting)return;
  var path=mob.getNavigation().getPath();
  Vec3 point=path!=null&&!path.isDone()?path.getNextEntityPos(mob):new Vec3(state.bank.getX()+.5D,state.bank.getY(),state.bank.getZ()+.5D);
  Vec3 shore=state!=null&&state.bank!=null
      ?new Vec3(state.bank.getX()+.5D,state.bank.getY(),state.bank.getZ()+.5D):point;
  double dy=point.y-mob.getY();
  double top=mob instanceof MinemonAquaticCreature aquatic?aquatic.waterSurface():WaterMovementGoal.restSurfaceAt(mob,mob.blockPosition());
  if(!Double.isFinite(top)||point.y>top+1.5||dy<-.35)return;
  double lipDistance=Math.max(1.0D,mob.getBbWidth()+.5D);
  if((mob.horizontalCollision || dy>.4D&&shore.subtract(mob.position()).horizontalDistanceSqr()<lipDistance*lipDistance)
      &&mob.getDeltaMovement().y<=.05D){
   mob.setDeltaMovement(mob.getDeltaMovement().x,Math.max(.42,mob.getDeltaMovement().y),mob.getDeltaMovement().z);
   mob.getJumpControl().jump();
  }
 }
 private static final java.util.Map<MinemonCreature,State> STATES=new java.util.WeakHashMap<>();
 static void clear(MinemonCreature mob){STATES.remove(mob);}
 static boolean coolingDown(MinemonCreature mob){var state=STATES.get(mob);return state!=null&&state.retryAt>mob.tickCount;}
 static void release(MinemonCreature mob){
  var state=STATES.get(mob);if(state==null)return;
  state.bank=null;state.direct=false;state.previous=null;
  if(state.retryAt<=mob.tickCount&&state.blocked.isEmpty())STATES.remove(mob);
 }
 static void defer(MinemonCreature mob){
  var state=STATES.computeIfAbsent(mob,key->new State());state.retryAt=mob.tickCount+100;
  state.bank=null;state.direct=false;mob.getNavigation().stop();
  mob.getMoveControl().setWantedPosition(mob.getX(),mob.getY(),mob.getZ(),0);
 }
 static boolean directSteering(MinemonCreature mob){var state=STATES.get(mob);return state!=null&&state.direct&&!mob.isSleeping()&&!mob.isOrderedToSit()&&!mob.isDefeated();}
 private static final class State {BlockPos bank;Vec3 previous;int stalled,retryAt;boolean direct;final java.util.Map<BlockPos,Integer> blocked=new java.util.HashMap<>();}
  static boolean followWaterline(MinemonCreature mob,net.minecraft.world.entity.LivingEntity owner,double speed){
  if(!mob.isAquaticSpecies()||SpeciesBehaviorProfiles.isVisitor(mob.getSpeciesSlug())&&!ownerNearWater(owner))return false;
  double towardX=owner.getX()-mob.getX(),towardZ=owner.getZ()-mob.getZ();
  double ownerDistance=towardX*towardX+towardZ*towardZ;
  var candidates=new java.util.ArrayList<BlockPos>();
  for(int dx=-12;dx<=12;dx++)for(int dz=-12;dz<=12;dz++){
   var column=new BlockPos(mob.blockPosition().getX()+dx,mob.getY(),mob.blockPosition().getZ()+dz);
   if(!mob.level.hasChunkAt(column))continue;
   double top=WaterMovementGoal.restSurfaceAt(mob,column);if(!Double.isFinite(top))continue;
   boolean submerged=!mob.isNaturalSurface();
   double feetY=submerged?Math.min(mob.getY(),top-mob.getBbHeight()-.3D)
       :top-WaterMovementGoal.preferredSurfaceDepth(mob);
   var feet=new BlockPos(column.getX(),Math.floor(feetY),column.getZ());
   if(!WaterMovementGoal.waterFits(mob,feet,submerged))continue;
   double x=feet.getX()+.5D-mob.getX(),z=feet.getZ()+.5D-mob.getZ();
   double distance=x*x+z*z;
   if(distance<1.0D)continue;
   double ownerX=owner.getX()-(feet.getX()+.5D),ownerZ=owner.getZ()-(feet.getZ()+.5D);
   if(ownerX*ownerX+ownerZ*ownerZ>=ownerDistance)continue;
   candidates.add(feet);
  }
  candidates.sort(java.util.Comparator.comparingDouble(p->{
   double ox=p.getX()+.5D-owner.getX(),oz=p.getZ()+.5D-owner.getZ();
   double mx=p.getX()+.5D-mob.getX(),mz=p.getZ()+.5D-mob.getZ();
   return ox*ox+oz*oz+.1D*(mx*mx+mz*mz);
  }));
  int tried=0;BlockPos fallback=null;
  for(var feet:candidates){
   var path=mob.getNavigation().createPath(feet,0);
   boolean wetPath=path!=null&&path.canReach();
   if(wetPath&&!SpeciesBehaviorProfiles.isVisitor(mob.getSpeciesSlug()))
    for(int i=0;i<path.getNodeCount();i++)if(!mob.level.getFluidState(path.getNodePos(i)).is(FluidTags.WATER)){wetPath=false;break;}
   if(wetPath){clear(mob);mob.getNavigation().moveTo(path,speed);return true;}
   if(fallback==null)fallback=feet;
   if(++tried>=12)break;
  }
  if(fallback!=null){
   double localTop=WaterMovementGoal.restSurfaceAt(mob,fallback);
   double targetY=mob.isNaturalSurface()?WaterMovementGoal.surfaceSwimY(mob,localTop)
       :Math.min(mob.getY(),localTop-mob.getBbHeight()-.3D);
   var end=new Vec3(fallback.getX()+.5D,targetY,fallback.getZ()+.5D);var start=mob.position();boolean clear=Double.isFinite(targetY);
   for(int i=1;i<=24;i++){
    var at=start.lerp(end,i/24.0D);
    if(!mob.level.getFluidState(new BlockPos(at)).is(FluidTags.WATER)
        ||!mob.level.noCollision(mob,mob.getBoundingBox().move(at.subtract(start)))){clear=false;break;}
   }
   if(clear){var state=STATES.computeIfAbsent(mob,key->new State());state.bank=fallback;state.direct=true;
    mob.getNavigation().stop();mob.getMoveControl().setWantedPosition(end.x,end.y,end.z,speed);return true;}
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
  if(!mob.isAquaticSpecies()||ownerNearWater(owner)){STATES.remove(mob);return false;}
  if(!SpeciesBehaviorProfiles.isVisitor(mob.getSpeciesSlug())){defer(mob);return true;}
  var state=STATES.get(mob);
  if(!mob.isInWater()&&(state==null||state.bank==null)){STATES.remove(mob);return false;}
  if(state!=null&&state.bank!=null&&!mob.isInWater()&&mob.isOnGround()){
   double dx=state.bank.getX()+.5D-mob.getX(),dz=state.bank.getZ()+.5D-mob.getZ();
   double arrivalRadius=.75D+mob.getBbWidth()*.5D;
   if(dx*dx+dz*dz<=arrivalRadius*arrivalRadius){STATES.remove(mob);return false;}
  }
  if(state==null){state=new State();STATES.put(mob,state);}
  if(state.bank!=null&&!mob.level.getFluidState(state.bank).isEmpty()){
   state.bank=null;state.direct=false;
  }
  state.blocked.entrySet().removeIf(e->e.getValue()<mob.tickCount);
  if(state.previous!=null && mob.position().distanceToSqr(state.previous)<.015)state.stalled+=10;else state.stalled=0;state.previous=mob.position();
  if(state.stalled>=40 && state.bank!=null){state.blocked.put(state.bank,mob.tickCount+200);state.bank=null;mob.getNavigation().stop();state.stalled=0;}
  if(state.bank!=null){
   // Keep an active route: SmoothSwimmingMoveControl ignores direct MOVE_TO
   // commands when navigation is stopped. Replan or reject stalled banks.
   if(!mob.getNavigation().isDone()){mob.getNavigation().setSpeedModifier(speed);return true;}
   var route=mob.getNavigation().createPath(state.bank,0);
   if(route!=null&&route.canReach()){state.direct=false;mob.getNavigation().moveTo(route,speed);return true;}
   // A failed path is not permission to steer through a wall. Only a short,
   // collision-checked lip approach may outlive the final rounded path node.
   if(clearBankApproach(mob,state.bank)){
    state.direct=true;mob.getMoveControl().setWantedPosition(state.bank.getX()+.5D,state.bank.getY(),state.bank.getZ()+.5D,speed);return true;
   }
   state.blocked.put(state.bank,mob.tickCount+200);state.bank=null;state.direct=false;
  }
  double top=mob instanceof MinemonAquaticCreature aquatic?aquatic.waterSurface():WaterMovementGoal.restSurfaceAt(mob,mob.blockPosition());if(!Double.isFinite(top))return false;
  var candidates=new java.util.ArrayList<BlockPos>();
  for(int dx=-12;dx<=12;dx++)for(int dz=-12;dz<=12;dz++){
   var column=mob.blockPosition().offset(dx,0,dz);if(!mob.level.hasChunkAt(column))continue;
   // Search at the waterline, including passages beneath a roof. A heightmap
   // reports the roof/wall top and misses the walkable opening underneath it.
   for(int y=(int)Math.floor(top);y<=(int)Math.ceil(top)+1;y++){
   var feet=new BlockPos(column.getX(),y,column.getZ());
   // A real path may initially lead sideways or away from the trainer to go
   // around a wall. Geometry/pathfinding, not alignment, decides reachability.
   if(y<=top+1.5 && y>=top-.5 && !state.blocked.containsKey(feet) && WaterMovementGoal.safeDry(mob,feet))candidates.add(feet);
   }
  }
  // Reach the owner's side of the shore first. Among those banks, prefer a
  // nearby path-reachable landing; once on land the normal follow goal closes
  // the remaining distance safely.
  candidates.sort(java.util.Comparator.comparingDouble(p->p.distSqr(mob.blockPosition())+.25*p.distSqr(owner.blockPosition())));
  int tried=0;
  for(var feet:candidates){
   var path=mob.getNavigation().createPath(feet,0);
   if(path!=null&&path.canReach()){
    state.bank=feet;state.direct=false;mob.getNavigation().moveTo(path,speed);
    return true;
   }
   if(++tried>=48)break;
  }
  defer(mob);return true;
 }
 private static boolean clearBankApproach(MinemonCreature mob,BlockPos target){
  if(!WaterMovementGoal.safeDry(mob,target))return false;
  var end=new Vec3(target.getX()+.5D,target.getY(),target.getZ()+.5D);
  var delta=end.subtract(mob.position());
  if(delta.horizontalDistanceSqr()>6.25D||delta.y>1.5D||delta.y<-.5D)return false;
  double rise=Math.max(0,delta.y);
  for(int i=1;i<=8;i++){
   if(!mob.level.noCollision(mob,mob.getBoundingBox().move(0,rise*i/8.0D,0)))return false;
   if(!mob.level.noCollision(mob,mob.getBoundingBox().move(delta.x*i/8.0D,rise,delta.z*i/8.0D)))return false;
  }
  return true;
 }
 private ShoreTraversal(){}
}
