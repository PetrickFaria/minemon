package br.com.petrickfaria.minemon.menu;

import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.entity.SpeciesDefinitions;
import br.com.petrickfaria.minemon.network.MinemonNetwork;
import br.com.petrickfaria.minemon.pokec.*;
import br.com.petrickfaria.minemon.registry.ModEntities;
import br.com.petrickfaria.minemon.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;

import java.util.*;

/** Pokédex selector and server-authoritative purchase/unlock panel. */
public final class PokecScreen extends AbstractContainerScreen<PokecMenu> {
    private static final int LOGICAL_WIDTH = 640, TOP_HEIGHT = 210, INVENTORY_HEIGHT = 100;
    private static final int LIST_X = 8, LIST_Y = 27, LIST_W = 136, LIST_H = 133, ROW_H = 11;
    private static final int INVENTORY_SLOT_WIDTH = 9 * 18, INVENTORY_LABEL_Y = 4;
    private static final int INVENTORY_MAIN_Y = 18, INVENTORY_HOTBAR_Y = 76;
    private static volatile int syncedCoins;
    private static volatile boolean syncedFreeAvailable = true;
    // Empty until the server sends the current player's real unlock state.
    private static volatile Set<String> syncedUnlocked = Set.of();
    private static volatile Map<String, Integer> syncedDeposits = Map.of();
    private static volatile Map<String, Integer> syncedPrices = Map.of();
    private static volatile Map<String, Integer> syncedRequirements = Map.of();
    private static volatile int syncedStagedCoins, syncedStagedSouls;
    private static volatile String syncedSelectedSlug = "bulbasaur";
    private static volatile ItemStack syncedConversionBall = ItemStack.EMPTY, syncedConversionOutput = ItemStack.EMPTY;
    private static volatile int syncedConversionResult = -1, syncedConversionLevel, syncedConversionBase, syncedConversionMinimum, syncedConversionMaximum;
    private static volatile String syncedConversionSpecies = "", syncedConversionRarity = "", syncedConversionStage = "", syncedConversionType = "";
    private static volatile boolean syncedConversionConfirming;

    private final Map<net.minecraft.resources.ResourceLocation,net.minecraft.resources.ResourceLocation> ghostTextures=new HashMap<>();
    private float uiScale = 1.0F;
    private boolean draggingPreview;
    private boolean draggingListScrollbar;
    private int listScrollbarGrabOffset;
    private float previewYaw=23.0F,previewPitch=-4.0F,targetPreviewYaw=23.0F,targetPreviewPitch=-4.0F;
    private final java.util.Map<String,Integer> previewMeshFrames=new java.util.HashMap<>();
    private final java.util.Set<String> calibratedPreviews=new java.util.HashSet<>();
    private final java.util.Set<String> rasterFramedPreviews=new java.util.HashSet<>();
    private final java.util.Map<String,String> previewCalibrationDiagnostics=new java.util.HashMap<>();
    private boolean lastPreviewFrameWithinTarget;
    private boolean lastPreviewFrameOutsideTarget;
    public boolean previewCalibratedForSmokeTest(String slug){return rasterFramedPreviews.contains(slug);}
    public String previewCalibrationDiagnosticsForSmokeTest(String slug){
        return previewCalibrationDiagnostics.getOrDefault(slug,"no idle mesh measurement recorded");
    }
    private PreviewBounds.Fit frameVisiblePixels(PreviewBounds.Fit current){
        lastPreviewFrameWithinTarget=false;
        lastPreviewFrameOutsideTarget=false;
        var mc=Minecraft.getInstance();double pixels=uiScale*mc.getWindow().getGuiScale();
        int left=(int)((leftPos+155*uiScale)*mc.getWindow().getGuiScale());
        int top=(int)((topPos+46*uiScale)*mc.getWindow().getGuiScale());
        int right=(int)(left+226*pixels),bottom=(int)(top+84*pixels);
        try(var frame=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            int bg=frame.getPixelRGBA(left,top),minX=right,minY=bottom,maxX=left,maxY=top,count=0;
            for(int py=top;py<bottom;py++)for(int px=left;px<right;px++){
                int c=frame.getPixelRGBA(px,py);
                int difference=Math.max(Math.abs((c&255)-(bg&255)),Math.max(Math.abs(((c>>>8)&255)-((bg>>>8)&255)),Math.abs(((c>>>16)&255)-((bg>>>16)&255))));
                if(difference<18)continue;
                minX=Math.min(minX,px);maxX=Math.max(maxX,px);minY=Math.min(minY,py);maxY=Math.max(maxY,py);count++;
            }
            double extent=Math.max(maxX-minX,maxY-minY);
            if(count<10||extent<1)return null;
            double target=48*pixels;
            double dx=(minX+maxX)*.5-(leftPos+268*uiScale)*mc.getWindow().getGuiScale();
            double dy=(minY+maxY)*.5-(topPos+88*uiScale)*mc.getWindow().getGuiScale();
            lastPreviewFrameWithinTarget=extent>=target*.65&&extent<=target*1.30
                    &&Math.abs(dx)<=target*.25&&Math.abs(dy)<=target*.25;
            lastPreviewFrameOutsideTarget=!lastPreviewFrameWithinTarget;
            double rx=dx/(pixels*current.scale()),ry=-dy/(pixels*current.scale());
            double yaw=Math.toRadians(previewYaw),pitch=Math.toRadians(previewPitch-6);
            double y=Math.cos(pitch)*ry,z=-Math.sin(pitch)*ry;
            return new PreviewBounds.Fit((float)(current.scale()*48*pixels/extent),
                    current.x()+Math.cos(yaw)*rx-Math.sin(yaw)*z,current.y()+y,
                    current.z()+Math.sin(yaw)*rx+Math.cos(yaw)*z);
        }
    }
    public void verifyVisiblePreviewForSmokeTest(String slug){
        if(!calibratedPreviews.contains(slug))throw new IllegalStateException("Preview not calibrated: "+slug);
        var mc=Minecraft.getInstance();double pixels=uiScale*mc.getWindow().getGuiScale();
        int left=(int)((leftPos+155*uiScale)*mc.getWindow().getGuiScale());
        int top=(int)((topPos+46*uiScale)*mc.getWindow().getGuiScale());
        int right=(int)(left+226*pixels),bottom=(int)(top+84*pixels);
        try(var frame=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            int bg=frame.getPixelRGBA(left,top),minX=right,minY=bottom,maxX=left,maxY=top,count=0;
            for(int py=top;py<bottom;py++)for(int px=left;px<right;px++){
                int c=frame.getPixelRGBA(px,py);
                int difference=Math.max(Math.abs((c&255)-(bg&255)),Math.max(Math.abs(((c>>>8)&255)-((bg>>>8)&255)),Math.abs(((c>>>16)&255)-((bg>>>16)&255))));
                if(difference<18)continue;
                minX=Math.min(minX,px);maxX=Math.max(maxX,px);minY=Math.min(minY,py);maxY=Math.max(maxY,py);count++;
            }
            double extent=Math.max(maxX-minX,maxY-minY),target=48*pixels;
            double centerX=(leftPos+268*uiScale)*mc.getWindow().getGuiScale();
            double centerY=(topPos+88*uiScale)*mc.getWindow().getGuiScale();
            if(count<10||extent<target*.65||extent>target*1.30
                    ||Math.abs((minX+maxX)*.5-centerX)>target*.25
                    ||Math.abs((minY+maxY)*.5-centerY)>target*.25)
                throw new IllegalStateException("Visible preview framing failed: "+slug+" extent="+extent+" target="+target
                        +" bounds="+minX+","+minY+".."+maxX+","+maxY+" center="+centerX+","+centerY);
        }
    }
    private int scrollOffset;
    private String selectedSlug = "bulbasaur";
    private Map<String, MinemonCreature> previews = new HashMap<>();
    private final Map<String, PreviewBounds.Fit> previewFits = new HashMap<>();
    private List<PokecSpecies.Entry> ordered = List.of();

    public PokecScreen(PokecMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        titleLabelX = 10000;
        titleLabelY = 10000;
        inventoryLabelY = 10000;
    }

    public static void updateState(int coins, boolean freeAvailable, Set<String> unlocked,
                                   Map<String, Integer> deposits, Map<String, Integer> prices,
                                   Map<String, Integer> requirements, String selected, int stagedCoins, int stagedSouls,
                                   ItemStack conversionBall, ItemStack conversionOutput, int conversionResult,
                                   String conversionSpecies, String conversionRarity, String conversionStage,
                                   String conversionType, int conversionLevel, int conversionBase,
                                   int conversionMinimum, int conversionMaximum, boolean conversionConfirming) {
        syncedCoins = coins;
        syncedFreeAvailable = freeAvailable;
        syncedUnlocked = Set.copyOf(unlocked);
        syncedDeposits = Map.copyOf(deposits);
        syncedPrices = Map.copyOf(prices);
        syncedRequirements = Map.copyOf(requirements);
        syncedSelectedSlug = selected;
        syncedStagedCoins = stagedCoins;
        syncedStagedSouls = stagedSouls;
        syncedConversionBall = conversionBall.copy(); syncedConversionOutput = conversionOutput.copy();
        syncedConversionResult = conversionResult; syncedConversionSpecies = conversionSpecies;
        syncedConversionRarity = conversionRarity; syncedConversionStage = conversionStage;
        syncedConversionType = conversionType; syncedConversionLevel = conversionLevel;
        syncedConversionBase = conversionBase; syncedConversionMinimum = conversionMinimum; syncedConversionMaximum = conversionMaximum;
        syncedConversionConfirming = conversionConfirming;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PokecScreen screen) {
            screen.selectedSlug = selected;
        }
    }

    /** Exposed for the focused client smoke check; the catalog remains sourced from PokecSpecies. */
    public int catalogSizeForSmokeTest() { return ordered.size(); }
    public boolean clickSpeciesForSmokeTest(String slug) {
        int index = -1;
        for (int i = 0; i < ordered.size(); i++) if (ordered.get(i).slug().equals(slug)) { index = i; break; }
        if (index < 0) return false;
        scrollOffset = Math.min(Math.max(0, ordered.size() - visibleRows()), index);
        int row = index - scrollOffset;
        double x = leftPos + (LIST_X + 24) * uiScale;
        double y = topPos + (LIST_Y + 13 + row * ROW_H + 2) * uiScale;
        return mouseClicked(x, y, 0) && selectedSlug.equals(slug);
    }
    /** Changes only the preview selection for stress testing repeated model/controller initialization. */
    public boolean selectPreviewSpeciesForSmokeTest(String slug) {
        if (PokecSpecies.get(slug) == null) return false;
        if (!slug.equals(selectedSlug)) {
            selectedSlug = slug;
            draggingPreview = false;
            previewYaw = targetPreviewYaw = 23.0F;
            previewPitch = targetPreviewPitch = -4.0F;
            previews.clear();
        }
        return true;
    }
    public boolean selectedSpeciesForSmokeTest(String slug) { return selectedSlug.equals(slug); }
    public boolean previewExistsForSmokeTest(String slug) { return previews.containsKey(slug); }
    public boolean speciesUnlockedForSmokeTest(String slug) { return syncedUnlocked.contains(slug); }
    public boolean dragPreviewForSmokeTest(){
        double x=leftPos+268*uiScale,y=topPos+88*uiScale;
        if(!mouseClicked(x,y,0))return false;
        mouseDragged(x+20*uiScale,y+10*uiScale,0,20*uiScale,10*uiScale);
        mouseReleased(x,y,0);
        boolean moved=targetPreviewYaw!=0&&targetPreviewPitch!=0;
        previewYaw=targetPreviewYaw=23.0F;previewPitch=targetPreviewPitch=-4.0F;
        return moved&&!draggingPreview;
    }
    public boolean conversionPanelFitsAboveInventoryForSmokeTest() { return menu.slots.get(38).x > menu.slots.get(8).x && menu.slots.get(39).y + 18 <= imageHeight; }
    public boolean inventorySlotsForSmokeTest() {
        if (menu.slots.size() != 40) return false;
        int x = Math.round(276 * uiScale) - INVENTORY_SLOT_WIDTH / 2;
        int mainY = Math.round(TOP_HEIGHT * uiScale) + INVENTORY_MAIN_Y;
        int hotbarY = Math.round(TOP_HEIGHT * uiScale) + INVENTORY_HOTBAR_Y;
        for (int i = 0; i < 36; i++) {
            var slot = menu.slots.get(i);
            int expectedX = x + (i % 9) * 18;
            int expectedY = i < 27 ? mainY + (i / 9) * 18 : hotbarY;
            if (slot.x != expectedX || slot.y != expectedY) return false;
        }
        return hotbarY + 18 <= imageHeight;
    }
    public float previewScaleForSmokeTest(String slug) {
        var definition = SpeciesDefinitions.get(slug);
        return definition == null ? 0.0F : PreviewBounds.fit(slug, definition.scale()).scale();
    }
    public double previewNormalizedSizeForSmokeTest(String slug){
        var definition=SpeciesDefinitions.get(slug);
        return definition==null?0:PreviewBounds.normalizedLargestExtent(slug,definition.scale());
    }
    public String previewAnimationForSmokeTest(String slug) {
        var entity=previews.get(slug);
        return entity==null?null:entity.getSelectedAnimationForValidation();
    }
    public int listScrollOffsetForSmokeTest(){return scrollOffset;}
    public int listScrollMaxForSmokeTest(){return Math.max(0,ordered.size()-visibleRows());}
    public float listUiScaleForSmokeTest(){return uiScale;}
    public boolean scrollListForSmokeTest(double delta){
        return mouseScrolled(leftPos+(LIST_X+20)*uiScale,topPos+(LIST_Y+25)*uiScale,delta);
    }
    public float[] previewOrientationForSmokeTest(){return new float[]{previewYaw,previewPitch};}
    public boolean dragListScrollbarForSmokeTest(boolean clickTrack,boolean dragPastTrack){
        int trackX=PokecScrollbarGeometry.X,trackY=PokecScrollbarGeometry.Y,trackHeight=visibleRows()*ROW_H;
        int thumbHeight=listThumbHeight(),thumbY=listThumbTop(trackY,trackHeight,thumbHeight);
        double x=leftPos+(trackX+1)*uiScale;
        double y=topPos+(clickTrack?trackY+trackHeight-1:thumbY+Math.max(0,thumbHeight/2))*uiScale;
        int before=scrollOffset;String selectedBefore=selectedSlug;
        boolean clicked=mouseClicked(x,y,0);
        int afterClick=scrollOffset;
        if(draggingListScrollbar)mouseDragged(x,topPos+(trackY+(dragPastTrack?-30:trackHeight+30))*uiScale,0,0,0);
        boolean released=mouseReleased(x,y,0);
        return clicked&&released&&!draggingListScrollbar&&scrollOffset>=0
                &&scrollOffset<=Math.max(0,ordered.size()-visibleRows())
                &&selectedSlug.equals(selectedBefore)
                &&(ordered.size()<=visibleRows()||scrollOffset!=afterClick||scrollOffset!=before);
    }

    @Override protected void init() {
        float availableHeight = Math.max(1.0F, height - 8.0F - INVENTORY_HEIGHT);
        uiScale = Math.min(1.0F, Math.min((width - 8.0F) / LOGICAL_WIDTH, availableHeight / TOP_HEIGHT));
        uiScale = Math.max(0.1F, uiScale);
        imageWidth = Math.round(LOGICAL_WIDTH * uiScale);
        imageHeight = Math.round(TOP_HEIGHT * uiScale) + INVENTORY_HEIGHT;
        super.init();
        int inventoryX = Math.round(276 * uiScale) - INVENTORY_SLOT_WIDTH / 2;
        int inventoryTop = Math.round(TOP_HEIGHT * uiScale);
        menu.layoutPlayerInventory(inventoryX, inventoryTop + INVENTORY_MAIN_Y, inventoryTop + INVENTORY_HOTBAR_Y);
        menu.terminal.layout(Math.round(420*uiScale),Math.round(55*uiScale),Math.round(550*uiScale),Math.round(55*uiScale),
            Math.round(420*uiScale),Math.round(244*uiScale),Math.round(550*uiScale),Math.round(244*uiScale));
        ordered = PokecSpecies.ordered();
        selectedSlug = PokecSpecies.get(syncedSelectedSlug) == null ? "bulbasaur" : syncedSelectedSlug;
        MinemonNetwork.requestPokecState(menu.pos());
        MinemonNetwork.performPokecAction(menu.pos(), selectedSlug, PokecTransactions.Action.SELECT);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override protected void renderLabels(PoseStack pose, int mouseX, int mouseY) { }

    @Override protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY) {
        pose.pushPose();
        pose.translate(leftPos, topPos, 0);
        pose.pushPose();
        pose.scale(uiScale, uiScale, 1);
        MinemonGui.panel(pose, 0, 0, LOGICAL_WIDTH, totalHeight());
        font.draw(pose, "POKÉC  /  POKÉDEX", 10, 9, 0xFF404040);
        font.draw(pose, "Saldo: " + syncedCoins + " PokéCoins", 420, 9, 0xFF705028);
        GuiComponent.fill(pose, 8, 20, LOGICAL_WIDTH - 8, 21, 0xFF9A4545);

        drawPanel(pose, LIST_X, LIST_Y - 5, LIST_W, totalHeight() - LIST_Y - 2);
        drawPanel(pose, 150, 24, 248, TOP_HEIGHT - 28);
        drawPanel(pose, 410, 24, 222, 118);
        drawPanel(pose, 410, 146, 222, totalHeight()-154);
        font.draw(pose, "ESPÉCIES   001–151", LIST_X + 5, LIST_Y - 1, 0xFF404040);
        int visible = visibleRows();
        for (int row = 0; row < visible; row++) {
            int index = scrollOffset + row;
            if (index >= ordered.size()) break;
            PokecSpecies.Entry species = ordered.get(index);
            int y = LIST_Y + 12 + row * ROW_H;
            boolean selected = species.slug().equals(selectedSlug);
            boolean unlocked = syncedUnlocked.contains(species.slug());
            GuiComponent.fill(pose, LIST_X + 4, y, LIST_X + LIST_W - 4, y + ROW_H - 1,
                    selected ? 0xFFB3C3CF : (row % 2 == 0 ? 0xFFB7B7B7 : 0xFFAEAEAE));
            if (selected) GuiComponent.fill(pose, LIST_X + 4, y, LIST_X + 6, y + ROW_H - 1, 0xFF404040);
            int color = unlocked ? 0xFF202020 : 0xFF555555;
            font.draw(pose, fitText(String.format(Locale.ROOT, "%03d %s", species.number(), species.name()), LIST_W - 20), LIST_X + 9, y + 1, color);
        }
        int maxScroll = Math.max(0, ordered.size() - visible);
        int trackX = PokecScrollbarGeometry.X, trackY = PokecScrollbarGeometry.Y;
        GuiComponent.fill(pose, trackX, trackY, trackX + 2, trackY + visible * ROW_H, 0xFFC6C6C6);
        int thumbHeight = listThumbHeight();
        int thumbY = listThumbTop(trackY,visible*ROW_H,thumbHeight);
        GuiComponent.fill(pose, trackX, thumbY, trackX + 2, thumbY + thumbHeight, 0xFF404040);
        drawSelectedPanel(pose, partialTick, mouseX, mouseY);
        pose.popPose();

        int inventoryTop = Math.round(TOP_HEIGHT * uiScale);
        drawPanel(pose, Math.round(276*uiScale)-INVENTORY_SLOT_WIDTH/2-2, inventoryTop, INVENTORY_SLOT_WIDTH+4, INVENTORY_HEIGHT);
        font.draw(pose, "INVENTÁRIO", Math.round(276*uiScale)-INVENTORY_SLOT_WIDTH/2+2, inventoryTop + INVENTORY_LABEL_Y, 0xFF404040);
        for (int i=0;i<menu.slots.size();i++) {
            var slot=menu.slots.get(i);
            if(!slot.isActive())continue;
            if(i==37&&!syncedUnlocked.contains(selectedSlug))continue;
            MinemonGui.slot(pose, slot.x-1, slot.y-1, 18, 18);
        }
        drawGhostHints(pose);
        pose.popPose();
    }

    private int totalHeight(){return Math.round(imageHeight/uiScale);}
    private int visibleRows() { return Math.max(1, (totalHeight() - LIST_Y - 22) / ROW_H); }
    private void drawPanel(PoseStack pose, int x, int y, int w, int h) {
        MinemonGui.slot(pose, x, y, w, h);
    }

    private void drawSelectedPanel(PoseStack pose, float partialTick, int mouseX, int mouseY) {
        PokecSpecies.Entry species = PokecSpecies.get(selectedSlug);
        if (species == null) return;
        font.draw(pose, String.format(Locale.ROOT, "%03d  %s", species.number(), fitText(species.name(), 145)), 155, 28, 0xFF404040);
        drawRightAligned(pose, rarityLabel(PokecEconomy.get().rarity(species.slug())), 379, 28, 0xFF404040);
        boolean unlocked = syncedUnlocked.contains(species.slug());
        if (unlocked) {
            drawPanel(pose, 152, 44, 232, 88);
            renderPreview(pose, species, 268, 88, partialTick);
        } else {
            drawPanel(pose, 152, 44, 232, 88);
            GuiComponent.fill(pose, 154, 46, 382, 130, 0xFFC6C6C6);
            drawCentered(pose, "?", 268, 61, 0xFF404040);
            drawCentered(pose, "ESPÉCIE BLOQUEADA", 268, 105, 0xFF404040);
        }
        // Retire the preview's depth before text render types enable depth testing again.
        com.mojang.blaze3d.systems.RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        pose.pushPose();
        pose.translate(0,0,300);
        GuiComponent.fill(pose, 154, 136, 382, 137, 0xFFC6C6C6);
        font.draw(pose, "VIDA", 191, 139, 0xFF404040);
        font.draw(pose, "DANO", 316, 139, 0xFF404040);
        String rarity=PokecEconomy.get().rarity(species.slug());
        int level=1;
        drawHealthHearts(pose,(int)Math.ceil(br.com.petrickfaria.minemon.entity.PokemonProgression.healthForRarity(rarity,level)),191,149);
        drawDamageHearts(pose,br.com.petrickfaria.minemon.entity.PokemonProgression.damageForRarity(rarity,level),316,149);
        font.draw(pose,"Estágio: "+evolutionStage(species),154,176,0xFF404040);
        font.draw(pose,"Nível: "+level,302,176,0xFF404040);
        font.draw(pose,"Raridade: "+rarityLabel(rarity),154,188,0xFF404040);
        String types=species.types().stream().map(type->{var soul=SoulType.fromSpeciesType(type);return soul==null?type:soulTypeLabel(soul.name());}).collect(java.util.stream.Collectors.joining(" / "));
        font.draw(pose,fitText("Tipo: "+types,234),154,197,0xFF404040);
        drawAcquisitionPanel(pose, species, unlocked, species.isEvolution() && !unlocked, mouseX, mouseY);
        drawCompactConversion(pose);
        pose.popPose();
    }

    private void drawAcquisitionPanel(PoseStack pose,PokecSpecies.Entry species,boolean unlocked,boolean evolution,int mouseX,int mouseY){
        font.draw(pose,unlocked?"COMPRA":"DESBLOQUEIO",420,29,0xFF404040);
        if(evolution){font.draw(pose,"Libere por evolução",420,47,0xFF404040);return;}
        font.draw(pose,"Valor",420,43,0xFF404040);
        if(unlocked){
            font.draw(pose,"→",503,59,0xFF202020);
            drawButton(pose,420,87,95,19,"Comprar",true,mouseX,mouseY);
            boolean enabled=menu.terminal.spawnEnabled();
            drawButton(pose,420,114,63,19,"Spawn",true,mouseX,mouseY);
            GuiComponent.fill(pose,423,117,480,130,enabled?0xFF398348:0xFFAB3939);
            drawCentered(pose,"Spawn",451,120,0xFFFFFFFF);
            font.draw(pose,String.format(Locale.ROOT,"%.2f%%",enabled?menu.terminal.spawnChance()*100:0),491,120,0xFF404040);
        }else drawButton(pose,420,87,116,19,"Desbloquear",menu.terminal.payment().getCount()>0,mouseX,mouseY);
    }
    private void drawCompactConversion(PoseStack pose){
        font.draw(pose,"LIBERAÇÃO",420,152,0xFF404040);
        var stored=br.com.petrickfaria.minemon.item.PokeballItem.readStored(menu.terminal.ball());
        if(stored!=null){
            var species=PokecSpecies.get(stored.species());
            String rarity=PokecEconomy.get().rarity(stored.species());
            font.draw(pose,fitText(species==null?stored.species():species.name(),195),420,166,0xFF404040);
            font.draw(pose,"Vida:",420,180,0xFF404040);
            drawHealthHearts(pose,(int)Math.ceil(stored.health()),454,178);
            font.draw(pose,"Dano:",420,205,0xFF404040);
            drawDamageHearts(pose,br.com.petrickfaria.minemon.entity.PokemonProgression.damageForRarity(rarity,stored.level()),454,203);
            font.draw(pose,"Nível: "+stored.level()+"  "+rarityLabel(rarity),420,230,0xFF404040);
        }else font.draw(pose,"Insira uma bola com Pokémon",420,174,0xFF666666);
        font.draw(pose,"→",503,249,0xFF202020);
        drawButton(pose,420,279,116,19,"Liberar",stored!=null&&menu.slots.get(39).getItem().isEmpty(),-1,-1);
    }
    private void drawGhostHints(PoseStack pose){
        var species=PokecSpecies.get(selectedSlug);if(species==null)return;
        boolean unlocked=syncedUnlocked.contains(selectedSlug);
        net.minecraft.world.item.Item item=null;
        int remaining;
        if(unlocked){item=ModItems.POKE_COIN.get();remaining=syncedFreeAvailable?0:Math.max(0,syncedPrices.getOrDefault(selectedSlug,0)-syncedCoins);}
        else{var type=SoulType.fromSpeciesType(species.primaryType());if(type!=null&&ModItems.SOULS.containsKey(type.id()))item=ModItems.SOULS.get(type.id()).get();remaining=Math.max(0,syncedRequirements.getOrDefault(selectedSlug,0)-syncedDeposits.getOrDefault(selectedSlug,0));}
        if(!species.isEvolution()||unlocked){
            var input=menu.slots.get(36);
            if(input.getItem().isEmpty()&&item!=null)ghostItem(pose,new ItemStack(item),input.x,input.y);
            font.draw(pose,Integer.toString(Math.max(0,remaining-input.getItem().getCount())),input.x+23,input.y+4,0xFF404040);
        }
        var ball=menu.slots.get(38);if(ball.getItem().isEmpty())ghostItem(pose,new ItemStack(ModItems.POKEBALL.get()),ball.x,ball.y);
    }
    private void ghostItem(PoseStack pose,ItemStack stack,int x,int y){
        var key=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if(key==null)return;
        var texture=ghostTextures.computeIfAbsent(key,id->{
            var source=new net.minecraft.resources.ResourceLocation(id.getNamespace(),"textures/item/"+id.getPath()+".png");
            try(var stream=minecraft.getResourceManager().getResource(source).orElseThrow().open()){
                var pixels=com.mojang.blaze3d.platform.NativeImage.read(stream);
                for(int py=0;py<pixels.getHeight();py++)for(int px=0;px<pixels.getWidth();px++){
                    int color=pixels.getPixelRGBA(px,py);
                    int gray=((color&255)*299+((color>>>8)&255)*587+((color>>>16)&255)*114)/1000;
                    pixels.setPixelRGBA(px,py,(color&0xFF000000)|(gray<<16)|(gray<<8)|gray);
                }
                return minecraft.getTextureManager().register("pokec_hint",new net.minecraft.client.renderer.texture.DynamicTexture(pixels));
            }catch(java.io.IOException|java.util.NoSuchElementException error){return null;}
        });
        if(texture==null)return;
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0,texture);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,.45F);
        GuiComponent.blit(pose,x,y,0,0F,0F,16,16,16,16);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }
    @Override public void removed(){
        for(var texture:ghostTextures.values())minecraft.getTextureManager().release(texture);
        ghostTextures.clear();super.removed();
    }
    private void drawDamageHearts(PoseStack pose,double damage,int x,int y){
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(.22F,.22F,.22F,1);
        drawHealthHearts(pose,(int)Math.ceil(damage),x,y);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);
    }

    private void drawCompactItemSlot(PoseStack pose, int x, int y, net.minecraft.world.item.Item item, int count) {
        MinemonGui.slot(pose, x, y, 20, 20);
        if (item == null) return;
        ItemStack stack = new ItemStack(item);
        Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack, x + 2, y + 2);
        if (count > 1) font.draw(pose, Integer.toString(count), x + 11, y + 11, 0xFF303030);
    }

    private void drawRightAligned(PoseStack pose, String text, int rightX, int y, int color) {
        font.draw(pose, text, rightX - font.width(text), y, color);
    }

    private String fitText(String text, int maxWidth) {
        String value = text == null ? "" : text;
        while (!value.isEmpty() && font.width(value) > maxWidth) value = value.substring(0, value.length() - 1);
        return value;
    }

    private void drawConversionSlot(PoseStack pose, int x, int y, ItemStack stack, boolean input) {
        int accent = stack.isEmpty() ? 0xFFC6C6C6 : 0xFF404040;
        MinemonGui.slot(pose, x, y, 36, 36);
        if (!stack.isEmpty()) Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack, x + 10, y + 9);
        else if (input) {
            GuiComponent.fill(pose, x + 11, y + 7, x + 25, y + 10, 0xFFC6C6C6);
            GuiComponent.fill(pose, x + 8, y + 10, x + 28, y + 17, 0xFF404040);
            GuiComponent.fill(pose, x + 6, y + 17, x + 30, y + 22, 0xFFC6C6C6);
            GuiComponent.fill(pose, x + 8, y + 22, x + 28, y + 28, 0xFF404040);
            GuiComponent.fill(pose, x + 11, y + 28, x + 25, y + 30, 0xFFC6C6C6);
            GuiComponent.fill(pose, x + 15, y + 14, x + 21, y + 23, 0xFFC6C6C6);
            GuiComponent.fill(pose, x + 16, y + 16, x + 20, y + 21, 0xFF404040);
        } else drawCentered(pose, "→", x + 18, y + 13, 0xFFC6C6C6);
        if (!stack.isEmpty() && stack.getCount() > 1) font.draw(pose, Integer.toString(stack.getCount()), x + 27, y + 25, 0xFF303030);
    }

    private String conversionStageLabel(String stage) {
        return switch (stage) { case "INITIAL" -> "Inicial"; case "INTERMEDIATE" -> "Intermediário"; case "FINAL" -> "Final"; default -> "Final"; };
    }

    private String soulTypeLabel(String type) {
        return switch (type) {
            case "NORMAL" -> "Normal"; case "FIRE" -> "Fogo"; case "WATER" -> "Água"; case "GRASS" -> "Grama";
            case "ELECTRIC" -> "Elétrica"; case "ICE" -> "Gelo"; case "FIGHTING" -> "Lutadora"; case "POISON" -> "Venenosa";
            case "GROUND" -> "Terrestre"; case "FLYING" -> "Voadora"; case "PSYCHIC" -> "Psíquica"; case "BUG" -> "Inseto";
            case "ROCK" -> "Pedra"; case "GHOST" -> "Fantasma"; case "DRAGON" -> "Dragão"; case "DARK" -> "Sombrio"; case "STEEL" -> "Aço"; case "FAIRY" -> "Fada"; default -> type;
        };
    }

    private void drawHealthHearts(PoseStack pose, int health, int x, int y) {
        int fullHearts = Math.max(0, health / 2);
        boolean halfHeart = health % 2 != 0;
        int visible = Math.min(fullHearts + (halfHeart ? 1 : 0), 20);
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, GuiComponent.GUI_ICONS_LOCATION);
        for (int i = 0; i < visible; i++) {
            int row = i / 10, column = i % 10;
            int hx = x + column * 8, hy = y + row * 9;
            GuiComponent.blit(pose, hx, hy, 0, 16, 0, 9, 9, 256, 256);
            if (i < fullHearts) GuiComponent.blit(pose, hx, hy, 0, 52, 0, 9, 9, 256, 256);
            else if (halfHeart) GuiComponent.blit(pose, hx, hy, 0, 61, 0, 9, 9, 256, 256);
        }
        int remaining = Math.max(0, fullHearts + (halfHeart ? 1 : 0) - visible);
        if (remaining > 0) font.draw(pose, "+" + remaining, x, y + 19, 0xFF404040);
    }
    private String formatStat(double value) { return String.format(Locale.ROOT, "%.0f", value); }
    private String rarityLabel(String rarity) {
        return switch (rarity) {
            case "common" -> "Comum";
            case "uncommon" -> "Incomum";
            case "rare" -> "Raro";
            case "legendary" -> "Lendário";
            case "mythic" -> "Mítico";
            case "very_rare" -> "Raro";
            default -> rarity;
        };
    }
    private String evolutionStage(PokecSpecies.Entry species) {
        int depth = 0;
        PokecSpecies.Entry current = species;
        while (current != null && current.evolvesFrom() != null) { depth++; current = PokecSpecies.get(current.evolvesFrom()); }
        if (depth == 0) return "Inicial";
        boolean hasEvolution = PokecSpecies.ALL.values().stream().anyMatch(candidate -> species.slug().equals(candidate.evolvesFrom()));
        if (!hasEvolution) return "Final";
        return depth == 1 ? "Intermediário" : "Final";
    }

    private void drawSlot(PoseStack pose, int x, int y, String icon, int accent, boolean active) {
        int border = active ? accent : 0xFFC6C6C6;
        MinemonGui.slot(pose, x, y, 36, 36);
        drawCentered(pose, icon, x + 18, y + 12, active ? accent : 0xFF404040);
        if (!active) drawCentered(pose, "X", x + 18, y + 12, 0xFF705028);
    }

    private void drawItemSlot(PoseStack pose, int x, int y, net.minecraft.world.item.Item item, int count, int accent) {
        MinemonGui.slot(pose, x, y, 36, 36);
        Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(new ItemStack(item), x + 10, y + 9);
        if (count > 0) font.draw(pose, Integer.toString(count), x + 24 - font.width(Integer.toString(count)), y + 26, 0xFF303030);
    }

    private void drawButton(PoseStack pose, int x, int y, int w, int h, String label, boolean enabled, int mouseX, int mouseY) {
        int mx = (int) ((mouseX - leftPos) / uiScale), my = (int) ((mouseY - topPos) / uiScale);
        boolean hover = enabled && mx >= x && mx < x + w && my >= y && my < y + h;
        MinemonGui.panel(pose, x, y, w, h);
        GuiComponent.fill(pose, x + 3, y + 3, x + w - 3, y + h - 3,
                !enabled ? 0xFFAAAAAA : hover ? 0xFFB3C3CF : 0xFFC6C6C6);
        drawCentered(pose, label, x + w / 2.0F, y + (h - 8) / 2.0F, enabled ? 0xFF202020 : 0xFF666666);
    }

    private void drawCentered(PoseStack pose, String text, float x, float y, int color) {
        font.draw(pose, text, x - font.width(text) / 2.0F, y, color);
    }

    private void renderPreview(PoseStack pose, PokecSpecies.Entry species, int x, int y, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            drawCentered(pose, "Modelo disponível no mundo", x, y, 0xFF404040);
            return;
        }
        MinemonCreature entity = previews.computeIfAbsent(species.slug(), slug -> {
            calibratedPreviews.remove(slug);rasterFramedPreviews.remove(slug);previewMeshFrames.remove(slug);previewFits.remove(slug);previewCalibrationDiagnostics.remove(slug);
            var type = ModEntities.SPECIES.get(slug);
            if (type == null) return null;
            MinemonCreature preview = type.get().create(mc.level);
            if (preview != null) {
                preview.setNoAi(true);preview.setNoGravity(true);preview.setDeltaMovement(0,0,0);
                preview.setPokecPreview(true);
            }
            return preview;
        });
        if (entity == null) return;
        entity.tickCount = (int) mc.level.getGameTime();
        entity.setYRot(0.0F);
        entity.yRotO = entity.getYRot();
        entity.yBodyRot=entity.yBodyRotO=0;entity.yHeadRot=entity.yHeadRotO=0;
        var def = SpeciesDefinitions.get(species.slug());
        var fit = previewFits.computeIfAbsent(species.slug(),slug->PreviewBounds.fit(slug,def.scale()));
        previewYaw += net.minecraft.util.Mth.wrapDegrees(targetPreviewYaw-previewYaw)*.25F;
        previewPitch += (targetPreviewPitch-previewPitch)*.25F;
        float scale = fit.scale();
        double windowScale = mc.getWindow().getGuiScale();
        int sx=(int)Math.floor((leftPos+152*uiScale)*windowScale);
        int sy=(int)Math.floor(mc.getWindow().getHeight()-(topPos+132*uiScale)*windowScale);
        com.mojang.blaze3d.systems.RenderSystem.enableScissor(sx,sy,
                (int)Math.ceil(232*uiScale*windowScale),(int)Math.ceil(88*uiScale*windowScale));
        pose.pushPose();
        pose.translate(x, y, 120);
        pose.scale(scale, -scale, scale);
        // PoseStack post-multiplies: these rotations followed by the local-space
        // pivot compensation render as R * T(-pivot), so each model turns around
        // its own measured body center instead of orbiting an external point.
        pose.mulPose(Vector3f.XP.rotationDegrees(previewPitch-6));
        pose.mulPose(Vector3f.YP.rotationDegrees(previewYaw));
        pose.translate(-fit.x(), -fit.y(), -fit.z());
        var measured=new PreviewMeshBounds(pose.last().pose());
        var dispatcher = mc.getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        try {
            br.com.petrickfaria.minemon.client.MinemonRenderer.previewRendering=true;
            if(entity.isPokecIdleReady()&&!calibratedPreviews.contains(species.slug()))PreviewMeshBounds.begin(measured);
            boolean visible=entity.isPokecIdleReady()&&calibratedPreviews.contains(species.slug());
            net.minecraft.client.renderer.MultiBufferSource previewBuffers=type->PreviewMeshBounds.wrap(visible
                    ?mc.renderBuffers().bufferSource().getBuffer(type):PreviewMeshBounds.sink());
            dispatcher.render(entity, 0, 0, 0, 0, partialTick, pose, previewBuffers, 15728880);
            mc.renderBuffers().bufferSource().endBatch();
            // Transparent texture regions and folded alternate meshes can make
            // geometric bounds larger than what the player actually sees. Refine
            // the rendered silhouette until its measured frame is within target;
            // never resize the world model.
            if(visible){
                var framed=frameVisiblePixels(fit);
                if(framed!=null){
                    boolean alreadyFramed=rasterFramedPreviews.contains(species.slug());
                    if(!alreadyFramed||lastPreviewFrameOutsideTarget)previewFits.put(species.slug(),framed);
                    if(lastPreviewFrameOutsideTarget)rasterFramedPreviews.remove(species.slug());
                    else if(lastPreviewFrameWithinTarget)rasterFramedPreviews.add(species.slug());
                }
            }
            if(entity.isPokecIdleReady()&&!calibratedPreviews.contains(species.slug())){
                int frames=previewMeshFrames.merge(species.slug(),1,Integer::sum);
                var calibrated=frames>=8?measured.fit():null;
                previewCalibrationDiagnostics.put(species.slug(),"idleReady=true,frames="+frames+","+measured.diagnostic()
                        +",fit="+calibrated+",rasterFramed="+rasterFramedPreviews.contains(species.slug())
                        +",lastRasterWithin="+lastPreviewFrameWithinTarget+",lastRasterOutside="+lastPreviewFrameOutsideTarget);
                if(calibrated!=null){
                    previewFits.put(species.slug(),calibrated);calibratedPreviews.add(species.slug());
                    if(Boolean.getBoolean("minemon.clientSmokeTest"))System.out.println("[POKEC MESH] "+species.slug()+" "+calibrated);
                }
            }
        } catch (RuntimeException ex) {
            if (Boolean.getBoolean("minemon.clientSmokeTest")) throw ex;
        } finally {
            PreviewMeshBounds.end();
            br.com.petrickfaria.minemon.client.MinemonRenderer.previewRendering=false;
            com.mojang.blaze3d.systems.RenderSystem.disableScissor();
            dispatcher.setRenderShadow(true);
            pose.popPose();
        }
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int x = (int) ((mouseX - leftPos) / uiScale), y = (int) ((mouseY - topPos) / uiScale);
        int trackX=PokecScrollbarGeometry.X,trackY=PokecScrollbarGeometry.Y,trackHeight=visibleRows()*ROW_H;
        // Match the two logical pixels drawn for the track and handle it before
        // the wider row hitbox so scrollbar clicks never select a species.
        if(PokecScrollbarGeometry.containsScaledPoint(mouseX,mouseY,leftPos,topPos,uiScale,
                trackX,trackY,PokecScrollbarGeometry.WIDTH,trackHeight)){
            int thumbHeight=listThumbHeight(),thumbTop=listThumbTop(trackY,trackHeight,thumbHeight);
            draggingListScrollbar=true;
            listScrollbarGrabOffset=(y>=thumbTop&&y<thumbTop+thumbHeight)?y-thumbTop:thumbHeight/2;
            updateListScrollFromMouse(y,trackY,trackHeight,thumbHeight);
            return true;
        }
        if (x >= LIST_X && x < LIST_X + LIST_W && y >= LIST_Y + 12 && y < LIST_Y + 12 + visibleRows() * ROW_H) {
            int index = scrollOffset + (y - (LIST_Y + 12)) / ROW_H;
            if (index >= 0 && index < ordered.size()) selectSpecies(ordered.get(index).slug());
            return true;
        }
        PokecSpecies.Entry species = PokecSpecies.get(selectedSlug);
        if (species != null && syncedUnlocked.contains(species.slug()) && inRect(x,y,152,44,232,88)) { draggingPreview=true; return true; }
        if (species == null) return super.mouseClicked(mouseX, mouseY, button);
        boolean unlocked = syncedUnlocked.contains(species.slug());
        if(inRect(x,y,420,279,116,19)){
            MinemonNetwork.performPokecAction(menu.pos(),selectedSlug,PokecTransactions.Action.RELEASE_INPUT);return true;
        }
        if(inRect(x,y,420,114,63,19)&&unlocked){
            MinemonNetwork.performPokecAction(menu.pos(),selectedSlug,PokecTransactions.Action.TOGGLE_SPAWN);return true;
        }
        if(inRect(x,y,420,87,116,19)){
            MinemonNetwork.performPokecAction(menu.pos(),selectedSlug,unlocked?PokecTransactions.Action.BUY_INPUT:PokecTransactions.Action.UNLOCK_INPUT);return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean inRect(int x, int y, int rx, int ry, int rw, int rh) { return x >= rx && x < rx + rw && y >= ry && y < ry + rh; }
    private void selectSpecies(String slug) {
        if (slug.equals(selectedSlug)) return;
        selectedSlug = slug;
        draggingPreview=false;previewYaw=targetPreviewYaw=23.0F;previewPitch=targetPreviewPitch=-4.0F;
        previews.clear();
        MinemonNetwork.performPokecAction(menu.pos(), slug, PokecTransactions.Action.SELECT);
    }

    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy) {
        if(draggingListScrollbar&&button==0){
            int trackY=LIST_Y+13,trackHeight=visibleRows()*ROW_H;
            updateListScrollFromMouse((int)Math.floor((y-topPos)/uiScale),trackY,trackHeight,listThumbHeight());
            return true;
        }
        if(draggingPreview&&button==0){
            targetPreviewYaw=(targetPreviewYaw+(float)(dx/uiScale)*3.0F)%360.0F;
            targetPreviewPitch=net.minecraft.util.Mth.clamp(targetPreviewPitch+(float)(dy/uiScale)*1.5F,-40,40);
            return true;
        }
        return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x,double y,int button) {
        if(button==0&&draggingListScrollbar){draggingListScrollbar=false;return true;}
        if(button==0&&draggingPreview){draggingPreview=false;return true;}
        return super.mouseReleased(x,y,button);
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int x = (int) ((mouseX - leftPos) / uiScale), y = (int) ((mouseY - topPos) / uiScale);
        if (x >= LIST_X && x < LIST_X + LIST_W && y >= LIST_Y && y < totalHeight()-8) {
            int max = Math.max(0, ordered.size() - visibleRows());
            scrollOffset = Math.max(0, Math.min(max, scrollOffset + (delta < 0 ? 3 : -3)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private int listThumbHeight(){
        int trackHeight=visibleRows()*ROW_H;
        return Math.min(trackHeight,Math.max(Math.min(18,trackHeight),
                (trackHeight*visibleRows())/Math.max(1,ordered.size())));
    }
    private int listThumbTop(int trackY,int trackHeight,int thumbHeight){
        int max=Math.max(0,ordered.size()-visibleRows()),travel=Math.max(0,trackHeight-thumbHeight);
        return trackY+(max==0?0:travel*scrollOffset/max);
    }
    private void updateListScrollFromMouse(int mouseY,int trackY,int trackHeight,int thumbHeight){
        int travel=Math.max(0,trackHeight-thumbHeight),max=Math.max(0,ordered.size()-visibleRows());
        int thumbTop=net.minecraft.util.Mth.clamp(mouseY-trackY-listScrollbarGrabOffset,0,travel);
        scrollOffset=travel==0?0:Math.round((float)thumbTop*max/travel);
        scrollOffset=net.minecraft.util.Mth.clamp(scrollOffset,0,max);
    }

    @Override public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTick);
        int tooltipX=(int)((mouseX-leftPos)/uiScale),tooltipY=(int)((mouseY-topPos)/uiScale);
        if(inRect(tooltipX,tooltipY,280,146,116,25))renderTooltip(pose,Component.literal("Dano"),mouseX,mouseY);
        else renderTooltip(pose,mouseX,mouseY);
    }
}
