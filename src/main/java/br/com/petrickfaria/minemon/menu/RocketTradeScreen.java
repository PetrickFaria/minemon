package br.com.petrickfaria.minemon.menu;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Vanilla-style trade window adapted to retain the Rocket's five soul-input slots. */
public final class RocketTradeScreen extends AbstractContainerScreen<RocketTradeMenu> {
    private static final ResourceLocation VILLAGER = new ResourceLocation("minecraft", "textures/gui/container/villager.png");
    private static final ResourceLocation INVENTORY = new ResourceLocation("minecraft", "textures/gui/container/container.png");

    public RocketTradeScreen(RocketTradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 276;
        imageHeight = 226;
        inventoryLabelY = 123;
    }

    @Override protected void init() {
        super.init();
        addRenderableWidget(new Button(leftPos + 18, topPos + 58, 20, 18, Component.literal("<"), button -> {
            if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }));
        addRenderableWidget(new Button(leftPos + 238, topPos + 58, 20, 18, Component.literal(">"), button -> {
            if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
        }));
    }

    @Override protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY) {
        GuiComponent.blit(pose, leftPos, topPos, 0, 0, 0, 276, 166, 512, 256);
        GuiComponent.blit(pose, leftPos + 50, topPos + 53, 0, 0, 0, 176, 166, 256, 256);
        font.draw(pose, "TROCAS ROCKET", leftPos + 12, topPos + 9, 0x404040);
        font.draw(pose, "5 almas de entrada", leftPos + 63, topPos + 25, 0x404040);
        font.draw(pose, "→", leftPos + 199, topPos + 40, 0x404040);
        font.draw(pose, "Oferta: " + menu.selectedLabel(), leftPos + 68, topPos + 62, 0x404040);
    }

    @Override protected void renderLabels(PoseStack pose, int mouseX, int mouseY) {
        font.draw(pose, "Inventário", 50, inventoryLabelY, 0x404040);
    }

    @Override public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTick);
        renderTooltip(pose, mouseX, mouseY);
    }
}
