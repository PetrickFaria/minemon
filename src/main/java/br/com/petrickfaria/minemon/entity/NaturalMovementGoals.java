package br.com.petrickfaria.minemon.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.pathfinder.Path;

/** Small, path-navigation based goals for non-combat natural movement. */
public final class NaturalMovementGoals {
    private NaturalMovementGoals() {}

    /** Finds the nearest real support surface beneath an entity, ignoring roofs above it. */
    public static double surfaceBelow(Level level, double x, double feetY, double z) {
        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);
        for (int y = Mth.floor(feetY - 0.001D); y >= level.getMinBuildHeight(); y--) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            if (!level.hasChunkAt(pos)) return level.getMinBuildHeight();
            // Open water is a navigable surface too. Measuring from the seabed
            // would otherwise pull flyers underwater above deep lakes/oceans.
            double top=level.getFluidState(pos).is(FluidTags.WATER)
                    ? level.getFluidState(pos).getHeight(level, pos) : Double.NEGATIVE_INFINITY;
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                double localX=x-blockX, localZ=z-blockZ;
                // Use the actual support under this column. A bottom slab has
                // no sturdy block-boundary UP face, but its half-height top is
                // still walkable; narrow fence shapes must not fill the column.
                for (net.minecraft.world.phys.AABB box : shape.toAabbs()) {
                    if (localX>=box.minX&&localX<=box.maxX&&localZ>=box.minZ&&localZ<=box.maxZ)
                        top=Math.max(top,box.maxY);
                }
            }
            if (top>0.0D) return y+top;
        }
        return level.getMinBuildHeight();
    }

    /** Sleep has priority over patrol, following and combat, but descent still uses a real path. */
    static final class SleepLanding extends Goal {
        private final MinemonCreature mob;
        private Path landingPath;
        private int retryDelay;

        SleepLanding(MinemonCreature mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override public boolean canUse() {
            if (!this.mob.wantsNaturalLanding() || (!this.mob.isSleeping()&&!this.mob.naturalBehaviorActive()) || this.mob.isOnGround() || this.mob.isPassenger()) return false;
            if (this.retryDelay > 0) { this.retryDelay--; return false; }
            this.landingPath = findLandingPath();
            if (this.landingPath == null) this.retryDelay = 40;
            return this.landingPath != null;
        }

        @Override public boolean canContinueToUse() {
            return this.mob.wantsNaturalLanding() && (this.mob.isSleeping()||this.mob.naturalBehaviorActive()) && !this.mob.isOnGround()
                    && !this.mob.getNavigation().isDone();
        }

        @Override public void start() { this.mob.getNavigation().moveTo(this.landingPath, 0.7D); }
        @Override public void stop() { this.mob.getNavigation().stop(); this.landingPath = null; }

        private Path findLandingPath() {
            BlockPos origin = this.mob.blockPosition();
            // Prefer the closest safe dry surface, and expand only if obstructed.
            for (int radius = 0; radius <= 25; radius++) {
                for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    double x = origin.getX() + dx + 0.5D, z = origin.getZ() + dz + 0.5D;
                    double surface = surfaceBelow(this.mob.level, x, this.mob.getY(), z);
                    BlockPos floor = new BlockPos(x, surface - 0.001D, z);
                    if (!this.mob.level.hasChunkAt(floor) || !this.mob.level.getFluidState(floor).isEmpty()) continue;
                    BlockState state = this.mob.level.getBlockState(floor);
                    if (state.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)
                            || state.is(net.minecraft.world.level.block.Blocks.CACTUS)
                            || state.is(net.minecraft.world.level.block.Blocks.CAMPFIRE)
                            || state.is(net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE)) continue;
                    BlockPos feet = new BlockPos(x, Math.ceil(surface), z);
                    if (!this.mob.level.getFluidState(feet).isEmpty()
                            || !this.mob.level.getBlockState(feet).isAir()
                            || !this.mob.level.noCollision(this.mob, this.mob.getBoundingBox().move(
                                    x - this.mob.getX(), surface - this.mob.getY(), z - this.mob.getZ()))) continue;
                    Path path = this.mob.getNavigation().createPath(feet, 0);
                    if (path != null && path.canReach()) return path;
                }
            }
            return null;
        }
    }

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
            return this.mob.isFlyingSpecies() && !this.mob.wantsNaturalLanding() && this.mob.naturalBehaviorActive();
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
                int low=(int)this.mob.naturalCycle().minimumAltitude(),high=(int)this.mob.naturalCycle().maximumAltitude();
                int altitude=low+this.mob.getRandom().nextInt(high-low+1);
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

    static final class GroundWander extends RandomStrollGoal {
        private final MinemonCreature mob;
        GroundWander(MinemonCreature mob){super(mob,0.8D);this.mob=mob;}
        @Override public boolean canUse(){return !this.mob.isFlyingSpecies()&&this.mob.naturalBehaviorActive()&&super.canUse();}
        @Override public boolean canContinueToUse(){return !this.mob.isFlyingSpecies()&&this.mob.naturalBehaviorActive()&&super.canContinueToUse();}
    }
    static final class AquaticLandWander extends net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal {
        private final MinemonCreature mob;
        AquaticLandWander(MinemonCreature mob) { super(mob, 0.65D); this.mob = mob; }
        @Override protected net.minecraft.world.phys.Vec3 getPosition(){
            var candidate=super.getPosition();if(candidate==null)return null;
            if(SpeciesBehaviorProfiles.isVisitor(this.mob.getSpeciesSlug())&&this.mob.naturalCycle().phase()==BehaviorCycle.Phase.COOLDOWN){
                var path=this.mob.getNavigation().createPath(new BlockPos(candidate),0);
                if(path==null||!path.canReach())return null;
                for(int i=0;i<path.getNodeCount();i++){
                    var node=path.getNode(i);
                    if(!this.mob.level.getFluidState(new BlockPos(node.x,node.y,node.z)).isEmpty())return null;
                }
            }
            return candidate;
        }
        @Override public boolean canUse() {
            return !this.mob.isInWater() && this.mob.naturalBehaviorActive() && super.canUse();
        }
    }
}
