package br.com.petrickfaria.minemon.entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
/** Tamed companions prefer charged projectiles, using one shared attack cooldown. */
final class ElementalCombatGoal extends MeleeAttackGoal {
 private final MinemonCreature pokemon;
 ElementalCombatGoal(MinemonCreature pokemon){super(pokemon,1.2D,true);this.pokemon=pokemon;}
 @Override public boolean canUse(){var target=pokemon.getTarget();return !pokemon.isDefeated() && (pokemon.isTame() && target!=null && pokemon.projectileReady() && pokemon.canFireAt(target) || super.canUse());}
 @Override public void tick(){
  var target=pokemon.getTarget();
  if(target==null||!pokemon.canAttack(target)){pokemon.setTarget(null);pokemon.getNavigation().stop();return;}
  if(pokemon.projectileReady()&&pokemon.canFireAt(target)){
   pokemon.getLookControl().setLookAt(target,30,30);pokemon.getNavigation().stop();pokemon.fireElementalProjectile(target);
  }else super.tick();
 }
 @Override protected void checkAndPerformAttack(LivingEntity target,double distance){
  if(!pokemon.isTame()){super.checkAndPerformAttack(target,distance);return;}
  if(isTimeToAttack()&&CombatGeometry.inRange(pokemon,target,.5D)&&pokemon.canAttack(target)&&pokemon.combatReady()){
   if(pokemon.doHurtTarget(target))resetAttackCooldown();
  }
 }
 @Override protected int getAttackInterval(){return pokemon.isTame()?60:50;}
}
