package br.com.petrickfaria.minemon.entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
public final class MinemonFlyingCreature extends MinemonCreature {
 private final PathNavigation airNavigation;
 private final PathNavigation groundNavigation;
 private final PathNavigation escapeNavigation;
 private final net.minecraft.world.entity.ai.control.MoveControl airControl;
 private final net.minecraft.world.entity.ai.control.MoveControl groundControl;
 public MinemonFlyingCreature(EntityType<? extends TamableAnimal> type,Level level){
  super(type,level);this.airNavigation=this.navigation;
  this.groundNavigation=new net.minecraft.world.entity.ai.navigation.GroundPathNavigation(this,level);
  this.escapeNavigation=new EscapeNavigation(this,level);
  this.airControl=new FlyingMoveControl(this,20,true);
  this.groundControl=new WaterAwareMoveControl(this);
  this.updateLocomotion();
 }
 private void updateLocomotion(){
  boolean flying=this.isFlyingSpecies()&&!this.isInWater();PathNavigation desired=this.isInWater()?this.escapeNavigation:flying?this.airNavigation:this.groundNavigation;
  if(this.navigation!=desired){this.navigation.stop();this.navigation=desired;}
  this.moveControl=flying?this.airControl:this.groundControl;
  if(!this.level.isClientSide||!this.wantsNaturalLanding())this.setNoGravity(flying||this.isInWater());
 }
 @Override protected PathNavigation createNavigation(Level level){FlyingPathNavigation n=new FlyingPathNavigation(this,level);n.setCanOpenDoors(false);n.setCanFloat(true);n.setCanPassDoors(true);return n;}
 @Override public void travel(net.minecraft.world.phys.Vec3 input){
  if(this.isFlyingSpecies()&&!this.level.isClientSide&&!this.wantsNaturalLanding()){
   double support=this.naturalSurface();
   double clearance=this.getY()-support;
   boolean combat=this.getTarget()!=null&&this.getTarget().isAlive();
   double floor=combat?0.15D:Math.max(0.15D,this.naturalCycle().minimumAltitude());
   double allowedDrop=Math.max(0.0D,clearance-floor);
   double vertical=Math.max(this.getDeltaMovement().y,-allowedDrop);
   if(clearance<floor)vertical=Math.max(vertical,0.06D);
   this.setDeltaMovement(this.getDeltaMovement().x,vertical,this.getDeltaMovement().z);
  }
  super.travel(input);
  // FlyingMoveControl can replace the pre-travel lift with a downward input
  // while an entity is waking from a landed sleep pose. Reassert the active
  // cycle's floor after travel so the entity actually resumes its flight band.
  if(!this.level.isClientSide&&this.isFlyingSpecies()&&!this.wantsNaturalLanding()&&this.naturalBehaviorActive()
      &&this.getTarget()==null&&(this.naturalCycle().phase()==BehaviorCycle.Phase.AIR
          ||this.naturalCycle().phase()==BehaviorCycle.Phase.TAKEOFF)){
   double minimum=this.naturalCycle().minimumAltitude();
   double altitude=this.getY()-this.naturalSurface();
   if(altitude<minimum){
    double lift=net.minecraft.util.Mth.clamp((minimum-altitude)*0.1D,0.04D,0.12D);
    // Cancel a stale ground-level FlyingMoveControl destination left behind
    // by SleepLanding. The controller otherwise keeps steering back down and
    // wins against a small upward velocity on every tick.
    this.getMoveControl().setWantedPosition(this.getX(),this.naturalSurface()+minimum+0.25D,
        this.getZ(),Math.max(0.25D,this.getMoveControl().getSpeed()));
    // Move through the normal collision resolver in small steps. A navigation
    // goal can keep writing a ground-level vertical input while waking, so a
    // velocity-only correction gets overwritten before it changes position.
    this.move(net.minecraft.world.entity.MoverType.SELF,new net.minecraft.world.phys.Vec3(0.0D,lift,0.0D));
   }
  }
 }
 @Override public void tick(){
  super.tick();
  this.updateLocomotion();
  if(!this.isFlyingSpecies()){return;}
  boolean sleeping=this.wantsNaturalLanding();
  // Hover safely until a dry landing path is available; follow that path
  // instead of dropping blindly into water, lava, or an obstructed column.
  if(sleeping){
  // Gravity is synchronized by the server. The client has no active landing
  // navigation and must not override that decision during interpolation.
  if(this.level.isClientSide){this.fallDistance=0;return;}
  double support=this.naturalSurface();
  net.minecraft.core.BlockPos feet=new net.minecraft.core.BlockPos(this.getX(),support,this.getZ());
  boolean safe=WaterMovementGoal.safeDry(this,feet);
  // Once a collision-safe landing column is found, gravity performs the
  // gradual descent. If the column is unsafe, keep hovering while the rest
  // goal searches for a nearby dry landing instead of dropping blindly.
  this.setNoGravity(!safe && !this.isOnGround());
  this.fallDistance=0;return;
  }
  this.setNoGravity(true);
  if(!this.level.isClientSide){
   double surface=this.naturalSurface();
   double altitude=this.getY()-surface;
   // Correct only excursions outside the natural band; normal navigation remains
   // responsible for smooth altitude changes and collision avoidance.
   boolean combat=this.getTarget()!=null&&this.getTarget().isAlive();
   if(combat&&altitude<0.6D){
    // Preserve native melee pursuit: small fliers must approach a ground
    // target without being pushed back into the normal patrol band.
    // Match the travel safety floor: a 1.25-block lift plus the normal
    // navigation stopping distance keeps Pidgey outside vanilla melee reach.
    double lift=net.minecraft.util.Mth.clamp((0.6D-altitude)*0.1D,0.02D,0.10D);
    this.setDeltaMovement(this.getDeltaMovement().x,Math.max(this.getDeltaMovement().y,lift),this.getDeltaMovement().z);
   }else if(!combat&&this.naturalBehaviorActive()&&(altitude<this.naturalCycle().minimumAltitude()||altitude>this.naturalCycle().maximumAltitude())){
    double desired=surface+(this.naturalCycle().minimumAltitude()+this.naturalCycle().maximumAltitude())*0.5D;
    double correction=net.minecraft.util.Mth.clamp((desired-this.getY())*0.025D-this.getDeltaMovement().y*0.15D,-0.08D,0.08D);
    this.setDeltaMovement(this.getDeltaMovement().x,
        net.minecraft.util.Mth.clamp(this.getDeltaMovement().y+correction,-0.12D,0.12D),this.getDeltaMovement().z);
   }

  }
  this.fallDistance=0;
 }
 @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return this.isFlyingSpecies()||this.wantsNaturalLanding()?false:super.causeFallDamage(distance,multiplier,source);}
}
