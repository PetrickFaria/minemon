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
    private static final int LOGICAL_WIDTH = 520, TOP_HEIGHT = 166, INVENTORY_HEIGHT = 100;
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

    private float uiScale = 1.0F;
    private boolean draggingPreview;
    private float previewYaw,previewPitch,targetPreviewYaw,targetPreviewPitch;
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
    public boolean selectedSpeciesForSmokeTest(String slug) { return selectedSlug.equals(slug); }
    public boolean previewExistsForSmokeTest(String slug) { return previews.containsKey(slug); }
    public boolean speciesUnlockedForSmokeTest(String slug) { return syncedUnlocked.contains(slug); }
    public boolean dragPreviewForSmokeTest(){
        double x=leftPos+268*uiScale,y=topPos+88*uiScale;
        if(!mouseClicked(x,y,0))return false;
        mouseDragged(x+20*uiScale,y+10*uiScale,0,20*uiScale,10*uiScale);
        mouseReleased(x,y,0);
        boolean moved=targetPreviewYaw!=0&&targetPreviewPitch!=0;
        previewYaw=previewPitch=targetPreviewYaw=targetPreviewPitch=0;
        return moved&&!draggingPreview;
    }
    public boolean conversionPanelFitsAboveInventoryForSmokeTest() { return 98 + 20 < TOP_HEIGHT && 145 + 18 < TOP_HEIGHT; }
    public boolean inventorySlotsForSmokeTest() {
        if (menu.slots.size() != 36) return false;
        int x = (imageWidth - INVENTORY_SLOT_WIDTH) / 2;
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

    @Override protected void init() {
        float availableHeight = Math.max(1.0F, height - 8.0F - INVENTORY_HEIGHT);
        uiScale = Math.min(1.0F, Math.min((width - 8.0F) / LOGICAL_WIDTH, availableHeight / TOP_HEIGHT));
        uiScale = Math.max(0.1F, uiScale);
        imageWidth = Math.round(LOGICAL_WIDTH * uiScale);
        imageHeight = Math.round(TOP_HEIGHT * uiScale) + INVENTORY_HEIGHT;
        super.init();
        int inventoryX = (imageWidth - INVENTORY_SLOT_WIDTH) / 2;
        int inventoryTop = Math.round(TOP_HEIGHT * uiScale);
        menu.layoutPlayerInventory(inventoryX, inventoryTop + INVENTORY_MAIN_Y, inventoryTop + INVENTORY_HOTBAR_Y);
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
        MinemonGui.panel(pose, 0, 0, LOGICAL_WIDTH, TOP_HEIGHT);
        font.draw(pose, "POKÉC  /  POKÉDEX", 10, 9, 0xFF404040);
        font.draw(pose, "Saldo: " + syncedCoins + " PokéCoins", 384, 9, 0xFF705028);
        GuiComponent.fill(pose, 8, 20, LOGICAL_WIDTH - 8, 21, 0xFF9A4545);

        drawPanel(pose, LIST_X, LIST_Y - 5, LIST_W, LIST_H + 8);
        drawPanel(pose, 150, 24, 236, TOP_HEIGHT - 28);
        drawPanel(pose, 390, 24, 122, TOP_HEIGHT - 28);
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
        int trackX = LIST_X + LIST_W - 8, trackY = LIST_Y + 13;
        GuiComponent.fill(pose, trackX, trackY, trackX + 2, trackY + visible * ROW_H, 0xFFC6C6C6);
        int thumbHeight = Math.max(18, (visible * ROW_H * visible) / Math.max(1, ordered.size()));
        int thumbY = trackY + (maxScroll == 0 ? 0 : (visible * ROW_H - thumbHeight) * scrollOffset / maxScroll);
        GuiComponent.fill(pose, trackX, thumbY, trackX + 2, thumbY + thumbHeight, 0xFF404040);
        drawSelectedPanel(pose, partialTick, mouseX, mouseY);
        pose.popPose();

        int inventoryTop = Math.round(TOP_HEIGHT * uiScale);
        drawPanel(pose, 0, inventoryTop, imageWidth, INVENTORY_HEIGHT);
        font.draw(pose, "INVENTÁRIO", 8, inventoryTop + INVENTORY_LABEL_Y, 0xFF404040);
        for (var slot : menu.slots) MinemonGui.slot(pose, slot.x, slot.y, 18, 18);
        pose.popPose();
    }

    private int visibleRows() { return Math.max(1, (LIST_H - 17) / ROW_H); }
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
        GuiComponent.fill(pose, 154, 136, 382, 137, 0xFFC6C6C6);
        font.draw(pose, "DADOS DA ESPÉCIE", 154, 139, 0xFF404040);
        var definition = SpeciesDefinitions.get(species.slug());
        if (definition != null) {
            int health = (int)Math.ceil(br.com.petrickfaria.minemon.entity.PokemonProgression.healthForRarity(PokecEconomy.get().rarity(species.slug()),1));
            font.draw(pose, "Vida:", 154, 150, 0xFF404040);
            drawHealthHearts(pose, health, 193, 148);
            font.draw(pose, "Dano: " + formatStat(br.com.petrickfaria.minemon.entity.PokemonProgression.damageForRarity(PokecEconomy.get().rarity(species.slug()),1)), 302, 150, 0xFF404040);
            font.draw(pose, "Raridade: " + rarityLabel(PokecEconomy.get().rarity(species.slug())), 154, 158, 0xFF404040);
            font.draw(pose, "Estágio: " + evolutionStage(species), 275, 158, 0xFF404040);
        }
        drawAcquisitionPanel(pose, species, unlocked, species.isEvolution() && !unlocked, mouseX, mouseY);
        drawCompactConversion(pose);
    }

    private void drawAcquisitionPanel(PoseStack pose, PokecSpecies.Entry species, boolean unlocked,
                                      boolean evolution, int mouseX, int mouseY) {
        int x = 394;
        if (evolution) {
            font.draw(pose, "POR EVOLUÇÃO", x, 28, 0xFF705028);
            font.draw(pose, "Evolua um", x, 42, 0xFF404040);
            font.draw(pose, "Minemon para liberar.", x, 52, 0xFF404040);
            font.draw(pose, "Não aceita almas ou moedas.", x, 62, 0xFF404040);
            return;
        }
        if (unlocked) {
            int price = syncedPrices.getOrDefault(species.slug(), PokecEconomy.get().coins(species.slug()));
            boolean free = syncedFreeAvailable;
            font.draw(pose, "AQUISIÇÃO", x, 28, free ? 0xFF705028 : 0xFF404040);
            font.draw(pose, "Preço: " + (free ? 0 : price), x, 40, 0xFF404040);
            drawCompactItemSlot(pose, x, 52, ModItems.POKE_COIN.get(), syncedStagedCoins);
            boolean canBuy = free || syncedCoins + syncedStagedCoins >= price;
            drawButton(pose, 420, 52, 84, 22, "ADQUIRIR", canBuy, mouseX, mouseY);
        } else {
            int required = syncedRequirements.getOrDefault(species.slug(), PokecEconomy.get().souls(species.slug()));
            int deposited = syncedDeposits.getOrDefault(species.slug(), 0);
            var soulType = SoulType.fromSpeciesType(species.primaryType());
            var soulItem = soulType == null ? null : ModItems.SOULS.get(soulType.id());
            font.draw(pose, "ALMAS " + species.primaryType().toUpperCase(Locale.ROOT), x, 28, 0xFF404040);
            font.draw(pose, fitText("Precisa: " + required, 112), x, 40, 0xFF404040);
            font.draw(pose, fitText("Progresso: " + deposited + "/" + required, 112), x, 50, 0xFF404040);
            if (soulItem != null) {
                drawCompactItemSlot(pose, x, 61, soulItem.get(), Math.min(64, syncedStagedSouls));
                drawCompactItemSlot(pose, x + 22, 61, soulItem.get(), Math.max(0, syncedStagedSouls - 64));
            }
            font.draw(pose, fitText("Separadas: " + syncedStagedSouls, 72), x + 46, 67, 0xFF404040);
            drawButton(pose, 420, 61, 84, 22, "LIBERAR", deposited + syncedStagedSouls >= required, mouseX, mouseY);
        }
    }

    private void drawCompactConversion(PoseStack pose) {
        int x = 394;
        font.draw(pose, "DEVOLVER À NATUREZA", x, 86, 0xFF404040);
        drawCompactItemSlot(pose, x, 98, syncedConversionBall.isEmpty() ? null : syncedConversionBall.getItem(),
                syncedConversionBall.isEmpty() ? 0 : syncedConversionBall.getCount());
        if (syncedConversionBall.isEmpty()) {
            font.draw(pose, "Insira Pokébola", x + 24, 100, 0xFF404040);
        } else if (!syncedConversionSpecies.isEmpty()) {
            var species = PokecSpecies.get(syncedConversionSpecies);
            String name = species == null ? syncedConversionSpecies : species.name();
            font.draw(pose, fitText(name, 88), x + 24, 97, 0xFF404040);
            font.draw(pose, String.format(Locale.ROOT, "#%03d  Nv. %d", species == null ? 0 : species.number(), syncedConversionLevel), x + 24, 106, 0xFF404040);
            font.draw(pose, fitText(rarityLabel(syncedConversionRarity) + " / " + conversionStageLabel(syncedConversionStage), 88), x + 24, 115, 0xFF404040);
            font.draw(pose, "Base: " + syncedConversionBase, x, 124, 0xFF404040);
            font.draw(pose, fitText("Recebe: " + syncedConversionMinimum + "–" + syncedConversionMaximum + " almas", 118), x, 133, 0xFF404040);
        } else if (syncedConversionResult >= 0) {
            font.draw(pose, "Devolução concluída", x + 24, 100, 0xFF404040);
            font.draw(pose, syncedConversionResult + " almas", x + 24, 110, 0xFF404040);
        }
        if (!syncedConversionBall.isEmpty() && syncedConversionResult < 0) {
            boolean confirming = syncedConversionConfirming;
            drawButton(pose, x, 145, 55, 18, confirming ? "CONFIRMAR" : "DEVOLVER", true, -1, -1);
            if (confirming) drawButton(pose, x + 61, 145, 55, 18, "CANCELAR", true, -1, -1);
        }
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
            case "ROCK" -> "Pedra"; case "GHOST" -> "Fantasma"; case "DRAGON" -> "Dragão"; default -> type;
        };
    }

    private void drawHealthHearts(PoseStack pose, int health, int x, int y) {
        int fullHearts = Math.max(0, health / 2);
        boolean halfHeart = health % 2 != 0;
        int visible = Math.min(fullHearts + (halfHeart ? 1 : 0), 20);
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, GuiComponent.GUI_ICONS_LOCATION);
        for (int i = 0; i < visible; i++) {
            int row = i / 10, column = i % 10;
            int hx = x + column * 9, hy = y + row * 9;
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
            var type = ModEntities.SPECIES.get(slug);
            if (type == null) return null;
            MinemonCreature preview = type.get().create(mc.level);
            if (preview != null) { preview.setNoAi(true); preview.setDeltaMovement(0, 0, 0); preview.tick(); preview.tickCount = 0; }
            return preview;
        });
        if (entity == null) return;
        entity.tickCount = (int) (System.currentTimeMillis() / 50L);
        entity.setYRot(180.0F);
        entity.yRotO = entity.getYRot();
        entity.yBodyRot=entity.yBodyRotO=180;entity.yHeadRot=entity.yHeadRotO=180;
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
        var dispatcher = mc.getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        try {
            br.com.petrickfaria.minemon.client.MinemonRenderer.previewRendering=true;
            dispatcher.render(entity, 0, 0, 0, 0, partialTick, pose, mc.renderBuffers().bufferSource(), 15728880);
            mc.renderBuffers().bufferSource().endBatch();
        } catch (RuntimeException ex) {
            if (Boolean.getBoolean("minemon.clientSmokeTest")) throw ex;
        } finally {
            br.com.petrickfaria.minemon.client.MinemonRenderer.previewRendering=false;
            com.mojang.blaze3d.systems.RenderSystem.disableScissor();
            dispatcher.setRenderShadow(true);
            pose.popPose();
        }
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int x = (int) ((mouseX - leftPos) / uiScale), y = (int) ((mouseY - topPos) / uiScale);
        if (x >= LIST_X && x < LIST_X + LIST_W && y >= LIST_Y + 12 && y < LIST_Y + 12 + visibleRows() * ROW_H) {
            int index = scrollOffset + (y - (LIST_Y + 12)) / ROW_H;
            if (index >= 0 && index < ordered.size()) selectSpecies(ordered.get(index).slug());
            return true;
        }
        PokecSpecies.Entry species = PokecSpecies.get(selectedSlug);
        if (species != null && syncedUnlocked.contains(species.slug()) && inRect(x,y,152,44,232,88)) { draggingPreview=true; return true; }
        if (species == null) return super.mouseClicked(mouseX, mouseY, button);
        if (inRect(x, y, 394, 98, 20, 20)) {
            MinemonNetwork.performPokecAction(menu.pos(), selectedSlug, syncedConversionBall.isEmpty() ? PokecTransactions.Action.STAGE_POKEBALL : PokecTransactions.Action.RETURN_POKEBALL);
            return true;
        }
        if (inRect(x, y, 394, 145, 55, 18) && !syncedConversionBall.isEmpty() && syncedConversionResult < 0) {
            MinemonNetwork.performPokecAction(menu.pos(), selectedSlug, syncedConversionConfirming ? PokecTransactions.Action.CONFIRM_CONVERSION : PokecTransactions.Action.REQUEST_CONVERSION);
            return true;
        }
        if (inRect(x, y, 455, 145, 55, 18) && syncedConversionConfirming) {
            MinemonNetwork.performPokecAction(menu.pos(), selectedSlug, PokecTransactions.Action.CANCEL_CONVERSION);
            return true;
        }
        boolean unlocked = syncedUnlocked.contains(species.slug());
        boolean evolution = species.isEvolution() && !unlocked;
        if (unlocked && !syncedFreeAvailable && inRect(x, y, 394, 52, 20, 20)) {
            MinemonNetwork.performPokecAction(menu.pos(), selectedSlug, PokecTransactions.Action.STAGE_COINS);
            return true;
        }
        if (!unlocked && !evolution && (inRect(x, y, 394, 61, 20, 20) || inRect(x, y, 416, 61, 20, 20))) {
            MinemonNetwork.performPokecAction(menu.pos(), selectedSlug, PokecTransactions.Action.STAGE_SOULS);
            return true;
        }
        if (unlocked && inRect(x, y, 420, 52, 84, 22)) {
            if (syncedFreeAvailable || syncedCoins + syncedStagedCoins >= syncedPrices.getOrDefault(selectedSlug, PokecEconomy.get().coins(selectedSlug)))
                MinemonNetwork.performPokecAction(menu.pos(), selectedSlug, PokecTransactions.Action.PURCHASE);
            return true;
        }
        if (!unlocked && !evolution && inRect(x, y, 420, 61, 84, 22)) {
            MinemonNetwork.performPokecAction(menu.pos(), selectedSlug, PokecTransactions.Action.CONFIRM_SOULS);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean inRect(int x, int y, int rx, int ry, int rw, int rh) { return x >= rx && x < rx + rw && y >= ry && y < ry + rh; }
    private void selectSpecies(String slug) {
        if (slug.equals(selectedSlug)) return;
        selectedSlug = slug;
        draggingPreview=false;previewYaw=previewPitch=targetPreviewYaw=targetPreviewPitch=0;
        previews.clear();
        MinemonNetwork.performPokecAction(menu.pos(), slug, PokecTransactions.Action.SELECT);
    }

    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy) {
        if(draggingPreview&&button==0){
            targetPreviewYaw=(targetPreviewYaw+(float)(dx/uiScale)*3.0F)%360.0F;
            targetPreviewPitch=net.minecraft.util.Mth.clamp(targetPreviewPitch+(float)(dy/uiScale)*1.5F,-40,40);
            return true;
        }
        return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x,double y,int button) {
        if(button==0&&draggingPreview){draggingPreview=false;return true;}
        return super.mouseReleased(x,y,button);
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int x = (int) ((mouseX - leftPos) / uiScale), y = (int) ((mouseY - topPos) / uiScale);
        if (x >= LIST_X && x < LIST_X + LIST_W && y >= LIST_Y && y < LIST_Y + LIST_H) {
            int max = Math.max(0, ordered.size() - visibleRows());
            scrollOffset = Math.max(0, Math.min(max, scrollOffset + (delta < 0 ? 3 : -3)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTick);
    }
}
