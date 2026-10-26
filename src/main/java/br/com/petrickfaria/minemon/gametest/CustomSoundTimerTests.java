package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

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

    @GameTest(template = "test_arena", timeoutTicks = 260)
    public static void timersAreDayOnlyAndEachDawnGetsIndependentDelay(GameTestHelper helper) {
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

            second.resetCustomSoundTimerAndPlay();
            if (second.customSoundTicksForValidation() != 6000) helper.fail("daytime click must cancel the dawn delay and restart five minutes");
            helper.runAfterDelay(201, () -> {
                if (first.customSoundTicksForValidation() < 5790 || first.customSoundTicksForValidation() > 6000
                        || second.customSoundTicksForValidation() < 5790 || second.customSoundTicksForValidation() > 6000)
                    helper.fail("first dawn playback and daytime click must each start a fresh five-minute timer");
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
                        if (!first.isSleeping()) helper.fail("sound scheduling must not wake a sleeping Pokémon");
                        helper.succeed();
                    });
                });
            });
        });
    }

    private CustomSoundTimerTests() {}
}
