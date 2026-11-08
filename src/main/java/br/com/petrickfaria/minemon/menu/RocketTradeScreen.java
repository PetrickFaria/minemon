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
    private int dragGrabOffset;

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
        int thumbY=thumbTop(max);
        GuiComponent.fill(pose,leftPos+95,topPos+18,leftPos+101,topPos+158,0xFF777777);
        MinemonGui.panel(pose,leftPos+95,thumbY,6,27);
        renderVanillaExperienceBar(pose);
    }
    /** Uses the same 1.19.2 vanilla MerchantScreen atlas region and native 102x5 bar. */
    private void renderVanillaExperienceBar(PoseStack pose) {
        int x = leftPos + 136;
        int y = topPos + 16;
        int filled = progressPixels(menu.progressionLevel(), menu.tradesThisLevel(), 102);
        GuiComponent.blit(pose, x, y, 0, 186, 102, 5, 512, 256);
        if (filled > 0) GuiComponent.blit(pose, x, y, 0, 191, filled, 5, 512, 256);
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
            int thumbY=thumbTop(Math.max(0,menu.offerCount()-ROWS));
            dragging=true;
            dragGrabOffset=(y>=thumbY&&y<thumbY+27)?(int)y-thumbY:13;
            setScroll(y); return true;
        }
        return super.mouseClicked(x,y,button);
    }
    private void setScroll(double y) {
        int max=Math.max(0,menu.offerCount()-ROWS);
        int thumbTop=net.minecraft.util.Mth.clamp((int)Math.round(y-topPos-18-dragGrabOffset),0,113);
        scroll=Math.round((float)thumbTop*max/113.0F);
        scroll=net.minecraft.util.Mth.clamp(scroll,0,max);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy) {
        if(dragging){setScroll(y);return true;}
        return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x,double y,int button) {
        if(button==0&&dragging){dragging=false;return true;}
        return super.mouseReleased(x,y,button);
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
    private int thumbTop(int max){return topPos+18+(max==0?0:113*scroll/max);}
    public int scrollOffsetForSmokeTest(){return scroll;}
    public boolean scrollWheelForSmokeTest(double delta){return mouseScrolled(leftPos+97,topPos+80,delta);}
    public boolean dragScrollbarForSmokeTest(boolean clickTrack,boolean dragOutside){
        int max=Math.max(0,menu.offerCount()-ROWS),before=scroll;
        double x=leftPos+97,y=clickTrack?topPos+157:thumbTop(max)+13;
        boolean clicked=mouseClicked(x,y,0);int afterClick=scroll;
        if(dragging)mouseDragged(x,topPos+(dragOutside?-30:188),0,0,0);
        boolean released=mouseReleased(x,y,0);
        return clicked&&released&&!dragging&&scroll>=0&&scroll<=max&&(max==0||scroll!=before||scroll!=afterClick);
    }
}
