package br.com.petrickfaria.minemon.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.pathfinder.AmphibiousNodeEvaluator;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;

/** Water and shore navigation without fish-style dry-land damage. */
public final class MinemonAquaticCreature extends MinemonCreature {
    public MinemonAquaticCreature(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.1F, 0.5F, false);
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
        };
    }
}
