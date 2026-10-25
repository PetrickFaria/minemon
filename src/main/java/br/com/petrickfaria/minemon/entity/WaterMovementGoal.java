package br.com.petrickfaria.minemon.entity;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** One navigation owner for seeking water, depth/surface swimming and shore exit. */
public final class WaterMovementGoal extends Goal {
 public static final int SEARCH_RADIUS=100;
 private final MinemonCreature mob;
 private int scanIndex,scanYIndex,delay,stalled;
 private Vec3 lastPosition;
 private boolean scanDepth;
 private Boolean lastDryScan;
 private BlockPos scanOrigin;
 private BlockPos destination;
 private final Map<BlockPos,Integer> blocked=new HashMap<>();
 private static final List<BlockPos> OFFSETS=offsets();
 private static final int[][] SWIM_DIRECTIONS={{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1},{0,-1},{1,-1}};
 private static List<BlockPos> offsets(){
  List<BlockPos> out=new ArrayList<>();
  for(int x=-SEARCH_RADIUS;x<=SEARCH_RADIUS;x++)for(int z=-SEARCH_RADIUS;z<=SEARCH_RADIUS;z++)if(x*x+z*z<=SEARCH_RADIUS*SEARCH_RADIUS)out.add(new BlockPos(x,0,z));
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
  else {
   if(SpeciesBehaviorProfiles.isVisitor(mob.getSpeciesSlug()))
    mob.chooseNaturalWaterDepth(!canDive(mob,mob.blockPosition())
        ||!SpeciesBehaviorProfiles.prefersBottom(mob.getSpeciesSlug())&&mob.getRandom().nextInt(3)==0);
   destination=swimDestination();
  }
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
   var path=mob.getNavigation().getPath();
   Vec3 waypoint=Vec3.atCenterOf(destination);
   if(path!=null&&!path.isDone()){
    var node=path.getNode(path.getNextNodeIndex());
    waypoint=new Vec3(node.x+0.5D,node.y,node.z+0.5D);
   }
   Vec3 toward=waypoint.subtract(mob.position()).normalize().scale(0.5D);
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
  // Shore exit starts near the swimmer, independent of how far the prior
  // water search progressed. Do not reuse distant-water scan cursors on land.
  if(lastDryScan==null||lastDryScan!=dry||scanOrigin==null||scanOrigin.distSqr(mob.blockPosition())>256.0D){
   lastDryScan=dry;scanIndex=0;scanYIndex=0;scanDepth=false;scanOrigin=mob.blockPosition();
  }
  BlockPos origin=scanOrigin;int paths=0;
  // Bounded batches cover the full three-dimensional radius, including
  // water farther above/below than a shore step. Both cursors survive retries;
  // a blocked deep node cannot restart the same column indefinitely.
  for(int n=0;n<2048;n++){
   BlockPos offset=OFFSETS.get(scanIndex%OFFSETS.size());
   BlockPos p;
   if(!scanDepth){
    BlockPos column=origin.offset(offset.getX(),0,offset.getZ());
    if(++scanIndex>=OFFSETS.size()){scanIndex=0;scanDepth=true;}
    if(!mob.level.hasChunkAt(column))continue;
    int top=mob.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,column.getX(),column.getZ());
    // Prioritize open water/shore columns over the exhaustive underground pass.
    p=new BlockPos(column.getX(),dry?top:top-2,column.getZ());
   }else{
    int dy=scanYIndex==0?0:((scanYIndex+1)/2)*(scanYIndex%2==1?-1:1);
    if(++scanYIndex>SEARCH_RADIUS*2){
     scanYIndex=0;
     if(++scanIndex>=OFFSETS.size()){scanIndex=0;scanDepth=false;}
    }
    p=origin.offset(offset.getX(),dy,offset.getZ());
   }
   if(!withinSearchRadius(mob.blockPosition(),p)||!mob.level.hasChunkAt(p)||blocked.containsKey(p))continue;
   if(dry?!safeDry(mob,p):!waterFits(mob,p,SpeciesBehaviorProfiles.profile(mob.getSpeciesSlug())!=SpeciesBehaviorProfiles.Profile.SURFACE)
       ||requiresNavigableVolume(mob.getSpeciesSlug())&&!waterRegion(mob,p))continue;
   paths++;
   var path=mob.getNavigation().createPath(p,0);
   if(path!=null&&path.canReach())return p;
   blocked.put(p.immutable(),mob.tickCount+200);
   if(paths>=2)return null;
  }
  return null;
 }
 private BlockPos swimDestination(){
  boolean surface=mob.isNaturalSurface();
  boolean bottomPreferred=SpeciesBehaviorProfiles.prefersBottom(mob.getSpeciesSlug());
  // Far choices distribute swimmers through large lakes; local and fixed
  // probes prevent small ponds/corners from depending on lucky random draws.
  for(int n=0;n<48;n++){
   int dx,dz;
   if(n<12){dx=mob.getRandom().nextInt(49)-24;dz=mob.getRandom().nextInt(49)-24;}
   else if(n<24){dx=mob.getRandom().nextInt(13)-6;dz=mob.getRandom().nextInt(13)-6;}
   else {int[] direction=SWIM_DIRECTIONS[(n-24)%8];int distance=2+2*((n-24)/8);dx=direction[0]*distance;dz=direction[1]*distance;}
   if(dx*dx+dz*dz<4)continue;
   int x=mob.blockPosition().getX()+dx,z=mob.blockPosition().getZ()+dz;
   BlockPos column=new BlockPos(x,mob.getY(),z);
   double top=surfaceAt(mob,column);
   if(!Double.isFinite(top))continue;
   double bottomY=bottomPreferred?preferredBottomY(mob,column,top):Double.NaN;
   double y=Double.isFinite(bottomY)?bottomY:surface?top-preferredSurfaceDepth(mob)
       :n>=24?top-Math.max(2.0D,mob.getBbHeight()+0.3D)
       :Math.min(top-mob.getBbHeight()-0.3D,mob.getY()+mob.getRandom().nextInt(5)-2);
   BlockPos p=new BlockPos(x,y,z);
   if(blocked.containsKey(p)||!waterFits(mob,p,!surface||bottomPreferred)
       ||requiresNavigableVolume(mob.getSpeciesSlug())&&!waterRegion(mob,p))continue;
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
 private static double preferredBottomY(MinemonCreature mob,BlockPos column,double surface){
  int floorWater=bottomWaterBlock(mob,column,surface);
  if(!safeWaterFloor(mob,column,floorWater))return Double.NaN;
  if(surface-floorWater<=mob.getBbHeight()+0.45D)return Double.NaN;
  double target=floorWater+0.02D;
  var box=mob.getBoundingBox().move(column.getX()+0.5D-mob.getX(),target-mob.getY(),column.getZ()+0.5D-mob.getZ());
  if(!mob.level.noCollision(mob,box))return Double.NaN;
  for(int x=(int)Math.floor(box.minX+0.001D);x<=Math.floor(box.maxX-0.001D);x++)
   for(int z=(int)Math.floor(box.minZ+0.001D);z<=Math.floor(box.maxZ-0.001D);z++)
    if(!mob.level.getFluidState(new BlockPos(x,floorWater,z)).is(FluidTags.WATER))return Double.NaN;
  return target;
 }
 public static double waterRestY(MinemonCreature mob){
  double top=surfaceAt(mob,mob.blockPosition());
  if(!Double.isFinite(top))return Double.NaN;
  if(SpeciesBehaviorProfiles.isVisitor(mob.getSpeciesSlug())){
   BlockPos column=mob.blockPosition();int floorWater=bottomWaterBlock(mob,column,top);
   if(safeWaterFloor(mob,column,floorWater)){
    double target=floorWater+0.02D;
    var box=mob.getBoundingBox().move(0,target-mob.getY(),0);
    if(mob.level.noCollision(mob,box))return target;
   }
  }
  return SpeciesBehaviorProfiles.profile(mob.getSpeciesSlug())==SpeciesBehaviorProfiles.Profile.SURFACE
      ?top-preferredSurfaceDepth(mob):top-mob.getBbHeight()-0.2D;
 }
 private static boolean safeWaterFloor(MinemonCreature mob,BlockPos column,int floorWater){
  int min=mob.level.getMinBuildHeight();if(floorWater<=min)return false;
  BlockPos floor=new BlockPos(column.getX(),floorWater-1,column.getZ());
  var state=mob.level.getBlockState(floor);
  return !state.getCollisionShape(mob.level,floor).isEmpty()
      &&!state.is(Blocks.MAGMA_BLOCK)&&!state.is(Blocks.CACTUS)
      &&!state.is(Blocks.CAMPFIRE)&&!state.is(Blocks.SOUL_CAMPFIRE);
 }
 private static int bottomWaterBlock(MinemonCreature mob,BlockPos column,double surface){
  int y=(int)Math.floor(surface)-1;
  int lowest=mob.level.getMinBuildHeight();
  while(y>=lowest&&mob.level.getFluidState(new BlockPos(column.getX(),y,column.getZ())).is(FluidTags.WATER))y--;
  return y+1;
 }
 static boolean safeDry(MinemonCreature mob,BlockPos feet){
  var floor=mob.level.getBlockState(feet.below());
  return mob.level.getFluidState(feet).isEmpty()&&mob.level.getFluidState(feet.below()).isEmpty()
      &&!floor.getCollisionShape(mob.level,feet.below()).isEmpty()
      &&!floor.is(Blocks.MAGMA_BLOCK)&&!floor.is(Blocks.CACTUS)&&!floor.is(Blocks.CAMPFIRE)&&!floor.is(Blocks.SOUL_CAMPFIRE)
      &&mob.level.noCollision(mob,mob.getBoundingBox().move(feet.getX()+0.5D-mob.getX(),feet.getY()-mob.getY(),feet.getZ()+0.5D-mob.getZ()));
 }
 static boolean waterFits(MinemonCreature mob,BlockPos p,boolean deep){
  if(!mob.level.getFluidState(p).is(FluidTags.WATER))return false;
  double top=surfaceAt(mob,p);if(!Double.isFinite(top))return false;
  double y=deep?p.getY():top-preferredSurfaceDepth(mob);
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
 public static double preferredSurfaceDepth(MinemonCreature mob){
  // The generic surface depth is measured from the feet; Gyarados instead
  // keeps its body center 1.5 blocks below the surface and its full hitbox wet.
  return mob.getSpeciesSlug().equals("gyarados")?1.5D+mob.getBbHeight()*0.5D:Math.min(0.8D,mob.getBbHeight()*0.3D);
 }
 public static boolean withinSearchRadius(BlockPos origin,BlockPos candidate){return origin.distSqr(candidate)<=SEARCH_RADIUS*SEARCH_RADIUS;}
 public static boolean requiresNavigableVolume(String speciesSlug){return SpeciesBehaviorProfiles.waterCapable(speciesSlug);}
 public static boolean isNavigableWaterRegion(BlockPos center,java.util.function.Predicate<BlockPos> water){
  for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)if(!water.test(center.offset(x,y,z)))return false;
  return true;
 }
 private static boolean waterRegion(MinemonCreature mob,BlockPos center){
  double top=surfaceAt(mob,center);
  if(!Double.isFinite(top))return false;
  int verticalCenter=SpeciesBehaviorProfiles.prefersBottom(mob.getSpeciesSlug())
      ?Math.max(center.getY(),bottomWaterBlock(mob,center,top)+1)
      :Math.min(center.getY(),(int)Math.ceil(top)-2);
  BlockPos volume=new BlockPos(center.getX(),verticalCenter,center.getZ());
  return isNavigableWaterRegion(volume,cell->mob.level.hasChunkAt(cell)
      &&mob.level.getFluidState(cell).is(FluidTags.WATER)
      &&mob.level.getBlockState(cell).getCollisionShape(mob.level,cell).isEmpty());
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
