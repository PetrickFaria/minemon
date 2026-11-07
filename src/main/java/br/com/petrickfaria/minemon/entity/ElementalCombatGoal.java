package br.com.petrickfaria.minemon.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;

/** Ranged-only combat with pathfinding to a safe firing position. */
final class ElementalCombatGoal extends Goal {
    private static final double MIN_DISTANCE = 7.0D;
    private final MinemonCreature pokemon;
    private int repositionCooldown;

    ElementalCombatGoal(MinemonCreature pokemon) {
        this.pokemon = pokemon;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private LivingEntity target() {
        LivingEntity target = pokemon.getTarget();
        return target != null && target.isAlive() && pokemon.canAttack(target) ? target : null;
    }

    @Override public boolean canUse() { return !pokemon.isDefeated() && target() != null; }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public void start() { repositionCooldown = 0; }

    @Override public void tick() {
        LivingEntity target = target();
        if (target == null) {
            pokemon.getNavigation().stop();
            pokemon.setTarget(null);
            return;
        }
        pokemon.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // A sitting order is a hard position lock, including during combat.
        if (pokemon.isTame() && pokemon.isOrderedToSit()) {
            pokemon.getNavigation().stop();
            if (pokemon.combatReady() && pokemon.canFireAt(target)) pokemon.fireElementalProjectile(target);
            return;
        }

        double distance = Math.sqrt(CombatGeometry.centerDistanceSquared(pokemon, target));
        if (distance < MIN_DISTANCE) {
            reposition(target, true);
            return;
        }

        if (pokemon.combatReady() && pokemon.canFireAt(target)) {
            pokemon.getNavigation().stop();
            pokemon.fireElementalProjectile(target);
            return;
        }

        // Hold position throughout the cooldown when the target remains in a
        // valid firing lane. Reposition only for range or line-of-sight loss.
        if (distance > pokemon.combatRange() || !pokemon.getSensing().hasLineOfSight(target)) {
            reposition(target, false);
        } else {
            pokemon.getNavigation().stop();
        }
    }

    private void reposition(LivingEntity target, boolean retreat) {
        if (repositionCooldown-- > 0) return;
        repositionCooldown = 10;
        Vec3 away = pokemon.position().subtract(target.position());
        away = new Vec3(away.x, 0.0D, away.z);
        if (away.lengthSqr() < 1.0E-6D) away = new Vec3(1.0D, 0.0D, 0.0D);
        away = away.normalize();

        double desired = retreat ? MIN_DISTANCE + 1.5D : Math.min(pokemon.combatRange() - 1.0D, 12.0D);
        // Prefer the nearest safe point on the desired radius, then try both
        // sides of it so ordinary pathfinding can work around obstacles.
        double[] angles = {0.0D, 0.65D, -0.65D, 1.3D, -1.3D, Math.PI};
        PathNavigation navigation = pokemon.getNavigation();
        for (double angle : angles) {
            double cos = Math.cos(angle), sin = Math.sin(angle);
            double dx = away.x * cos - away.z * sin;
            double dz = away.x * sin + away.z * cos;
            BlockPos destination = new BlockPos((int)Math.floor(target.getX() + dx * desired),
                    (int)Math.floor(pokemon.getY()), (int)Math.floor(target.getZ() + dz * desired));
            var path = navigation.createPath(destination, 0);
            if (path != null && path.canReach()) {
                navigation.moveTo(path, 1.0D);
                return;
            }
        }
        navigation.stop();
    }
}
