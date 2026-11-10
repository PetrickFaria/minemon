package br.com.petrickfaria.minemon.entity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
public final class CombatGeometry {
 public static double distanceSquared(Entity a,Entity b){var x=a.getBoundingBox();var y=b.getBoundingBox();double dx=Math.max(0,Math.max(x.minX-y.maxX,y.minX-x.maxX)),dy=Math.max(0,Math.max(x.minY-y.maxY,y.minY-x.maxY)),dz=Math.max(0,Math.max(x.minZ-y.maxZ,y.minZ-x.maxZ));return dx*dx+dy*dy+dz*dz;}
 public static boolean inRange(Entity a,Entity b,double range){return distanceSquared(a,b)<=range*range;}
 public static double centerDistanceSquared(Entity a,Entity b){return a.position().distanceToSqr(b.position());}
 /** Distance from a real origin (such as a projectile muzzle) to the nearest
  * point of a target's collision volume. This prevents target height and width
  * from consuming range before the shot reaches the entity. */
 public static double pointToBoxDistanceSquared(Vec3 origin, AABB box){
  double dx=Math.max(box.minX-origin.x,Math.max(0.0D,origin.x-box.maxX));
  double dy=Math.max(box.minY-origin.y,Math.max(0.0D,origin.y-box.maxY));
  double dz=Math.max(box.minZ-origin.z,Math.max(0.0D,origin.z-box.maxZ));
  return dx*dx+dy*dy+dz*dz;
 }
 public static double horizontalCenterDistanceSquared(Entity a,Entity b){
  double dx=a.getX()-b.getX(),dz=a.getZ()-b.getZ();
  return dx*dx+dz*dz;
 }
 private CombatGeometry(){}
}
