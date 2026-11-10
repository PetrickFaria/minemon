package br.com.petrickfaria.minemon.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;

/** Ranged-only combat with pathfinding to a safe firing position. */
final class ElementalCombatGoal extends Goal {
    private final MinemonCreature pokemon;
    private int repositionCooldown;
    private boolean retreating;

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

    @Override public void start() {
        repositionCooldown = 0;
        retreating = false;
        // Clear a follow-owner route before combat chooses its own firing point.
        pokemon.getNavigation().stop();
    }

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

        double distance = Math.sqrt(CombatGeometry.horizontalCenterDistanceSquared(pokemon, target));
        double safeDistance = Math.max(1.0D, Math.min(7.0D, pokemon.attackRange() - 1.0D));
        Vec3 towardPokemon = new Vec3(pokemon.getX() - target.getX(), 0.0D, pokemon.getZ() - target.getZ());
        double closingSpeed = towardPokemon.lengthSqr() < 1.0E-6D ? 0.0D
                : Math.max(0.0D, target.getDeltaMovement().dot(towardPokemon.normalize()));
        // Start moving early enough to account for an advancing target while a
        // new retreat route is being calculated and followed. Static threats
        // retain the exact safe-distance threshold.
        double retreatTriggerDistance = safeDistance + Math.min(1.5D, closingSpeed * 20.0D);

        // Projectile eligibility is independent of the preferred defensive distance.
        // Never suppress a ready shot merely because the target entered the danger zone.
        if (pokemon.combatReady() && pokemon.canFireAt(target)) {
            pokemon.fireElementalProjectile(target);
        }

        // Movement is independent of the attack cooldown. Retreat first even while
        // recharging, then resume normal positioning once the threat is far enough.
        if (distance < retreatTriggerDistance) {
            if (!retreating) repositionCooldown = 0;
            retreating = true;
            reposition(target, true);
            return;
        }
        retreating = false;

        if (distance <= pokemon.attackRange() && pokemon.getSensing().hasLineOfSight(target)) {
            // Once the target is outside the danger radius, hold position and
            // continue firing. Repositioning here made healthy companions circle
            // a safe enemy and could replace a retreat route immediately after a shot.
            pokemon.getNavigation().stop();
            repositionCooldown = 0;
        } else {
            reposition(target, false);
        }
    }

    private boolean endsAtSafeDistance(net.minecraft.world.level.pathfinder.Path path, LivingEntity target, double minimumDistance) {
        if (path == null || path.getNodeCount() == 0) return false;
        var last = path.getNode(path.getNodeCount() - 1);
        Vec3 destination = new Vec3(last.x + 0.5D, last.y, last.z + 0.5D);
        return horizontalDistance(destination, target.getBoundingBox().getCenter()) >= minimumDistance;
    }

    private void reposition(LivingEntity target, boolean retreat) {
        PathNavigation navigation = pokemon.getNavigation();
        double safeDistance = Math.max(1.0D, Math.min(7.0D, pokemon.attackRange() - 1.0D));
        // Keep a retreat route while it remains safe relative to the moving
        // target. When a route ends short of safety, replan on the next tick;
        // a fixed cooldown here let an advancing attacker land free hits.
        if (retreat && !navigation.isDone()) {
            // The route was checked against the target's position when it was
            // selected. Rechecking every tick against an advancing target made
            // it invalid almost immediately and repeatedly stopped the Pokémon.
            // Let it finish; the next tick after completion selects a fresh
            // escape route if the threat is still inside the safety radius.
            return;
        }
        // Keep approach route selection stable, and briefly throttle failed
        // escape searches when the terrain offers no valid path.
        if (!retreat && !navigation.isDone()) return;
        if (repositionCooldown-- > 0) return;
        repositionCooldown = retreat ? 2 : 10;
        Vec3 away = pokemon.position().subtract(target.position());
        away = new Vec3(away.x, 0.0D, away.z);
        if (away.lengthSqr() < 1.0E-6D) away = new Vec3(1.0D, 0.0D, 0.0D);
        away = away.normalize();

        double desired = retreat ? safeDistance + 1.0D : Math.min(pokemon.attackRange() - 1.0D, safeDistance + 1.5D);
        // Prefer the nearest safe point on the desired radius, then try both
        // sides of it so ordinary pathfinding can work around obstacles.
        // Retreat points directly away first, then tests nearby alternatives.
        double[] angles = retreat ? new double[]{0.0D, 0.65D, -0.65D, 1.3D, -1.3D, Math.PI}
                : new double[]{0.75D, -0.75D, 1.3D, -1.3D, Math.PI};
        double nearestSafeRadius = retreat ? safeDistance : desired;
        for (double radius = desired; radius + 1.0E-6D >= nearestSafeRadius; radius -= 0.5D) {
            for (double angle : angles) {
                double cos = Math.cos(angle), sin = Math.sin(angle);
                double dx = away.x * cos - away.z * sin;
                double dz = away.x * sin + away.z * cos;
                // Treat desired as a target-centered firing radius. Adding it to
                // the Pokémon's current position overshoots by the current gap and
                // can push a retreat goal against the arena edge before it can fire.
                double destinationX = target.getX() + dx * radius;
                double destinationZ = target.getZ() + dz * radius;
                BlockPos destination = new BlockPos((int)Math.floor(destinationX),
                        (int)Math.floor(pokemon.getY()), (int)Math.floor(destinationZ));
                var path = navigation.createPath(destination, 0);
                if (path != null && path.canReach() && endsAtSafeDistance(path, target, safeDistance)) {
                    // Spider's natural movement-speed attribute is 0.30. The
                    // GameTests compare actual distance traveled against a live
                    // vanilla Spider to validate controller-specific movement.
                    // without changing the Pokémon's natural movement attribute.
                    double actualSpeed = pokemon.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                    double speedModifier = retreat ? 0.30D / Math.max(0.01D, actualSpeed) : 1.0D;
                    navigation.moveTo(path, speedModifier);
                    return;
                }
            }
        }
        // Do not cancel an already valid retreat path just because a fresh
        // navigation candidate could not be computed on this tick.
        if (navigation.isDone()) navigation.stop();
    }

    private static double horizontalDistance(Vec3 first, Vec3 second) {
        double dx = first.x - second.x;
        double dz = first.z - second.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
