package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.item.PokemonTreatItem;
import br.com.petrickfaria.minemon.item.PokemonTreatType;
import br.com.petrickfaria.minemon.pokec.PokemonTreatTypes;
import br.com.petrickfaria.minemon.registry.ModEntities;
import br.com.petrickfaria.minemon.registry.ModItems;
import br.com.petrickfaria.minemon.registry.ModBlocks;
import br.com.petrickfaria.minemon.entity.BreedingEggBlockEntity;
import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Minemon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PokemonTreatGameTests {
    private static final Set<ServerPlayer> TEST_PLAYERS = new HashSet<>();
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void tickPlayers(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        TEST_PLAYERS.removeIf(net.minecraft.world.entity.Entity::isRemoved);
        for (ServerPlayer player : TEST_PLAYERS) player.doTick();
    }
    private static void check(GameTestHelper h, boolean condition, String message) { if (!condition) h.fail(message); }
    private static void floor(GameTestHelper h) {
        for (int x=0;x<16;x++) for(int z=0;z<16;z++) {
            h.setBlock(new BlockPos(x,0,z), Blocks.STONE);
            for(int y=1;y<7;y++) h.setBlock(new BlockPos(x,y,z), Blocks.AIR);
        }
    }
    private static ServerPlayer player(GameTestHelper h, BlockPos pos, GameType mode) {
        ServerPlayer p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), new GameProfile(UUID.randomUUID(), "TreatTester"), null);
        p.connection = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "TreatConnection")).connection;
        p.setGameMode(mode); p.tickCount=100;
        BlockPos at=h.absolutePos(pos); p.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
        h.getLevel().addNewPlayer(p); TEST_PLAYERS.add(p); return p;
    }
    private static MinemonCreature spawn(GameTestHelper h, String slug, BlockPos pos) {
        return h.spawn(ModEntities.SPECIES.get(slug).get(), pos);
    }
    private static void tame(MinemonCreature mob, ServerPlayer owner) { mob.tame(owner); mob.setOrderedToSit(false); }

    @GameTest(template="test_arena", timeoutTicks=100)
    public static void allSeventeenTreatItemsAndModernTypeOverrides(GameTestHelper h) {
        check(h, ModItems.TREATS.size()==17, "Exactly 17 treats must be registered");
        for (PokemonTreatType type : PokemonTreatType.values()) {
            var item=ModItems.TREATS.get(type).get();
            var id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
            check(h, id!=null && id.getNamespace().equals("minemon") && id.getPath().equals(type.itemId()), "Wrong treat registry id for "+type);
            check(h, item.getDefaultInstance().getMaxStackSize()==64, "Treats must stack to 64: "+type);
            check(h, item instanceof PokemonTreatItem, "Treat must carry its type: "+type);
        }
        check(h, PokemonTreatTypes.accepts("charizard","fire")&&PokemonTreatTypes.accepts("charizard","flying"), "Both modern Charizard types must match");
        check(h, PokemonTreatTypes.accepts("gengar","ghost")&&PokemonTreatTypes.accepts("gengar","poison"), "Both Gengar types must match");
        check(h, PokemonTreatTypes.accepts("gyarados","water")&&PokemonTreatTypes.accepts("gyarados","flying"), "Both Gyarados types must match");
        check(h, PokemonTreatTypes.accepts("clefairy","fairy")&&PokemonTreatTypes.accepts("jigglypuff","fairy")&&PokemonTreatTypes.accepts("mrmime","fairy"), "Modern Fairy types must match");
        check(h, PokemonTreatTypes.accepts("magnemite","steel")&&PokemonTreatTypes.accepts("magneton","steel"), "Modern Steel types must match");
        check(h, PokemonTreatTypes.accepts("bulbasaur","grass")&&PokemonTreatTypes.accepts("oddish","grass"), "Grass treats must match Grass Pokémon");
        check(h, !PokemonTreatTypes.accepts("bulbasaur","fire"), "Incompatible types must not match");
        h.succeed();
    }

    @GameTest(template="test_arena", timeoutTicks=100)
    public static void compatibleGrassTreatTamesAndBoneDoesNot(GameTestHelper h) {
        floor(h); ServerPlayer owner=player(h,new BlockPos(3,1,3),GameType.SURVIVAL);
        MinemonCreature bulbasaur=spawn(h,"bulbasaur",new BlockPos(5,1,5));
        int previous=br.com.petrickfaria.minemon.config.MinemonConfig.TAME_CHANCE_PERCENT.get();
        try {
            br.com.petrickfaria.minemon.config.MinemonConfig.TAME_CHANCE_PERCENT.set(100);
            var bone=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BONE,2);
            owner.setItemInHand(InteractionHand.MAIN_HAND,bone);
            bulbasaur.mobInteract(owner,InteractionHand.MAIN_HAND);
            check(h,!bulbasaur.isTame()&&bone.getCount()==2,"Bone must not tame or be consumed");
            var grass=ModItems.TREATS.get(PokemonTreatType.GRASS).get().getDefaultInstance();
            owner.setItemInHand(InteractionHand.MAIN_HAND,grass);
            bulbasaur.mobInteract(owner,InteractionHand.MAIN_HAND);
            check(h,bulbasaur.isTame()&&owner.getUUID().equals(bulbasaur.getOwnerUUID()),"Compatible grass treat must tame and assign owner");
            check(h,grass.isEmpty(),"Successful treat taming consumes one treat");
        } finally { br.com.petrickfaria.minemon.config.MinemonConfig.TAME_CHANCE_PERCENT.set(previous); }
        h.succeed();
    }

    @GameTest(template="test_arena", timeoutTicks=100)
    public static void treatHealsOnlyCompatibleOwnedPokemonAndConsumesOne(GameTestHelper h) {
        floor(h); ServerPlayer owner=player(h,new BlockPos(3,1,3),GameType.SURVIVAL);
        MinemonCreature charizard=spawn(h,"charizard",new BlockPos(5,1,5)); tame(charizard,owner);
        charizard.setHealth(10.0F);
        var fire=ModItems.TREATS.get(PokemonTreatType.FIRE).get().getDefaultInstance(); owner.setItemInHand(InteractionHand.MAIN_HAND,fire);
        charizard.mobInteract(owner,InteractionHand.MAIN_HAND);
        check(h, charizard.getHealth()==14.0F, "Fire treat must restore 4 health");
        check(h, fire.getCount()==0, "One valid survival use must consume one treat");
        var water=ModItems.TREATS.get(PokemonTreatType.AQUATIC).get().getDefaultInstance(); owner.setItemInHand(InteractionHand.MAIN_HAND,water);
        float health=charizard.getHealth(); charizard.mobInteract(owner,InteractionHand.MAIN_HAND);
        check(h, charizard.getHealth()==health&&water.getCount()==1, "An incompatible treat must have no effect and not be consumed");
        h.succeed();
    }

    @GameTest(template="test_arena", timeoutTicks=100)
    public static void fullHealthTreatStartsBreedingWithoutConsumingInCreative(GameTestHelper h) {
        floor(h); ServerPlayer owner=player(h,new BlockPos(3,1,3),GameType.CREATIVE);
        MinemonCreature first=spawn(h,"charizard",new BlockPos(5,1,5)); tame(first,owner);
        var fire=ModItems.TREATS.get(PokemonTreatType.FIRE).get().getDefaultInstance(); owner.setItemInHand(InteractionHand.MAIN_HAND,fire);
        first.mobInteract(owner,InteractionHand.MAIN_HAND);
        check(h, first.isInLove(), "Compatible full-health adult should enter love mode");
        check(h, fire.getCount()==1, "Creative use must not consume the treat");
        MinemonCreature second=spawn(h,"metapod",new BlockPos(6,1,5)); tame(second,owner);
        var bug=ModItems.TREATS.get(PokemonTreatType.BUG).get().getDefaultInstance(); owner.setItemInHand(InteractionHand.MAIN_HAND,bug);
        second.mobInteract(owner,InteractionHand.MAIN_HAND);
        check(h,second.isInLove(),"A different species must accept its own compatible treat");
        check(h,bug.getCount()==1,"Creative use must not consume the second parent's treat");
        check(h, first.canMate(second), "Different species with the same owner and separate compatible treats should be able to mate");
        h.succeed();
    }

    @GameTest(template="test_arena", timeoutTicks=100)
    public static void successfulBreedingCreatesOneOwnedIncubatingEggWithCooldown(GameTestHelper h) {
        floor(h); ServerPlayer owner=player(h,new BlockPos(3,1,3),GameType.SURVIVAL);
        MinemonCreature first=spawn(h,"charizard",new BlockPos(5,1,5)); tame(first,owner);
        MinemonCreature second=spawn(h,"blastoise",new BlockPos(7,1,5)); tame(second,owner);
        first.setInLove(owner); second.setInLove(owner);
        check(h,first.canMate(second),"Different evolution lines owned by the same player should be compatible");
        first.spawnChildFromBreeding(h.getLevel(),second);
        BlockPos eggPos=null;
        BlockPos origin=first.blockPosition();
        for(BlockPos candidate:BlockPos.betweenClosed(origin.offset(-3,0,-3),origin.offset(3,1,3)))
            if(h.getLevel().getBlockState(candidate).is(ModBlocks.BREEDING_EGG.get())){eggPos=candidate.immutable();break;}
        check(h,eggPos!=null,"One cross-species breeding event must create one physical egg instead of a baby entity");
        BlockPos eggPosition=eggPos==null?origin:eggPos;
        var blockEntity=eggPos==null?null:h.getLevel().getBlockEntity(eggPos);
        if(!(blockEntity instanceof BreedingEggBlockEntity egg)){h.fail("Physical breeding egg must have persistent incubation data");return;}
        String babySpecies=egg.species();
        check(h, babySpecies.equals("charmander")||babySpecies.equals("squirtle"),"Charizard and Blastoise must produce only their initial forms");
        check(h,owner.getUUID().equals(egg.owner()), "Incubating egg must preserve the parents' owner");
        check(h,egg.remainingTicks()==24_000,"Egg incubation must preserve the configured adult-growth duration");
        List<MinemonCreature> babies=h.getLevel().getEntitiesOfClass(MinemonCreature.class, first.getBoundingBox().inflate(8), MinemonCreature::isBaby);
        check(h,babies.isEmpty(),"Breeding must not spawn a baby Pokémon alongside the egg");
        check(h, first.getAge()==6000&&second.getAge()==6000, "Adult cooldown should default to 6000 ticks");
        check(h, !first.canMate(second), "Adults on cooldown must not breed again");
        check(h,h.getLevel().getFluidState(eggPosition).isEmpty()
                &&h.getLevel().getBlockState(eggPosition.below()).isFaceSturdy(h.getLevel(),eggPosition.below(),net.minecraft.core.Direction.UP),
                "Physical egg must be centered on a dry, supported floor position");
        egg.initialize(babySpecies,owner.getUUID(),2);
        h.runAfterDelay(4,()->{
            List<MinemonCreature> hatched=h.getLevel().getEntitiesOfClass(MinemonCreature.class,first.getBoundingBox().inflate(8),
                    pokemon->pokemon.getSpeciesSlug().equals(babySpecies));
            check(h,!h.getLevel().getBlockState(eggPosition).is(ModBlocks.BREEDING_EGG.get()),"Egg block must disappear after incubation");
            check(h,hatched.size()==1&&!hatched.get(0).isBaby()&&owner.getUUID().equals(hatched.get(0).getOwnerUUID()),
                    "Egg must hatch once into one owned adult of the selected initial species");
            h.succeed();
        });
    }

    @GameTest(template="test_arena", timeoutTicks=100)
    public static void differentOwnersAndMythicalsCannotBreed(GameTestHelper h) {
        floor(h); ServerPlayer one=player(h,new BlockPos(2,1,2),GameType.SURVIVAL); ServerPlayer two=player(h,new BlockPos(3,1,2),GameType.SURVIVAL);
        MinemonCreature owned=spawn(h,"pikachu",new BlockPos(5,1,5)); tame(owned,one); owned.setInLove(one);
        MinemonCreature otherOwner=spawn(h,"charizard",new BlockPos(6,1,5)); tame(otherOwner,two); otherOwner.setInLove(two);
        check(h, !owned.canMate(otherOwner), "Different species belonging to different owners must not breed");
        MinemonCreature mewtwo=spawn(h,"mewtwo",new BlockPos(8,1,5)); tame(mewtwo,one); mewtwo.setInLove(one);
        MinemonCreature mewtwoPartner=spawn(h,"mewtwo",new BlockPos(9,1,5)); tame(mewtwoPartner,one); mewtwoPartner.setInLove(one);
        check(h, !mewtwo.canMate(mewtwoPartner), "Mewtwo must remain excluded from breeding");
        MinemonCreature mew=spawn(h,"mew",new BlockPos(10,1,5)); tame(mew,one); mew.setInLove(one);
        MinemonCreature mewPartner=spawn(h,"mew",new BlockPos(11,1,5)); tame(mewPartner,one); mewPartner.setInLove(one);
        check(h,!mew.canMate(mewPartner),"Mew must remain excluded from breeding");
        MinemonCreature articuno=spawn(h,"articuno",new BlockPos(12,1,5)); tame(articuno,one); articuno.setInLove(one);
        MinemonCreature articunoPartner=spawn(h,"articuno",new BlockPos(13,1,5)); tame(articunoPartner,one); articunoPartner.setInLove(one);
        check(h,!articuno.canMate(articunoPartner),"Legendary birds must remain excluded from breeding");
        h.succeed();
    }
    private PokemonTreatGameTests() {}
}
