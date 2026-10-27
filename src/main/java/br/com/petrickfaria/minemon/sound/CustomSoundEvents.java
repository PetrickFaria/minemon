package br.com.petrickfaria.minemon.sound;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Observes right-click interactions without cancelling normal entity interactions. */
@Mod.EventBusSubscriber(modid = Minemon.MOD_ID)
public final class CustomSoundEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled() || event.getEntity().level.isClientSide || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (event.getTarget() instanceof MinemonCreature creature && creature.isAlive()) {
            creature.resetCustomSoundTimerAndPlay();
        }
    }

    private CustomSoundEvents() {}
}
