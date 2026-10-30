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
    private final CombatCharges rangedCharges=new CombatCharges();
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
    private final RandomSource customSoundRandom = RandomSource.create();
    // Client-only diagnostic sample used by the automated locomotion smoke test.
    private String selectedAnimationForValidation;
    private String selectedTailFlameAnimationForValidation;

    public MinemonCreature(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        if(!this.isAquaticSpecies())this.lookControl=new net.minecraft.world.entity.ai.control.LookControl(this){
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
        if(this.isAquaticSpecies())this.maxUpStep=1.0F;
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
        rangedCharges.load(tag);
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
        if(isDefeated()){setNoAi(true);setTarget(null);setSleeping(true);}
        this.loadingPokemonData=false;
        this.syncNaturalState();
    }

    public int getPokemonLevel() { return Math.max(1, this.entityData.get(POKEMON_LEVEL)); }
    public int getPokemonExperience() { return Math.max(0, this.entityData.get(POKEMON_XP)); }
    public String getPokemonRarity() { return this.entityData.get(POKEMON_RARITY); }
    public int getPokemonXpToNextLevel() { return Math.max(0, this.entityData.get(POKEMON_XP_TO_NEXT)); }
    public void setPokemonLevel(int level) { setPokemonProgress(level, 0); }
    public void setPokemonProgress(int level, int experience) {
        int oldLevel=this.getPokemonLevel();
        int boundedLevel = Math.max(1, Math.min(PokemonProgression.maxLevel(), level));
        this.entityData.set(POKEMON_LEVEL, boundedLevel);
        int needed = PokemonProgression.xpForNextLevel(boundedLevel, this.getSpeciesSlug());
        this.entityData.set(POKEMON_XP, needed <= 0 ? 0 : Math.max(0, loadingPokemonData?experience:Math.min(needed - 1, experience)));
        this.refreshPokemonXpThreshold();
        this.refreshPokemonAttributes(loadingPokemonData?0:Math.max(0,boundedLevel-oldLevel));
    }
    public int awardPokemonExperience(int amount) {
        if (this.isDefeated() || amount <= 0 || !this.isTame() || this.level.isClientSide) return 0;
        int oldLevel=this.getPokemonLevel();
        int awarded = 0;
        long pending = Math.min((long) amount, Integer.MAX_VALUE);
        while (pending > 0 && this.getPokemonLevel() < PokemonProgression.maxLevel()) {
            int need = PokemonProgression.xpForNextLevel(this.getPokemonLevel(), this.getSpeciesSlug());
            int room = Math.max(0,need - this.getPokemonExperience());
            int add = (int) Math.min(pending, room);
            this.entityData.set(POKEMON_XP, this.getPokemonExperience() + add);
            pending -= add;
            awarded += add;
            if (this.getPokemonExperience() >= need) {
                this.entityData.set(POKEMON_XP, this.getPokemonExperience()-need);
                this.entityData.set(POKEMON_LEVEL, this.getPokemonLevel() + 1);
            }
        }
        this.refreshPokemonXpThreshold();
        this.refreshPokemonAttributes(Math.max(0,this.getPokemonLevel()-oldLevel));
        return awarded;
    }
    private void refreshPokemonAttributes(int gainedLevels) {
        if(this.level.isClientSide)return;
        String rarity=br.com.petrickfaria.minemon.pokec.PokecEconomy.get().rarity(this.getSpeciesSlug());
        this.entityData.set(POKEMON_RARITY,rarity);
        float health=this.getHealth();
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(PokemonProgression.healthForRarity(rarity,this.getPokemonLevel()));
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(PokemonProgression.damageForRarity(rarity,this.getPokemonLevel()));
        this.setHealth(Math.min(this.getMaxHealth(),health+2.0F*gainedLevels));
    }
    private void refreshPokemonXpThreshold() {
        if (this.entityData == null) return;
        this.entityData.set(POKEMON_XP_TO_NEXT,
                PokemonProgression.xpForNextLevelForRarity(this.getPokemonLevel(), this.getPokemonRarity()));
    }
    public int chanseyCooldownTicksForValidation() { return this.chanseyCooldownTicks; }
    public void tryChanseyHealForValidation() { this.tickChanseyHealing(); }
    private void tickChanseyHealing() {
        if (this.level.isClientSide || !"chansey".equals(this.getSpeciesSlug()) || !this.isTame()) return;
        if (this.chanseyCooldownTicks > 0) { this.chanseyCooldownTicks--; return; }
        if (this.chanseyScanDelay > 0) { this.chanseyScanDelay--; return; }
        this.chanseyScanDelay = 20;
        MinemonCreature target = this.level.getEntitiesOfClass(MinemonCreature.class,
                        this.getBoundingBox().inflate(10.0D), candidate -> candidate != this
                                && candidate.isAlive() && !candidate.isDefeated() && candidate.isTame()
                                && candidate.getHealth() < candidate.getMaxHealth()
                                && this.distanceToSqr(candidate) <= 100.0D)
                .stream().min(java.util.Comparator.comparingDouble(candidate ->
                        candidate.getHealth() / Math.max(1.0F, candidate.getMaxHealth()))).orElse(null);
        if (target == null) return;
        var current = target.getEffect(net.minecraft.world.effect.MobEffects.REGENERATION);
        if (current != null && (current.getAmplifier() > 0 || current.getDuration() >= 200)) return;
        boolean applied = target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.REGENERATION, 200, 0), this);
        if (!applied) return;
        this.chanseyCooldownTicks = PokemonProgression.chanseyCooldownTicks(this.getPokemonLevel());
        if (this.level instanceof ServerLevel serverLevel)
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                    target.getX(), target.getY() + target.getBbHeight() + 0.25D, target.getZ(), 5,
                    0.25D, 0.2D, 0.25D, 0.0D);
    }
    public boolean isRocketSpecial() { return this.entityData.get(ROCKET_SPECIAL); }
    public void setRocketSpecial(boolean special) { this.entityData.set(ROCKET_SPECIAL, special); }
    public void setRocketVillager(UUID villager) { this.getPersistentData().putUUID("RocketVillager", villager); }

    /** Capture is blocked while the Pokémon is sleeping, breeding, or engaged in combat. */
    public boolean canBeStoredInPokeball() {
        return !this.isDeadOrDying() && !this.isSleeping() && !this.isNightResting()
                && !this.isInLove() && this.getTarget() == null && this.attackAnimationTicks <= 0
                && !this.isPassenger();
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        this.pendingBreedOffspring = null;
        if (!(partner instanceof MinemonCreature other)
                || !this.isTame() || !other.isTame()
                || this.getOwnerUUID() == null || !this.getOwnerUUID().equals(other.getOwnerUUID())
                || !isBreedingPermitted(this.getSpeciesSlug()) || !isBreedingPermitted(other.getSpeciesSlug())) return null;
        String initialForm = PokemonBreeding.chooseInitialForm(
                this.getSpeciesSlug(), other.getSpeciesSlug(), this.getRandom()::nextDouble);
        var babyType = br.com.petrickfaria.minemon.registry.ModEntities.SPECIES.get(initialForm);
        if (babyType == null) return null;
        MinemonCreature child = babyType.get().create(level);
        if (child != null) {
            child.setOwnerUUID(this.getOwnerUUID());
            child.setTame(true);
            child.setOrderedToSit(false);
            this.pendingBreedOffspring = child.getUUID();
        }
        return child;
    }

    @Override
    public boolean canMate(Animal partner) {
        if (!(partner instanceof MinemonCreature other) || other == this) return false;
        UUID owner = this.getOwnerUUID();
        if (owner == null || !owner.equals(other.getOwnerUUID())) return false;
        if (!this.isTame() || !other.isTame()) return false;
        if (!isBreedingPermitted(this.getSpeciesSlug()) || !isBreedingPermitted(other.getSpeciesSlug())) return false;
        if (isBreedingBusy() || other.isBreedingBusy()) return false;
        if (!hasSafeBabyPosition((ServerLevel)this.level, this) || !hasSafeBabyPosition((ServerLevel)other.level, other)) return false;
        // Animal.canMate requires identical Java classes; aquatic Minemon use a subclass,
        // so reproduce its remaining age/love rules without excluding cross-class species.
        return !this.isBaby() && !other.isBaby()
                && this.getAge() == 0 && other.getAge() == 0
                && this.isInLove() && other.isInLove();
    }

    private boolean isBreedingBusy() {
        return this.isDefeated() || this.isSleeping() || this.isNightResting() || this.isOrderedToSit() || this.getTarget() != null || this.attackAnimationTicks > 0;
    }

    private static boolean isBreedingPermitted(String species) {
        return MinemonConfig.TREAT_BREEDING_ENABLED.get()
                && !NON_BREEDABLE_SPECIES.contains(species)
                && !MinemonConfig.TREAT_DISABLED_BREEDING_SPECIES.get().contains(species);
    }

    private static boolean isUnsafeBabyBlock(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.LAVA)
                || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE);
    }

    private static boolean hasSafeBabyPosition(ServerLevel level, MinemonCreature parent) {
        double width = Math.max(0.2D, parent.getBbWidth() * 0.5D);
        double height = Math.max(0.2D, parent.getBbHeight() * 0.5D);
        BlockPos origin = parent.blockPosition();
        int[][] offsets = {{0,0},{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
        for (int[] offset : offsets) {
            for (int dy = 0; dy <= 1; dy++) {
                BlockPos feet = origin.offset(offset[0], dy, offset[1]);
                BlockPos floor = feet.below();
                if (level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()) continue;
                if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) continue;
                if (isUnsafeBabyBlock(level, floor) || isUnsafeBabyBlock(level, feet) || isUnsafeBabyBlock(level, feet.above())) continue;
                double x = feet.getX() + 0.5D, y = feet.getY(), z = feet.getZ() + 0.5D;
                AABB box = new AABB(x - width/2, y, z - width/2, x + width/2, y + height, z + width/2);
                if (level.getWorldBorder().isWithinBounds(box) && level.noCollision(box)) return true;
            }
        }
        return false;
    }

    @Override
    public void spawnChildFromBreeding(ServerLevel level, Animal partner) {
        super.spawnChildFromBreeding(level, partner);
        UUID offspringId = this.pendingBreedOffspring;
        this.pendingBreedOffspring = null;
        if (offspringId == null) return;
        int cooldown = MinemonConfig.TREAT_BREEDING_COOLDOWN_TICKS.get();
        this.setAge(cooldown);
        partner.setAge(cooldown);
        net.minecraft.world.entity.Entity spawned = level.getEntity(offspringId);
        if (spawned instanceof MinemonCreature baby) {
            BlockPos safe = findSafeBabyPosition(level, this, baby);
            if (safe != null && level.getBlockState(safe).canBeReplaced(level.getFluidState(safe).getType())) {
                var eggBlock = br.com.petrickfaria.minemon.registry.ModBlocks.BREEDING_EGG.get();
                if (level.setBlock(safe, eggBlock.defaultBlockState(), 3)) {
                    var blockEntity = level.getBlockEntity(safe);
                    if (blockEntity instanceof BreedingEggBlockEntity egg) {
                        egg.initialize(baby.getSpeciesSlug(), baby.getOwnerUUID(), BreedingEggBlockEntity.INCUBATION_TICKS);
                        baby.discard();
                    } else {
                        level.setBlock(safe, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static BlockPos findSafeBabyPosition(ServerLevel level, MinemonCreature parent, MinemonCreature baby) {
        BlockPos origin = parent.blockPosition();
        int[][] offsets = {{0,0},{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
        for (int[] offset : offsets) {
            for (int dy = 0; dy <= 1; dy++) {
                BlockPos feet = origin.offset(offset[0], dy, offset[1]);
                BlockPos floor = feet.below();
                if (level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()) continue;
                if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) continue;
                if (isUnsafeBabyBlock(level, floor) || isUnsafeBabyBlock(level, feet) || isUnsafeBabyBlock(level, feet.above())) continue;
                baby.setPos(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D);
                AABB box = baby.getBoundingBox();
                if (level.getWorldBorder().isWithinBounds(box) && level.noCollision(baby, box)) return feet;
            }
        }
        return null;
    }

    @Override
    protected void registerGoals() {
        if (!this.isAquaticSpecies()) this.goalSelector.addGoal(0, new WaterEscapeGoal(this));
        this.goalSelector.addGoal(0,new NightRestGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this) {
            @Override public boolean canUse(){return (!MinemonCreature.this.commandLanding() || MinemonCreature.this.isOnGround()) && super.canUse();}
        });
        this.goalSelector.addGoal(1, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new ElementalCombatGoal(this));
        this.goalSelector.addGoal(3, new NaturalFollowOwnerGoal(this));
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
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                target -> MinemonCreature.this.isTame() && !MinemonCreature.this.isOrderedToSit()
                        && target instanceof net.minecraft.world.entity.Mob mob
                        && MinemonCreature.this.isBlackListTarget(mob)) {
            @Override protected net.minecraft.world.phys.AABB getTargetSearchArea(double radius){return this.mob.getBoundingBox().inflate(10.0D);}
        });
        this.targetSelector.addGoal(4, new HurtByTargetGoal(this) {
            @Override public boolean canUse() {
                return (MinemonCreature.this.isTame() || MinemonCreature.this.wildAggressionTicks > 0) && super.canUse();
            }
            @Override public boolean canContinueToUse() {
                return (MinemonCreature.this.isTame() || MinemonCreature.this.wildAggressionTicks > 0) && super.canContinueToUse();
            }
        });
    }

    private static void consumeTreat(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) stack.shrink(1);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (isDefeated() || target == null || target == this || this.isOwnerEntity(target)) return false;
        if (this.isTame() && !CombatGeometry.inRange(this,target,10)) return false;
        // Ordered sitting companions may defend themselves or their owner at close
        // range, but must not start walking after a distant target.
        if (this.isTame() && this.isOrderedToSit() && !CombatGeometry.inRange(this,target,10)) return false;
        if (target instanceof MinemonCreature other) {
            if(other.isDefeated())return false;
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

    private static final String BLACK_LIST_KEY = "MinemonBlackList";
    public boolean isBlackListTarget(net.minecraft.world.entity.Mob target) {
        if (target == null || target == this || target instanceof MinemonCreature pokemonTarget && pokemonTarget.isDefeated() || target.isInvulnerable() || !target.isAlive()
                || this.isOwnerEntity(target)) return false;
        if (target instanceof MinemonCreature pokemon && pokemon.isTame()
                && this.getOwnerUUID() != null && this.getOwnerUUID().equals(pokemon.getOwnerUUID())) return false;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (id == null) return false;
        CompoundTag overrides = this.getPersistentData().getCompound(BLACK_LIST_KEY);
        return overrides.contains(id.toString()) ? overrides.getBoolean(id.toString())
                : MobTargetCategories.defaultSelected(target.getType());
    }
    public Map<String, Boolean> getBlackListOverrides() {
        CompoundTag stored = this.getPersistentData().getCompound(BLACK_LIST_KEY);
        Map<String, Boolean> result = new TreeMap<>();
        for (String id : stored.getAllKeys()) result.put(id, stored.getBoolean(id));
        return result;
    }
    public void setBlackListOverride(ResourceLocation type, boolean selected) {
        CompoundTag stored = this.getPersistentData().getCompound(BLACK_LIST_KEY);
        stored.putBoolean(type.toString(), selected);
        this.getPersistentData().put(BLACK_LIST_KEY, stored);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        // A capture item in either hand owns the interaction. Failed, occupied, and
        // foreign-owner attempts must not fall through into the Pokémon screen.
        for (InteractionHand ballHand : InteractionHand.values()) {
            ItemStack ball = player.getItemInHand(ballHand);
            if (!br.com.petrickfaria.minemon.item.PokeballItem.isOccupied(ball)
                    && ball.getItem() instanceof br.com.petrickfaria.minemon.item.PokeballItem pokeball) {
                InteractionResult result = pokeball.interactLivingEntity(ball, player, this, ballHand);
                return result == InteractionResult.PASS || result == InteractionResult.FAIL
                        ? InteractionResult.CONSUME : result;
            }
        }
        if (held.getItem() instanceof PokemonTreatItem treat) {
            boolean compatible = PokemonTreatTypes.accepts(this.getSpeciesSlug(), treat.getTreatType().pokemonType());
            if (this.level.isClientSide) return InteractionResult.sidedSuccess(compatible);
            if (!compatible || this.getTarget() != null || this.attackAnimationTicks > 0)
                return InteractionResult.sidedSuccess(false);
            if (!this.isTame()) {
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
                return InteractionResult.sidedSuccess(false);
            }
            if (!this.isOwnedBy(player)) return InteractionResult.sidedSuccess(false);
            float maximum = this.getMaxHealth();
            if (this.getHealth() < maximum) {
                float amount = Math.min((float)MinemonConfig.TREAT_HEAL_AMOUNT.get(), maximum - this.getHealth());
                if (amount <= 0.0F) return InteractionResult.sidedSuccess(false);
                this.setHealth(this.getHealth() + amount);
                if(isDefeated() && this.getHealth()>=this.getMaxHealth()){
                    this.entityData.set(DEFEATED,false);this.defeatTargetsCleared=false;this.setNoAi(aiBeforeDefeat);this.setSleeping(false);
                }
                consumeTreat(player, held);
                this.level.broadcastEntityEvent(this, (byte)18);
                return InteractionResult.sidedSuccess(false);
            }
            if (!this.isInLove() && !this.isBaby() && this.getAge() == 0 && !this.isBreedingBusy()
                    && isBreedingPermitted(this.getSpeciesSlug()) && hasSafeBabyPosition((ServerLevel)this.level, this)) {
                this.setInLove(player);
                consumeTreat(player, held);
                this.level.broadcastEntityEvent(this, (byte)18);
            }
            return InteractionResult.sidedSuccess(false);
        }
        // TamableAnimal has vanilla bone taming; handle it here without mutation.
        if (held.is(Items.BONE)) {
            return InteractionResult.sidedSuccess(this.level.isClientSide);
        }

        if (this.isTame() && this.isOwnedBy(player) && !this.isFood(held)) {
            if (!this.level.isClientSide && player instanceof ServerPlayer serverPlayer)
                br.com.petrickfaria.minemon.menu.PokemonMenus.openInfo(serverPlayer, this);
            return InteractionResult.sidedSuccess(this.level.isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if(isDefeated())return false;
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
        if(!this.level.isClientSide&&!(Boolean.getBoolean("minemon.clientSmokeTest")&&this.getTags().contains("minemon-locomotion-fixture"))){if(!isDefeated())this.tickNightRest();}
        super.tick();
        if(!level.isClientSide && isTame())rangedCharges.tick(!isDefeated());
        if(level.isClientSide)tickGastlySmoke();
        if(isDefeated()){
            this.getNavigation().stop();this.setTarget(null);this.setJumping(false);
            this.setSpeed(0);this.setXxa(0);this.setYya(0);this.setZza(0);
            if(!level.isClientSide){
                if(!defeatTargetsCleared){DefeatedTargetProtection.clear(this);defeatTargetsCleared=true;}
                defeatedRest.tick();
            }else this.setDeltaMovement(0,0,0);
            if(!level.isClientSide && tickCount%12==0)((ServerLevel)level).sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    getX(),getY()+getBbHeight()*.5,getZ(),1,getBbWidth()*.2,getBbHeight()*.15,getBbWidth()*.2,.005);
            return;
        }
        if(!level.isClientSide)ShoreTraversal.tick(this);
        if(this.getTarget()!=null && !canAttack(this.getTarget())){this.setTarget(null);this.getNavigation().stop();}
        if (!this.level.isClientSide) {
            this.tickNaturalCycle();
            this.tickChanseyHealing();
            this.tickCustomSoundTimer();
            // Keep the challenge entity in the Rocket workstation's loaded area so an
            // unloaded chunk cannot look like a vanished Rattata and spawn a duplicate.
            if (this.isRocketSpecial() && this.getPersistentData().hasUUID("RocketVillager")
                    && this.level instanceof ServerLevel serverLevel) {
                var linked = serverLevel.getEntity(this.getPersistentData().getUUID("RocketVillager"));
                if (linked instanceof net.minecraft.world.entity.npc.Villager villager
                        && this.distanceToSqr(villager) > 36.0D * 36.0D) {
                    this.moveTo(villager.getX() + 1.5D, villager.getY(), villager.getZ() + 0.5D,
                            this.getYRot(), this.getXRot());
                    this.setTarget(null);
                    this.getNavigation().stop();
                }
            }
        }
        if (this.level.isClientSide) {
            if (this.isRocketSpecial() && !this.isDeadOrDying() && this.tickCount % 3 == 0) {
                double spread = Math.max(0.15D, this.getBbWidth() * 0.38D);
                this.level.addParticle(new net.minecraft.core.particles.DustParticleOptions(
                                new com.mojang.math.Vector3f(0.19F, 0.025F, 0.27F), 1.0F),
                        this.getX() + (this.random.nextDouble() - 0.5D) * spread,
                        this.getY() + this.getBbHeight() * (0.35D + this.random.nextDouble() * 0.45D),
                        this.getZ() + (this.random.nextDouble() - 0.5D) * spread,
                        0.0D, 0.025D, 0.0D);
            }
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

    private void tickGastlySmoke(){
        if(!getSpeciesSlug().equals("gastly") || isRemoved())return;
        int phase=Math.floorMod(tickCount,16);
        int large=switch(phase){case 0->0;case 3->1;case 6->2;case 9->3;case 12->4;default->-1;};
        int small=phase%2==1 && phase<14?phase/2:-1;
        if(large>=0)level.addParticle(br.com.petrickfaria.minemon.registry.ModParticles.GASTLY_GHOSTLY_SMOKE.get(),getX(),getY(),getZ(),getId(),large,0);
        if(small>=0)level.addParticle(br.com.petrickfaria.minemon.registry.ModParticles.GASTLY_SMALL_SMOKE.get(),getX(),getY(),getZ(),getId(),small+5,0);
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
            this.customSoundTicks = 20 + this.getRandom().nextInt(181);
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
        boolean night=this.level.dimensionType().hasSkyLight()
                &&SpeciesBehaviorProfiles.shouldSleep(this.getSpeciesSlug(), this.level.getDayTime());
        boolean waterRest=SpeciesBehaviorProfiles.waterCapable(this.getSpeciesSlug())&&this.isInWaterOrBubble();
        boolean restingEnvironment=SpeciesBehaviorProfiles.canFly(this.getSpeciesSlug())||waterRest||!SpeciesBehaviorProfiles.isAquatic(this.getSpeciesSlug())&&!this.isInWaterOrBubble();
        boolean eligible=restingEnvironment&&!this.isOrderedToSit()&&!this.isNoAi()
            &&!this.isPassenger()&&!this.isDeadOrDying()
            &&target==null&&this.wakeGraceTicks==0;
        if(night&&eligible){
            if(!this.nightRoutine){
                this.nightRoutine=true;this.getNavigation().stop();this.stopSpontaneousRun();
            }
            this.entityData.set(NIGHT_REST,true);
            if(waterRest&&!Double.isFinite(this.aquaticSleepY)&&!SpeciesBehaviorProfiles.restsAtWaterBottom(this.getSpeciesSlug())){
                double surface=WaterMovementGoal.restSurfaceAt(this,this.blockPosition());
                // Choose once per sleep episode, including migration of older saves.
                boolean atSurface=Double.isFinite(surface)&&this.getBoundingBox().maxY>=surface-.08D;
                this.aquaticSleepY=atSurface?Math.min(this.getY(),surface-2):this.getY();
            }
            boolean hover=SpeciesBehaviorProfiles.hoversDuringNightRest(this.getSpeciesSlug());
            double restHeight=SpeciesBehaviorProfiles.nightRestHoverHeight(this.getSpeciesSlug());
            boolean settled=waterRest?Math.abs(this.getY()-WaterMovementGoal.waterRestY(this))<0.18D
                :hover?Math.abs(this.getY()-this.naturalSurface()-restHeight)<0.18D
                :this.isOnGround()&&WaterMovementGoal.safeDry(this,this.blockPosition());
            if(settled&&!this.isSleeping())this.setSleeping(true);
        }else{
            this.entityData.set(NIGHT_REST,false);
            if(this.nightRoutine){this.nightRoutine=false;this.aquaticSleepY=Double.NaN;this.setSleeping(false);this.getNavigation().stop();}
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
        if (!(target instanceof LivingEntity living) || !this.canAttack(living) || !combatReady() || !CombatGeometry.inRange(this,living,.5D)) return false;
        boolean hit=super.doHurtTarget(target);
        if(hit && !this.level.isClientSide){
            if(!isTame())successfulMeleeAttacks++;nextCombatTick=level.getGameTime()+(isTame()?60:50);
            this.level.broadcastEntityEvent(this,(byte)61);
        }
        return hit;
    }
    public boolean projectileReady(){return isTame()?rangedCharges.charges()>0:successfulMeleeAttacks>=4;}
    public boolean canFireAt(LivingEntity target){return canAttack(target) && target.isAlive() && CombatGeometry.inRange(this,target,isTame()?10:5) && getSensing().hasLineOfSight(target);}
    public boolean combatReady(){return !isDefeated() && !isSleeping() && level.getGameTime()>=nextCombatTick;}
    public boolean fireElementalProjectile(LivingEntity target){
        if(level.isClientSide || !projectileReady() || !combatReady() || !canAttack(target)
                || !canFireAt(target))return false;
        ElementalProjectile shot=new ElementalProjectile(this,target);
        if(!level.addFreshEntity(shot))return false;
        if(isTame())rangedCharges.consume();else successfulMeleeAttacks=0;nextCombatTick=level.getGameTime()+(isTame()?60:50);
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
        attackAnimationTicks=0;stopSpontaneousRun();setFollowingRun(false);
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
            String restKey=slug.equals("vaporeon")
                    ? (this.isInWaterOrBubble()?"night_sleep_water":"night_sleep_land") : "night_sleep";
            clip = SpeciesAssetIndex.clip(slug, restKey, "night_sleep");
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
