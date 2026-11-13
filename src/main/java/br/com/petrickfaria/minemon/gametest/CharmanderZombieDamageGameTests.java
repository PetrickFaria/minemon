package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.registry.ModEntities;
import com.mojang.authlib.GameProfile;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@Mod.EventBusSubscriber(modid = Minemon.MOD_ID)
@GameTestHolder("minemon_damage")
@PrefixGameTestTemplate(false)
public final class CharmanderZombieDamageGameTests {
    private static final Set<ServerPlayer> TEST_PLAYERS = new HashSet<>();

    @SubscribeEvent
    public static void tickPlayers(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
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

    @GameTest(template = "test_arena", templateNamespace = "minemon_damage", timeoutTicks = 6600)
    public static void uncommonLevelTwoCharmanderAppliesTwentyNormalProjectileHitsToZombie(GameTestHelper h) {
        // This GameTest runs in its own enabled namespace and uses an isolated
        // template, so no delay against the legacy suite is needed.
        setupTwentyShotZombieTest(h);
    }

    private static void setupTwentyShotZombieTest(GameTestHelper h) {
        floor(h);
        ServerPlayer owner = player(h, h.absolutePos(new BlockPos(7, 1, 5)));
        MinemonCreature pokemon = h.spawn(ModEntities.SPECIES.get("charmander").get(), new BlockPos(5, 1, 5));
        pokemon.tame(owner);
        pokemon.setNoAi(true);
        pokemon.setPokemonProgress(1, 0);
        check(h, "uncommon".equals(pokemon.getPokemonRarity()) && pokemon.pokemonAttackDamage() == 4.0D,
                "Expected uncommon level 1 Charmander damage=4.0; species=" + pokemon.getSpeciesSlug()
                        + ", rarity=" + pokemon.getPokemonRarity() + ", level=" + pokemon.getPokemonLevel()
                        + ", damage=" + pokemon.pokemonAttackDamage() + ", attribute="
                        + pokemon.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue()
                        + ", catalogRarity=" + br.com.petrickfaria.minemon.pokec.PokecEconomy.get().rarity(pokemon.getSpeciesSlug()));
        Zombie zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(9, 1, 5));
        zombie.setNoAi(true); // Real vanilla Zombie entity and hurt pipeline, with movement disabled for controlled shots.
        // Other GameTests can change the shared test world's clock while this
        // series runs; a solid roof keeps daylight burn out of the measurement.
        h.setBlock(new BlockPos(9, 4, 5), Blocks.STONE);
        String spawnedGear = java.util.Arrays.stream(net.minecraft.world.entity.EquipmentSlot.values())
                .map(slot -> slot.name() + "=" + zombie.getItemBySlot(slot))
                .collect(java.util.stream.Collectors.joining(","));
        System.out.println("[MINEMON ZOMBIE FIXTURE] spawned armor=" + zombie.getArmorValue()
                + " armorAttribute=" + zombie.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).getBaseValue()
                + " effects=" + zombie.getActiveEffects() + " gear=" + spawnedGear);
        for (var slot : net.minecraft.world.entity.EquipmentSlot.values())
            zombie.setItemSlot(slot, ItemStack.EMPTY);
        for (var effect : new java.util.ArrayList<>(zombie.getActiveEffects())) zombie.removeEffect(effect.getEffect());
        // Keep the vanilla Zombie's natural armor attribute (2), while clearing
        // randomized gear and effects so the fixture is reproducible.
        zombie.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200.0D);
        zombie.setHealth(200.0F);
        check(h, zombie.getArmorValue() == 2 && zombie.getActiveEffects().isEmpty(),
                "Combat target must retain only its vanilla natural armor and no effects (armor=" + zombie.getArmorValue()
                        + ", effects=" + zombie.getActiveEffects() + ")");
        System.out.println("[MINEMON ZOMBIE FIXTURE] controlled armor=" + zombie.getArmorValue()
                + " effects=" + zombie.getActiveEffects().size() + " gear=" + spawnedGear);

        // Verify the requested level 1 value through a normal projectile against
        // the same armored Zombie before running the 20-hit level 2 series.
        check(h, pokemon.pokemonAttackDamage() == 4.0D, "Uncommon level 1 Charmander must configure 4.0 damage");
        float levelOneBefore = zombie.getHealth();
        long levelOneLaunch = h.getLevel().getGameTime();
        pokemon.focusPlayerTarget(zombie);
        check(h, pokemon.fireElementalProjectile(zombie), "Uncommon level 1 Charmander failed to launch its projectile");
        var levelOneShots = h.getLevel().getEntitiesOfClass(br.com.petrickfaria.minemon.entity.ElementalProjectile.class,
                pokemon.getBoundingBox().inflate(12.0D), shot -> shot.getOwner() == pokemon && shot.getTargetIdForValidation(zombie));
        check(h, levelOneShots.size() == 1, "Expected exactly one level 1 aimed projectile");
        if (levelOneShots.size() != 1) return;
        check(h, levelOneShots.get(0).damageForValidation() == 4.0F,
                "Uncommon level 1 projectile must carry exactly 4.0 damage");
        h.runAfterDelay(15, () -> {
            float applied = levelOneBefore - zombie.getHealth();
            check(h, applied == 4.0F,
                    "Armored Zombie received " + applied + " HP from level 1; expected exactly 4.0");
            check(h, zombie.invulnerableTime > 0,
                    "Accepted level 1 damage must preserve vanilla invulnerability behavior");
            System.out.println("[MINEMON COMBAT] level=1 configured=4.0 projectile=4.0 applied=" + applied
                    + " targetHP=" + zombie.getHealth() + " invulnerableTime=" + zombie.invulnerableTime
                    + " armor=" + zombie.getArmorValue() + " effects=" + zombie.getActiveEffects().size());
            pokemon.setPokemonProgress(2, 0);
            check(h, pokemon.pokemonAttackDamage() == 5.0D
                            && pokemon.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() == 5.0D,
                    "Uncommon level 2 Charmander must configure exactly 5 attack-damage points");
            runControlledZombieShot(h, owner, pokemon, zombie, 1, levelOneLaunch, 0.0F, 0);
        });
    }

    private static void runControlledZombieShot(GameTestHelper h, ServerPlayer owner, MinemonCreature pokemon,
                                                  Zombie zombie, int hit, long previousLaunch,
                                                  float totalProjectileDamage, int waitedForTarget) {
        if (!zombie.isAlive()) { h.fail("The high-health zombie died before all controlled hits"); return; }
        check(h, zombie.getArmorValue() == 2 && zombie.getActiveEffects().isEmpty(),
                "Target mitigation state changed before shot " + hit);
        if (zombie.invulnerableTime > 0 || !pokemon.combatReady()) {
            if (waitedForTarget == 0 || waitedForTarget % 10 == 0) {
                var lastSource = zombie.getLastDamageSource();
                System.out.println("[MINEMON COMBAT] hold hit=" + hit + " waitingTicks=" + waitedForTarget
                        + " invulnerableTime=" + zombie.invulnerableTime + " combatReady=" + pokemon.combatReady()
                        + " health=" + zombie.getHealth() + " lastDamage="
                        + (lastSource == null ? "none" : lastSource.getMsgId() + "/" + lastSource.getEntity()));
            }
            h.runAfterDelay(1, () -> runControlledZombieShot(h, owner, pokemon, zombie, hit,
                    previousLaunch, totalProjectileDamage, waitedForTarget + 1));
            return;
        }
        long launchedAt = h.getLevel().getGameTime();
        long interval = previousLaunch < 0L ? -1L : launchedAt - previousLaunch;
        if (previousLaunch >= 0L) check(h, interval >= 60L,
                "Normal attack cooldown interval was shorter than 60 ticks before shot " + hit + ": " + interval);
        float shotHealthBefore = zombie.getHealth();
        float configuredDamage = (float) pokemon.pokemonAttackDamage();
        pokemon.focusPlayerTarget(zombie);
        check(h, pokemon.fireElementalProjectile(zombie), "Charmander failed to launch normal projectile " + hit);
        var shots = h.getLevel().getEntitiesOfClass(br.com.petrickfaria.minemon.entity.ElementalProjectile.class,
                pokemon.getBoundingBox().inflate(12.0D), shot -> shot.getOwner() == pokemon && shot.getTargetIdForValidation(zombie));
        check(h, shots.size() == 1, "Expected exactly one aimed projectile for shot " + hit);
        if (shots.size() != 1) return;
        var shot = shots.get(0);
        check(h, shot.damageForValidation() == 5.0F,
                "Projectile " + hit + " carried " + shot.damageForValidation() + " instead of 5.0 damage");
        System.out.println("[MINEMON COMBAT] launch hit=" + hit + " configured=" + configuredDamage
                + " projectile=" + shot.damageForValidation() + " targetHP=" + shotHealthBefore
                + " interval=" + (previousLaunch < 0 ? "initial" : interval)
                + " invulnerableTime=" + zombie.invulnerableTime + " armor=" + zombie.getArmorValue()
                + " effects=" + zombie.getActiveEffects().size() + " fireTicks=" + zombie.getRemainingFireTicks());
        h.runAfterDelay(15, () -> {
            float applied = shotHealthBefore - zombie.getHealth();
            check(h, applied == 5.0F, "Zombie received " + applied + " HP on shot " + hit + "; expected 5.0"
                    + " (health=" + zombie.getHealth() + ", hurtTime=" + zombie.hurtTime
                    + ", invulnerableTime=" + zombie.invulnerableTime + ", fireTicks=" + zombie.getRemainingFireTicks() + ")");
            check(h, zombie.invulnerableTime > 0,
                    "Accepted vanilla damage must enter the normal invulnerability window after shot " + hit);
            check(h, h.getLevel().getGameTime() - launchedAt >= 8,
                    "Projectile interval/flight was shorter than the measured normal hit window");
            System.out.println("[MINEMON COMBAT] impact hit=" + hit + " applied=" + applied
                    + " targetHP=" + zombie.getHealth() + " interval=" + (h.getLevel().getGameTime() - launchedAt)
                    + " hurtTime=" + zombie.hurtTime + " invulnerableTime=" + zombie.invulnerableTime
                    + " armor=" + zombie.getArmorValue() + " effects=" + zombie.getActiveEffects().size()
                    + " fireTicks=" + zombie.getRemainingFireTicks());
            if (hit == 20) {
                check(h, totalProjectileDamage + applied == 100.0F,
                        "Twenty accepted hits must total 100 projectile damage; actual=" + (totalProjectileDamage + applied));
                System.out.println("[MINEMON COMBAT] summary acceptedHits=20 totalProjectileDamage="
                        + (totalProjectileDamage + applied) + " finalZombieHP=" + zombie.getHealth());
                owner.discard(); pokemon.discard(); zombie.discard(); h.succeed();
                return;
            }
            h.runAfterDelay(45, () -> runControlledZombieShot(h, owner, pokemon, zombie, hit + 1,
                    launchedAt, totalProjectileDamage + applied, 0));
        });
    }
}
