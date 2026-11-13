package br.com.petrickfaria.minemon.entity;

import br.com.petrickfaria.minemon.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.*;
import net.minecraft.world.damagesource.DamageSource;

/** Direct, single-hit damage only: no fire, explosion, terrain edit or area damage. */
public final class ElementalProjectile extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> COLOR=SynchedEntityData.defineId(ElementalProjectile.class,EntityDataSerializers.INT);
    private float damage;
    private java.util.UUID targetId;
    private Vec3 launchPosition;
    private double maxTravelDistance;
    public ElementalProjectile(EntityType<? extends ElementalProjectile> type,Level level){super(type,level);}
    public ElementalProjectile(MinemonCreature attacker,LivingEntity target){
        this(ModEntities.ELEMENTAL_PROJECTILE.get(),attacker.level);
        setOwner(attacker);setPos(attacker.getX(),attacker.getEyeY()-.1,attacker.getZ());
        launchPosition=position();
        maxTravelDistance=attacker.attackRange();
        damage=(float)attacker.pokemonAttackDamage();
        targetId=target.getUUID();
        var types=br.com.petrickfaria.minemon.pokec.PokemonTreatTypes.forSpecies(attacker.getSpeciesSlug());
        entityData.set(COLOR,EggAppearance.color(types.isEmpty()?"normal":types.get(0)));
        Vec3 aim=attacker.attackAimPoint(target);
        Vec3 direction=(aim==null?target.getBoundingBox().getCenter():aim).subtract(position());
        shoot(direction.x,direction.y,direction.z,.65F,0);
    }
    public int color(){return entityData.get(COLOR);}
    /** Damage value attached to this specific server-side shot. */
    public float damageForValidation(){return damage;}
    public boolean getTargetIdForValidation(LivingEntity target){return targetId!=null&&target.getUUID().equals(targetId);}
    float configuredDamageForValidation(){return damage;}
    @Override protected void defineSynchedData(){entityData.define(COLOR,0xFFFFFF);}
    @Override protected float getGravity(){return 0;}
    @Override protected boolean canHitEntity(net.minecraft.world.entity.Entity entity){
        // This shot is aimed at one authorized target. Creatures crossing its
        // path (including the attacker's allies) must not absorb and waste it.
        if(targetId!=null&&!targetId.equals(entity.getUUID()))return false;
        if(!super.canHitEntity(entity))return false;
        if(getOwner() instanceof MinemonCreature owner && entity instanceof LivingEntity living){
            if(owner.getOwnerUUID()!=null && owner.getOwnerUUID().equals(living.getUUID()))return false;
            if(living instanceof MinemonCreature pokemon && owner.getOwnerUUID()!=null
                    && owner.getOwnerUUID().equals(pokemon.getOwnerUUID()))return false;
        }
        return true;
    }
    @Override protected void onHitEntity(EntityHitResult result){
        if(level.isClientSide)return;
        try {
            if(getOwner() instanceof MinemonCreature owner && !owner.isDefeated()
                    && result.getEntity() instanceof LivingEntity target
                    && target.getUUID().equals(targetId) && owner.canAttack(target)) {
                target.hurt(DamageSource.indirectMobAttack(this,owner),damage);
            }
        } finally {
            // One projectile represents one attack. A blocked hit cannot be retried later.
            discard();
        }
    }
    @Override protected void onHitBlock(BlockHitResult result){if(!level.isClientSide)discard();}
    @Override public void tick(){
        super.tick();
        // ThrowableProjectile applies 0.8 water drag after each movement. Restore
        // the normal 0.99 air retention so a straight shot keeps its intended range.
        if(isInWater())setDeltaMovement(getDeltaMovement().scale(0.99D/0.8D));
        if(!level.isClientSide && launchPosition!=null && maxTravelDistance>0.0D){
            Vec3 traveled=position().subtract(launchPosition);
            if(traveled.lengthSqr()>maxTravelDistance*maxTravelDistance){
                setPos(launchPosition.add(traveled.normalize().scale(maxTravelDistance)));
                discard();
            }
        }
        // Level 10 companions reach 23 blocks without changing projectile speed;
        // keep the projectile alive long enough to cover that configured range.
        if(!level.isClientSide && tickCount>60)discard();
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);tag.putInt("ElementColor",color());tag.putFloat("Damage",damage);
        if(targetId!=null)tag.putUUID("Target",targetId);
        if(launchPosition!=null){tag.putDouble("LaunchX",launchPosition.x);tag.putDouble("LaunchY",launchPosition.y);tag.putDouble("LaunchZ",launchPosition.z);}
        tag.putDouble("MaxTravelDistance",maxTravelDistance);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);entityData.set(COLOR,tag.getInt("ElementColor"));damage=tag.getFloat("Damage");
        targetId=tag.hasUUID("Target")?tag.getUUID("Target"):null;
        launchPosition=tag.contains("LaunchX")?new Vec3(tag.getDouble("LaunchX"),tag.getDouble("LaunchY"),tag.getDouble("LaunchZ")):position();
        maxTravelDistance=tag.contains("MaxTravelDistance")?tag.getDouble("MaxTravelDistance"):40.0D;
    }
    @Override public net.minecraft.network.protocol.Packet<?> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
