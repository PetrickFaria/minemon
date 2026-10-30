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
    public ElementalProjectile(EntityType<? extends ElementalProjectile> type,Level level){super(type,level);}
    public ElementalProjectile(MinemonCreature attacker,LivingEntity target){
        this(ModEntities.ELEMENTAL_PROJECTILE.get(),attacker.level);
        setOwner(attacker);setPos(attacker.getX(),attacker.getEyeY()-.1,attacker.getZ());
        damage=(float)attacker.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        targetId=target.getUUID();
        var types=br.com.petrickfaria.minemon.pokec.PokemonTreatTypes.forSpecies(attacker.getSpeciesSlug());
        entityData.set(COLOR,EggAppearance.color(types.isEmpty()?"normal":types.get(0)));
        Vec3 direction=target.getBoundingBox().getCenter().subtract(position());
        shoot(direction.x,direction.y,direction.z,.65F,0);
    }
    public int color(){return entityData.get(COLOR);}
    @Override protected void defineSynchedData(){entityData.define(COLOR,0xFFFFFF);}
    @Override protected float getGravity(){return 0;}
    @Override protected boolean canHitEntity(net.minecraft.world.entity.Entity entity){
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
        if(getOwner() instanceof MinemonCreature owner && !owner.isDefeated()
                && result.getEntity() instanceof LivingEntity target
                && target.getUUID().equals(targetId) && owner.canAttack(target))
            target.hurt(DamageSource.indirectMobAttack(this,owner),(float)owner.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
        discard();
    }
    @Override protected void onHitBlock(BlockHitResult result){if(!level.isClientSide)discard();}
    @Override public void tick(){super.tick();if(!level.isClientSide && tickCount>40)discard();}
    @Override protected void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("ElementColor",color());tag.putFloat("Damage",damage);if(targetId!=null)tag.putUUID("Target",targetId);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(COLOR,tag.getInt("ElementColor"));damage=tag.getFloat("Damage");targetId=tag.hasUUID("Target")?tag.getUUID("Target"):null;}
    @Override public net.minecraft.network.protocol.Packet<?> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
