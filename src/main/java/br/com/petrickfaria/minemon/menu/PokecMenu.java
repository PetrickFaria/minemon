package br.com.petrickfaria.minemon.menu;

import br.com.petrickfaria.minemon.pokec.PokecSpecies;
import br.com.petrickfaria.minemon.pokec.PokecPlayerData;
import br.com.petrickfaria.minemon.pokec.SoulType;
import br.com.petrickfaria.minemon.pokec.PokemonSoulConversion;
import br.com.petrickfaria.minemon.item.PokeballItem;
import br.com.petrickfaria.minemon.registry.ModBlocks;
import br.com.petrickfaria.minemon.registry.ModItems;
import br.com.petrickfaria.minemon.network.MinemonNetwork;
import br.com.petrickfaria.minemon.pokec.PokecTransactions;
import br.com.petrickfaria.minemon.pokec.SoulItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

/** Menu anchor and short-lived, server-owned PokéC payment slots. */
public final class PokecMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final Inventory playerInventory;
    private final SimpleContainer paymentInventory = new SimpleContainer(2);
    private String selectedSlug = "bulbasaur";
    private int stagedCoins;
    private int stagedSouls;
    private int lastPurchaseTick = -20;
    private ItemStack conversionBall = ItemStack.EMPTY;
    private ItemStack conversionOutput = ItemStack.EMPTY;
    private int conversionResult = -1;
    private int conversionConfirmUntil = -1;
    private PokemonSoulConversion.Preview conversionPreview;

    public PokecMenu(int id, Inventory inventory, FriendlyByteBuf buf) { this(id, inventory, buf.readBlockPos()); }
    public PokecMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenus.POKEC.get(), id);
        this.pos = pos;
        this.playerInventory = inventory;
        addSlot(new net.minecraft.world.inventory.Slot(paymentInventory, 0, 0, 0) {
            @Override public boolean mayPlace(ItemStack stack) { return paymentAccepts(stack); }
        });
        addSlot(new net.minecraft.world.inventory.Slot(paymentInventory, 1, 0, 0) {
            @Override public boolean mayPlace(ItemStack stack) { return paymentAccepts(stack); }
        });
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new net.minecraft.world.inventory.Slot(inventory, column + row * 9 + 9, 0, 0));
        for (int column = 0; column < 9; column++)
            addSlot(new net.minecraft.world.inventory.Slot(inventory, column, 0, 0));
        if (!inventory.player.level.isClientSide && inventory.player instanceof ServerPlayer serverPlayer) {
            stagedCoins = PokecPlayerData.stagedCoins(serverPlayer);
            stagedSouls = PokecPlayerData.stagedSouls(serverPlayer);
            conversionBall = PokecPlayerData.conversionBall(serverPlayer);
            conversionOutput = PokecPlayerData.conversionOutput(serverPlayer);
            conversionResult = PokecPlayerData.conversionResult(serverPlayer);
            refreshConversionPreview(serverPlayer);
            restoreLegacyPayment(serverPlayer);
        }
    }
    public BlockPos pos() { return pos; }
    /** Screen-only coordinates; slot indices and backing Inventory remain vanilla. */
    public void layoutPlayerInventory(int x, int mainY, int hotbarY, float scale) {
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) {
            int menuIndex = 2 + row * 9 + column;
            var slot = new net.minecraft.world.inventory.Slot(playerInventory,
                    column + row * 9 + 9, x + column * 18, mainY + row * 18);
            slot.index = menuIndex;
            slots.set(menuIndex, slot);
        }
        for (int column = 0; column < 9; column++) {
            int menuIndex = 29 + column;
            var slot = new net.minecraft.world.inventory.Slot(playerInventory,
                    column, x + column * 18, hotbarY);
            slot.index = menuIndex;
            slots.set(menuIndex, slot);
        }
        slots.get(0).x = Math.round((560 + 8) * scale);
        slots.get(0).y = Math.round((28 + 43) * scale);
        slots.get(1).x = Math.round((560 + 30) * scale);
        slots.get(1).y = Math.round((28 + 45) * scale);
    }
    public String selectedSlug() { return selectedSlug; }
    public int stagedCoins() { return paymentCount(ModItems.POKE_COIN.get()); }
    public int stagedSouls() { return (paymentInventory.getItem(0).getItem() instanceof SoulItem ? paymentInventory.getItem(0).getCount() : 0)
            + (paymentInventory.getItem(1).getItem() instanceof SoulItem ? paymentInventory.getItem(1).getCount() : 0); }
    private int paymentCount(net.minecraft.world.item.Item item) {
        int count=0;for(int i=0;i<paymentInventory.getContainerSize();i++)if(paymentInventory.getItem(i).is(item))count+=paymentInventory.getItem(i).getCount();return count;
    }
    private boolean paymentAccepts(ItemStack stack) {
        if(stack.isEmpty())return true;
        if(playerInventory.player.level.isClientSide)return stack.is(ModItems.POKE_COIN.get())||stack.getItem() instanceof SoulItem;
        var species=br.com.petrickfaria.minemon.pokec.PokecSpecies.get(selectedSlug);
        if(species==null)return false;
        if(stack.is(ModItems.POKE_COIN.get()))return PokecPlayerData.isUnlocked((ServerPlayer)playerInventory.player,selectedSlug)
                &&!(PokecEconomy.get().firstPurchaseFree()&&!PokecPlayerData.usedFreePurchase((ServerPlayer)playerInventory.player));
        if(!(stack.getItem() instanceof SoulItem soul)||species.isEvolution()||PokecPlayerData.isUnlocked((ServerPlayer)playerInventory.player,selectedSlug))return false;
        return soul.soulType()==SoulType.fromSpeciesType(species.primaryType());
    }
    private void restoreLegacyPayment(ServerPlayer player) {
        int coins=stagedCoins,souls=stagedSouls;stagedCoins=stagedSouls=0;
        if(coins>0)addPayment(ModItems.POKE_COIN.get(),coins);
        if(souls>0){
            SoulType type;try{type=SoulType.valueOf(PokecPlayerData.stagedSoulType(player).toUpperCase(java.util.Locale.ROOT));}
            catch(Exception ignored){var species=PokecSpecies.get(selectedSlug);type=species==null?null:SoulType.fromSpeciesType(species.primaryType());}
            var item=type==null?null:ModItems.SOULS.get(type.id());
            if(item!=null)addPayment(item.get(),souls);
        }
        persistEscrow(player);
    }
    public ItemStack conversionBall() { return conversionBall.copy(); }
    public ItemStack conversionOutput() { return conversionOutput.copy(); }
    public int conversionResult() { return conversionResult; }
    public PokemonSoulConversion.Preview conversionPreview() { return conversionPreview; }
    public boolean isConversionConfirming(int gameTick) { return conversionConfirmUntil >= gameTick; }
    public boolean requestConversionConfirmation(int gameTick) {
        if (conversionPreview == null || conversionBall.isEmpty() || !conversionOutput.isEmpty()) return false;
        conversionConfirmUntil = gameTick + 80;
        return true;
    }
    public void cancelConversionConfirmation() { conversionConfirmUntil = -1; }
    public void setConversionState(ServerPlayer player, ItemStack ball, ItemStack output, int result) {
        conversionBall = ball.copy();
        conversionOutput = output.copy();
        conversionResult = Math.max(-1, result);
        conversionConfirmUntil = -1;
        if (!conversionBall.isEmpty() || conversionResult < 0) refreshConversionPreview(player);
        PokecPlayerData.setConversion(player, conversionBall, conversionOutput, conversionResult);
    }
    public void refreshConversionPreview(ServerPlayer player) {
        PokeballItem.StoredPokemon stored = PokeballItem.readStored(conversionBall);
        if (stored == null || !stored.owner().equals(player.getUUID())) {
            conversionPreview = null;
            return;
        }
        conversionPreview = PokemonSoulConversion.preview(stored.species(), stored.level());
    }
    public boolean beginPurchase(int gameTick) {
        if (gameTick - lastPurchaseTick < 4) return false;
        lastPurchaseTick = gameTick;
        return true;
    }
    public void addStagedCoins(int count) { addPayment(ModItems.POKE_COIN.get(),count); }
    public void addStagedSouls(int count) { var species=PokecSpecies.get(selectedSlug);var type=species==null?null:SoulType.fromSpeciesType(species.primaryType());var item=type==null?null:ModItems.SOULS.get(type.id());if(item!=null)addPayment(item.get(),count); }
    private void addPayment(net.minecraft.world.item.Item item,int count){int remaining=Math.max(0,count);for(int i=0;i<2&&remaining>0;i++){ItemStack stack=paymentInventory.getItem(i);if(stack.isEmpty()){int add=Math.min(remaining,item.getMaxStackSize());paymentInventory.setItem(i,new ItemStack(item,add));remaining-=add;}else if(stack.is(item)&&stack.getCount()<stack.getMaxStackSize()){int add=Math.min(remaining,stack.getMaxStackSize()-stack.getCount());stack.grow(add);remaining-=add;}}}
    public int consumeStagedCoins(int count) { return consumePayment(ModItems.POKE_COIN.get(),count); }
    public int consumeStagedSouls(int count) { int remaining=Math.max(0,count),used=0;for(int i=0;i<2&&remaining>0;i++){ItemStack stack=paymentInventory.getItem(i);if(stack.getItem() instanceof SoulItem){int take=Math.min(remaining,stack.getCount());stack.shrink(take);remaining-=take;used+=take;}}return used; }
    private int consumePayment(net.minecraft.world.item.Item item,int count){int remaining=Math.max(0,count),used=0;for(int i=0;i<2&&remaining>0;i++){ItemStack stack=paymentInventory.getItem(i);if(stack.is(item)){int take=Math.min(remaining,stack.getCount());stack.shrink(take);remaining-=take;used+=take;}}return used;}

    /** A species change returns all uncommitted physical items before changing the slot filters. */
    public void selectSpecies(ServerPlayer player, String slug) {
        if (!selectedSlug.equals(slug)) {
            returnEscrow(player);
            selectedSlug = slug;
        }
    }

    /** Returning via Inventory handles stacking and drops any remainder at the player safely. */
    public void returnEscrow(ServerPlayer player) {
        for(int i=0;i<paymentInventory.getContainerSize();i++){ItemStack payment=paymentInventory.getItem(i);if(!payment.isEmpty())player.getInventory().placeItemBackInInventory(payment.copy());paymentInventory.setItem(i,ItemStack.EMPTY);}
        stagedCoins=stagedSouls=0;
        if (!conversionBall.isEmpty()) player.getInventory().placeItemBackInInventory(conversionBall.copy());
        if (!conversionOutput.isEmpty()) player.getInventory().placeItemBackInInventory(conversionOutput.copy());
        conversionBall = ItemStack.EMPTY;
        conversionOutput = ItemStack.EMPTY;
        conversionResult = -1;
        conversionPreview = null;
        conversionConfirmUntil = -1;
        PokecPlayerData.clearStaged(player);
    }

    public void persistEscrow(ServerPlayer player) {
        stagedCoins=stagedCoins();stagedSouls=stagedSouls();
        PokecPlayerData.setStagedCoins(player, stagedCoins);
        SoulType type=null;for(int i=0;i<2;i++)if(paymentInventory.getItem(i).getItem() instanceof SoulItem soul){type=soul.soulType();break;}
        PokecPlayerData.setStagedSouls(player, stagedSouls, type==null?"":type.name());
    }

    @Override public void slotsChanged(net.minecraft.world.Container container) {
        super.slotsChanged(container);
        if(container==paymentInventory&&playerInventory.player instanceof ServerPlayer serverPlayer)persistEscrow(serverPlayer);
    }

    @Override public void clicked(int slotId,int button,net.minecraft.world.inventory.ClickType clickType,Player player){
        super.clicked(slotId,button,clickType,player);
        if(slotId>=0&&slotId<2&&player instanceof ServerPlayer serverPlayer)persistEscrow(serverPlayer);
    }

    @Override public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= 38) return ItemStack.EMPTY;
        var menuSlot = slots.get(slotIndex);
        ItemStack stack = menuSlot.getItem();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack original = stack.copy();
        if(slotIndex<2){
            if(!moveItemStackTo(stack,2,38,false))return ItemStack.EMPTY;
            if(stack.isEmpty())menuSlot.set(ItemStack.EMPTY);else menuSlot.setChanged();
            if(player instanceof ServerPlayer serverPlayer)persistEscrow(serverPlayer);
            return original;
        }
        if(PokeballItem.readStored(stack)!=null){
            if(player.level.isClientSide)MinemonNetwork.performPokecAction(pos,selectedSlug,PokecTransactions.Action.STAGE_POKEBALL);
            return ItemStack.EMPTY;
        }
        int paymentStart=stack.is(ModItems.POKE_COIN.get())||stack.getItem() instanceof SoulItem?0:2;
        boolean moved = paymentStart==0
                ? moveItemStackTo(stack,0,2,false)
                : slotIndex<29?moveItemStackTo(stack,29,38,false):moveItemStackTo(stack,2,29,false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) menuSlot.set(ItemStack.EMPTY);
        else menuSlot.setChanged();
        if(player instanceof ServerPlayer serverPlayer)persistEscrow(serverPlayer);
        return original;
    }
    @Override public boolean stillValid(Player player) { return stillValid(ContainerLevelAccess.create(player.level, pos), player, ModBlocks.POKEC.get()); }
    @Override public void removed(Player player) {
        if (!player.level.isClientSide && player instanceof ServerPlayer serverPlayer) returnEscrow(serverPlayer);
        super.removed(player);
    }
}
