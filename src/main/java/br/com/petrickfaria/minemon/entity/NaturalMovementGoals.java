package br.com.petrickfaria.minemon.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.level.pathfinder.Path;

/** Small, path-navigation based goals for non-combat natural movement. */
final class NaturalMovementGoals {
    private NaturalMovementGoals() {}

    static final class AirPatrol extends Goal {
        private final MinemonCreature mob;
        private BlockPos destination;
        private int cooldown;

        AirPatrol(MinemonCreature mob) {
            this.mob = mob;
            this.cooldown = 20 + mob.getRandom().nextInt(40);
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override public boolean canUse() {
            if (this.cooldown > 0) { this.cooldown--; return false; }
            if (!eligible() || this.mob.getTarget() != null || !this.mob.getNavigation().isDone()
                    || this.mob.getRandom().nextInt(4) != 0) return false;
            if (chooseDestination()) return true;
            this.cooldown = 40;
            return false;
        }

        @Override public boolean canContinueToUse() {
            return eligible() && this.mob.getTarget() == null && !this.mob.getNavigation().isDone();
        }

        @Override public void start() {
            if (this.destination != null) this.mob.getNavigation().moveTo(
                    this.destination.getX() + 0.5D, this.destination.getY(), this.destination.getZ() + 0.5D, 0.82D);
        }

        @Override public void stop() {
            this.destination = null;
            this.cooldown = 70 + this.mob.getRandom().nextInt(150);
        }

        private boolean eligible() {
            return !this.mob.isSleeping() && !this.mob.isOrderedToSit() && !this.mob.isPassenger()
                    && !this.mob.isDeadOrDying() && !this.mob.isNoAi();
        }

        private boolean chooseDestination() {
            int x0 = this.mob.blockPosition().getX();
            int z0 = this.mob.blockPosition().getZ();
            for (int attempt = 0; attempt < 12; attempt++) {
                int x = x0 + this.mob.getRandom().nextInt(25) - 12;
                int z = z0 + this.mob.getRandom().nextInt(25) - 12;
                if (!this.mob.level.hasChunkAt(new BlockPos(x, this.mob.blockPosition().getY(), z))) continue;
                int surfaceY = this.mob.level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                int altitude = 4 + this.mob.getRandom().nextInt(9);
                int y = Math.min(this.mob.level.getMaxBuildHeight() - 2, surfaceY + altitude);
                BlockPos candidate = new BlockPos(x, y, z);
                if (!this.mob.level.hasChunkAt(candidate)
                        || !this.mob.level.noCollision(this.mob, this.mob.getBoundingBox().move(
                                x + 0.5D - this.mob.getX(), y - this.mob.getY(), z + 0.5D - this.mob.getZ()))) continue;
                Path path = this.mob.getNavigation().createPath(candidate, 0);
                if (path != null && path.canReach()) { this.destination = candidate; return true; }
            }
            return false;
        }
    }

    static final class SeekWater extends Goal {
        private final MinemonCreature mob;
        private BlockPos destination;
        private int retryDelay;

        SeekWater(MinemonCreature mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override public boolean canUse() {
            if (this.retryDelay > 0) { this.retryDelay--; return false; }
            if (!eligible() || this.mob.getTarget() != null || !this.mob.getNavigation().isDone()) return false;
            this.destination = findWaterDestination();
            return this.destination != null;
        }

        @Override public boolean canContinueToUse() {
            return eligible() && this.mob.getTarget() == null && !this.mob.getNavigation().isDone()
                    && this.destination != null;
        }

        @Override public void start() {
            Path path = this.mob.getNavigation().createPath(this.destination, 0);
            if (path == null || !path.canReach()) this.mob.getNavigation().stop();
            else this.mob.getNavigation().moveTo(path, 0.85D);
        }

        @Override public void stop() {
            this.destination = null;
            this.retryDelay = 35 + this.mob.getRandom().nextInt(45);
        }

        private boolean eligible() {
            return !this.mob.isSleeping() && !this.mob.isOrderedToSit() && !this.mob.isPassenger()
                    && !this.mob.isDeadOrDying() && !this.mob.isNoAi();
        }

        private BlockPos findWaterDestination() {
            BlockPos origin = this.mob.blockPosition();
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            int localRadius = this.mob.isInWater() ? 4 : 5;
            java.util.List<BlockPos> nearbyWater = new java.util.ArrayList<>();
            for (int dx = -localRadius; dx <= localRadius; dx++) for (int dz = -localRadius; dz <= localRadius; dz++) {
                int x = origin.getX() + dx, z = origin.getZ() + dz;
                for (int dy = -3; dy <= 3; dy++) {
                    BlockPos candidate = new BlockPos(x, origin.getY() + dy, z);
                    if (!this.mob.level.hasChunkAt(candidate)
                            || !this.mob.level.getFluidState(candidate).is(FluidTags.WATER)) continue;
                    double distance = candidate.distSqr(origin);
                    if (distance >= 4.0D) nearbyWater.add(candidate.immutable());
                }
            }
            nearbyWater.sort(java.util.Comparator.comparingDouble(candidate -> candidate.distSqr(origin)));
            for (int i = 0; i < Math.min(8, nearbyWater.size()); i++) {
                BlockPos candidate = nearbyWater.get(i);
                Path path = this.mob.getNavigation().createPath(candidate, 0);
                if (path != null && path.canReach()) return candidate;
            }
            for (int attempt = 0; attempt < 36; attempt++) {
                int radius = this.mob.isInWater() ? 7 : 16;
                int x = origin.getX() + this.mob.getRandom().nextInt(radius * 2 + 1) - radius;
                int z = origin.getZ() + this.mob.getRandom().nextInt(radius * 2 + 1) - radius;
                int y = origin.getY() + this.mob.getRandom().nextInt(9) - 4;
                for (int dy = -4; dy <= 4; dy++) {
                    BlockPos candidate = new BlockPos(x, y + dy, z);
                    if (!this.mob.level.hasChunkAt(candidate)
                            || !this.mob.level.getFluidState(candidate).is(FluidTags.WATER)) continue;
                    double distance = candidate.distSqr(origin);
                    if ((this.mob.isInWater() && distance < 20.0D) || distance < 4.0D || distance >= bestDistance) continue;
                    Path path = this.mob.getNavigation().createPath(candidate, 0);
                    if (path != null && path.canReach()) { best = candidate.immutable(); bestDistance = distance; }
                    break;
                }
            }
            return best;
        }
    }

    static final class AquaticLandWander extends RandomStrollGoal {
        private final MinemonCreature mob;
        AquaticLandWander(MinemonCreature mob) { super(mob, 0.65D); this.mob = mob; }
        @Override public boolean canUse() {
            return !this.mob.isInWater() && !this.mob.isSleeping() && !this.mob.isOrderedToSit()
                    && this.mob.getTarget() == null && super.canUse();
        }
    }
}
