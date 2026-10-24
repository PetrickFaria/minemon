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
        if(this.waterCache==null||!column.equals(this.waterCache.column())||this.tickCount-this.waterCache.tick()>10)
            this.waterCache=new BlockPosCache(column,this.tickCount,WaterMovementGoal.surfaceAt(this,column));
        return this.waterCache.top();
    }
    @Override public void tick(){
        super.tick();
        if(this.level.isClientSide||!this.isInWater()||!this.naturalBehaviorActive())return;
        double top=this.waterSurface();
        if(!Double.isFinite(top))return;
        boolean surface=this.isNaturalSurface();
        double target=surface?top-Math.min(0.8D,this.getBbHeight()*0.3D):top-this.getBbHeight()-0.3D;
        // Surface and submerged correction share the swimming controller; do not
        // add another navigation goal or override horizontal pursuit.
        if(surface||this.getY()>target){
            double vy=net.minecraft.util.Mth.clamp(this.getDeltaMovement().y*0.5D+(target-this.getY())*0.05D,-0.09D,0.09D);
            this.setDeltaMovement(this.getDeltaMovement().x,vy,this.getDeltaMovement().z);
        }
    }

    @Override protected PathNavigation createNavigation(Level level) {
        // FollowOwnerGoal in 1.19.2 accepts only GroundPathNavigation or
        // FlyingPathNavigation. Retain that contract while using amphibious
        // nodes, as the stock amphibious navigation does for water/shore paths.
        return new GroundPathNavigation(this, level) {
            @Override protected PathFinder createPathFinder(int maxVisitedNodes) {
                this.nodeEvaluator = new AmphibiousNodeEvaluator(false);
                this.nodeEvaluator.setCanPassDoors(true);
                return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
            }

            @Override protected net.minecraft.world.phys.Vec3 getTempMobPos() {
                return MinemonAquaticCreature.this.isInWater()
                        ? MinemonAquaticCreature.this.position() : super.getTempMobPos();
            }

            @Override protected double getGroundY(net.minecraft.world.phys.Vec3 waypoint) {
                // Land navigation projects nodes onto the block below. Water
                // has no collision floor: that projection lowers an ascending
                // node onto the current Y and prevents any vertical progress.
                return MinemonAquaticCreature.this.isInWater()
                        ? waypoint.y : super.getGroundY(waypoint);
            }
        };
    }
}
