package br.com.petrickfaria.minemon.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.pathfinder.AmphibiousNodeEvaluator;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;

/** Water and shore navigation without fish-style dry-land damage. */
public final class MinemonAquaticCreature extends MinemonCreature {
    public MinemonAquaticCreature(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.lookControl = new net.minecraft.world.entity.ai.control.LookControl(this) {
    @Override public void tick(){
                float swimmingPitch=MinemonAquaticCreature.this.getXRot();super.tick();
                if(SpeciesBehaviorProfiles.isVisitor(getSpeciesSlug()) && MinemonAquaticCreature.this.isInWater() && !getNavigation().isDone())MinemonAquaticCreature.this.setXRot(swimmingPitch);
            }
            @Override protected boolean resetXRotOnTick() {
                // LookControl ticks after MoveControl. Its land default resets
                // pitch to zero every tick, preventing smooth swimming from
                // accumulating the inclination needed to ascend or descend.
                return !MinemonAquaticCreature.this.isInWater()
                        || MinemonAquaticCreature.this.getNavigation().isDone();
            }
        };
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.1F, 0.5F, false) {
            private final MoveControl land = new MoveControl(MinemonAquaticCreature.this);

            @Override public void setWantedPosition(double x, double y, double z, double speed) {
                super.setWantedPosition(x, y, z, speed);
                this.land.setWantedPosition(x, y, z, speed);
            }

            @Override public boolean hasWanted() {
                return MinemonAquaticCreature.this.isInWater() ? super.hasWanted() : this.land.hasWanted();
            }

            @Override public void strafe(float forward, float sideways) {
                super.strafe(forward, sideways);
                this.land.strafe(forward, sideways);
            }

            @Override public void tick() {
                // Swimming control alone does not perform ordinary land jumps.
                // Use Minecraft's land controller on shores/steps and smooth
                // three-dimensional steering after entering the water.
                if (MinemonAquaticCreature.this.isInWater()) super.tick();
                else this.land.tick();
            }
        };
    }

    @Override public void travel(net.minecraft.world.phys.Vec3 input){
        // LivingEntity otherwise subtracts 0.005 vertical velocity every water
        // tick. Neutral buoyancy lets the swimming controller reach higher
        // nodes; ordinary land gravity is restored immediately on shore.
        if(!this.level.isClientSide)this.setNoGravity(this.isInWater());
        super.travel(input);
    }

    private BlockPosCache waterCache;
    private record BlockPosCache(net.minecraft.core.BlockPos column,int tick,double top){}
    public double waterSurface(){
        var column=this.blockPosition();
        if(this.waterCache==null||!column.equals(this.waterCache.column())||this.tickCount-this.waterCache.tick()>10){
            double top=WaterMovementGoal.surfaceAt(this,column);
            if(!Double.isFinite(top))
                top=nearbyWaterSurface();
            this.waterCache=new BlockPosCache(column,this.tickCount,top);
        }
        return this.waterCache.top();
    }
    private double nearbyWaterSurface(){
        var box=this.getBoundingBox();
        int minX=net.minecraft.util.Mth.floor(box.minX-1.001D),maxX=net.minecraft.util.Mth.floor(box.maxX+1.001D);
        int minZ=net.minecraft.util.Mth.floor(box.minZ-1.001D),maxZ=net.minecraft.util.Mth.floor(box.maxZ+1.001D);
        int minY=net.minecraft.util.Mth.floor(this.getY())-1;
        int maxY=net.minecraft.util.Mth.ceil(this.getY()+this.getBbHeight())+1;
        double best=Double.NaN,bestScore=Double.POSITIVE_INFINITY;
        double bodyCenter=this.getY()+this.getBbHeight()*.5D;
        for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++){
            double dx=x+.5D-this.getX(),dz=z+.5D-this.getZ();
            double horizontal=dx*dx+dz*dz;
            for(int y=minY;y<=maxY;y++){
                double top=WaterMovementGoal.surfaceAt(this,new net.minecraft.core.BlockPos(x,y,z));
                if(!Double.isFinite(top))continue;
                double score=horizontal+.25D*Math.abs(top-(bodyCenter+1.0D));
                if(score<bestScore){bestScore=score;best=top;}
            }
        }
        return best;
    }
    @Override public void tick(){
        super.tick();
        if(this.isDefeated()||this.level.isClientSide)return;
        // A completed surface route can leave SmoothSwimmingMoveControl holding
        // its last horizontal waypoint. Clear it even if the body has just
        // crossed the waterline and the current column has no measurable top.
        if(this.getSpeciesSlug().equals("gyarados")&&this.isNaturalSurface()
                &&!this.isSleeping()&&!this.isNightResting()&&this.getNavigation().isDone()){
            double holdY=this.getY();
            if(this.isInWater()){
                double localTop=this.waterSurface();
                if(Double.isFinite(localTop))
                    holdY=localTop-WaterMovementGoal.preferredSurfaceDepth(this);
            }
            this.getMoveControl().setWantedPosition(this.getX(),holdY,this.getZ(),0.5D);
            // The completed route no longer owns horizontal motion. Damp its
            // residual momentum so inertia cannot carry Gyarados over a bank
            // after the controller has already cleared the stale waypoint.
            var motion=this.getDeltaMovement();
            this.setDeltaMovement(motion.x*0.2D,motion.y,motion.z*0.2D);
        }
        if(!this.isInWater()||(!this.naturalBehaviorActive()&&!this.isSleeping()&&!this.isNightResting()))return;
        double top=this.waterSurface();
        if(!Double.isFinite(top))return;
        if(this.isSleeping()||this.isNightResting()){
            // Night rest has its own species-specific depth. Ease toward it
            // before the settled check marks the creature asleep, then keep
            // holding that depth for the duration of sleep. In particular,
            // visitors that entered the water during their land cooldown are
            // marked EXIT_WATER (a surface phase), but must remain asleep at
            // the safe bottom until dawn rather than being pulled upward.
            double restY=WaterMovementGoal.waterRestY(this);
            if(Double.isFinite(restY)){
                var velocity=this.getDeltaMovement();
                double vy=net.minecraft.util.Mth.clamp(velocity.y*0.5D+(restY-this.getY())*0.05D,-0.09D,0.09D);
                this.setDeltaMovement(velocity.x*0.5D,vy,velocity.z*0.5D);
            }
            return;
        }
        // Awake swimmers follow the surface in their own column. A stale
        // navigation target across a waterfall can refer to water many blocks
        // higher and must not pull Gyarados away from its local surface.
        boolean surface=this.isNaturalSurface();
        double target=surface?top-WaterMovementGoal.preferredSurfaceDepth(this):top-this.getBbHeight()-0.3D;
        // Surface and submerged correction share the swimming controller; do not
        // add another navigation goal or override horizontal pursuit.
        if(!ShoreTraversal.exiting(this)&&(surface||this.getY()>target)){
            double vy=net.minecraft.util.Mth.clamp(this.getDeltaMovement().y*0.5D+(target-this.getY())*0.05D,-0.09D,0.09D);
            this.setDeltaMovement(this.getDeltaMovement().x,vy,this.getDeltaMovement().z);
        }
    }

    @Override protected PathNavigation createNavigation(Level level) {
        // FollowOwnerGoal in 1.19.2 accepts only GroundPathNavigation or
        // FlyingPathNavigation. Retain that contract while using amphibious
        // nodes, as the stock amphibious navigation does for water/shore paths.
        return new GroundPathNavigation(this, level) {
            @Override public net.minecraft.world.level.pathfinder.Path createPath(net.minecraft.core.BlockPos destination,int reach){
                // The native path-length limit can reject a valid distant
                // amphibious route even inside the geometric search radius.
                // Only distant water searches use the larger bounded budget;
                // ordinary land, swim, follow and combat paths stay unchanged.
                if(destination.distSqr(this.mob.blockPosition())>32.0D*32.0D
                        &&level.getFluidState(destination).is(net.minecraft.tags.FluidTags.WATER)){
                    this.setMaxVisitedNodesMultiplier(8.0F);
                    try{return super.createPath(destination,reach,256);}
                    finally{this.resetMaxVisitedNodesMultiplier();}
                }
                return super.createPath(destination,reach);
            }
            @Override protected void doStuckDetection(net.minecraft.world.phys.Vec3 position){
                // 1.19.2 retains timeoutTimer when the next node changes. On
                // long routes, time spent at earlier nodes eventually cancels
                // a moving entity's path. Keep the timeout local to its node;
                // vanilla's distance and blocked-node checks still run.
                if(this.path!=null&&!this.path.isDone()
                        &&!this.path.getNextNodePos().equals(this.timeoutCachedNode))
                    this.timeoutTimer=0L;
                super.doStuckDetection(position);
            }
            @Override protected PathFinder createPathFinder(int maxVisitedNodes) {
                this.nodeEvaluator = SpeciesBehaviorProfiles.isVisitor(getSpeciesSlug())?new ShoreNodeEvaluator():new AmphibiousNodeEvaluator(false);
                this.nodeEvaluator.setCanPassDoors(true);
                return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
            }

            @Override protected void followThePath() {
                // Follow the native path's actual steering point. Land-style
                // width rounding can reject an exactly reached fractional-width
                // node; its one-block vertical tolerance can also finish a dive
                // before reaching the bottom. Water needs 3D arrival checks.
                if (this.mob.isInWater() && this.path != null && !this.path.isDone()) {
                    var point = this.path.getNextEntityPos(this.mob);
                    if (Math.abs(this.mob.getX()-point.x)<0.35D
                            && Math.abs(this.mob.getZ()-point.z)<0.35D
                            && Math.abs(this.mob.getY()-this.getGroundY(point))<0.30D) {
                        this.path.advance();
                    }
                    this.doStuckDetection(this.getTempMobPos());
                    return;
                }
                super.followThePath();
            }

            @Override protected net.minecraft.world.phys.Vec3 getTempMobPos() {
                return MinemonAquaticCreature.this.isInWater()
                        ? MinemonAquaticCreature.this.position() : super.getTempMobPos();
            }

            @Override protected double getGroundY(net.minecraft.world.phys.Vec3 waypoint) {
                // Land navigation projects nodes onto the block below. Water
                // has no collision floor: that projection lowers an ascending
                // node onto the current Y and prevents any vertical progress.
                if (MinemonAquaticCreature.this.isInWater()) {
                    if (MinemonAquaticCreature.this.naturalBehaviorActive()
                            && MinemonAquaticCreature.this.isNaturalSurface()
                            && (!SpeciesBehaviorProfiles.isVisitor(getSpeciesSlug()) || level.getFluidState(new net.minecraft.core.BlockPos(waypoint)).is(net.minecraft.tags.FluidTags.WATER)
                            && !ShoreTraversal.exiting(MinemonAquaticCreature.this))) {
                        double surface=WaterMovementGoal.surfaceAt(MinemonAquaticCreature.this,
                                new net.minecraft.core.BlockPos(waypoint));
                        // Surface depth is fractional and model-sized. Steering
                        // toward a rounded path Y must not fight the existing
                        // continuous surface-depth correction.
                        if (Double.isFinite(surface)) return surface
                                - WaterMovementGoal.preferredSurfaceDepth(MinemonAquaticCreature.this);
                    }
                    return waypoint.y;
                }
                return super.getGroundY(waypoint);
            }
        };
    }
}
