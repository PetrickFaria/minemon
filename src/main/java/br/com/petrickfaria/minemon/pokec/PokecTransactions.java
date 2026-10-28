package br.com.petrickfaria.minemon.pokec;

import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.item.PokeballItem;
import br.com.petrickfaria.minemon.item.PokemonBallLedger;
import br.com.petrickfaria.minemon.menu.PokecMenu;
import br.com.petrickfaria.minemon.registry.ModEntities;
import br.com.petrickfaria.minemon.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** All PokéC state changes are validated and committed on the server. */
public final class PokecTransactions {
    public enum Action { DEPOSIT, PURCHASE, SELECT, STAGE_COINS, STAGE_SOULS, CONFIRM_SOULS,
        STAGE_POKEBALL, RETURN_POKEBALL, CLAIM_CONVERSION_OUTPUT, REQUEST_CONVERSION, CONFIRM_CONVERSION, CANCEL_CONVERSION }
    private static final Map<UUID, Set<Long>> RECENT_IDS = new ConcurrentHashMap<>();

    public static boolean perform(ServerPlayer player, BlockPos pos, String slug, Action action, long id) {
        if (!(player.containerMenu instanceof PokecMenu menu) || !menu.pos().equals(pos) || !menu.stillValid(player)) return false;
        if (player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) > 64) return false;
        if (action != Action.STAGE_POKEBALL && action != Action.RETURN_POKEBALL
                && action != Action.CLAIM_CONVERSION_OUTPUT && action != Action.REQUEST_CONVERSION
                && action != Action.CONFIRM_CONVERSION && action != Action.CANCEL_CONVERSION
                && !PokecSpecies.ALL.containsKey(slug)) return false;
        if (!remember(player.getUUID(), id)) return false;
        synchronized (player.getPersistentData()) {
            PokecSpecies.Entry species = PokecSpecies.get(slug);
            if (action == Action.SELECT) {
                menu.selectSpecies(player, slug);
                return true;
            }
            if (action != Action.SELECT && action != Action.STAGE_POKEBALL && action != Action.RETURN_POKEBALL
                    && action != Action.CLAIM_CONVERSION_OUTPUT && action != Action.REQUEST_CONVERSION
                    && action != Action.CONFIRM_CONVERSION && action != Action.CANCEL_CONVERSION
                    && !menu.selectedSlug().equals(slug)) return false;
            return switch (action) {
                case STAGE_POKEBALL -> stagePokeball(player, menu);
                case RETURN_POKEBALL -> returnPokeball(player, menu);
                case CLAIM_CONVERSION_OUTPUT -> claimConversionOutput(player, menu);
                case REQUEST_CONVERSION -> menu.requestConversionConfirmation(player.tickCount);
                case CONFIRM_CONVERSION -> confirmConversion(player, menu);
                case CANCEL_CONVERSION -> { menu.cancelConversionConfirmation(); yield true; }
                default -> performExisting(player, menu, slug, action);
            };
        }
    }

    private static boolean performExisting(ServerPlayer player, PokecMenu menu, String slug, Action action) {
            PokecSpecies.Entry species = PokecSpecies.get(slug);
            return switch (action) {
                case DEPOSIT -> legacyDeposit(player, species);
                case PURCHASE -> purchase(player, menu, species);
                case STAGE_COINS -> stageCoins(player, menu, species);
                case STAGE_SOULS -> stageSouls(player, menu, species);
                case CONFIRM_SOULS -> confirmSouls(player, menu, species);
                case SELECT -> true;
                default -> false;
            };
    }

    private static boolean stagePokeball(ServerPlayer player, PokecMenu menu) {
        if (!menu.conversionBall().isEmpty() || !menu.conversionOutput().isEmpty()) return false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            PokeballItem.StoredPokemon stored = PokeballItem.readStored(stack);
            if (stored == null || stack.getCount() != 1 || !stored.owner().equals(player.getUUID())
                    || PokemonBallLedger.get((ServerLevel) player.level).isConsumed(stored.token())) continue;
            player.getInventory().removeItem(i, 1);
            menu.setConversionState(player, stack.copy(), ItemStack.EMPTY, -1);
            return true;
        }
        return false;
    }

    private static boolean returnPokeball(ServerPlayer player, PokecMenu menu) {
        ItemStack ball = menu.conversionBall();
        if (ball.isEmpty()) return false;
        player.getInventory().placeItemBackInInventory(ball);
        menu.setConversionState(player, ItemStack.EMPTY, menu.conversionOutput(), menu.conversionResult());
        return true;
    }

    private static boolean claimConversionOutput(ServerPlayer player, PokecMenu menu) {
        ItemStack output = menu.conversionOutput();
        if (output.isEmpty()) return false;
        player.getInventory().placeItemBackInInventory(output);
        menu.setConversionState(player, menu.conversionBall(), ItemStack.EMPTY, menu.conversionResult());
        return true;
    }

    private static boolean confirmConversion(ServerPlayer player, PokecMenu menu) {
        if (!menu.isConversionConfirming(player.tickCount) || !(player.level instanceof ServerLevel level)
                || !menu.conversionOutput().isEmpty()) return false;
        ItemStack ball = menu.conversionBall();
        if (ball.getCount() != 1) return false;
        PokeballItem.StoredPokemon stored = PokeballItem.readStored(ball);
        if (stored == null || !stored.owner().equals(player.getUUID())) return false;
        PokemonSoulConversion.Preview preview = menu.conversionPreview();
        if (preview == null || !preview.species().equals(stored.species()) || preview.soulType() == null) return false;
        var soulItem = ModItems.SOULS.get(preview.soulType().id());
        if (soulItem == null) return false;
        if (!PokemonBallLedger.get(level).consume(stored.token())) return false;
        int result = PokemonSoulConversion.roll(preview, level.getRandom()::nextDouble);
        int stackLimit = soulItem.get().getMaxStackSize();
        int inOutput = Math.min(result, stackLimit);
        ItemStack output = inOutput == 0 ? ItemStack.EMPTY : new ItemStack(soulItem.get(), inOutput);
        for (int remaining = result - inOutput; remaining > 0; remaining -= Math.min(remaining, stackLimit))
            player.getInventory().placeItemBackInInventory(new ItemStack(soulItem.get(), Math.min(remaining, stackLimit)));
        menu.setConversionState(player, PokeballItem.emptyBall(), output, result);
        return true;
    }

    private static boolean stageCoins(ServerPlayer player, PokecMenu menu, PokecSpecies.Entry species) {
        if (!PokecPlayerData.isUnlocked(player, species.slug())) return false;
        if (PokecEconomy.get().firstPurchaseFree() && !PokecPlayerData.usedFreePurchase(player)) return false;
        int needed = Math.max(0, PokecEconomy.get().coins(species.slug()) - PokecPlayerData.coins(player) - menu.stagedCoins());
        int taken = takeItems(player, ModItems.POKE_COIN.get(), needed);
        if (taken == 0) return false;
        menu.addStagedCoins(taken);
        menu.persistEscrow(player);
        return true;
    }

    private static boolean stageSouls(ServerPlayer player, PokecMenu menu, PokecSpecies.Entry species) {
        if (PokecPlayerData.isUnlocked(player, species.slug()) || species.isEvolution()) return false;
        SoulType required = SoulType.fromSpeciesType(species.primaryType());
        if (required == null) return false;
        int needed = Math.max(0, PokecEconomy.get().souls(species.slug()) - PokecPlayerData.deposited(player, species.slug()) - menu.stagedSouls());
        int taken = takeSouls(player, required, needed);
        if (taken == 0) return false;
        menu.addStagedSouls(taken);
        menu.persistEscrow(player);
        return true;
    }

    private static boolean confirmSouls(ServerPlayer player, PokecMenu menu, PokecSpecies.Entry species) {
        if (PokecPlayerData.isUnlocked(player, species.slug()) || species.isEvolution()) return false;
        int required = PokecEconomy.get().souls(species.slug());
        int remaining = Math.max(0, required - PokecPlayerData.deposited(player, species.slug()));
        if (remaining == 0 || menu.stagedSouls() < remaining) return false;
        menu.consumeStagedSouls(remaining);
        menu.persistEscrow(player);
        for (int i = 0; i < remaining; i++) PokecPlayerData.addDeposit(player, species.slug());
        if (PokecPlayerData.deposited(player, species.slug()) >= required) PokecPlayerData.unlock(player, species.slug());
        menu.returnEscrow(player); // returns any excess staged souls
        return true;
    }

    /** Compatibility path for existing server callers; the new screen uses staged soul deposits. */
    private static boolean legacyDeposit(ServerPlayer player, PokecSpecies.Entry species) {
        PokecEconomy economy = PokecEconomy.get();
        if (PokecPlayerData.isUnlocked(player, species.slug()) || species.isEvolution()) return false;
        SoulType required = SoulType.fromSpeciesType(species.primaryType());
        if (required == null) return false;
        ItemStack hand = matchingSoul(player.getMainHandItem(), required) ? player.getMainHandItem()
                : matchingSoul(player.getOffhandItem(), required) ? player.getOffhandItem() : ItemStack.EMPTY;
        if (hand.isEmpty() || PokecPlayerData.deposited(player, species.slug()) >= economy.souls(species.slug())) return false;
        if (!player.getAbilities().instabuild) hand.shrink(1);
        if (PokecPlayerData.addDeposit(player, species.slug()) >= economy.souls(species.slug())) PokecPlayerData.unlock(player, species.slug());
        return true;
    }

    private static boolean matchingSoul(ItemStack stack, SoulType type) {
        return !stack.isEmpty() && stack.getItem() instanceof SoulItem soul && soul.soulType() == type;
    }

    private static int takeItems(ServerPlayer player, Item item, int requested) {
        int remaining = Math.max(0, requested), taken = 0;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || stack.getItem() != item) continue;
            int count = Math.min(remaining, stack.getCount());
            stack.shrink(count);
            remaining -= count;
            taken += count;
        }
        if (taken > 0) player.getInventory().setChanged();
        return taken;
    }

    /** Recover staged physical items after a disconnect or crash prevented normal close cleanup. */
    public static void recoverEscrow(ServerPlayer player) {
        if (player.containerMenu instanceof PokecMenu menu) {
            menu.returnEscrow(player);
            return;
        }
        int coins = PokecPlayerData.stagedCoins(player);
        while (coins > 0) {
            int count = Math.min(coins, ModItems.POKE_COIN.get().getMaxStackSize());
            player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.POKE_COIN.get(), count));
            coins -= count;
        }
        int souls = PokecPlayerData.stagedSouls(player);
        SoulType soulType;
        try { soulType = SoulType.valueOf(PokecPlayerData.stagedSoulType(player).toUpperCase(Locale.ROOT)); }
        catch (Exception ignored) { soulType = null; }
        var soulItem = soulType == null ? null : ModItems.SOULS.get(soulType.id());
        while (souls > 0 && soulItem != null) {
            int count = Math.min(souls, soulItem.get().getMaxStackSize());
            player.getInventory().placeItemBackInInventory(new ItemStack(soulItem.get(), count));
            souls -= count;
        }
        ItemStack ball = PokecPlayerData.conversionBall(player);
        if (!ball.isEmpty()) player.getInventory().placeItemBackInInventory(ball);
        ItemStack output = PokecPlayerData.conversionOutput(player);
        if (!output.isEmpty()) player.getInventory().placeItemBackInInventory(output);
        PokecPlayerData.clearStaged(player);
    }

    private static int takeSouls(ServerPlayer player, SoulType type, int requested) {
        int remaining = Math.max(0, requested), taken = 0;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!matchingSoul(stack, type)) continue;
            int count = Math.min(remaining, stack.getCount());
            stack.shrink(count);
            remaining -= count;
            taken += count;
        }
        if (taken > 0) player.getInventory().setChanged();
        return taken;
    }

    private static boolean purchase(ServerPlayer player, PokecMenu menu, PokecSpecies.Entry species) {
        if (!PokecPlayerData.isUnlocked(player, species.slug())) return false;
        if (!menu.beginPurchase(player.tickCount)) return false;
        if (!(player.level instanceof ServerLevel level)) return false;
        var type = ModEntities.SPECIES.get(species.slug());
        if (type == null) return false;
        MinemonCreature creature = type.get().create(level);
        if (creature == null) return false;
        BlockPos safe = findSafePosition(level, player, creature);
        if (safe == null) { creature.discard(); return false; }
        creature.moveTo(safe.getX() + .5, safe.getY(), safe.getZ() + .5, player.getYRot(), 0);
        creature.tame(player);
        creature.setOrderedToSit(true);
        boolean free = PokecEconomy.get().firstPurchaseFree() && !PokecPlayerData.usedFreePurchase(player);
        int price = free ? 0 : PokecEconomy.get().coins(species.slug());
        int wallet = PokecPlayerData.coins(player);
        if (wallet + menu.stagedCoins() < price) { creature.discard(); return false; }
        if (!level.addFreshEntity(creature)) { creature.discard(); return false; }
        if (free) {
            PokecPlayerData.setFreePurchaseUsed(player);
        } else {
            int walletUse = Math.min(wallet, price);
            PokecPlayerData.setCoins(player, wallet - walletUse);
            menu.consumeStagedCoins(price - walletUse);
            menu.persistEscrow(player);
        }
        menu.returnEscrow(player); // returns unused physical coins, including coins on the free acquisition
        return true;
    }

    private static BlockPos findSafePosition(ServerLevel level, ServerPlayer player, Entity entity) {
        BlockPos origin = player.blockPosition();
        for (int radius = 1; radius <= 3; radius++) for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
            for (int dy = 0; dy <= 2; dy++) {
                BlockPos p = origin.offset(dx, dy, dz);
                if (!level.getWorldBorder().isWithinBounds(p) || p.distToCenterSqr(player.position()) < 1.5 || !level.getBlockState(p).getCollisionShape(level, p).isEmpty()) continue;
                AABB box = entity.getBoundingBox().move(p.getX() + .5 - entity.getX(), p.getY() - entity.getY(), p.getZ() + .5 - entity.getZ());
                if (level.noCollision(entity, box)) return p;
            }
        }
        return null;
    }

    /** Called only by server evolution code after confirming the owner's UUID. */
    public static void onOwnedEvolution(MinemonCreature source, String evolvedSlug) {
        if (source == null || !source.isTame() || source.getOwnerUUID() == null || !(source.level instanceof ServerLevel level)) return;
        PokecSpecies.Entry target = PokecSpecies.get(evolvedSlug);
        if (target == null || !source.getSpeciesSlug().equals(target.evolvesFrom())) return;
        ServerPlayer player = source.getOwner() instanceof ServerPlayer loadedOwner ? loadedOwner
                : level.players().stream().filter(candidate -> candidate.getUUID().equals(source.getOwnerUUID())).findFirst()
                .orElseGet(() -> level.getServer().getPlayerList().getPlayer(source.getOwnerUUID()));
        if (player != null) { PokecPlayerData.unlock(player, evolvedSlug); br.com.petrickfaria.minemon.network.MinemonNetwork.refreshPokec(player); }
    }

    public static void clearTransactions(UUID player) { RECENT_IDS.remove(player); }
    private static boolean remember(UUID player, long id) { return RECENT_IDS.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).add(id); }
    private PokecTransactions() {}
}
