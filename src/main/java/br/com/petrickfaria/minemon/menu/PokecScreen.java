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
    private static final int LOGICAL_WIDTH = 520, LOGICAL_HEIGHT = 410;
    private static final int LIST_X = 8, LIST_Y = 35, LIST_W = 174, LIST_H = 180, ROW_H = 12;
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
    private int scrollOffset;
    private String selectedSlug = "bulbasaur";
    private Map<String, MinemonCreature> previews = new HashMap<>();
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
        double y = topPos + (LIST_Y + 14 + row * ROW_H + 2) * uiScale;
        return mouseClicked(x, y, 0) && selectedSlug.equals(slug);
    }
    public boolean selectedSpeciesForSmokeTest(String slug) { return selectedSlug.equals(slug); }
    public boolean previewExistsForSmokeTest(String slug) { return previews.containsKey(slug); }
    public boolean speciesUnlockedForSmokeTest(String slug) { return syncedUnlocked.contains(slug); }
    public boolean conversionSlotsBelowListForSmokeTest() { return 253 > LIST_Y + LIST_H && 20 + 36 < LIST_X + LIST_W; }
    public float previewScaleForSmokeTest(String slug) {
        var definition = SpeciesDefinitions.get(slug);
        return definition == null ? 0.0F : computePreviewScale(definition.width(), definition.height(), definition.scale());
    }

    @Override protected void init() {
        uiScale = Math.min(1.0F, Math.min((width - 8.0F) / LOGICAL_WIDTH, (height - 8.0F) / LOGICAL_HEIGHT));
        uiScale = Math.max(0.1F, uiScale);
        imageWidth = Math.round(LOGICAL_WIDTH * uiScale);
        imageHeight = Math.round(LOGICAL_HEIGHT * uiScale);
        super.init();
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
        pose.scale(uiScale, uiScale, 1);
        MinemonGui.panel(pose, 0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);
        GuiComponent.fill(pose, 8, 28, LOGICAL_WIDTH - 8, 29, 0xFF9A4545);
        font.draw(pose, "POKÉC  /  POKÉDEX", 12, 12, 0xFF404040);
        font.draw(pose, "Saldo: " + syncedCoins + " PokéCoins", 346, 12, 0xFF705028);

        drawPanel(pose, LIST_X, LIST_Y - 5, LIST_W, LIST_H + 8);
        drawPanel(pose, 190, LIST_Y - 5, 322, LOGICAL_HEIGHT - LIST_Y - 8);
        font.draw(pose, "ESPÉCIES   001–151", LIST_X + 7, LIST_Y - 1, 0xFF404040);
        int visible = visibleRows();
        for (int row = 0; row < visible; row++) {
            int index = scrollOffset + row;
            if (index >= ordered.size()) break;
            PokecSpecies.Entry species = ordered.get(index);
            int y = LIST_Y + 13 + row * ROW_H;
            boolean selected = species.slug().equals(selectedSlug);
            boolean unlocked = syncedUnlocked.contains(species.slug());
            GuiComponent.fill(pose, LIST_X + 5, y, LIST_X + LIST_W - 5, y + ROW_H - 1,
                    selected ? 0xFFB3C3CF : (row % 2 == 0 ? 0xFFB7B7B7 : 0xFFAEAEAE));
            if (selected) GuiComponent.fill(pose, LIST_X + 5, y, LIST_X + 7, y + ROW_H - 1, 0xFF404040);
            int color = unlocked ? 0xFF202020 : 0xFF555555;
            font.draw(pose, String.format(Locale.ROOT, "%03d - %s", species.number(), species.name()), LIST_X + 11, y + 2, color);
        }
        int maxScroll = Math.max(0, ordered.size() - visible);
        int trackX = LIST_X + LIST_W - 8, trackY = LIST_Y + 13;
        GuiComponent.fill(pose, trackX, trackY, trackX + 2, trackY + visible * ROW_H, 0xFFC6C6C6);
        int thumbHeight = Math.max(18, (visible * ROW_H * visible) / Math.max(1, ordered.size()));
        int thumbY = trackY + (maxScroll == 0 ? 0 : (visible * ROW_H - thumbHeight) * scrollOffset / maxScroll);
        GuiComponent.fill(pose, trackX, thumbY, trackX + 2, thumbY + thumbHeight, 0xFF404040);
        drawLeftConversion(pose);
        font.draw(pose, "INVENTÁRIO", 200, 305, 0xFF404040);
        for (int i = 0; i < 36; i++) {
            int column = i % 9;
            int baseX = 200 + column * 18;
            int baseY = i < 27 ? 320 + (i / 9) * 18 : 378;
            MinemonGui.slot(pose, baseX, baseY, 18, 18);
        }
        drawSelectedPanel(pose, partialTick, mouseX, mouseY);
        pose.popPose();
    }

    private int visibleRows() { return Math.max(1, (LIST_H - 21) / ROW_H); }
    private void drawPanel(PoseStack pose, int x, int y, int w, int h) {
        MinemonGui.slot(pose, x, y, w, h);
    }

    private void drawSelectedPanel(PoseStack pose, float partialTick, int mouseX, int mouseY) {
        PokecSpecies.Entry species = PokecSpecies.get(selectedSlug);
        if (species == null) return;
        boolean unlocked = syncedUnlocked.contains(species.slug());
        boolean evolution = species.isEvolution() && !unlocked;
        int x = 200;
        font.draw(pose, String.format(Locale.ROOT, "%03d  %s", species.number(), species.name()), x, 37, 0xFF404040);
        font.draw(pose, rarityLabel(PokecEconomy.get().rarity(species.slug())), 426, 38, 0xFF404040);
        if (unlocked) {
            renderPreview(pose, species, 354, 119, partialTick);
        } else {
            drawPanel(pose, 220, 48, 268, 112);
            GuiComponent.fill(pose, 222, 50, 486, 158, 0xFFC6C6C6);
            drawCentered(pose, "?", 354, 72, 0xFF404040);
            drawCentered(pose, "ESPÉCIE BLOQUEADA", 354, 132, 0xFF404040);
        }
        GuiComponent.fill(pose, 205, 164, 498, 165, 0xFFC6C6C6);

        if (evolution) {
            font.draw(pose, "DESBLOQUEIO POR EVOLUÇÃO", 207, 173, 0xFF705028);
            drawSlot(pose, 208, 190, "X", 0xFF705028, false);
            font.draw(pose, "Evolua um Minemon seu para esta espécie.", 258, 202, 0xFF404040);
            font.draw(pose, "Almas e PokéCoins não podem desbloqueá-la.", 258, 216, 0xFF404040);
        } else if (unlocked) {
            int price = syncedPrices.getOrDefault(species.slug(), PokecEconomy.get().coins(species.slug()));
            int staged = syncedStagedCoins;
            int balance = syncedCoins;
            int due = syncedFreeAvailable ? 0 : price;
            font.draw(pose, "AQUISIÇÃO DE ESPÉCIE", 207, 171,
                    syncedFreeAvailable ? 0xFF705028 : 0xFF404040);
            font.draw(pose, "Preço: " + (syncedFreeAvailable ? 0 : price) + " PokéCoins", 207, 183, 0xFF404040);
            drawItemSlot(pose, 208, 198, ModItems.POKE_COIN.get(), staged, 0xFF705028);

            boolean canBuy = syncedFreeAvailable || balance + staged >= due;
            drawButton(pose, 408, 197, 82, 30, "ADQUIRIR", canBuy, mouseX, mouseY);
        } else {
            int required = syncedRequirements.getOrDefault(species.slug(), PokecEconomy.get().souls(species.slug()));
            int deposited = syncedDeposits.getOrDefault(species.slug(), 0);
            int staged = syncedStagedSouls;
            String type = species.primaryType();
            font.draw(pose, "ALMAS DE " + type.toUpperCase(Locale.ROOT), 207, 171, 0xFF404040);
            font.draw(pose, "Necessárias: " + required + "   Progresso: " + deposited + "/" + required, 207, 183, 0xFF404040);
            var soulType = SoulType.fromSpeciesType(type);
            var soulItem = soulType == null ? null : ModItems.SOULS.get(soulType.id());
            if (soulItem != null) {
                drawItemSlot(pose, 208, 198, soulItem.get(), Math.min(64, staged), 0xFF404040);
                drawItemSlot(pose, 250, 198, soulItem.get(), Math.max(0, staged - 64), 0xFF404040);
            }
            font.draw(pose, "No slot: " + staged, 300, 202, 0xFF404040);
            font.draw(pose, "Clique para inserir", 300, 216, 0xFF404040);
            boolean ready = deposited + staged >= required;
            drawButton(pose, 408, 198, 82, 36, "LIBERAR", ready, mouseX, mouseY);
        }

        GuiComponent.fill(pose, 205, 240, 498, 241, 0xFFC6C6C6);
        font.draw(pose, "DADOS DA ESPÉCIE", 207, 246, 0xFF404040);
        var definition = SpeciesDefinitions.get(species.slug());
        if (definition != null) {
            font.draw(pose, "Vida:", 207, 260, 0xFF404040);
            drawHealthHearts(pose, (int)Math.ceil(definition.health()), 246, 259);
            font.draw(pose, "Dano: " + formatStat(definition.attack()), 354, 260, 0xFF404040);
            font.draw(pose, "Raridade: " + rarityLabel(PokecEconomy.get().rarity(species.slug())), 207, 289, 0xFF404040);
            font.draw(pose, "Estágio: " + evolutionStage(species), 354, 289, 0xFF404040);
        }
    }

    private void drawLeftConversion(PoseStack pose) {
        GuiComponent.fill(pose, 8, 219, 182, 220, 0xFFC6C6C6);
        font.draw(pose, "DEVOLVER À NATUREZA", 13, 223, 0xFF404040);
        font.draw(pose, "POKÉBOLA", 20, 240, 0xFF404040);
        drawConversionSlot(pose, 20, 253, syncedConversionBall, true);
        if (syncedConversionBall.isEmpty()) {
            font.draw(pose, "Insira uma Pokébola ocupada", 63, 258, 0xFF404040);
        } else if (!syncedConversionSpecies.isEmpty()) {
            var species = PokecSpecies.get(syncedConversionSpecies);
            String name = species == null ? syncedConversionSpecies : species.name();
            font.draw(pose, fitText(name, 110), 63, 250, 0xFF404040);
            font.draw(pose, String.format(Locale.ROOT, "#%03d  Nível %d", species == null ? 0 : species.number(), syncedConversionLevel), 63, 261, 0xFF404040);
            font.draw(pose, rarityLabel(syncedConversionRarity) + " / " + conversionStageLabel(syncedConversionStage), 63, 272, 0xFF404040);
            font.draw(pose, "Base: " + syncedConversionBase + " almas", 20, 294, 0xFF404040);
            font.draw(pose, "Recebe: " + syncedConversionMinimum + "–" + syncedConversionMaximum + " almas", 20, 306, 0xFF404040);
            if (syncedConversionResult >= 0) {
                font.draw(pose, "Devolução concluída: " + syncedConversionResult, 20, 323, 0xFF404040);
            } else {
                drawButton(pose, 20, 341, 152, 22, syncedConversionConfirming ? "CONFIRMAR DEVOLUÇÃO" : "DEVOLVER", true, -1, -1);
                if (syncedConversionConfirming) drawButton(pose, 20, 367, 152, 18, "CANCELAR", true, -1, -1);
            }
        } else if (syncedConversionResult >= 0) {
            font.draw(pose, "Devolução concluída:", 20, 294, 0xFF404040);
            font.draw(pose, syncedConversionResult + " almas entregues", 20, 306, 0xFF404040);
        }
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
        entity.setYRot(180.0F + (float) Math.sin(entity.tickCount * .006F) * 4.0F);
        entity.yRotO = entity.getYRot();
        var def = SpeciesDefinitions.get(species.slug());
        float scale = computePreviewScale(def.width(), def.height(), def.scale());
        pose.pushPose();
        pose.translate(x, y, 120);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Vector3f.XP.rotationDegrees(-6));
        var dispatcher = mc.getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        try {
            dispatcher.render(entity, 0, 0, 0, 0, partialTick, pose, mc.renderBuffers().bufferSource(), 15728880);
            mc.renderBuffers().bufferSource().endBatch();
        } catch (RuntimeException ex) {
            if (Boolean.getBoolean("minemon.clientSmokeTest")) throw ex;
        } finally {
            dispatcher.setRenderShadow(true);
            pose.popPose();
        }
    }

    private static float computePreviewScale(float width, float height, float modelScale) {
        float span = Math.max(height * modelScale, width * modelScale);
        // Fill most of the preview for small species, while scaling giant models
        // to the same available bounding box without changing world dimensions.
        return Math.min(112.0F / Math.max(span, .2F), 180.0F);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int x = (int) ((mouseX - leftPos) / uiScale), y = (int) ((mouseY - topPos) / uiScale);
        if (x >= LIST_X && x < LIST_X + LIST_W && y >= LIST_Y + 13 && y < LIST_Y + 13 + visibleRows() * ROW_H) {
            int index = scrollOffset + (y - (LIST_Y + 13)) / ROW_H;
            if (index >= 0 && index < ordered.size()) selectSpecies(ordered.get(index).slug());
            return true;
        }
        PokecSpecies.Entry species = PokecSpecies.get(selectedSlug);
        if (species == null) return super.mouseClicked(mouseX, mouseY, button);
        if (inRect(x, y, 20, 253, 36, 36)) {
            MinemonNetwork.performPokecAction(menu.pos(), selectedSlug, syncedConversionBall.isEmpty() ? PokecTransactions.Action.STAGE_POKEBALL : PokecTransactions.Action.RETURN_POKEBALL);
            return true;
        }
        if (inRect(x, y, 20, 341, 152, 22) && !syncedConversionBall.isEmpty() && syncedConversionResult < 0) {
            MinemonNetwork.performPokecAction(menu.pos(), selectedSlug, syncedConversionConfirming ? PokecTransactions.Action.CONFIRM_CONVERSION : PokecTransactions.Action.REQUEST_CONVERSION);
            return true;
        }
        if (inRect(x, y, 20, 367, 152, 18) && syncedConversionConfirming) {
            MinemonNetwork.performPokecAction(menu.pos(), selectedSlug, PokecTransactions.Action.CANCEL_CONVERSION);
            return true;
        }
        boolean unlocked = syncedUnlocked.contains(species.slug());
        boolean evolution = species.isEvolution() && !unlocked;
        if (unlocked && !syncedFreeAvailable && inRect(x, y, 208, 198, 36, 36)) {
            MinemonNetwork.performPokecAction(menu.pos(), selectedSlug, PokecTransactions.Action.STAGE_COINS);
            return true;
        }
        if (!unlocked && !evolution && (inRect(x, y, 208, 198, 36, 36) || inRect(x, y, 250, 198, 36, 36))) {
            MinemonNetwork.performPokecAction(menu.pos(), selectedSlug, PokecTransactions.Action.STAGE_SOULS);
            return true;
        }
        if (unlocked && inRect(x, y, 408, 197, 82, 30)) {
            if (syncedFreeAvailable || syncedCoins + syncedStagedCoins >= syncedPrices.getOrDefault(selectedSlug, PokecEconomy.get().coins(selectedSlug)))
                MinemonNetwork.performPokecAction(menu.pos(), selectedSlug, PokecTransactions.Action.PURCHASE);
            return true;
        }
        if (!unlocked && !evolution && inRect(x, y, 408, 198, 82, 36)) {
            MinemonNetwork.performPokecAction(menu.pos(), selectedSlug, PokecTransactions.Action.CONFIRM_SOULS);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean inRect(int x, int y, int rx, int ry, int rw, int rh) { return x >= rx && x < rx + rw && y >= ry && y < ry + rh; }
    private void selectSpecies(String slug) {
        if (slug.equals(selectedSlug)) return;
        selectedSlug = slug;
        previews.clear();
        MinemonNetwork.performPokecAction(menu.pos(), slug, PokecTransactions.Action.SELECT);
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
