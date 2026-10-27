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
        h.assertBlockPresent(ModBlocks.POKEC.get(),p);h.assertBlockPresent(ModBlocks.POKEC.get(),p.above());
        h.assertBlockProperty(p,PokecBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
        h.assertBlockProperty(p.above(),PokecBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);h.succeed();
    }
    @GameTest(template="test_arena",timeoutTicks=200)
    public static void firstSuccessfulStarterPurchaseIsOwnedAndFree(GameTestHelper h){
        floor(h);BlockPos p=h.absolutePos(new BlockPos(5,1,5));terminal(h,p);ServerPlayer buyer=player(h,h.absolutePos(new BlockPos(7,1,5)));
        buyer.containerMenu=new PokecMenu(7,buyer.getInventory(),p);PokecPlayerData.unlock(buyer,"bulbasaur");PokecPlayerData.setCoins(buyer,0);
        boolean ok=PokecTransactions.perform(buyer,p,"bulbasaur",PokecTransactions.Action.PURCHASE,0x501L);
        h.runAfterDelay(2,()->{
            var delivered=h.getLevel().getEntitiesOfClass(MinemonCreature.class,new net.minecraft.world.phys.AABB(p).inflate(5),e->e.getSpeciesSlug().equals("bulbasaur")&&buyer.getUUID().equals(e.getOwnerUUID()));
            h.assertTrue(ok,"first purchase should succeed without coins");h.assertTrue(PokecPlayerData.usedFreePurchase(buyer),"free purchase flag must persist");h.assertTrue(!delivered.isEmpty(),"delivered Minemon must be owned by buyer");
            h.assertTrue(!PokecTransactions.perform(buyer,p,"bulbasaur",PokecTransactions.Action.PURCHASE,0x501L),"duplicate transaction id must not deliver twice");
            PokecPlayerData.setCoins(buyer,0);h.assertTrue(!PokecTransactions.perform(buyer,p,"bulbasaur",PokecTransactions.Action.PURCHASE,0x502L),"second purchase cannot reuse the free acquisition");
            PokecPlayerData.setCoins(buyer,100);h.assertTrue(PokecTransactions.perform(buyer,p,"bulbasaur",PokecTransactions.Action.PURCHASE,0x503L),"later acquisition should charge the configured price");
            h.assertTrue(PokecPlayerData.coins(buyer)==0,"successful purchase must debit PokéCoins once");
            var afterSecond=h.getLevel().getEntitiesOfClass(MinemonCreature.class,new net.minecraft.world.phys.AABB(p).inflate(5),e->e.getSpeciesSlug().equals("bulbasaur")&&buyer.getUUID().equals(e.getOwnerUUID()));
            h.assertTrue(afterSecond.size()==2,"free and paid purchases should each deliver exactly one owned Minemon");
            buyer.discard();h.succeed();
        });
    }
    @GameTest(template="test_arena",timeoutTicks=200)
    public static void correctSoulTypeUnlocksBaseSpecies(GameTestHelper h){
        floor(h);BlockPos p=h.absolutePos(new BlockPos(5,1,5));terminal(h,p);ServerPlayer buyer=player(h,h.absolutePos(new BlockPos(7,1,5)));
        buyer.containerMenu=new PokecMenu(8,buyer.getInventory(),p);buyer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(ModItems.SOULS.get("grass").get(),8));
        boolean all=true;for(int i=0;i<8;i++)all&=PokecTransactions.perform(buyer,p,"bulbasaur",PokecTransactions.Action.DEPOSIT,0x600L+i);
        h.assertTrue(all,"matching Grass Soul should be accepted");h.assertTrue(PokecPlayerData.isUnlocked(buyer,"bulbasaur"),"species unlock should persist");
        h.assertTrue(buyer.getMainHandItem().isEmpty(),"exactly the required souls should be consumed");buyer.discard();h.succeed();
    }
    @GameTest(template="test_arena",timeoutTicks=200)
    public static void evolutionUnlockIsOwnerAndEdgeBound(GameTestHelper h){
        floor(h);ServerPlayer owner=player(h,h.absolutePos(new BlockPos(7,1,5)));
        MinemonCreature owned=h.spawn(ModEntities.SPECIES.get("charmander").get(),new BlockPos(5,1,5));owned.tame(owner);
        owned.notifyPokecEvolution("charmeleon");h.assertTrue(PokecPlayerData.isUnlocked(owner,"charmeleon"),"owned valid evolution unlocks its target");
        owned.notifyPokecEvolution("charizard");h.assertTrue(!PokecPlayerData.isUnlocked(owner,"charizard"),"must evolve through the declared adjacent stage");owner.discard();h.succeed();
    }
    private PokecGameTests(){}
}
