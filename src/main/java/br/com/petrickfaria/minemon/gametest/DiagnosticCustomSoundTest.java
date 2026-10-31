package br.com.petrickfaria.minemon.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Forge namespace wrapper for isolated custom-sound interaction diagnostics. */
@GameTestHolder("minemon_diag_sound")
@PrefixGameTestTemplate(false)
public final class DiagnosticCustomSoundTest {
    @GameTest(template="test_arena", templateNamespace="minemon_diag_sound", batch="diagnostic_custom_sound", timeoutTicks=30)
    public static void onlyMainHandRightClickTriggersCustomSound(GameTestHelper helper) {
        CustomSoundTimerTests.onlyMainHandRightClickTriggersCustomSound(helper);
    }

    private DiagnosticCustomSoundTest() {}
}
