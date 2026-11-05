package br.com.petrickfaria.minemon.item;

import java.util.UUID;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeSpawnEggItem;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.pokec.WildEncounterManager;

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
        if (!(context.getLevel() instanceof ServerLevel level)
                || !(context.getPlayer() instanceof ServerPlayer player)) return super.useOn(context);
        EntityType<?> type = getType(context.getItemInHand().getTag());
        AABB area = new AABB(context.getClickedPos()).inflate(4.0D);
        Set<UUID> existing = level.getEntitiesOfClass(MinemonCreature.class, area,
                pokemon -> pokemon.getType() == type).stream().map(MinemonCreature::getUUID)
                .collect(java.util.stream.Collectors.toSet());
        InteractionResult result = super.useOn(context);
        if (!result.consumesAction()) return result;
        var spawned = level.getEntitiesOfClass(MinemonCreature.class, area,
                        pokemon -> pokemon.getType() == type && !existing.contains(pokemon.getUUID()))
                .stream().min(java.util.Comparator.comparingDouble(pokemon -> pokemon.distanceToSqr(
                        context.getClickedPos().getX() + .5D, context.getClickedPos().getY() + .5D,
                        context.getClickedPos().getZ() + .5D)));
        spawned.ifPresent(pokemon -> WildEncounterManager.registerSpawnEggEncounter(pokemon, player));
        return result;
    }
}
