package br.com.petrickfaria.minemon.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Forge namespace wrapper for isolated continuous-following and Gastly altitude diagnostics. */
@GameTestHolder("minemon_diag_gastly")
@PrefixGameTestTemplate(false)
public final class DiagnosticGastlyFollowingTest {
    @GameTest(template="test_arena", templateNamespace="minemon_diag_gastly", batch="diagnostic_gastly_follow", timeoutTicks=650)
    public static void followsWithContinuousMovementThenObeysSit(GameTestHelper helper) {
        NaturalFollowingGameTests.followsWithContinuousMovementThenObeysSit(helper);
    }

    private DiagnosticGastlyFollowingTest() {}
}
