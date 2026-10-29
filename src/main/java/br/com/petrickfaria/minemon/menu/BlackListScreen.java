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
    private int firstRow;
    private int top;
    private int left;
    private static final int ROW_HEIGHT = 22;
    private static final int VISIBLE_ROWS = 15;

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
        left = (width - 430) / 2;
        top = (height - 390) / 2;
    }

    private boolean selected(Row row) {
        return overrides.getOrDefault(row.id().toString(), row.type().getCategory() == MobCategory.MONSTER);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        firstRow = Math.max(0, Math.min(Math.max(0, rows.size() - VISIBLE_ROWS), firstRow + (delta < 0 ? 3 : -3)));
        return true;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= left + 14 && mouseX <= left + 416
                && mouseY >= top + 52 && mouseY < top + 52 + VISIBLE_ROWS * ROW_HEIGHT) {
            int index = firstRow + (int) ((mouseY - top - 52) / ROW_HEIGHT);
            if (index >= 0 && index < rows.size()) {
                Row row = rows.get(index);
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
        GuiComponent.fill(pose, left, top, left + 430, top + 390, 0xFF14232F);
        GuiComponent.fill(pose, left + 3, top + 3, left + 427, top + 387, 0xFF263F52);
        font.draw(pose, "Black List", left + 14, top + 14, 0xFFFFFFFF);
        font.draw(pose, "Selecione os tipos de entidade que o Pokémon atacará.", left + 14, top + 31, 0xFFD8E8F0);
        int end = Math.min(rows.size(), firstRow + VISIBLE_ROWS);
        for (int i = firstRow; i < end; i++) {
            Row row = rows.get(i);
            int y = top + 52 + (i - firstRow) * ROW_HEIGHT;
            int color = mouseX >= left + 14 && mouseX < left + 416 && mouseY >= y && mouseY < y + ROW_HEIGHT
                    ? 0xFF3A5B70 : 0xFF213747;
            GuiComponent.fill(pose, left + 12, y, left + 418, y + ROW_HEIGHT - 1, color);
            font.draw(pose, selected(row) ? "[✓]" : "[ ]", left + 19, y + 6, selected(row) ? 0xFF9FE29A : 0xFFE5E5E5);
            font.draw(pose, row.name(), left + 55, y + 3, 0xFFFFFFFF);
            font.draw(pose, row.id().toString(), left + 55, y + 12, 0xFFB7CBD6);
        }
        int thumbHeight = Math.max(18, (VISIBLE_ROWS * ROW_HEIGHT * VISIBLE_ROWS) / Math.max(VISIBLE_ROWS, rows.size()));
        int trackTop = top + 52, trackHeight = VISIBLE_ROWS * ROW_HEIGHT;
        int thumbY = trackTop + (rows.size() <= VISIBLE_ROWS ? 0
                : (trackHeight - thumbHeight) * firstRow / Math.max(1, rows.size() - VISIBLE_ROWS));
        GuiComponent.fill(pose, left + 421, trackTop, left + 426, trackTop + trackHeight, 0xFF101A20);
        GuiComponent.fill(pose, left + 421, thumbY, left + 426, thumbY + thumbHeight, 0xFF9CB5C2);
        font.draw(pose, "Tipos: " + rows.size(), left + 14, top + 386, 0xFFD8E8F0);
        super.render(pose, mouseX, mouseY, partialTick);
    }
}
