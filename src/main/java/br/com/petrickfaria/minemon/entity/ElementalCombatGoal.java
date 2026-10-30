package br.com.petrickfaria.minemon.entity;

import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/** Four successful contacts, then one collision-respecting elemental shot. */
final class ElementalCombatGoal extends MeleeAttackGoal {
    private final MinemonCreature pokemon;
    ElementalCombatGoal(MinemonCreature pokemon){super(pokemon,1.2D,true);this.pokemon=pokemon;}
    @Override public void tick(){
        var target=pokemon.getTarget();
        if(target==null || !pokemon.canAttack(target)){pokemon.setTarget(null);pokemon.getNavigation().stop();return;}
        if(pokemon.projectileReady()){
            pokemon.getLookControl().setLookAt(target,30,30);
            if(pokemon.distanceToSqr(target)<=25 && pokemon.getSensing().hasLineOfSight(target)){
                pokemon.getNavigation().stop();pokemon.fireElementalProjectile(target);
            }else if(!pokemon.isTame())pokemon.getNavigation().moveTo(target,1.2D);
        }else super.tick();
    }
    @Override protected int getAttackInterval(){return 50;}
}
