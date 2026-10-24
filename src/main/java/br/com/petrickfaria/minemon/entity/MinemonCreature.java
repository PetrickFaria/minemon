package br.com.petrickfaria.minemon.entity;

import br.com.petrickfaria.minemon.config.MinemonConfig;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AgeableMob;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import java.util.UUID;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.AnimationState;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

/** Shared wolf-like companion behavior. This first registered species is the integration fixture. */
public class MinemonCreature extends TamableAnimal implements IAnimatable {
    private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(MinemonCreature.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SPONTANEOUS_RUN = SynchedEntityData.defineId(MinemonCreature.class, EntityDataSerializers.BOOLEAN);
    private static final java.util.UUID SPONTANEOUS_RUN_SPEED_ID = java.util.UUID.fromString("b4db65e0-39b8-43a6-a454-3c6e9f16d920");
    private static final AttributeModifier SPONTANEOUS_RUN_SPEED = new AttributeModifier(
            SPONTANEOUS_RUN_SPEED_ID, "Minemon spontaneous natural run", 0.55D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final int WILD_AGGRESSION_DURATION_TICKS = 20 * 20;
    private static final int WILD_MAX_CHASE_DISTANCE = 24;
    private static final int WILD_UNREACHABLE_LIMIT_TICKS = 20 * 5;
    private final AnimationFactory animationFactory = GeckoLibUtil.createFactory(this);
    private int wildAggressionTicks;
    private int wildUnreachableTicks;
    private int blinkCooldownTicks;
    private boolean blinkPending;
    private int attackAnimationTicks;
    private int attackSequence;
    private int playedAttackSequence;
    private int spontaneousRunTicks;
    private int spontaneousRunCooldown = 50 + this.getRandom().nextInt(100);
    private int spontaneousRunCount;
    // Client-only diagnostic sample used by the automated locomotion smoke test.
    private String selectedAnimationForValidation;
    private String selectedTailFlameAnimationForValidation;

    public MinemonCreature(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.blinkCooldownTicks = 80 + this.getRandom().nextInt(120);
    }

    public String getSpeciesSlug() {
        net.minecraft.resources.ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(this.getType());
        return key == null ? "bulbasaur" : key.getPath();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    public static AttributeSupplier.Builder createAttributes(SpeciesDefinitions.Definition d) {
        return createAttributes().add(Attributes.MAX_HEALTH,d.health()).add(Attributes.ATTACK_DAMAGE,d.attack()).add(Attributes.MOVEMENT_SPEED,d.speed()).add(Attributes.FLYING_SPEED,0.4D);
    }
    public boolean isFlyingSpecies() { return SpeciesBehaviorProfiles.isAirborne(this.getSpeciesSlug()); }
    public boolean isAquaticSpecies() { return SpeciesBehaviorProfiles.isAquatic(this.getSpeciesSlug()); }
    public boolean isSpontaneouslyRunning() { return this.entityData.get(SPONTANEOUS_RUN); }
    public void setSpontaneousRunForValidation(boolean running) {
        if (Boolean.getBoolean("minemon.clientSmokeTest") && !this.level.isClientSide
                && !this.isFlyingSpecies() && !this.isAquaticSpecies()) {
            this.entityData.set(SPONTANEOUS_RUN, running);
            this.spontaneousRunTicks = running ? 100 : 0;
        }
    }

    public boolean isSleeping() { return this.entityData.get(SLEEPING); }
    public void setSleeping(boolean sleeping) {
        this.entityData.set(SLEEPING, sleeping);
        if (sleeping) {
            this.getNavigation().stop(); this.setTarget(null); this.stopSpontaneousRun();
            if (this instanceof MinemonFlyingCreature) this.setNoGravity(false);
        } else if (this instanceof MinemonFlyingCreature) {
            this.setNoGravity(true);
        }
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SLEEPING, false);
        this.entityData.define(SPONTANEOUS_RUN, false);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("MinemonSleeping", this.isSleeping());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setSleeping(tag.getBoolean("MinemonSleeping"));
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return (AgeableMob) this.getType().create(level);
    }

    @Override
    protected void registerGoals() {
        if (!this.isAquaticSpecies()) this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, this.isFlyingSpecies()));
        if (this.isFlyingSpecies()) {
            this.goalSelector.addGoal(0, new NaturalMovementGoals.SleepLanding(this));
            this.goalSelector.addGoal(4, new NaturalMovementGoals.AirPatrol(this));
        } else if (this.isAquaticSpecies()) {
            this.getNavigation().setCanFloat(true);
            this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
            this.goalSelector.addGoal(4, new NaturalMovementGoals.SeekWater(this));
            this.goalSelector.addGoal(5, new NaturalMovementGoals.AquaticLandWander(this));
        } else {
            this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.8D));
        }
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // Wild creatures only acquire a target after being hurt. Tamed creatures also protect
        // their owner and assist against the owner's current target.
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this) {
            @Override public boolean canUse() {
                return (MinemonCreature.this.isTame() || MinemonCreature.this.wildAggressionTicks > 0) && super.canUse();
            }
            @Override public boolean canContinueToUse() {
                return (MinemonCreature.this.isTame() || MinemonCreature.this.wildAggressionTicks > 0) && super.canContinueToUse();
            }
        });
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target == null || target == this || this.isOwnerEntity(target)) return false;
        // Ordered sitting companions may defend themselves or their owner at close
        // range, but must not start walking after a distant target.
        if (this.isTame() && this.isOrderedToSit() && target.distanceToSqr(this) > 16.0D) return false;
        if (target instanceof MinemonCreature other) {
            UUID ownerId = this.getOwnerUUID();
            UUID otherOwnerId = other.getOwnerUUID();
            if (ownerId != null && ownerId.equals(otherOwnerId)) return false;
        }
        return super.canAttack(target);
    }

    private boolean isOwnerEntity(LivingEntity target) {
        UUID ownerId = this.getOwnerUUID();
        return ownerId != null && ownerId.equals(target.getUUID());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!this.isTame() && held.is(Items.BONE)) {
            if (!this.level.isClientSide) {
                if (!player.getAbilities().instabuild) held.shrink(1);
                int chance = MinemonConfig.TAME_CHANCE_PERCENT.get();
                if (this.getRandom().nextInt(100) < chance) {
                    this.tame(player);
                    this.setOrderedToSit(true);
                    this.setTarget(null);
                    this.getNavigation().stop();
                    this.level.broadcastEntityEvent(this, (byte) 7);
                } else {
                    this.level.broadcastEntityEvent(this, (byte) 6);
                }
            }
            return InteractionResult.sidedSuccess(this.level.isClientSide);
        }

        if (this.isTame() && this.isOwnedBy(player) && !this.isFood(held)) {
            if (!this.level.isClientSide) {
                this.setOrderedToSit(!this.isOrderedToSit());
                this.getNavigation().stop();
                this.setTarget(null);
            }
            return InteractionResult.sidedSuccess(this.level.isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level.isClientSide && source.getEntity() instanceof LivingEntity attacker
                && this.canAttack(attacker)) {
            this.setTarget(attacker);
            if (!this.isTame()) {
                this.wildAggressionTicks = WILD_AGGRESSION_DURATION_TICKS;
                this.wildUnreachableTicks = 0;
            }
        }
        return hurt;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isSleeping()) {
            if (!this.isFlyingSpecies()) this.getNavigation().stop();
            this.setTarget(null);
        }
        if (this.level.isClientSide) {
            if (this.getSpeciesSlug().equals("gastly") && !this.isSleeping() && !this.isOrderedToSit()
                    && !this.isDeadOrDying() && this.attackAnimationTicks <= 0) {
                // Cobblemon ground_idle/ground_walk each emit ghostly_smoke
                // twice at locator `gas`, with two 10 particles/s emitters.
                // This one client particle per game tick preserves that rate.
                double scale = SpeciesDefinitions.get("gastly").scale();
                double yaw = Math.toRadians(this.getYRot());
                double locatorForward = 4.0D / 16.0D * scale;
                double centerX = this.getX() - Math.sin(yaw) * locatorForward;
                double centerY = this.getY() + 4.0D / 16.0D * scale;
                double centerZ = this.getZ() + Math.cos(yaw) * locatorForward;
                double angle = this.getRandom().nextDouble() * Math.PI * 2.0D;
                double radius = 0.34D * scale;
                double px = centerX + Math.cos(angle) * radius;
                double pz = centerZ + Math.sin(angle) * radius;
                double inwardSpeed = 0.10D * scale;
                this.level.addParticle(br.com.petrickfaria.minemon.registry.ModParticles.GASTLY_GHOSTLY_SMOKE.get(),
                        px, centerY, pz, -Math.cos(angle) * inwardSpeed, 0.0D,
                        -Math.sin(angle) * inwardSpeed);
            }
            if (this.attackAnimationTicks > 0) this.attackAnimationTicks--;
            if (this.blinkCooldownTicks > 0) this.blinkCooldownTicks--;
            if (this.blinkCooldownTicks == 0) {
                this.blinkPending = true;
                this.blinkCooldownTicks = 80 + this.getRandom().nextInt(120);
            }
        } else {
            if (!this.level.isClientSide) this.tickSpontaneousRun();
            this.tickWildAggression();
        }
    }

    private void tickWildAggression() {
        if (this.isTame()) {
            this.wildAggressionTicks = 0;
            this.wildUnreachableTicks = 0;
            return;
        }
        if (this.wildAggressionTicks <= 0) {
            // Target selection runs inside super.tick() before this timer is reconciled.
            // Never leave a wild target active after the aggression window has ended.
            if (this.getTarget() != null) this.endWildAggression();
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || target.isRemoved()
                || target.distanceToSqr(this) > (double) WILD_MAX_CHASE_DISTANCE * WILD_MAX_CHASE_DISTANCE) {
            this.endWildAggression();
            return;
        }

        this.wildAggressionTicks--;
        if (this.getNavigation().isDone() && target.distanceToSqr(this) > 16.0D) {
            this.wildUnreachableTicks++;
        } else {
            this.wildUnreachableTicks = 0;
        }
        if (this.wildAggressionTicks <= 0 || this.wildUnreachableTicks >= WILD_UNREACHABLE_LIMIT_TICKS) {
            this.endWildAggression();
        }
    }

    private void endWildAggression() {
        this.wildAggressionTicks = 0;
        this.wildUnreachableTicks = 0;
        this.setTarget(null);
        this.getNavigation().stop();
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit=super.doHurtTarget(target);
        if(hit && !this.level.isClientSide) this.level.broadcastEntityEvent(this,(byte)61);
        return hit;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 61 && this.attackAnimationTicks <= 0) { this.attackAnimationTicks=SpeciesDefinitions.get(this.getSpeciesSlug()).attackTicks(); this.attackSequence++; }
        else if (id == 61) { /* Let the current physical clip finish instead of restarting its first frames. */ }
        else if (id == 7) this.level.addParticle(net.minecraft.core.particles.ParticleTypes.HEART,
                this.getX(), this.getY() + 0.5D, this.getZ(), 0.0D, 0.1D, 0.0D);
        else if (id == 6) this.level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                this.getX(), this.getY() + 0.5D, this.getZ(), 0.0D, 0.1D, 0.0D);
        else super.handleEntityEvent(id);
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "movement", 4, this::animationPredicate));
        data.addAnimationController(new AnimationController<>(this, "blink", 0, this::blinkPredicate));
        data.addAnimationController(new AnimationController<>(this, "tail_flame", 0, this::tailFlamePredicate));
    }

    private <E extends IAnimatable> PlayState animationPredicate(AnimationEvent<E> event) {
        AnimationController<?> controller = event.getController();
        String slug = this.getSpeciesSlug();
        String clip;
        boolean loop = true;
        var definition=SpeciesDefinitions.get(slug);
        String attack=SpeciesAssetIndex.clip(slug,"physical_attack","physical");
        if(attack==null) attack=SpeciesAssetIndex.clip(slug,"attack","attack");
        if (this.isDeadOrDying()) {
            clip = SpeciesAssetIndex.clip(slug, "faint", "ground_idle");
            loop = false;
        } else if (this.attackAnimationTicks>0 && attack!=null) {
            clip=attack;loop=false;
            if(this.playedAttackSequence!=this.attackSequence){controller.markNeedsReload();this.playedAttackSequence=this.attackSequence;}
        } else if (this.isSleeping() && (!this.isFlyingSpecies() || this.isOnGround())) {
            clip = SpeciesAssetIndex.clip(slug, "sleep", "sleep");
        } else if (this.isOrderedToSit()) {
            clip = SpeciesAssetIndex.clip(slug, "sit", "sit");
        } else if (this.isAquaticSpecies() && this.isInWater()) {
            String swim = firstClip(slug, "water_swim", "water_surface_swim", "surface_water_swim", "surfacewater_swim");
            String waterIdle = firstClip(slug, "water_idle", "water_surface_idle", "surface_water_idle", "surfacewater_idle");
            clip = this.isMoving(event) && swim != null ? swim : (waterIdle != null ? waterIdle : definition.idle());
        } else if (this.isMoving(event)) {
            if (this.isFlyingSpecies()) {
                String fly = firstClip(slug, "air_fly", "air_glide", "air_idle");
                clip = fly != null ? fly : definition.walk();
            } else {
                clip = definition.walk();
                String run=SpeciesAssetIndex.clip(slug,"ground_run","run");
                if(this.isSpontaneouslyRunning() && run!=null) clip=run;
            }
        } else {
            String airIdle = this.isFlyingSpecies() ? SpeciesAssetIndex.clip(slug, "air_idle", "air_idle") : null;
            clip = airIdle != null ? airIdle : definition.idle();
        }
        if (clip != null) controller.setAnimation(new AnimationBuilder().addAnimation(clip, loop));
        if (Boolean.getBoolean("minemon.clientSmokeTest")) this.selectedAnimationForValidation = clip;
        return PlayState.CONTINUE;
    }

    private static String firstClip(String slug, String... keys) {
        for (String key : keys) {
            String clip = SpeciesAssetIndex.clip(slug, key, key);
            if (clip != null) return clip;
        }
        return null;
    }

    private void tickSpontaneousRun() {
        if (this.isSpontaneouslyRunning()) {
            this.spontaneousRunTicks--;
            if (this.spontaneousRunTicks <= 0 || !this.canSpontaneouslyRun()) {
                this.stopSpontaneousRun();
                this.spontaneousRunCooldown = 180 + this.getRandom().nextInt(520);
            }
            return;
        }
        if (this.spontaneousRunCooldown > 0) this.spontaneousRunCooldown--;
        if (this.spontaneousRunCooldown == 0 && this.canSpontaneouslyRun()
                && this.getNavigation().getPath() != null && !this.getNavigation().isDone()
                && this.getDeltaMovement().horizontalDistanceSqr() > 0.0004D
                && this.getRandom().nextInt(80) == 0) {
            this.entityData.set(SPONTANEOUS_RUN, true);
            this.spontaneousRunTicks = 40 + this.getRandom().nextInt(51);
            this.spontaneousRunCount++;
            AttributeInstance movement = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movement != null && movement.getModifier(SPONTANEOUS_RUN_SPEED_ID) == null) movement.addTransientModifier(SPONTANEOUS_RUN_SPEED);
        }
    }

    private boolean canSpontaneouslyRun() {
        return this.isOnGround() && !this.isFlyingSpecies() && !this.isAquaticSpecies() && !this.isSleeping() && !this.isOrderedToSit()
                && !this.isInWaterOrBubble() && !this.isDeadOrDying() && this.getTarget() == null
                && SpeciesAssetIndex.clip(this.getSpeciesSlug(), "ground_run", "run") != null;
    }

    private void stopSpontaneousRun() {
        if (this.entityData.get(SPONTANEOUS_RUN)) this.entityData.set(SPONTANEOUS_RUN, false);
        this.spontaneousRunTicks = 0;
        AttributeInstance movement = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) movement.removeModifier(SPONTANEOUS_RUN_SPEED_ID);
    }

    @Override public boolean canBreatheUnderwater() { return this.isAquaticSpecies() || super.canBreatheUnderwater(); }

    public String getSelectedAnimationForValidation() { return this.selectedAnimationForValidation; }
    public int getSpontaneousRunCountForValidation() { return this.spontaneousRunCount; }

    public String getSelectedTailFlameAnimationForValidation() { return this.selectedTailFlameAnimationForValidation; }

    private <E extends IAnimatable> PlayState tailFlamePredicate(AnimationEvent<E> event) {
        String clip=SpeciesAssetIndex.clip(this.getSpeciesSlug(), "tail_flame", "tail_flame");
        if (clip == null) return PlayState.STOP;
        event.getController().setAnimation(new AnimationBuilder().addAnimation(clip, true));
        if (Boolean.getBoolean("minemon.clientSmokeTest")) this.selectedTailFlameAnimationForValidation=clip;
        return PlayState.CONTINUE;
    }

    private boolean isMoving(AnimationEvent<?> event) {
        return !this.isOrderedToSit() && (event.isMoving() || this.getDeltaMovement().horizontalDistanceSqr() > 0.0004D || (this.getX()-this.xo)*(this.getX()-this.xo)+(this.getZ()-this.zo)*(this.getZ()-this.zo)>0.0001D);
    }

    private <E extends IAnimatable> PlayState blinkPredicate(AnimationEvent<E> event) {
        AnimationController<?> controller = event.getController();
        if (this.isSleeping() || this.isOrderedToSit() || this.isDeadOrDying() || this.isMoving(event) || this.attackAnimationTicks > 0) return PlayState.STOP;

        String blink = SpeciesAssetIndex.clip(this.getSpeciesSlug(), "blink", "blink");
        if (this.blinkPending && blink != null && controller.getAnimationState() == AnimationState.Stopped) {
            controller.markNeedsReload();
            controller.setAnimation(new AnimationBuilder().addAnimation(blink, false));
            this.blinkPending = false;
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimationFactory getFactory() {
        return this.animationFactory;
    }
}
