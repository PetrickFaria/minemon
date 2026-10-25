package br.com.petrickfaria.minemon.entity;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

/** Closest reachable dry shore first; failed destinations cool down instead of wall loops. */
final class WaterEscapeGoal extends Goal {
 private final MinemonCreature mob;
 private Path path;
 private BlockPos destination;
 private final Map<BlockPos,Integer> blocked=new HashMap<>();
 private net.minecraft.world.phys.Vec3 previous;
 private int retry,stalled,cursor;
 private static final List<BlockPos> OFFSETS=offsets();
 private static List<BlockPos> offsets(){
  var out=new ArrayList<BlockPos>();
  for(int x=-48;x<=48;x++)for(int z=-48;z<=48;z++)if(x*x+z*z<=48*48)out.add(new BlockPos(x,0,z));
  out.sort(Comparator.comparingDouble(p->p.distSqr(BlockPos.ZERO)));return List.copyOf(out);
 }
 WaterEscapeGoal(MinemonCreature mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.JUMP));}
 @Override public boolean canUse(){
  if(mob.isAquaticSpecies()||!mob.isInWater()||mob.isNoAi()||mob.isPassenger())return false;
  if(retry-->0)return false;
  blocked.entrySet().removeIf(e->e.getValue()<mob.tickCount);
  mob.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WATER,0);
  int tried=0;
  for(int n=0;n<256;n++){
   var offset=OFFSETS.get(cursor++%OFFSETS.size());
   var column=mob.blockPosition().offset(offset.getX(),0,offset.getZ());
   if(!mob.level.hasChunkAt(column))continue;
   int top=mob.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,column.getX(),column.getZ());
   var feet=new BlockPos(column.getX(),top,column.getZ());
   if(blocked.containsKey(feet)||!WaterMovementGoal.safeDry(mob,feet))continue;
   path=mob.getNavigation().createPath(feet,0);tried++;
   if(path!=null&&path.canReach()){destination=feet;return true;}
   blocked.put(feet,mob.tickCount+200);
   if(tried>=2)break;
  }
  retry=4;return false;
 }
 @Override public boolean canContinueToUse(){return mob.isInWater()&&path!=null&&!mob.getNavigation().isDone()&&stalled<30;}
 @Override public void start(){mob.setSleeping(false);mob.getNavigation().moveTo(path,1.0D);previous=mob.position();stalled=0;}
 @Override public void tick(){
  if(previous!=null&&mob.position().distanceToSqr(previous)<0.0001D)stalled++;else stalled=0;
  previous=mob.position();
  if(mob.horizontalCollision)mob.setJumping(true);
 }
 @Override public void stop(){
  if(destination!=null&&stalled>=30)blocked.put(destination,mob.tickCount+200);
  mob.getNavigation().stop();mob.setJumping(false);path=null;retry=4;cursor=0;
  mob.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WATER,8);
 }
}
