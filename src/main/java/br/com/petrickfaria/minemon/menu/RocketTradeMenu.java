package br.com.petrickfaria.minemon.menu;

import br.com.petrickfaria.minemon.pokec.SoulItem;
import br.com.petrickfaria.minemon.pokec.SoulType;
import br.com.petrickfaria.minemon.registry.ModItems;
import br.com.petrickfaria.minemon.registry.ModVillagers;
import java.util.Comparator;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.npc.Villager;
import br.com.petrickfaria.minemon.registry.RocketChallengeManager;

/** Server-authoritative five-slot Rocket soul exchange unlocked by its challenge. */
public final class RocketTradeMenu extends AbstractContainerMenu {
    private static final List<SoulType> ALPHABETICAL_TYPES = java.util.Arrays.stream(SoulType.values())
            .sorted(Comparator.comparing(RocketTradeMenu::soulDisplayName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    private final Inventory playerInventory;
    private final Container input = new SimpleContainer(5) {
        @Override public void setChanged() { super.setChanged(); RocketTradeMenu.this.slotsChanged(this); }
    };
    private final Container result = new SimpleContainer(1);
    private final Villager villager;
    private int selectedIndex; // 0 is PokéCoin; 1..N follow localized alphabetical soul names.
    private final DataSlot selectedSync = new DataSlot() {
        @Override public int get() { return selectedIndex; }
        @Override public void set(int value) { selectedIndex = Math.max(0, Math.min(ALPHABETICAL_TYPES.size(), value)); }
    };
    private final DataSlot levelSync = DataSlot.standalone();
    private final DataSlot tradesSync = DataSlot.standalone();

    public RocketTradeMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, inventory.player.level.getEntity(data.readInt()) instanceof Villager found ? found : null);
    }

    public RocketTradeMenu(int id, Inventory inventory, Villager villager) {
        super(ModMenus.ROCKET_TRADE.get(), id);
        this.playerInventory = inventory;
        this.villager = villager;
        for (int i = 0; i < 5; i++) {
            final int slot = i;
            addSlot(new Slot(input, slot, 111 + i * 18, 37) {
                @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof SoulItem; }
                @Override public int getMaxStackSize() { return 64; }
            });
        }
        addSlot(new Slot(result, 0, 243, 37) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return !getItem().isEmpty() && eligibleCount() >= 10; }
            @Override public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                if (!player.level.isClientSide && player instanceof ServerPlayer && isUnlocked()) {
                    consumeTen();
                        RocketChallengeManager.recordCompletedTrade(villager, (ServerPlayer) player);
                    updateResult();
                    broadcastChanges();
                }
            }
        });
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + row * 9 + 9, 108 + column * 18, 84 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 108 + column * 18, 142));
        addDataSlot(selectedSync);
        addDataSlot(levelSync);
        addDataSlot(tradesSync);
        if (!inventory.player.level.isClientSide) updateResult();
    }

    public int offerCount() { return ALPHABETICAL_TYPES.size() + 1; }
    public ItemStack offerResult(int index) {
        if (index == 0) return new ItemStack(ModItems.POKE_COIN.get());
        if (index < 1 || index >= offerCount()) return ItemStack.EMPTY;
        return new ItemStack(ModItems.SOULS.get(ALPHABETICAL_TYPES.get(index - 1).id()).get());
    }
    public int selectedIndex() { return selectedIndex; }
    public String selectedLabel() {
        if (selectedIndex == 0) return "1 PokéCoin / 10 almas";
        SoulType type = ALPHABETICAL_TYPES.get(Math.min(ALPHABETICAL_TYPES.size() - 1, selectedIndex - 1));
        return soulDisplayName(type);
    }
    private static String soulDisplayName(SoulType type) {
        var registeredItem = ModItems.SOULS.get(type.id());
        return registeredItem == null ? type.id() : registeredItem.get().getName(ItemStack.EMPTY).getString();
    }
    public Villager villager() { return villager; }
    public boolean isUnlocked() { return villager != null && RocketChallengeManager.isChallengeComplete(villager); }
    public int progressionLevel() { return levelSync.get(); }
    public int tradesThisLevel() { return tradesSync.get(); }
    public int tradesRequired() { return RocketChallengeManager.requiredTradesForNextLevel(progressionLevel()); }

    @Override public void broadcastChanges() {
        if (!playerInventory.player.level.isClientSide && villager != null) {
            levelSync.set(RocketChallengeManager.getLevel(villager));
            tradesSync.set(RocketChallengeManager.getTradesThisLevel(villager));
        }
        super.broadcastChanges();
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (!isUnlocked()) return false;
        if (button >= 100 && button < 100 + offerCount()) {
            selectedIndex = button - 100;
            updateResult(); broadcastChanges(); return true;
        }
        if (button != 0 && button != 1) return false;
        int max = ALPHABETICAL_TYPES.size();
        selectedIndex = Math.floorMod(selectedIndex + (button == 0 ? 1 : -1), max + 1);
        updateResult();
        broadcastChanges();
        return true;
    }

    @Override public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == input && !playerInventory.player.level.isClientSide) updateResult();
    }

    private int eligibleCount() {
        int count = 0;
        SoulType target = selectedSoul();
        for (int i = 0; i < 5; i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof SoulItem soul && (target == null || soul.soulType() != target)) count += stack.getCount();
        }
        return count;
    }

    private SoulType selectedSoul() { return selectedIndex == 0 ? null : ALPHABETICAL_TYPES.get(selectedIndex - 1); }

    private void updateResult() {
        result.setItem(0, ItemStack.EMPTY);
        if (!isUnlocked() || eligibleCount() < 10) return;
        if (selectedIndex == 0) result.setItem(0, new ItemStack(ModItems.POKE_COIN.get()));
        else {
            SoulType type = selectedSoul();
            var output = ModItems.SOULS.get(type.id());
            if (output != null) result.setItem(0, new ItemStack(output.get()));
        }
    }

    private void consumeTen() {
        SoulType target = selectedSoul();
        int remaining = 10;
        for (int i = 0; i < 5 && remaining > 0; i++) {
            ItemStack stack = input.getItem(i);
            if (!(stack.getItem() instanceof SoulItem soul) || (target != null && soul.soulType() == target)) continue;
            int used = Math.min(remaining, stack.getCount());
            stack.shrink(used);
            remaining -= used;
            if (stack.isEmpty()) input.setItem(i, ItemStack.EMPTY);
        }
        input.setChanged();
    }

    @Override public ItemStack quickMoveStack(Player player, int slotIndex) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) {
        return isUnlocked() && villager.isAlive() && villager.level == player.level && player.distanceToSqr(villager) <= 64.0D;
    }
    @Override public void removed(Player player) {
        if (!player.level.isClientSide) {
            for (int i = 0; i < 5; i++) {
                ItemStack stack = input.removeItemNoUpdate(i);
                if (!stack.isEmpty()) player.getInventory().placeItemBackInInventory(stack);
            }
        }
        super.removed(player);
    }
}
