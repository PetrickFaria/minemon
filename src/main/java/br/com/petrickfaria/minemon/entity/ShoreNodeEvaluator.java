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
  if(!Double.isFinite(top))return count;
  // Amphibious path nodes can sit at any depth. Plan the bank node from the
  // actual water surface and allow a swimmer to approach from the top two
  // water blocks; a fixed node.y + 2 can still land inside the bank wall.
  int waterline=(int)Math.ceil(top);
  if(node.y<waterline-2)return count;
  for(int[] direction:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
   // Include flush shores and slab/step landings, rather than only a bank
   // exactly one full block above the surface.
   for(int bankFeetY=waterline;bankFeetY<=waterline+1;bankFeetY++){
   var feet=new BlockPos(node.x+direction[0],bankFeetY,node.z+direction[1]);
   if(!WaterMovementGoal.safeDry(pokemon,feet))continue;
   if(count>=neighbors.length)break;
   Node next=getNode(feet.getX(),feet.getY(),feet.getZ());if(next.closed)continue;
   next.type=BlockPathTypes.WALKABLE;next.costMalus=Math.max(0,next.costMalus);neighbors[count++]=next;
   }
  }
  return count;
 }
}
