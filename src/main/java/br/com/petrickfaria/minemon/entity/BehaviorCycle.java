package br.com.petrickfaria.minemon.entity;

import br.com.petrickfaria.minemon.entity.SpeciesBehaviorProfiles.Profile;
import net.minecraft.nbt.CompoundTag;

import java.util.Random;

/** Per-entity, persisted routine clock. Combat, sleep, sitting and unloaded time pause it. */
public final class BehaviorCycle {
    public enum Phase { LAND, TAKEOFF, AIR, LANDING, SEEK_WATER, SUBMERGED, SURFACE, EXIT_WATER, COOLDOWN }
    public record Observation(boolean active, boolean ground, boolean flightBand, boolean wet,
                              boolean submerged, boolean surface, boolean dryGround, boolean canDive) {}

    private static final int TICKS_PER_MINUTE = 1_200;
    private final Profile profile;
    private final String speciesSlug;
    private final Random random;
    private Phase phase;
    private int ticks;
    private int stage;
    private boolean gaitWalking = true;
    private int gaitTicks;
    private int aquaticTicks;
    private int surfaceTicks;
    private int surfaceVisits;
    private int surfaceVisitLimit;
    private int nextSurfaceAt = Integer.MAX_VALUE;

    public BehaviorCycle(Profile profile) { this(profile, null, new Random()); }
    public BehaviorCycle(Profile profile, String speciesSlug) { this(profile, speciesSlug, new Random()); }
    public BehaviorCycle(Profile profile, String speciesSlug, Random random) {
        this.profile = profile;
        this.speciesSlug = speciesSlug;
        this.random = random;
        this.phase = switch (profile) {
            case WATER_VISITOR -> Phase.COOLDOWN;
            case AQUATIC_CYCLE, AQUATIC_BOTTOM -> Phase.SUBMERGED;
            case AQUATIC_NEAR_SURFACE, AQUATIC_SURFACE -> Phase.SURFACE;
            case GROUND_FLIGHT, GROUND_FLOAT_CYCLE -> Phase.LAND;
            case GROUND -> Phase.LAND;
            default -> Phase.AIR;
        };
        this.gaitTicks = nextGaitDuration();
    }

    public Phase phase() { return phase; }
    public int ticks() { return ticks; }
    public int stage() { return stage; }
    public boolean walking() { return gaitWalking; }
    public int gaitTicks() { return gaitTicks; }
    public int aquaticTicks() { return aquaticTicks; }
    public int surfaceTicks() { return surfaceTicks; }
    public int surfaceVisits() { return surfaceVisits; }
    public int surfaceVisitLimit() { return surfaceVisitLimit; }

    public double minimumAltitude() {
        return switch (profile) {
            case FLYER, GROUND_FLIGHT -> stage == 0 ? 3.0D : 6.0D;
            case FLOAT_CYCLE, GROUND_FLOAT_CYCLE -> stage == 0 ? 1.0D : 4.0D;
            case FLOAT_HALF -> SpeciesBehaviorProfiles.nightRestHoverHeight(speciesSlug == null ? "abra" : speciesSlug);
            case FLOAT_LOW -> 0.5D;
            default -> 0.0D;
        };
    }

    public double maximumAltitude() {
        return switch (profile) {
            case FLYER, GROUND_FLIGHT -> stage == 0 ? 5.0D : 9.0D;
            case FLOAT_CYCLE, GROUND_FLOAT_CYCLE -> stage == 0 ? 3.0D : 6.0D;
            case FLOAT_HALF -> SpeciesBehaviorProfiles.nightRestHoverHeight(speciesSlug == null ? "abra" : speciesSlug);
            case FLOAT_LOW -> 1.0D;
            default -> 0.0D;
        };
    }

    public boolean flying() {
        return speciesSlug != null && SpeciesBehaviorProfiles.isAirborne(speciesSlug)
                || (profile == Profile.GROUND_FLIGHT || profile == Profile.GROUND_FLOAT_CYCLE)
                && phase != Phase.LAND;
    }

    public boolean landing() {
        return (profile == Profile.GROUND_FLIGHT || profile == Profile.GROUND_FLOAT_CYCLE)
                && phase == Phase.LANDING;
    }

    private void enter(Phase next) {
        phase = next;
        ticks = 0;
        if (next == Phase.LAND || next == Phase.COOLDOWN) resetGait();
    }

    private boolean hasGroundRoutine() {
        return switch (profile) {
            case GROUND, WATER_VISITOR, GROUND_FLIGHT, GROUND_FLOAT_CYCLE ->
                    phase == Phase.LAND || profile == Profile.WATER_VISITOR
                            && (phase == Phase.COOLDOWN || phase == Phase.SEEK_WATER);
            default -> false;
        };
    }

    private void tickGait(boolean active, boolean onGround) {
        if (!active || !onGround || !hasGroundRoutine()) return;
        if (--gaitTicks <= 0) {
            gaitWalking = !gaitWalking;
            gaitTicks = nextGaitDuration();
        }
    }

    private int nextGaitDuration() {
        // Moving 3–5 seconds; stationary 3–7 seconds, independently per entity.
        return gaitWalking ? 60 + random.nextInt(41) : 60 + random.nextInt(81);
    }

    private void resetGait() {
        gaitWalking = true;
        gaitTicks = nextGaitDuration();
    }

    public void tick(Observation observation) {
        if (!observation.active()) return;
        tickGait(true, observation.ground());

        switch (profile) {
            case GROUND -> { }
            case FLYER -> tickAltitude(observation, TICKS_PER_MINUTE * 3, TICKS_PER_MINUTE * 3);
            case FLOAT_CYCLE -> tickAltitude(observation, TICKS_PER_MINUTE * 5, TICKS_PER_MINUTE * 3);
            case FLOAT_HALF, FLOAT_LOW, AQUATIC_BOTTOM, AQUATIC_NEAR_SURFACE, AQUATIC_SURFACE -> { }
            case GROUND_FLIGHT -> tickGroundFlight(observation, TICKS_PER_MINUTE * 5,
                    TICKS_PER_MINUTE * 3, TICKS_PER_MINUTE * 2);
            case GROUND_FLOAT_CYCLE -> tickGroundFloat(observation);
            case WATER_VISITOR -> tickWaterVisitor(observation);
            case AQUATIC_CYCLE -> tickAquaticCycle(observation);
        }
    }

    private void tickAltitude(Observation observation, int lowDuration, int highDuration) {
        if (!observation.flightBand()) return;
        int limit = stage == 0 ? lowDuration : highDuration;
        if (++ticks >= limit) {
            stage = 1 - stage;
            ticks = 0;
        }
    }

    private void tickGroundFlight(Observation o, int landDuration, int lowDuration, int highDuration) {
        switch (phase) {
            case LAND -> {
                if (o.ground() && ++ticks >= landDuration) { stage = 0; enter(Phase.TAKEOFF); }
            }
            case TAKEOFF -> { if (o.flightBand()) enter(Phase.AIR); }
            case AIR -> {
                int limit = stage == 0 ? lowDuration : highDuration;
                if (o.flightBand() && ++ticks >= limit) {
                    if (stage == 0) { stage = 1; ticks = 0; }
                    else enter(Phase.LANDING);
                }
            }
            case LANDING -> { if (o.dryGround()) { stage = 0; enter(Phase.LAND); } }
            default -> enter(Phase.LAND);
        }
    }

    private void tickGroundFloat(Observation o) {
        switch (phase) {
            case LAND -> {
                if (o.ground() && ++ticks >= TICKS_PER_MINUTE * 5) { stage = 0; enter(Phase.TAKEOFF); }
            }
            case TAKEOFF -> { if (o.flightBand()) enter(Phase.AIR); }
            case AIR -> {
                int limit = stage == 0 ? TICKS_PER_MINUTE * 3
                        : "gengar".equals(speciesSlug) ? TICKS_PER_MINUTE * 2 : 0;
                if (limit > 0 && o.flightBand() && ++ticks >= limit) {
                    if (stage == 0 && "gengar".equals(speciesSlug)) { stage = 1; ticks = 0; }
                    else enter(Phase.LANDING);
                }
            }
            case LANDING -> { if (o.dryGround()) { stage = 0; enter(Phase.LAND); } }
            default -> enter(Phase.LAND);
        }
    }

    private void tickWaterVisitor(Observation o) {
        switch (phase) {
            case COOLDOWN -> {
                if (o.wet()) enter(Phase.EXIT_WATER);
                else if (o.ground() && ++ticks >= TICKS_PER_MINUTE * 5) enter(Phase.SEEK_WATER);
            }
            case SEEK_WATER -> { if (o.wet()) beginVisitorWater(o.canDive()); }
            case SUBMERGED, SURFACE -> {
                if (!o.wet()) return;
                aquaticTicks++;
                if (phase == Phase.SURFACE && o.surface()) surfaceTicks++;
                if (aquaticTicks >= TICKS_PER_MINUTE * 5) { enter(Phase.EXIT_WATER); return; }
                if (phase == Phase.SURFACE) {
                    if (surfaceTicks >= 600) { phase = Phase.SUBMERGED; surfaceTicks = 0; }
                } else if (surfaceVisits < surfaceVisitLimit && aquaticTicks >= nextSurfaceAt && o.canDive()) {
                    phase = Phase.SURFACE;
                    surfaceTicks = 0;
                    surfaceVisits++;
                    nextSurfaceAt = surfaceVisits == 1
                            ? aquaticTicks + 900 + random.nextInt(901) : Integer.MAX_VALUE;
                }
            }
            case EXIT_WATER -> { if (o.dryGround()) { aquaticTicks = 0; enter(Phase.COOLDOWN); } }
            default -> enter(Phase.COOLDOWN);
        }
    }

    private void beginVisitorWater(boolean canDive) {
        aquaticTicks = 0;
        surfaceTicks = 0;
        surfaceVisits = 0;
        surfaceVisitLimit = canDive ? random.nextInt(3) : 0;
        nextSurfaceAt = surfaceVisitLimit == 0 ? Integer.MAX_VALUE : 900 + random.nextInt(1_801);
        enter(canDive ? Phase.SUBMERGED : Phase.SURFACE);
    }

    private void tickAquaticCycle(Observation o) {
        if (!o.wet()) return;
        if (phase == Phase.SUBMERGED) {
            if (o.submerged() && ++ticks >= TICKS_PER_MINUTE * 5 && o.canDive()) enter(Phase.SURFACE);
        } else if (phase == Phase.SURFACE) {
            if (++ticks >= 600) enter(Phase.SUBMERGED);
        }
    }

    public void chooseWaterDepth(boolean surface) {
        if (profile == Profile.WATER_VISITOR && (phase == Phase.SUBMERGED || phase == Phase.SURFACE))
            phase = surface ? Phase.SURFACE : Phase.SUBMERGED;
    }

    public void restore(String savedPhase, int elapsed, int altitudeStage) {
        try { phase = Phase.valueOf(savedPhase); }
        catch (IllegalArgumentException exception) { phase = initialPhase(); }
        stage = Math.max(0, Math.min(1, altitudeStage));
        int limit = phaseLimit();
        ticks = limit <= 0 ? Math.max(0, elapsed) : Math.max(0, Math.min(limit - 1, elapsed));
        if (profile == Profile.GROUND_FLIGHT || profile == Profile.GROUND_FLOAT_CYCLE) {
            if (phase != Phase.LAND && phase != Phase.TAKEOFF && phase != Phase.AIR && phase != Phase.LANDING) enter(Phase.LAND);
        } else if (profile == Profile.WATER_VISITOR
                && phase != Phase.SEEK_WATER && phase != Phase.SUBMERGED && phase != Phase.SURFACE
                && phase != Phase.EXIT_WATER && phase != Phase.COOLDOWN) enter(Phase.COOLDOWN);
        else if (profile == Profile.AQUATIC_CYCLE && phase != Phase.SUBMERGED && phase != Phase.SURFACE) enter(Phase.SUBMERGED);
    }

    private Phase initialPhase() {
        return switch (profile) {
            case WATER_VISITOR -> Phase.COOLDOWN;
            case AQUATIC_CYCLE, AQUATIC_BOTTOM -> Phase.SUBMERGED;
            case AQUATIC_NEAR_SURFACE, AQUATIC_SURFACE -> Phase.SURFACE;
            case GROUND_FLIGHT, GROUND_FLOAT_CYCLE, GROUND -> Phase.LAND;
            default -> Phase.AIR;
        };
    }

    private int phaseLimit() {
        return switch (phase) {
            case LAND, COOLDOWN -> TICKS_PER_MINUTE * 5;
            case AIR -> stage == 0 ? TICKS_PER_MINUTE * 3 : profile == Profile.GROUND_FLIGHT ? TICKS_PER_MINUTE * 2
                    : profile == Profile.GROUND_FLOAT_CYCLE && "gengar".equals(speciesSlug) ? TICKS_PER_MINUTE * 2 : TICKS_PER_MINUTE * 3;
            case SUBMERGED -> TICKS_PER_MINUTE * 5;
            case SURFACE -> profile == Profile.AQUATIC_CYCLE ? 600 : TICKS_PER_MINUTE * 5;
            default -> 0;
        };
    }

    public void save(CompoundTag tag) {
        tag.putString("NaturalBehaviorPhase", phase.name());
        tag.putInt("NaturalBehaviorTicks", ticks);
        tag.putInt("NaturalAltitudeStage", stage);
        tag.putBoolean("NaturalGaitWalking", gaitWalking);
        tag.putInt("NaturalGaitTicks", gaitTicks);
        tag.putInt("NaturalAquaticTicks", aquaticTicks);
        tag.putInt("NaturalSurfaceTicks", surfaceTicks);
        tag.putInt("NaturalSurfaceVisits", surfaceVisits);
        tag.putInt("NaturalSurfaceVisitLimit", surfaceVisitLimit);
        tag.putInt("NaturalNextSurfaceAt", nextSurfaceAt);
    }

    public void restore(CompoundTag tag) {
        restore(tag.getString("NaturalBehaviorPhase"), tag.getInt("NaturalBehaviorTicks"), tag.getInt("NaturalAltitudeStage"));
        if (tag.contains("NaturalGaitWalking")) gaitWalking = tag.getBoolean("NaturalGaitWalking");
        if (tag.contains("NaturalGaitTicks")) gaitTicks = Math.max(0, tag.getInt("NaturalGaitTicks"));
        aquaticTicks = Math.max(0, Math.min(TICKS_PER_MINUTE * 5, tag.getInt("NaturalAquaticTicks")));
        surfaceTicks = Math.max(0, Math.min(600, tag.getInt("NaturalSurfaceTicks")));
        surfaceVisits = Math.max(0, Math.min(2, tag.getInt("NaturalSurfaceVisits")));
        surfaceVisitLimit = Math.max(surfaceVisits, Math.min(2, tag.getInt("NaturalSurfaceVisitLimit")));
        nextSurfaceAt = tag.contains("NaturalNextSurfaceAt") ? tag.getInt("NaturalNextSurfaceAt") : Integer.MAX_VALUE;
    }
}
