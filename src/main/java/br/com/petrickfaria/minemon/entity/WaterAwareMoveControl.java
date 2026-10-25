package br.com.petrickfaria.minemon.entity;

import net.minecraft.world.entity.ai.control.*;

/** Ordinary walking outside water, smooth pitch/yaw steering while escaping. */
final class WaterAwareMoveControl extends SmoothSwimmingMoveControl {
 private final MoveControl land;
 private final MinemonCreature mob;
 WaterAwareMoveControl(MinemonCreature mob){super(mob,85,10,0.1F,1.0F,false);this.mob=mob;this.land=new MoveControl(mob);}
 @Override public void setWantedPosition(double x,double y,double z,double speed){super.setWantedPosition(x,y,z,speed);land.setWantedPosition(x,y,z,speed);}
 @Override public boolean hasWanted(){return mob.isInWater()?super.hasWanted():land.hasWanted();}
 @Override public void strafe(float forward,float sideways){super.strafe(forward,sideways);land.strafe(forward,sideways);}
 @Override public void tick(){if(mob.isInWater())super.tick();else land.tick();}
}
