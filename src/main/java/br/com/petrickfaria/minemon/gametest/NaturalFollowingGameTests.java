package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.registry.ModEntities;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Minemon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NaturalFollowingGameTests {
    @GameTest(template="test_arena", batch="natural_following", timeoutTicks=180)
    public static void followsWithContinuousMovementThenObeysSit(GameTestHelper h) {
        h.getLevel().setDayTime(1000);
        for(int x=0;x<40;x++)for(int z=0;z<10;z++) {
            h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
            for(int y=1;y<5;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
        }
        ServerPlayer owner=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"FollowOwner"),null);
        owner.connection=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"FollowConnection")).connection;
        BlockPos end=h.absolutePos(new BlockPos(30,1,5));
        owner.moveTo(end.getX()+.5,end.getY(),end.getZ()+.5,0,0);
        h.getLevel().addNewPlayer(owner);
        MinemonCreature mob=ModEntities.SPECIES.get("bulbasaur").get().create(h.getLevel());
        BlockPos start=h.absolutePos(new BlockPos(5,1,5));
        mob.moveTo(start.getX()+.5,start.getY(),start.getZ()+.5,0,0);
        mob.setTame(true);mob.setOwnerUUID(owner.getUUID());mob.setOrderedToSit(false);
        h.getLevel().addFreshEntity(mob);
        Vec3[] previous={mob.position()},sitting={null};
        h.onEachTick(()->{
            if(mob.position().distanceTo(previous[0])>1.5)h.fail("Following relocated the Pokémon discontinuously");
            previous[0]=mob.position();
        });
        h.runAtTickTime(20,()->{if(!mob.isFollowingRun())h.fail("Distant companion did not enter running state");});
        h.runAtTickTime(80,()->{
            if(mob.getX()<start.getX()+1.5)h.fail("Companion did not walk toward owner");
            mob.setOrderedToSit(true);sitting[0]=mob.position();
        });
        h.runAtTickTime(140,()->{
            if(mob.position().distanceTo(sitting[0])>.5)h.fail("Sitting companion abandoned its order: start="+sitting[0]+" end="+mob.position()+" velocity="+mob.getDeltaMovement()+" target="+mob.getTarget());
            if(mob.isSleeping()||mob.isFollowingRun())h.fail("Sitting must remain awake without following run");
            owner.discard();mob.discard();h.succeed();
        });
    }
}
