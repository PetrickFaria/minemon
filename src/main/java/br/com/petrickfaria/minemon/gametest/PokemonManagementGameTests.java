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
        pokemon.setOrderedToSit(true);
        pokemon.mobInteract(owner, InteractionHand.MAIN_HAND);
        check(h, owner.containerMenu instanceof PokemonInfoMenu,
                "an owned Pokémon with an empty hand must open its own information menu");
        PokemonInfoMenu menu = (PokemonInfoMenu) owner.containerMenu;
        check(h, menu.pokemon() == pokemon, "the status interface must be tied to the clicked Pokémon");
        check(h, menu.clickMenuButton(owner, 0) && !pokemon.isOrderedToSit(), "Seguir must clear the sit command");
        check(h, menu.clickMenuButton(owner, 1) && pokemon.isOrderedToSit(), "Sentar must hold the Pokémon in place");
        owner.discard();
        h.succeed();
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
        MinemonCreature restored = ModEntities.SPECIES.get("rattata").get().create(h.getLevel());
        restored.load(saved);
        var savedAgain = restored.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        MinemonCreature reopened = ModEntities.SPECIES.get("rattata").get().create(h.getLevel());
        reopened.load(savedAgain);
        br.com.petrickfaria.minemon.pokec.WildEncounterManager.awardCompanionExperience(second, DamageSource.mobAttack(reopened));
        check(h, reopened.getPokemonExperienceHalfUnits() == 10 && reopened.getPokemonExperienceExact() == 5.0D,
                "half XP must survive saving and a second common defeat must total exactly 5 XP");
        reopened.awardPokemonExperienceHalfUnits(460);
        check(h, reopened.getPokemonLevel() == 5 && reopened.getPokemonExperienceExact() == 5.0D,
                "a large reward must pass through every affordable level and preserve its remainder");
        attacker.discard(); restored.discard(); reopened.discard(); first.discard(); second.discard(); owner.discard();
        h.succeed();
    }

    @GameTest(template = "test_arena", timeoutTicks = 220)
    public static void sittingPokemonRetaliatesOnlyWithAuthorizationAndStaysAtItsAnchor(GameTestHelper h) {
        floor(h);
        ServerPlayer owner = player(h, h.absolutePos(new BlockPos(7, 1, 5)));
        MinemonCreature pokemon = h.spawn(ModEntities.SPECIES.get("pikachu").get(), new BlockPos(5, 1, 5));
        pokemon.tame(owner);
        pokemon.setOrderedToSit(true);
        net.minecraft.world.entity.monster.Zombie target = h.spawn(EntityType.ZOMBIE, new BlockPos(10, 1, 5));
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
    public static void sittingPokemonCanMeleeWithinReachWithoutSteppingForward(GameTestHelper h) {
        floor(h);
        ServerPlayer owner = player(h, h.absolutePos(new BlockPos(7, 1, 5)));
        MinemonCreature pokemon = h.spawn(ModEntities.SPECIES.get("rattata").get(), new BlockPos(5, 1, 5));
        pokemon.tame(owner);
        for (int i = 0; i < br.com.petrickfaria.minemon.entity.CombatCharges.CAPACITY; i++) pokemon.rangedCharges().consume();
        pokemon.setOrderedToSit(true);
        net.minecraft.world.entity.animal.Cow target = h.spawn(EntityType.COW, new BlockPos(5, 1, 5));
        target.setNoAi(true);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200.0D);
        target.setHealth(200.0F);
        pokemon.focusPlayerTarget(target);
        var anchor = pokemon.position();
        h.runAfterDelay(80, () -> {
            check(h, target.getHealth() < 200.0F, "a seated Pokémon must use melee attacks against a target already in reach");
            check(h, pokemon.isOrderedToSit() && pokemon.position().distanceToSqr(anchor) < 1.0E-6D,
                    "a seated melee attacker must remain at its original position");
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
            check(h, !pokemon.canAttack(zombie), "a sitting Pokémon must not chase a distant Black List target");
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
