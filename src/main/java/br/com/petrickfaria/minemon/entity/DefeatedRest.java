package br.com.petrickfaria.minemon.entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.phys.Vec3;
/** Bounded rescue descent of the original body; every step uses Minecraft collisions. */
final class DefeatedRest {
 private final MinemonCreature mob;
 private FlyingPathNavigation navigation;
 private net.minecraft.world.level.pathfinder.Path path;
 private BlockPos site;
 private int retry,stalled;
 private Vec3 previous;
 private final java.util.Map<BlockPos,Integer> rejected=new java.util.HashMap<>();
 DefeatedRest(MinemonCreature mob){this.mob=mob;}
 void tick(){
  mob.setNoGravity(true);mob.fallDistance=0;mob.setDeltaMovement(Vec3.ZERO);
  if(WaterMovementGoal.safeLandingBelow(mob)){
   path=null;double floor=mob.naturalSurface();double dy=Math.min(.08,Math.max(.02,mob.getY()-floor));
   if(dy>0)mob.move(MoverType.SELF,new Vec3(0,-dy,0));return;
  }
  if(navigation==null){navigation=new FlyingPathNavigation(mob,mob.level);navigation.setCanFloat(true);navigation.setCanPassDoors(true);}
  rejected.entrySet().removeIf(e->e.getValue()<mob.tickCount);
  if(path==null||path.isDone()){
   if(retry-->0)return;retry=40;
   var origin=mob.blockPosition();
   search:for(int r=1;r<=12;r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
    if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;
    for(int dy=4;dy>=-32;dy--){
     var feet=origin.offset(dx,dy,dz);if(!mob.level.hasChunkAt(feet)||rejected.containsKey(feet)||!WaterMovementGoal.safeLanding(mob,feet))continue;
     var approach=new BlockPos(feet.getX(),Math.max(feet.getY()+1,mob.getY()),feet.getZ());
     var candidate=navigation.createPath(approach,0);
     if(candidate!=null&&candidate.canReach()){path=candidate;site=feet;previous=mob.position();stalled=0;break search;}
    }
   }
  }
  if(path==null||path.isDone())return;
  Vec3 target=path.getNextEntityPos(mob);Vec3 delta=target.subtract(mob.position());
  if(delta.length()<.2){path.advance();return;}
  mob.move(MoverType.SELF,delta.normalize().scale(Math.min(.08,delta.length())));
  if(previous!=null&&mob.position().distanceToSqr(previous)<.0001)stalled++;else stalled=0;
  previous=mob.position();
  if(stalled>=40){rejected.put(site,mob.tickCount+200);path=null;retry=40;}
 }
}
