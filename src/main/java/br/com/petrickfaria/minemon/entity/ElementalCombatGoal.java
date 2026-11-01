package br.com.petrickfaria.minemon.entity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
/** Pokémon use range-only combat and hold a five-block firing distance. */
final class ElementalCombatGoal extends MeleeAttackGoal {
 private final MinemonCreature pokemon;
 ElementalCombatGoal(MinemonCreature pokemon){super(pokemon,1.2D,true);this.pokemon=pokemon;}
 @Override public boolean canUse(){var target=pokemon.getTarget();return !pokemon.isDefeated() && target!=null && pokemon.projectileReady() && pokemon.canAttack(target);}
 @Override public void tick(){
  var target=pokemon.getTarget();
  if(target==null||!pokemon.canAttack(target)){pokemon.setTarget(null);pokemon.getNavigation().stop();return;}
  pokemon.getLookControl().setLookAt(target,30,30);
  double distance=pokemon.distanceTo(target);
  if(distance<4.5D){
   var away=pokemon.position().subtract(target.position()).multiply(1,0,1);
   if(away.lengthSqr()<1.0E-6D)away=new net.minecraft.world.phys.Vec3(1,0,0);
   var retreat= pokemon.position().add(away.normalize().scale(5.5D));
   pokemon.getNavigation().moveTo(retreat.x,retreat.y,retreat.z,1.2D);
  }else if(distance>6.5D){pokemon.getNavigation().moveTo(target,1.2D);}
  else pokemon.getNavigation().stop();
  if(pokemon.canFireAt(target))pokemon.fireElementalProjectile(target);
 }
 @Override protected void checkAndPerformAttack(net.minecraft.world.entity.LivingEntity target,double distance){ }
 @Override protected int getAttackInterval(){return pokemon.projectileCooldownTicks();}
}
