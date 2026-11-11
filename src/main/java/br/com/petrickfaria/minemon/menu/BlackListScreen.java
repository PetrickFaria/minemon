package br.com.petrickfaria.minemon.menu;

import br.com.petrickfaria.minemon.entity.MobTargetCategories;
import br.com.petrickfaria.minemon.network.MinemonNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/** Scrollable, modal registry-backed target selector for one owned companion. */
public final class BlackListScreen {
    private record Row(EntityType<?> type, ResourceLocation id, String name) { }
    public enum Ordering { NUMERIC, ALPHABETICAL }

    private static final int ROW_HEIGHT = 22;
    private static final int LIST_TOP_OFFSET = 72;
    private final PokemonInfoScreen parent;
    private Minecraft minecraft;
    private Font font;
    private int width;
    private int height;
    private final int entityId;
    private final UUID pokemonUuid;
    private final List<Row> rows;
    private final Map<String, Boolean> overrides = new HashMap<>();
    private MobTargetCategories.Category category = MobTargetCategories.Category.HOSTILE;
    private MobTargetCategories.Origin origin = MobTargetCategories.Origin.VANILLA;
    private Ordering ordering = Ordering.ALPHABETICAL;
    private int firstRow;
    private int top;
    private int left;
    private int visibleRows = 13;
    private int panelHeight;
    private int panelWidth;
    private boolean draggingScrollbar;
    private int scrollbarGrabOffset;

    public BlackListScreen(PokemonInfoScreen parent, int entityId, UUID pokemonUuid, Map<String, Boolean> overrides) {
        this.parent = parent;
        this.entityId = entityId;
        this.pokemonUuid = pokemonUuid;
        this.overrides.putAll(overrides);
        this.rows = ForgeRegistries.ENTITY_TYPES.getEntries().stream()
                .filter(entry -> entry.getValue().getCategory() != MobCategory.MISC)
                .map(entry -> new Row(entry.getValue(), entry.getKey().location(),
                        Component.translatable(entry.getValue().getDescriptionId()).getString()))
                .toList();
    }

    public void replaceOverrides(Map<String, Boolean> state) {
        overrides.clear();
        overrides.putAll(state);
    }

    private Comparator<Row> rowComparator() {
        if (ordering == Ordering.NUMERIC) {
            Comparator<ResourceLocation> ids = MobTargetCategories.numericIdOrder();
            return (a, b) -> ids.compare(a.id(), b.id());
        }
        return Comparator.comparing(Row::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(row -> row.id().toString());
    }

    private List<Row> visible() {
        return rows.stream()
                .filter(row -> MobTargetCategories.category(row.id(), row.type()) == category)
                .filter(row -> MobTargetCategories.origin(row.id()) == origin)
                .sorted(rowComparator())
                .toList();
    }

    private void resetList() {
        firstRow = 0;
    }

    public List<ResourceLocation> visibleIdsForSmokeTest() { return visible().stream().map(Row::id).toList(); }
    public List<ResourceLocation> allIdsForSmokeTest() { return rows.stream().map(Row::id).toList(); }
    public List<String> visibleNamesForSmokeTest() { return visible().stream().map(Row::name).toList(); }
    public int firstRowForSmokeTest() { return firstRow; }
    public int maxFirstRowForSmokeTest() { return Math.max(0, visible().size() - visibleRows); }
    public int rowCountForSmokeTest() { return rows.size(); }
    public int visibleCountForSmokeTest() { return visible().size(); }
    public boolean containsTypeForSmokeTest(ResourceLocation id) { return rows.stream().anyMatch(row -> row.id().equals(id)); }
    public boolean selectedForSmokeTest(ResourceLocation id) {
        return rows.stream().filter(row -> row.id().equals(id)).findFirst().map(this::selected).orElse(false);
    }
    public MobTargetCategories.Category categoryForSmokeTest() { return category; }
    public MobTargetCategories.Origin originForSmokeTest() { return origin; }
    public Ordering orderingForSmokeTest() { return ordering; }

    public boolean selectCategoryForSmokeTest(MobTargetCategories.Category value) {
        int index = value.ordinal();
        double x = left + 14 + (index + .5) * (panelWidth - 28) / 3.0;
        return mouseClicked(x, top + 37, 0);
    }
    public boolean selectOriginForSmokeTest(MobTargetCategories.Origin value) {
        int index = value.ordinal();
        double x = left + 14 + (index + .5) * (panelWidth - 28) / 3.0;
        return mouseClicked(x, top + 57, 0);
    }
    public boolean selectOrderingForSmokeTest(Ordering value) {
        double x = left + panelWidth - (value == Ordering.NUMERIC ? 39 : 17);
        return mouseClicked(x, top + 18, 0);
    }

    /** Exercises the real hit, drag, release and row-selection code at the thumb's visible center. */
    public boolean dragScrollbarForSmokeTest(boolean clickTrack, boolean dragOutside) {
        int trackTop = top + LIST_TOP_OFFSET, trackHeight = visibleRows * ROW_HEIGHT;
        int thumbHeight = thumbHeight(), thumbTop = thumbTop(trackTop, trackHeight, thumbHeight);
        double x = left + panelWidth - 6.5;
        double y = clickTrack ? trackTop + trackHeight - 1 : thumbTop + thumbHeight / 2.0;
        int before = firstRow;
        boolean clicked = mouseClicked(x, y, 0);
        int afterClick = firstRow;
        if (draggingScrollbar) mouseDragged(x, trackTop + (dragOutside ? -24 : trackHeight + 24), 0, 0, 0);
        boolean released = mouseReleased(x, y, 0);
        return clicked && released && !draggingScrollbar && firstRow >= 0
                && firstRow <= Math.max(0, visible().size() - visibleRows)
                && (visible().size() <= visibleRows || firstRow != before || firstRow != afterClick);
    }

    public void initializeOverlay(Minecraft minecraft, int width, int height) {
        this.minecraft = minecraft;
        this.font = minecraft.font;
        this.width = width;
        this.height = height;
        init();
    }

    private void init() {
        panelWidth = Math.max(1, Math.min(430, width - 12));
        left = (width - panelWidth) / 2;
        panelHeight = Math.max(1, Math.min(390, height - 12));
        visibleRows = Math.max(1, (panelHeight - 92) / ROW_HEIGHT);
        top = (height - panelHeight) / 2;
        firstRow = Math.min(firstRow, Math.max(0, visible().size() - visibleRows));
    }

    private boolean selected(Row row) {
        return overrides.getOrDefault(row.id().toString(), MobTargetCategories.defaultSelected(row.type()));
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int listTop = top + LIST_TOP_OFFSET;
        if (mouseX < left + 12 || mouseX >= left + panelWidth - 12
                || mouseY < listTop || mouseY >= listTop + visibleRows * ROW_HEIGHT) return false;
        firstRow = Math.max(0, Math.min(Math.max(0, visible().size() - visibleRows),
                firstRow + (delta < 0 ? 3 : -3)));
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        int trackTop = top + LIST_TOP_OFFSET;
        int trackHeight = visibleRows * ROW_HEIGHT;
        int trackX = left + panelWidth - 9;
        if (mouseX >= trackX && mouseX < trackX + 5 && mouseY >= trackTop && mouseY < trackTop + trackHeight) {
            int thumbHeight = thumbHeight(), thumbTop = thumbTop(trackTop, trackHeight, thumbHeight);
            draggingScrollbar = true;
            scrollbarGrabOffset = (mouseY >= thumbTop && mouseY < thumbTop + thumbHeight)
                    ? (int) mouseY - thumbTop : thumbHeight / 2;
            updateScrollFromMouse((int) mouseY, trackTop, trackHeight, thumbHeight);
            return true;
        }
        if (mouseY >= top + 28 && mouseY < top + 47 && mouseX >= left + 14 && mouseX < left + panelWidth - 14) {
            int tab = (int) ((mouseX - left - 14) / ((panelWidth - 28) / 3.0));
            MobTargetCategories.Category[] values = MobTargetCategories.Category.values();
            category = values[Math.min(values.length - 1, tab)];
            resetList();
            return true;
        }
        if (mouseY >= top + 48 && mouseY < top + 67 && mouseX >= left + 14 && mouseX < left + panelWidth - 14) {
            int tab = (int) ((mouseX - left - 14) / ((panelWidth - 28) / 3.0));
            MobTargetCategories.Origin[] values = MobTargetCategories.Origin.values();
            origin = values[Math.min(values.length - 1, tab)];
            resetList();
            return true;
        }
        if (mouseY >= top + 8 && mouseY < top + 28) {
            if (mouseX >= left + panelWidth - 48 && mouseX < left + panelWidth - 27) {
                ordering = Ordering.NUMERIC;
                resetList();
                return true;
            }
            if (mouseX >= left + panelWidth - 26 && mouseX < left + panelWidth - 5) {
                ordering = Ordering.ALPHABETICAL;
                resetList();
                return true;
            }
        }
        int listBottom = trackTop + visibleRows * ROW_HEIGHT;
        if (mouseX >= left + 12 && mouseX < left + panelWidth - 12 && mouseY >= trackTop && mouseY < listBottom) {
            int index = firstRow + (int) ((mouseY - trackTop) / ROW_HEIGHT);
            List<Row> displayed = visible();
            if (index >= 0 && index < displayed.size()) {
                Row row = displayed.get(index);
                boolean now = !selected(row);
                overrides.put(row.id().toString(), now);
                MinemonNetwork.setBlackListTarget(entityId, pokemonUuid, row.id(), now);
            }
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar && button == 0) {
            updateScrollFromMouse((int) mouseY, top + LIST_TOP_OFFSET, visibleRows * ROW_HEIGHT, thumbHeight());
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return false;
    }

    private int thumbHeight() {
        int trackHeight = visibleRows * ROW_HEIGHT;
        return Math.min(trackHeight, Math.max(Math.min(18, trackHeight),
                (trackHeight * visibleRows) / Math.max(visibleRows, visible().size())));
    }
    private int thumbTop(int trackTop, int trackHeight, int thumbHeight) {
        int max = Math.max(0, visible().size() - visibleRows), travel = Math.max(0, trackHeight - thumbHeight);
        return trackTop + (max == 0 ? 0 : travel * firstRow / max);
    }
    private void updateScrollFromMouse(int mouseY, int trackTop, int trackHeight, int thumbHeight) {
        int travel = Math.max(0, trackHeight - thumbHeight), max = Math.max(0, visible().size() - visibleRows);
        int thumb = net.minecraft.util.Mth.clamp(mouseY - trackTop - scrollbarGrabOffset, 0, travel);
        firstRow = travel == 0 ? 0 : Math.round((float) thumb * max / travel);
        firstRow = net.minecraft.util.Mth.clamp(firstRow, 0, max);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { onClose(); return true; }
        return false;
    }
    public void onClose() { parent.closeBlackListOverlay(this); }
    public boolean isPauseScreen() { return false; }

    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        GuiComponent.fill(pose, 0, 0, width, height, 0xA0000000);
        MinemonGui.panel(pose, left, top, panelWidth, panelHeight);
        font.draw(pose, "Black List", left + 14, top + 14, 0xFF303030);
        drawSortButton(pose, left + panelWidth - 48, "N", ordering == Ordering.NUMERIC);
        drawSortButton(pose, left + panelWidth - 26, "A", ordering == Ordering.ALPHABETICAL);
        drawTabs(pose, MobTargetCategories.Category.values(), new String[]{"Pacíficos", "Neutros", "Agressivos"}, top + 28,
                category.ordinal());
        drawTabs(pose, MobTargetCategories.Origin.values(), new String[]{"Vanilla", "Minemon", "Mods"}, top + 48,
                origin.ordinal());
        int listTop = top + LIST_TOP_OFFSET;
        List<Row> displayed = visible();
        if (displayed.isEmpty()) {
            font.draw(pose, "Nenhum mob nesta categoria", left + 15, listTop + 6, 0xFF404040);
        } else {
            int end = Math.min(displayed.size(), firstRow + visibleRows);
            for (int i = firstRow; i < end; i++) {
                Row row = displayed.get(i);
                int y = listTop + (i - firstRow) * ROW_HEIGHT;
                int color = mouseX >= left + 12 && mouseX < left + panelWidth - 12 && mouseY >= y && mouseY < y + ROW_HEIGHT
                        ? 0xFFB3C3CF : 0xFFB7B7B7;
                GuiComponent.fill(pose, left + 12, y, left + panelWidth - 12, y + ROW_HEIGHT - 1, color);
                font.draw(pose, selected(row) ? "[✓]" : "[ ]", left + 19, y + 6, selected(row) ? 0xFF705028 : 0xFF404040);
                font.draw(pose, font.plainSubstrByWidth(row.name(), panelWidth - 75), left + 55, y + 3, 0xFF303030);
                font.draw(pose, font.plainSubstrByWidth(row.id().toString(), panelWidth - 75), left + 55, y + 12, 0xFF404040);
            }
        }
        int trackHeight = visibleRows * ROW_HEIGHT;
        int thumbHeight = thumbHeight(), thumbY = thumbTop(listTop, trackHeight, thumbHeight);
        GuiComponent.fill(pose, left + panelWidth - 9, listTop, left + panelWidth - 4, listTop + trackHeight, 0xFFC6C6C6);
        GuiComponent.fill(pose, left + panelWidth - 9, thumbY, left + panelWidth - 4, thumbY + thumbHeight, 0xFF404040);
        font.draw(pose, "Tipos: " + displayed.size(), left + 14, top + panelHeight - 13, 0xFF404040);
    }

    private void drawSortButton(PoseStack pose, int x, String label, boolean selected) {
        GuiComponent.fill(pose, x, top + 8, x + 19, top + 27, selected ? 0xFF91A8BB : 0xFFB7B7B7);
        font.draw(pose, label, x + 6, top + 13, 0xFF303030);
    }

    private void drawTabs(PoseStack pose, Enum<?>[] values, String[] labels, int y, int selected) {
        int tabWidth = (panelWidth - 28) / 3;
        for (int tab = 0; tab < 3; tab++) {
            int x = left + 14 + tab * tabWidth;
            GuiComponent.fill(pose, x, y, x + tabWidth - 2, y + 19,
                    selected == tab ? 0xFF91A8BB : 0xFFB7B7B7);
            font.draw(pose, labels[tab], x + 4, y + 5, 0xFF303030);
        }
    }
}
