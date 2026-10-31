package br.com.petrickfaria.minemon.entity;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
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

    static boolean nightWaterSetbackRequired(MinemonCreature mob) {
        if (!mob.isFlyingSpecies() || !mob.isNightResting()
                || !SpeciesBehaviorProfiles.landsForNightRest(mob.getSpeciesSlug())) return false;
        return mob.isInWaterOrBubble() || !hasThreeBlockWaterClearance(mob,mob.getBoundingBox());
    }

    private static boolean hasThreeBlockWaterClearance(MinemonCreature mob, net.minecraft.world.phys.AABB box) {
        int minX = Mth.floor(box.minX - 3.0D), maxX = Mth.floor(box.maxX + 3.0D);
        int minZ = Mth.floor(box.minZ - 3.0D), maxZ = Mth.floor(box.maxZ + 3.0D);
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            if (!NightShoreLandingRules.waterIsTooClose(box, x, z, 3.0D)) continue;
            BlockPos column = new BlockPos(x, Mth.floor(box.minY), z);
            if (!mob.level.hasChunkAt(column)) continue;
            int surface = mob.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,x,z);
            for (int depth = 1; depth <= 4 && surface - depth >= mob.level.getMinBuildHeight(); depth++)
                if (mob.level.getFluidState(new BlockPos(x,surface-depth,z)).is(FluidTags.WATER)) return false;
        }
        return true;
    }

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
        private static final int SHORE_HORIZONTAL_RADIUS = 25;
        private static final int SHORE_VERTICAL_RADIUS = 25;
        private final MinemonCreature mob;
        private Path landingPath;
        private int retryDelay;
        private int pathTicks;
        private BlockPos approachSite;
        private BlockPos cachedLandingFeet;
        private long cachedLandingUntil;
        private int hoverMoveDelay;
        private int hoverDirection;
        private boolean shoreRoute;
        private boolean directShoreRoute;
        private int shoreStallTicks;
        private net.minecraft.world.phys.Vec3 lastShorePosition;
        private final Map<BlockPos, Long> rejectedShoreLandings = new HashMap<>();

        SleepLanding(MinemonCreature mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override public boolean canUse() {
            if (!this.mob.wantsNaturalLanding() || (!this.mob.commandLanding()&&!this.mob.isSleeping()&&!this.mob.isNightResting()&&!this.mob.naturalBehaviorActive()) || this.mob.isOnGround() && !this.mob.isInWaterOrBubble() || this.mob.isPassenger()) return false;
            if (this.retryDelay > 0) {
                this.retryDelay--;
                if (NaturalMovementGoals.nightWaterSetbackRequired(this.mob)) keepFlyingWhileSearching();
                return false;
            }
            this.shoreRoute = NaturalMovementGoals.nightWaterSetbackRequired(this.mob);
            this.landingPath = findLandingPath();
            if (this.landingPath == null) {
                this.retryDelay = 40;
                if (this.shoreRoute) keepFlyingWhileSearching();
            }
            return this.landingPath != null;
        }

        @Override public boolean canContinueToUse() {
            return this.mob.wantsNaturalLanding() && (this.mob.commandLanding()||this.mob.isSleeping()||this.mob.isNightResting()||this.mob.naturalBehaviorActive()) && (!this.mob.isOnGround() || this.mob.isInWaterOrBubble())
                    && this.pathTicks < 200;
        }

        @Override public void start() {
            this.pathTicks=0;
            this.shoreStallTicks=0;
            this.lastShorePosition=this.mob.position();
            // 1.0 is a moderate 1.43× increase over the usual 0.7 landing speed, only for shore escapes.
            if (this.directShoreRoute) {
                this.mob.getNavigation().stop();
                moveDirectlyToApproach();
            } else this.mob.getNavigation().moveTo(this.landingPath, this.shoreRoute ? 1.0D : 0.7D);
        }
        @Override public void tick() {
            this.pathTicks++;
            if (this.shoreRoute && !this.directShoreRoute && !this.mob.getNavigation().isDone()) {
                if (this.lastShorePosition != null && this.mob.position().distanceToSqr(this.lastShorePosition) < 0.0025D)
                    this.shoreStallTicks++;
                else this.shoreStallTicks=0;
                this.lastShorePosition=this.mob.position();
                if (this.shoreStallTicks >= 20 && this.approachSite != null) {
                    var target=this.mob.getBoundingBox().move(this.approachSite.getX()+.5D-this.mob.getX(),
                            this.approachSite.getY()-this.mob.getY(),this.approachSite.getZ()+.5D-this.mob.getZ());
                    if (directLineClear(target)) {
                        this.directShoreRoute=true;
                        this.mob.getNavigation().stop();
                    } else {
                        rejectCachedShoreLanding();
                        this.pathTicks=200;
                        this.mob.getNavigation().stop();
                    }
                }
            }
            if (this.directShoreRoute && this.approachSite != null) {
                moveDirectlyToApproach();
                return;
            }
            // Native air paths stop within a width-dependent radius. Complete
            // the final short approach with the same collision-respecting move
            // control before allowing gravity to lower a wide body into water.
            var feet=new BlockPos(this.mob.getX(),this.mob.naturalSurface(),this.mob.getZ());
            if(this.mob.getNavigation().isDone() && this.approachSite!=null
                    && (!WaterMovementGoal.safeLandingBelow(this.mob)
                        || NaturalMovementGoals.nightWaterSetbackRequired(this.mob)))
            {
                double x=this.approachSite.getX()+.5D,y=this.approachSite.getY(),z=this.approachSite.getZ()+.5D;
                var offset=new net.minecraft.world.phys.Vec3(x-this.mob.getX(),y-this.mob.getY(),z-this.mob.getZ());
                double distance=offset.length();
                if(distance<=Math.max(3.0D,this.mob.getBbWidth()*2.5D)){
                    if(distance<=.25D)return;
                    var step=offset.scale(Math.min(.2D,distance)/distance);
                    if(this.mob.level.noCollision(this.mob,this.mob.getBoundingBox().move(step)))
                        this.mob.move(net.minecraft.world.entity.MoverType.SELF,step);
                    else {rejectCachedShoreLanding();this.pathTicks=200;this.mob.getNavigation().stop();}
                    return;
                }
                this.mob.getMoveControl().setWantedPosition(x,y,z,this.shoreRoute?1.0D:.7D);
            }
        }
        @Override public void stop() { this.mob.getNavigation().stop(); this.landingPath = null; this.retryDelay=20; this.shoreRoute=false; this.directShoreRoute=false; this.shoreStallTicks=0; this.lastShorePosition=null; }

        private Path findLandingPath() {
            if (this.shoreRoute) return findShoreLandingPath();
            BlockPos origin = this.mob.blockPosition();
            double waterTop=WaterMovementGoal.restSurfaceAt(this.mob,origin);
            double searchHeight=Double.isFinite(waterTop)?Math.max(this.mob.getY(),waterTop+2):this.mob.getY();
            // Prefer the closest safe dry surface, and expand only if obstructed.
            for (int radius = 0; radius <= 25; radius++) {
                for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    double x = origin.getX() + dx + 0.5D, z = origin.getZ() + dz + 0.5D;
                    double surface = surfaceBelow(this.mob.level, x, searchHeight, z);
                    BlockPos floor = new BlockPos(x, surface - 0.001D, z);
                    if (!this.mob.level.hasChunkAt(floor) || !this.mob.level.getFluidState(floor).isEmpty()) continue;
                    BlockState state = this.mob.level.getBlockState(floor);
                    if (state.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)
                            || state.is(net.minecraft.world.level.block.Blocks.CACTUS)
                            || state.is(net.minecraft.world.level.block.Blocks.CAMPFIRE)
                            || state.is(net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE)) continue;
                    BlockPos feet = new BlockPos(x, Math.ceil(surface), z);
                    if (!WaterMovementGoal.safeLanding(this.mob,feet) || !this.mob.level.getFluidState(feet).isEmpty()
                            || !this.mob.level.getBlockState(feet).isAir()
                            || !this.mob.level.noCollision(this.mob, this.mob.getBoundingBox().move(
                                    x - this.mob.getX(), surface - this.mob.getY(), z - this.mob.getZ()))) continue;
                    var landingBox = this.mob.getBoundingBox().move(x - this.mob.getX(),
                            surface - this.mob.getY(), z - this.mob.getZ());
                    if (this.mob.isFlyingSpecies() && this.mob.isNightResting()
                            && SpeciesBehaviorProfiles.landsForNightRest(this.mob.getSpeciesSlug())
                            && !hasThreeBlockWaterClearance(this.mob, landingBox)) continue;
                    // Cross a shoreline above its collision lip before descending.
                    // A diagonal path straight to ground can wedge a wide
                    // flying hitbox against the bank while its center is wet.
                    double currentSurface=this.mob.naturalSurface();
                    BlockPos under=new BlockPos(this.mob.getX(),currentSurface-0.001D,this.mob.getZ());
                    boolean overFluid=!this.mob.level.getFluidState(under).isEmpty();
                    double approachY=Math.max(feet.getY()+2,Math.max(this.mob.getY(),Double.isFinite(waterTop)?waterTop+1:this.mob.getY()));
                    BlockPos approach=overFluid?new BlockPos(x,Math.ceil(approachY),z):feet;
                    if(!this.mob.level.noCollision(this.mob,this.mob.getBoundingBox().move(
                            x-this.mob.getX(),approach.getY()-this.mob.getY(),z-this.mob.getZ())))continue;
                    Path path = this.mob.getNavigation().createPath(approach, 0);
                    if (path != null && path.canReach()) { this.approachSite=approach; return path; }
                }
            }
            return null;
        }

        /** Searches a bounded 50×50×50 volume without querying unloaded chunks. */
        private Path findShoreLandingPath() {
            this.directShoreRoute = false;
            BlockPos origin = this.mob.blockPosition();
            double waterTop = WaterMovementGoal.restSurfaceAt(this.mob, origin);
            if (this.cachedLandingFeet != null && this.mob.tickCount < this.cachedLandingUntil
                    && this.mob.level.hasChunkAt(this.cachedLandingFeet)
                    && WaterMovementGoal.safeLanding(this.mob, this.cachedLandingFeet)) {
                Path cached = createShorePath(this.cachedLandingFeet, waterTop);
                if (cached != null) return cached;
            }
            this.cachedLandingFeet = null;
            this.rejectedShoreLandings.entrySet().removeIf(entry -> entry.getValue() < this.mob.tickCount);
            int originX = origin.getX(), originZ = origin.getZ(), originY = origin.getY();
            int lowY = Math.max(this.mob.level.getMinBuildHeight() + 1, originY - SHORE_VERTICAL_RADIUS - 1);
            int highY = Math.min(this.mob.level.getMaxBuildHeight() - 2, originY + SHORE_VERTICAL_RADIUS - 2);
            for (int radius = 0; radius <= SHORE_HORIZONTAL_RADIUS; radius++) {
                for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius || !NightShoreLandingRules.insideSearchVolume(dx, 0, dz)) continue;
                    int x = originX + dx, z = originZ + dz;
                    if (!this.mob.level.hasChunkAt(new BlockPos(x, originY, z))) continue;
                    for (int y = highY; y >= lowY; y--) {
                        if (!NightShoreLandingRules.insideSearchVolume(dx, y - originY, dz)) continue;
                        BlockPos support = new BlockPos(x, y, z);
                        var state = this.mob.level.getBlockState(support);
                        if (!this.mob.level.getFluidState(support).isEmpty()
                                || state.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)
                                || state.is(net.minecraft.world.level.block.Blocks.CACTUS)
                                || state.is(net.minecraft.world.level.block.Blocks.CAMPFIRE)
                                || state.is(net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE)) continue;
                        var shape = state.getCollisionShape(this.mob.level, support);
                        if (shape.isEmpty()) continue;
                        double top = Double.NEGATIVE_INFINITY;
                        for (var part : shape.toAabbs())
                            if (0.5D >= part.minX && 0.5D <= part.maxX && 0.5D >= part.minZ && 0.5D <= part.maxZ)
                                top = Math.max(top, part.maxY);
                        if (!Double.isFinite(top)) continue;
                        double surface = y + top;
                        BlockPos feet = new BlockPos(x, Mth.ceil(surface - 1.0E-6D), z);
                        if (!NightShoreLandingRules.insideSearchVolume(dx, feet.getY() - originY, dz)) continue;
                        if (this.rejectedShoreLandings.getOrDefault(feet, 0L) >= this.mob.tickCount) continue;
                        if (!WaterMovementGoal.safeLanding(this.mob, feet)) continue;
                        Path path = createShorePath(feet, waterTop);
                        if (path != null) return path;
                    }
                }
            }
            return null;
        }

        private Path createShorePath(BlockPos feet, double waterTop) {
            double surface = feet.getY();
            var landingBox = this.mob.getBoundingBox().move(feet.getX() + 0.5D - this.mob.getX(),
                    surface - this.mob.getY(), feet.getZ() + 0.5D - this.mob.getZ());
            if (!loadedBox(landingBox) || !hasThreeBlockWaterClearance(this.mob, landingBox)) return null;
            double approachY = Math.max(feet.getY() + 2.0D,
                    Math.max(this.mob.getY(), Double.isFinite(waterTop) ? waterTop + 1.0D : this.mob.getY()));
            BlockPos approach = new BlockPos(feet.getX(), Mth.ceil(approachY), feet.getZ());
            var approachBox = this.mob.getBoundingBox().move(feet.getX() + 0.5D - this.mob.getX(),
                    approach.getY() - this.mob.getY(), feet.getZ() + 0.5D - this.mob.getZ());
            if (!loadedBox(approachBox) || !this.mob.level.noCollision(this.mob, approachBox)) return null;
            Path path = this.mob.getNavigation().createPath(approach, 0);
            boolean directClear = directLineClear(approachBox);
            if (directClear) this.directShoreRoute = true;
            else if (path == null || !path.canReach()) return null;
            if (path == null) {
                if (!directClear) return null;
                path = new Path(java.util.List.of(), approach, true);
            }
            this.approachSite = approach;
            this.cachedLandingFeet = feet.immutable();
            this.cachedLandingUntil = this.mob.tickCount + 200L;
            return path;
        }

        private boolean loadedBox(net.minecraft.world.phys.AABB box) {
            int minChunkX = Mth.floor(box.minX) >> 4, maxChunkX = Mth.floor(box.maxX) >> 4;
            int minChunkZ = Mth.floor(box.minZ) >> 4, maxChunkZ = Mth.floor(box.maxZ) >> 4;
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++)
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++)
                    if (!this.mob.level.hasChunkAt(new BlockPos(chunkX << 4, Mth.floor(box.minY), chunkZ << 4))) return false;
            return true;
        }

        private void moveDirectlyToApproach() {
            double x = this.approachSite.getX() + 0.5D;
            double y = this.approachSite.getY();
            double z = this.approachSite.getZ() + 0.5D;
            var offset = new net.minecraft.world.phys.Vec3(x - this.mob.getX(),
                    y - this.mob.getY(), z - this.mob.getZ());
            double distance = offset.length();
            // FlyingMoveControl treats wide bodies as arrived while their center
            // is still short of a safe shoreline. Finish that final radius with
            // small collision-resolved steps instead of restarting the search.
            if (distance <= Math.max(3.0D, this.mob.getBbWidth() * 2.5D)) {
                if (distance <= 0.25D) {
                    this.directShoreRoute = false;
                    this.mob.getNavigation().stop();
                    return;
                }
                var step = offset.scale(Math.min(0.2D, distance) / distance);
                if (this.mob.level.noCollision(this.mob, this.mob.getBoundingBox().move(step))) {
                    this.mob.move(net.minecraft.world.entity.MoverType.SELF, step);
                } else {
                    this.directShoreRoute = false;
                    rejectCachedShoreLanding();
                    this.pathTicks = 200;
                    this.mob.getNavigation().stop();
                }
                return;
            }
            var targetBox = this.mob.getBoundingBox().move(x - this.mob.getX(), y - this.mob.getY(), z - this.mob.getZ());
            if (!directLineClear(targetBox)) {
                this.directShoreRoute = false;
                rejectCachedShoreLanding();
                this.pathTicks=200;
                this.mob.getNavigation().stop();
                return;
            }
            this.mob.getMoveControl().setWantedPosition(x, y, z, 1.0D);
        }

        private void rejectCachedShoreLanding() {
            if (this.cachedLandingFeet != null)
                this.rejectedShoreLandings.put(this.cachedLandingFeet.immutable(), this.mob.tickCount + 200L);
            this.cachedLandingFeet = null;
        }

        /** Fallback for unobstructed air corridors where native pathfinding cannot connect the nodes. */
        private boolean directLineClear(net.minecraft.world.phys.AABB targetBox) {
            var start = this.mob.getBoundingBox();
            double dx = targetBox.minX - start.minX, dy = targetBox.minY - start.minY, dz = targetBox.minZ - start.minZ;
            int steps = Math.max(1, Mth.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz) * 2.0D));
            for (int step = 1; step <= steps; step++) {
                double fraction = step / (double) steps;
                var probe = start.move(dx * fraction, dy * fraction, dz * fraction);
                if (!loadedBox(probe) || !this.mob.level.noCollision(this.mob, probe)) return false;
            }
            return true;
        }

        private void keepFlyingWhileSearching() {
            if (this.hoverMoveDelay-- > 0 && this.mob.getMoveControl().hasWanted()) return;
            this.hoverMoveDelay = 20;
            this.mob.getNavigation().stop();
            double waterTop = WaterMovementGoal.restSurfaceAt(this.mob, this.mob.blockPosition());
            double y = Math.max(this.mob.getY(), Double.isFinite(waterTop) ? waterTop + 2.0D : this.mob.getY() + 1.0D);
            for (int attempt = 0; attempt < 8; attempt++) {
                double angle = Math.toRadians(this.mob.getYRot()) + (this.hoverDirection++ & 7) * (Math.PI / 4.0D);
                double x = this.mob.getX() - Math.sin(angle) * 5.0D;
                double z = this.mob.getZ() + Math.cos(angle) * 5.0D;
                BlockPos target = new BlockPos(x, y, z);
                if (!this.mob.level.hasChunkAt(target)) continue;
                var targetBox = this.mob.getBoundingBox().move(x - this.mob.getX(), y - this.mob.getY(), z - this.mob.getZ());
                if (!this.mob.level.noCollision(this.mob, targetBox)) continue;
                this.mob.getMoveControl().setWantedPosition(x, y, z, 0.6D);
                return;
            }
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
