package br.com.petrickfaria.minemon.entity;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** One navigation owner for seeking water, depth/surface swimming and shore exit. */
public final class WaterMovementGoal extends Goal {
 private final MinemonCreature mob;
 private int scanIndex,delay,stalled;
 private Vec3 lastPosition;
 private BlockPos destination;
 private final Map<BlockPos,Integer> blocked=new HashMap<>();
 private static final List<BlockPos> OFFSETS=offsets();
 private static List<BlockPos> offsets(){
  List<BlockPos> out=new ArrayList<>();
  for(int x=-25;x<=25;x++)for(int z=-25;z<=25;z++)if(x*x+z*z<=625)out.add(new BlockPos(x,0,z));
  out.sort(Comparator.comparingDouble(p->p.distSqr(BlockPos.ZERO)));return List.copyOf(out);
 }
 public WaterMovementGoal(MinemonCreature mob){this.mob=mob;delay=5+mob.getRandom().nextInt(15);setFlags(EnumSet.of(Flag.MOVE));}
 private boolean needsExit(){return SpeciesBehaviorProfiles.isVisitor(mob.getSpeciesSlug())
    &&(mob.naturalCycle().phase()==BehaviorCycle.Phase.EXIT_WATER||mob.naturalCycle().phase()==BehaviorCycle.Phase.COOLDOWN&&mob.isInWater());}
 @Override public boolean canUse(){
  if(!mob.naturalBehaviorActive())return false;
  if(delay-->0)return false;
  delay=20;
  blocked.entrySet().removeIf(e->e.getValue()<mob.tickCount);
  if(!mob.isInWater()){
   if(needsExit()||SpeciesBehaviorProfiles.isVisitor(mob.getSpeciesSlug())&&mob.naturalCycle().phase()==BehaviorCycle.Phase.COOLDOWN)return false;
   destination=scan(false);
  }else if(needsExit())destination=scan(true);
  else destination=swimDestination();
  return destination!=null;
 }
 @Override public boolean canContinueToUse(){
  return mob.naturalBehaviorActive()&&destination!=null&&!mob.getNavigation().isDone()&&stalled<60;
 }
 @Override public void start(){
  var path=mob.getNavigation().createPath(destination,0);
  if(path==null||!path.canReach()){blocked.put(destination,mob.tickCount+200);destination=null;return;}
  mob.getNavigation().moveTo(path,0.85D);lastPosition=mob.position();stalled=0;
 }
 @Override public void tick(){
  if(destination==null||lastPosition==null)return;
  if(mob.position().distanceToSqr(lastPosition)<0.0001D||mob.horizontalCollision)stalled++;else stalled=0;
  lastPosition=mob.position();
  if(needsExit()&&!mob.isInWater()&&mob.isOnGround())mob.getNavigation().stop();
  // Check the next half-block early, before a wall becomes a repeated destination.
  if(mob.isInWater()&&!needsExit()){
   Vec3 toward=Vec3.atCenterOf(destination).subtract(mob.position()).normalize().scale(0.5D);
   if(!mob.level.noCollision(mob,mob.getBoundingBox().move(toward))){
    stalled=60;mob.getNavigation().stop();
   }
  }
 }
 @Override public void stop(){
  if(destination!=null&&stalled>=60)blocked.put(destination,mob.tickCount+200);
  mob.getNavigation().stop();destination=null;delay=15+mob.getRandom().nextInt(25);
 }
 private BlockPos scan(boolean dry){
  BlockPos origin=mob.blockPosition();int paths=0;
  // Bounded work: at most 64 columns/20 ticks, two path computations per batch.
  // The sorted cursor eventually covers every column inside radius 25.
  for(int n=0;n<64;n++){
   BlockPos offset=OFFSETS.get(scanIndex++%OFFSETS.size());
   for(int dy=-6;dy<=6;dy++){
    BlockPos p=origin.offset(offset.getX(),dy,offset.getZ());
    if(p.distSqr(origin)>625||!mob.level.hasChunkAt(p)||blocked.containsKey(p))continue;
    if(dry?!safeDry(mob,p):!waterFits(mob,p,SpeciesBehaviorProfiles.profile(mob.getSpeciesSlug())!=SpeciesBehaviorProfiles.Profile.SURFACE))continue;
    if(paths++>=2)return null;
    var path=mob.getNavigation().createPath(p,0);
    if(path!=null&&path.canReach())return p;
    blocked.put(p.immutable(),mob.tickCount+200);
   }
  }
  return null;
 }
 private BlockPos swimDestination(){
  boolean surface=mob.isNaturalSurface();
  for(int n=0;n<12;n++){
   int dx=mob.getRandom().nextInt(15)-7,dz=mob.getRandom().nextInt(15)-7;
   if(dx*dx+dz*dz<4)continue;
   int x=mob.blockPosition().getX()+dx,z=mob.blockPosition().getZ()+dz;
   BlockPos column=new BlockPos(x,mob.getY(),z);
   double top=surfaceAt(mob,column);
   if(!Double.isFinite(top))continue;
   double y=surface?top-Math.min(0.8D,mob.getBbHeight()*0.3D)
       :Math.min(top-mob.getBbHeight()-0.3D,mob.getY()+mob.getRandom().nextInt(5)-2);
   BlockPos p=new BlockPos(x,y,z);
   if(blocked.containsKey(p)||!waterFits(mob,p,!surface))continue;
   boolean clear=true;
   for(int step=1;step<=4;step++){
    Vec3 point=mob.position().lerp(Vec3.atCenterOf(p),step/4.0D);
    if(!mob.level.noCollision(mob,mob.getBoundingBox().move(point.subtract(mob.position())))){clear=false;break;}
   }
   if(!clear)continue;
   var path=mob.getNavigation().createPath(p,0);
   if(path!=null&&path.canReach())return p;
   blocked.put(p.immutable(),mob.tickCount+200);
  }
  return null;
 }
 static boolean safeDry(MinemonCreature mob,BlockPos feet){
  var floor=mob.level.getBlockState(feet.below());
  return mob.level.getFluidState(feet).isEmpty()&&mob.level.getFluidState(feet.below()).isEmpty()
      &&floor.isFaceSturdy(mob.level,feet.below(),net.minecraft.core.Direction.UP)
      &&!floor.is(Blocks.MAGMA_BLOCK)&&!floor.is(Blocks.CACTUS)&&!floor.is(Blocks.CAMPFIRE)&&!floor.is(Blocks.SOUL_CAMPFIRE)
      &&mob.level.noCollision(mob,mob.getBoundingBox().move(feet.getX()+0.5D-mob.getX(),feet.getY()-mob.getY(),feet.getZ()+0.5D-mob.getZ()));
 }
 static boolean waterFits(MinemonCreature mob,BlockPos p,boolean deep){
  if(!mob.level.getFluidState(p).is(FluidTags.WATER))return false;
  double top=surfaceAt(mob,p);if(!Double.isFinite(top))return false;
  double y=deep?p.getY():top-Math.min(0.8D,mob.getBbHeight()*0.3D);
  var box=mob.getBoundingBox().move(p.getX()+0.5D-mob.getX(),y-mob.getY(),p.getZ()+0.5D-mob.getZ());
  if(!mob.level.noCollision(mob,box))return false;
  if(deep&&y+mob.getBbHeight()+0.15D>=top)return false;
  if(mob.level.getBlockState(new BlockPos(p.getX(),y-0.1D,p.getZ())).is(Blocks.MAGMA_BLOCK))return false;
  for(int x=(int)Math.floor(box.minX+0.001D);x<=Math.floor(box.maxX-0.001D);x++)
   for(int z=(int)Math.floor(box.minZ+0.001D);z<=Math.floor(box.maxZ-0.001D);z++){
    if(!mob.level.getFluidState(new BlockPos(x,y+0.1D,z)).is(FluidTags.WATER))return false;
    if(deep&&!mob.level.getFluidState(new BlockPos(x,y+mob.getBbHeight()-0.05D,z)).is(FluidTags.WATER))return false;
   }
  return true;
 }
 public static boolean canDive(MinemonCreature mob,BlockPos p){
  if(!mob.level.getFluidState(p).is(FluidTags.WATER))return false;
  double top=surfaceAt(mob,p);
  int depth=(int)Math.ceil(mob.getBbHeight()+0.4D);
  return waterFits(mob,new BlockPos(p.getX(),top-depth,p.getZ()),true);
 }
 public static double surfaceAt(MinemonCreature mob,BlockPos p){
  if(!mob.level.hasChunkAt(p))return Double.NaN;
  BlockPos q=p;
  // Surface followers may be fractionally above water after interpolation.
  if(!mob.level.getFluidState(q).is(FluidTags.WATER))q=q.below();
  if(!mob.level.getFluidState(q).is(FluidTags.WATER))return Double.NaN;
  for(int n=0;n<32&&mob.level.getFluidState(q.above()).is(FluidTags.WATER);n++)q=q.above();
  return q.getY()+mob.level.getFluidState(q).getHeight(mob.level,q);
 }
}
