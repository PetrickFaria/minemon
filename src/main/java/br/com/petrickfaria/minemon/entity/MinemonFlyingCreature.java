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
 @Override public void tick(){
  super.tick();
  boolean sleeping=this.isSleeping();
  this.setNoGravity(!sleeping);
  if(sleeping){this.fallDistance=0;return;}
  if(!this.level.isClientSide){
   int surface=this.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
       net.minecraft.util.Mth.floor(this.getX()),net.minecraft.util.Mth.floor(this.getZ()));
   double altitude=this.getY()-surface;
   // Correct only excursions outside the natural band; normal navigation remains
   // responsible for smooth altitude changes and collision avoidance.
   if(altitude<3.0D||altitude>15.0D){
    double desired=surface+(altitude<3.0D?7.0D:11.0D);
    double correction=net.minecraft.util.Mth.clamp((desired-this.getY())*0.035D,-0.12D,0.12D);
    this.setDeltaMovement(this.getDeltaMovement().x,
        net.minecraft.util.Mth.clamp(this.getDeltaMovement().y+correction,-0.18D,0.18D),this.getDeltaMovement().z);
   }
  }
  this.fallDistance=0;
 }
 @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
}
