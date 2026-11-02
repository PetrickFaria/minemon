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
 public static final double WALL_PREFERENCE_DISTANCE=5.0D;
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
   destination=swimDestination();
  }
  return destination!=null;
 }
 @Override public boolean canContinueToUse(){
  return mob.naturalBehaviorActive()&&destination!=null&&!mob.getNavigation().isDone()&&stalled<60;
 }
 @Override public void start(){
  var path=mob.getNavigation().createPath(destination,0);
  if(path==null||!path.canReach()
      ||mob.getSpeciesSlug().equals("gyarados")&&!surfacePathIsWater(path)){
   blocked.put(destination,mob.tickCount+200);mob.getNavigation().stop();destination=null;return;
  }
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
    // Match Path's width-aware steering point, including wide swimmers.
    waypoint=path.getNextEntityPos(mob);
   }
   Vec3 delta=waypoint.subtract(mob.position());
   // Never probe past a nearby node into a wall on its opposite side.
   Vec3 toward=delta.normalize().scale(Math.min(0.5D,delta.length()));
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
   if(dry?!safeDry(mob,p):!waterFits(mob,p,SpeciesBehaviorProfiles.profile(mob.getSpeciesSlug())!=SpeciesBehaviorProfiles.Profile.AQUATIC_SURFACE)
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
  if(bottomPreferred&&!surface){
   BlockPos nearby=nearbyBottomDestination();
   if(nearby!=null)return nearby;
  }
  // Far choices distribute swimmers through large lakes; local and fixed
  // probes prevent small ponds/corners from depending on lucky random draws.
  // Aquatic profiles score the full path by clearance from lateral collision
  // shapes, measuring from the entity AABB. Five blocks is preferred; if the
  // water is too narrow, the route with the greatest available clearance wins.
  boolean preferWalls=SpeciesBehaviorProfiles.isAquatic(mob.getSpeciesSlug());
  var wallShapeCache=new HashMap<BlockPos,List<net.minecraft.world.phys.AABB>>();
  BlockPos bestDestination=null;double bestClearance=-1.0D;
  for(int n=0;n<48;n++){
   int dx,dz;
   if(surface&&n<12){dx=mob.getRandom().nextInt(5)-2;dz=mob.getRandom().nextInt(5)-2;}
   else if(surface&&n<24){int[] direction=SWIM_DIRECTIONS[(n-12)%8];int distance=2+(n-12)/8;dx=direction[0]*distance;dz=direction[1]*distance;}
   else if(n<12){dx=mob.getRandom().nextInt(49)-24;dz=mob.getRandom().nextInt(49)-24;}
   else if(n<24){dx=mob.getRandom().nextInt(13)-6;dz=mob.getRandom().nextInt(13)-6;}
   else if(surface){int[] direction=SWIM_DIRECTIONS[(n-24)%8];int distance=2+((n-24)/8)%2;dx=direction[0]*distance;dz=direction[1]*distance;}
   else {int[] direction=SWIM_DIRECTIONS[(n-24)%8];int distance=2+2*((n-24)/8);dx=direction[0]*distance;dz=direction[1]*distance;}
   if(dx*dx+dz*dz<4)continue;
   int x=mob.blockPosition().getX()+dx,z=mob.blockPosition().getZ()+dz;
   BlockPos column=new BlockPos(x,mob.getY(),z);
   double top=surfaceAt(mob,column);
   if(!Double.isFinite(top))continue;
   double bottomY=bottomPreferred&&!surface?preferredBottomY(mob,column,top):Double.NaN;
   double y=Double.isFinite(bottomY)?bottomY:surface?top-preferredSurfaceDepth(mob)
       :n>=24?top-Math.max(2.0D,mob.getBbHeight()+0.3D)
       :Math.min(top-mob.getBbHeight()-0.3D,mob.getY()+mob.getRandom().nextInt(5)-2);
   BlockPos p=new BlockPos(x,bottomPreferred&&Double.isFinite(bottomY)?Math.ceil(y):y,z);
   if(blocked.containsKey(p)||!waterFits(mob,p,!surface)
       ||requiresNavigableVolume(mob.getSpeciesSlug())&&!waterRegion(mob,p))continue;
   boolean clear=true;
   for(int step=1;step<=4;step++){
    Vec3 point=mob.position().lerp(Vec3.atCenterOf(p),step/4.0D);
    if(!mob.level.noCollision(mob,mob.getBoundingBox().move(point.subtract(mob.position())))){clear=false;break;}
   }
   if(!clear)continue;
   var path=mob.getNavigation().createPath(p,0);
   if(path==null||!path.canReach()||mob.getSpeciesSlug().equals("gyarados")&&!surfacePathIsWater(path)){
    blocked.put(p.immutable(),mob.tickCount+200);continue;
   }
   if(!preferWalls)return p;
   double clearance=wallClearanceAlongPath(path,wallShapeCache);
   if(clearance>=WALL_PREFERENCE_DISTANCE)return p;
   if(clearance>bestClearance){bestClearance=clearance;bestDestination=p;}
  }
  return bestDestination;
 }
 private double wallClearanceAlongPath(net.minecraft.world.level.pathfinder.Path path,Map<BlockPos,List<net.minecraft.world.phys.AABB>> shapeCache){
  double clearance=Double.POSITIVE_INFINITY;
  double previousX=mob.getX(),previousY=mob.getY(),previousZ=mob.getZ();
  for(int i=0;i<path.getNodeCount();i++){
   var node=path.getNode(i);
   double endX=node.x+.5D,endY=node.y,endZ=node.z+.5D;
   double distance=Math.sqrt((endX-previousX)*(endX-previousX)+(endY-previousY)*(endY-previousY)+(endZ-previousZ)*(endZ-previousZ));
   int samples=Math.max(1,(int)Math.ceil(distance*2.0D));
   for(int sample=1;sample<=samples;sample++){
    double t=sample/(double)samples;
    double x=previousX+(endX-previousX)*t,y=previousY+(endY-previousY)*t,z=previousZ+(endZ-previousZ)*t;
    var box=mob.getBoundingBox().move(x-mob.getX(),y-mob.getY(),z-mob.getZ());
    clearance=Math.min(clearance,wallClearanceAt(box,shapeCache));
   }
   previousX=endX;previousY=endY;previousZ=endZ;
  }
  return Math.min(WALL_PREFERENCE_DISTANCE+1.0D,clearance);
 }
 private double wallClearanceAt(net.minecraft.world.phys.AABB body,Map<BlockPos,List<net.minecraft.world.phys.AABB>> shapeCache){
  int minX=net.minecraft.util.Mth.floor(body.minX-WALL_PREFERENCE_DISTANCE-1.0D);
  int maxX=net.minecraft.util.Mth.floor(body.maxX+WALL_PREFERENCE_DISTANCE+1.0D);
  int minY=net.minecraft.util.Mth.floor(body.minY);
  int maxY=net.minecraft.util.Mth.floor(body.maxY);
  int minZ=net.minecraft.util.Mth.floor(body.minZ-WALL_PREFERENCE_DISTANCE-1.0D);
  int maxZ=net.minecraft.util.Mth.floor(body.maxZ+WALL_PREFERENCE_DISTANCE+1.0D);
  var obstacles=new ArrayList<net.minecraft.world.phys.AABB>();
  for(int x=minX;x<=maxX;x++)for(int y=minY;y<=maxY;y++)for(int z=minZ;z<=maxZ;z++){
   BlockPos pos=new BlockPos(x,y,z);
   var boxes=shapeCache.computeIfAbsent(pos.immutable(),key->{
    var shape=mob.level.getBlockState(key).getCollisionShape(mob.level,key);
    if(shape.isEmpty())return List.of();
    var worldBoxes=new ArrayList<net.minecraft.world.phys.AABB>();
    for(var local:shape.toAabbs())worldBoxes.add(local.move(key.getX(),key.getY(),key.getZ()));
    return List.copyOf(worldBoxes);
   });
   for(var obstacle:boxes)
    if(body.maxY>obstacle.minY+1.0E-6D&&body.minY<obstacle.maxY-1.0E-6D)obstacles.add(obstacle);
  }
  return horizontalWallClearance(body,obstacles);
 }
 public static double horizontalWallClearance(net.minecraft.world.phys.AABB body,Iterable<net.minecraft.world.phys.AABB> obstacles){
  double nearest=Double.POSITIVE_INFINITY;
  for(var obstacle:obstacles){
   if(body.maxY<=obstacle.minY+1.0E-6D||body.minY>=obstacle.maxY-1.0E-6D)continue;
   double dx=Math.max(0.0D,Math.max(obstacle.minX-body.maxX,body.minX-obstacle.maxX));
   double dz=Math.max(0.0D,Math.max(obstacle.minZ-body.maxZ,body.minZ-obstacle.maxZ));
   nearest=Math.min(nearest,Math.sqrt(dx*dx+dz*dz));
  }
  return nearest;
 }
 private boolean surfacePathIsWater(net.minecraft.world.level.pathfinder.Path path){
  // Validate the whole route, including the space between path nodes. Wide
  // surface swimmers can clear each node yet clip a shallow corner while the
  // navigator interpolates toward the following node.
  // Include the horizontal gap from the live entity to the first path node.
  // Keep Y unset here: after waking at the lake floor, the first surface route
  // still targets its surface height while the entity is rising vertically.
  double routeTop=Double.NaN,previousX=mob.getX(),previousY=Double.NaN,previousZ=mob.getZ();
  for(int i=0;i<path.getNodeCount();i++){
   var node=path.getNode(i);
   double endX=node.x+.5D,endY=node.y,endZ=node.z+.5D;
   int samples=Double.isFinite(previousX)?4:1;
   for(int sample=1;sample<=samples;sample++){
    double t=sample/(double)samples;
    double x=Double.isFinite(previousX)?previousX+(endX-previousX)*t:endX;
    double navY=Double.isFinite(previousY)?previousY+(endY-previousY)*t:endY;
    double z=Double.isFinite(previousZ)?previousZ+(endZ-previousZ)*t:endZ;
    BlockPos column=new BlockPos(net.minecraft.util.Mth.floor(x),net.minecraft.util.Mth.floor(navY),net.minecraft.util.Mth.floor(z));
    double top=surfaceAt(mob,column);
    if(!Double.isFinite(top)||!waterRegion(mob,column))return false;
    if(!Double.isFinite(routeTop))routeTop=top;
    else if(Math.abs(top-routeTop)>.5D)return false;
    double feetY=top-preferredSurfaceDepth(mob);
    if(Math.abs(navY-feetY)>1.0D)return false;
    var box=mob.getBoundingBox().move(x-mob.getX(),feetY-mob.getY(),z-mob.getZ());
    if(!mob.level.noCollision(mob,box))return false;
    if(mob.getSpeciesSlug().equals("gyarados")){
     // Gyarados is modeled as an exclusively aquatic surface swimmer. Validate
     // its measured visible lower 10% at the waterline, not the hitbox center.
     double scale=SpeciesDefinitions.get("gyarados").scale()*(mob.isBaby()?0.5D:1.0D);
     double bodyBottom=ModelMetrics.bodyBottom("gyarados")*scale;
     double bodyHeight=ModelMetrics.bodyHeight("gyarados")*scale;
     double submergedFraction=(top-(feetY+bodyBottom))/bodyHeight;
     if(Math.abs(submergedFraction-0.10D)>1.0E-6D)return false;
     int waterY=net.minecraft.util.Mth.floor(top-1.0E-4D);
     for(int bx=net.minecraft.util.Mth.floor(box.minX+.001D);bx<=net.minecraft.util.Mth.floor(box.maxX-.001D);bx++)
      for(int bz=net.minecraft.util.Mth.floor(box.minZ+.001D);bz<=net.minecraft.util.Mth.floor(box.maxZ-.001D);bz++)
       if(!mob.level.getFluidState(new BlockPos(bx,waterY,bz)).is(FluidTags.WATER))return false;
    }else{
     int bodyY=net.minecraft.util.Mth.floor(feetY+mob.getBbHeight()*.5D);
     for(int bx=net.minecraft.util.Mth.floor(box.minX+.001D);bx<=net.minecraft.util.Mth.floor(box.maxX-.001D);bx++)
      for(int bz=net.minecraft.util.Mth.floor(box.minZ+.001D);bz<=net.minecraft.util.Mth.floor(box.maxZ-.001D);bz++)
       if(!mob.level.getFluidState(new BlockPos(bx,bodyY,bz)).is(FluidTags.WATER))return false;
    }
   }
   previousX=endX;previousY=endY;previousZ=endZ;
  }
  return path.getNodeCount()>0;
 }
 private BlockPos nearbyBottomDestination(){
  // Bottom-seeking species should settle locally before choosing a distant
  // patrol destination. Search farther when needed to find an open route, but
  // keep the best reachable fallback in narrow channels.
  BlockPos best=null;double bestClearance=-1.0D;
  for(int radius=0;radius<=8;radius++)for(int d=0;d<(radius==0?1:SWIM_DIRECTIONS.length);d++){
   int[] direction=SWIM_DIRECTIONS[d];
   int dx=direction[0]*radius,dz=direction[1]*radius;
   int x=mob.blockPosition().getX()+dx,z=mob.blockPosition().getZ()+dz;
   BlockPos column=new BlockPos(x,mob.getY(),z);
   double top=surfaceAt(mob,column);
   if(!Double.isFinite(top))continue;
   double y=preferredBottomY(mob,column,top);
   if(!Double.isFinite(y))continue;
   BlockPos p=new BlockPos(x,Math.ceil(y),z);
   if(blocked.containsKey(p)||!waterFits(mob,p,true)
       ||requiresNavigableVolume(mob.getSpeciesSlug())&&!waterRegion(mob,p))continue;
   boolean clear=true;
   for(int step=1;step<=4;step++){
    Vec3 point=mob.position().lerp(Vec3.atCenterOf(p),step/4.0D);
    if(!mob.level.noCollision(mob,mob.getBoundingBox().move(point.subtract(mob.position())))){clear=false;break;}
   }
   if(!clear)continue;
   var path=mob.getNavigation().createPath(p,0);
   if(path==null||!path.canReach()){blocked.put(p.immutable(),mob.tickCount+200);continue;}
   double clearance=SpeciesBehaviorProfiles.isAquatic(mob.getSpeciesSlug())?wallClearanceAlongPath(path,new HashMap<>()):Double.POSITIVE_INFINITY;
   if(clearance>=WALL_PREFERENCE_DISTANCE)return p;
   if(clearance>bestClearance){best=p;bestClearance=clearance;}
  }
  return best;
 }
 private static double preferredBottomY(MinemonCreature mob,BlockPos column,double surface){
  int floorWater=bottomWaterBlock(mob,column,surface);
  if(!safeWaterFloor(mob,column,floorWater))return Double.NaN;
  if(surface-floorWater<=mob.getBbHeight()+0.45D)return Double.NaN;
  // Keep a useful collision margin above the floor. A nearly zero clearance
  // can place the navigation node inside the support block after interpolation.
  double target=floorWater+0.30D;
  var box=mob.getBoundingBox().move(column.getX()+0.5D-mob.getX(),target-mob.getY(),column.getZ()+0.5D-mob.getZ());
  if(!mob.level.noCollision(mob,box))return Double.NaN;
  for(int x=(int)Math.floor(box.minX+0.001D);x<=Math.floor(box.maxX-0.001D);x++)
   for(int z=(int)Math.floor(box.minZ+0.001D);z<=Math.floor(box.maxZ-0.001D);z++)
    if(!mob.level.getFluidState(new BlockPos(x,floorWater,z)).is(FluidTags.WATER))return Double.NaN;
  return target;
 }
 public static double waterRestY(MinemonCreature mob){
  double top=restSurfaceAt(mob,mob.blockPosition());
  if(!Double.isFinite(top))return Double.NaN;
  if(mob.getSpeciesSlug().equals("lapras"))return top-preferredSurfaceDepth(mob);
  if(SpeciesBehaviorProfiles.restsAtWaterBottom(mob.getSpeciesSlug())){
   double target=preferredBottomY(mob,mob.blockPosition(),top);
   if(Double.isFinite(target))return target;
  }
  return Double.isFinite(mob.aquaticSleepY())?mob.aquaticSleepY():mob.getY();
 }
 static double shallowAquaticSleepY(double currentY,double surfaceY){
  return Double.isFinite(surfaceY)?Math.min(currentY,surfaceY-2.0D):currentY;
 }
 static boolean safeWaterRestAt(MinemonCreature mob,double y){
  var box=mob.getBoundingBox().move(0.0D,y-mob.getY(),0.0D);
  if(!mob.level.noCollision(mob,box))return false;
  double surface=restSurfaceAt(mob,mob.blockPosition());
  if(!Double.isFinite(surface))return false;
  int targetWater=(int)Math.floor(y),surfaceWater=(int)Math.floor(surface-1.0E-4D);
  for(int x=(int)Math.floor(box.minX+0.001D);x<=Math.floor(box.maxX-0.001D);x++)
   for(int z=(int)Math.floor(box.minZ+0.001D);z<=Math.floor(box.maxZ-0.001D);z++)
    if(!mob.level.getFluidState(new BlockPos(x,targetWater,z)).is(FluidTags.WATER)
        ||!mob.level.getFluidState(new BlockPos(x,surfaceWater,z)).is(FluidTags.WATER))return false;
  return true;
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
 static boolean safeLanding(MinemonCreature mob,BlockPos feet){
  if(!safeDry(mob,feet))return false;
  var box=mob.getBoundingBox().move(feet.getX()+.5D-mob.getX(),feet.getY()-mob.getY(),feet.getZ()+.5D-mob.getZ());
  return dryLandingBox(mob,box);
 }
 static boolean safeLandingBelow(MinemonCreature mob){
  var feet=new BlockPos(mob.getX(),mob.naturalSurface(),mob.getZ());
  return safeDry(mob,feet)&&dryLandingBox(mob,mob.getBoundingBox().move(0,feet.getY()-mob.getY(),0));
 }
 private static boolean dryLandingBox(MinemonCreature mob,net.minecraft.world.phys.AABB box){
  if(!mob.level.noCollision(mob,box))return false;
  for(int x=(int)Math.floor(box.minX+.001);x<=Math.floor(box.maxX-.001);x++)
   for(int y=(int)Math.floor(box.minY+.001);y<=Math.floor(box.maxY-.001);y++)
    for(int z=(int)Math.floor(box.minZ+.001);z<=Math.floor(box.maxZ-.001);z++)
     if(!mob.level.getFluidState(new BlockPos(x,y,z)).isEmpty())return false;
  return true;
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
  if(java.util.Set.of("blastoise","poliwrath","dewgong","vaporeon").contains(mob.getSpeciesSlug()))
   return 0.5D + mob.getBbHeight()*0.5D;
  if(mob.getSpeciesSlug().equals("gyarados")){
   // Keep the server anchor identical to the client renderer's 10%-submerged
   // measured-body anchor. This is a positioning target, not a hitbox depth.
   double scale=SpeciesDefinitions.get("gyarados").scale()*(mob.isBaby()?0.5D:1.0D);
   return (ModelMetrics.bodyBottom("gyarados")+ModelMetrics.bodyHeight("gyarados")*0.10D)*scale;
  }
  return Math.min(0.8D,mob.getBbHeight()*0.3D);
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
 public static double surfaceAt(MinemonCreature mob,BlockPos p){return surfaceAt(mob,p,32);}
 public static double restSurfaceAt(MinemonCreature mob,BlockPos p){return surfaceAt(mob,p,mob.level.getHeight());}
 private static double surfaceAt(MinemonCreature mob,BlockPos p,int maximumSteps){
  if(!mob.level.hasChunkAt(p))return Double.NaN;
  BlockPos q=p;
  // Surface followers may be fractionally above water after interpolation.
  if(!mob.level.getFluidState(q).is(FluidTags.WATER))q=q.below();
  if(!mob.level.getFluidState(q).is(FluidTags.WATER))return Double.NaN;
  for(int n=0;n<maximumSteps&&q.getY()<mob.level.getMaxBuildHeight()-1&&mob.level.getFluidState(q.above()).is(FluidTags.WATER);n++)q=q.above();
  return q.getY()+mob.level.getFluidState(q).getHeight(mob.level,q);
 }
}
