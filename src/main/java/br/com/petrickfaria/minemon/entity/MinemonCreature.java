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
import net.minecraft.util.RandomSource;
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
    private static final EntityDataAccessor<Boolean> NATURAL_FLIGHT = SynchedEntityData.defineId(MinemonCreature.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> NATURAL_SURFACE = SynchedEntityData.defineId(MinemonCreature.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> NATURAL_LANDING = SynchedEntityData.defineId(MinemonCreature.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> NIGHT_REST = SynchedEntityData.defineId(MinemonCreature.class, EntityDataSerializers.BOOLEAN);
    private net.minecraft.world.entity.ai.navigation.PathNavigation dryNavigation;
    private net.minecraft.world.entity.ai.navigation.PathNavigation escapeNavigation;
    private int nightCombatTicks,nightUnreachableTicks;
    private int wakeGraceTicks;
    private boolean nightRoutine;
    private BehaviorCycle naturalCycle;
    private int naturalStartDelay;
    private int smokeParticles;
    private net.minecraft.core.BlockPos surfaceColumn;
    private int surfaceTick;
    private double cachedSurface;
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
    private int customSoundTicks;
    private boolean customSoundDayActive;
    private final RandomSource customSoundRandom = RandomSource.create();
    // Client-only diagnostic sample used by the automated locomotion smoke test.
    private String selectedAnimationForValidation;
    private String selectedTailFlameAnimationForValidation;

    public MinemonCreature(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        if(!this.isAquaticSpecies())this.lookControl=new net.minecraft.world.entity.ai.control.LookControl(this){
            @Override protected boolean resetXRotOnTick(){return !MinemonCreature.this.isInWater()||MinemonCreature.this.getNavigation().isDone();}
        };
        this.naturalCycle = new BehaviorCycle(SpeciesBehaviorProfiles.profile(this.getSpeciesSlug()),this.getSpeciesSlug());
        this.naturalStartDelay=this.getRandom().nextInt(30);
        if(!this.isAquaticSpecies()&&!SpeciesBehaviorProfiles.canFly(this.getSpeciesSlug())){
            this.moveControl=new WaterAwareMoveControl(this);
            this.dryNavigation=this.navigation;this.escapeNavigation=new EscapeNavigation(this,level);
        }
        this.blinkCooldownTicks = 80 + this.getRandom().nextInt(120);
    }

    @Override public void travel(net.minecraft.world.phys.Vec3 input){
        if(!this.level.isClientSide&&!this.isAquaticSpecies()&&!SpeciesBehaviorProfiles.canFly(this.getSpeciesSlug()))
            this.setNoGravity(this.isInWater());
        super.travel(input);
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
        return createAttributes().add(Attributes.FOLLOW_RANGE,SpeciesBehaviorProfiles.waterCapable(d.slug())?128.0D:24.0D).add(Attributes.MAX_HEALTH,d.health()).add(Attributes.ATTACK_DAMAGE,d.attack()).add(Attributes.MOVEMENT_SPEED,d.speed()).add(Attributes.FLYING_SPEED,0.4D);
    }
    public boolean isFlyingSpecies() { return SpeciesBehaviorProfiles.isAirborne(this.getSpeciesSlug())
            &&!(this.isNightResting()&&SpeciesBehaviorProfiles.landsForNightRest(this.getSpeciesSlug()))
            ||this.entityData.get(NATURAL_FLIGHT)&&!this.wantsNaturalLanding(); }
    public boolean isAquaticSpecies() { return SpeciesBehaviorProfiles.waterCapable(this.getSpeciesSlug()); }
    public boolean isSpontaneouslyRunning() { return this.entityData.get(SPONTANEOUS_RUN); }
    public void setSpontaneousRunForValidation(boolean running) {
        if (Boolean.getBoolean("minemon.clientSmokeTest") && !this.level.isClientSide
                && !this.isFlyingSpecies() && !SpeciesBehaviorProfiles.isAquatic(this.getSpeciesSlug())) {
            this.entityData.set(SPONTANEOUS_RUN, running);
            this.spontaneousRunTicks = running ? 100 : 0;
        }
    }

    public boolean isSleeping() { return this.entityData.get(SLEEPING); }
    public void setSleeping(boolean sleeping) {
        this.entityData.set(SLEEPING, sleeping);
        if (sleeping) {
            this.getNavigation().stop(); this.setTarget(null); this.stopSpontaneousRun();
            if (this instanceof MinemonFlyingCreature) this.setNoGravity(SpeciesBehaviorProfiles.isAirborne(this.getSpeciesSlug()));
        } else if (this instanceof MinemonFlyingCreature) {
            this.setNoGravity(true);
        }
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(NIGHT_REST, false);
        this.entityData.define(NATURAL_LANDING, false);
        this.entityData.define(NATURAL_FLIGHT, false);
        this.entityData.define(NATURAL_SURFACE, SpeciesBehaviorProfiles.profile(this.getSpeciesSlug())==SpeciesBehaviorProfiles.Profile.SURFACE);
        this.entityData.define(SLEEPING, false);
        this.entityData.define(SPONTANEOUS_RUN, false);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("MinemonSleeping", this.isSleeping());
        tag.putBoolean("NightRoutine",this.nightRoutine);
        tag.putInt("WakeGraceTicks",this.wakeGraceTicks);
        tag.putInt("NaturalBehaviorStartDelay",this.naturalStartDelay);
        tag.putString("NaturalBehaviorPhase",this.naturalCycle.phase().name());
        tag.putInt("NaturalBehaviorTicks",this.naturalCycle.ticks());
        tag.putInt("NaturalAltitudeStage",this.naturalCycle.stage());
        tag.putInt("MinemonCustomSoundTicks", this.customSoundTicks);
        tag.putBoolean("MinemonCustomSoundDayActive", this.customSoundDayActive);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setSleeping(tag.getBoolean("MinemonSleeping"));
        this.nightRoutine=tag.getBoolean("NightRoutine");
        this.wakeGraceTicks=tag.getInt("WakeGraceTicks");
        if(tag.contains("NaturalBehaviorStartDelay"))this.naturalStartDelay=Math.max(0,Math.min(29,tag.getInt("NaturalBehaviorStartDelay")));
        this.naturalCycle.restore(tag.getString("NaturalBehaviorPhase"),tag.getInt("NaturalBehaviorTicks"),tag.getInt("NaturalAltitudeStage"));
        if (tag.contains("MinemonCustomSoundTicks")) {
            this.customSoundTicks = Math.max(0, Math.min(6000, tag.getInt("MinemonCustomSoundTicks")));
            // Saves from before the day-only scheduler had a valid five-minute timer.
            this.customSoundDayActive = tag.contains("MinemonCustomSoundDayActive")
                    ? tag.getBoolean("MinemonCustomSoundDayActive") : this.customSoundTicks > 0;
        }
        this.syncNaturalState();
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return (AgeableMob) this.getType().create(level);
    }

    @Override
    protected void registerGoals() {
        if (!this.isAquaticSpecies()) this.goalSelector.addGoal(0, new WaterEscapeGoal(this));
        this.goalSelector.addGoal(0,new NightRestGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, this.isFlyingSpecies()));
        this.goalSelector.addGoal(6, new NaturalSeparationGoal(this));
        if (SpeciesBehaviorProfiles.canFly(this.getSpeciesSlug())) {
            this.goalSelector.addGoal(0, new NaturalMovementGoals.SleepLanding(this));
            this.goalSelector.addGoal(4, new NaturalMovementGoals.AirPatrol(this));
            this.goalSelector.addGoal(5, new NaturalMovementGoals.GroundWander(this));
        } else if (this.isAquaticSpecies()) {
            this.getNavigation().setCanFloat(true);
            this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
            this.goalSelector.addGoal(4, new WaterMovementGoal(this));
            this.goalSelector.addGoal(5, new NaturalMovementGoals.AquaticLandWander(this));
        } else {
            this.goalSelector.addGoal(4, new NaturalMovementGoals.GroundWander(this));
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
        boolean wokeFromRest=this.isSleeping()||this.isNightResting();
        boolean hurt = super.hurt(source, amount);
        if(hurt&&!this.level.isClientSide){
            this.setSleeping(false);this.entityData.set(NIGHT_REST,false);this.wakeGraceTicks=100;this.syncNaturalState();
        }
        if (hurt && !this.level.isClientSide && source.getEntity() instanceof LivingEntity attacker
                && this.canAttack(attacker)) {
            this.setTarget(attacker);
            if(wokeFromRest){this.nightCombatTicks=WILD_AGGRESSION_DURATION_TICKS;this.nightUnreachableTicks=0;}
            if (!this.isTame()) {
                this.wildAggressionTicks = WILD_AGGRESSION_DURATION_TICKS;
                this.wildUnreachableTicks = 0;
            }
        }
        return hurt;
    }

    @Override
    public void tick() {
        if(this.escapeNavigation!=null){
            var desired=this.isInWater()?this.escapeNavigation:this.dryNavigation;
            if(this.navigation!=desired){this.navigation.stop();this.navigation=desired;}
        }
        if(!this.level.isClientSide&&!(Boolean.getBoolean("minemon.clientSmokeTest")&&this.getTags().contains("minemon-locomotion-fixture")))this.tickNightRest();
        super.tick();
        if (!this.level.isClientSide) {
            this.tickNaturalCycle();
            this.tickCustomSoundTimer();
        }
        if (this.level.isClientSide) {
            if ((this.getSpeciesSlug().equals("koffing")||this.getSpeciesSlug().equals("weezing"))
                    && !this.isDeadOrDying() && this.tickCount%5==Math.floorMod(this.getId(),5)) {
                double radius=this.getBbWidth()*0.35D;
                for(int i=0;i<2;i++){
                    double angle=this.random.nextDouble()*Math.PI*2.0D;
                    double spread=radius*(0.5D+this.random.nextDouble()*0.8D);
                    this.level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                        this.getX()+Math.cos(angle)*spread,this.getY()+this.getBbHeight()*(0.30D+this.random.nextDouble()*0.42D),
                        this.getZ()+Math.sin(angle)*spread,0.0D,0.015D+this.random.nextDouble()*0.02D,0.0D);
                    this.smokeParticles++;
                }
            }
            if (this.getSpeciesSlug().equals("gastly") && !this.isOrderedToSit()
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
            this.tickNightCombat();
        }
    }

    /** Independent, persisted world-time counter. Unloaded chunks do not advance it. */
    private void tickCustomSoundTimer() {
        if (!this.isAlive() || this.isRemoved()) return;
        if (!this.isCustomSoundDaytime()) {
            this.customSoundDayActive = false;
            this.customSoundTicks = 0;
            return;
        }
        if (!this.customSoundDayActive) {
            this.customSoundDayActive = true;
            this.customSoundTicks = 20 + this.customSoundRandom.nextInt(181);
            return;
        }
        if (this.customSoundTicks > 0) this.customSoundTicks--;
        if (this.customSoundTicks == 0) {
            br.com.petrickfaria.minemon.network.MinemonNetwork.playForTracking(this);
            this.customSoundTicks = this.nextCustomSoundInterval();
        }
    }

    public void resetCustomSoundTimerAndPlay() {
        if (!this.isCustomSoundDaytime()) return;
        this.customSoundDayActive = true;
        this.customSoundTicks = this.nextCustomSoundInterval();
        br.com.petrickfaria.minemon.network.MinemonNetwork.playForTracking(this);
    }

    /** Returns an independent 3–5 minute interval, inclusive, for this entity. */
    private int nextCustomSoundInterval() {
        return 3600 + this.customSoundRandom.nextInt(2401);
    }

    /** Uses this world's daylight state, never the host computer clock. Dimensions without a day/night cycle stay eligible. */
    public boolean isCustomSoundDaytime() {
        return !this.level.dimensionType().hasSkyLight() || !this.level.isNight();
    }

    public int customSoundTicksForValidation() { return this.customSoundTicks; }
    public void setCustomSoundTicksForValidation(int ticks) {
        this.customSoundTicks = Math.max(0, Math.min(6000, ticks));
        this.customSoundDayActive = ticks > 0;
    }
    public boolean customSoundDayActiveForValidation() { return this.customSoundDayActive; }

    private void tickNightRest(){
        if(this.wakeGraceTicks>0)this.wakeGraceTicks--;
        LivingEntity target=this.getTarget();
        if(target!=null&&(!target.isAlive()||target.isRemoved())){
            this.setTarget(null);
            // HurtByTargetGoal can otherwise reacquire the discarded attacker
            // from LivingEntity's remembered damage source on its next tick.
            if(this.getLastHurtByMob()==target)this.setLastHurtByMob(null);
            target=null;
        }
        boolean night=this.level.dimensionType().hasSkyLight()&&this.level.isNight();
        boolean waterRest=SpeciesBehaviorProfiles.waterCapable(this.getSpeciesSlug())&&this.isInWaterOrBubble();
        boolean restingEnvironment=waterRest||!SpeciesBehaviorProfiles.isAquatic(this.getSpeciesSlug())&&!this.isInWaterOrBubble();
        boolean eligible=restingEnvironment&&!this.isNoAi()
            &&!this.isPassenger()&&!this.isDeadOrDying()
            &&target==null&&this.wakeGraceTicks==0;
        if(night&&eligible){
            if(!this.nightRoutine){this.nightRoutine=true;this.getNavigation().stop();this.stopSpontaneousRun();}
            this.entityData.set(NIGHT_REST,true);
            boolean hover=SpeciesBehaviorProfiles.hoversDuringNightRest(this.getSpeciesSlug());
            boolean settled=waterRest?Math.abs(this.getY()-WaterMovementGoal.waterRestY(this))<0.18D
                :hover?Math.abs(this.getY()-this.naturalSurface()-1.0D)<0.18D
                :this.isOnGround()&&WaterMovementGoal.safeDry(this,this.blockPosition());
            if(settled&&!this.isSleeping())this.setSleeping(true);
        }else{
            this.entityData.set(NIGHT_REST,false);
            if(this.nightRoutine){this.nightRoutine=false;this.setSleeping(false);this.getNavigation().stop();}
        }
        this.syncNaturalState();
    }

    @Override public void moveRelative(float acceleration,net.minecraft.world.phys.Vec3 input){
        // Vanilla water travel uses 0.02 acceleration and 0.8 horizontal drag.
        // SmoothSwimmingMoveControl supplies an attribute-scaled input (<1),
        // which previously multiplied this down a second time. Match the
        // player's full forward input (including LivingEntity's 0.98 damping).
        if(this.isInWater()&&!this.level.isClientSide&&!this.isSleeping()
                &&this.getMoveControl().hasWanted()&&input.lengthSqr()>1.0E-8){
            input=input.normalize().scale(0.98D);
            if(!this.isAquaticSpecies())acceleration*=1.25F;
        }
        super.moveRelative(acceleration,input);
    }

    private void tickNightCombat(){
        if(this.nightCombatTicks<=0)return;
        var target=this.getTarget();
        if(target!=null&&target.isAlive()&&target.distanceToSqr(this)<=WILD_MAX_CHASE_DISTANCE*WILD_MAX_CHASE_DISTANCE){
            if(this.getNavigation().isDone()&&target.distanceToSqr(this)>16)this.nightUnreachableTicks++;
            else this.nightUnreachableTicks=0;
            if(--this.nightCombatTicks>0&&this.nightUnreachableTicks<WILD_UNREACHABLE_LIMIT_TICKS)return;
        }
        this.nightCombatTicks=0;this.nightUnreachableTicks=0;
        this.setTarget(null);this.getNavigation().stop();
        // Do not reacquire the same expired personal attacker while returning to rest.
        this.setLastHurtByMob(null);
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
        } else if (this.isSleeping()) {
            String restKey=slug.equals("vaporeon")
                    ? (this.isInWaterOrBubble()?"night_sleep_water":"night_sleep_land") : "night_sleep";
            clip = SpeciesAssetIndex.clip(slug, restKey, "night_sleep");
        } else if (this.isNightResting() && this.wantsNaturalLanding() && !this.isOnGround()) {
            // Keep a real air pose while descending. A ground gait during the
            // landing path looks like Spearow/Fearow are walking in mid-air.
            clip = firstClip(slug, "air_idle", "air_fly");
        } else if (this.isOrderedToSit()) {
            clip = SpeciesAssetIndex.clip(slug, "sit", "sit");
        } else if (this.isAquaticSpecies() && this.isInWater()) {
            String swim = this.isNaturalSurface() ? firstClip(slug,"surface_water_swim","surfacewater_swim","water_surface_swim","water_swim")
                    : firstClip(slug,"water_swim","water_surface_swim","surface_water_swim","surfacewater_swim");
            String waterIdle = this.isNaturalSurface() ? firstClip(slug,"surface_water_idle","surfacewater_idle","water_surface_idle","water_idle")
                    : firstClip(slug,"water_idle","water_surface_idle","surface_water_idle","surfacewater_idle");
            clip = this.isMoving(event) ? (swim!=null?swim:definition.walk()) : (waterIdle != null ? waterIdle : definition.idle());
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
        return this.isOnGround() && !this.isFlyingSpecies() && !SpeciesBehaviorProfiles.isAquatic(this.getSpeciesSlug()) && !this.isSleeping() && !this.isOrderedToSit()
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

    public BehaviorCycle naturalCycle(){return this.naturalCycle;}
    public boolean isNaturalSurface(){return this.entityData.get(NATURAL_SURFACE);}
    public void chooseNaturalWaterDepth(boolean surface){
        if(SpeciesBehaviorProfiles.isVisitor(this.getSpeciesSlug()))
            this.naturalCycle.chooseWaterDepth(surface);
        this.syncNaturalState();
    }
    public boolean isNightResting(){return this.entityData.get(NIGHT_REST);}
    public boolean wantsNaturalLanding(){return (!SpeciesBehaviorProfiles.isAirborne(this.getSpeciesSlug())
            ||SpeciesBehaviorProfiles.landsForNightRest(this.getSpeciesSlug()))&&(this.isSleeping()||this.isNightResting())
            ||this.entityData.get(NATURAL_LANDING); }
    public int getSmokeParticleCountForValidation(){return this.smokeParticles;}
    public boolean naturalBehaviorActive(){
        return this.naturalStartDelay==0&&(!this.isInWater()||this.isAquaticSpecies())&&!this.isNightResting()&&!this.isSleeping()&&!this.isOrderedToSit()&&!this.isPassenger()&&!this.isNoAi()
            &&!this.isDeadOrDying()&&this.getTarget()==null&&this.attackAnimationTicks<=0
            &&(this.getOwner()==null||this.distanceToSqr(this.getOwner())<100.0D)
            &&this.goalSelector.getAvailableGoals().stream().noneMatch(goal->goal.isRunning()
                &&goal.getGoal() instanceof FollowOwnerGoal);
    }
    public double naturalSurface(){
        var column=this.blockPosition();
        if(this.surfaceColumn==null||!column.equals(this.surfaceColumn)||this.tickCount-this.surfaceTick>=10){
            this.cachedSurface=NaturalMovementGoals.surfaceBelow(this.level,this.getX(),this.getY(),this.getZ());
            this.surfaceColumn=column;this.surfaceTick=this.tickCount;
        }
        return this.cachedSurface;
    }
    private void syncNaturalState(){
        this.entityData.set(NATURAL_LANDING,this.naturalCycle.landing());
        this.entityData.set(NATURAL_FLIGHT,SpeciesBehaviorProfiles.profile(this.getSpeciesSlug())==SpeciesBehaviorProfiles.Profile.GROUND_FLIGHT
            &&this.naturalCycle.flying()&&!this.isNightResting());
        this.entityData.set(NATURAL_SURFACE,SpeciesBehaviorProfiles.profile(this.getSpeciesSlug())==SpeciesBehaviorProfiles.Profile.SURFACE
            ||SpeciesBehaviorProfiles.isVisitor(this.getSpeciesSlug())&&(this.naturalCycle.phase()==BehaviorCycle.Phase.SURFACE||this.naturalCycle.phase()==BehaviorCycle.Phase.EXIT_WATER));
    }
    private void tickNaturalCycle(){
        if(this.naturalStartDelay>0&&!this.isNightResting()&&!this.isSleeping()&&!this.isOrderedToSit()&&!this.isPassenger()&&!this.isNoAi()
                &&!this.isDeadOrDying()&&this.getTarget()==null)this.naturalStartDelay--;
        boolean wet=this.isInWater();
        boolean flying=this.isFlyingSpecies();
        double altitude=flying?this.getY()-this.naturalSurface():0.0D;
        boolean band=flying&&altitude>=this.naturalCycle.minimumAltitude()&&altitude<=this.naturalCycle.maximumAltitude();
        boolean submerged=wet&&this.level.getFluidState(new net.minecraft.core.BlockPos(this.getX(),this.getY()+this.getBbHeight()+0.1D,this.getZ())).is(net.minecraft.tags.FluidTags.WATER);
        double waterTop=wet&&this instanceof MinemonAquaticCreature water?water.waterSurface():this.getY();
        boolean surface=wet&&Math.abs(this.getY()-(waterTop-WaterMovementGoal.preferredSurfaceDepth(this)))<0.3D;
        boolean canDive=wet&&this.naturalCycle.phase()==BehaviorCycle.Phase.SEEK_WATER&&WaterMovementGoal.canDive(this,this.blockPosition());
        var previousPhase=this.naturalCycle.phase();int previousStage=this.naturalCycle.stage();
        this.naturalCycle.tick(new BehaviorCycle.Observation(this.naturalBehaviorActive(),
            this.isOnGround()&&!wet,
            band,wet,submerged,surface,this.isOnGround()&&!wet,canDive));
        if(previousPhase!=this.naturalCycle.phase()||previousStage!=this.naturalCycle.stage())this.getNavigation().stop();
        this.syncNaturalState();
    }

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
