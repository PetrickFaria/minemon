package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.block.PokecBlock;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.entity.CaptureBallProjectile;
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
import net.minecraft.world.entity.monster.Zombie;
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
        Zombie activeEnemy=h.spawn(net.minecraft.world.entity.EntityType.ZOMBIE,new BlockPos(9,1,6));
        clickRouted.setTarget(activeEnemy);
        clickRouted.setDeltaMovement(0.2D,0.0D,0.0D);
        clickRouted.setInLove(owner);
        clickRouted.handleEntityEvent((byte)61); // active attack animation
        ItemStack clickBall=new ItemStack(ModItems.POKEBALL.get());owner.setItemInHand(InteractionHand.OFF_HAND,clickBall);
        var routedResult=clickRouted.mobInteract(owner,InteractionHand.OFF_HAND);
        check(routedResult.consumesAction()&&PokeballItem.isOccupied(owner.getOffhandItem())&&!clickRouted.isAlive(),
                "A valid empty Pokébola must take priority and store its owner’s Pokémon during movement, combat, attack animation, and breeding state");
        check(PokeballItem.readStoredEntity(owner.getOffhandItem())!=null,
                "Combat interruption must finish storing complete entity data before removing the Pokémon");
        var storedToken=PokeballItem.readStoredEntity(owner.getOffhandItem()).token();
        var repeated=ModItems.POKEBALL.get().interactLivingEntity(owner.getOffhandItem(),owner,clickRouted,InteractionHand.OFF_HAND);
        check(!repeated.consumesAction() && PokeballItem.readStoredEntity(owner.getOffhandItem()).token().equals(storedToken)
                        && PokeballItem.isOccupied(owner.getOffhandItem()),
                "Repeated immediate use cannot duplicate a Pokémon or overwrite its stored token");

        MinemonCreature wild=h.spawn(ModEntities.SPECIES.get("squirtle").get(),new BlockPos(6,1,5));
        ItemStack empty=new ItemStack(ModItems.POKEBALL.get());owner.setItemInHand(InteractionHand.OFF_HAND,empty);
        check(!ModItems.POKEBALL.get().interactLivingEntity(empty,owner,wild,InteractionHand.OFF_HAND).consumesAction(),"wild Pokémon cannot be captured");
        MinemonCreature foreign=h.spawn(ModEntities.SPECIES.get("pikachu").get(),new BlockPos(10,1,5));foreign.tame(stranger);
        check(!ModItems.POKEBALL.get().interactLivingEntity(empty,owner,foreign,InteractionHand.OFF_HAND).consumesAction(),"another owner's Pokémon cannot be captured");

        for(int y=1;y<=4;y++)h.setBlock(new BlockPos(5,y,5),Blocks.WATER);
        BlockPos floorPos=new BlockPos(5,0,5);
        var hit=new BlockHitResult(Vec3.atCenterOf(h.absolutePos(floorPos)),net.minecraft.core.Direction.UP,h.absolutePos(floorPos),false);
        var release=ModItems.POKEBALL.get().useOn(new net.minecraft.world.item.context.UseOnContext(owner,InteractionHand.MAIN_HAND,hit));
        check(release.consumesAction()&&!PokeballItem.isOccupied(owner.getMainHandItem()),"successful release must empty, not destroy, the ball");
        var restored=h.getLevel().getEntitiesOfClass(MinemonCreature.class,new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(3,1,3)),h.absolutePos(new BlockPos(12,5,8))),e->e.getSpeciesSlug().equals("charmander")&&owner.getUUID().equals(e.getOwnerUUID()));
        check(restored.size()==1&&restored.get(0).getPokemonLevel()==7&&restored.get(0).getHealth()==5.0F,"release restores one owned Pokémon with captured state");
        check(h.getLevel().getFluidState(restored.get(0).blockPosition()).is(net.minecraft.tags.FluidTags.WATER),"Pokébola must release directly inside submerged space");
        for(int y=1;y<=4;y++)h.setBlock(new BlockPos(5,y,5),Blocks.AIR);
        check(!PokeballItem.isOccupied(empty),"empty balls stay empty after rejected capture attempts");
        stranger.discard();owner.discard();h.succeed();
    }

    @GameTest(template="test_arena",timeoutTicks=100)
    public static void pokeballsStoreSleepingAndDefeatedOwnedPokemonWithoutHealing(GameTestHelper h){
        floor(h);ServerPlayer owner=player(h,h.absolutePos(new BlockPos(7,1,5)));
        MinemonCreature sleeper=h.spawn(ModEntities.SPECIES.get("pikachu").get(),new BlockPos(5,1,5));
        sleeper.tame(owner);sleeper.setOwnerUUID(owner.getUUID());sleeper.setHealth(sleeper.getMaxHealth()-4);sleeper.setSleeping(true);
        float expectedSleepHealth=sleeper.getHealth();
        ActivePokemonData.register(sleeper);
        owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.POKEBALL.get()));
        var sleepResult=ModItems.POKEBALL.get().interactLivingEntity(owner.getMainHandItem(),owner,sleeper,InteractionHand.MAIN_HAND);
        check(sleepResult.consumesAction()&&PokeballItem.isOccupied(owner.getMainHandItem()),"An owned sleeping Pokémon should be stored directly");
        // ItemStack NBT is the representation written into player/world saves. Round-trip it
        // before reading or releasing so both the stored payload and entity restoration are covered.
        owner.setItemInHand(InteractionHand.MAIN_HAND,
                ItemStack.of(owner.getMainHandItem().save(new net.minecraft.nbt.CompoundTag())));
        var sleepStored=PokeballItem.readStoredEntity(owner.getMainHandItem());
        check(sleepStored!=null,"Saved occupied Pokébola should remain readable after an NBT reload");
        check(sleepStored.entityData().getBoolean("MinemonSleeping"),"Stored sleep state must survive an NBT reload");
        check(sleepStored.entityData().getFloat("Health")==expectedSleepHealth,"Stored current health must survive without healing");
        check(sleeper.isRemoved()&&ActivePokemonData.get(h.getLevel()).count(owner.getUUID())==0,
                "Storage must remove the original and release the active slot");
        BlockPos releaseFloor=h.absolutePos(new BlockPos(4,0,4));
        var releaseHit=new BlockHitResult(Vec3.atCenterOf(releaseFloor),net.minecraft.core.Direction.UP,releaseFloor,false);
        var sleepRelease=ModItems.POKEBALL.get().useOn(new net.minecraft.world.item.context.UseOnContext(owner,InteractionHand.MAIN_HAND,releaseHit));
        var releasedSleeping=h.getLevel().getEntitiesOfClass(MinemonCreature.class,
                new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(2,1,2)),h.absolutePos(new BlockPos(9,5,9))),
                pokemon->pokemon.getSpeciesSlug().equals("pikachu")&&owner.getUUID().equals(pokemon.getOwnerUUID()));
        check(sleepRelease.consumesAction()&&releasedSleeping.size()==1,"Saved sleeping Pokémon should release exactly once");
        check(releasedSleeping.get(0).getHealth()==expectedSleepHealth,"Pokébola release must restore current health without healing");
        check(releasedSleeping.get(0).isSleeping(),"Pokébola release must restore the saved sleeping state");
        releasedSleeping.get(0).discard();ActivePokemonData.unregister(releasedSleeping.get(0));

        MinemonCreature defeated=h.spawn(ModEntities.SPECIES.get("charmander").get(),new BlockPos(6,1,5));
        defeated.tame(owner);defeated.setOwnerUUID(owner.getUUID());defeated.setPokemonProgress(4,3);ActivePokemonData.register(defeated);
        defeated.die(net.minecraft.world.damagesource.DamageSource.GENERIC);
        check(defeated.isDefeated()&&defeated.getHealth()==0.0F,"Fixture must be genuinely defeated before storage");
        owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.POKEBALL.get()));
        var downResult=ModItems.POKEBALL.get().interactLivingEntity(owner.getMainHandItem(),owner,defeated,InteractionHand.MAIN_HAND);
        var downStored=PokeballItem.readStoredEntity(owner.getMainHandItem());
        check(downResult.consumesAction()&&downStored!=null&&downStored.entityData().getBoolean("MinemonDefeated"),
                "A defeated Pokémon should be stored with its defeat flag intact");
        check(downStored.entityData().getFloat("Health")==0.0F&&downStored.entityData().getInt("MinemonPokemonLevel")==4,
                "Storage must preserve zero health and progression without reviving");
        check(defeated.isRemoved()&&ActivePokemonData.get(h.getLevel()).count(owner.getUUID())==0,
                "Storing a defeated Pokémon must release its active slot exactly once");
        owner.discard();h.succeed();
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

    @GameTest(template="test_arena",timeoutTicks=200)
    public static void pokecPlayerInventoryQuickMoveUsesVanillaRangesWithoutDuplication(GameTestHelper h){
        floor(h);ServerPlayer owner=player(h,h.absolutePos(new BlockPos(7,1,5)));
        PokecMenu menu=new PokecMenu(22,owner.getInventory(),h.absolutePos(new BlockPos(5,1,5)));
        owner.containerMenu=menu;
        check(menu.slots.size()==40,"PokéC must expose 36 player slots plus four terminal slots");
        owner.getInventory().setItem(9,new ItemStack(Blocks.DIRT,7));
        ItemStack movedToHotbar=menu.quickMoveStack(owner,0);
        check(movedToHotbar.getCount()==7,"shift-click should return the moved original stack");
        check(owner.getInventory().getItem(9).isEmpty(),"main-inventory source slot should be emptied");
        check(owner.getInventory().getItem(0).is(Blocks.DIRT.asItem())&&owner.getInventory().getItem(0).getCount()==7,
                "main-inventory stack should move to the hotbar");
        check(owner.getInventory().items.stream().filter(stack->stack.is(Blocks.DIRT.asItem())).mapToInt(ItemStack::getCount).sum()==7,
                "shift-click must not duplicate the moved stack");

        owner.getInventory().setItem(0,new ItemStack(Blocks.OAK_LOG,4));
        ItemStack movedToMain=menu.quickMoveStack(owner,27);
        check(movedToMain.getCount()==4,"hotbar shift-click should return the moved original stack");
        check(owner.getInventory().getItem(0).isEmpty(),"hotbar source slot should be emptied");
        check(owner.getInventory().getItem(9).is(Blocks.OAK_LOG.asItem())&&owner.getInventory().getItem(9).getCount()==4,
                "hotbar stack should move into the main inventory");
        check(owner.getInventory().items.stream().filter(stack->stack.is(Blocks.OAK_LOG.asItem())).mapToInt(ItemStack::getCount).sum()==4,
                "reverse shift-click must not duplicate the moved stack");
        owner.discard();h.succeed();
    }
    // Preserve legacy combat arena placement by sharing the existing persistence batch.
    @GameTest(template="test_arena",batch="refinement_stats",timeoutTicks=200)
    public static void terminalSlotsSupportVanillaClicksAndPartialSpeciesDeposits(GameTestHelper h){
        floor(h);BlockPos pos=h.absolutePos(new BlockPos(5,1,5));terminal(h,pos);
        ServerPlayer p=player(h,h.absolutePos(new BlockPos(7,1,5)));
        PokecMenu menu=new PokecMenu(31,p.getInventory(),pos);p.containerMenu=menu;
        p.getInventory().setItem(9,new ItemStack(Blocks.DIRT,12));
        menu.clicked(0,1,net.minecraft.world.inventory.ClickType.PICKUP,p);
        check(menu.getCarried().getCount()==6&&menu.slots.get(0).getItem().getCount()==6,"right click splits vanilla stack");
        menu.clicked(1,1,net.minecraft.world.inventory.ClickType.PICKUP,p);
        check(menu.slots.get(1).getItem().getCount()==1&&menu.getCarried().getCount()==5,"right click places one item");
        menu.clicked(2,0,net.minecraft.world.inventory.ClickType.PICKUP,p);
        check(menu.getCarried().isEmpty()&&menu.slots.get(2).getItem().getCount()==5,"left click places remainder");
        menu.clicked(2,2,net.minecraft.world.inventory.ClickType.SWAP,p);
        check(p.getInventory().getItem(2).getCount()==5&&menu.slots.get(2).getItem().isEmpty(),"number key swaps with hotbar");
        menu.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,p);
        menu.clicked(-999,0,net.minecraft.world.inventory.ClickType.QUICK_CRAFT,p);
        menu.clicked(3,1,net.minecraft.world.inventory.ClickType.QUICK_CRAFT,p);
        menu.clicked(4,1,net.minecraft.world.inventory.ClickType.QUICK_CRAFT,p);
        menu.clicked(-999,2,net.minecraft.world.inventory.ClickType.QUICK_CRAFT,p);
        check(menu.slots.get(3).getItem().getCount()==3&&menu.slots.get(4).getItem().getCount()==3&&menu.getCarried().isEmpty(),"vanilla left drag distributes evenly");
        menu.selectSpecies(p,"gastly");
        var type=SoulType.fromSpeciesType(PokecSpecies.get("gastly").primaryType());
        var soul=ModItems.SOULS.get(type.id()).get();int cost=PokecEconomy.get().souls("gastly");
        p.getInventory().setItem(9,new ItemStack(soul,10));menu.quickMoveStack(p,0);
        check(menu.slots.get(36).getItem().getCount()==10,"shift click moves actual soul stack to input");
        check(menu.terminal.unlock()&&PokecPlayerData.deposited(p,"gastly")==10,"partial deposit consumes only inserted souls");
        check(!PokecPlayerData.isUnlocked(p,"gastly"),"partial deposit does not unlock early");
        menu.selectSpecies(p,"abra");check(PokecPlayerData.deposited(p,"abra")==0,"deposits are species specific");
        menu.removed(p);menu=new PokecMenu(32,p.getInventory(),pos);p.containerMenu=menu;menu.selectSpecies(p,"gastly");
        check(PokecPlayerData.deposited(p,"gastly")==10,"committed souls survive reopening terminal");
        for(int left=cost-10;left>0;){
            int remaining=cost-PokecPlayerData.deposited(p,"gastly");
            p.getInventory().setItem(9,new ItemStack(soul,64));menu.quickMoveStack(p,0);
            int inserted=Math.min(64,remaining);
            check(menu.slots.get(36).getItem().getCount()==inserted,"shift click is capped at outstanding cost");
            check(menu.terminal.unlock(),"remaining deposit accepted");left-=inserted;
        }
        check(PokecPlayerData.isUnlocked(p,"gastly")&&menu.slots.get(36).getItem().isEmpty(),"exact cost unlocks and empties soul slot");
        check(PokecPlayerData.spawnEnabled(p,"gastly"),"unlocked species defaults to enabled spawn");
        PokecPlayerData.toggleSpawn(p,"gastly");
        check(!PokecPlayerData.spawnEnabled(p,"gastly")&&PokecPlayerData.spawnEnabled(p,"abra"),"spawn toggle changes only selected species");
        menu.removed(p);p.discard();h.succeed();
    }
    @GameTest(template="test_arena",batch="refinement_stats",timeoutTicks=200)
    public static void terminalPurchaseResultChargesOnceAndReturnsUnspentPayment(GameTestHelper h){
        floor(h);BlockPos pos=h.absolutePos(new BlockPos(5,1,5));terminal(h,pos);
        ServerPlayer p=player(h,h.absolutePos(new BlockPos(7,1,5)));
        PokecMenu menu=new PokecMenu(33,p.getInventory(),pos);p.containerMenu=menu;
        PokecPlayerData.setFreePurchaseUsed(p);int price=PokecEconomy.get().coins("bulbasaur");
        p.getInventory().setItem(9,new ItemStack(ModItems.POKE_COIN.get(),64));menu.quickMoveStack(p,0);menu.broadcastChanges();
        check(menu.slots.get(36).getItem().getCount()==price&&p.getInventory().getItem(9).getCount()==64-price,"coin shift click inserts exact purchase cost");
        check(menu.slots.get(37).getItem().is(ModItems.EGGS.get("bulbasaur").get()),"funded purchase previews selected egg");
        check(!menu.slots.get(37).mayPlace(new ItemStack(Blocks.DIRT)),"purchase output rejects inserted items");
        menu.clicked(37,0,net.minecraft.world.inventory.ClickType.PICKUP,p);menu.broadcastChanges();
        check(menu.getCarried().is(ModItems.EGGS.get("bulbasaur").get())&&menu.getCarried().getCount()==1,"result gives exactly one egg");
        check(menu.slots.get(36).getItem().isEmpty()&&menu.slots.get(37).getItem().isEmpty(),"taking result spends coins and clears preview");
        menu.clicked(2,0,net.minecraft.world.inventory.ClickType.PICKUP,p);
        menu.clicked(37,0,net.minecraft.world.inventory.ClickType.PICKUP,p);
        check(menu.getCarried().isEmpty(),"empty result cannot be taken again");
        p.getInventory().setItem(9,new ItemStack(ModItems.POKE_COIN.get(),3));menu.quickMoveStack(p,0);menu.removed(p);
        PokecTransactions.recoverEscrow(p);
        check(p.getInventory().items.stream().filter(s->s.is(ModItems.POKE_COIN.get())).mapToInt(ItemStack::getCount).sum()==3,"closing and recovery return unpaid coins only once");
        p.discard();h.succeed();
    }
    @GameTest(template="test_arena",batch="refinement_stats",timeoutTicks=200)
    public static void terminalReleaseKeepsBothBallTypesAndRejectsSpentTokens(GameTestHelper h){
        floor(h);BlockPos pos=h.absolutePos(new BlockPos(5,1,5));terminal(h,pos);
        ServerPlayer p=player(h,h.absolutePos(new BlockPos(7,1,5)));
        PokecMenu menu=new PokecMenu(34,p.getInventory(),pos);p.containerMenu=menu;
        for(var ballItem:java.util.List.of(ModItems.POKEBALL.get(),ModItems.MASTER_BALL.get())){
            MinemonCreature creature=h.spawn(ModEntities.SPECIES.get("charmander").get(),new BlockPos(8,1,5));creature.tame(p);creature.setPokemonLevel(4);
            ItemStack ball=new ItemStack(ballItem);p.setItemInHand(InteractionHand.MAIN_HAND,ball);
            check(ballItem.interactLivingEntity(ball,p,creature,InteractionHand.MAIN_HAND).consumesAction(),"fixture captures owned Pokémon");
            ItemStack filled=p.getMainHandItem().copy();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            menu.slots.get(38).set(filled);menu.broadcastChanges();
            check(!menu.slots.get(39).hasItem()&&PokeballItem.isOccupied(menu.slots.get(38).getItem()),"insertion previews without releasing");
            var preview=PokemonSoulConversion.preview("charmander",4);
            check(menu.terminal.release(),"Liberar commits release");
            check(menu.slots.get(38).getItem().is(ballItem)&&!PokeballItem.isOccupied(menu.slots.get(38).getItem()),"original ball remains empty in input");
            int reward=menu.slots.get(39).getItem().getCount();
            check(reward>=preview.minimum()&&reward<=preview.maximum(),"reward remains within existing soul yield rules");
            check(!menu.terminal.release(),"repeated release cannot duplicate reward");
            menu.quickMoveStack(p,39);menu.quickMoveStack(p,38);
            menu.slots.get(38).set(filled.copy());
            check(!menu.terminal.release(),"replayed consumed ball token cannot produce another reward");menu.slots.get(38).set(ItemStack.EMPTY);
        }
        menu.removed(p);p.discard();h.succeed();
    }

    @GameTest(template="test_arena",timeoutTicks=200)
    public static void companionOnlyAttacksTheWildTargetItsOwnerHits(GameTestHelper h) {
        floor(h);
        ServerPlayer owner=player(h,h.absolutePos(new BlockPos(7,1,5)));
        MinemonCreature companion=h.spawn(ModEntities.SPECIES.get("bulbasaur").get(),new BlockPos(5,1,5));
        MinemonCreature first=h.spawn(ModEntities.SPECIES.get("charmander").get(),new BlockPos(9,1,5));
        MinemonCreature second=h.spawn(ModEntities.SPECIES.get("squirtle").get(),new BlockPos(10,1,5));
        companion.tame(owner);companion.setOwnerUUID(owner.getUUID());
        check(!companion.canAttack(first)&&!companion.canAttack(second),"a nearby wild Pokémon is not a target before its owner hits one");
        companion.focusPlayerTarget(first);
        check(companion.canAttack(first)&&!companion.canAttack(second),"a direct owner hit focuses the companion on that wild Pokémon alone");
        companion.discard();first.discard();second.discard();owner.discard();h.succeed();
    }

    @GameTest(template="test_arena",timeoutTicks=200)
    public static void defeatedWildCanBeLootedByOneDirectLeftClick(GameTestHelper h) {
        floor(h);
        ServerPlayer owner=player(h,h.absolutePos(new BlockPos(7,1,5)));
        MinemonCreature wild=h.spawn(ModEntities.SPECIES.get("bulbasaur").get(),new BlockPos(8,1,5));
        var data=wild.getPersistentData();data.putBoolean("MinemonWildEncounter",true);
        data.putUUID("MinemonEncounterOwner",owner.getUUID());data.putString("MinemonEncounterState","downed");
        wild.setWildEncounter(true);wild.setWildEncounterOwner(owner.getUUID());wild.setEncounterDowned(true);
        var hit=new net.minecraftforge.event.entity.living.LivingAttackEvent(wild,net.minecraft.world.damagesource.DamageSource.playerAttack(owner),1.0F);
        PokecEvents.playerPokemonAttack(hit);
        check(hit.isCanceled()&&wild.isRemoved(),"one direct left-click collects and removes the defeated wild without opening a menu");
        int collected=owner.getInventory().items.stream().filter(stack->ModItems.SOULS.values().stream().anyMatch(item->stack.is(item.get()))).mapToInt(ItemStack::getCount).sum();
        check(collected>0,"loot is delivered into the owner's inventory");
        var duplicate=new net.minecraftforge.event.entity.living.LivingAttackEvent(wild,net.minecraft.world.damagesource.DamageSource.playerAttack(owner),1.0F);
        PokecEvents.playerPokemonAttack(duplicate);
        int after=owner.getInventory().items.stream().filter(stack->ModItems.SOULS.values().stream().anyMatch(item->stack.is(item.get()))).mapToInt(ItemStack::getCount).sum();
        check(after==collected,"repeated attacks cannot duplicate the reward");
        owner.discard();h.succeed();
    }

    @GameTest(template="test_arena",timeoutTicks=200)
    public static void spawnEggEncounterCreditsLethalBlowToCompanion(GameTestHelper h) {
        floor(h);
        ServerPlayer owner=player(h,h.absolutePos(new BlockPos(7,1,5)));
        MinemonCreature attacker=h.spawn(ModEntities.SPECIES.get("bulbasaur").get(),new BlockPos(5,1,5));
        MinemonCreature wild=h.spawn(ModEntities.SPECIES.get("charmander").get(),new BlockPos(8,1,5));
        attacker.tame(owner);attacker.setOwnerUUID(owner.getUUID());wild.setHealth(1.0F);
        WildEncounterManager.registerSpawnEggEncounter(wild,owner);
        int oldLevel=attacker.getPokemonLevel(),oldXp=attacker.getPokemonExperience();
        int oldTrainerLevel=PokecPlayerData.trainerLevel(owner);
        double oldTrainerXp=PokecPlayerData.trainerXp(owner);
        int rewardHalfUnits=br.com.petrickfaria.minemon.entity.PokemonProgression.xpRewardHalfUnitsForWildRarity(
                wild.getPokemonRarity(),br.com.petrickfaria.minemon.config.MinemonConfig.POKEMON_XP_PER_ZOMBIE.get());
        check(WildEncounterManager.enterDowned(wild,net.minecraft.world.damagesource.DamageSource.mobAttack(attacker)),"lethal companion hit downs an owned spawn-egg encounter");
        check(wild.getPersistentData().getBoolean("MinemonCompanionXpHandled"),"spawn-egg lethal blow is recorded by the exactly-once XP handler");
        check(attacker.getPokemonLevel()>oldLevel||attacker.getPokemonExperience()>oldXp,
                "spawn-egg lethal blow awards rarity-scaled Pokémon XP");
        check(PokecPlayerData.trainerLevel(owner)>oldTrainerLevel||PokecPlayerData.trainerXp(owner)>oldTrainerXp,
                "trainer receives the additional ten percent share");
        double awardedTrainerXp=PokecPlayerData.trainerXp(owner);
        int awardedLevel=attacker.getPokemonLevel(),awardedXp=attacker.getPokemonExperience();
        WildEncounterManager.awardCompanionExperience(wild,net.minecraft.world.damagesource.DamageSource.mobAttack(attacker));
        check(attacker.getPokemonLevel()==awardedLevel&&attacker.getPokemonExperience()==awardedXp,"the same encounter cannot award companion XP twice");
        check(PokecPlayerData.trainerXp(owner)==awardedTrainerXp,"the same encounter cannot award trainer XP twice");
        attacker.discard();wild.discard();owner.discard();h.succeed();
    }

    @GameTest(template="test_arena",timeoutTicks=200)
    public static void masterBallStoresWildEncounterWithoutDomesticatingIt(GameTestHelper h) {
        floor(h);
        ServerPlayer owner=player(h,h.absolutePos(new BlockPos(7,1,5)));
        MinemonCreature wild=h.spawn(ModEntities.SPECIES.get("bulbasaur").get(),new BlockPos(8,1,5));
        wild.getPersistentData().putBoolean("MinemonWildEncounter",true);
        wild.getPersistentData().putUUID("MinemonEncounterOwner",UUID.randomUUID());
        wild.getPersistentData().putString("MinemonEncounterState","active");
        wild.setCustomName(net.minecraft.network.chat.Component.literal("Stored Wild Pokémon"));
        wild.setHealth(3.0F);
        ItemStack ball=new ItemStack(ModItems.MASTER_BALL.get());
        owner.setItemInHand(InteractionHand.MAIN_HAND,ball);
        var result=ModItems.MASTER_BALL.get().interactLivingEntity(ball,owner,wild,InteractionHand.MAIN_HAND);
        var stored=PokeballItem.readStoredEntity(owner.getMainHandItem());
        check(result.consumesAction()&&stored!=null,"Master Ball stores a wild encounter without conventional capture rolls");
        var forgeData=stored.entityData().getCompound("ForgeData");
        check(forgeData.getBoolean("MinemonWildEncounter")
                &&forgeData.getString("MinemonEncounterState").equals("active"),
                "Master Ball preserves wild encounter state in entity NBT");
        check(stored.entityData().getBoolean("Tame")==false,
                "stored wild encounter remains untamed in the saved entity data");
        var releaseHit=new BlockHitResult(Vec3.atCenterOf(h.absolutePos(new BlockPos(5,0,5))),
                net.minecraft.core.Direction.UP,h.absolutePos(new BlockPos(5,0,5)),false);
        var release=ModItems.MASTER_BALL.get().useOn(new net.minecraft.world.item.context.UseOnContext(
                owner,InteractionHand.MAIN_HAND,releaseHit));
        var restored=h.getLevel().getEntitiesOfClass(MinemonCreature.class,
                new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(3,1,3)),h.absolutePos(new BlockPos(12,5,8))),
                entity->entity.hasCustomName()&&entity.getCustomName().getString().equals("Stored Wild Pokémon"));
        check(release.consumesAction()&&restored.size()==1
                &&!restored.get(0).isTame()
                &&restored.get(0).getPersistentData().getBoolean("MinemonWildEncounter"),
                "released Master Ball restores the wild encounter without domesticating it");
        owner.discard();h.succeed();
    }

    @GameTest(template="test_arena",timeoutTicks=120)
    public static void ordinaryMobKillAwardsCompanionAndTrainerExperienceOnce(GameTestHelper h) {
        floor(h);
        ServerPlayer owner=player(h,h.absolutePos(new BlockPos(7,1,5)));
        MinemonCreature attacker=h.spawn(ModEntities.SPECIES.get("bulbasaur").get(),new BlockPos(5,1,5));
        var zombie=h.spawn(net.minecraft.world.entity.EntityType.ZOMBIE,new BlockPos(8,1,5));
        attacker.tame(owner);attacker.setOwnerUUID(owner.getUUID());
        int expected=zombie.getExperienceReward();
        WildEncounterManager.awardCompanionExperience(zombie,net.minecraft.world.damagesource.DamageSource.mobAttack(attacker));
        check(zombie.getPersistentData().getBoolean("MinemonCompanionXpHandled"),"ordinary mob XP award is marked exactly once");
        check(expected<=0||(attacker.getPokemonLevel()>1||attacker.getPokemonExperience()>0),
                "companion receives ordinary mob XP");
        double expectedTrainer=Math.max(0,expected)*0.1D;
        check(Math.abs(PokecPlayerData.trainerXp(owner)-expectedTrainer)<1.0E-9D,
                "trainer receives exactly ten percent of ordinary mob XP");
        double before=PokecPlayerData.trainerXp(owner);
        WildEncounterManager.awardCompanionExperience(zombie,net.minecraft.world.damagesource.DamageSource.mobAttack(attacker));
        check(PokecPlayerData.trainerXp(owner)==before,"duplicate mob death handling does not award trainer XP twice");
        owner.discard();zombie.discard();attacker.discard();h.succeed();
    }

    @GameTest(template="test_arena",timeoutTicks=120)
    public static void encounterCapsMatchRequestedLimits(GameTestHelper h) {
        check(WildEncounterManager.MAX_ACTIVE==25,"active encounter cap is twenty-five");
        check(WildEncounterManager.MAX_DOWNED==10,"downed encounter cap is ten");
        h.succeed();
    }

    @GameTest(template="test_arena",timeoutTicks=120)
    public static void captureCommitsOnlyAfterTheBallAnimationCompletes(GameTestHelper h) {
        floor(h);
        ServerPlayer owner=player(h,h.absolutePos(new BlockPos(7,1,5)));
        MinemonCreature wild=h.spawn(ModEntities.SPECIES.get("bulbasaur").get(),new BlockPos(8,1,5));
        var data=wild.getPersistentData();data.putBoolean("MinemonWildEncounter",true);
        data.putUUID("MinemonEncounterOwner",owner.getUUID());data.putString("MinemonEncounterState","downed");
        wild.setWildEncounter(true);wild.setWildEncounterOwner(owner.getUUID());wild.setEncounterDowned(true);
        WildEncounterManager.markCapturePending(wild);
        check(CaptureBallProjectile.launch((net.minecraft.server.level.ServerLevel)h.getLevel(),owner,wild,true,false,false,InteractionHand.MAIN_HAND),"capture animation projectile should spawn");
        check(WildEncounterManager.isDowned(wild)&&!wild.isTame()&&!wild.isRemoved(),"capture must remain uncommitted during the ball animation");
        h.runAfterDelay(65,()->{
            check(wild.isRemoved(),"successful capture removes the wild only after animation completion");
            check(PokeballItem.isOccupied(owner.getMainHandItem()),"completed capture delivers one occupied Pokébola to the source slot");
            check(PokeballItem.readStoredEntity(owner.getMainHandItem())!=null,"the delivered ball contains the captured Pokémon");
            owner.discard();h.succeed();
        });
    }

    private PokecGameTests(){}
}
