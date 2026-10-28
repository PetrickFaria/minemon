package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.NonNullList;
import net.minecraftforge.event.TickEvent;

/** Loads each registered treat model in a real Minecraft client and verifies the creative tab. */
public final class PokemonTreatClientSmokeTest {
    private static int titleTicks;
    private static boolean done;
    public static void tick(TickEvent.ClientTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END || !Boolean.getBoolean("minemon.clientSmokeTest")
                || !Boolean.getBoolean("minemon.clientSmokeTreats")) return;
        Minecraft minecraft=Minecraft.getInstance();
        if (!(minecraft.screen instanceof TitleScreen) || ++titleTicks < 20) return;
        try {
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
            java.nio.file.Files.writeString(java.nio.file.Path.of("client-validation-treats.txt"),
                    "[TREAT CLIENT] PASS: all 16 item models, translations, and creative entries loaded\n");
            done=true;
            System.out.println("[TREAT CLIENT] PASS: all 16 item models, translations, and creative entries loaded");
            minecraft.execute(minecraft::stop);
        } catch (Exception failure) {
            throw new IllegalStateException("Treat client validation failed",failure);
        }
    }
    private PokemonTreatClientSmokeTest() {}
}
