package br.com.petrickfaria.minemon.entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
/** Tamed companions prefer charged projectiles, using one shared attack cooldown. */
final class ElementalCombatGoal extends MeleeAttackGoal {
 private final MinemonCreature pokemon;
 ElementalCombatGoal(MinemonCreature pokemon){super(pokemon,1.2D,true);this.pokemon=pokemon;}
 @Override public boolean canUse(){
  var target=pokemon.getTarget();
  if(pokemon.isDefeated())return false;
  if(pokemon.isTame()&&pokemon.isOrderedToSit())
   return target!=null&&pokemon.canAttack(target)
       &&((pokemon.projectileReady()&&pokemon.canFireAt(target))
       ||(br.com.petrickfaria.minemon.entity.CombatGeometry.inRange(pokemon,target,.5D)
       &&pokemon.getSensing().hasLineOfSight(target)));
  return pokemon.isTame()&&target!=null&&pokemon.projectileReady()&&pokemon.canFireAt(target)||super.canUse();
 }
 @Override public void tick(){
  var target=pokemon.getTarget();
  if(target==null||!pokemon.canAttack(target)){pokemon.setTarget(null);pokemon.getNavigation().stop();return;}
  if(pokemon.isTame()&&pokemon.isOrderedToSit()){
   pokemon.getNavigation().stop();pokemon.getLookControl().setLookAt(target,30,30);
   if(pokemon.projectileReady()&&pokemon.canFireAt(target))pokemon.fireElementalProjectile(target);
   else if(isTimeToAttack()&&CombatGeometry.inRange(pokemon,target,.5D)
       &&pokemon.getSensing().hasLineOfSight(target)&&pokemon.combatReady()&&pokemon.doHurtTarget(target))resetAttackCooldown();
   return;
  }
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
