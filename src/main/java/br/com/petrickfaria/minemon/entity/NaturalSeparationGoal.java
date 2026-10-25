package br.com.petrickfaria.minemon.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/** Soft, path-aware spacing for natural movement; combat, sleep and sit retain priority. */
final class NaturalSeparationGoal extends Goal {
 private final MinemonCreature mob;
 private MinemonCreature neighbor;
 private BlockPos destination;
 private int nextCheck,stuckTicks;

 NaturalSeparationGoal(MinemonCreature mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE));}

 private double preferredDistance(MinemonCreature other){
  return Math.max(3.0D,(mob.getBbWidth()+other.getBbWidth())*0.5D+0.5D);
 }

 @Override public boolean canUse(){
  if(mob.tickCount<nextCheck||!mob.naturalBehaviorActive()||mob.getTarget()!=null)return false;
  neighbor=mob.level.getEntitiesOfClass(MinemonCreature.class,mob.getBoundingBox().inflate(4.0D),
      e->e!=mob&&e.isAlive()).stream().filter(e->mob.distanceToSqr(e)<preferredDistance(e)*preferredDistance(e))
      .min(java.util.Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
  if(neighbor==null)return false;
  Vec3 away=mob.position().subtract(neighbor.position());
  if(away.lengthSqr()<1.0E-5D)away=new Vec3((mob.getId()&1)==0?1.0D:-1.0D,0,0);
  away=away.normalize();
  for(int i=0;i<8;i++){
   double angle=Math.PI*2.0D*i/8.0D;
   Vec3 dir=i==0?away:new Vec3(away.x*Math.cos(angle)-away.z*Math.sin(angle),0,away.x*Math.sin(angle)+away.z*Math.cos(angle));
   BlockPos p=new BlockPos(net.minecraft.util.Mth.floor(mob.getX()+dir.x*4.5D),
       net.minecraft.util.Mth.floor(mob.getY()),net.minecraft.util.Mth.floor(mob.getZ()+dir.z*4.5D));
   var path=mob.getNavigation().createPath(p,0);
   if(path!=null&&path.canReach()){destination=p;return true;}
  }
  nextCheck=mob.tickCount+60;return false;
 }

 @Override public boolean canContinueToUse(){
  return destination!=null&&mob.naturalBehaviorActive()&&!mob.getNavigation().isDone()
      &&neighbor!=null&&mob.distanceToSqr(neighbor)<preferredDistance(neighbor)*preferredDistance(neighbor)*1.12D
      &&stuckTicks<30;
 }

 @Override public void start(){
  var path=mob.getNavigation().createPath(destination,0);
  if(path==null||!path.canReach()){destination=null;nextCheck=mob.tickCount+60;return;}
  mob.getNavigation().moveTo(path,1.0D);stuckTicks=0;
 }

 @Override public void tick(){
  if(mob.getDeltaMovement().horizontalDistanceSqr()<1.0E-5D)stuckTicks++;
 }

 @Override public void stop(){
  if(destination!=null&&!mob.getNavigation().isDone())mob.getNavigation().stop();
  destination=null;neighbor=null;nextCheck=mob.tickCount+40;
 }
}
