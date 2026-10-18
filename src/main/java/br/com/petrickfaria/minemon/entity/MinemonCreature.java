package br.com.petrickfaria.minemon.entity;

import br.com.petrickfaria.minemon.config.MinemonConfig;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
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
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

/** Shared wolf-like companion behavior. This first registered species is the integration fixture. */
public class MinemonCreature extends TamableAnimal implements IAnimatable {
    private static final EntityDataAccessor<Boolean> ATTACKING =
            SynchedEntityData.defineId(MinemonCreature.class, EntityDataSerializers.BOOLEAN);
    private final AnimationFactory animationFactory = GeckoLibUtil.createFactory(this);

    public MinemonCreature(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
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

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATTACKING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, false));
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        // Wild creatures only acquire a target after being hurt. Tamed creatures also protect
        // their owner and assist against the owner's current target.
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target == this.getOwner()) return false;
        if (target instanceof MinemonCreature other) {
            if (this.getOwnerUUID() == null && other.getOwnerUUID() == null) return false;
            if (this.getOwnerUUID() != null && this.getOwnerUUID().equals(other.getOwnerUUID())) return false;
        }
        return super.canAttack(target);
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
        if (hurt && source.getEntity() instanceof LivingEntity attacker && this.canAttack(attacker)) {
            this.setTarget(attacker);
            this.entityData.set(ATTACKING, true);
        }
        return hurt;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getTarget() == null || !this.getTarget().isAlive()) this.entityData.set(ATTACKING, false);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 7) this.level.addParticle(net.minecraft.core.particles.ParticleTypes.HEART,
                this.getX(), this.getY() + 0.5D, this.getZ(), 0.0D, 0.1D, 0.0D);
        else if (id == 6) this.level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                this.getX(), this.getY() + 0.5D, this.getZ(), 0.0D, 0.1D, 0.0D);
        else super.handleEntityEvent(id);
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "movement", 4, this::animationPredicate));
    }

    private <E extends IAnimatable> PlayState animationPredicate(AnimationEvent<E> event) {
        AnimationController<?> controller = event.getController();
        String slug = this.getSpeciesSlug();
        String clip;
        boolean loop = true;
        if (this.isDeadOrDying()) {
            clip = SpeciesAssetIndex.clip(slug, "faint", "ground_idle");
            loop = false;
        } else if (event.isMoving() || this.entityData.get(ATTACKING)) {
            clip = SpeciesAssetIndex.clip(slug, "ground_walk", "ground_idle");
        } else {
            clip = SpeciesAssetIndex.clip(slug, "ground_idle", "idle");
        }
        if (clip != null) controller.setAnimation(new AnimationBuilder().addAnimation(clip, loop));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimationFactory getFactory() {
        return this.animationFactory;
    }
}
