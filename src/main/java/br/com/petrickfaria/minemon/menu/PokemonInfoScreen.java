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
    private Button freeButton;
    private BlackListScreen blackListOverlay;

    public PokemonInfoScreen(PokemonInfoMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 260;
        inventoryLabelY = 500;
        titleLabelX = -1000;
    }

    @Override protected void init() {
        super.init();
        evolutionButtons.clear();
        int y = topPos + 174;
        followButton = addRenderableWidget(new Button(leftPos + 14, y, 70, 20, Component.literal(
                menu.pokemon() != null && menu.pokemon().isOrderedToSit() ? "Seguir" : "Seguindo"),
                button -> sendButton(0)));
        sitButton = addRenderableWidget(new Button(leftPos + 93, y, 70, 20, Component.literal(
                menu.pokemon() != null && menu.pokemon().isOrderedToSit() ? "Sentado" : "Sentar"),
                button -> sendButton(1)));
        freeButton = addRenderableWidget(new Button(leftPos + 172, y, 70, 20,
                Component.literal(menu.pokemon() != null && menu.pokemon().isFreeMode() ? "Livre ✓" : "Livre"),
                button -> sendButton(2)));
        addRenderableWidget(new Button(leftPos + 93, topPos + 198, 70, 18, Component.literal("Black List"),
                button -> sendButton(3)));
        MinemonCreature pokemon = menu.pokemon();
        List<PokecSpecies.Entry> options = menu.evolutionOptions();
        boolean ready = pokemon != null && !options.isEmpty()
                && pokemon.getPokemonLevel() >= PokemonEvolutionRules.requiredLevel(pokemon.getSpeciesSlug());
        if (ready) {
            addRenderableWidget(new Button(leftPos + 83, topPos + 220, 90, 18, Component.literal("Evoluir"), button -> {
                if (options.size() == 1) sendButton(4);
                else {
                    choosingEvolution = !choosingEvolution;
                    evolutionButtons.forEach(option -> option.visible = choosingEvolution);
                }
            }));
            int optionY = topPos + 240;
            for (int i = 0; i < options.size(); i++) {
                final int index = i;
                Button choice = new Button(leftPos + 8 + i * 80, optionY, 78, 18,
                        Component.literal(options.get(i).name()), button -> sendButton(4 + index));
                choice.visible = false;
                evolutionButtons.add(addRenderableWidget(choice));
            }
        }
        if (blackListOverlay != null) blackListOverlay.initializeOverlay(minecraft, width, height);
    }

    private void sendButton(int action) {
        if (Boolean.getBoolean("minemon.clientSmokeTest"))
            com.mojang.logging.LogUtils.getLogger().info("[POKEMON SCREEN BUTTON] action={} container={}", action, menu.containerId);
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
    }

    public void openBlackList(int entityId, java.util.UUID uuid, java.util.Map<String, Boolean> overrides) {
        if (blackListOverlay == null)
            blackListOverlay = new BlackListScreen(this, entityId, uuid, overrides);
        else
            blackListOverlay.replaceOverrides(overrides);
        blackListOverlay.initializeOverlay(minecraft, width, height);
    }

    public void closeBlackListOverlay(BlackListScreen overlay) {
        if (blackListOverlay == overlay) blackListOverlay = null;
    }

    public void replaceBlackListOverrides(java.util.Map<String, Boolean> overrides) {
        if (blackListOverlay != null) blackListOverlay.replaceOverrides(overrides);
    }

    public boolean hasBlackListOverlay() { return blackListOverlay != null; }
    public BlackListScreen blackListOverlayForSmokeTest() { return blackListOverlay; }
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
        font.draw(pose, "Vida:", leftPos + 18, topPos + 44, 0xFF404040);
        float healthHeight=drawHeartValue(pose,leftPos+54,topPos+41,pokemon.getHealth(),maxHealth,false,188);
        int damageY=topPos+Math.max(58,(int)Math.ceil(41+healthHeight+5));
        font.draw(pose, "Dano:", leftPos + 18, damageY+2, 0xFF404040);
        float damageHeight=drawHeartValue(pose,leftPos+53,damageY,pokemon.getAttributeValue(Attributes.ATTACK_DAMAGE),
                pokemon.getAttributeValue(Attributes.ATTACK_DAMAGE),true,91);
        font.draw(pose, "Ataque: " + formatRange(pokemon.attackRange()), leftPos + 151, damageY+2, 0xFF404040);
        int detailsY=damageY+Math.max(13,(int)Math.ceil(damageHeight))+4;
        font.draw(pose, "Estágio: " + evolutionStage(pokemon.getSpeciesSlug()), leftPos + 18, detailsY, 0xFF404040);
        font.draw(pose, "Raridade: " + Component.translatable("rarity.minemon_gen1."+br.com.petrickfaria.minemon.entity.PokemonProgression.rarityKey(pokemon.getPokemonRarity())).getString(), leftPos + 151, detailsY, 0xFF404040);
        font.draw(pose, "Percepção: " + formatRange(pokemon.perceptionRange()), leftPos + 18, detailsY + 10, 0xFF404040);

        int barX = leftPos + 37, barY = topPos + Math.max(134,detailsY-topPos+31);
        int needed = pokemon.getPokemonXpToNextLevel();
        float progress = needed == 0 ? 1.0F : Math.min(1.0F, (float)(pokemon.getPokemonExperienceExact() / needed));
        RenderSystem.setShaderTexture(0, ICONS);
        GuiComponent.blit(pose, barX, barY, 0, 64, 182, 5, 256, 256);
        int fill = Math.round(progress * 182.0F);
        if (fill > 0) GuiComponent.blit(pose, barX, barY, 0, 69, fill, 5, 256, 256);
        String level = "Nível " + pokemon.getPokemonLevel();
        font.draw(pose, level, leftPos + (imageWidth - font.width(level)) / 2.0F, barY - 12, 0xFF303030);
        String xp = needed == 0 ? "XP máximo" : pokemon.getPokemonExperienceText() + " / " + needed + " XP";
        font.draw(pose, xp, leftPos + (imageWidth - font.width(xp)) / 2.0F, barY + 8, 0xFF404040);
        List<PokecSpecies.Entry> options = menu.evolutionOptions();
        if (!options.isEmpty()) {
            int required = PokemonEvolutionRules.requiredLevel(pokemon.getSpeciesSlug());
            String line = "Evolui no nível " + required;
            font.draw(pose, line, leftPos + (imageWidth - font.width(line)) / 2.0F, barY + 18, 0xFF404040);
        }
    }

    private float drawHeartValue(PoseStack pose,int x,int y,double current,double maximum,boolean black,int maxWidth){
        int count=Math.max(1,(int)Math.ceil(maximum/2.0D));
        float scale=Math.max(0.65F,Math.min(1.0F,maxWidth/(Math.min(count,28)*9.0F)));
        int perRow=Math.max(1,(int)Math.floor(maxWidth/(9.0F*scale)));
        int rows=(count+perRow-1)/perRow;
        RenderSystem.setShaderTexture(0,ICONS);
        RenderSystem.setShaderColor(black?0.03F:1.0F,black?0.03F:1.0F,black?0.03F:1.0F,1.0F);
        for(int i=0;i<count;i++){
            double hp=current-i*2.0D;
            int u=heartSpriteU(hp);
            int row=i/perRow,column=i%perRow;
            pose.pushPose();pose.translate(x+column*9.0F*scale,y+row*9.0F*scale,0);pose.scale(scale,scale,1.0F);
            GuiComponent.blit(pose,0,0,u,0,9,9,256,256);
            pose.popPose();
        }
        RenderSystem.setShaderColor(1,1,1,1);
        return rows*9.0F*scale;
    }

    static int heartSpriteU(double remainingHealthPoints) {
        if (remainingHealthPoints >= 2.0D) return 52; // full heart
        if (remainingHealthPoints >= 1.0D) return 61; // half heart
        return 16; // empty outline
    }

    private static String formatRange(double range){
        String value=String.format(java.util.Locale.ROOT,"%.1f",range);
        return value.endsWith(".0")?value.substring(0,value.length()-2):value;
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
            if (freeButton != null) freeButton.setMessage(Component.literal(pokemon.isFreeMode() ? "Livre ✓" : "Livre"));
        }
        super.render(pose, mouseX, mouseY, partialTick);
        renderTooltip(pose, mouseX, mouseY);
        if (blackListOverlay != null) blackListOverlay.render(pose, mouseX, mouseY, partialTick);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (blackListOverlay != null) {
            blackListOverlay.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (blackListOverlay != null) {
            blackListOverlay.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (blackListOverlay != null) {
            blackListOverlay.mouseReleased(mouseX, mouseY, button);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (blackListOverlay != null) {
            blackListOverlay.mouseScrolled(mouseX, mouseY, delta);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (blackListOverlay != null) {
            blackListOverlay.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
