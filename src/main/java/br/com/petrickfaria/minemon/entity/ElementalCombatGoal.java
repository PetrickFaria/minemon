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

        // Projectile eligibility is independent of the preferred defensive distance.
        // Never suppress a ready shot merely because the target entered the danger zone.
        if (pokemon.combatReady() && pokemon.canFireAt(target)) {
            pokemon.fireElementalProjectile(target);
        }

        // Movement is independent of the attack cooldown. Retreat first even while
        // recharging, then resume normal positioning once the threat is far enough.
        if (distance < safeDistance) {
            if (!retreating) repositionCooldown = 0;
            retreating = true;
            reposition(target, true);
            return;
        }
        retreating = false;

        if (distance > pokemon.attackRange() || !pokemon.getSensing().hasLineOfSight(target)) {
            reposition(target, false);
        } else {
            // Being within firing range is not a reason to stand still.
            // Keep circling the target, including while the shot recharges.
            reposition(target, false);
        }
    }

    private boolean keepsSafeDistance(net.minecraft.world.level.pathfinder.Path path, LivingEntity target, double minimumDistance) {
        Vec3 targetCenter = target.getBoundingBox().getCenter();
        double previousDistance = horizontalDistance(pokemon.position(), targetCenter);
        for (int i = 0; i < path.getNodeCount(); i++) {
            var node = path.getNode(i);
            Vec3 end = new Vec3(node.x + 0.5D, node.y, node.z + 0.5D);
            Vec3 start = i == 0 ? pokemon.position() : new Vec3(
                    path.getNode(i - 1).x + 0.5D, path.getNode(i - 1).y, path.getNode(i - 1).z + 0.5D);
            int samples = Math.max(1, (int)Math.ceil(end.distanceTo(start) * 2.0D));
            for (int sample = 1; sample <= samples; sample++) {
                Vec3 point = start.lerp(end, sample / (double)samples);
                double distance = horizontalDistance(point, targetCenter);
                // A path's first node can be the block center already occupied
                // by the creature. Equal-distance samples are harmless; rejecting
                // them discards every valid escape before its first actual step.
                if (distance + 1.0E-6D < minimumDistance
                        && (previousDistance >= minimumDistance || distance + 1.0E-6D < previousDistance)) return false;
                previousDistance = distance;
            }
        }
        return path.getNodeCount() > 0;
    }

    private void reposition(LivingEntity target, boolean retreat) {
        // Do not interrupt a valid circling route every ten ticks; select a
        // fresh route once the previous route has actually completed.
        if (!retreat && !pokemon.getNavigation().isDone()) return;
        if (repositionCooldown-- > 0) return;
        repositionCooldown = 10;
        Vec3 away = pokemon.position().subtract(target.position());
        away = new Vec3(away.x, 0.0D, away.z);
        if (away.lengthSqr() < 1.0E-6D) away = new Vec3(1.0D, 0.0D, 0.0D);
        away = away.normalize();

        double safeDistance = Math.max(1.0D, Math.min(7.0D, pokemon.attackRange() - 1.0D));
        double desired = retreat ? safeDistance + 1.0D : Math.min(pokemon.attackRange() - 1.0D, safeDistance + 1.5D);
        // Prefer the nearest safe point on the desired radius, then try both
        // sides of it so ordinary pathfinding can work around obstacles.
        // Circle tangentially when in range; retreat along the safest escape.
        double[] angles = retreat ? new double[]{0.0D, 0.65D, -0.65D, 1.3D, -1.3D, Math.PI}
                : new double[]{0.75D, -0.75D, 1.3D, -1.3D, Math.PI};
        PathNavigation navigation = pokemon.getNavigation();
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
                if (path != null && path.canReach() && keepsSafeDistance(path, target, safeDistance)) {
                    // Match the vanilla zombie's 0.23 movement-speed reference
                    // without changing the Pokémon's natural movement attribute.
                    double actualSpeed = pokemon.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                    double speedModifier = retreat ? 0.23D / Math.max(0.01D, actualSpeed) : 1.0D;
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
