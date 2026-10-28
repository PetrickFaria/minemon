package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.block.PokecBlock;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.menu.PokecMenu;
import br.com.petrickfaria.minemon.pokec.*;
import br.com.petrickfaria.minemon.registry.ModBlocks;
import br.com.petrickfaria.minemon.registry.ModEntities;
import br.com.petrickfaria.minemon.registry.ModItems;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@Mod.EventBusSubscriber(modid=Minemon.MOD_ID)
@GameTestHolder(Minemon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PokecGameTests {
    private static final java.util.Set<ServerPlayer> TEST_PLAYERS=new java.util.HashSet<>();
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void tickPlayers(net.minecraftforge.event.TickEvent.ServerTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;TEST_PLAYERS.removeIf(net.minecraft.world.entity.Entity::isRemoved);for(ServerPlayer p:TEST_PLAYERS)p.doTick();
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static void floor(GameTestHelper h){for(int x=0;x<16;x++)for(int z=0;z<16;z++){h.setBlock(new BlockPos(x,0,z),Blocks.STONE);for(int y=1;y<6;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);}}
    private static ServerPlayer player(GameTestHelper h,BlockPos pos){
        ServerPlayer p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"PokecTester"),null);
        p.connection=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"PokecConnection")).connection;
        p.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);p.tickCount=100;h.getLevel().addNewPlayer(p);TEST_PLAYERS.add(p);return p;
    }
    private static void terminal(GameTestHelper h,BlockPos p){
        var lower=ModBlocks.POKEC.get().defaultBlockState().setValue(PokecBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
        h.getLevel().setBlock(p,lower,3);h.getLevel().setBlock(p.above(),lower.setValue(PokecBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),3);
    }
    @GameTest(template="test_arena",timeoutTicks=200)
    public static void terminalHasPairedTwoBlockState(GameTestHelper h){
        floor(h);BlockPos p=h.absolutePos(new BlockPos(5,1,5));terminal(h,p);
        BlockPos relative=new BlockPos(5,1,5);
        h.assertBlockPresent(ModBlocks.POKEC.get(),relative);h.assertBlockPresent(ModBlocks.POKEC.get(),relative.above());
        h.assertBlockProperty(relative,PokecBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
        h.assertBlockProperty(relative.above(),PokecBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);h.succeed();
    }
    @GameTest(template="test_arena",timeoutTicks=200)
    public static void firstSuccessfulStarterPurchaseIsOwnedAndFree(GameTestHelper h){
        floor(h);BlockPos p=h.absolutePos(new BlockPos(5,1,5));terminal(h,p);ServerPlayer buyer=player(h,h.absolutePos(new BlockPos(7,1,5)));
        buyer.containerMenu=new PokecMenu(7,buyer.getInventory(),p);PokecPlayerData.unlock(buyer,"bulbasaur");PokecPlayerData.setCoins(buyer,0);
        PokecTransactions.perform(buyer,p,"bulbasaur",PokecTransactions.Action.SELECT,0x500L);
        boolean ok=PokecTransactions.perform(buyer,p,"bulbasaur",PokecTransactions.Action.PURCHASE,0x501L);
        h.runAfterDelay(2,()->{
            var delivered=h.getLevel().getEntitiesOfClass(MinemonCreature.class,new net.minecraft.world.phys.AABB(p).inflate(5),e->e.getSpeciesSlug().equals("bulbasaur")&&buyer.getUUID().equals(e.getOwnerUUID()));
            check(ok,"first purchase should succeed without coins");check(PokecPlayerData.usedFreePurchase(buyer),"free purchase flag must persist");check(!delivered.isEmpty(),"delivered Minemon must be owned by buyer");
            check(!PokecTransactions.perform(buyer,p,"bulbasaur",PokecTransactions.Action.PURCHASE,0x501L),"duplicate transaction id must not deliver twice");
            PokecPlayerData.setCoins(buyer,0);check(!PokecTransactions.perform(buyer,p,"bulbasaur",PokecTransactions.Action.PURCHASE,0x502L),"second purchase cannot reuse the free acquisition");
            PokecPlayerData.setCoins(buyer,PokecEconomy.get().coins("bulbasaur"));buyer.tickCount+=4;check(PokecTransactions.perform(buyer,p,"bulbasaur",PokecTransactions.Action.PURCHASE,0x503L),"later acquisition should charge the configured price");
            check(PokecPlayerData.coins(buyer)==0,"successful purchase must debit PokéCoins once");
            var afterSecond=h.getLevel().getEntitiesOfClass(MinemonCreature.class,new net.minecraft.world.phys.AABB(p).inflate(5),e->e.getSpeciesSlug().equals("bulbasaur")&&buyer.getUUID().equals(e.getOwnerUUID()));
            check(afterSecond.size()==2,"free and paid purchases should each deliver exactly one owned Minemon");
            buyer.discard();h.succeed();
        });
    }
    @GameTest(template="test_arena",timeoutTicks=200)
    public static void correctSoulTypeUnlocksBaseSpecies(GameTestHelper h){
        floor(h);BlockPos p=h.absolutePos(new BlockPos(5,1,5));terminal(h,p);ServerPlayer buyer=player(h,h.absolutePos(new BlockPos(7,1,5)));
        buyer.containerMenu=new PokecMenu(8,buyer.getInventory(),p);int required=PokecEconomy.get().souls("oddish");buyer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(ModItems.SOULS.get("grass").get(),required));
        PokecTransactions.perform(buyer,p,"oddish",PokecTransactions.Action.SELECT,0x5FFL);
        boolean all=true;for(int i=0;i<required;i++)all&=PokecTransactions.perform(buyer,p,"oddish",PokecTransactions.Action.DEPOSIT,0x600L+i);
        check(all,"matching Grass Soul should be accepted");check(PokecPlayerData.isUnlocked(buyer,"oddish"),"species unlock should persist");
        check(buyer.getMainHandItem().isEmpty(),"exactly the required souls should be consumed");buyer.discard();h.succeed();
    }

    @GameTest(template="test_arena",timeoutTicks=200)
    public static void stagedPhysicalCoinsChargeExactPriceAndDoNotDuplicate(GameTestHelper h){
        floor(h);BlockPos p=h.absolutePos(new BlockPos(5,1,5));terminal(h,p);ServerPlayer buyer=player(h,h.absolutePos(new BlockPos(7,1,5)));
        PokecMenu menu=new PokecMenu(9,buyer.getInventory(),p);buyer.containerMenu=menu;PokecPlayerData.unlock(buyer,"bulbasaur");PokecPlayerData.setCoins(buyer,0);PokecPlayerData.setFreePurchaseUsed(buyer);
        int price=PokecEconomy.get().coins("bulbasaur"), total=price+5;int slot=0;
        while(total>0){int count=Math.min(64,total);buyer.getInventory().setItem(slot++,new net.minecraft.world.item.ItemStack(ModItems.POKE_COIN.get(),count));total-=count;}
        PokecTransactions.perform(buyer,p,"bulbasaur",PokecTransactions.Action.SELECT,0x700L);
        check(PokecTransactions.perform(buyer,p,"bulbasaur",PokecTransactions.Action.STAGE_COINS,0x701L),"physical coins should stage from inventory");
        check(menu.stagedCoins()==price,"slot stages only the selected species price");
        check(buyer.getInventory().items.stream().filter(s->s.is(ModItems.POKE_COIN.get())).mapToInt(net.minecraft.world.item.ItemStack::getCount).sum()==5,"coins beyond the price remain with the player");
        check(PokecTransactions.perform(buyer,p,"bulbasaur",PokecTransactions.Action.PURCHASE,0x702L),"staged coins should complete purchase");
        check(menu.stagedCoins()==0,"spent coins must leave escrow exactly once");
        check(buyer.getInventory().items.stream().filter(s->s.is(ModItems.POKE_COIN.get())).mapToInt(net.minecraft.world.item.ItemStack::getCount).sum()==5,"purchase must not consume excess coins");
        var owned=h.getLevel().getEntitiesOfClass(MinemonCreature.class,new net.minecraft.world.phys.AABB(p).inflate(5),e->e.getSpeciesSlug().equals("bulbasaur")&&buyer.getUUID().equals(e.getOwnerUUID()));
        check(owned.size()==1,"one accepted payment delivers one owned Minemon");buyer.discard();h.succeed();
    }

    @GameTest(template="test_arena",timeoutTicks=200)
    public static void stagedSoulsRequirePrimaryTypeAndPersistOnlyOnConfirmation(GameTestHelper h){
        floor(h);BlockPos p=h.absolutePos(new BlockPos(5,1,5));terminal(h,p);ServerPlayer buyer=player(h,h.absolutePos(new BlockPos(7,1,5)));
        PokecMenu menu=new PokecMenu(10,buyer.getInventory(),p);buyer.containerMenu=menu;int required=PokecEconomy.get().souls("oddish");
        buyer.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(ModItems.SOULS.get("fire").get(),4));
        buyer.getInventory().setItem(1,new net.minecraft.world.item.ItemStack(ModItems.SOULS.get("grass").get(),required+3));
        PokecTransactions.perform(buyer,p,"oddish",PokecTransactions.Action.SELECT,0x800L);
        check(PokecTransactions.perform(buyer,p,"oddish",PokecTransactions.Action.STAGE_SOULS,0x801L),"primary-type souls should stage");
        check(menu.stagedSouls()==required,"two display slots accept the same required soul type up to the remaining amount");
        check(buyer.getInventory().getItem(0).getCount()==4,"incompatible souls are untouched");
        check(!PokecPlayerData.isUnlocked(buyer,"oddish"),"staging alone cannot unlock a species");
        check(PokecTransactions.perform(buyer,p,"oddish",PokecTransactions.Action.CONFIRM_SOULS,0x802L),"complete staged soul amount should confirm");
        check(PokecPlayerData.isUnlocked(buyer,"oddish"),"confirmed soul requirement unlocks permanently");
        check(PokecPlayerData.deposited(buyer,"oddish")==required,"confirmed progress persists per player");
        check(buyer.getInventory().getItem(1).getCount()==3,"excess souls are returned to the player's inventory");
        check(buyer.getInventory().getItem(0).getCount()==4,"wrong-type souls remain unchanged");buyer.discard();h.succeed();
    }

    @GameTest(template="test_arena",timeoutTicks=200)
    public static void stagedCoinsReturnWhenMenuCloses(GameTestHelper h){
        floor(h);BlockPos p=h.absolutePos(new BlockPos(5,1,5));terminal(h,p);ServerPlayer buyer=player(h,h.absolutePos(new BlockPos(7,1,5)));
        PokecMenu menu=new PokecMenu(11,buyer.getInventory(),p);buyer.containerMenu=menu;PokecPlayerData.unlock(buyer,"bulbasaur");PokecPlayerData.setFreePurchaseUsed(buyer);
        buyer.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(ModItems.POKE_COIN.get(),12));
        PokecTransactions.perform(buyer,p,"bulbasaur",PokecTransactions.Action.SELECT,0x900L);
        check(PokecTransactions.perform(buyer,p,"bulbasaur",PokecTransactions.Action.STAGE_COINS,0x901L),"coins should move into the temporary slot");
        check(menu.stagedCoins()==12&&buyer.getInventory().getItem(0).isEmpty(),"staged coins leave inventory temporarily");
        menu.removed(buyer);
        check(menu.stagedCoins()==0,"closing clears the temporary slot");
        check(buyer.getInventory().items.stream().filter(s->s.is(ModItems.POKE_COIN.get())).mapToInt(net.minecraft.world.item.ItemStack::getCount).sum()==12,"uncommitted coins return on close");
        buyer.discard();h.succeed();
    }

    @GameTest(template="test_arena",timeoutTicks=200)
    public static void lockedEvolutionRejectsEveryPokecDeposit(GameTestHelper h){
        floor(h);BlockPos p=h.absolutePos(new BlockPos(5,1,5));terminal(h,p);ServerPlayer buyer=player(h,h.absolutePos(new BlockPos(7,1,5)));
        PokecMenu menu=new PokecMenu(12,buyer.getInventory(),p);buyer.containerMenu=menu;
        buyer.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(ModItems.POKE_COIN.get(),16));
        buyer.getInventory().setItem(1,new net.minecraft.world.item.ItemStack(ModItems.SOULS.get("fire").get(),16));
        PokecTransactions.perform(buyer,p,"charmeleon",PokecTransactions.Action.SELECT,0xA00L);
        check(!PokecTransactions.perform(buyer,p,"charmeleon",PokecTransactions.Action.STAGE_COINS,0xA01L),"locked evolutions cannot stage PokéCoins");
        check(!PokecTransactions.perform(buyer,p,"charmeleon",PokecTransactions.Action.STAGE_SOULS,0xA02L),"locked evolutions cannot stage souls");
        check(!PokecTransactions.perform(buyer,p,"charmeleon",PokecTransactions.Action.PURCHASE,0xA03L),"locked evolutions cannot be purchased");
        check(buyer.getInventory().getItem(0).getCount()==16&&buyer.getInventory().getItem(1).getCount()==16,"blocked inputs remain untouched");
        check(menu.stagedCoins()==0&&menu.stagedSouls()==0,"blocked slots stay empty");buyer.discard();h.succeed();
    }
    @GameTest(template="test_arena",timeoutTicks=200)
    public static void evolutionUnlockIsOwnerAndEdgeBound(GameTestHelper h){
        floor(h);ServerPlayer owner=player(h,h.absolutePos(new BlockPos(7,1,5)));
        MinemonCreature owned=h.spawn(ModEntities.SPECIES.get("charmander").get(),new BlockPos(5,1,5));owned.tame(owner);
        owned.notifyPokecEvolution("charmeleon");check(PokecPlayerData.isUnlocked(owner,"charmeleon"),"owned valid evolution unlocks its target");
        owned.notifyPokecEvolution("charizard");check(!PokecPlayerData.isUnlocked(owner,"charizard"),"must evolve through the declared adjacent stage");owner.discard();h.succeed();
    }
    private PokecGameTests(){}
}
