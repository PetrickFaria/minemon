package br.com.petrickfaria.minemon.entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.core.BlockPos;
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
 private int flightRecoveryAttempts;
 private int flightRecoveryMoves;
 private int recoveryTickSamples;
 private int recoveryAirPhaseSamples;
 private int recoveryNaturalActiveSamples;
 private int recoveryAwakeSamples;
 private int recoveryTargetFreeSamples;
 private int recoveryNotLandingSamples;
 private int recoveryBelowMinimumSamples;
 private boolean fixedFloater(){
  return java.util.Set.of("abra","kakuna","metapod").contains(this.getSpeciesSlug());
 }
 private boolean shouldRecoverNaturalFlight(){
  return !this.fixedFloater()&&!this.level.isClientSide&&this.isFlyingSpecies()&&!this.isSleeping()&&!this.isNightResting()
      &&!this.naturalCycle().landing()&&!this.isOrderedToSit()&&!this.isPassenger()&&!this.isNoAi()
      &&!this.isDeadOrDying()&&this.getTarget()==null;
 }
 public MinemonFlyingCreature(EntityType<? extends TamableAnimal> type,Level level){
  super(type,level);this.airNavigation=this.navigation;
  this.groundNavigation=new net.minecraft.world.entity.ai.navigation.GroundPathNavigation(this,level);
  this.escapeNavigation=new EscapeNavigation(this,level);
  this.airControl=new FlyingMoveControl(this,20,true);
  this.groundControl=new WaterAwareMoveControl(this);
  this.updateLocomotion();
 }
 private void updateLocomotion(){
  boolean landing=this.wantsNaturalLanding() && (this.isNightResting() || this.isSleeping());
  var feet=new BlockPos(this.getX(),this.naturalSurface(),this.getZ());
  boolean dry=WaterMovementGoal.safeLandingBelow(this);
  boolean flying=landing?!dry:this.isFlyingSpecies()&&!this.isInWater();
  PathNavigation desired=flying?this.airNavigation:this.isInWater()&&!landing?this.escapeNavigation:this.groundNavigation;
  if(this.navigation!=desired){this.navigation.stop();this.navigation=desired;}
  this.moveControl=flying?this.airControl:this.groundControl;
  if(!this.level.isClientSide||!this.wantsNaturalLanding())this.setNoGravity(flying||this.isInWater());
 }
 @Override protected PathNavigation createNavigation(Level level){FlyingPathNavigation n=new FlyingPathNavigation(this,level);n.setCanOpenDoors(false);n.setCanFloat(true);n.setCanPassDoors(true);return n;}
 @Override public void travel(net.minecraft.world.phys.Vec3 input){
  if(!this.fixedFloater()&&this.isFlyingSpecies()&&!this.level.isClientSide&&!this.wantsNaturalLanding()){
   double support=this.naturalSurface();
   double clearance=this.getY()-support;
   boolean combat=this.getTarget()!=null&&this.getTarget().isAlive();
   if(!combat&&this.shouldRecoverNaturalFlight()
       &&(this.naturalCycle().phase()==BehaviorCycle.Phase.AIR||this.naturalCycle().phase()==BehaviorCycle.Phase.TAKEOFF)){
    // Patrol paths can end on the minimum-altitude block while the waking
    // creature is still below the band. Let the recovery lift own movement
    // until the full flight band is reached instead of letting the path
    // controller keep steering toward that low endpoint.
    if(clearance<this.naturalCycle().minimumAltitude())this.getNavigation().stop();
   }
   double floor=combat?0.15D:Math.max(0.15D,this.naturalCycle().minimumAltitude());
   double allowedDrop=Math.max(0.0D,clearance-floor);
   double vertical=Math.max(this.getDeltaMovement().y,-allowedDrop);
   if(clearance<floor)vertical=Math.max(vertical,0.06D);
   this.setDeltaMovement(this.getDeltaMovement().x,vertical,this.getDeltaMovement().z);
  }
  boolean fixed=this.fixedFloater()&&!this.wantsNaturalLanding()&&!this.isSleeping()
      &&!this.isOrderedToSit()&&this.getTarget()==null&&!this.isInWater();
  if(fixed){
   double error=this.naturalSurface()+this.naturalCycle().minimumAltitude()-this.getY();
   this.setYya(0);input=new net.minecraft.world.phys.Vec3(input.x,0,input.z);
   this.setDeltaMovement(this.getDeltaMovement().x,net.minecraft.util.Mth.clamp(error*.25D,-.08D,.08D),this.getDeltaMovement().z);
  }
  super.travel(input);
  // FlyingMoveControl can replace the pre-travel lift with a downward input
  // while an entity is waking from a landed sleep pose. Reassert the active
  // cycle's floor after travel so the entity actually resumes its flight band.
  if(this.shouldRecoverNaturalFlight()&&(this.naturalCycle().phase()==BehaviorCycle.Phase.AIR
          ||this.naturalCycle().phase()==BehaviorCycle.Phase.TAKEOFF)){
   double minimum=this.naturalCycle().minimumAltitude();
   double altitude=this.getY()-this.naturalSurface();
   if(altitude<minimum){
    double lift=net.minecraft.util.Mth.clamp((minimum-altitude)*0.1D,0.04D,0.12D);
    // Cancel a stale ground-level FlyingMoveControl destination left behind
    // by SleepLanding. The controller otherwise keeps steering back down and
    // wins against a small upward velocity on every tick.
    this.getMoveControl().setWantedPosition(this.getX(),this.naturalSurface()+minimum+0.25D,
        this.getZ(),0.5D);
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
  if(!this.isFlyingSpecies() && !this.wantsNaturalLanding()){return;}
  if(!this.level.isClientSide){
   this.recoveryTickSamples++;
   if(this.naturalCycle().phase()==BehaviorCycle.Phase.AIR||this.naturalCycle().phase()==BehaviorCycle.Phase.TAKEOFF)this.recoveryAirPhaseSamples++;
   if(this.naturalBehaviorActive())this.recoveryNaturalActiveSamples++;
   if(!this.isSleeping()&&!this.isNightResting())this.recoveryAwakeSamples++;
   if(this.getTarget()==null)this.recoveryTargetFreeSamples++;
   if(!this.naturalCycle().landing())this.recoveryNotLandingSamples++;
   if(this.getY()-this.naturalSurface()<this.naturalCycle().minimumAltitude())this.recoveryBelowMinimumSamples++;
  }
  boolean sleeping=this.isSleeping()||this.isNightResting()||this.naturalCycle().landing();
  // Hover safely until a dry landing path is available; follow that path
  // instead of dropping blindly into water, lava, or an obstructed column.
  if(sleeping){
  // Gravity is synchronized by the server. The client has no active landing
  // navigation and must not override that decision during interpolation.
  if(this.level.isClientSide){this.fallDistance=0;return;}
  double support=this.naturalSurface();
  net.minecraft.core.BlockPos feet=new net.minecraft.core.BlockPos(this.getX(),support,this.getZ());
  boolean safe=WaterMovementGoal.safeLandingBelow(this);
  // Keep hovering while the column is unsafe, even if the entity touched the
  // bottom of water. The rest goal can then move it toward a dry landing.
  // Gravity starts the gradual descent only inside a collision-safe column.
  this.setNoGravity(!safe);
  if(!safe && this.getNavigation().isDone())
   this.setDeltaMovement(this.getDeltaMovement().x*.6D,0,this.getDeltaMovement().z*.6D);
  // Ground navigation can finish while a small fixed-height floater is still
  // fractionally above its support. Keep the sleep landing state authoritative
  // and carry it through the final descent instead of letting the move control
  // hold it at the edge of its altitude band.
  if(safe&&this.wantsNaturalLanding()&&!this.isOnGround())
   this.setDeltaMovement(this.getDeltaMovement().x,Math.min(this.getDeltaMovement().y,-0.08D),this.getDeltaMovement().z);
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
   }else if(!this.fixedFloater()&&!combat&&this.naturalBehaviorActive()&&(altitude<this.naturalCycle().minimumAltitude()||altitude>this.naturalCycle().maximumAltitude())){
    double desired=surface+(this.naturalCycle().minimumAltitude()+this.naturalCycle().maximumAltitude())*0.5D;
    double correction=net.minecraft.util.Mth.clamp((desired-this.getY())*0.025D-this.getDeltaMovement().y*0.15D,-0.08D,0.08D);
    this.setDeltaMovement(this.getDeltaMovement().x,
        net.minecraft.util.Mth.clamp(this.getDeltaMovement().y+correction,-0.12D,0.12D),this.getDeltaMovement().z);
   }
   if(!combat&&this.shouldRecoverNaturalFlight()
       &&(this.naturalCycle().phase()==BehaviorCycle.Phase.AIR||this.naturalCycle().phase()==BehaviorCycle.Phase.TAKEOFF)
       &&altitude<this.naturalCycle().minimumAltitude()){
    double lift=net.minecraft.util.Mth.clamp((this.naturalCycle().minimumAltitude()-altitude)*0.1D,0.04D,0.12D);
    // Apply the small takeoff correction after all goals have ticked. Keep it
    // collision checked and capped at 0.12 blocks per tick so it stays smooth.
    this.flightRecoveryAttempts++;
    var liftedBox=this.getBoundingBox().move(0.0D,lift,0.0D);
    if(this.level.noCollision(this,liftedBox)){
     this.setPos(this.getX(),this.getY()+lift,this.getZ());
     this.flightRecoveryMoves++;
    }
   }

  }
  this.fallDistance=0;
 }
 public int flightRecoveryAttemptsForValidation(){return this.flightRecoveryAttempts;}
 public int flightRecoveryMovesForValidation(){return this.flightRecoveryMoves;}
 public String flightRecoverySamplesForValidation(){return this.recoveryTickSamples+"/"+this.recoveryAirPhaseSamples+"/"+this.recoveryNaturalActiveSamples+"/"+this.recoveryAwakeSamples+"/"+this.recoveryTargetFreeSamples+"/"+this.recoveryNotLandingSamples+"/"+this.recoveryBelowMinimumSamples;}
 @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return this.isFlyingSpecies()||this.wantsNaturalLanding()?false:super.causeFallDamage(distance,multiplier,source);}
}
