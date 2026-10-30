package br.com.petrickfaria.minemon.menu;

import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.network.MinemonNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Scrollable registry-backed target selector for one owned companion. */
public final class BlackListScreen extends Screen {
    private record Row(EntityType<?> type, ResourceLocation id, String name) { }
    private final PokemonInfoScreen parent;
    private final int entityId;
    private final UUID pokemonUuid;
    private final List<Row> rows;
    private final Map<String, Boolean> overrides = new HashMap<>();
    private br.com.petrickfaria.minemon.entity.MobTargetCategories.Category category=br.com.petrickfaria.minemon.entity.MobTargetCategories.Category.HOSTILE;
    private List<Row> visible(){return rows.stream().filter(row->br.com.petrickfaria.minemon.entity.MobTargetCategories.category(row.id(),row.type())==category).toList();}
    private int firstRow;
    private int top;
    private int left;
    private static final int ROW_HEIGHT = 22;
    private int visibleRows = 15;
    private int panelHeight;
    private int panelWidth;

    public BlackListScreen(PokemonInfoScreen parent, int entityId, UUID pokemonUuid, Map<String, Boolean> overrides) {
        super(Component.literal("Black List"));
        this.parent = parent;
        this.entityId = entityId;
        this.pokemonUuid = pokemonUuid;
        this.overrides.putAll(overrides);
        this.rows = ForgeRegistries.ENTITY_TYPES.getEntries().stream()
                .filter(entry -> entry.getValue().getCategory() != MobCategory.MISC)
                .map(entry -> new Row(entry.getValue(), entry.getKey().location(),
                        Component.translatable(entry.getValue().getDescriptionId()).getString()))
                .sorted(Comparator.comparing(Row::name, String.CASE_INSENSITIVE_ORDER).thenComparing(row -> row.id().toString()))
                .toList();
    }

    public void replaceOverrides(Map<String, Boolean> state) {
        this.overrides.clear();
        this.overrides.putAll(state);
    }
    public int rowCountForSmokeTest() { return rows.size(); }
    public boolean containsTypeForSmokeTest(ResourceLocation id) {
        return rows.stream().anyMatch(row -> row.id().equals(id));
    }
    public boolean selectedForSmokeTest(ResourceLocation id) {
        return rows.stream().filter(row -> row.id().equals(id)).findFirst().map(this::selected).orElse(false);
    }

    @Override protected void init() {
        panelWidth = Math.min(430, width - 12);
        left = (width - panelWidth) / 2;
        panelHeight = Math.min(390, height - 12);
        visibleRows = Math.max(1, (panelHeight - 70) / ROW_HEIGHT);
        top = (height - panelHeight) / 2;
        firstRow = Math.min(firstRow, Math.max(0, visible().size() - visibleRows));
    }

    private boolean selected(Row row) {
        return overrides.getOrDefault(row.id().toString(), br.com.petrickfaria.minemon.entity.MobTargetCategories.defaultSelected(row.type()));
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        firstRow = Math.max(0, Math.min(Math.max(0, visible().size() - visibleRows), firstRow + (delta < 0 ? 3 : -3)));
        return true;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(button==0 && mouseY>=top+29 && mouseY<top+48 && mouseX>=left+14 && mouseX<left+panelWidth-14){
            int tab=(int)((mouseX-left-14)/((panelWidth-28)/3.0));
            category=br.com.petrickfaria.minemon.entity.MobTargetCategories.Category.values()[Math.min(2,tab)];firstRow=0;return true;
        }
        if (button == 0 && mouseX >= left + 14 && mouseX <= left + panelWidth - 14
                && mouseY >= top + 52 && mouseY < top + 52 + visibleRows * ROW_HEIGHT) {
            int index = firstRow + (int) ((mouseY - top - 52) / ROW_HEIGHT);
            if (index >= 0 && index < visible().size()) {
                Row row = visible().get(index);
                boolean now = !selected(row);
                overrides.put(row.id().toString(), now);
                MinemonNetwork.setBlackListTarget(entityId, pokemonUuid, row.id(), now);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }

    @Override public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        MinemonGui.panel(pose, left, top, panelWidth, panelHeight);
        font.draw(pose, "Black List", left + 14, top + 14, 0xFF303030);
        String[] labels={"Pacíficos","Neutros","Agressivos"};
        for(int tab=0;tab<3;tab++){
            int x=left+14+tab*(panelWidth-28)/3,w=(panelWidth-28)/3;
            GuiComponent.fill(pose,x,top+29,x+w-2,top+48,category.ordinal()==tab?0xFF91A8BB:0xFFB7B7B7);
            font.draw(pose,labels[tab],x+4,top+34,0xFF303030);
        }
        int end = Math.min(visible().size(), firstRow + visibleRows);
        for (int i = firstRow; i < end; i++) {
            Row row = visible().get(i);
            int y = top + 52 + (i - firstRow) * ROW_HEIGHT;
            int color = mouseX >= left + 14 && mouseX < left + panelWidth - 14 && mouseY >= y && mouseY < y + ROW_HEIGHT
                    ? 0xFFB3C3CF : 0xFFB7B7B7;
            GuiComponent.fill(pose, left + 12, y, left + panelWidth - 12, y + ROW_HEIGHT - 1, color);
            font.draw(pose, selected(row) ? "[✓]" : "[ ]", left + 19, y + 6, selected(row) ? 0xFF705028 : 0xFF404040);
            font.draw(pose, font.plainSubstrByWidth(row.name(), panelWidth - 75), left + 55, y + 3, 0xFF303030);
            font.draw(pose, font.plainSubstrByWidth(row.id().toString(), panelWidth - 75), left + 55, y + 12, 0xFF404040);
        }
        int thumbHeight = Math.max(18, (visibleRows * ROW_HEIGHT * visibleRows) / Math.max(visibleRows, visible().size()));
        int trackTop = top + 52, trackHeight = visibleRows * ROW_HEIGHT;
        int thumbY = trackTop + (visible().size() <= visibleRows ? 0
                : (trackHeight - thumbHeight) * firstRow / Math.max(1, visible().size() - visibleRows));
        GuiComponent.fill(pose, left + panelWidth - 9, trackTop, left + panelWidth - 4, trackTop + trackHeight, 0xFFC6C6C6);
        GuiComponent.fill(pose, left + panelWidth - 9, thumbY, left + panelWidth - 4, thumbY + thumbHeight, 0xFF404040);
        font.draw(pose, "Tipos: " + visible().size(), left + 14, top + panelHeight - 13, 0xFF404040);
        super.render(pose, mouseX, mouseY, partialTick);
    }
}
