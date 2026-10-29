package br.com.petrickfaria.minemon.menu;

import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.pokec.PokemonEvolutionRules;
import br.com.petrickfaria.minemon.pokec.PokecSpecies;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import java.util.ArrayList;
import java.util.List;

/** Species-owned status panel for one domesticated Pokémon. */
public final class PokemonInfoScreen extends AbstractContainerScreen<PokemonInfoMenu> {
    private static final ResourceLocation ICONS = new ResourceLocation("minecraft", "textures/gui/icons.png");
    private boolean choosingEvolution;
    private final List<Button> evolutionButtons = new ArrayList<>();
    private Button followButton;
    private Button sitButton;

    public PokemonInfoScreen(PokemonInfoMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 222;
        inventoryLabelY = 500;
        titleLabelX = -1000;
    }

    @Override protected void init() {
        super.init();
        evolutionButtons.clear();
        int y = topPos + 140;
        followButton = addRenderableWidget(new Button(leftPos + 14, y, 70, 20, Component.literal(
                menu.pokemon() != null && menu.pokemon().isOrderedToSit() ? "Seguir" : "Seguindo"),
                button -> sendButton(0)));
        sitButton = addRenderableWidget(new Button(leftPos + 93, y, 70, 20, Component.literal(
                menu.pokemon() != null && menu.pokemon().isOrderedToSit() ? "Sentado" : "Sentar"),
                button -> sendButton(1)));
        addRenderableWidget(new Button(leftPos + 172, y, 70, 20, Component.literal("Black List"),
                button -> sendButton(2)));
        MinemonCreature pokemon = menu.pokemon();
        List<PokecSpecies.Entry> options = menu.evolutionOptions();
        boolean ready = pokemon != null && !options.isEmpty()
                && pokemon.getPokemonLevel() >= PokemonEvolutionRules.requiredLevel(pokemon.getSpeciesSlug());
        if (ready) {
            addRenderableWidget(new Button(leftPos + 83, topPos + 164, 90, 18, Component.literal("Evoluir"), button -> {
                if (options.size() == 1) sendButton(3);
                else {
                    choosingEvolution = !choosingEvolution;
                    evolutionButtons.forEach(option -> option.visible = choosingEvolution);
                }
            }));
            int optionY = topPos + 187;
            for (int i = 0; i < options.size(); i++) {
                final int index = i;
                Button choice = new Button(leftPos + 8 + i * 80, optionY, 78, 18,
                        Component.literal(options.get(i).name()), button -> sendButton(3 + index));
                choice.visible = false;
                evolutionButtons.add(addRenderableWidget(choice));
            }
        }
    }

    private void sendButton(int action) {
        if (Boolean.getBoolean("minemon.clientSmokeTest"))
            com.mojang.logging.LogUtils.getLogger().info("[POKEMON SCREEN BUTTON] action={} container={}", action, menu.containerId);
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
    }

    public void openBlackList(int entityId, java.util.UUID uuid, java.util.Map<String, Boolean> overrides) {
        if (Minecraft.getInstance() != null)
            Minecraft.getInstance().setScreen(new BlackListScreen(this, entityId, uuid, overrides));
    }
    public MinemonCreature pokemonForSmokeTest() { return menu.pokemon(); }
    public String headerForSmokeTest() {
        MinemonCreature pokemon = menu.pokemon();
        PokecSpecies.Entry species = pokemon == null ? null : PokecSpecies.get(pokemon.getSpeciesSlug());
        return species == null ? "" : String.format(java.util.Locale.ROOT, "%03d - %s", species.number(), species.name());
    }
    public boolean evolveButtonAvailableForSmokeTest() {
        MinemonCreature pokemon = menu.pokemon();
        return pokemon != null && !menu.evolutionOptions().isEmpty()
                && pokemon.getPokemonLevel() >= PokemonEvolutionRules.requiredLevel(pokemon.getSpeciesSlug());
    }

    @Override protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY) {
        MinemonGui.panel(pose, leftPos, topPos, imageWidth, imageHeight);
        GuiComponent.fill(pose, leftPos + 8, topPos + 34, leftPos + imageWidth - 8, topPos + 35, 0xFF647E92);
        MinemonCreature pokemon = menu.pokemon();
        if (pokemon == null) return;
        PokecSpecies.Entry species = PokecSpecies.get(pokemon.getSpeciesSlug());
        if (species == null) return;
        String title = String.format(java.util.Locale.ROOT, "%03d - %s", species.number(), species.name());
        font.draw(pose, title, leftPos + (imageWidth - font.width(title)) / 2.0F, topPos + 17, 0xFF303030);
        float maxHealth = pokemon.getMaxHealth();
        double attack = pokemon.getAttribute(Attributes.ATTACK_DAMAGE) == null ? 0.0D
                : pokemon.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
        font.draw(pose, String.format(java.util.Locale.ROOT, "Vida: %.1f / %.1f", pokemon.getHealth(), maxHealth),
                leftPos + 18, topPos + 43, 0xFF404040);
        font.draw(pose, String.format(java.util.Locale.ROOT, "Dano: %.1f", attack), leftPos + 142, topPos + 43, 0xFF404040);
        font.draw(pose, "Raridade: " + pokemon.getPokemonRarity(), leftPos + 18, topPos + 58, 0xFF404040);
        font.draw(pose, "Estágio: " + evolutionStage(pokemon.getSpeciesSlug()), leftPos + 142, topPos + 58, 0xFF404040);

        int barX = leftPos + 37, barY = topPos + 119;
        int needed = pokemon.getPokemonXpToNextLevel();
        float progress = needed == 0 ? 1.0F : Math.min(1.0F, pokemon.getPokemonExperience() / (float) needed);
        RenderSystem.setShaderTexture(0, ICONS);
        GuiComponent.blit(pose, barX, barY, 0, 64, 182, 5, 256, 256);
        int fill = Math.round(progress * 182.0F);
        if (fill > 0) GuiComponent.blit(pose, barX, barY, 0, 69, fill, 5, 256, 256);
        String level = "Nível " + pokemon.getPokemonLevel();
        font.draw(pose, level, leftPos + (imageWidth - font.width(level)) / 2.0F, barY - 12, 0xFF303030);
        String xp = needed == 0 ? "XP máximo" : pokemon.getPokemonExperience() + " / " + needed + " XP";
        font.draw(pose, xp, leftPos + (imageWidth - font.width(xp)) / 2.0F, barY + 8, 0xFF404040);
        List<PokecSpecies.Entry> options = menu.evolutionOptions();
        if (!options.isEmpty()) {
            int required = PokemonEvolutionRules.requiredLevel(pokemon.getSpeciesSlug());
            String line = "Evolui no nível " + required;
            font.draw(pose, line, leftPos + (imageWidth - font.width(line)) / 2.0F, topPos + 91, 0xFF404040);
        }
    }

    private static String evolutionStage(String slug) {
        boolean hasParent = PokecSpecies.get(slug) != null && PokecSpecies.get(slug).isEvolution();
        boolean hasChild = !PokemonEvolutionRules.nextForms(slug).isEmpty();
        boolean childHasChild = PokemonEvolutionRules.nextForms(slug).stream()
                .anyMatch(next -> !PokemonEvolutionRules.nextForms(next.slug()).isEmpty());
        if (!hasParent && childHasChild) return "Inicial";
        if (hasParent && hasChild) return "Intermediário";
        if (hasParent || hasChild) return "Final";
        return "Final";
    }

    @Override protected void renderLabels(PoseStack pose, int mouseX, int mouseY) { }
    @Override public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        MinemonCreature pokemon = menu.pokemon();
        if (pokemon != null) {
            if (followButton != null) followButton.setMessage(Component.literal(pokemon.isOrderedToSit() ? "Seguir" : "Seguindo"));
            if (sitButton != null) sitButton.setMessage(Component.literal(pokemon.isOrderedToSit() ? "Sentado" : "Sentar"));
        }
        super.render(pose, mouseX, mouseY, partialTick);
        renderTooltip(pose, mouseX, mouseY);
    }
}
