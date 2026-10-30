package br.com.petrickfaria.minemon.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.pathfinder.*;
import net.minecraft.world.phys.Vec3;

/** Land navigation retains amphibious shore nodes for accidental water entry. */
final class EscapeNavigation extends GroundPathNavigation {
 EscapeNavigation(Mob mob,Level level){super(mob,level);setCanFloat(true);}
 @Override protected PathFinder createPathFinder(int limit){
  this.nodeEvaluator=new ShoreNodeEvaluator();
  this.nodeEvaluator.setCanPassDoors(true);
  return new PathFinder(this.nodeEvaluator,limit);
 }
 @Override protected Vec3 getTempMobPos(){return mob.isInWater()?mob.position():super.getTempMobPos();}
 @Override protected double getGroundY(Vec3 p){return mob.isInWater()?p.y:super.getGroundY(p);}
}
