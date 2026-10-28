package br.com.petrickfaria.minemon.menu;

import br.com.petrickfaria.minemon.pokec.PokecSpecies;
import br.com.petrickfaria.minemon.pokec.PokecPlayerData;
import br.com.petrickfaria.minemon.pokec.SoulType;
import br.com.petrickfaria.minemon.registry.ModBlocks;
import br.com.petrickfaria.minemon.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

/** Menu anchor and short-lived, server-owned PokéC payment slots. */
public final class PokecMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private String selectedSlug = "bulbasaur";
    private int stagedCoins;
    private int stagedSouls;
    private int lastPurchaseTick = -20;

    public PokecMenu(int id, Inventory inventory, FriendlyByteBuf buf) { this(id, inventory, buf.readBlockPos()); }
    public PokecMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenus.POKEC.get(), id);
        this.pos = pos;
        if (!inventory.player.level.isClientSide && inventory.player instanceof ServerPlayer serverPlayer) {
            stagedCoins = PokecPlayerData.stagedCoins(serverPlayer);
            stagedSouls = PokecPlayerData.stagedSouls(serverPlayer);
        }
    }
    public BlockPos pos() { return pos; }
    public String selectedSlug() { return selectedSlug; }
    public int stagedCoins() { return stagedCoins; }
    public int stagedSouls() { return stagedSouls; }
    public boolean beginPurchase(int gameTick) {
        if (gameTick - lastPurchaseTick < 4) return false;
        lastPurchaseTick = gameTick;
        return true;
    }
    public void addStagedCoins(int count) { stagedCoins = Math.max(0, stagedCoins + count); }
    public void addStagedSouls(int count) { stagedSouls = Math.max(0, stagedSouls + count); }
    public int consumeStagedCoins(int count) { int used = Math.min(stagedCoins, Math.max(0, count)); stagedCoins -= used; return used; }
    public int consumeStagedSouls(int count) { int used = Math.min(stagedSouls, Math.max(0, count)); stagedSouls -= used; return used; }

    /** A species change returns all uncommitted physical items before changing the slot filters. */
    public void selectSpecies(ServerPlayer player, String slug) {
        if (!selectedSlug.equals(slug)) {
            returnEscrow(player);
            selectedSlug = slug;
        }
    }

    /** Returning via Inventory handles stacking and drops any remainder at the player safely. */
    public void returnEscrow(ServerPlayer player) {
        int coins = stagedCoins;
        stagedCoins = 0;
        while (coins > 0) {
            int stackSize = Math.min(coins, ModItems.POKE_COIN.get().getMaxStackSize());
            player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.POKE_COIN.get(), stackSize));
            coins -= stackSize;
        }
        int souls = stagedSouls;
        stagedSouls = 0;
        SoulType type;
        try { type = SoulType.valueOf(PokecPlayerData.stagedSoulType(player).toUpperCase(java.util.Locale.ROOT)); }
        catch (Exception ignored) {
            var species = PokecSpecies.get(selectedSlug);
            type = species == null ? null : SoulType.fromSpeciesType(species.primaryType());
        }
        RegistryObject<net.minecraft.world.item.Item> soul = type == null ? null : ModItems.SOULS.get(type.id());
        while (souls > 0 && soul != null) {
            int stackSize = Math.min(souls, soul.get().getMaxStackSize());
            player.getInventory().placeItemBackInInventory(new ItemStack(soul.get(), stackSize));
            souls -= stackSize;
        }
        PokecPlayerData.clearStaged(player);
    }

    public void persistEscrow(ServerPlayer player) {
        PokecPlayerData.setStagedCoins(player, stagedCoins);
        var species = PokecSpecies.get(selectedSlug);
        String type = species == null ? "" : species.primaryType();
        PokecPlayerData.setStagedSouls(player, stagedSouls, type);
    }

    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) { return stillValid(ContainerLevelAccess.create(player.level, pos), player, ModBlocks.POKEC.get()); }
    @Override public void removed(Player player) {
        if (!player.level.isClientSide && player instanceof ServerPlayer serverPlayer) returnEscrow(serverPlayer);
        super.removed(player);
    }
}
