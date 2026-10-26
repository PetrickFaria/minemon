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

    @GameTest(template = "test_arena", timeoutTicks = 30)
    public static void independentTimersContinueDuringSleepAndClickResets(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            helper.setBlock(new BlockPos(x, 3, z), Blocks.STONE);
            for (int y = 4; y <= 8; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        }
        MinemonCreature first = helper.spawn(ModEntities.BULBASAUR.get(), new BlockPos(5, 4, 5));
        MinemonCreature second = helper.spawn(ModEntities.SPECIES.get("pikachu").get(), new BlockPos(8, 4, 5));
        first.setCustomSoundTicksForValidation(1);
        second.setCustomSoundTicksForValidation(100);
        first.setSleeping(true);
        helper.runAfterDelay(2, () -> {
            if (first.customSoundTicksForValidation() > 6000 || first.customSoundTicksForValidation() < 5998) helper.fail("first timer should emit and restart while asleep");
            if (second.customSoundTicksForValidation() > 99 || second.customSoundTicksForValidation() <= 0)
                helper.fail("second entity timer must advance independently");
            second.resetCustomSoundTimerAndPlay();
            if (second.customSoundTicksForValidation() != 6000) helper.fail("click trigger must restart the five minute timer");
            helper.succeed();
        });
    }

    private CustomSoundTimerTests() {}
}
