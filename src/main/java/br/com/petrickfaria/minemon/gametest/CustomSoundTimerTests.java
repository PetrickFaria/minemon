package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.registry.ModEntities;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import java.util.UUID;

@GameTestHolder(Minemon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CustomSoundTimerTests {
    @GameTest(template = "test_arena", timeoutTicks = 30)
    public static void createsExactly151FoldersFromNationalDex(GameTestHelper helper) {
        try {
            br.com.petrickfaria.minemon.sound.CustomSoundFolders.createAll();
            var root = br.com.petrickfaria.minemon.sound.CustomSoundFolders.root();
            try (var folders = java.nio.file.Files.list(root)) {
                long count = folders.filter(path -> java.nio.file.Files.isDirectory(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)).count();
                if (count != 151) helper.fail("expected 151 folders, found " + count);
            }
            for (var definition : br.com.petrickfaria.minemon.entity.SpeciesDefinitions.ALL.values()) {
                var path = br.com.petrickfaria.minemon.sound.CustomSoundFolders.folder(definition);
                if (!java.nio.file.Files.isDirectory(path, java.nio.file.LinkOption.NOFOLLOW_LINKS))
                    helper.fail("missing directory for National Pokédex #" + definition.nationalPokedexNumber() + ": " + path.getFileName());
            }
            helper.succeed();
        } catch (Exception failure) { helper.fail("folder setup failed: " + failure); }
    }

    @GameTest(template = "test_arena", timeoutTicks = 30)
    public static void customSoundIntervalsStayBetweenThreeAndFiveMinutes(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            helper.setBlock(new BlockPos(x, 3, z), Blocks.STONE);
            for (int y = 4; y <= 8; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        }
        helper.getLevel().setDayTime(1000);
        MinemonCreature first = helper.spawn(ModEntities.BULBASAUR.get(), new BlockPos(5, 4, 5));
        MinemonCreature second = helper.spawn(ModEntities.BULBASAUR.get(), new BlockPos(8, 4, 5));
        first.setSleeping(true);
        helper.runAfterDelay(2, () -> {
            int firstDelay = first.customSoundTicksForValidation();
            int secondDelay = second.customSoundTicksForValidation();
            if (firstDelay < 18 || firstDelay > 200 || secondDelay < 18 || secondDelay > 200)
                helper.fail("entities loaded during daytime need independent 20..200 tick dawn delays");
            if (!first.customSoundDayActiveForValidation() || !second.customSoundDayActiveForValidation())
                helper.fail("daytime entities must have an active scheduler, including sleeping Pokémon");

            first.setSleeping(true);
            first.resetCustomSoundTimerAndPlay();
            if (!first.isSleeping()) helper.fail("custom playback must not wake an already sleeping Pokémon");
            if (!withinThreeToFiveMinutes(first.customSoundTicksForValidation()))
                helper.fail("daytime click must cancel the dawn delay and choose 3600..6000 ticks");
            second.setCustomSoundTicksForValidation(1);
            helper.runAfterDelay(2, () -> {
                if (!withinIntervalAfterTwoTicks(first.customSoundTicksForValidation())
                        || !withinIntervalAfterTwoTicks(second.customSoundTicksForValidation()))
                    helper.fail("automatic playback and interaction reset must leave timers in the 3600..6000 tick range");

                second.setCustomSoundTicksForValidation(1);
                helper.runAfterDelay(2, () -> {
                    if (!withinIntervalAfterTwoTicks(second.customSoundTicksForValidation()))
                        helper.fail("a new 3..5 minute interval must be selected after every automatic playback");
                    helper.getLevel().setDayTime(18000);
                    MinemonCreature loadedAtNight = helper.spawn(ModEntities.SPECIES.get("pikachu").get(), new BlockPos(11, 4, 5));
                    helper.runAfterDelay(2, () -> {
                        if (first.customSoundTicksForValidation() != 0 || second.customSoundTicksForValidation() != 0)
                            helper.fail("night must suspend and discard both pending timers");
                        if (loadedAtNight.customSoundTicksForValidation() != 0 || loadedAtNight.customSoundDayActiveForValidation())
                            helper.fail("entities loaded during night must not schedule custom sounds");
                        second.resetCustomSoundTimerAndPlay();
                        if (second.customSoundTicksForValidation() != 0)
                            helper.fail("nighttime click must not schedule or play custom audio");

                        helper.getLevel().setDayTime(1000);
                        helper.runAfterDelay(2, () -> {
                            if (first.customSoundTicksForValidation() < 18 || first.customSoundTicksForValidation() > 200
                                    || second.customSoundTicksForValidation() < 18 || second.customSoundTicksForValidation() > 200
                                    || loadedAtNight.customSoundTicksForValidation() < 18 || loadedAtNight.customSoundTicksForValidation() > 200)
                                helper.fail("dawn after night must schedule fresh randomized delays");
                            helper.succeed();
                        });
                    });
                });
            });
        });
    }

    @GameTest(template = "test_arena", timeoutTicks = 30)
    public static void onlyMainHandRightClickTriggersCustomSound(GameTestHelper helper) {
        helper.getLevel().setDayTime(1000);
        MinemonCreature creature = helper.spawn(ModEntities.BULBASAUR.get(), new BlockPos(5, 1, 5));
        float initialHealth = creature.getHealth();
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "MinemonSoundTester"), null);
        player.connection = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "MinemonSoundConnection")).connection;
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(7.5D, 1.0D, 5.5D, 0.0F, 0.0F);
        helper.getLevel().addNewPlayer(player);

        creature.setCustomSoundTicksForValidation(1);
        int attackTicksBefore = creature.customSoundTicksForValidation();
        int attackPlaysBefore = creature.customSoundInteractionDispatchesForValidation();
        var attack = new AttackEntityEvent(player, creature);
        MinecraftForge.EVENT_BUS.post(attack);
        int attackTicksAfter = creature.customSoundTicksForValidation();
        int attackPlaysAfter = creature.customSoundInteractionDispatchesForValidation();
        com.mojang.logging.LogUtils.getLogger().info("CUSTOM SOUND TRACE phase=attack hand=MAIN_ATTACK eventCanceled="+attack.isCanceled()
                +",intervalBefore="+attackTicksBefore+",intervalAfter="+attackTicksAfter+",expectedIntervalAfter="+attackTicksBefore
                +",audioDispatchesBefore="+attackPlaysBefore+",audioDispatchesAfter="+attackPlaysAfter);
        if (attackTicksAfter != attackTicksBefore || attackPlaysAfter != attackPlaysBefore
                || creature.getHealth() != initialHealth)
            helper.fail("LEFT_CLICK_DIAGNOSTIC: expected uncanceled attack, unchanged timer/audio count and health; hand=MAIN_ATTACK"
                    +",eventCanceled="+attack.isCanceled()+",intervalBefore="+attackTicksBefore+",intervalAfter="+attackTicksAfter
                    +",expectedIntervalAfter="+attackTicksBefore+",audioDispatchesBefore="+attackPlaysBefore
                    +",audioDispatchesAfter="+attackPlaysAfter+",healthBefore="+initialHealth+",healthAfter="+creature.getHealth());

        int offhandTicksBefore = creature.customSoundTicksForValidation();
        int offhandPlaysBefore = creature.customSoundInteractionDispatchesForValidation();
        var offHand = new PlayerInteractEvent.EntityInteract(player, InteractionHand.OFF_HAND, creature);
        MinecraftForge.EVENT_BUS.post(offHand);
        int offhandTicksAfter = creature.customSoundTicksForValidation();
        int offhandPlaysAfter = creature.customSoundInteractionDispatchesForValidation();
        com.mojang.logging.LogUtils.getLogger().info("CUSTOM SOUND TRACE phase=right_click hand="+offHand.getHand()
                +",eventCanceled="+offHand.isCanceled()+",cancellationResult="+offHand.getCancellationResult()
                +",intervalBefore="+offhandTicksBefore+",intervalAfter="+offhandTicksAfter+",expectedIntervalAfter="+offhandTicksBefore
                +",audioDispatchesBefore="+offhandPlaysBefore+",audioDispatchesAfter="+offhandPlaysAfter);
        if (offHand.isCanceled())
            helper.fail("OFF_HAND_CANCELED_DIAGNOSTIC: right-click event was canceled; hand="+offHand.getHand()
                    +",eventCanceled="+offHand.isCanceled()+",cancellationResult="+offHand.getCancellationResult()
                    +",intervalBefore="+offhandTicksBefore+",intervalAfter="+offhandTicksAfter
                    +",audioDispatchesBefore="+offhandPlaysBefore+",audioDispatchesAfter="+offhandPlaysAfter);
        if (offhandTicksAfter != offhandTicksBefore || offhandPlaysAfter != offhandPlaysBefore)
            helper.fail("OFF_HAND_DUPLICATE_DIAGNOSTIC: off-hand must not reset the timer or dispatch audio; hand="+offHand.getHand()
                    +",eventCanceled="+offHand.isCanceled()+",cancellationResult="+offHand.getCancellationResult()
                    +",intervalBefore="+offhandTicksBefore+",intervalAfter="+offhandTicksAfter
                    +",expectedIntervalAfter="+offhandTicksBefore+",audioDispatchesBefore="+offhandPlaysBefore
                    +",audioDispatchesAfter="+offhandPlaysAfter+",expectedAudioDispatchesAfter="+offhandPlaysBefore);

        int mainhandTicksBefore = creature.customSoundTicksForValidation();
        int mainhandPlaysBefore = creature.customSoundInteractionDispatchesForValidation();
        var mainHand = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, creature);
        long worldTimeBeforePost = helper.getLevel().getDayTime();
        boolean daylightBeforePost = creature.isCustomSoundDaytime();
        MinecraftForge.EVENT_BUS.post(mainHand);
        int resetTicks = creature.customSoundTicksForValidation();
        int mainhandPlaysAfter = creature.customSoundInteractionDispatchesForValidation();
        com.mojang.logging.LogUtils.getLogger().info("CUSTOM SOUND TRACE phase=right_click hand="+mainHand.getHand()
                +",eventCanceled="+mainHand.isCanceled()+",cancellationResult="+mainHand.getCancellationResult()
                +",intervalBefore="+mainhandTicksBefore+",intervalAfter="+resetTicks+",expectedInterval=3600..6000"
                +",audioDispatchesBefore="+mainhandPlaysBefore+",audioDispatchesAfter="+mainhandPlaysAfter
                +",worldTimeBeforePost="+worldTimeBeforePost+",worldTimeAfterPost="+helper.getLevel().getDayTime()
                +",daylightBeforePost="+daylightBeforePost+",daylightAfterPost="+creature.isCustomSoundDaytime()
                +",dimensionHasSkyLight="+helper.getLevel().dimensionType().hasSkyLight()
                +",eventEntityLevelClientSide="+mainHand.getEntity().level.isClientSide
                +",eventEntitySameLevel="+(mainHand.getEntity().level==helper.getLevel())
                +",targetType="+mainHand.getTarget().getType()+",targetIsCreature="+(mainHand.getTarget()==creature)
                +",targetAlive="+creature.isAlive()+",targetRemoved="+creature.isRemoved());
        if (mainHand.isCanceled())
            helper.fail("MAIN_HAND_CANCELED_DIAGNOSTIC: right-click event was canceled; hand="+mainHand.getHand()
                    +",eventCanceled="+mainHand.isCanceled()+",cancellationResult="+mainHand.getCancellationResult()
                    +",intervalBefore="+mainhandTicksBefore+",intervalAfter="+resetTicks
                    +",expectedIntervalAfter=3600..6000,audioDispatchesBefore="+mainhandPlaysBefore
                    +",audioDispatchesAfter="+mainhandPlaysAfter);
        if (resetTicks < 3600 || resetTicks > 6000)
            helper.fail("MAIN_HAND_INTERVAL_DIAGNOSTIC: right-click did not reset the timer into 3600..6000 ticks; hand="+mainHand.getHand()
                    +",eventCanceled="+mainHand.isCanceled()+",cancellationResult="+mainHand.getCancellationResult()
                    +",intervalBefore="+mainhandTicksBefore+",intervalAfter="+resetTicks
                    +",expectedIntervalAfter=3600..6000,audioDispatchesBefore="+mainhandPlaysBefore
                    +",audioDispatchesAfter="+mainhandPlaysAfter);
        if (mainhandPlaysAfter != mainhandPlaysBefore + 1)
            helper.fail("MAIN_HAND_AUDIO_COUNT_DIAGNOSTIC: main-hand interaction must dispatch exactly one sound; hand="+mainHand.getHand()
                    +",eventCanceled="+mainHand.isCanceled()+",cancellationResult="+mainHand.getCancellationResult()
                    +",intervalBefore="+mainhandTicksBefore+",intervalAfter="+resetTicks
                    +",expectedIntervalAfter=3600..6000,audioDispatchesBefore="+mainhandPlaysBefore
                    +",audioDispatchesAfter="+mainhandPlaysAfter+",expectedAudioDispatchesAfter="+(mainhandPlaysBefore+1));
        if (creature.getHealth() != initialHealth)
            helper.fail("right-clicking to play a sound must not damage the Pokémon");

        player.discard();
        helper.succeed();
    }

    private static boolean withinThreeToFiveMinutes(int ticks) {
        return ticks >= 3600 && ticks <= 6000;
    }

    private static boolean withinIntervalAfterTwoTicks(int ticks) {
        return ticks >= 3598 && ticks <= 6000;
    }

    private CustomSoundTimerTests() {}
}
