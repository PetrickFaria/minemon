package br.com.petrickfaria.minemon.entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.*;
/** A swimmer's feet are below the surface: a one-block bank can be two nodes higher. */
final class ShoreNodeEvaluator extends AmphibiousNodeEvaluator {
 ShoreNodeEvaluator(){super(false);}
 @Override public int getNeighbors(Node[] neighbors,Node node){
  int count=super.getNeighbors(neighbors,node);
  if(!(mob instanceof MinemonCreature pokemon)||!pokemon.isInWater()||node.type!=BlockPathTypes.WATER)return count;
  double top=WaterMovementGoal.surfaceAt(pokemon,new BlockPos(node.x,node.y,node.z));
  if(!Double.isFinite(top)||node.y+2>top+1.05)return count;
  for(int[] direction:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
   var feet=new BlockPos(node.x+direction[0],node.y+2,node.z+direction[1]);
   if(!WaterMovementGoal.safeDry(pokemon,feet))continue;
   var overhead=pokemon.getBoundingBox().move(node.x+.5-pokemon.getX(),node.y+1-pokemon.getY(),node.z+.5-pokemon.getZ());
   if(!pokemon.level.noCollision(pokemon,overhead))continue;
   Node next=getNode(feet.getX(),feet.getY(),feet.getZ());if(next.closed)continue;
   next.type=BlockPathTypes.WALKABLE;next.costMalus=Math.max(0,next.costMalus);neighbors[count++]=next;
  }
  return count;
 }
}
