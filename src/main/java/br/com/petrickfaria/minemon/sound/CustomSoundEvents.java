package br.com.petrickfaria.minemon.sound;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Observes attacks without cancelling or replacing vanilla combat. */
@Mod.EventBusSubscriber(modid = Minemon.MOD_ID)
public final class CustomSoundEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (event.isCanceled() || event.getEntity().level.isClientSide) return;
        if (event.getTarget() instanceof MinemonCreature creature && creature.isAlive()) {
            creature.resetCustomSoundTimerAndPlay();
        }
    }

    private CustomSoundEvents() {}
}
