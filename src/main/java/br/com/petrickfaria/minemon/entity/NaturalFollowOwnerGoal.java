package br.com.petrickfaria.minemon.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/** Native navigation only: distance never authorizes relocation. */
final class NaturalFollowOwnerGoal extends Goal {
    private final MinemonCreature mob;
    private LivingEntity owner;
    private boolean running;
    private boolean moving;
    private double speed;
    private int recalculate;

    NaturalFollowOwnerGoal(MinemonCreature mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean eligible() {
        LivingEntity current = mob.getOwner();
        if (current == null || !current.isAlive() || current.isSpectator()
                || current.level != mob.level || mob.isDefeated() || mob.isOrderedToSit() || mob.isSleeping()
                || mob.isNightResting() || mob.isPassenger() || mob.getTarget() != null) return false;
        return !mob.isAquaticSpecies() || SpeciesBehaviorProfiles.isVisitor(mob.getSpeciesSlug())
                || current.isInWaterOrBubble() && mob.isInWaterOrBubble();
    }

    @Override public boolean canUse() { return eligible(); }
    @Override public boolean canContinueToUse() { return eligible() && owner == mob.getOwner(); }
    @Override public void start() { owner = mob.getOwner(); mob.setFollowingOwner(true); mob.stopSpontaneousRun(); running = false; moving = mob.distanceToSqr(owner) > 25.0D; speed = 0.8D; recalculate = 0; }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void tick() {
        double distance = mob.distanceToSqr(owner);
        if (moving && distance <= 20.25D) moving = false;
        else if (!moving && distance > 25.0D) moving = true;
        if (!moving) {
            mob.getNavigation().stop(); mob.setFollowingRun(false); running=false; speed=0.8D; return;
        }
        if (distance > 100.0D) running = true;
        else if (distance < 90.25D) running = false;
        double target = running ? 1.9375D : 0.8D;
        speed += Math.max(-0.04D, Math.min(0.04D, target - speed));
        mob.setFollowingRun(running);
        mob.getLookControl().setLookAt(owner, 10.0F, mob.getMaxHeadXRot());
        mob.getNavigation().setSpeedModifier(speed);
        if (--recalculate <= 0) {
            recalculate = 10;
            if(mob.isFlyingSpecies()){
                double surface=NaturalMovementGoals.surfaceBelow(mob.level,owner.getX(),Math.max(owner.getY()+4,mob.getY()),owner.getZ());
                mob.getNavigation().moveTo(owner.getX(),surface+mob.followAltitude(),owner.getZ(),speed);
            }else mob.getNavigation().moveTo(owner, speed);
        }
    }
    @Override public void stop() { mob.getNavigation().stop(); mob.setFollowingRun(false); mob.setFollowingOwner(false); owner = null; }
}
