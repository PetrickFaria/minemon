package br.com.petrickfaria.minemon.client;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.pokec.TrainerProgression;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Separate Trainer Pokémon experience strip, positioned above the vanilla XP bar. */
@Mod.EventBusSubscriber(modid = Minemon.MOD_ID, value = Dist.CLIENT)
public final class TrainerHud {
    private static volatile int level = 1;
    private static volatile double xp;

    public static void update(int nextLevel, double nextXp) {
        level = Math.max(1, Math.min(TrainerProgression.MAX_LEVEL, nextLevel));
        xp = Double.isFinite(nextXp) ? Math.max(0.0D, nextXp) : 0.0D;
    }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) return;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int x = width / 2 - 91, y = height - 48;
        double needed = TrainerProgression.xpToNext(level);
        float progress = Double.isInfinite(needed) ? 1.0F : (float)Math.min(1.0D, xp / needed);
        GuiComponent.fill(event.getPoseStack(), x, y, x + 182, y + 5, 0xC0000000);
        GuiComponent.fill(event.getPoseStack(), x + 1, y + 1, x + 1 + Math.round(180 * progress), y + 4, 0xFF38D8D2);
        String label = "Treinador Pokémon Lv. " + level;
        minecraft.font.draw(event.getPoseStack(), label, width / 2.0F - minecraft.font.width(label) / 2.0F, y - 10.0F, 0xFFFFFFFF);
    }

    private TrainerHud() { }
}
