package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.registry.ModItems;
import br.com.petrickfaria.minemon.registry.ModVillagers;
import br.com.petrickfaria.minemon.registry.RocketChallengeManager;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Minemon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RocketChallengeGameTests {
    private static void check(GameTestHelper helper, boolean value, String message) { if (!value) helper.fail(message); }

    private static ServerPlayer player(GameTestHelper helper) {
        var profile = new GameProfile(UUID.randomUUID(), "RocketTester");
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), profile, null);
        player.connection = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "RocketConnection")).connection;
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(helper.absolutePos(new BlockPos(4, 1, 4)).getX() + .5,
                helper.absolutePos(new BlockPos(4, 1, 4)).getY(), helper.absolutePos(new BlockPos(4, 1, 4)).getZ() + .5, 0, 0);
        helper.getLevel().addNewPlayer(player);
        return player;
    }

    @GameTest(template="test_arena", timeoutTicks=100)
    public static void firstLevelChallengeAndCompletedTradePersist(GameTestHelper helper) {
        System.setProperty("minemon.debugRocketSpawns", "true");
        for (int x=0;x<16;x++) for(int z=0;z<16;z++) helper.setBlock(new BlockPos(x,0,z),Blocks.STONE);
        Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(6,1,6));
        villager.setVillagerData(villager.getVillagerData().setProfession(ModVillagers.ROCKET.get()));
        ServerPlayer player = player(helper);
        check(helper, RocketChallengeManager.getLevel(villager) == 1, "New Rocket villager must start at level 1");
        MinemonCreature challenge = RocketChallengeManager.spawnChallengePokemon(helper.getLevel(), villager, player);
        if (challenge == null) { helper.fail("Rocket level 1 challenge did not spawn: "
                + villager.getPersistentData().getString("MinemonRocketSpawnFailure")); return; }
        check(helper, challenge.isRocketSpecial(), "Rocket villager must mark its challenge as special");
        check(helper, challenge.getSpeciesSlug().equals("zubat"), "Level 1 must summon Zubat");
        check(helper, challenge.getTarget() == player, "Special Pokémon must target the activating player");
        check(helper, challenge.getMaxHealth() == 10.0F && challenge.getHealth() == 10.0F,
                "Level 1 Zubat must have ten HP");
        check(helper, challenge.getAttributeValue(Attributes.ATTACK_DAMAGE) == 4.0D,
                "Level 1 Zubat must deal four internal damage");
        check(helper, RocketChallengeManager.spawnChallengePokemon(helper.getLevel(), villager, player) == null,
                "One Rocket villager may not spawn a duplicate active challenge");
        challenge.hurt(net.minecraft.world.damagesource.DamageSource.playerAttack(player), 100.0F);
        check(helper, RocketChallengeManager.isChallengeComplete(villager), "Defeating Zubat must unlock level 1 trades");
        long coins = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, villager.getBoundingBox().inflate(5.0D))
                .stream().filter(item -> item.getItem().is(ModItems.POKE_COIN.get())).mapToLong(item -> item.getItem().getCount()).sum();
        check(helper, coins == 1, "Challenge defeat must drop exactly one physical PokéCoin; found " + coins);
        CompoundTagVerifier.checkPersisted(helper, villager);
        br.com.petrickfaria.minemon.menu.RocketTradeMenu menu = new br.com.petrickfaria.minemon.menu.RocketTradeMenu(
                player.containerMenu.containerId + 1, player.getInventory(), villager);
        menu.getSlot(0).set(new ItemStack(ModItems.SOULS.get("fire").get(), 10));
        check(helper, menu.getSlot(5).getItem().is(ModItems.POKE_COIN.get()),
                "Unlocked exchange must preview one PokéCoin for ten souls");
        menu.clicked(5, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        check(helper, menu.getCarried().is(ModItems.POKE_COIN.get()) && !menu.getSlot(5).hasItem()
                        && menu.getSlot(0).getItem().isEmpty(),
                "Taking the output consumes ten souls exactly once and delivers the PokéCoin");
        check(helper, RocketChallengeManager.getTradesThisLevel(villager) == 1,
                "A completed exchange output pickup must count as one trade");
        player.getInventory().placeItemBackInInventory(menu.getCarried());
        menu.setCarried(ItemStack.EMPTY);
        menu.getSlot(0).set(new ItemStack(ModItems.SOULS.get("fire").get(), 10));
        check(helper, menu.clickMenuButton(player, 0), "Unlocked menu should let the player cycle to a soul exchange");
        check(helper, menu.getSlot(5).getItem().getItem() instanceof br.com.petrickfaria.minemon.pokec.SoulItem targetSoul
                        && targetSoul.soulType() != br.com.petrickfaria.minemon.pokec.SoulType.FIRE,
                "Selected soul exchange output must differ from the input soul type");
        menu.removed(player);
        player.discard();
        helper.succeed();
    }

    @GameTest(template="test_arena", timeoutTicks=3000)
    public static void allTenRocketLevelsUseExactSpeciesStatsAndTradeThresholds(GameTestHelper helper) {
        System.setProperty("minemon.debugRocketSpawns", "true");
        for (int x=0;x<16;x++) for(int z=0;z<16;z++) helper.setBlock(new BlockPos(x,0,z),Blocks.STONE);
        Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(6,1,6));
        villager.setVillagerData(villager.getVillagerData().setProfession(ModVillagers.ROCKET.get()));
        ServerPlayer player = player(helper);
        String[] species = {"zubat", "rattata", "ekans", "koffing", "golbat", "raticate", "arbok", "koffing", "kangaskhan", "mewtwo"};
        int[] health = {10, 14, 18, 22, 26, 30, 34, 38, 42, 46};
        int[] damage = {4, 6, 8, 10, 12, 14, 16, 18, 20, 22};
        int[] trades = {5, 15, 25, 35, 45, 55, 65, 75, 85};

        for (int level = 1; level <= 10; level++) {
            check(helper, RocketChallengeManager.getLevel(villager) == level,
                    "Rocket level should be " + level);
            MinemonCreature challenge = RocketChallengeManager.spawnChallengePokemon(helper.getLevel(), villager, player);
            if (challenge == null) { helper.fail("Challenge failed to spawn at Rocket level " + level + ": "
                    + villager.getPersistentData().getString("MinemonRocketSpawnFailure")); return; }
            check(helper, challenge.getSpeciesSlug().equals(species[level - 1]),
                    "Rocket level " + level + " must summon " + species[level - 1]);
            check(helper, challenge.getMaxHealth() == health[level - 1] && challenge.getHealth() == health[level - 1],
                    "Rocket level " + level + " must have " + health[level - 1] + " HP");
            check(helper, challenge.getAttributeValue(Attributes.ATTACK_DAMAGE) == damage[level - 1],
                    "Rocket level " + level + " must deal " + damage[level - 1] + " damage");
            challenge.hurt(net.minecraft.world.damagesource.DamageSource.playerAttack(player), 100.0F);
            check(helper, RocketChallengeManager.isChallengeComplete(villager),
                    "Victory must unlock Rocket level " + level + " trades");
            if (level == 10) continue;

            for (int trade = 0; trade < trades[level - 1]; trade++)
                RocketChallengeManager.recordCompletedTrade(villager, player);
            check(helper, RocketChallengeManager.getLevel(villager) == level + 1,
                    "Exactly " + trades[level - 1] + " trades must advance Rocket to level " + (level + 1));
            check(helper, RocketChallengeManager.getTradesThisLevel(villager) == 0,
                    "Trade count must reset after Rocket reaches level " + (level + 1));
            check(helper, !RocketChallengeManager.isChallengeComplete(villager),
                    "Trades must relock after Rocket reaches level " + (level + 1));
        }

        for (int i = 0; i < 200; i++) RocketChallengeManager.recordCompletedTrade(villager, player);
        check(helper, RocketChallengeManager.getLevel(villager) == 10,
                "Rocket level 10 must be capped; no level 11 may be created");
        check(helper, RocketChallengeManager.isChallengeComplete(villager),
                "Final Mewtwo victory must keep level 10 trades unlocked");
        CompoundTagVerifier.checkPersisted(helper, villager);
        player.discard();
        helper.succeed();
    }

    private static final class CompoundTagVerifier {
        static void checkPersisted(GameTestHelper helper, Villager villager) {
            net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
            villager.saveWithoutId(saved);
            Villager copy = EntityType.VILLAGER.create(helper.getLevel());
            copy.load(saved);
            check(helper, RocketChallengeManager.isChallengeComplete(copy), "Challenge completion must survive villager save/load");
            copy.discard();
        }
    }

    private RocketChallengeGameTests() { }
}
