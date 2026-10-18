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
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
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
    public boolean isFlyingSpecies() { return false; }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return (AgeableMob) this.getType().create(level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, this.isFlyingSpecies()));
        this.goalSelector.addGoal(4, this.isFlyingSpecies() ? new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal(this, 0.8D) : new RandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

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
        if (this.level.isClientSide) {
            if (this.attackAnimationTicks > 0) this.attackAnimationTicks--;
            if (this.blinkCooldownTicks > 0) this.blinkCooldownTicks--;
            if (this.blinkCooldownTicks == 0) {
                this.blinkPending = true;
                this.blinkCooldownTicks = 80 + this.getRandom().nextInt(120);
            }
        } else {
            this.tickWildAggression();
        }
    }

    private void tickWildAggression() {
        if (this.isTame()) {
            this.wildAggressionTicks = 0;
            this.wildUnreachableTicks = 0;
            return;
        }
        if (this.wildAggressionTicks <= 0) return;

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
        if (id == 61) { this.attackAnimationTicks=16; this.attackSequence++; }
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
        } else if (this.isMoving(event)) {
            clip = definition.walk();
            String run=SpeciesAssetIndex.clip(slug,"ground_run","run");
            if(!this.isFlyingSpecies() && run!=null && this.getDeltaMovement().horizontalDistanceSqr()>0.01D) clip=run;
        } else {
            clip = definition.idle();
        }
        if (clip != null) controller.setAnimation(new AnimationBuilder().addAnimation(clip, loop));
        return PlayState.CONTINUE;
    }

    private boolean isMoving(AnimationEvent<?> event) {
        return !this.isOrderedToSit() && (event.isMoving() || this.getDeltaMovement().horizontalDistanceSqr() > 0.0004D || (this.getX()-this.xo)*(this.getX()-this.xo)+(this.getZ()-this.zo)*(this.getZ()-this.zo)>0.0001D);
    }

    private <E extends IAnimatable> PlayState blinkPredicate(AnimationEvent<E> event) {
        AnimationController<?> controller = event.getController();
        if (this.isDeadOrDying() || this.isMoving(event) || this.attackAnimationTicks > 0) return PlayState.STOP;

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
