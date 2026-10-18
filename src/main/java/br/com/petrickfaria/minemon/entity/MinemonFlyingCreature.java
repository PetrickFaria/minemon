package br.com.petrickfaria.minemon.entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
public final class MinemonFlyingCreature extends MinemonCreature {
 public MinemonFlyingCreature(EntityType<? extends TamableAnimal> type,Level level){super(type,level);this.moveControl=new FlyingMoveControl(this,20,true);this.setNoGravity(true);}
 @Override public boolean isFlyingSpecies(){return true;}
 @Override protected PathNavigation createNavigation(Level level){FlyingPathNavigation n=new FlyingPathNavigation(this,level);n.setCanOpenDoors(false);n.setCanFloat(true);n.setCanPassDoors(true);return n;}
 @Override public void tick(){super.tick();this.setNoGravity(true);this.fallDistance=0;}
 @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
}
