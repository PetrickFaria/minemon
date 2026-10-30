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
    public static void challengeRattataHasExpectedStatsAndRewardsExactlyOneCoin(GameTestHelper helper) {
        for (int x=0;x<16;x++) for(int z=0;z<16;z++) helper.setBlock(new BlockPos(x,0,z),Blocks.STONE);
        Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(6,1,6));
        villager.setVillagerData(villager.getVillagerData().setProfession(ModVillagers.ROCKET.get()));
        ServerPlayer player = player(helper);
        MinemonCreature rattata = RocketChallengeManager.spawnChallengeRattata(helper.getLevel(), villager, player);
        if (rattata == null) { helper.fail("Rocket challenge Rattata did not spawn"); return; }
        check(helper, rattata != null && rattata.isRocketSpecial(), "Rocket villager must create its special Rattata");
        check(helper, rattata != null && rattata.getTarget() == player, "Special Rattata must target the activating player");
        check(helper, rattata != null && rattata.getMaxHealth() == 10.0F && rattata.getHealth() == 10.0F,
                "Special Rattata must use the Common level-one base of ten HP");
        check(helper, rattata.getAttributeValue(Attributes.ATTACK_DAMAGE) == 4.0D,
                "Special Rattata must use the Common level-one base of four damage");
        check(helper, RocketChallengeManager.spawnChallengeRattata(helper.getLevel(), villager, player) == null,
                "One Rocket villager may not spawn a second active Rattata");
        rattata.hurt(net.minecraft.world.damagesource.DamageSource.playerAttack(player), 20.0F);
        check(helper, RocketChallengeManager.isChallengeComplete(villager), "Defeating Rattata must permanently complete the challenge");
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
