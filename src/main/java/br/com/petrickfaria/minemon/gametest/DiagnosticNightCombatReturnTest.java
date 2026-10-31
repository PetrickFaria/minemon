package br.com.petrickfaria.minemon.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Runs the complete mixed-night combat and rest scenario in its own GameTest namespace. */
@GameTestHolder("minemon_diag_night")
@PrefixGameTestTemplate(false)
public final class DiagnosticNightCombatReturnTest {
    @GameTest(template="test_arena", templateNamespace="minemon_diag_night", batch="diagnostic_night_return", timeoutTicks=1000)
    public static void realNightLandingHoverWakeRetaliationReturnAndDawn(GameTestHelper helper) {
        NightAndSwimmingTests.runRealNightLandingHoverWakeRetaliationReturnAndDawn(helper);
    }

    private DiagnosticNightCombatReturnTest() {}
}
