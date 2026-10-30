package br.com.petrickfaria.minemon.entity;
import net.minecraft.world.entity.Entity;
public final class CombatGeometry {
 public static double distanceSquared(Entity a,Entity b){var x=a.getBoundingBox();var y=b.getBoundingBox();double dx=Math.max(0,Math.max(x.minX-y.maxX,y.minX-x.maxX)),dy=Math.max(0,Math.max(x.minY-y.maxY,y.minY-x.maxY)),dz=Math.max(0,Math.max(x.minZ-y.maxZ,y.minZ-x.maxZ));return dx*dx+dy*dy+dz*dz;}
 public static boolean inRange(Entity a,Entity b,double range){return distanceSquared(a,b)<=range*range;}
 private CombatGeometry(){}
}
