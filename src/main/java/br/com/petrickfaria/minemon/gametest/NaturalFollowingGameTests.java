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
    private static void fail(GameTestHelper h,ServerPlayer owner,MinemonCreature mob,String message) {
        owner.discard(); mob.discard(); h.fail(message);
    }
    @GameTest(template="test_arena", batch="natural_following", timeoutTicks=500)
    public static void followsWithContinuousMovementThenObeysSit(GameTestHelper h) {
        h.getLevel().setDayTime(1000);
        var arenaBox=new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(0,0,0)),h.absolutePos(new BlockPos(40,14,10)));
        h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,arenaBox).forEach(net.minecraft.world.entity.Entity::discard);
        for(int x=0;x<40;x++)for(int z=0;z<10;z++) {
            h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
            for(int y=1;y<=12;y++)h.setBlock(new BlockPos(x,y,z),
                    x==0||x==39||z==0||z==9?Blocks.STONE:Blocks.AIR);
        }
        ServerPlayer owner=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"FollowOwner"),null);
        owner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
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
            if(mob.position().distanceTo(previous[0])>1.5)fail(h,owner,mob,"Following relocated the Pokémon discontinuously");
            previous[0]=mob.position();
        });
        h.runAtTickTime(20,()->{if(!mob.isFollowingRun())fail(h,owner,mob,"Distant companion did not enter running state: pos="+mob.position()+" owner="+owner.position()+" water="+mob.isInWater()+" sleep="+mob.isSleeping()+" target="+mob.getTarget());});
        h.runAtTickTime(80,()->{
            if(mob.getX()<start.getX()+1.5)fail(h,owner,mob,"Companion did not walk toward owner");
            mob.setOrderedToSit(true);sitting[0]=mob.position();
        });
        h.runAtTickTime(140,()->{
            if(mob.position().distanceTo(sitting[0])>.5)fail(h,owner,mob,"Sitting companion abandoned its order: start="+sitting[0]+" end="+mob.position()+" velocity="+mob.getDeltaMovement()+" target="+mob.getTarget());
            if(mob.isSleeping()||mob.isFollowingRun())fail(h,owner,mob,"Sitting must remain awake without following run");
            owner.discard();mob.discard();NineBehaviorChecks.followingAltitudes(h);
        });
    }

    @GameTest(template="test_arena", batch="natural_following", timeoutTicks=180)
    public static void explicitFollowWakesPokemonAndOverridesNightSleep(GameTestHelper h) {
        h.getLevel().setDayTime(13000L);
        ServerPlayer owner=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),
                new GameProfile(UUID.randomUUID(),"NightFollowOwner"),null);
        owner.connection=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"NightFollowConnection")).connection;
        BlockPos ownerPos=h.absolutePos(new BlockPos(10,1,5));
        owner.moveTo(ownerPos.getX()+.5D,ownerPos.getY(),ownerPos.getZ()+.5D,0,0);
        h.getLevel().addNewPlayer(owner);
        MinemonCreature mob=ModEntities.SPECIES.get("bulbasaur").get().create(h.getLevel());
        BlockPos start=h.absolutePos(new BlockPos(5,1,5));
        mob.moveTo(start.getX()+.5D,start.getY(),start.getZ()+.5D,0,0);
        mob.tame(owner);mob.setOwnerUUID(owner.getUUID());mob.setSleeping(true);
        h.getLevel().addFreshEntity(mob);
        mob.applyManagedBehavior("follow");
        if(mob.isSleeping()||!mob.isFollowingOwner())fail(h,owner,mob,"Explicit follow must wake a sleeping Pokémon and activate following");
        double initial=mob.distanceTo(owner);
        owner.moveTo(owner.getX()+4.0D,owner.getY(),owner.getZ(),owner.getYRot(),owner.getXRot());
        h.runAfterDelay(70,()->{
            if(mob.isSleeping()||!mob.isFollowingOwner()||mob.distanceTo(owner)>=initial)
                fail(h,owner,mob,"Following must remain active through natural bedtime and move toward the owner; initial="+initial+" current="+mob.distanceTo(owner)
                        +" mob="+mob.position()+" owner="+owner.position()+" ownerAlive="+owner.isAlive()+" ownerGround="+owner.onGround()
                        +" sleeping="+mob.isSleeping()+" nightRest="+mob.isNightResting()+" following="+mob.isFollowingOwner()
                        +" navDone="+mob.getNavigation().isDone()+" navTarget="+mob.getNavigation().getTargetPos()+" target="+mob.getTarget());
            var roster=br.com.petrickfaria.minemon.pokec.ActivePokemonData.get((net.minecraft.server.level.ServerLevel)owner.level);
            if(roster.entries(owner.getUUID()).stream().noneMatch(e->e.id().equals(mob.getUUID())&&e.behavior().equals("follow")))
                fail(h,owner,mob,"The explicit follow mode must be saved in the owner's roster");
            mob.setSleeping(true);
            br.com.petrickfaria.minemon.pokec.ActivePokemonData.register(mob);
            if(mob.isSleeping()||!mob.isFollowingOwner())
                fail(h,owner,mob,"Restoring the saved follow mode must wake the Pokémon even when its sleep flag was saved");
            owner.discard();mob.discard();h.succeed();
        });
    }
}
