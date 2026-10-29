package br.com.petrickfaria.minemon.item;

import br.com.petrickfaria.minemon.entity.MinemonCreature;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeSpawnEggItem;

/** Spawn egg used by PokéC; the owner tag is only present on purchased eggs. */
public final class PokecOwnedSpawnEggItem extends ForgeSpawnEggItem {
    private final Supplier<? extends EntityType<? extends Mob>> type;

    public PokecOwnedSpawnEggItem(Supplier<? extends EntityType<? extends Mob>> type, int baseColor, int spotColor, Properties properties) {
        super(type, baseColor, spotColor, properties);
        this.type = type;
    }

    public static ItemStack ownedStack(ItemStack template, UUID owner, String ownerName) {
        ItemStack stack = template.copy();
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID("MinemonPokecOwner", owner);
        tag.putString("MinemonPokecOwnerName", ownerName);
        return stack;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (stack.getTag() == null || !stack.getTag().hasUUID("MinemonPokecOwner"))
            return super.useOn(context);
        if (context.getLevel().isClientSide) return InteractionResult.sidedSuccess(true);
        if (!(context.getPlayer() instanceof ServerPlayer player)
                || !player.getUUID().equals(stack.getTag().getUUID("MinemonPokecOwner"))
                || !(context.getLevel() instanceof ServerLevel level)) return InteractionResult.FAIL;

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Mob pokemon = type.get().create(level);
        if (pokemon == null) return InteractionResult.FAIL;
        pokemon.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.getYRot(), 0);
        if (!level.getWorldBorder().isWithinBounds(pokemon.getBoundingBox())
                || !level.noCollision(pokemon, pokemon.getBoundingBox())
                || !level.getFluidState(pos).isEmpty()) {
            pokemon.discard();
            return InteractionResult.FAIL;
        }
        if (pokemon instanceof MinemonCreature creature) {
            creature.tame(player);
            creature.setOwnerUUID(player.getUUID());
            creature.setOrderedToSit(true);
        }
        if (!level.addFreshEntity(pokemon)) {
            pokemon.discard();
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.CONSUME;
    }
}
