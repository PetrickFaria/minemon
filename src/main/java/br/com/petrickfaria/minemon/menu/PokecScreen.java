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
    private static final int LIST_X = 8, LIST_Y = 35, LIST_W = 174, LIST_H = 272, ROW_H = 12;
    private static volatile int syncedCoins;
    private static volatile boolean syncedFreeAvailable = true;
    private static volatile Set<String> syncedUnlocked = Set.of("bulbasaur", "charmander", "squirtle", "pikachu", "eevee");
    private static volatile Map<String, Integer> syncedDeposits = Map.of();
    private static volatile Map<String, Integer> syncedPrices = Map.of();
    private static volatile Map<String, Integer> syncedRequirements = Map.of();
    private static volatile int syncedStagedCoins, syncedStagedSouls;
    private static volatile String syncedSelectedSlug = "bulbasaur";
    private static volatile ItemStack syncedConversionBall = ItemStack.EMPTY, syncedConversionOutput = ItemStack.EMPTY;
    private static volatile int syncedConversionResult = -1, syncedConversionLevel, syncedConversionMaximum, syncedConversionChance;
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
                                   String conversionType, int conversionLevel, int conversionMaximum,
                                   int conversionChance, boolean conversionConfirming) {
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
        syncedConversionMaximum = conversionMaximum; syncedConversionChance = conversionChance;
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

    @Override protected void init() {
        uiScale = Math.min(1.0F, Math.min((width - 8.0F) / LOGICAL_WIDTH, (height - 8.0F) / LOGICAL_HEIGHT));
        uiScale = Math.max(0.55F, uiScale);
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
        GuiComponent.fill(pose, 0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT, 0xF20A1018);
        GuiComponent.fill(pose, 2, 2, LOGICAL_WIDTH - 2, LOGICAL_HEIGHT - 2, 0xFF111D29);
        GuiComponent.fill(pose, 5, 5, LOGICAL_WIDTH - 5, 29, 0xFF152B3D);
        GuiComponent.fill(pose, 5, 29, LOGICAL_WIDTH - 5, 31, 0xFF2384BB);
        font.draw(pose, "POKÉC  /  POKÉDEX", 12, 12, 0xFF9BDFFF);
        font.draw(pose, "Saldo: " + syncedCoins + " PokéCoins", 346, 12, 0xFFFFD778);

        drawPanel(pose, LIST_X, LIST_Y - 5, LIST_W, LIST_H + 8);
        drawPanel(pose, 190, LIST_Y - 5, 322, LOGICAL_HEIGHT - LIST_Y - 8);
        font.draw(pose, "ESPÉCIES   001–151", LIST_X + 7, LIST_Y - 1, 0xFF75CFF5);
        int visible = visibleRows();
        for (int row = 0; row < visible; row++) {
            int index = scrollOffset + row;
            if (index >= ordered.size()) break;
            PokecSpecies.Entry species = ordered.get(index);
            int y = LIST_Y + 13 + row * ROW_H;
            boolean selected = species.slug().equals(selectedSlug);
            boolean unlocked = syncedUnlocked.contains(species.slug());
            GuiComponent.fill(pose, LIST_X + 5, y, LIST_X + LIST_W - 5, y + ROW_H - 1,
                    selected ? 0xFF245879 : (row % 2 == 0 ? 0xFF142330 : 0xFF111F2A));
            if (selected) GuiComponent.fill(pose, LIST_X + 5, y, LIST_X + 7, y + ROW_H - 1, 0xFF56CFFF);
            int color = unlocked ? 0xFFE5F4FC : 0xFF9BAFBD;
            font.draw(pose, String.format(Locale.ROOT, "%03d - %s", species.number(), species.name()), LIST_X + 11, y + 2, color);
        }
        int maxScroll = Math.max(0, ordered.size() - visible);
        int trackX = LIST_X + LIST_W - 8, trackY = LIST_Y + 13;
        GuiComponent.fill(pose, trackX, trackY, trackX + 2, trackY + visible * ROW_H, 0xFF263B4A);
        int thumbHeight = Math.max(18, (visible * ROW_H * visible) / Math.max(1, ordered.size()));
        int thumbY = trackY + (maxScroll == 0 ? 0 : (visible * ROW_H - thumbHeight) * scrollOffset / maxScroll);
        GuiComponent.fill(pose, trackX, thumbY, trackX + 2, thumbY + thumbHeight, 0xFF53B9E7);
        drawSelectedPanel(pose, partialTick, mouseX, mouseY);
        pose.popPose();
    }

    private int visibleRows() { return Math.max(1, (LIST_H - 21) / ROW_H); }
    private void drawPanel(PoseStack pose, int x, int y, int w, int h) {
        GuiComponent.fill(pose, x, y, x + w, y + h, 0xFF10202D);
        GuiComponent.fill(pose, x, y, x + w, y + 1, 0xFF286183);
        GuiComponent.fill(pose, x, y + h - 1, x + w, y + h, 0xFF223E51);
        GuiComponent.fill(pose, x, y, x + 1, y + h, 0xFF223E51);
        GuiComponent.fill(pose, x + w - 1, y, x + w, y + h, 0xFF223E51);
    }

    private void drawSelectedPanel(PoseStack pose, float partialTick, int mouseX, int mouseY) {
        PokecSpecies.Entry species = PokecSpecies.get(selectedSlug);
        if (species == null) return;
        boolean unlocked = syncedUnlocked.contains(species.slug());
        boolean evolution = species.isEvolution() && !unlocked;
        int x = 200;
        font.draw(pose, String.format(Locale.ROOT, "%03d  %s", species.number(), species.name()), x, 37, 0xFFEAF8FF);
        font.draw(pose, rarityLabel(species.rarity()), 426, 38, 0xFF78D7F6);
        renderPreview(pose, species, 354, 119, partialTick);
        GuiComponent.fill(pose, 205, 164, 498, 165, 0xFF2B536B);

        if (evolution) {
            font.draw(pose, "DESBLOQUEIO POR EVOLUÇÃO", 207, 173, 0xFFFF9298);
            drawSlot(pose, 208, 190, "X", 0xFFCB4E5C, false);
            font.draw(pose, "Evolua um Minemon seu para esta espécie.", 258, 202, 0xFFC3D1DB);
            font.draw(pose, "Almas e PokéCoins não podem desbloqueá-la.", 258, 216, 0xFF879CA9);
        } else if (unlocked) {
            int price = syncedPrices.getOrDefault(species.slug(), PokecEconomy.get().coins(species.slug()));
            int staged = syncedStagedCoins;
            int balance = syncedCoins;
            int due = syncedFreeAvailable ? 0 : price;
            font.draw(pose, syncedFreeAvailable ? "PRIMEIRA AQUISIÇÃO GRATUITA" : "AQUISIÇÃO DA ESPÉCIE", 207, 171,
                    syncedFreeAvailable ? 0xFFFFD778 : 0xFF79D9FF);
            font.draw(pose, "Preço: " + (syncedFreeAvailable ? 0 : price) + " PokéCoins", 207, 183, 0xFFE4EEF5);
            drawItemSlot(pose, 208, 198, ModItems.POKE_COIN.get(), staged, 0xFFFFD16A);
            font.draw(pose, "Inseridas: " + staged, 254, 199, 0xFFC5D6E1);
            font.draw(pose, "Clique no slot para inserir", 254, 211, 0xFF8FA6B5);
            font.draw(pose, "Disponível: " + (balance + staged), 254, 223, 0xFF8FA6B5);
            boolean canBuy = syncedFreeAvailable || balance + staged >= due;
            drawButton(pose, 408, 197, 82, 30, "ADQUIRIR", canBuy, mouseX, mouseY);
        } else {
            int required = syncedRequirements.getOrDefault(species.slug(), PokecEconomy.get().souls(species.slug()));
            int deposited = syncedDeposits.getOrDefault(species.slug(), 0);
            int staged = syncedStagedSouls;
            String type = species.primaryType();
            font.draw(pose, "ALMAS DE " + type.toUpperCase(Locale.ROOT), 207, 171, 0xFF79D9FF);
            font.draw(pose, "Necessárias: " + required + "   Progresso: " + deposited + "/" + required, 207, 183, 0xFFE4EEF5);
            var soulType = SoulType.fromSpeciesType(type);
            var soulItem = soulType == null ? null : ModItems.SOULS.get(soulType.id());
            if (soulItem != null) {
                drawItemSlot(pose, 208, 198, soulItem.get(), Math.min(64, staged), 0xFFB9D7EF);
                drawItemSlot(pose, 250, 198, soulItem.get(), Math.max(0, staged - 64), 0xFFB9D7EF);
            }
            font.draw(pose, "No slot: " + staged, 300, 202, 0xFFB6C8D4);
            font.draw(pose, "Clique para inserir", 300, 216, 0xFF8FA6B5);
            boolean ready = deposited + staged >= required;
            drawButton(pose, 408, 198, 82, 36, "LIBERAR", ready, mouseX, mouseY);
        }

        GuiComponent.fill(pose, 205, 240, 498, 241, 0xFF2B536B);
        font.draw(pose, "DADOS DA ESPÉCIE", 207, 246, 0xFF72CFF5);
        var definition = SpeciesDefinitions.get(species.slug());
        if (definition != null) {
            font.draw(pose, "Vida: " + formatStat(definition.health()), 207, 260, 0xFFE1EDF4);
            font.draw(pose, "Dano: " + formatStat(definition.attack()), 354, 260, 0xFFE1EDF4);
            font.draw(pose, "Raridade: " + rarityLabel(species.rarity()), 207, 278, 0xFFE1EDF4);
            font.draw(pose, "Estágio: " + evolutionStage(species), 354, 278, 0xFFE1EDF4);
        }

        GuiComponent.fill(pose, 205, 296, 498, 297, 0xFF2B536B);
        font.draw(pose, "CONVERSÃO EM ALMAS", 207, 301, 0xFF72CFF5);
        font.draw(pose, "Entrada", 208, 315, 0xFFAFBFCA);
        font.draw(pose, "Saída", 274, 315, 0xFFAFBFCA);
        drawConversionSlot(pose, 208, 329, syncedConversionBall, true);
        drawConversionSlot(pose, 274, 329, syncedConversionOutput, false);
        drawCentered(pose, "→", 263, 342, 0xFF7AD7FF);
        if (syncedConversionSpecies.isEmpty()) {
            if (syncedConversionResult >= 0) {
                font.draw(pose, "Conversão concluída.", 322, 329, 0xFFB9C8D2);
                font.draw(pose, "Resultado: " + syncedConversionResult + " alma(s).", 322, 341, syncedConversionResult == 0 ? 0xFFFFA1A1 : 0xFF9BEEAE);
            } else {
                font.draw(pose, "Coloque uma Pokébola ocupada", 322, 329, 0xFFB9C8D2);
                font.draw(pose, "de sua propriedade no slot de entrada.", 322, 341, 0xFF879CA9);
            }
        } else {
            PokecSpecies.Entry stored = PokecSpecies.get(syncedConversionSpecies);
            String storedName = stored == null ? syncedConversionSpecies : stored.name();
            font.draw(pose, "Pokémon: " + storedName + "  Nv. " + syncedConversionLevel, 322, 327, 0xFFE4EEF5);
            font.draw(pose, "Raridade: " + rarityLabel(syncedConversionRarity) + "  Estágio: " + conversionStageLabel(syncedConversionStage), 322, 339, 0xFFB9C8D2);
            font.draw(pose, "Alma: " + soulTypeLabel(syncedConversionType) + "  Máx.: " + syncedConversionMaximum + "  Chance: " + syncedConversionChance + "%", 322, 351, 0xFFB9C8D2);
            if (syncedConversionResult >= 0) font.draw(pose, "Resultado: " + syncedConversionResult + " alma(s)", 322, 363, syncedConversionResult == 0 ? 0xFFFFA1A1 : 0xFF9BEEAE);
        }
        boolean hasInput = !syncedConversionBall.isEmpty() && syncedConversionResult < 0;
        drawButton(pose, 408, 328, 82, 24, syncedConversionConfirming ? "CONFIRMAR" : "CONVERTER", hasInput, mouseX, mouseY);
        if (syncedConversionConfirming) drawButton(pose, 408, 356, 82, 22, "CANCELAR", true, mouseX, mouseY);
    }

    private void drawConversionSlot(PoseStack pose, int x, int y, ItemStack stack, boolean input) {
        int accent = stack.isEmpty() ? 0xFF586B78 : 0xFF52A7CE;
        GuiComponent.fill(pose, x, y, x + 36, y + 36, accent);
        GuiComponent.fill(pose, x + 2, y + 2, x + 34, y + 34, 0xFF0B141C);
        if (!stack.isEmpty()) Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack, x + 10, y + 9);
        else if (input) {
            GuiComponent.fill(pose, x + 11, y + 7, x + 25, y + 10, 0xFF65737C);
            GuiComponent.fill(pose, x + 8, y + 10, x + 28, y + 17, 0xFF929DA3);
            GuiComponent.fill(pose, x + 6, y + 17, x + 30, y + 22, 0xFF65737C);
            GuiComponent.fill(pose, x + 8, y + 22, x + 28, y + 28, 0xFF929DA3);
            GuiComponent.fill(pose, x + 11, y + 28, x + 25, y + 30, 0xFF65737C);
            GuiComponent.fill(pose, x + 15, y + 14, x + 21, y + 23, 0xFF303A40);
            GuiComponent.fill(pose, x + 16, y + 16, x + 20, y + 21, 0xFFB9C3C8);
        } else drawCentered(pose, "→", x + 18, y + 13, 0xFF748894);
        if (!stack.isEmpty() && stack.getCount() > 1) font.draw(pose, Integer.toString(stack.getCount()), x + 27, y + 25, 0xFFFFFFFF);
    }

    private String conversionStageLabel(String stage) {
        return switch (stage) { case "INITIAL" -> "Inicial"; case "INTERMEDIATE" -> "Intermediário"; case "FINAL" -> "Final"; default -> "Único"; };
    }

    private String soulTypeLabel(String type) {
        return switch (type) {
            case "NORMAL" -> "Normal"; case "FIRE" -> "Fogo"; case "WATER" -> "Água"; case "GRASS" -> "Grama";
            case "ELECTRIC" -> "Elétrica"; case "ICE" -> "Gelo"; case "FIGHTING" -> "Lutadora"; case "POISON" -> "Venenosa";
            case "GROUND" -> "Terrestre"; case "FLYING" -> "Voadora"; case "PSYCHIC" -> "Psíquica"; case "BUG" -> "Inseto";
            case "ROCK" -> "Pedra"; case "GHOST" -> "Fantasma"; case "DRAGON" -> "Dragão"; default -> type;
        };
    }

    private String formatStat(double value) { return String.format(Locale.ROOT, "%.0f", value); }
    private String rarityLabel(String rarity) {
        return switch (rarity) {
            case "common" -> "Comum";
            case "uncommon" -> "Incomum";
            case "rare" -> "Raro";
            case "very_rare" -> "Muito raro";
            case "legendary" -> "Lendário/Mítico";
            default -> rarity;
        };
    }
    private String evolutionStage(PokecSpecies.Entry species) {
        int depth = 0;
        PokecSpecies.Entry current = species;
        while (current != null && current.evolvesFrom() != null) { depth++; current = PokecSpecies.get(current.evolvesFrom()); }
        if (depth == 0) {
            boolean hasEvolution = PokecSpecies.ALL.values().stream().anyMatch(candidate -> species.slug().equals(candidate.evolvesFrom()));
            return hasEvolution ? "Básico" : "Estágio único";
        }
        return depth == 1 ? "Primeira evolução" : "Segunda evolução";
    }

    private void drawSlot(PoseStack pose, int x, int y, String icon, int accent, boolean active) {
        int border = active ? accent : 0xFF657785;
        GuiComponent.fill(pose, x, y, x + 36, y + 36, border);
        GuiComponent.fill(pose, x + 2, y + 2, x + 34, y + 34, 0xFF0B141C);
        drawCentered(pose, icon, x + 18, y + 12, active ? accent : 0xFF8998A3);
        if (!active) drawCentered(pose, "X", x + 18, y + 12, 0xFFE85765);
    }

    private void drawItemSlot(PoseStack pose, int x, int y, net.minecraft.world.item.Item item, int count, int accent) {
        GuiComponent.fill(pose, x, y, x + 36, y + 36, accent);
        GuiComponent.fill(pose, x + 2, y + 2, x + 34, y + 34, 0xFF0B141C);
        Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(new ItemStack(item), x + 10, y + 9);
        if (count > 0) font.draw(pose, Integer.toString(count), x + 24 - font.width(Integer.toString(count)), y + 26, 0xFFFFFFFF);
    }

    private void drawButton(PoseStack pose, int x, int y, int w, int h, String label, boolean enabled, int mouseX, int mouseY) {
        int color = enabled ? 0xFF2476A1 : 0xFF394852;
        GuiComponent.fill(pose, x, y, x + w, y + h, color);
        GuiComponent.fill(pose, x + 1, y + 1, x + w - 1, y + h - 1, enabled ? 0xFF173D54 : 0xFF202B32);
        int mx = (int) ((mouseX - leftPos) / uiScale), my = (int) ((mouseY - topPos) / uiScale);
        if (enabled && mx >= x && mx < x + w && my >= y && my < y + h)
            GuiComponent.fill(pose, x + 1, y + 1, x + w - 1, y + 3, 0xFF68D5FF);
        drawCentered(pose, label, x + w / 2.0F, y + (h - 8) / 2.0F, enabled ? 0xFFE7F8FF : 0xFF9CA8AF);
    }

    private void drawCentered(PoseStack pose, String text, float x, float y, int color) {
        font.draw(pose, text, x - font.width(text) / 2.0F, y, color);
    }

    private void renderPreview(PoseStack pose, PokecSpecies.Entry species, int x, int y, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            drawCentered(pose, "Modelo disponível no mundo", x, y, 0xFF7794A7);
            return;
        }
        MinemonCreature entity = previews.computeIfAbsent(species.slug(), slug -> {
            var type = ModEntities.SPECIES.get(slug);
            if (type == null) return null;
            MinemonCreature preview = type.get().create(mc.level);
            if (preview != null) { preview.setNoAi(true); preview.setDeltaMovement(0, 0, 0); }
            return preview;
        });
        if (entity == null) return;
        entity.tickCount = (int) (System.currentTimeMillis() / 50L);
        entity.setYRot(180.0F + (float) Math.sin(entity.tickCount * .006F) * 4.0F);
        entity.yRotO = entity.getYRot();
        var def = SpeciesDefinitions.get(species.slug());
        float span = Math.max(def.height() * def.scale(), def.width() * def.scale());
        float scale = Math.min(82.0F / Math.max(span, .2F), 2.6F);
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
        if (inRect(x, y, 208, 329, 36, 36)) {
            MinemonNetwork.performPokecAction(menu.pos(), selectedSlug, syncedConversionBall.isEmpty() ? PokecTransactions.Action.STAGE_POKEBALL : PokecTransactions.Action.RETURN_POKEBALL);
            return true;
        }
        if (inRect(x, y, 274, 329, 36, 36) && !syncedConversionOutput.isEmpty()) {
            MinemonNetwork.performPokecAction(menu.pos(), selectedSlug, PokecTransactions.Action.CLAIM_CONVERSION_OUTPUT);
            return true;
        }
        if (inRect(x, y, 408, 328, 82, 24) && !syncedConversionBall.isEmpty() && syncedConversionResult < 0) {
            MinemonNetwork.performPokecAction(menu.pos(), selectedSlug, syncedConversionConfirming ? PokecTransactions.Action.CONFIRM_CONVERSION : PokecTransactions.Action.REQUEST_CONVERSION);
            return true;
        }
        if (inRect(x, y, 408, 356, 82, 22) && syncedConversionConfirming) {
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
