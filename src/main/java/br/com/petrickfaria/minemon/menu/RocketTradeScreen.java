package br.com.petrickfaria.minemon.menu;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compact Rocket trade UI with five soul inputs and one result slot. */
public final class RocketTradeScreen extends AbstractContainerScreen<RocketTradeMenu> {
    public RocketTradeScreen(RocketTradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 196;
        inventoryLabelY = 87;
    }

    @Override protected void init() {
        super.init();
        addRenderableWidget(new Button(leftPos + 8, topPos + 59, 20, 18, Component.literal("<"), button -> {
            if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }));
        addRenderableWidget(new Button(leftPos + 148, topPos + 59, 20, 18, Component.literal(">"), button -> {
            if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
        }));
    }

    @Override protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY) {
        GuiComponent.fill(pose, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xEE0A1018);
        GuiComponent.fill(pose, leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFF152331);
        GuiComponent.fill(pose, leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + 22, 0xFF1A3448);
        font.draw(pose, "TROCAS ROCKET", leftPos + 8, topPos + 9, 0xFF9BDFFF);
        font.draw(pose, "5 slots: almas de entrada", leftPos + 8, topPos + 25, 0xFFD5E4EC);
        font.draw(pose, "←", leftPos + 119, topPos + 39, 0xFF75D7FF);
        font.draw(pose, "Troca: " + menu.selectedLabel(), leftPos + 32, topPos + 64, 0xFFFFD778);
        for (int i = 0; i < 5; i++) drawSlot(pose, leftPos + 8 + i * 18, topPos + 35);
        drawSlot(pose, leftPos + 142, topPos + 35);
        GuiComponent.fill(pose, leftPos + 5, topPos + 94, leftPos + imageWidth - 5, topPos + 95, 0xFF2B536B);
    }

    private void drawSlot(PoseStack pose, int x, int y) {
        GuiComponent.fill(pose, x, y, x + 18, y + 18, 0xFF6A8292);
        GuiComponent.fill(pose, x + 1, y + 1, x + 17, y + 17, 0xFF101A22);
    }

    @Override protected void renderLabels(PoseStack pose, int mouseX, int mouseY) {
        font.draw(pose, "Inventário", 8, inventoryLabelY, 0xFFE1EDF4);
    }

    @Override public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTick);
        renderTooltip(pose, mouseX, mouseY);
    }
}
