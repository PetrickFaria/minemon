package br.com.petrickfaria.minemon.menu;

import br.com.petrickfaria.minemon.item.*;
import br.com.petrickfaria.minemon.pokec.*;
import br.com.petrickfaria.minemon.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

/** Physical terminal inputs and release rewards; purchase output is an unpaid recipe preview. */
public final class PokecTerminalSlots {
    private final PokecMenu menu;
    private final Inventory inventory;
    private final SimpleContainer items = new SimpleContainer(4);
    private boolean changing;
    private final net.minecraft.world.inventory.SimpleContainerData state=new net.minecraft.world.inventory.SimpleContainerData(2);
    public PokecTerminalSlots(PokecMenu menu, Inventory inventory) {
        this.menu=menu;this.inventory=inventory;
        if(inventory.player instanceof ServerPlayer player){
            CompoundTag saved=player.getPersistentData().getCompound("MinemonPokecSlots");
            for(int i:new int[]{0,2,3})if(saved.contains("slot"+i))items.setItem(i,ItemStack.of(saved.getCompound("slot"+i)));
        }
        for(int i=0;i<4;i++)menu.addTerminalSlot(createSlot(i,0,0));
        items.addListener(c->save());
        menu.addTerminalData(state);
    }
    private Slot createSlot(int index,int x,int y) {
        return switch(index) {
            case 0 -> new Slot(items,0,x,y){
                @Override public boolean mayPlace(ItemStack stack){return acceptsPayment(stack);}
                @Override public int getMaxStackSize(ItemStack stack){return Math.min(stack.getMaxStackSize(),remaining());}
            };
            case 1 -> new Slot(items,1,x,y){
                @Override public boolean mayPlace(ItemStack stack){return false;}
                @Override public boolean mayPickup(Player player){return player.level.isClientSide||canBuy();}
                @Override public void onTake(Player player,ItemStack stack){if(player instanceof ServerPlayer server)pay(server);super.onTake(player,stack);}
            };
            case 2 -> new Slot(items,2,x,y){
                @Override public boolean mayPlace(ItemStack stack){var stored=PokeballItem.readStored(stack);return stack.getCount()==1&&stored!=null&&stored.owner().equals(inventory.player.getUUID())&&items.getItem(3).isEmpty();}
                @Override public int getMaxStackSize(){return 1;}
            };
            default -> new Slot(items,3,x,y){@Override public boolean mayPlace(ItemStack stack){return false;}};
        };
    }
    public boolean spawnEnabled(){return state.get(0)==1;}
    public double spawnChance(){return state.get(1)/10000.0D;}
    public ItemStack payment(){return items.getItem(0);}
    public ItemStack ball(){return items.getItem(2);}
    public void layout(int inputX,int inputY,int outputX,int outputY,int releaseX,int releaseY,int rewardX,int rewardY){
        int[][] p={{inputX,inputY},{outputX,outputY},{releaseX,releaseY},{rewardX,rewardY}};
        for(int i=0;i<4;i++){Slot slot=createSlot(i,p[i][0],p[i][1]);slot.index=36+i;menu.slots.set(36+i,slot);}
    }
    private ServerPlayer server(){return inventory.player instanceof ServerPlayer p?p:null;}
    private boolean unlocked(){return server()!=null&&PokecPlayerData.isUnlocked(server(),menu.selectedSlug());}
    private boolean free(){return server()!=null&&PokecEconomy.get().firstPurchaseFree()&&!PokecPlayerData.usedFreePurchase(server());}
    public int remaining(){
        if(server()==null)return 64;
        if(unlocked())return Math.max(0,(free()?0:PokecEconomy.get().coins(menu.selectedSlug()))-PokecPlayerData.coins(server()));
        return Math.max(0,PokecEconomy.get().souls(menu.selectedSlug())-PokecPlayerData.deposited(server(),menu.selectedSlug()));
    }
    private boolean acceptsPayment(ItemStack stack){
        if(server()==null)return stack.is(ModItems.POKE_COIN.get())||stack.getItem() instanceof SoulItem;
        var species=PokecSpecies.get(menu.selectedSlug());
        if(species==null||remaining()==0)return false;
        return unlocked()?stack.is(ModItems.POKE_COIN.get()):!species.isEvolution()&&stack.getItem() instanceof SoulItem soul
            &&soul.soulType()==SoulType.fromSpeciesType(species.primaryType());
    }
    private boolean canBuy(){return unlocked()&&ModItems.EGGS.containsKey(menu.selectedSlug())
        &&(remaining()==0||payment().is(ModItems.POKE_COIN.get())&&payment().getCount()>=remaining());}
    private void pay(ServerPlayer player){
        int cost=free()?0:PokecEconomy.get().coins(menu.selectedSlug());
        int wallet=Math.min(cost,PokecPlayerData.coins(player));
        PokecPlayerData.setCoins(player,PokecPlayerData.coins(player)-wallet);
        payment().shrink(cost-wallet);
        if(free())PokecPlayerData.setFreePurchaseUsed(player);
        save();
    }
    public void refresh(){
        if(server()==null||changing)return;
        state.set(0,PokecPlayerData.spawnEnabled(server(),menu.selectedSlug())?1:0);
        state.set(1,(int)Math.round(WildEncounterManager.configuredSpawnChance(server(),menu.selectedSlug())*10000));
        changing=true;
        try{
            ItemStack result=canBuy()?PokecOwnedSpawnEggItem.ownedStack(ModItems.EGGS.get(menu.selectedSlug()).get().getDefaultInstance(),server().getUUID(),server().getGameProfile().getName()):ItemStack.EMPTY;
            if(!ItemStack.matches(result,items.getItem(1)))items.setItem(1,result);
        }finally{changing=false;}
    }
    public boolean unlock(){
        var player=server();var species=PokecSpecies.get(menu.selectedSlug());
        if(player==null||species==null||unlocked()||species.isEvolution()||!acceptsPayment(payment()))return false;
        int used=Math.min(payment().getCount(),remaining());if(used<=0)return false;
        payment().shrink(used);
        for(int i=0;i<used;i++)PokecPlayerData.addDeposit(player,menu.selectedSlug());
        if(remaining()==0)PokecPlayerData.unlock(player,menu.selectedSlug());
        save();refresh();return true;
    }
    public boolean release(){
        ServerPlayer player=server();if(player==null||!items.getItem(3).isEmpty())return false;
        var stored=PokeballItem.readStored(ball());
        if(stored==null||ball().getCount()!=1||!stored.owner().equals(player.getUUID()))return false;
        var preview=PokemonSoulConversion.preview(stored.species(),stored.level());
        if(preview==null||preview.soulType()==null)return false;
        var soul=ModItems.SOULS.get(preview.soulType().id());if(soul==null)return false;
        var ledger=PokemonBallLedger.get((ServerLevel)player.level);
        if(ledger.isConsumed(stored.token()))return false;
        int count=PokemonSoulConversion.roll(preview,player.getRandom()::nextDouble);
        if(count>soul.get().getMaxStackSize()||!ledger.consume(stored.token()))return false;
        items.setItem(2,PokeballItem.emptyBall(ball()));
        items.setItem(3,new ItemStack(soul.get(),count));
        PokecPlayerData.addTrainerXp(player,TrainerProgression.pokemonXp(stored.species(),stored.level())*.5D);
        save();return true;
    }
    public void returnPayment(ServerPlayer player){
        ItemStack stack=items.removeItemNoUpdate(0);if(!stack.isEmpty())player.getInventory().placeItemBackInInventory(stack);
        items.setItem(1,ItemStack.EMPTY);save();
    }
    public void close(ServerPlayer player){
        for(int i:new int[]{0,2,3}){ItemStack stack=items.removeItemNoUpdate(i);if(!stack.isEmpty())player.getInventory().placeItemBackInInventory(stack);}
        items.setItem(1,ItemStack.EMPTY);player.getPersistentData().remove("MinemonPokecSlots");
    }
    private void save(){
        if(server()==null||changing)return;
        CompoundTag saved=new CompoundTag();
        for(int i:new int[]{0,2,3})if(!items.getItem(i).isEmpty())saved.put("slot"+i,items.getItem(i).save(new CompoundTag()));
        server().getPersistentData().put("MinemonPokecSlots",saved);
    }
    public static void recover(ServerPlayer player){
        CompoundTag saved=player.getPersistentData().getCompound("MinemonPokecSlots");
        player.getPersistentData().remove("MinemonPokecSlots");
        for(int i:new int[]{0,2,3})if(saved.contains("slot"+i))player.getInventory().placeItemBackInInventory(ItemStack.of(saved.getCompound("slot"+i)));
    }
}
