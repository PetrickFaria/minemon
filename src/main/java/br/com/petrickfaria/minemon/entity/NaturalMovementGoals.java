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
            if (!this.mob.wantsNaturalLanding() || (!this.mob.isSleeping()&&!this.mob.isNightResting()&&!this.mob.naturalBehaviorActive()) || this.mob.isOnGround() || this.mob.isPassenger()) return false;
            if (this.retryDelay > 0) { this.retryDelay--; return false; }
            this.landingPath = findLandingPath();
            if (this.landingPath == null) this.retryDelay = 40;
            return this.landingPath != null;
        }

        @Override public boolean canContinueToUse() {
            return this.mob.wantsNaturalLanding() && (this.mob.isSleeping()||this.mob.isNightResting()||this.mob.naturalBehaviorActive()) && !this.mob.isOnGround()
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
                    // Cross a shoreline above its collision lip before descending.
                    // A diagonal path straight to ground can wedge a wide
                    // flying hitbox against the bank while its center is wet.
                    double currentSurface=this.mob.naturalSurface();
                    BlockPos under=new BlockPos(this.mob.getX(),currentSurface-0.001D,this.mob.getZ());
                    boolean overFluid=!this.mob.level.getFluidState(under).isEmpty();
                    BlockPos approach=overFluid?feet.above(2):feet;
                    if(!this.mob.level.noCollision(this.mob,this.mob.getBoundingBox().move(
                            x-this.mob.getX(),approach.getY()-this.mob.getY(),z-this.mob.getZ())))continue;
                    Path path = this.mob.getNavigation().createPath(approach, 0);
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
        private Path selectedPath;
        private net.minecraft.world.phys.Vec3 progressPosition;
        private int lastProgressTick;

        AirPatrol(MinemonCreature mob) {
            this.mob = mob;
            this.cooldown = fluidFloater()?0:20 + mob.getRandom().nextInt(40);
            this.setFlags(this.mob.getSpeciesSlug().equals("haunter")
                    ? EnumSet.of(Goal.Flag.MOVE,Goal.Flag.LOOK) : EnumSet.of(Goal.Flag.MOVE));
        }

        private boolean fluidFloater(){return this.mob.getSpeciesSlug().equals("haunter")||this.mob.getSpeciesSlug().equals("gengar");}

        @Override public boolean canUse() {
            if (this.cooldown > 0) { this.cooldown--; return false; }
            if (!eligible() || this.mob.getTarget() != null || !this.mob.getNavigation().isDone()
                    || !fluidFloater()&&this.mob.getRandom().nextInt(4) != 0) return false;
            if (chooseDestination()) return true;
            this.cooldown = 40;
            return false;
        }

        @Override public boolean canContinueToUse() {
            return eligible() && this.mob.getTarget() == null && !this.mob.getNavigation().isDone();
        }

        @Override public void start() {
            this.progressPosition=this.mob.position();this.lastProgressTick=this.mob.tickCount;
            if(this.mob.getSpeciesSlug().equals("haunter"))this.mob.getNavigation().moveTo(this.selectedPath,0.82D);
            else if (this.destination != null) this.mob.getNavigation().moveTo(
                    this.destination.getX() + 0.5D, this.destination.getY(), this.destination.getZ() + 0.5D, 0.82D);
        }

        @Override public void tick() {
            if(!this.mob.getSpeciesSlug().equals("haunter"))return;
            if(this.progressPosition==null||this.mob.position().distanceToSqr(this.progressPosition)>.0625D){
                this.progressPosition=this.mob.position();this.lastProgressTick=this.mob.tickCount;
            }else if(this.mob.tickCount-this.lastProgressTick>=80){
                this.mob.getNavigation().stop();return;
            }
            var path=this.mob.getNavigation().getPath();
            if(path!=null&&!path.isDone()){
                var next=path.getNextEntityPos(this.mob);
                this.mob.getLookControl().setLookAt(next.x,next.y+this.mob.getEyeHeight(),next.z,10.0F,10.0F);
            }
        }

        @Override public void stop() {
            if(this.mob.getSpeciesSlug().equals("haunter"))this.mob.getNavigation().stop();
            this.selectedPath=null;
            this.destination = null;
            this.cooldown = fluidFloater()?1+this.mob.getRandom().nextInt(5):70 + this.mob.getRandom().nextInt(150);
        }

        private boolean eligible() {
            var profile=SpeciesBehaviorProfiles.profile(this.mob.getSpeciesSlug());
            boolean fixedHeight=profile==SpeciesBehaviorProfiles.Profile.FLOAT_HALF
                    ||profile==SpeciesBehaviorProfiles.Profile.FLOAT_LOW;
            return this.mob.isFlyingSpecies()&&!fixedHeight&&!this.mob.wantsNaturalLanding()
                    &&this.mob.naturalBehaviorActive();
        }

        private boolean chooseDestination() {
            int x0 = this.mob.blockPosition().getX();
            int z0 = this.mob.blockPosition().getZ();
            for (int attempt = 0; attempt < 12; attempt++) {
                int x,z;
                if(fluidFloater()){
                    double offset=this.mob.getSpeciesSlug().equals("haunter")&&attempt>=6
                            ?this.mob.getRandom().nextInt(360)-180:this.mob.getRandom().nextInt(61)-30;
                    double yaw=Math.toRadians(this.mob.getYRot()+offset);
                    int distance=8+this.mob.getRandom().nextInt(9);
                    x=x0+(int)Math.round(-Math.sin(yaw)*distance);
                    z=z0+(int)Math.round(Math.cos(yaw)*distance);
                }else{
                    x=x0+this.mob.getRandom().nextInt(25)-12;
                    z=z0+this.mob.getRandom().nextInt(25)-12;
                }
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
                if (path != null && path.canReach()) { this.destination = candidate; this.selectedPath=path; return true; }
            }
            return false;
        }
    }

    static final class GroundWander extends RandomStrollGoal {
        private final MinemonCreature mob;
        GroundWander(MinemonCreature mob){super(mob,0.8D,1);this.mob=mob;}
        private boolean eligible(){return !this.mob.isFlyingSpecies()&&!this.mob.isInWaterOrBubble()
                &&this.mob.naturalBehaviorActive()&&this.mob.naturalCycle().walking();}
        @Override public boolean canUse(){return eligible()&&super.canUse();}
        @Override public boolean canContinueToUse(){return eligible()&&super.canContinueToUse();}
    }
    static final class AquaticLandWander extends RandomStrollGoal {
        private final MinemonCreature mob;
        AquaticLandWander(MinemonCreature mob) { super(mob, 0.65D, 1); this.mob = mob; }
        @Override protected net.minecraft.world.phys.Vec3 getPosition(){
            var candidate=net.minecraft.world.entity.ai.util.LandRandomPos.getPos(this.mob,10,7);
            if(candidate==null)return null;
            if(SpeciesBehaviorProfiles.isVisitor(this.mob.getSpeciesSlug())){
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
            return SpeciesBehaviorProfiles.isVisitor(this.mob.getSpeciesSlug())&&!this.mob.isInWaterOrBubble()
                    &&this.mob.naturalBehaviorActive()&&this.mob.naturalCycle().walking()&&super.canUse();
        }
        @Override public boolean canContinueToUse() {
            return SpeciesBehaviorProfiles.isVisitor(this.mob.getSpeciesSlug())&&!this.mob.isInWaterOrBubble()
                    &&this.mob.naturalBehaviorActive()&&this.mob.naturalCycle().walking()&&super.canContinueToUse();
        }
    }
}
