package br.com.petrickfaria.minemon.entity;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Minemon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PokemonCombatGameTests {
    @GameTest(template = "test_arena", batch = "pokemon_fixed_damage", timeoutTicks = 260)
    public static void uncommonLevelTwoCharmanderDealsExactlyFivePointsForTwentyHits(GameTestHelper helper) {
        MinemonCreature attacker = helper.spawn(ModEntities.SPECIES.get("charmander").get(), new BlockPos(2, 1, 2));
        attacker.setPokemonLevel(2);
        Cow target = helper.spawn(EntityType.COW, new BlockPos(7, 1, 2));
        target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0D);
        target.setHealth(200.0F);
        if (!"uncommon".equals(attacker.getPokemonRarity())) {
            helper.fail("Charmander test fixture must use uncommon rarity; got " + attacker.getPokemonRarity());
            return;
        }

        for (int hit = 0; hit < 20; hit++) {
            final int hitNumber = hit + 1;
            helper.runAtTickTime(hit * 12 + 1, () -> {
                ElementalProjectile projectile = new ElementalProjectile(attacker, target);
                float before = target.getHealth();
                boolean accepted = target.hurt(
                        DamageSource.indirectMobAttack(projectile, attacker), projectile.configuredDamageForValidation());
                float applied = before - target.getHealth();
                if (!accepted || Math.abs(applied - 5.0F) > 0.0001F) {
                    helper.fail("Hit " + hitNumber + " applied " + applied + " HP; expected exactly 5.0 HP");
                    return;
                }
                if (hitNumber == 20) {
                    target.discard();
                    attacker.discard();
                    helper.succeed();
                }
            });
        }
    }

    private PokemonCombatGameTests() { }
}
