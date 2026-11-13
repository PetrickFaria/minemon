package br.com.petrickfaria.minemon.client;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.pokec.TrainerProgression;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import com.mojang.blaze3d.systems.RenderSystem;
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
        RenderSystem.setShaderTexture(0, GuiComponent.GUI_ICONS_LOCATION);
        GuiComponent.blit(event.getPoseStack(), x, y, 0, 64, 182, 5, 256, 256);
        // Preserve the vanilla XP bar atlas and change only the trainer fill tint.
        RenderSystem.setShaderColor(0.0F, 0.75F, 1.0F, 1.0F);
        int fill = Math.round(progress * 182.0F);
        if (fill > 0) GuiComponent.blit(event.getPoseStack(), x, y, 0, 69, fill, 5, 256, 256);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        String label = "Treinador Pokémon Lv. " + level;
        minecraft.font.draw(event.getPoseStack(), label, width / 2.0F - minecraft.font.width(label) / 2.0F, y - 10.0F, 0xFFFFFFFF);
    }

    private TrainerHud() { }
}
