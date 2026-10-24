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
 @Override public void travel(net.minecraft.world.phys.Vec3 input){
  if(!this.level.isClientSide&&!this.isSleeping()){
   double support=NaturalMovementGoals.surfaceBelow(this.level,this.getX(),this.getY(),this.getZ());
   double clearance=this.getY()-support;
   double allowedDrop=Math.max(0.0D,clearance-0.6D);
   double vertical=Math.max(this.getDeltaMovement().y,-allowedDrop);
   if(clearance<0.6D)vertical=Math.max(vertical,0.08D);
   this.setDeltaMovement(this.getDeltaMovement().x,vertical,this.getDeltaMovement().z);
  }
  super.travel(input);
 }
 @Override public void tick(){
  super.tick();
  boolean sleeping=this.isSleeping();
  // Hover safely until a dry landing path is available; follow that path
  // instead of dropping blindly into water, lava, or an obstructed column.
  if(sleeping){
  // Gravity is synchronized by the server. The client has no active landing
  // navigation and must not override that decision during interpolation.
  if(this.level.isClientSide){this.fallDistance=0;return;}
  double support=NaturalMovementGoals.surfaceBelow(this.level,this.getX(),this.getY(),this.getZ());
  net.minecraft.core.BlockPos floor=new net.minecraft.core.BlockPos(this.getX(),support-0.001D,this.getZ());
  var state=this.level.getBlockState(floor);
  boolean safe=this.level.getFluidState(floor).isEmpty()
      && !state.isAir() && !state.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)
      && !state.is(net.minecraft.world.level.block.Blocks.CACTUS)
      && !state.is(net.minecraft.world.level.block.Blocks.CAMPFIRE)
      && !state.is(net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE);
  boolean settling=safe && this.getY()-support<=1.25D && this.getNavigation().isDone();
  this.setNoGravity(!sleeping || !this.isOnGround() && !settling);
  this.fallDistance=0;return;
  }
  this.setNoGravity(true);
  if(!this.level.isClientSide){
   double surface=NaturalMovementGoals.surfaceBelow(this.level,this.getX(),this.getY(),this.getZ());
   double altitude=this.getY()-surface;
   // Correct only excursions outside the natural band; normal navigation remains
   // responsible for smooth altitude changes and collision avoidance.
   boolean combat=this.getTarget()!=null&&this.getTarget().isAlive();
   if(combat&&altitude<1.25D){
    // Preserve native melee pursuit: small fliers must approach a ground
    // target without being pushed back into the normal patrol band.
    double lift=net.minecraft.util.Mth.clamp((1.25D-altitude)*0.1D,0.02D,0.10D);
    this.setDeltaMovement(this.getDeltaMovement().x,Math.max(this.getDeltaMovement().y,lift),this.getDeltaMovement().z);
   }else if(!combat&&(altitude<3.0D||altitude>15.0D)){
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
