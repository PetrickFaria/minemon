package br.com.petrickfaria.minemon.item;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.common.ForgeSpawnEggItem;

/** Spawn egg used by PokéC; the owner tag is only present on purchased eggs. */
public final class PokecOwnedSpawnEggItem extends ForgeSpawnEggItem {
    public PokecOwnedSpawnEggItem(Supplier<? extends EntityType<? extends Mob>> type, int baseColor, int spotColor, Properties properties) {
        super(type, baseColor, spotColor, properties);
    }

    public static ItemStack ownedStack(ItemStack template, UUID owner, String ownerName) {
        ItemStack stack = template.copy();
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID("MinemonPokecOwner", owner);
        tag.putString("MinemonPokecOwnerName", ownerName);
        return stack;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        // Spawn eggs create ordinary wild encounters even when the egg was bought
        // through PokéC. Ownership and active-slot limits apply only after taming.
        return super.useOn(context);
    }
}
