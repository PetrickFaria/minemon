package br.com.petrickfaria.minemon.menu;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;

/** Small shared vanilla-style drawing primitives; no interaction or server state. */
final class MinemonGui {
    private MinemonGui() { }
    static void panel(PoseStack p, int x, int y, int w, int h) {
        GuiComponent.fill(p,x,y,x+w,y+h,0xFF373737);
        GuiComponent.fill(p,x+1,y+1,x+w-1,y+h-1,0xFFFFFFFF);
        GuiComponent.fill(p,x+3,y+3,x+w-1,y+h-1,0xFF555555);
        GuiComponent.fill(p,x+3,y+3,x+w-3,y+h-3,0xFFC6C6C6);
    }
    static void slot(PoseStack p, int x, int y, int w, int h) {
        GuiComponent.fill(p,x,y,x+w,y+h,0xFFFFFFFF);
        GuiComponent.fill(p,x,y,x+w-1,y+h-1,0xFF373737);
        GuiComponent.fill(p,x+1,y+1,x+w-1,y+h-1,0xFF8B8B8B);
    }
}
