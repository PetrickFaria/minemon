package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.block.PokecBlock;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.menu.PokecMenu;
import br.com.petrickfaria.minemon.pokec.*;
import br.com.petrickfaria.minemon.registry.ModBlocks;
import br.com.petrickfaria.minemon.registry.ModEntities;
import br.com.petrickfaria.minemon.registry.ModItems;
import br.com.petrickfaria.minemon.item.PokeballItem;
import br.com.petrickfaria.minemon.item.PokemonBallLedger;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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
            var egg=ModItems.EGGS.get("bulbasaur").get();
            long delivered=buyer.getInventory().items.stream().filter(stack->stack.is(egg)&&stack.hasTag()
                    &&stack.getTag().hasUUID("MinemonPokecOwner")&&buyer.getUUID().equals(stack.getTag().getUUID("MinemonPokecOwner")))
                    .mapToLong(net.minecraft.world.item.ItemStack::getCount).sum();
            check(ok,"first purchase should succeed without coins");check(PokecPlayerData.usedFreePurchase(buyer),"free purchase flag must persist");check(delivered==1,"free purchase delivers one spawn egg bound to the buyer");
            check(!PokecTransactions.perform(buyer,p,"bulbasaur",PokecTransactions.Action.PURCHASE,0x501L),"duplicate transaction id must not deliver twice");
            PokecPlayerData.setCoins(buyer,0);check(!PokecTransactions.perform(buyer,p,"bulbasaur",PokecTransactions.Action.PURCHASE,0x502L),"second purchase cannot reuse the free acquisition");
            PokecPlayerData.setCoins(buyer,PokecEconomy.get().coins("bulbasaur"));buyer.tickCount+=4;check(PokecTransactions.perform(buyer,p,"bulbasaur",PokecTransactions.Action.PURCHASE,0x503L),"later acquisition should charge the configured price");
            check(PokecPlayerData.coins(buyer)==0,"successful purchase must debit PokéCoins once");
            long afterSecond=buyer.getInventory().items.stream().filter(stack->stack.is(egg)&&stack.hasTag()
                    &&stack.getTag().hasUUID("MinemonPokecOwner")&&buyer.getUUID().equals(stack.getTag().getUUID("MinemonPokecOwner")))
                    .mapToLong(net.minecraft.world.item.ItemStack::getCount).sum();
            check(afterSecond==2,"free and paid purchases deliver exactly two owned spawn eggs without spawning Pokémon");
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
        var egg=ModItems.EGGS.get("bulbasaur").get();
        long ownedEggs=buyer.getInventory().items.stream().filter(stack->stack.is(egg)&&stack.hasTag()
                &&stack.getTag().hasUUID("MinemonPokecOwner")&&buyer.getUUID().equals(stack.getTag().getUUID("MinemonPokecOwner")))
                .mapToLong(net.minecraft.world.item.ItemStack::getCount).sum();
        check(ownedEggs==1,"one accepted payment delivers one owner-bound spawn egg");buyer.discard();h.succeed();
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
        int staged=Math.min(12,PokecEconomy.get().coins("bulbasaur"));
        check(menu.stagedCoins()==staged&&buyer.getInventory().items.stream().filter(s->s.is(ModItems.POKE_COIN.get())).mapToInt(net.minecraft.world.item.ItemStack::getCount).sum()==12-staged,
                "only coins needed for the configured price leave inventory temporarily");
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

    @GameTest(template="test_arena",timeoutTicks=200)
    public static void pokeballCapturesOnlyOwnersAndRestoresPokemonData(GameTestHelper h){
        floor(h);ServerPlayer owner=player(h,h.absolutePos(new BlockPos(7,1,5)));
        ServerPlayer stranger=player(h,h.absolutePos(new BlockPos(9,1,5)));
        MinemonCreature owned=h.spawn(ModEntities.SPECIES.get("charmander").get(),new BlockPos(5,1,5));
        owned.tame(owner);owned.setPokemonLevel(7);owned.setHealth(5.0F);
        ItemStack occupied=new ItemStack(ModItems.POKEBALL.get());owner.setItemInHand(InteractionHand.MAIN_HAND,occupied);
        var capture=ModItems.POKEBALL.get().interactLivingEntity(occupied,owner,owned,InteractionHand.MAIN_HAND);
        ItemStack storedBall=owner.getMainHandItem();var stored=PokeballItem.readStored(storedBall);
        check(capture.consumesAction()&&stored!=null,"owned tamed Pokémon should be captured in a filled Pokébola");
        check(stored.species().equals("charmander")&&stored.owner().equals(owner.getUUID())&&stored.level()==7,"species, owner, and level must persist in the ball");
        check(stored.health()==5.0F&&stored.entityData().getFloat("Health")==5.0F,"current health and full entity NBT must persist");
        check(!owned.isAlive(),"capturing removes exactly the original entity");

        MinemonCreature clickRouted=h.spawn(ModEntities.SPECIES.get("pikachu").get(),new BlockPos(6,1,6));
        clickRouted.tame(owner);
        ItemStack clickBall=new ItemStack(ModItems.POKEBALL.get());owner.setItemInHand(InteractionHand.OFF_HAND,clickBall);
        var routedResult=clickRouted.mobInteract(owner,InteractionHand.OFF_HAND);
        check(routedResult.consumesAction()&&PokeballItem.isOccupied(owner.getOffhandItem())&&!clickRouted.isAlive(),
                "Pokémon mob interaction must route a Pokébola click to the existing capture handler");

        MinemonCreature wild=h.spawn(ModEntities.SPECIES.get("squirtle").get(),new BlockPos(6,1,5));
        ItemStack empty=new ItemStack(ModItems.POKEBALL.get());owner.setItemInHand(InteractionHand.OFF_HAND,empty);
        check(!ModItems.POKEBALL.get().interactLivingEntity(empty,owner,wild,InteractionHand.OFF_HAND).consumesAction(),"wild Pokémon cannot be captured");
        MinemonCreature foreign=h.spawn(ModEntities.SPECIES.get("pikachu").get(),new BlockPos(10,1,5));foreign.tame(stranger);
        check(!ModItems.POKEBALL.get().interactLivingEntity(empty,owner,foreign,InteractionHand.OFF_HAND).consumesAction(),"another owner's Pokémon cannot be captured");

        BlockPos floorPos=new BlockPos(5,0,5);
        var hit=new BlockHitResult(Vec3.atCenterOf(h.absolutePos(floorPos)),net.minecraft.core.Direction.UP,h.absolutePos(floorPos),false);
        var release=ModItems.POKEBALL.get().useOn(new net.minecraft.world.item.context.UseOnContext(owner,InteractionHand.MAIN_HAND,hit));
        check(release.consumesAction()&&!PokeballItem.isOccupied(owner.getMainHandItem()),"successful release must empty, not destroy, the ball");
        var restored=h.getLevel().getEntitiesOfClass(MinemonCreature.class,new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(3,1,3)),h.absolutePos(new BlockPos(12,5,8))),e->e.getSpeciesSlug().equals("charmander")&&owner.getUUID().equals(e.getOwnerUUID()));
        check(restored.size()==1&&restored.get(0).getPokemonLevel()==7&&restored.get(0).getHealth()==5.0F,"release restores one owned Pokémon with captured state");
        check(!PokeballItem.isOccupied(empty),"empty balls stay empty after rejected capture attempts");
        stranger.discard();owner.discard();h.succeed();
    }

    @GameTest(template="test_arena",timeoutTicks=200)
    public static void masterBallStoresAndRestoresSerializableWildMobs(GameTestHelper h){
        floor(h);BlockPos p=h.absolutePos(new BlockPos(5,1,5));terminal(h,p);
        ServerPlayer owner=player(h,h.absolutePos(new BlockPos(7,1,5)));
        var cow=h.spawn(net.minecraft.world.entity.EntityType.COW,new BlockPos(5,1,7));
        cow.setNoAi(true);cow.setHealth(4.0F);cow.setCustomName(net.minecraft.network.chat.Component.literal("Stored Cow"));
        ItemStack masterBall=new ItemStack(ModItems.MASTER_BALL.get());owner.setItemInHand(InteractionHand.MAIN_HAND,masterBall);
        var capture=ModItems.MASTER_BALL.get().interactLivingEntity(masterBall,owner,cow,InteractionHand.MAIN_HAND);
        ItemStack occupied=owner.getMainHandItem();
        var stored=PokeballItem.readStoredEntity(occupied);
        check(capture.consumesAction()&&stored!=null,"Master Ball should store a serializable wild Mob");
        check(!cow.isAlive()&&stored.entityType().equals("minecraft:cow")
                &&stored.entityData().getFloat("Health")==4.0F,"capturing preserves mob type and full saved health without leaving the source entity");
        owner.getInventory().setItem(9,occupied.copy());
        PokecMenu menu=new PokecMenu(21,owner.getInventory(),p);owner.containerMenu=menu;
        check(!PokecTransactions.perform(owner,p,"bulbasaur",PokecTransactions.Action.STAGE_POKEBALL,0xB04L)
                &&PokeballItem.isOccupied(owner.getMainHandItem()),"PokéC rejects a Master Ball containing a non-Pokémon Mob without consuming it");
        menu.removed(owner);
        owner.getInventory().setItem(9,ItemStack.EMPTY);
        var hit=new BlockHitResult(Vec3.atCenterOf(h.absolutePos(new BlockPos(5,0,5))),net.minecraft.core.Direction.UP,
                h.absolutePos(new BlockPos(5,0,5)),false);
        var release=ModItems.MASTER_BALL.get().useOn(new net.minecraft.world.item.context.UseOnContext(owner,InteractionHand.MAIN_HAND,hit));
        check(release.consumesAction()&&!PokeballItem.isOccupied(owner.getMainHandItem()),"releasing a saved Mob empties and retains the Master Ball");
        var cows=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Cow.class,
                new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(3,1,3)),h.absolutePos(new BlockPos(12,5,8))),
                entity->entity.hasCustomName()&&entity.getCustomName().getString().equals("Stored Cow"));
        check(cows.size()==1&&cows.get(0).getHealth()==4.0F,"Master Ball restores one wild mob with its custom name and health");
        owner.discard();h.succeed();
    }

    @GameTest(template="test_arena",timeoutTicks=200)
    public static void pokecConversionRequiresConfirmationAndConsumesTokenOnce(GameTestHelper h){
        floor(h);BlockPos p=h.absolutePos(new BlockPos(5,1,5));terminal(h,p);ServerPlayer owner=player(h,h.absolutePos(new BlockPos(7,1,5)));
        MinemonCreature creature=h.spawn(ModEntities.SPECIES.get("charmander").get(),new BlockPos(8,1,5));creature.tame(owner);creature.setPokemonLevel(1);
        ItemStack ball=new ItemStack(ModItems.POKEBALL.get());owner.setItemInHand(InteractionHand.MAIN_HAND,ball);
        check(ModItems.POKEBALL.get().interactLivingEntity(ball,owner,creature,InteractionHand.MAIN_HAND).consumesAction(),"test Pokémon should be captured before conversion");
        ItemStack filledBall=owner.getMainHandItem().copy();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        PokecMenu menu=new PokecMenu(20,owner.getInventory(),p);owner.containerMenu=menu;
        owner.getInventory().setItem(9,new ItemStack(ModItems.POKEBALL.get()));
        check(!PokecTransactions.perform(owner,p,"bulbasaur",PokecTransactions.Action.STAGE_POKEBALL,0xAFFL),"an empty Pokébola cannot be staged as a stored Pokémon");
        ItemStack foreignBall=filledBall.copy();foreignBall.getOrCreateTag().getCompound("MinemonStoredPokemon").putUUID("Owner",UUID.randomUUID());
        owner.getInventory().setItem(9,foreignBall);
        check(!PokecTransactions.perform(owner,p,"bulbasaur",PokecTransactions.Action.STAGE_POKEBALL,0xAFEL),"PokéC rejects a stored Pokémon owned by another player");
        check(PokeballItem.isOccupied(owner.getInventory().getItem(9)),"a rejected foreign Pokébola remains untouched");
        owner.getInventory().setItem(9,filledBall);
        check(PokecTransactions.perform(owner,p,"bulbasaur",PokecTransactions.Action.STAGE_POKEBALL,0xB00L),"filled player-owned Pokébola should be staged");
        check(menu.conversionPreview()!=null&&menu.conversionPreview().base()>=3&&menu.conversionPreview().base()<=13,"PokéC previews the 3–13 base from stage, rarity, level, and type");
        UUID token=PokeballItem.readStored(menu.conversionBall()).token();int minimum=menu.conversionPreview().minimum(),maximum=menu.conversionPreview().maximum();
        check(PokeballItem.isOccupied(menu.conversionBall()),"staging must not consume the Pokémon");
        check(PokecTransactions.perform(owner,p,"bulbasaur",PokecTransactions.Action.REQUEST_CONVERSION,0xB01L),"first confirmation request should open a short confirmation window");
        check(PokeballItem.isOccupied(menu.conversionBall()),"requesting confirmation must leave the stored Pokémon intact");
        check(PokecTransactions.perform(owner,p,"bulbasaur",PokecTransactions.Action.CONFIRM_CONVERSION,0xB02L),"confirmed conversion should complete");
        int result=menu.conversionResult();
        check(result>=minimum&&result<=maximum,"nature return must use the configured 75–100 percent integer range");
        check(menu.conversionOutput().isEmpty(),"conversion must deliver souls directly without keeping a PokéC result slot");
        check(owner.getInventory().items.stream().filter(stack->ModItems.SOULS.values().stream().anyMatch(item->stack.is(item.get()))).mapToInt(ItemStack::getCount).sum()==result,
                "confirmed conversion must place its rolled souls directly in the player's inventory");
        check(!PokeballItem.isOccupied(menu.conversionBall()),"confirmed conversion leaves an empty Pokébola");
        check(PokemonBallLedger.get(h.getLevel()).isConsumed(token),"spent token cannot be converted or released a second time");
        check(!PokecTransactions.perform(owner,p,"bulbasaur",PokecTransactions.Action.CONFIRM_CONVERSION,0xB03L),"a second confirm cannot roll or deliver souls again");
        menu.removed(owner);
        long returned=owner.getInventory().items.stream().filter(stack->stack.is(ModItems.POKEBALL.get())).mapToLong(ItemStack::getCount).sum();
        long souls=owner.getInventory().items.stream().filter(stack->ModItems.SOULS.values().stream().anyMatch(item->stack.is(item.get()))).mapToLong(ItemStack::getCount).sum();
        check(returned==1,"closing PokéC returns the reusable empty Pokébola exactly once");
        check(souls==result,"closing PokéC safely returns only the souls actually rolled");
        owner.discard();h.succeed();
    }
    private PokecGameTests(){}
}
