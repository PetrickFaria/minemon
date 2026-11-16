package br.com.petrickfaria.minemon.entity;

import java.util.List;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.AmphibiousNodeEvaluator;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Native amphibious routes that account for living bodies during Gyarados' night descent. */
final class GyaradosRestNodeEvaluator extends AmphibiousNodeEvaluator {
 private List<AABB> obstacles=List.of();
 GyaradosRestNodeEvaluator(){super(false);}
 @Override public void prepare(PathNavigationRegion region,Mob mob){
  super.prepare(region,mob);obstacles=List.of();
  if(mob instanceof MinemonCreature pokemon&&pokemon.getSpeciesSlug().equals("gyarados")&&pokemon.isNightResting())
   obstacles=mob.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,mob.getBoundingBox().inflate(Math.max(48,mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE))),
     other->other!=mob&&other.isAlive()&&!other.isSpectator()&&!other.noPhysics).stream().map(other->other.getBoundingBox().inflate(.02D)).toList();
 }
 @Override public void done(){super.done();obstacles=List.of();}
 @Override public int getNeighbors(Node[] neighbors,Node node){
  int count=super.getNeighbors(neighbors,node);
  if(obstacles.isEmpty())return count;
  int accepted=0;Vec3 from=position(node);
  for(int i=0;i<count;i++)if(clear(from,position(neighbors[i])))neighbors[accepted++]=neighbors[i];
  return accepted;
 }
 private Vec3 position(Node node){
  double offset=(int)(mob.getBbWidth()+1.0F)*.5D;
  return new Vec3(node.x+offset,node.y,node.z+offset);
 }
 private boolean clear(Vec3 from,Vec3 to){
  int steps=Math.max(1,(int)Math.ceil(from.distanceTo(to)/.5D));
  for(int i=1;i<=steps;i++){
   AABB box=mob.getBoundingBox().move(from.lerp(to,i/(double)steps).subtract(mob.position()));
   for(AABB obstacle:obstacles)if(box.intersects(obstacle))return false;
  }
  return true;
 }
}
