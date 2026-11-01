Warning: truncated output (original token count: 19103)
Total output lines: 1241

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
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import br.com.petrickfaria.minemon.item.PokemonTreatItem;
import br.com.petrickfaria.minemon.pokec.PokemonTreatTypes;
import br.com.petrickfaria.minemon.pokec.PokemonBreeding;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import java.util.UUID;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;
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
    private static final EntityDataAccessor<Boolean> DEFEATED = SynchedEntityData.defineId(MinemonCreature.class, EntityDataSerializers.BOOLEAN);
    private boolean aiBeforeDefeat;
    private final DefeatedRest defeatedRest=new DefeatedRest(this);
    private boolean defeatTargetsCleared;
    private int successfulMeleeAttacks;
    private static final EntityDataAccessor<Integer> RANGED_CHARGES=SynchedEntityData.defineId(MinemonCreature.class,EntityDataSerializers.INT);
    private final CombatCharges rangedCharges=new CombatCharges();
    public int getRangedCharges(){return entityData.get(RANGED_CHARGES);}
    public CombatCharges rangedCharges(){return rangedCharges;}
    private long nextCombatTick;
    private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(MinemonCreature.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ORDERED_SIT = SynchedEntityData.defineId(MinemonCreature.class, EntityDataSerializers.BOOLEAN);
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
    private static final EntityDataAccessor<Boolean> FOLLOWING = SynchedEntityData.defineId(MinemonCreature.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FOLLOWING_RUN = SynchedEntityData.defineId(MinemonCreature.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SPONTANEOUS_RUN = SynchedEntityData.defineId(MinemonCreature.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ROCKET_SPECIAL = SynchedEntityData.defineId(MinemonCreature.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> POKEMON_LEVEL = SynchedEntityData.defineId(MinemonCreature.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> POKEMON_XP = SynchedEntityData.defineId(MinemonCreature.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> POKEMON_XP_TO_NEXT = SynchedEntityData.defineId(MinemonCreature.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> POKEMON_RARITY = SynchedEntityData.defineId(MinemonCreature.class, EntityDataSerializers.STRING);
    private static final java.util.UUID SPONTANEOUS_RUN_SPEED_ID = java.util.UUID.fromString("b4db65e0-39b8-43a6-a454-3c6e9f16d920");
    private static final AttributeModifier SPONTANEOUS_RUN_SPEED = new AttributeModifier(
            SPONTANEOUS_RUN_SPEED_ID, "Minemon spontaneous natural run", 0.55D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final int WILD_AGGRESSION_DURATION_TICKS = 20 * 20;
    private static final int WILD_MAX_CHASE_DISTANCE = 24;
    private static final int WILD_UNREACHABLE_LIMIT_TICKS = 20 * 5;
    private final AnimationFactory animationFactory = GeckoLibUtil.createFactory(this);
    private int wildAggressionTicks;
    private UUID pendingBreedOffspring;
    private static final java.util.Set<String> NON_BREEDABLE_SPECIES = java.util.Set.of("articuno", "zapdos", "moltres", "mewtwo", "mew");
    private int wildUnreachableTicks;
    private int blinkCooldownTicks;
    private boolean blinkPending;
    private int attackAnimationTicks;
    private int chanseyCooldownTicks;
    private int chanseyScanDelay;
    private int attackSequence;
    private int playedAttackSequence;
    private boolean loadingPokemonData;
    private double aquaticSleepY=Double.NaN;
    public double aquaticSleepY() { return aquaticSleepY; }
    private int spontaneousRunTicks;
    private int spontaneousRunCooldown = 50 + this.getRandom().nextInt(100);
    private int spontaneousRunCount;
    private int customSoundTicks;
    private boolean customSoundDayActive;
    // Test instrumentation: counts interaction-triggered sound packet dispatches per entity.
    private int customSoundInteractionDispatchesForValidation;
    private final RandomSource customSoundRandom = RandomSource.create();
    // Client-only diagnostic sample used by the automated locomotion smoke test.
    private String selectedAnimationForValidation;
    private String selectedAquaticSleepEyesForValidation;
    private String selectedTailFlameAnimationForValidation;

    public MinemonCreature(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        if(!this.isAquaticSpecies())this.lookControl=new net.minecraft.world.entity.ai.control.LookControl(this){
            @Override public void tick(){float pitch=MinemonCreature.this.getXRot();super.tick();if(MinemonCreature.this.isInWater()&&!MinemonCreature.this.getNavigation().isDone())MinemonCreature.this.setXRot(pitch);}
            @Override protected boolean resetXRotOnTick(){return !MinemonCreature.this.isInWater()||MinemonCreature.this.getNavigation().isDone();}
        };
        this.naturalCycle = new BehaviorCycle(SpeciesBehaviorProfiles.profile(this.getSpeciesSlug()), this.getSpeciesSlug(),
                new java.util.Random(this.getRandom().nextLong()));
        this.naturalStartDelay=this.getRandom().nextInt(30);
        this.entityData.set(POKEMON_RARITY, br.com.petrickfaria.minemon.pokec.PokecEconomy.get().rarity(this.getSpeciesSlug()));
        this.refreshPokemonXpThreshold();
        this.refreshPokemonAttributes(0); this.setHealth(this.getMaxHealth());
        if(!this.isAquaticSpecies()&&!SpeciesBehaviorProfiles.canFly(this.getSpeciesSlug())){
            this.moveControl=new WaterAwareMoveControl(this);
            this.dryNavigation=this.navigation;this.escapeNavigation=new EscapeNavigation(this,level);
        }

        this.blinkCooldownTicks = 80 + this.getRandom().nextInt(120);
    }

    @Override public void travel(net.minecraft.world.phys.Vec3 input){
        if(!this.level.isClientSide&&!this.isAquaticSpecies()&&!SpeciesBehaviorProfiles.canFly(this.getSpeciesSlug()))
            this.setNoGravity(this.isInWater());
        if (isDefeated() || this.isOrderedToSit() && !this.commandLanding() || this.commandLanding() && this.isOnGround()) {
            input=net.minecraft.world.phys.Vec3.ZERO;
            this.setSpeed(0); this.setXxa(0); this.setYya(0); this.setZza(0); this.setJumping(false);
        }
        super.travel(input);
    }

    public String getSpeciesSlug() {
        net.minecraft.resources.ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(this.getType());
        return key == null ? "bulbasaur" : key.getPath();
    }

    /**
     * Evolution implementations must call this on the old, owned entity after creating
     * its next stage. PokéC verifies ownership and the declared species edge on the server.
     */
    public void notifyPokecEvolution(String evolvedSpeciesSlug) {
        br.com.petrickfaria.minemon.pokec.PokecTransactions.onOwnedEvolution(this,evolvedSpeciesSlug);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, PokemonProgression.healthForRarity("common",1))
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, PokemonProgression.damageForRarity("common",1))
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    public static AttributeSupplier.Builder createAttributes(SpeciesDefinitions.Definition d) {
        return createAttributes().add(Attributes.FOLLOW_RANGE,SpeciesBehaviorProfiles.waterCapable(d.slug())?128.0D:24.0D).add(Attributes.MAX_HEALTH,PokemonProgression.healthForRarity(br.com.petrickfaria.minemon.pokec.PokecEconomy.get().rarity(d.slug()),1)).add(Attributes.ATTACK_DAMAGE,PokemonProgression.damageForRarity(br.com.petrickfaria.minemon.pokec.PokecEconomy.get().rarity(d.slug()),1)).add(Attributes.MOVEMENT_SPEED,d.speed()).add(Attributes.FLYING_SPEED,0.4D);
    }
    public boolean isFlyingSpecies() { return SpeciesBehaviorProfiles.isAirborne(this.getSpeciesSlug())
            &&!(this.isNightResting()&&SpeciesBehaviorProfiles.landsForNightRest(this.getSpeciesSlug()))
            ||this.entityData.get(NATURAL_FLIGHT)&&!this.wantsNaturalLanding(); }
    public boolean isAquaticSpecies() { return SpeciesBehaviorProfiles.waterCapable(this.getSpeciesSlug()); }
    public boolean isFollowingOwner(){return entityData.get(FOLLOWING);}
    void setFollowingOwner(boolean value){entityData.set(FOLLOWING,value);}
    public double followAltitude(){return new BehaviorCycle(SpeciesBehaviorProfiles.profile(getSpeciesSlug()),getSpeciesSlug()).minimumAltitude();}
    public double flightMinimum(){return isFollowingOwner()?followAltitude():naturalCycle.minimumAltitude();}
    public double flightMaximum(){return isFollowingOwner()?followAltitude():naturalCycle.maximumAltitude();}
    void setFollowingRun(boolean value) { this.entityData.set(FOLLOWING_RUN, value); }
    public boolean isFollowingRun() { return this.entityData.get(FOLLOWING_RUN); }
    public boolean isSpontaneouslyRunning() { return this.entityData.get(SPONTANEOUS_RUN); }
    public void setSpontaneousRunForValidation(boolean running) {
        if (Boolean.getBoolean("minemon.clientSmokeTest") && !this.level.isClientSide
                && !this.isFlyingSpecies() && !SpeciesBehaviorProfiles.isAquatic(this.getSpeciesSlug())) {
            this.entityData.set(SPONTANEOUS_RUN, running);
            this.spontaneousRunTicks = running ? 100 : 0;
        }
    }

    @Override public boolean isOrderedToSit() { return this.entityData.get(ORDERED_SIT); }
    @Override public void setOrderedToSit(boolean orderedToSit) {
        super.setOrderedToSit(orderedToSit);
        this.entityData.set(ORDERED_SIT, orderedToSit);
        if (orderedToSit) {
            this.setSleeping(false); this.entityData.set(NIGHT_REST, false); this.nightRoutine=false;
            this.getNavigation().stop(); this.stopSpontaneousRun(); this.setFollowingRun(false);
            this.getMoveControl().setWantedPosition(this.getX(),this.getY(),this.getZ(),0);
            this.setSpeed(0); this.setXxa(0); this.setYya(0); this.setZza(0); this.setJumping(false);
            this.setDeltaMovement(0,this.getDeltaMovement().y,0);
        }
    }

    public boolean isSleeping() { return this.entityData.get(SLEEPING); }
    public void setSleeping(boolean sleeping) {
        if(isDefeated())sleeping=true;
        if (!sleeping) this.aquaticSleepY=Double.NaN;
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
        this.entityData.define(RANGED_CHARGES,3);
        this.entityData.define(DEFEATED, false);
        this.entityData.define(NIGHT_REST, false);
        this.entityData.define(NATURAL_LANDING, false);
        this.entityData.define(NATURAL_FLIGHT, false);
        this.entityData.define(NATURAL_SURFACE, SpeciesBehaviorProfiles.profile(this.getSpeciesSlug())==SpeciesBehaviorProfiles.Profile.AQUATIC_SURFACE);
        this.entityData.define(SLEEPING, false);
        this.entityData.define(ORDERED_SIT, false);
        this.entityData.define(SPONTANEOUS_RUN, false);
        this.entityData.define(FOLLOWING_RUN, false);
        this.entityData.define(FOLLOWING,false);
        this.entityData.define(ROCKET_SPECIAL, false);
        this.entityData.define(POKEMON_LEVEL, 1);
        this.entityData.define(POKEMON_XP, 0);
        this.entityData.define(POKEMON_XP_TO_NEXT, 5);
        this.entityData.define(POKEMON_RARITY, "common");
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("MinemonDefeated",isDefeated());
        tag.putBoolean("MinemonAiBeforeDefeat",aiBeforeDefeat);
        rangedCharges.save(tag);
        tag.putInt("MinemonMeleeSequence",successfulMeleeAttacks);
        tag.putInt("MinemonCombatCooldown",(int)Math.max(0,nextCombatTick-level.getGameTime()));
        tag.putInt("MinemonPokemonLevel", this.getPokemonLevel());
        tag.putInt("MinemonPokemonExperience", this.getPokemonExperience());
        tag.putInt("MinemonChanseyCooldownTicks", this.chanseyCooldownTicks);
        tag.putBoolean("MinemonRocketSpecial", this.isRocketSpecial());
        if (this.getPersistentData().hasUUID("RocketVillager"))
            tag.putUUID("MinemonRocketVillager", this.getPersistentData().getUUID("RocketVillager"));
        tag.putBoolean("MinemonSleeping", this.isSleeping());
        tag.putBoolean("NightRoutine",this.nightRoutine);
        if(Double.isFinite(this.aquaticSleepY))tag.putDouble("AquaticSleepAnchorY",this.aquaticSleepY);
        tag.putInt("WakeGraceTicks",this.wakeGraceTicks);
        tag.putInt("NaturalBehaviorStartDelay",this.naturalStartDelay);
        this.naturalCycle.save(tag);
        tag.putInt("MinemonCustomSoundTicks", this.customSoundTicks);
        tag.putBoolean("MinemonCustomSoundDayActive", this.customSoundDayActive);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(ORDERED_SIT, super.isOrderedToSit());
        this.loadingPokemonData=true;
        this.entityData.set(DEFEATED,tag.getBoolean("MinemonDefeated"));
        this.aiBeforeDefeat=tag.getBoolean("MinemonAiBeforeDefeat");
        rangedCharges.load(tag);entityData.set(RANGED_CHARGES,rangedCharges.charges());
        this.successfulMeleeAttacks=Math.max(0,Math.min(4,tag.getInt("MinemonMeleeSequence")));
        this.nextCombatTick=level.getGameTime()+Math.max(0,tag.getInt("MinemonCombatCooldown"));
        this.setRocketSpecial(tag.getBoolean("MinemonRocketSpecial"));
        this.setPokemonProgress(tag.contains("MinemonPokemonLevel") ? tag.getInt("MinemonPokemonLevel") : 1,
                tag.getInt("MinemonPokemonExperience"));
        this.entityData.set(POKEMON_RARITY, br.com.petrickfaria.minemon.pokec.PokecEconomy.get().rarity(this.getSpeciesSlug()));
        this.refreshPokemonXpThreshold();
        this.chanseyCooldownTicks = Math.max(0, tag.getInt("MinemonChanseyCooldownTicks"));
        this.setRocketSpecial(tag.getBoolean("MinemonRocketSpecial"));
        if (tag.hasUUID("MinemonRocketVillager")) this.getPersistentData().putUUID("RocketVillager", tag.getUUID("MinemonRocketVillager"));
        this.setSleeping(tag.getBoolean("MinemonSleeping"));
        this.nightRoutine=tag.getBoolean("NightRoutine");
        this.wakeGraceTicks=tag.getInt("WakeGraceTicks");
        if(tag.contains("NaturalBehaviorStartDelay"))this.naturalStartDelay=Math.max(0,Math.min(29,tag.getInt("NaturalBehaviorStartDelay")));
        this.naturalCycle.restore(tag);
        if (tag.contains("MinemonCustomSoundTicks")) {
            this.customSoundTicks = Math.max(0, Math.min(6000, tag.getInt("MinemonCustomSoundTicks")));
            // Saves from before the day-only scheduler had a valid five-minute timer.
            this.customSoundDayActive = tag.contains("MinemonCustomSoundDayActive")
                    ? tag.getBoolean("MinemonCustomSoundDayActive") : this.customSoundTicks > 0;
        }
        this.aquaticSleepY=tag.contains("AquaticSleepAnchorY")?tag.getDouble("AquaticSleepAnchorY"):Double.NaN;
        this.refreshPokemonAttributes(0);
        if(tag.contains("Health"))this.setHealth(Math.min(this.getMaxHealth(),tag.getFloat("Health")));
        if(isDefeated()){setNoAi(true);se…9103 tokens truncated…ks++;
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
        // Companion attacks are always projectiles; never fall back to vanilla melee.
        return false;
    }
    public boolean projectileReady(){return !isDefeated() && !isSleeping();}
    public boolean canFireAt(LivingEntity target){
        if(!canAttack(target)||!target.isAlive()||!CombatGeometry.inRange(this,target,isTame()?10:5)||!getSensing().hasLineOfSight(target))return false;
        var start=new net.minecraft.world.phys.Vec3(getX(),getEyeY()-.1,getZ());
        return level.clip(new net.minecraft.world.level.ClipContext(start,target.getBoundingBox().getCenter(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,this)).getType()==net.minecraft.world.phys.HitResult.Type.MISS;
    }
    public boolean combatReady(){return !isDefeated() && !isSleeping() && level.getGameTime()>=nextCombatTick;}
    public int projectileCooldownTicks(){
        return PokemonProgression.projectileCooldownTicks(getPokemonLevel());
    }
    public boolean fireElementalProjectile(LivingEntity target){
        if(level.isClientSide || !projectileReady() || !combatReady() || !canAttack(target)
                || !canFireAt(target))return false;
        ElementalProjectile shot=new ElementalProjectile(this,target);
        if(!level.addFreshEntity(shot))return false;
        nextCombatTick=level.getGameTime()+projectileCooldownTicks();
        level.broadcastEntityEvent(this,(byte)61);return true;
    }
    public boolean isDefeated(){return entityData.get(DEFEATED);}
    @Override public boolean isAttackable(){return !isDefeated() && super.isAttackable();}
    @Override public boolean canBeSeenAsEnemy(){return !isDefeated() && super.canBeSeenAsEnemy();}
    @Override public boolean isPushable(){return !isDefeated() && super.isPushable();}
    @Override public boolean isAlive(){return isDefeated()?!isRemoved():super.isAlive();}
    @Override public boolean isDeadOrDying(){return !isDefeated() && super.isDeadOrDying();}
    @Override public void heal(float amount){if(!isDefeated())super.heal(amount);}
    @Override public void die(DamageSource source){
        if(!isTame()){super.die(source);return;}
        if(isDefeated())return;
        entityData.set(DEFEATED,true);aiBeforeDefeat=isNoAi();setNoAi(true);
        setHealth(0);setTarget(null);getNavigation().stop();resetLove();
        attackAnimationTicks=0;stopSpontaneousRun();setFollowingRun(false);setFollowingOwner(false);
        setSleeping(true);setDeltaMovement(0,0,0);fallDistance=0;
        var owner=getOwner();
        if(owner instanceof ServerPlayer player)player.sendSystemMessage(net.minecraft.network.chat.Component.literal(getName().getString()+" foi derrotado."));
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
        if(SpeciesBehaviorProfiles.isAquatic(this.getSpeciesSlug()))
            data.addAnimationController(new AnimationController<>(this, "aquatic_sleep_eyes", 0, this::aquaticSleepEyesPredicate));
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
        if (this.isDefeated()) {
            clip = SpeciesAssetIndex.clip(slug,"night_sleep","ground_idle");
        } else if (this.isDeadOrDying()) {
            clip = SpeciesAssetIndex.clip(slug, "faint", "ground_idle");
            loop = false;
        } else if (this.attackAnimationTicks>0 && attack!=null) {
            clip=attack;loop=false;
            if(this.playedAttackSequence!=this.attackSequence){controller.markNeedsReload();this.playedAttackSequence=this.attackSequence;}
        } else if (this.isSleeping()) {
            if(SpeciesBehaviorProfiles.isAquatic(slug)) {
                clip=(slug.equals("lapras")||slug.equals("gyarados"))?SpeciesAssetIndex.clip(slug,"night_sleep","night_sleep")
                        :SpeciesAssetIndex.clip(slug,"aquatic_sleep","night_sleep");
            } else {
                String restKey=slug.equals("vaporeon")
                        ? (this.isInWaterOrBubble()?"night_sleep_water":"night_sleep_land") : "night_sleep";
                clip = SpeciesAssetIndex.clip(slug, restKey, "night_sleep");
            }
        } else if ((this.isNightResting() || this.commandLanding()) && this.wantsNaturalLanding() && !this.isOnGround()) {
            // Keep a real air pose while descending. A ground gait during the
            // landing path looks like Spearow/Fearow are walking in mid-air.
            clip = firstClip(slug, "air_idle", "air_fly");
        } else if (this.isOrderedToSit()) {
            clip = SpeciesAssetIndex.clip(slug, slug.equals("vaporeon") && this.isInWaterOrBubble() ? "awake_rest_water" : "awake_rest", "night_sleep");
        } else if (this.isAquaticSpecies() && this.isInWater()) {
            String swim = this.isNaturalSurface() ? firstClip(slug,"surface_water_swim","surfacewater_swim","water_surface_swim","water_swim")
                    : firstClip(slug,"water_swim","water_surface_swim","surface_water_swim","surfacewater_swim");
            String waterIdle = this.isNaturalSurface() ? firstClip(slug,"surface_water_idle","surfacewater_idle","water_surface_idle","water_idle")
                    : firstClip(slug,"water_idle","water_surface_idle","surface_water_idle","surfacewater_idle");
            clip = this.isMoving(event) ? (swim!=null?swim:definition.walk()) : (waterIdle != null ? waterIdle : definition.idle());
        } else if (slug.equals("geodude") && this.isFlyingSpecies()) {
            clip = firstClip(slug,"air_idle","ground_idle");
            if(clip == null) clip=definition.idle();
        } else if (this.isMoving(event)) {
            if (this.isFlyingSpecies()) {
                String fly = firstClip(slug, "air_fly", "air_glide", "air_idle");
                clip = fly != null ? fly : definition.walk();
            } else {
                clip = definition.walk();
                String run=SpeciesAssetIndex.clip(slug,"ground_run","run");
                if((this.isSpontaneouslyRunning() || this.isFollowingRun()) && run!=null) clip=run;
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
                && !this.isInWaterOrBubble() && !this.isDeadOrDying() && this.getTarget() == null && this.naturalCycle.walking()
                && this.goalSelector.getAvailableGoals().stream().noneMatch(goal -> goal.isRunning() && goal.getGoal() instanceof NaturalFollowOwnerGoal)
                && SpeciesAssetIndex.clip(this.getSpeciesSlug(), "ground_run", "run") != null;
    }

    void stopSpontaneousRun() {
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
    public int wakeGraceTicksForValidation(){return this.wakeGraceTicks;}
    public int nightCombatTicksForValidation(){return this.nightCombatTicks;}
    public int nightUnreachableTicksForValidation(){return this.nightUnreachableTicks;}
    public boolean nightRoutineForValidation(){return this.nightRoutine;}
    public boolean commandLanding(){return this.isOrderedToSit() && SpeciesBehaviorProfiles.landsForNightRest(this.getSpeciesSlug());}
    public boolean wantsNaturalLanding(){return this.commandLanding() || (SpeciesBehaviorProfiles.landsForNightRest(this.getSpeciesSlug())
            ||!SpeciesBehaviorProfiles.hoversDuringNightRest(this.getSpeciesSlug()))&&(this.isSleeping()||this.isNightResting())
            ||this.entityData.get(NATURAL_LANDING); }
    public int getSmokeParticleCountForValidation(){return this.smokeParticles;}
    public boolean naturalBehaviorActive(){
        return this.naturalStartDelay==0&&(!this.isInWater()||this.isAquaticSpecies())&&!this.isNightResting()&&!this.isSleeping()&&!this.isOrderedToSit()&&!this.isPassenger()&&!this.isNoAi()
            &&!this.isDeadOrDying()&&this.getTarget()==null&&this.attackAnimationTicks<=0
            &&(this.getOwner()==null||this.distanceToSqr(this.getOwner())<100.0D)
            &&this.goalSelector.getAvailableGoals().stream().noneMatch(goal->goal.isRunning()
                &&goal.getGoal() instanceof NaturalFollowOwnerGoal);
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
        this.entityData.set(NATURAL_FLIGHT, this.naturalCycle.flying() && !this.isNightResting());
        this.entityData.set(NATURAL_SURFACE,SpeciesBehaviorProfiles.profile(this.getSpeciesSlug())==SpeciesBehaviorProfiles.Profile.AQUATIC_SURFACE
            ||SpeciesBehaviorProfiles.profile(this.getSpeciesSlug())==SpeciesBehaviorProfiles.Profile.AQUATIC_NEAR_SURFACE
            ||SpeciesBehaviorProfiles.isVisitor(this.getSpeciesSlug())&&(this.naturalCycle.phase()==BehaviorCycle.Phase.SURFACE||this.naturalCycle.phase()==BehaviorCycle.Phase.EXIT_WATER)
            ||SpeciesBehaviorProfiles.profile(this.getSpeciesSlug())==SpeciesBehaviorProfiles.Profile.AQUATIC_CYCLE
                &&this.naturalCycle.phase()==BehaviorCycle.Phase.SURFACE);
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
        boolean canDive=wet&&(this.naturalCycle.phase()==BehaviorCycle.Phase.SEEK_WATER
                ||this.naturalCycle.phase()==BehaviorCycle.Phase.SUBMERGED
                ||this.naturalCycle.phase()==BehaviorCycle.Phase.SURFACE)
                &&WaterMovementGoal.canDive(this,this.blockPosition());
        var previousPhase=this.naturalCycle.phase();int previousStage=this.naturalCycle.stage();
        this.naturalCycle.tick(new BehaviorCycle.Observation(this.naturalBehaviorActive(),
            this.isOnGround()&&!wet,
            band,wet,submerged,surface,this.isOnGround()&&!wet,canDive));
        if(previousPhase!=this.naturalCycle.phase()||previousStage!=this.naturalCycle.stage())this.getNavigation().stop();
        this.syncNaturalState();
    }

    public String getSelectedAnimationForValidation() { return this.selectedAnimationForValidation; }
    public String getSelectedAquaticSleepEyesForValidation() { return this.selectedAquaticSleepEyesForValidation; }
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

    private <E extends IAnimatable> PlayState aquaticSleepEyesPredicate(AnimationEvent<E> event) {
        if(!SpeciesBehaviorProfiles.isAquatic(this.getSpeciesSlug())||!this.isSleeping()) {
            this.selectedAquaticSleepEyesForValidation=null;
            return PlayState.STOP;
        }
        String clip=SpeciesAssetIndex.clip(this.getSpeciesSlug(),"aquatic_sleep_eyes","aquatic_sleep_eyes");
        if(clip==null)return PlayState.STOP;
        event.getController().setAnimation(new AnimationBuilder().addAnimation(clip,true));
        if(Boolean.getBoolean("minemon.clientSmokeTest"))this.selectedAquaticSleepEyesForValidation=clip;
        return PlayState.CONTINUE;
    }

    @Override
    public AnimationFactory getFactory() {
        return this.animationFactory;
    }
}
