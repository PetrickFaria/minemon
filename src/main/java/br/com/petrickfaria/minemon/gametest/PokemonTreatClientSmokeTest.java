package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.registry.ModItems;
import br.com.petrickfaria.minemon.menu.PokecMenu;
import br.com.petrickfaria.minemon.menu.PokecScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;

/** Loads all treat and PokéCoin models in a real Minecraft client and verifies the creative tab. */
public final class PokemonTreatClientSmokeTest {
    private static int clientTicks;
    private static boolean done;
    private static boolean pokecUiPhase;
    private static boolean pokecSelectionPhase;
    public static void tick(TickEvent.ClientTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END || !Boolean.getBoolean("minemon.clientSmokeTest")
                || !Boolean.getBoolean("minemon.clientSmokeTreats")) return;
        Minecraft minecraft=Minecraft.getInstance();
        if (++clientTicks == 1) {
            System.out.println("[TREAT CLIENT] tick handler active; screen="
                    + (minecraft.screen == null ? "null" : minecraft.screen.getClass().getSimpleName()));
        }
        // This smoke only inspects registered item resources, so it can run in
        // either the title screen or an already opened world.
        if (clientTicks < 20) return;
        try {
            if (pokecSelectionPhase) {
                if (!(minecraft.screen instanceof PokecScreen screen) || !screen.selectedSpeciesForSmokeTest("mew"))
                    throw new IllegalStateException("PokéC list click did not select Mew in the detail panel");
                java.nio.file.Files.writeString(java.nio.file.Path.of("client-validation-treats.txt"),
                        "[TREAT CLIENT] PASS: resources, 151-species list, list selection, and animated PokéC preview rendered\n");
                done = true;
                System.out.println("[TREAT CLIENT] PASS: resources, 151-species list, list selection, and animated PokéC preview rendered");
                minecraft.execute(minecraft::stop);
                return;
            }
            if (pokecUiPhase) {
                if (!(minecraft.screen instanceof PokecScreen screen))
                    throw new IllegalStateException("PokéC selector closed during focused UI render check");
                if (screen.catalogSizeForSmokeTest() != 151)
                    throw new IllegalStateException("PokéC UI did not load the complete 151-species list");
                if (!screen.clickSpeciesForSmokeTest("mew"))
                    throw new IllegalStateException("PokéC list could not select Mew through its click handler");
                pokecSelectionPhase = true;
                clientTicks = 0;
                return;
            }
            NonNullList<net.minecraft.world.item.ItemStack> tab=NonNullList.create();
            ModItems.MINEMON_TAB.fillItemList(tab);
            long treatCount=tab.stream().filter(stack->stack.getItem() instanceof br.com.petrickfaria.minemon.item.PokemonTreatItem).count();
            if (treatCount != 16) throw new IllegalStateException("Creative tab exposes "+treatCount+" treats instead of 16");
            for (var entry : ModItems.TREATS.entrySet()) {
                var item=entry.getValue().get();
                var id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
                if (id==null) throw new IllegalStateException("Treat has no registered ID: "+entry.getKey());
                var modelId=new ModelResourceLocation(id,"inventory");
                var model=minecraft.getModelManager().getModel(modelId);
                if (model==minecraft.getModelManager().getMissingModel()) throw new IllegalStateException("Missing baked treat model: "+modelId);
                var key="item.minemon."+entry.getKey().itemId();
                if (net.minecraft.client.resources.language.I18n.get(key).equals(key)) throw new IllegalStateException("Missing translated treat name: "+key);
            }
            var coin=ModItems.POKE_COIN.get();
            var coinId=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(coin);
            if (coinId==null || !coinId.toString().equals("minemon:poke_coin"))
                throw new IllegalStateException("PokéCoin has wrong registry ID: "+coinId);
            if (coin.getMaxStackSize()!=64) throw new IllegalStateException("PokéCoin must stack to 64");
            if (tab.stream().filter(stack->stack.getItem()==coin).count()!=1)
                throw new IllegalStateException("PokéCoin must appear exactly once in the Minemon creative tab");
            var coinModelId=new ModelResourceLocation(coinId,"inventory");
            if (minecraft.getModelManager().getModel(coinModelId)==minecraft.getModelManager().getMissingModel())
                throw new IllegalStateException("Missing baked PokéCoin model: "+coinModelId);
            var coinName="item.minemon.poke_coin";
            if (net.minecraft.client.resources.language.I18n.get(coinName).equals(coinName))
                throw new IllegalStateException("Missing PokéCoin translation: "+coinName);
            try (var texture=PokemonTreatClientSmokeTest.class.getResourceAsStream("/assets/minemon/textures/item/poke_coin.png")) {
                var image=texture==null?null:javax.imageio.ImageIO.read(texture);
                if (image==null || image.getWidth()!=32 || image.getHeight()!=32 || !image.getColorModel().hasAlpha())
                    throw new IllegalStateException("PokéCoin texture must be a 32x32 transparent PNG");
            }
            if (minecraft.player != null && minecraft.level != null) {
                pokecUiPhase = true;
                clientTicks = 0;
                var menu = new PokecMenu(minecraft.player.containerMenu.containerId + 1,
                        minecraft.player.getInventory(), minecraft.player.blockPosition());
                minecraft.setScreen(new PokecScreen(menu, minecraft.player.getInventory(), Component.literal("PokéC")));
                return;
            }
            java.nio.file.Files.writeString(java.nio.file.Path.of("client-validation-treats.txt"),
                    "[TREAT CLIENT] PASS: resources loaded; no world was available for the PokéC screen render check\n");
            done = true;
            System.out.println("[TREAT CLIENT] PASS: resources loaded; screen render check skipped (no world)");
            minecraft.execute(minecraft::stop);
        } catch (Exception failure) {
            throw new IllegalStateException("Treat client validation failed",failure);
        }
    }
    private PokemonTreatClientSmokeTest() {}
}
