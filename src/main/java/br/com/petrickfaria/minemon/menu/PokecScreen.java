package br.com.petrickfaria.minemon.menu;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.entity.SpeciesDefinitions;
import br.com.petrickfaria.minemon.network.MinemonNetwork;
import br.com.petrickfaria.minemon.pokec.*;
import br.com.petrickfaria.minemon.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.*;

/** Page-based 3D Pokédex view. Only the current 18 previews are instantiated, never added to a level. */
public final class PokecScreen extends AbstractContainerScreen<PokecMenu> {
    private static final int PER_PAGE=18,COLS=6,CELL_W=55,CELL_H=57;
    private static volatile int syncedCoins;
    private static volatile boolean syncedFreeAvailable=true;
    private static volatile Set<String> syncedUnlocked=Set.of("bulbasaur","charmander","squirtle","pikachu","eevee");
    private static volatile Map<String,Integer> syncedDeposits=Map.of();
    private static volatile Map<String,Integer> syncedPrices=Map.of();
    private static volatile Map<String,Integer> syncedRequirements=Map.of();
    private int page;
    private final Map<String,MinemonCreature> previews=new HashMap<>();
    private List<PokecSpecies.Entry> ordered;
    public PokecScreen(PokecMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=370;imageHeight=242;titleLabelX=10;titleLabelY=7;inventoryLabelY=10000;}
    public static void updateState(int coins,boolean freeAvailable,Set<String> unlocked,Map<String,Integer> deposits,Map<String,Integer> prices,Map<String,Integer> requirements){syncedCoins=coins;syncedFreeAvailable=freeAvailable;syncedUnlocked=Set.copyOf(unlocked);syncedDeposits=Map.copyOf(deposits);syncedPrices=Map.copyOf(prices);syncedRequirements=Map.copyOf(requirements);}
    @Override protected void init(){super.init();ordered=PokecSpecies.ordered();MinemonNetwork.requestPokecState(menu.pos());}
    @Override public boolean isPauseScreen(){return false;}
    @Override protected void renderBg(PoseStack g,float partial,int mouseX,int mouseY){
        int x=leftPos,y=topPos;
        GuiComponent.fill(g,x,y,x+imageWidth,y+imageHeight,0xF20B111B);GuiComponent.fill(g,x+3,y+3,x+imageWidth-3,y+imageHeight-3,0xFF172432);
        GuiComponent.fill(g,x+6,y+24,x+imageWidth-6,y+26,0xFF2487C8);
        font.draw(g,Component.translatable("gui.minemon_gen1.pokec.coins",syncedCoins),x+230,y+8,0x75D8FF);
        font.draw(g,Component.translatable("gui.minemon_gen1.pokec.free",syncedFreeAvailable?"1":"—"),x+10,y+imageHeight-13,0xFFE39A);
        int start=page*PER_PAGE;
        for(int i=0;i<PER_PAGE&&start+i<ordered.size();i++){
            PokecSpecies.Entry species=ordered.get(start+i);int col=i%COLS,row=i/COLS;int bx=x+8+col*CELL_W,by=y+31+row*CELL_H;
            boolean unlocked=syncedUnlocked.contains(species.slug());boolean evolution=species.isEvolution();
            int border=unlocked?0xFF2BA7E3:evolution?0xFF805765:0xFF357AA0;
            GuiComponent.fill(g,bx,by,bx+CELL_W-3,by+CELL_H-3,border);GuiComponent.fill(g,bx+1,by+1,bx+CELL_W-4,by+CELL_H-4,unlocked?0xFF102D45:0xFF0D1E2B);
            renderPreview(g,species,bx+CELL_W/2-1,by+29,partial);
            if(evolution&&!unlocked)font.drawCenteredString(g,"X",bx+CELL_W/2-1,by+14,0xFFE95865);
            int textColor=unlocked?0xFFE8F7FF:0xFFB1C2D1;
            font.drawCenteredString(g,String.format(Locale.ROOT,"#%03d",species.number()),bx+CELL_W/2-1,by+3,0x68CFFF);
            String label=species.name();float labelScale=Math.min(1.0F,(CELL_W-5.0F)/Math.max(1,font.width(label)));
            g.pushPose();g.translate(bx+CELL_W/2-1,by+CELL_H-16,0);g.scale(labelScale,labelScale,1);font.drawCenteredString(g,label,0,0,textColor);g.popPose();
            String cost;
            if(unlocked){cost=(syncedFreeAvailable?"FREE ":"")+syncedPrices.getOrDefault(species.slug(),PokecEconomy.get().coins(species.slug()))+" C";}
            else if(evolution)cost="✕  "+Component.translatable("gui.minemon_gen1.pokec.evolve").getString();
            else cost="+ "+syncedDeposits.getOrDefault(species.slug(),0)+"/"+syncedRequirements.getOrDefault(species.slug(),PokecEconomy.get().souls(species.slug()))+" "+species.primaryType().toUpperCase(Locale.ROOT);
            GuiComponent.fill(g,bx+3,by+CELL_H-13,bx+14,by+CELL_H-3,0xFF7FD9FF);GuiComponent.fill(g,bx+4,by+CELL_H-12,bx+13,by+CELL_H-4,0xFF0D1E2B);
            font.drawCenteredString(g,unlocked?"C":evolution?"X":"+",bx+8,by+CELL_H-11,unlocked?0xFFFFD76A:evolution?0xFFFF7E86:0xFF75CFF2);
            float costScale=Math.min(1.0F,(CELL_W-19.0F)/Math.max(1,font.width(cost)));
            g.pushPose();g.translate(bx+16,by+CELL_H-10,0);g.scale(costScale,costScale,1);font.draw(g,cost,0,0,unlocked?0xFFFFD76A:evolution?0xFFFF7E86:0xFF75CFF2);g.popPose();
        }
        GuiComponent.fill(g,x+8,y+imageHeight-23,x+imageWidth-8,y+imageHeight-21,0xFF24465B);
        font.draw(g,Component.translatable("gui.minemon_gen1.pokec.page",page+1,(ordered.size()+PER_PAGE-1)/PER_PAGE),x+imageWidth/2-15,y+imageHeight-18,0xD8E9F4);
        font.draw(g,Component.literal("‹"),x+imageWidth-41,y+imageHeight-20,0xFFFFFFFF);
        font.draw(g,Component.literal("›"),x+imageWidth-20,y+imageHeight-20,0xFFFFFFFF);
    }
    private void renderPreview(PoseStack g,PokecSpecies.Entry species,int x,int y,float partial){
        Minecraft mc=Minecraft.getInstance();if(mc.level==null)return;
        MinemonCreature entity=previews.computeIfAbsent(species.slug(),slug->{var type=ModEntities.SPECIES.get(slug);if(type==null)return null;MinemonCreature e=type.get().create(mc.level);if(e!=null)e.setNoAi(true);return e;});
        if(entity==null)return;
        entity.tickCount=(int)(System.currentTimeMillis()/50L);entity.setYRot((float)((entity.tickCount*1.1)%360));entity.yRotO=entity.getYRot();
        var def=SpeciesDefinitions.get(species.slug());float span=Math.max(def.height(),def.width());float scale=Math.min(20.0F/Math.max(span,0.2F),2.2F);
        PoseStack pose=g;pose.pushPose();pose.translate(x,y,120);pose.scale(scale,-scale,scale);pose.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));pose.mulPose(Axis.XP.rotationDegrees(-6));
        var dispatcher=mc.getEntityRenderDispatcher();dispatcher.setRenderShadow(false);
        try{dispatcher.render(entity,0,0,0,0,partial,pose,mc.renderBuffers().bufferSource(),15728880);mc.renderBuffers().bufferSource().endBatch();}
        catch(RuntimeException ex){/* A bad preview must not interrupt navigation through the Pokédex. */}
        finally{dispatcher.setRenderShadow(true);pose.popPose();}
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button==0&&mx>=leftPos+8&&mx<leftPos+8+COLS*CELL_W&&my>=topPos+31&&my<topPos+31+CELL_H*3){
            int col=(int)(mx-leftPos-8)/CELL_W,row=(int)(my-topPos-31)/CELL_H;
            if(col>=0&&col<COLS&&row>=0&&row<3){int index=page*PER_PAGE+row*COLS+col;if(index<ordered.size()){
                var species=ordered.get(index);boolean unlocked=syncedUnlocked.contains(species.slug());
                if(unlocked)MinemonNetwork.performPokecAction(menu.pos(),species.slug(),PokecTransactions.Action.PURCHASE);
                else if(!species.isEvolution())MinemonNetwork.performPokecAction(menu.pos(),species.slug(),PokecTransactions.Action.DEPOSIT);
                return true;
            }}
        }
        if(button==0&&my>=topPos+imageHeight-23){if(mx>=leftPos+imageWidth-48&&mx<leftPos+imageWidth-26){page=Math.max(0,page-1);previews.clear();return true;}if(mx>=leftPos+imageWidth-26){page=Math.min((ordered.size()-1)/PER_PAGE,page+1);previews.clear();return true;}}
        return super.mouseClicked(mx,my,button);
    }
    @Override public void render(PoseStack g,int mouseX,int mouseY,float partial){renderBackground(g);super.render(g,mouseX,mouseY,partial);}
}
