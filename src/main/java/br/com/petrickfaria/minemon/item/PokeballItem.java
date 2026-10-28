package br.com.petrickfaria.minemon.item;

import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.pokec.PokecSpecies;
import br.com.petrickfaria.minemon.registry.ModEntities;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

/** Empty Pokéballs store only a domesticated Minemon owned by the interacting player. */
public final class PokeballItem extends Item {
    private static final String DATA_KEY = "MinemonStoredPokemon";
    private static final String POKEMON_KEY = "Pokemon";
    private static final String SLUG_KEY = "Species";
    private static final String OWNER_KEY = "Owner";
    private static final String OWNER_NAME_KEY = "OwnerName";
    private static final String TOKEN_KEY = "Token";
    private static final String HEALTH_KEY = "Health";
    private static final String MAX_HEALTH_KEY = "MaxHealth";

    public record StoredPokemon(String species, UUID owner, String ownerName, UUID token,
                                int level, float health, float maxHealth, CompoundTag entityData) { }

    public PokeballItem(Properties properties) { super(properties); }

    /** Forge's stack-aware limit lets empty balls stack to 64 and stored balls to one. */
    @Override public int getMaxStackSize(ItemStack stack) { return isOccupied(stack) ? 1 : 64; }

    public static boolean isOccupied(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof PokeballItem
                && stack.hasTag() && stack.getTag().contains(DATA_KEY, CompoundTag.TAG_COMPOUND);
    }

    public static StoredPokemon readStored(ItemStack stack) {
        if (!isOccupied(stack)) return null;
        CompoundTag data = stack.getTag().getCompound(DATA_KEY);
        CompoundTag entityData = data.getCompound(POKEMON_KEY);
        if (!data.hasUUID(OWNER_KEY) || !data.hasUUID(TOKEN_KEY) || entityData.isEmpty()) return null;
        String species = data.getString(SLUG_KEY);
        if (PokecSpecies.get(species) == null) return null;
        return new StoredPokemon(species, data.getUUID(OWNER_KEY), data.getString(OWNER_NAME_KEY),
                data.getUUID(TOKEN_KEY), Math.max(1, data.getInt("Level")),
                data.getFloat(HEALTH_KEY), data.getFloat(MAX_HEALTH_KEY), entityData.copy());
    }

    public static ItemStack emptyBall() { return new ItemStack(br.com.petrickfaria.minemon.registry.ModItems.POKEBALL.get()); }

    private static void store(ItemStack ball, MinemonCreature creature, ServerPlayer owner) {
        CompoundTag entityData = new CompoundTag();
        creature.saveWithoutId(entityData);
        var typeId = ForgeRegistries.ENTITY_TYPES.getKey(creature.getType());
        if (typeId == null) throw new IllegalStateException("Minemon entity has no registered type");
        entityData.putString("id", typeId.toString());

        CompoundTag data = new CompoundTag();
        data.put(POKEMON_KEY, entityData);
        data.putString(SLUG_KEY, creature.getSpeciesSlug());
        data.putUUID(OWNER_KEY, owner.getUUID());
        data.putString(OWNER_NAME_KEY, owner.getGameProfile().getName());
        data.putUUID(TOKEN_KEY, UUID.randomUUID());
        data.putInt("Level", creature.getPokemonLevel());
        data.putFloat(HEALTH_KEY, creature.getHealth());
        data.putFloat(MAX_HEALTH_KEY, creature.getMaxHealth());
        ball.getOrCreateTag().put(DATA_KEY, data);
    }

    private static void clearStored(ItemStack stack) {
        if (!stack.hasTag()) return;
        stack.getTag().remove(DATA_KEY);
        if (stack.getTag().isEmpty()) stack.setTag(null);
    }

    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                            LivingEntity target, InteractionHand hand) {
        if (!(target instanceof MinemonCreature creature) || isOccupied(stack)) return InteractionResult.PASS;
        if (player.level.isClientSide) return InteractionResult.sidedSuccess(true);
        if (!(player instanceof ServerPlayer serverPlayer) || !(player.level instanceof ServerLevel)
                || !creature.isTame() || !creature.isOwnedBy(player) || !creature.canBeStoredInPokeball())
            return InteractionResult.FAIL;

        ItemStack occupied = stack.copyWithCount(1);
        try { store(occupied, creature, serverPlayer); }
        catch (RuntimeException invalidEntity) { return InteractionResult.FAIL; }

        if (stack.getCount() == 1) {
            player.setItemInHand(hand, occupied);
        } else {
            stack.shrink(1);
            if (!player.getInventory().add(occupied)) {
                stack.grow(1);
                return InteractionResult.FAIL;
            }
        }
        creature.discard();
        return InteractionResult.CONSUME;
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack ball = player.getItemInHand(hand);
        if (!isOccupied(ball)) return InteractionResultHolder.pass(ball);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(ball, true);
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel))
            return InteractionResultHolder.fail(ball);
        BlockPos origin = BlockPos.containing(player.position().add(player.getLookAngle().scale(1.5D)));
        return release(ball, serverPlayer, serverLevel, hand, origin)
                ? InteractionResultHolder.consume(player.getItemInHand(hand))
                : InteractionResultHolder.fail(ball);
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        ItemStack ball = context.getItemInHand();
        if (!isOccupied(ball)) return InteractionResult.PASS;
        if (context.getLevel().isClientSide) return InteractionResult.sidedSuccess(true);
        if (!(context.getPlayer() instanceof ServerPlayer player) || !(context.getLevel() instanceof ServerLevel level))
            return InteractionResult.FAIL;
        BlockPos origin = context.getClickedPos().relative(context.getClickedFace());
        return release(ball, player, level, context.getHand(), origin) ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    private static boolean release(ItemStack ball, ServerPlayer player, ServerLevel level,
                                   InteractionHand hand, BlockPos origin) {
        if (ball.getCount() != 1) return false;
        StoredPokemon stored = readStored(ball);
        if (stored == null || !stored.owner().equals(player.getUUID())) return false;
        PokemonBallLedger ledger = PokemonBallLedger.get(level);
        if (ledger.isConsumed(stored.token())) return false;
        var entityType = ModEntities.SPECIES.get(stored.species());
        if (entityType == null) return false;
        MinemonCreature restored;
        try {
            restored = entityType.get().create(level);
            if (restored == null) return false;
            restored.load(stored.entityData().copy());
        } catch (RuntimeException invalidData) {
            return false;
        }
        if (!restored.isTame() || !player.getUUID().equals(restored.getOwnerUUID())) {
            restored.discard();
            return false;
        }
        BlockPos safe = findSafePosition(level, restored, origin);
        if (safe == null) { restored.discard(); return false; }
        restored.moveTo(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D,
                player.getYRot(), 0.0F);
        if (!level.addFreshEntity(restored)) { restored.discard(); return false; }
        if (!ledger.consume(stored.token())) {
            restored.discard();
            return false;
        }
        clearStored(ball);
        ball.setCount(1);
        player.setItemInHand(hand, ball);
        return true;
    }

    private static BlockPos findSafePosition(ServerLevel level, Entity entity, BlockPos origin) {
        for (int radius = 0; radius <= 2; radius++) {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos feet = origin.offset(dx, dy, dz), floor = feet.below();
                    if (!level.getWorldBorder().isWithinBounds(feet)
                            || !level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                            || level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()
                            || !level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()
                            || unsafe(level, feet) || unsafe(level, floor) || unsafe(level, feet.above())) continue;
                    double x = feet.getX() + 0.5D, y = feet.getY(), z = feet.getZ() + 0.5D;
                    AABB box = entity.getBoundingBox().move(x - entity.getX(), y - entity.getY(), z - entity.getZ());
                    if (level.noCollision(entity, box)) return feet;
                }
            }
        }
        return null;
    }

    private static boolean unsafe(ServerLevel level, BlockPos pos) {
        var block = level.getBlockState(pos).getBlock();
        return block == Blocks.FIRE || block == Blocks.SOUL_FIRE || block == Blocks.LAVA
                || block == Blocks.MAGMA_BLOCK || block == Blocks.CACTUS || block == Blocks.SWEET_BERRY_BUSH
                || block == Blocks.WITHER_ROSE || block == Blocks.CAMPFIRE || block == Blocks.SOUL_CAMPFIRE;
    }

    @Override public Component getName(ItemStack stack) {
        StoredPokemon stored = readStored(stack);
        if (stored == null) return Component.translatable("item.minemon.pokeball");
        PokecSpecies.Entry species = PokecSpecies.get(stored.species());
        return Component.translatable("item.minemon.pokeball.stored", species == null ? stored.species() : species.name());
    }

    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        StoredPokemon stored = readStored(stack);
        if (stored == null) {
            tooltip.add(Component.translatable("tooltip.minemon.pokeball.empty"));
            return;
        }
        PokecSpecies.Entry species = PokecSpecies.get(stored.species());
        tooltip.add(Component.translatable("tooltip.minemon.pokeball.species", species == null ? stored.species() : species.name()));
        tooltip.add(Component.translatable("tooltip.minemon.pokeball.level", stored.level()));
        tooltip.add(Component.translatable("tooltip.minemon.pokeball.health", stored.health(), stored.maxHealth()));
        tooltip.add(Component.translatable("tooltip.minemon.pokeball.owner", stored.ownerName()));
    }

    @Override public boolean isFoil(ItemStack stack) { return isOccupied(stack); }
}
