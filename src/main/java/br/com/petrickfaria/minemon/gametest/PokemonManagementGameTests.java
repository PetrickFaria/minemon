package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.entity.PokemonEvolution;
import br.com.petrickfaria.minemon.menu.PokemonInfoMenu;
import br.com.petrickfaria.minemon.pokec.PokecPlayerData;
import br.com.petrickfaria.minemon.registry.ModEntities;
import br.com.petrickfaria.minemon.registry.ModItems;
import com.mojang.authlib.GameProfile;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@Mod.EventBusSubscriber(modid = Minemon.MOD_ID)
@GameTestHolder(Minemon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PokemonManagementGameTests {
    private static final Set<ServerPlayer> TEST_PLAYERS = new HashSet<>();

    @SubscribeEvent public static void tickPlayers(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        TEST_PLAYERS.removeIf(net.minecraft.world.entity.Entity::isRemoved);
        for (ServerPlayer player : TEST_PLAYERS) player.doTick();
    }

    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            for (int y = 1; y < 6; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        }
    }

    private static ServerPlayer player(GameTestHelper h, BlockPos pos) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "PokemonManagementTester");
        ServerPlayer player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, null);
        player.connection = new FakePlayer(h.getLevel(), profile).connection;
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(pos.getX() + .5D, pos.getY(), pos.getZ() + .5D, 0, 0);
        player.tickCount = 100;
        h.getLevel().addNewPlayer(player);
        TEST_PLAYERS.add(player);
        return player;
    }

    private static void check(GameTestHelper h, boolean condition, String message) {
        if (!condition) h.fail(message);
    }

    @GameTest(template = "test_arena", timeoutTicks = 160)
    public static void captureItemsConsumeTheInteractionBeforeThePokemonMenu(GameTestHelper h) {
        floor(h);
        ServerPlayer owner = player(h, h.absolutePos(new BlockPos(7, 1, 5)));
        ServerPlayer stranger = player(h, h.absolutePos(new BlockPos(10, 1, 5)));
        MinemonCreature pokemon = h.spawn(ModEntities.SPECIES.get("pikachu").get(), new BlockPos(5, 1, 5));
        pokemon.tame(stranger);
        owner.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.POKEBALL.get()));
        var result = pokemon.mobInteract(owner, InteractionHand.MAIN_HAND);
        check(h, result.consumesAction() && owner.containerMenu == owner.inventoryMenu,
                "a failed capture with the offhand ball must not open the Pokémon management interface");
        check(h, pokemon.isAlive() && !br.com.petrickfaria.minemon.item.PokeballItem.isOccupied(owner.getOffhandItem()),
                "a ball that cannot capture must remain empty and must not remove the Pokémon");

        owner.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.MASTER_BALL.get()));
        var protectedAttempt = pokemon.mobInteract(owner, InteractionHand.MAIN_HAND);
        check(h, protectedAttempt.consumesAction() && pokemon.isAlive()
                        && !br.com.petrickfaria.minemon.item.PokeballItem.isOccupied(owner.getOffhandItem())
                        && owner.containerMenu == owner.inventoryMenu,
                "a Master Ball must not capture another player's Pokémon or open its status menu");

        pokemon.tame(owner);
        var master = pokemon.mobInteract(owner, InteractionHand.MAIN_HAND);
        check(h, master.consumesAction() && !pokemon.isAlive()
                        && br.com.petrickfaria.minemon.item.PokeballItem.isOccupied(owner.getOffhandItem()),
                "Master Ball capture has priority over every other Pokémon interaction");
        stranger.discard();
        owner.discard();
        h.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 160)
    public static void ownedPokemonOpensItsOwnStatusMenuAndFollowSitCommandsWork(GameTestHelper h) {
        floor(h);
        ServerPlayer owner = player(h, h.absolutePos(new BlockPos(7, 1, 5)));
        MinemonCreature pokemon = h.spawn(ModEntities.SPECIES.get("bulbasaur").get(), new BlockPos(5, 1, 5));
        pokemon.tame(owner);
        var roster = br.com.petrickfaria.minemon.pokec.ActivePokemonData.get((net.minecraft.server.level.ServerLevel) owner.level);
        check(h, roster.entries(owner.getUUID()).stream().anyMatch(entry -> entry.id().equals(pokemon.getUUID())),
                "The server roster must index owned Pokémon by their stable entity UUID");
        pokemon.setOrderedToSit(true);
        pokemon.mobInteract(owner, InteractionHand.MAIN_HAND);
        check(h, owner.containerMenu instanceof PokemonInfoMenu,
                "an owned Pokémon with an empty hand must open its own information menu");
        PokemonInfoMenu menu = (PokemonInfoMenu) owner.containerMenu;
        check(h, menu.pokemon() == pokemon, "the status interface must be tied to the clicked Pokémon");
        check(h, menu.clickMenuButton(owner, 0) && !pokemon.isOrderedToSit(), "Seguir must clear the sit command");
        check(h, menu.clickMenuButton(owner, 1) && pokemon.isOrderedToSit(), "Sentar must hold the Pokémon in place");
        check(h, menu.clickMenuButton(owner, 2) && pokemon.isFreeMode() && !pokemon.isOrderedToSit(),
                "Livre must release a sitting Pokémon into natural behavior");
        check(h, roster.entries(owner.getUUID()).stream().anyMatch(entry -> entry.id().equals(pokemon.getUUID()) && entry.behavior().equals("free")),
                "The saved team manager state must update when the individual Pokémon menu changes its mode");
        var saved = pokemon.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        MinemonCreature restored = ModEntities.SPECIES.get("bulbasaur").get().create(h.getLevel());
        restored.load(saved);
        check(h, restored.isFreeMode(), "Livre mode must persist when the world is saved and reopened");
        check(h, menu.clickMenuButton(owner, 0) && !pokemon.isFreeMode(), "Seguir must leave Livre mode");
        restored.discard();
        owner.discard();
        h.succeed();
    }

    @GameTest(template="test_arena", timeoutTicks=140)
    public static void exposedZombieHitboxCanBeShotOverFenceButNotThroughSolidWall(GameTestHelper h) {
        floor(h);
        ServerPlayer owner=player(h,h.absolutePos(new BlockPos(4,1,5)));
        MinemonCreature pokemon=h.spawn(ModEntities.SPECIES.get("charmander").get(),new BlockPos(5,1,5));
        pokemon.tame(owner);pokemon.setNoAi(true);pokemon.setOrderedToSit(true);
        pokemon.setBlackListOverride(new ResourceLocation("minecraft","zombie"),true);
        h.setBlock(new BlockPos(8,1,5),Blocks.OAK_FENCE);
        Zombie baby=h.spawn(EntityType.ZOMBIE,new BlockPos(10,1,5));
        baby.setBaby(true);baby.setNoAi(true);baby.setPos(pokemon.getX()+4.0D,pokemon.getY(),pokemon.getZ());
        pokemon.focusPlayerTarget(baby);
        check(h,pokemon.canFireAt(baby),"A baby zombie with hitbox exposed above a fence must remain targetable through an actually clear ray");
        check(h,pokemon.fireElementalProjectile(baby),"The Pokémon must launch a shot at the visible part of a baby zombie's hitbox");
        var shots=h.getLevel().getEntitiesOfClass(br.com.petrickfaria.minemon.entity.ElementalProjectile.class,
                pokemon.getBoundingBox().inflate(12),shot->shot.getOwner()==pokemon);
        check(h,shots.size()==1,"The exposed-hitbox attack must create one projectile");
        float health=baby.getHealth();
        var shot=shots.get(0);for(int tick=0;tick<30&&!shot.isRemoved();tick++)shot.tick();
        check(h,baby.getHealth()<health,"A projectile aimed at an exposed baby-zombie hitbox must not be consumed by a nearby fence");

        baby.discard();
        Zombie adult=h.spawn(EntityType.ZOMBIE,new BlockPos(10,1,5));adult.setNoAi(true);
        adult.setPos(pokemon.getX()+4.0D,pokemon.getY(),pokemon.getZ());pokemon.focusPlayerTarget(adult);
        check(h,pokemon.canFireAt(adult),"An adult zombie's exposed upper hitbox must also remain targetable over a fence");
        h.setBlock(new BlockPos(8,1,5),Blocks.STONE);
        h.setBlock(new BlockPos(8,2,5),Blocks.STONE);h.setBlock(new BlockPos(8,3,5),Blocks.STONE);
        check(h,!pokemon.canFireAt(adult),"A solid wall that covers every target ray must continue to block attacks");
        owner.discard();pokemon.discard();adult.discard();h.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 180)
    public static void companionXPUsesMobRewardAndIsGrantedOnlyOnce(GameTestHelper h) {
        floor(h);
        ServerPlayer owner = player(h, h.absolutePos(new BlockPos(7, 1, 5)));
        MinemonCreature pokemon = h.spawn(ModEntities.SPECIES.get("bulbasaur").get(), new BlockPos(5, 1, 5));
        pokemon.tame(owner);
        Zombie zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(9, 1, 5));
        int reward = zombie.getExperienceReward();
        LivingDeathEvent death = new LivingDeathEvent(zombie, DamageSource.mobAttack(pokemon));
        br.com.petrickfaria.minemon.pokec.PokecEvents.companionExperience(death);
        int afterLevel = pokemon.getPokemonLevel(), afterXp = pokemon.getPokemonExperience();
        check(h, reward > 0 && (afterLevel > 1 || afterXp > 0), "the responsible domesticated Pokémon must receive the defeated mob's XP");
        br.com.petrickfaria.minemon.pokec.PokecEvents.companionExperience(death);
        check(h, pokemon.getPokemonLevel() == afterLevel && pokemon.getPokemonExperience() == afterXp,
                "a single entity death must not award companion XP twice");

        MinemonCreature otherCompanion = h.spawn(ModEntities.SPECIES.get("charmander").get(), new BlockPos(4, 1, 5));
        otherCompanion.tame(owner);
        MinemonCreature defeatedWild = h.spawn(ModEntities.SPECIES.get("rattata").get(), new BlockPos(10, 1, 5));
        br.com.petrickfaria.minemon.entity.ElementalProjectile shot =
                new br.com.petrickfaria.minemon.entity.ElementalProjectile(pokemon, defeatedWild);
        int projectileRewardHalfUnits = br.com.petrickfaria.minemon.entity.PokemonProgression
                .xpRewardHalfUnitsForWildRarity(defeatedWild.getPokemonRarity(), 5);
        br.com.petrickfaria.minemon.pokec.PokecEvents.companionExperience(
                new LivingDeathEvent(defeatedWild, DamageSource.indirectMobAttack(shot, pokemon)));
        check(h, pokemon.getPokemonExperienceHalfUnits() == afterXp * 2 + projectileRewardHalfUnits
                        && otherCompanion.getPokemonExperienceHalfUnits() == 0,
                "a final elemental projectile must credit exactly its owning Pokémon for wild Pokémon XP");
        otherCompanion.discard(); defeatedWild.discard();

        pokemon.setPokemonProgress(1, 4);
        var saved = pokemon.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        MinemonCreature restored = ModEntities.SPECIES.get("bulbasaur").get().create(h.getLevel());
        restored.load(saved);
        check(h, restored.getPokemonLevel() == 1 && restored.getPokemonExperience() == 4,
                "companion level progress must survive entity save and load");
        owner.discard();
        h.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 160)
    public static void companionXPUsesLastValidHitAndTenPercentParticipationShares(GameTestHelper h) {
        floor(h);
        ServerPlayer owner = player(h, h.absolutePos(new BlockPos(7, 1, 5)));
        MinemonCreature last = h.spawn(ModEntities.SPECIES.get("charmander").get(), new BlockPos(4, 1, 5)); last.tame(owner);
        MinemonCreature secondaryA = h.spawn(ModEntities.SPECIES.get("bulbasaur").get(), new BlockPos(5, 1, 5)); secondaryA.tame(owner);
        MinemonCreature secondaryB = h.spawn(ModEntities.SPECIES.get("squirtle").get(), new BlockPos(6, 1, 5)); secondaryB.tame(owner);
        Zombie zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(9, 1, 5));
        int base = zombie.getExperienceReward() * 2;
        br.com.petrickfaria.minemon.pokec.WildEncounterManager.recordCompanionHit(zombie, secondaryA);
        br.com.petrickfaria.minemon.pokec.WildEncounterManager.recordCompanionHit(zombie, secondaryB);
        br.com.petrickfaria.minemon.pokec.WildEncounterManager.recordCompanionHit(zombie, last);
        br.com.petrickfaria.minemon.pokec.WildEncounterManager.awardCompanionExperience(zombie, DamageSource.FALL);
        check(h, last.getPokemonExperienceHalfUnits() == base, "The latest valid companion hitter must receive 100% XP after environmental death");
        check(h, secondaryA.getPokemonExperienceHalfUnits() == base / 10 && secondaryB.getPokemonExperienceHalfUnits() == base / 10,
                "Each other valid participant must receive an individual 10% share");
        owner.discard(); h.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 220)
    public static void wildPokemonHalfXpAccumulatesAndPersistsAcrossEntityReload(GameTestHelper h) {
        floor(h);
        ServerPlayer owner = player(h, h.absolutePos(new BlockPos(7, 1, 5)));
        MinemonCreature attacker = h.spawn(ModEntities.SPECIES.get("rattata").get(), new BlockPos(5, 1, 5));
        attacker.tame(owner);
        MinemonCreature first = h.spawn(ModEntities.SPECIES.get("rattata").get(), new BlockPos(9, 1, 5));
        MinemonCreature second = h.spawn(ModEntities.SPECIES.get("rattata").get(), new BlockPos(10, 1, 5));
        br.com.petrickfaria.minemon.pokec.WildEncounterManager.awardCompanionExperience(first, DamageSource.mobAttack(attacker));
        check(h, attacker.getPokemonExperienceHalfUnits() == 5 && attacker.getPokemonLevel() == 1,
                "a common wild Pokémon must award exactly 2.5 XP without rounding or an early level-up");
        var saved = attacker.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        attacker.discard(); // A reload replaces the old UUID owner; two live copies make the lookup ambiguous.
        MinemonCreature restored = ModEntities.SPECIES.get("rattata").get().create(h.getLevel());
        restored.load(saved);
        BlockPos restoredPos = h.absolutePos(new BlockPos(12, 1, 5));
        restored.moveTo(restoredPos.getX() + 0.5D, restoredPos.getY(), restoredPos.getZ() + 0.5D, 0.0F, 0.0F);
        check(h, h.getLevel().addFreshEntity(restored), "first reloaded companion should enter the server entity index");
        var savedAgain = restored.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        restored.discard();
        MinemonCreature reopened = ModEntities.SPECIES.get("rattata").get().create(h.getLevel());
        reopened.load(savedAgain);
        BlockPos reopenedPos = h.absolutePos(new BlockPos(13, 1, 5));
        reopened.moveTo(reopenedPos.getX() + 0.5D, reopenedPos.getY(), reopenedPos.getZ() + 0.5D, 0.0F, 0.0F);
        check(h, h.getLevel().addFreshEntity(reopened), "second reloaded companion should enter the server entity index");
        br.com.petrickfaria.minemon.pokec.WildEncounterManager.awardCompanionExperience(second, DamageSource.mobAttack(reopened));
        check(h, reopened.getPokemonExperienceHalfUnits() == 10 && reopened.getPokemonExperienceExact() == 5.0D,
                "half XP must survive saving and a second common defeat must total exactly 5 XP");
        reopened.awardPokemonExperienceHalfUnits(460);
        check(h, reopened.getPokemonLevel() == 5 && reopened.getPokemonExperienceExact() == 5.0D,
                "a large reward must pass through every affordable level and preserve its remainder");
        attacker.discard(); restored.discard(); reopened.discard(); first.discard(); second.discard(); owner.discard();
        h.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 80)
    public static void companionRangeScalesLinearlyAndPhysicalAttacksAreDisabled(GameTestHelper h) {
        floor(h);
        ServerPlayer owner = player(h, h.absolutePos(new BlockPos(7, 1, 5)));
        MinemonCreature pokemon = h.spawn(ModEntities.SPECIES.get("bulbasaur").get(), new BlockPos(5, 1, 5));
        pokemon.tame(owner);
        net.minecraft.world.entity.animal.Cow rangeTarget=h.spawn(EntityType.COW,new BlockPos(10,1,5));
        pokemon.setBlackListOverride(new net.minecraft.resources.ResourceLocation("minecraft","cow"),true);
        // The upper range cases extend beyond this compact structure. Clear a
        // narrow, level shot lane so this assertion measures projectile range,
        // rather than the arena's perimeter wall or ceiling.
        int laneY=new BlockPos(pokemon.getX(),pokemon.getY(),pokemon.getZ()).getY();
        int laneZ=new BlockPos(pokemon.getX(),pokemon.getY(),pokemon.getZ()).getZ();
        for(int x=new BlockPos(pokemon.getX(),0,pokemon.getZ()).getX()+1;
                x<=new BlockPos(pokemon.getX()+26.0D,0,pokemon.getZ()).getX();x++)
            for(int y=laneY;y<=laneY+3;y++)
                h.getLevel().setBlockAndUpdate(new BlockPos(x,y,laneZ),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        for(int level=1;level<=10;level++){
            pokemon.setPokemonProgress(level,0);
            double expected=5.0D+(level-1)*2.0D;
            check(h,Math.abs(pokemon.attackRange()-expected)<1.0E-6D,
                    "Companion projectile range must follow the linear level progression at level "+level);
            double targetY=pokemon.getY();
            rangeTarget.setPos(pokemon.getX()+expected+0.4D,targetY,pokemon.getZ());
            check(h,pokemon.canAttack(rangeTarget)&&pokemon.canFireAt(rangeTarget),
                    "An authorized target inside the level "+level+" projectile range must be hittable"
                            +" [canAttack="+pokemon.canAttack(rangeTarget)+", canFireAt="+pokemon.canFireAt(rangeTarget)
                            +", range="+expected+", distance="+pokemon.distanceTo(rangeTarget)
                            +", level="+pokemon.getPokemonLevel()+", targetPos="+rangeTarget.position()
                            +", mobTarget="+pokemon.getTarget()+"]");
            rangeTarget.setPos(pokemon.getX()+expected+0.8D,targetY,pokemon.getZ());
            check(h,pokemon.canAttack(rangeTarget)&&!pokemon.canFireAt(rangeTarget),
                    "A perceived target beyond the level "+level+" projectile range must remain authorized but not be hittable");
            double perception=level>=6?25.0D:15.0D;
            check(h,Math.abs(pokemon.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE)-perception)<1.0E-6D,
                    "FOLLOW_RANGE must match level-based perception at level "+level);
            double centeredY=pokemon.getY()+(pokemon.getBbHeight()-rangeTarget.getBbHeight())/2.0D;
            rangeTarget.setPos(pokemon.getX()+perception-0.1D,centeredY,pokemon.getZ());
            check(h,pokemon.canPerceive(rangeTarget),"Perception must include targets inside the level "+level+" limit");
            // Center is beyond the configured radius, but the real cow hitbox
            // still reaches inside it and remains perceptible.
            rangeTarget.setPos(pokemon.getX()+perception+0.4D,centeredY,pokemon.getZ());
            check(h,pokemon.canPerceive(rangeTarget),"Perception must include a target whose hitbox remains inside the level "+level+" limit");
            rangeTarget.setPos(pokemon.getX()+perception+0.8D,centeredY,pokemon.getZ());
            check(h,!pokemon.canPerceive(rangeTarget),"Perception must exclude targets beyond the level "+level+" limit");
        }
        pokemon.setPokemonProgress(5,0);
        check(h,Math.abs(pokemon.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE)-15.0D)<1.0E-6D,
                "Perception must remain 15 at level 5");
        pokemon.setPokemonProgress(6,0);
        check(h,Math.abs(pokemon.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE)-25.0D)<1.0E-6D,
                "Perception must update immediately when leveling from 5 to 6");
        pokemon.setPokemonProgress(10,0);
        check(h,Math.abs(pokemon.attackRange()-23.0D)<1.0E-6D,"Level 10 attack range must be 23 blocks");
        pokemon.setPokemonProgress(11,0);
        check(h,Math.abs(pokemon.attackRange()-23.0D)<1.0E-6D,"Attack range must remain capped at 23 blocks after level 10");
        MinemonCreature wild=h.spawn(ModEntities.SPECIES.get("pikachu").get(),new BlockPos(11,1,11));
        check(h,Math.abs(wild.attackRange()-10.0D)<1.0E-6D,"All wild Pokémon must use 10-block projectile range");
        net.minecraft.world.entity.animal.Cow contact=h.spawn(EntityType.COW,new BlockPos(5,1,5));
        check(h,!pokemon.doHurtTarget(contact),"Pokémon must never deal melee damage, even on direct contact");
        pokemon.discard();wild.discard();rangeTarget.discard();contact.discard();owner.discard();h.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 100)
    public static void seatedLevelOneCharmanderCanFireThroughItsFifthBlockOnly(GameTestHelper h) {
        floor(h);
        ServerPlayer owner = player(h, h.absolutePos(new BlockPos(7, 1, 5)));
        MinemonCreature pokemon = h.spawn(ModEntities.SPECIES.get("charmander").get(), new BlockPos(5, 1, 5));
        pokemon.tame(owner);
        pokemon.setPokemonLevel(1);
        pokemon.setOrderedToSit(true);
        pokemon.setBlackListOverride(new net.minecraft.resources.ResourceLocation("minecraft", "zombie"), true);
        Zombie zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(10, 1, 5));
        zombie.setNoAi(true);
        float initialHealth=zombie.getHealth();
        for(int block=1;block<=5;block++){
            zombie.setPos(pokemon.getX()+block,pokemon.getY(),pokemon.getZ());
            check(h, pokemon.isOrderedToSit() && pokemon.canAttack(zombie) && pokemon.canFireAt(zombie),
                    "A seated level 1 Charmander must be able to fire at an authorized zombie in block "+block
                            +" [origin="+pokemon.position()+", target="+zombie.position()+"]");
        }
        check(h,pokemon.fireElementalProjectile(zombie),"A seated level 1 Charmander must launch its shot in the fifth block");
        h.runAfterDelay(20,()->{
            check(h,zombie.getHealth()<initialHealth,"The fifth-block projectile must reach and damage the zombie");
            zombie.setPos(pokemon.getX()+6.0D,pokemon.getY(),pokemon.getZ());
            check(h,!pokemon.canFireAt(zombie),"The same level 1 Charmander must not fire at a zombie in the sixth block");
            owner.discard(); pokemon.discard(); zombie.discard(); h.succeed();
        });
    }

    @GameTest(template = "test_arena", timeoutTicks = 760)
    public static void focusedTargetCanLeaveAndReenterPerceptionBeforeTheFifteenSecondTimeout(GameTestHelper h) {
        floor(h);
        ServerPlayer owner = player(h, h.absolutePos(new BlockPos(7, 1, 5)));
        // This test measures focus expiry, not trainer damage. A stray hostile
        // from a neighboring arena must not replace its focus via trainer defense.
        owner.setGameMode(GameType.CREATIVE);
        MinemonCreature pokemon = h.spawn(ModEntities.SPECIES.get("pikachu").get(), new BlockPos(5, 1, 5));
        pokemon.tame(owner);
        pokemon.setNoAi(true);
        Cow target = h.spawn(EntityType.COW, new BlockPos(9, 1, 5));
        target.setNoAi(true);
        pokemon.focusPlayerTarget(target);
        target.setPos(pokemon.getX() + 15.1D, pokemon.getY(), pokemon.getZ());
        h.runAfterDelay(299, () -> {
            check(h, pokemon.getTarget() == target, "A valid target must stay focused for the first 299 ticks outside perception; target="
                    + pokemon.getTarget() + " canAttack=" + pokemon.canAttack(target) + " canPerceive=" + pokemon.canPerceive(target)
                    + " alive=" + target.isAlive() + " distance=" + pokemon.distanceTo(target) + " noAI=" + pokemon.isNoAi()
                    + " sleeping=" + pokemon.isSleeping() + " owner=" + pokemon.getOwner());
            target.setPos(pokemon.getX() + 14.9D, pokemon.getY(), pokemon.getZ());
            h.runAfterDelay(2, () -> {
                check(h, pokemon.getTarget() == target, "Reentering perception must preserve and reset the focus timeout");
                target.setPos(pokemon.getX() + 15.1D, pokemon.getY(), pokemon.getZ());
                h.runAfterDelay(299, () -> {
                    check(h, pokemon.getTarget() == target, "The second continuous out-of-range interval must also allow 299 ticks");
                    h.runAfterDelay(1, () -> {
                        check(h, pokemon.getTarget() == null, "The focus must clear after 300 continuous ticks outside perception");
                        target.discard(); pokemon.discard(); owner.discard(); h.succeed();
                    });
                });
            });
        });
    }

    @GameTest(template = "test_arena", timeoutTicks = 480)
    public static void wildPokemonDoesNotAbandonACombatWithADomesticatedPokemon(GameTestHelper h) {
        floor(h);
        ServerPlayer owner = player(h, h.absolutePos(new BlockPos(7, 1, 5)));
        MinemonCreature companion = h.spawn(ModEntities.SPECIES.get("pikachu").get(), new BlockPos(5, 1, 5));
        companion.tame(owner);
        companion.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200.0D);
        companion.setHealth(200.0F);
        MinemonCreature wild = h.spawn(ModEntities.SPECIES.get("rattata").get(), new BlockPos(10, 1, 5));
        check(h, wild.hurt(DamageSource.mobAttack(companion), 1.0F), "The companion must be able to provoke a wild Pokémon");
        h.runAfterDelay(420, () -> {
            check(h, wild.getTarget() == companion,
                    "A wild Pokémon must keep its domesticated opponent focused beyond the former aggression timeout");
            wild.discard(); companion.discard(); owner.discard(); h.succeed();
        });
    }

    @GameTest(template = "test_arena", timeoutTicks = 220)
    public static void sittingPokemonRetaliatesOnlyWithAuthorizationAndStaysAtItsAnchor(GameTestHelper h) {
        floor(h);
        ServerPlayer owner = player(h, h.absolutePos(new BlockPos(7, 1, 5)));
        MinemonCreature pokemon = h.spawn(ModEntities.SPECIES.get("pikachu").get(), new BlockPos(5, 1, 5));
        pokemon.tame(owner);
        pokemon.setOrderedToSit(true);
        net.minecraft.world.entity.animal.Cow target = h.spawn(EntityType.COW, new BlockPos(10, 1, 5));
        target.setNoAi(true);
        target.setHealth(200.0F);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200.0D);
        target.setHealth(200.0F);
        var anchor = pokemon.position();
        pokemon.setTarget(target);
        h.runAfterDelay(8, () -> {
            check(h, target.getHealth() == 200.0F, "a nearby mob must not be attacked before trainer authorization");
            br.com.petrickfaria.minemon.pokec.PokecEvents.playerPokemonAttack(
                    new net.minecraftforge.event.entity.living.LivingAttackEvent(target, DamageSource.playerAttack(owner), 1.0F));
            h.runAfterDelay(100, () -> {
                check(h, target.getHealth() < 200.0F, "a seated Pokémon must attack an authorized target within ranged reach");
                check(h, pokemon.isOrderedToSit() && pokemon.position().distanceToSqr(anchor) < 1.0E-6D,
                        "the seated Pokémon must remain at its anchor while attacking");
                pokemon.discard(); target.discard(); owner.discard();
                h.succeed();
            });
        });
    }

    @GameTest(template = "test_arena", timeoutTicks = 140)
    public static void sittingPokemonUsesOnlyRangedAttacksWithoutSteppingForward(GameTestHelper h) {
        floor(h);
        ServerPlayer owner = player(h, h.absolutePos(new BlockPos(7, 1, 5)));
        MinemonCreature pokemon = h.spawn(ModEntities.SPECIES.get("rattata").get(), new BlockPos(5, 1, 5));
        pokemon.tame(owner);
        pokemon.setOrderedToSit(true);
        net.minecraft.world.entity.animal.Cow target = h.spawn(EntityType.COW, new BlockPos(10, 1, 5));
        target.setNoAi(true);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200.0D);
        target.setHealth(200.0F);
        pokemon.focusPlayerTarget(target);
        var anchor = pokemon.position();
        h.runAfterDelay(80, () -> {
            check(h, target.getHealth() < 200.0F, "a seated Pokémon must use a ranged attack at safe distance even with previous charge slots depleted");
            check(h, pokemon.isOrderedToSit() && pokemon.position().distanceToSqr(anchor) < 1.0E-6D,
                    "a seated ranged attacker must remain at its original position");
            pokemon.discard(); target.discard(); owner.discard();
            h.succeed();
        });
    }

    @GameTest(template = "test_arena", timeoutTicks = 220)
    public static void evolutionPreservesOwnerXPAndBlacklistAndUnlocksTheNextSpecies(GameTestHelper h) {
        floor(h);
        ServerPlayer owner = player(h, h.absolutePos(new BlockPos(7, 1, 5)));
        MinemonCreature charmander = h.spawn(ModEntities.SPECIES.get("charmander").get(), new BlockPos(5, 1, 5));
        charmander.tame(owner);
        charmander.setPokemonProgress(5, 7);
        ResourceLocation zombieId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(EntityType.ZOMBIE);
        charmander.setBlackListOverride(zombieId, false);
        MinemonCreature charmeleon = PokemonEvolution.evolve(owner, charmander, "charmeleon");
        check(h, charmeleon != null && !charmander.isAlive(), "a valid manual evolution must replace the source Pokémon once");
        check(h, charmeleon.getSpeciesSlug().equals("charmeleon") && charmeleon.getPokemonLevel() == 5
                        && charmeleon.getPokemonExperience() == 7 && owner.getUUID().equals(charmeleon.getOwnerUUID()),
                "evolution must preserve the selected form, level, XP, and owner");
        check(h, Boolean.FALSE.equals(charmeleon.getBlackListOverrides().get(zombieId.toString())),
                "the evolved Pokémon must retain its individual Black List settings");
        check(h, PokecPlayerData.isUnlocked(owner, "charmeleon"), "evolution must unlock the next PokéC species for its owner");
        check(h, PokemonEvolution.evolve(owner, charmeleon, "charizard") == null,
                "the next evolution must remain unavailable before its level requirement");
        owner.discard();
        h.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 220)
    public static void blacklistDefaultsToHostilesHonorsChangesAndPreservesAllyProtections(GameTestHelper h) {
        floor(h);
        ServerPlayer owner = player(h, h.absolutePos(new BlockPos(7, 1, 5)));
        MinemonCreature pokemon = h.spawn(ModEntities.SPECIES.get("pikachu").get(), new BlockPos(5, 1, 5));
        pokemon.tame(owner);
        pokemon.setOrderedToSit(false);
        Zombie zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(9, 1, 5));
        Cow cow = h.spawn(EntityType.COW, new BlockPos(5, 1, 8));
        zombie.setNoAi(true);
        cow.setNoAi(true);
        check(h, pokemon.isBlackListTarget(zombie), "registered hostile mobs must be selected by default");
        check(h, !pokemon.isBlackListTarget(cow), "neutral/passive mobs must be unselected by default");
        var zombieId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(EntityType.ZOMBIE);
        var cowId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(EntityType.COW);
        pokemon.setBlackListOverride(zombieId, false);
        pokemon.setBlackListOverride(cowId, true);
        check(h, !pokemon.isBlackListTarget(zombie) && pokemon.isBlackListTarget(cow),
                "the owner must be able to remove a hostile and select a passive target");
        check(h, !pokemon.canAttack(owner), "a Pokémon must never attack its owner");
        MinemonCreature ally = h.spawn(ModEntities.SPECIES.get("bulbasaur").get(), new BlockPos(6, 1, 6));
        ally.tame(owner);
        pokemon.setBlackListOverride(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(ally.getType()), true);
        check(h, !pokemon.isBlackListTarget(ally), "same-owner Pokémon must remain protected even when explicitly selected");
        var saved = pokemon.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        MinemonCreature restored = ModEntities.SPECIES.get("pikachu").get().create(h.getLevel());
        restored.load(saved);
        check(h, Boolean.FALSE.equals(restored.getBlackListOverrides().get(zombieId.toString()))
                        && Boolean.TRUE.equals(restored.getBlackListOverrides().get(cowId.toString())),
                "Black List changes must persist with this individual Pokémon");
        owner.discard();
        h.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 200)
    public static void blacklistMakesSelectedEntitiesPreventiveCombatTargets(GameTestHelper h) {
        floor(h);
        ServerPlayer owner = player(h, h.absolutePos(new BlockPos(7, 1, 5)));
        MinemonCreature pokemon = h.spawn(ModEntities.SPECIES.get("pikachu").get(), new BlockPos(5, 1, 5));
        pokemon.tame(owner);
        pokemon.setOrderedToSit(false);
        Zombie zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(9, 1, 5));
        zombie.setNoAi(true);
        h.runAfterDelay(40, () -> {
            check(h, pokemon.getTarget() == zombie || zombie.getHealth() < zombie.getMaxHealth(),
                    "a selected hostile must be targeted and attacked without first harming the Pokémon");
            pokemon.setOrderedToSit(true);
            zombie.setPos(15.5D, 1.0D, 5.5D);
            check(h, pokemon.isOrderedToSit() && !pokemon.canFireAt(zombie),
                    "a sitting Pokémon must not chase a Black List target outside its projectile range");
            owner.discard();
            h.succeed();
        });
    }

    @GameTest(template = "test_arena", timeoutTicks = 180)
    public static void chanseyHealsOnlyTheMostInjuredOwnedPokemonAndUsesItsLevelCooldown(GameTestHelper h) {
        floor(h);
        ServerPlayer owner = player(h, h.absolutePos(new BlockPos(7, 1, 5)));
        MinemonCreature chansey = h.spawn(ModEntities.SPECIES.get("chansey").get(), new BlockPos(5, 1, 5));
        chansey.tame(owner);
        MinemonCreature lightlyHurt = h.spawn(ModEntities.SPECIES.get("pikachu").get(), new BlockPos(7, 1, 5));
        MinemonCreature badlyHurt = h.spawn(ModEntities.SPECIES.get("bulbasaur").get(), new BlockPos(6, 1, 6));
        lightlyHurt.tame(owner); badlyHurt.tame(owner);
        lightlyHurt.setHealth(lightlyHurt.getMaxHealth() * .5F);
        badlyHurt.setHealth(badlyHurt.getMaxHealth() * .25F);
        chansey.tryChanseyHealForValidation();
        check(h, badlyHurt.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION)
                        && !lightlyHurt.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),
                "Chansey must heal only the party member with the lowest health ratio");
        check(h, chansey.chanseyCooldownTicksForValidation() == 6000,
                "level 1 Chansey must start a five-minute server-tick cooldown after successful healing");

        MinemonCreature otherSpecies = h.spawn(ModEntities.SPECIES.get("clefairy").get(), new BlockPos(8, 1, 5));
        otherSpecies.tame(owner); otherSpecies.setOrderedToSit(false); otherSpecies.setPokemonLevel(10);
        MinemonCreature waiting = h.spawn(ModEntities.SPECIES.get("squirtle").get(), new BlockPos(9, 1, 5));
        waiting.tame(owner); waiting.setHealth(1.0F);
        otherSpecies.tryChanseyHealForValidation();
        check(h, !waiting.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),
                "the automatic regeneration ability must remain exclusive to Chansey");
        owner.discard();
        h.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 160)
    public static void wildPokemonRemainNeutralUntilAttacked(GameTestHelper h) {
        floor(h);
        ServerPlayer player = player(h, h.absolutePos(new BlockPos(7, 1, 5)));
        MinemonCreature wild = h.spawn(ModEntities.SPECIES.get("bulbasaur").get(), new BlockPos(5, 1, 5));
        h.runAfterDelay(35, () -> {
            check(h, wild.getTarget() == null, "wild Pokémon must not acquire a nearby player as an unprovoked target");
            float health=wild.getHealth();
            check(h, wild.hurt(DamageSource.playerAttack(player), 9.0F), "a direct player hit must reach a wild Pokémon");
            h.runAfterDelay(2, () -> {
                check(h, wild.getHealth()==health-1.0F&&wild.getTarget()==null,
                        "direct player hits must deal one HP and must not provoke a wild Pokémon");
                player.discard();
                h.succeed();
            });
        });
    }
}
