package br.com.petrickfaria.minemon.menu;

import br.com.petrickfaria.minemon.pokec.ActivePokemonData;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiComponent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Team-wide companion behavior controls. The server owns and validates every change. */
public final class PokemonManagerScreen extends Screen {
    private static final int VISIBLE_ROWS = 5;
    private final List<ActivePokemonData.Entry> entries;
    private int firstRow;

    public PokemonManagerScreen(List<ActivePokemonData.Entry> entries) {
        super(Component.literal("Gerenciar Pokémon"));
        this.entries = List.copyOf(entries);
    }

    @Override protected void init() {
        int left = (width - 360) / 2;
        int top = (height - 230) / 2 + 23;
        int visible = Math.min(VISIBLE_ROWS, Math.max(0, entries.size() - firstRow));
        for (int row = 0; row < visible; row++) {
            var pokemon = entries.get(firstRow + row);
            int y = top + row * 37;
            addRenderableWidget(modeButton(left + 194, y, pokemon, "follow", "Seguir"));
            addRenderableWidget(modeButton(left + 250, y, pokemon, "sit", "Sentar"));
            addRenderableWidget(modeButton(left + 306, y, pokemon, "free", "Livre"));
        }
        if (entries.size() > VISIBLE_ROWS) {
            addRenderableWidget(Button.builder(Component.literal("▲"), b -> { firstRow=Math.max(0,firstRow-1); refreshRows(); })
                    .bounds(left+332, top-21, 24, 18).build());
            addRenderableWidget(Button.builder(Component.literal("▼"), b -> { firstRow=Math.min(entries.size()-VISIBLE_ROWS,firstRow+1); refreshRows(); })
                    .bounds(left+332, top+VISIBLE_ROWS*37-3, 24, 18).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Fechar"), b -> onClose())
                .bounds(left+136, top+VISIBLE_ROWS*37+3, 88, 20).build());
    }

    private void refreshRows() { clearWidgets(); init(); }

    private Button modeButton(int x, int y, ActivePokemonData.Entry pokemon, String mode, String label) {
        String text = pokemon.behavior().equals(mode) ? "✓" + label : label;
        return Button.builder(Component.literal(text), button ->
                        br.com.petrickfaria.minemon.network.MinemonNetwork.setManagedPokemonBehavior(pokemon.id(), mode))
                .bounds(x, y, 53, 20).build();
    }

    @Override public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        int left=(width-360)/2, top=(height-230)/2;
        GuiComponent.fill(pose,left,top,left+360,top+230,0xE0101010);
        GuiComponent.fill(pose,left,top,left+360,top+1,0xFF806040);
        GuiComponent.fill(pose,left,top+229,left+360,top+230,0xFF806040);
        GuiComponent.fill(pose,left,top,left+1,top+230,0xFF806040);
        GuiComponent.fill(pose,left+359,top,left+360,top+230,0xFF806040);
        font.draw(pose,title,width/2-font.width(title)/2,top+8,0xFFFFFF);
        String emptyMessage="Nenhum Pokémon domesticado registrado";
        if(entries.isEmpty()) font.draw(pose,emptyMessage,width/2-font.width(emptyMessage)/2,top+78,0xC0C0C0);
        int visible=Math.min(VISIBLE_ROWS,Math.max(0,entries.size()-firstRow));
        for(int row=0;row<visible;row++){
            var entry=entries.get(firstRow+row);int y=top+25+row*37;
            var egg=br.com.petrickfaria.minemon.registry.ModItems.EGGS.get(entry.species());
            if(egg!=null)net.minecraft.client.Minecraft.getInstance().getItemRenderer()
                    .renderAndDecorateItem(egg.get().getDefaultInstance(),left+8,y-1);
            font.draw(pose,entry.name(),left+30,y,0xFFFFFF);
            font.draw(pose,entry.species()+"  ·  Nível "+entry.level(),left+30,y+12,0xA0A0A0);
        }
        super.render(pose,mouseX,mouseY,partialTick);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if(entries.size()<=VISIBLE_ROWS)return super.mouseScrolled(mouseX,mouseY,delta);
        firstRow=Math.max(0,Math.min(entries.size()-VISIBLE_ROWS,firstRow+(delta<0?1:-1)));
        refreshRows();
        return true;
    }

    @Override public boolean isPauseScreen() { return false; }
}
