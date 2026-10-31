package br.com.petrickfaria.minemon.menu;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Vanilla trade layout adapted to the approved five-input Rocket exchange. */
public final class RocketTradeScreen extends AbstractContainerScreen<RocketTradeMenu> {
    private static final ResourceLocation VILLAGER = new ResourceLocation("minecraft", "textures/gui/container/villager2.png");
    private static final int ROWS = 7, ROW_HEIGHT = 20;
    private int scroll;
    private boolean dragging;

    public RocketTradeScreen(RocketTradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 276; imageHeight = 166;
        inventoryLabelX = 108; inventoryLabelY = 72;
    }
    static int progressPixels(int level, int completedTrades, int width) {
        return Math.round(br.com.petrickfaria.minemon.registry.RocketChallengeManager.tradeProgress(level, completedTrades) * width);
    }
    @Override protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, VILLAGER);
        GuiComponent.blit(pose, leftPos, topPos, 0, 0, imageWidth, imageHeight, 512, 256);
        // Cover vanilla's two-input area before placing all five retained inputs.
        GuiComponent.fill(pose, leftPos+105, topPos+25, leftPos+266, topPos+66, 0xFFC6C6C6);
        for (int i=0;i<6;i++) {
            var slot=menu.getSlot(i);
            MinemonGui.slot(pose,leftPos+slot.x-1,topPos+slot.y-1,18,18);
        }
        font.draw(pose,"→",leftPos+218,topPos+40,0xFF404040);
        int progressX=leftPos+136,progressY=topPos+16;
        int progressWidth=progressPixels(menu.progressionLevel(),menu.tradesThisLevel(),102);
        GuiComponent.blit(pose,progressX,progressY,0,186,102,5,512,256);
        if(progressWidth>0)GuiComponent.blit(pose,progressX,progressY,0,191,progressWidth,5,512,256);
        font.draw(pose,"Lv. "+menu.progressionLevel(),leftPos+108,topPos+14,0xFF404040);
        for (int row=0;row<ROWS;row++) {
            int index=scroll+row;
            if(index>=menu.offerCount())break;
            int y=topPos+18+row*ROW_HEIGHT;
            boolean selected=index==menu.selectedIndex();
            MinemonGui.panel(pose,leftPos+5,y,88,ROW_HEIGHT);
            if(selected)GuiComponent.fill(pose,leftPos+7,y+2,leftPos+91,y+18,0xFFADC1A4);
            var result=menu.offerResult(index);
            itemRenderer.renderAndDecorateItem(result,leftPos+72,y+2);
            font.draw(pose,"10 almas →",leftPos+10,y+6,0xFF404040);
        }
        int max=Math.max(0,menu.offerCount()-ROWS);
        int thumbY=topPos+18+(max==0?0:113*scroll/max);
        GuiComponent.fill(pose,leftPos+95,topPos+18,leftPos+101,topPos+158,0xFF777777);
        MinemonGui.panel(pose,leftPos+95,thumbY,6,27);
    }
    @Override protected void renderLabels(PoseStack pose, int mouseX, int mouseY) {
        font.draw(pose,"Trocas",5,6,0xFF404040);
        font.draw(pose,title,108,6,0xFF404040);
        font.draw(pose,playerInventoryTitle,108,72,0xFF404040);
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        if(button==0 && x>=leftPos+5 && x<leftPos+93 && y>=topPos+18 && y<topPos+158) {
            int index=scroll+(int)(y-topPos-18)/ROW_HEIGHT;
            if(index<menu.offerCount() && minecraft.gameMode!=null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId,100+index);
            return true;
        }
        if(button==0 && x>=leftPos+94 && x<leftPos+103 && y>=topPos+18 && y<topPos+158) {
            dragging=true; setScroll(y); return true;
        }
        return super.mouseClicked(x,y,button);
    }
    private void setScroll(double y) {
        int max=Math.max(0,menu.offerCount()-ROWS);
        scroll=Math.max(0,Math.min(max,(int)Math.round((y-topPos-31.5)*max/113.0)));
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy) {
        if(dragging){setScroll(y);return true;}
        return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x,double y,int button) {
        dragging=false;return super.mouseReleased(x,y,button);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta) {
        if(x>=leftPos && x<leftPos+103 && y>=topPos && y<topPos+imageHeight) {
            scroll=Math.max(0,Math.min(menu.offerCount()-ROWS,scroll+(delta<0?1:-1)));return true;
        }
        return super.mouseScrolled(x,y,delta);
    }
    @Override public void render(PoseStack pose,int mouseX,int mouseY,float partialTick) {
        renderBackground(pose);super.render(pose,mouseX,mouseY,partialTick);renderTooltip(pose,mouseX,mouseY);
        int row=(mouseY-topPos-18)/ROW_HEIGHT,index=scroll+row;
        if(mouseX>=leftPos+5 && mouseX<leftPos+93 && mouseY>=topPos+18 && row<ROWS && index<menu.offerCount())
            renderTooltip(pose,menu.offerResult(index),mouseX,mouseY);
    }
}
