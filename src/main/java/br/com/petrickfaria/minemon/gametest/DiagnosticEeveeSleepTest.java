package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.gametest.NightAndSwimmingTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Forge namespace wrapper so the Eevee sleep test can run without the rest of minemon_gen1. */
@GameTestHolder("minemon_diag_eevee")
@PrefixGameTestTemplate(false)
public final class DiagnosticEeveeSleepTest {
    @GameTest(template="test_arena", templateNamespace="minemon_diag_eevee", batch="diagnostic_eevee_sleep", timeoutTicks=2400)
    public static void eeveeLineSleepsWakesRetaliatesReturnsAndWakesAtDawn(GameTestHelper helper) {
        NightAndSwimmingTests.eeveeLineSleepsWakesRetaliatesReturnsAndWakesAtDawn(helper);
    }

    private DiagnosticEeveeSleepTest() {}
}
