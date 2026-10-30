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
    private static boolean worldOpening;
    private static boolean pokecUiPhase;
    private static boolean pokecSelectionPhase;
    private static boolean pokecUnlockedPreviewPhase;
    private static boolean pokeballPhase;
    private static boolean pokeballInteractionSent;
    private static volatile int pokeballFixtureEntityId = -1;
    private static volatile boolean pokeballCaptureVerified;
    private static int worldWaitTicks;
    public static void tick(TickEvent.ClientTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END || !Boolean.getBoolean("minemon.clientSmokeTest")
                || !Boolean.getBoolean("minemon.clientSmokeTreats")) return;
        Minecraft minecraft=Minecraft.getInstance();
        if (++clientTicks == 1) {
            System.out.println("[TREAT CLIENT] tick handler active; screen="
                    + (minecraft.screen == null ? "null" : minecraft.screen.getClass().getSimpleName()));
        }
        if (worldOpening) {
            if (minecraft.player != null && minecraft.level != null) {
                worldOpening = false;
                openPokecUi(minecraft);
                return;
            }
            if (++worldWaitTicks > 600) throw new IllegalStateException("Timed out opening the prepared PokéC client world");
            return;
        }
        if (clientTicks < 20) return;
        try {
            if (pokeballPhase) {
                var server = minecraft.getSingleplayerServer();
                if (server == null || minecraft.player == null || minecraft.level == null)
                    throw new IllegalStateException("Pokébola interaction smoke requires the integrated client world");
                if (pokeballFixtureEntityId < 0) {
                    pokeballFixtureEntityId = -2;
                    server.execute(() -> {
                        var owner = server.getPlayerList().getPlayer(minecraft.player.getUUID());
                        var level = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                        if (owner == null || level == null) return;
                        level.setDayTime(6000L);
                        var type = br.com.petrickfaria.minemon.registry.ModEntities.SPECIES.get("pikachu");
                        var creature = type == null ? null : type.get().create(level);
                        if (creature == null) return;
                        creature.tame(owner);
                        creature.setOrderedToSit(true);
                        creature.setNoAi(true);
                        creature.moveTo(owner.getX() + 2.0D, owner.getY(), owner.getZ(), owner.getYRot(), 0.0F);
                        if (level.addFreshEntity(creature)) {
                            owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                                    new net.minecraft.world.item.ItemStack(ModItems.POKEBALL.get()));
                            pokeballFixtureEntityId = creature.getId();
                        }
                    });
                    return;
                }
                if (pokeballFixtureEntityId == -2) return;
                var target = minecraft.level.getEntity(pokeballFixtureEntityId);
                if (!pokeballInteractionSent && target instanceof br.com.petrickfaria.minemon.entity.MinemonCreature) {
                    minecraft.setScreen(null);
                    minecraft.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                            new net.minecraft.world.item.ItemStack(ModItems.POKEBALL.get()));
                    minecraft.gameMode.interact(minecraft.player, target, net.minecraft.world.InteractionHand.MAIN_HAND);
                    pokeballInteractionSent = true;
                    clientTicks = 0;
                    return;
                }
                if (pokeballCaptureVerified) {
                    var held = minecraft.player.getMainHandItem();
                    if (!br.com.petrickfaria.minemon.item.PokeballItem.isOccupied(held)) return;
                    java.nio.file.Files.writeString(java.nio.file.Path.of("client-validation-treats.txt"),
                            "[TREAT CLIENT] PASS: grass treat resources, PokéC locked/unlocked preview rules, and live Pokébola entity interaction captured an owned creature\n"
                                    + "PokéCoin and Pokébola models, translations, textures, and creative entries loaded\n");
                    done = true;
                    System.out.println("[TREAT CLIENT] PASS: real client right-click interaction captured the owned Pikachu into the Pokébola");
                    System.out.println("PokéCoin and Pokébola models, translations, textures, and creative entries loaded");
                    minecraft.execute(minecraft::stop);
                    return;
                }
                if (pokeballInteractionSent) {
                    server.execute(() -> {
                        var owner = server.getPlayerList().getPlayer(minecraft.player.getUUID());
                        var level = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                        if (owner != null && level != null && br.com.petrickfaria.minemon.item.PokeballItem.isOccupied(owner.getMainHandItem())
                                && level.getEntity(pokeballFixtureEntityId) == null) pokeballCaptureVerified = true;
                    });
                    if (clientTicks > 400) throw new IllegalStateException("Live client Pokébola interaction did not capture the owned Pokémon");
                }
                return;
            }
            if (pokecUnlockedPreviewPhase) {
                if (!(minecraft.screen instanceof PokecScreen screen) || !screen.selectedSpeciesForSmokeTest("bulbasaur")
                        || !screen.previewExistsForSmokeTest("bulbasaur"))
                    throw new IllegalStateException("Unlocked Bulbasaur preview was not instantiated after selection");
                pokeballPhase = true;
                pokeballFixtureEntityId = -1;
                clientTicks = 0;
                return;
            }
            if (pokecSelectionPhase) {
                if (!(minecraft.screen instanceof PokecScreen screen) || !screen.selectedSpeciesForSmokeTest("mew"))
                    throw new IllegalStateException("PokéC list click did not select Mew in the detail panel");
                if (screen.speciesUnlockedForSmokeTest("mew") && !screen.previewExistsForSmokeTest("mew"))
                    throw new IllegalStateException("Unlocked Mew preview was not created");
                if (!screen.speciesUnlockedForSmokeTest("mew") && screen.previewExistsForSmokeTest("mew"))
                    throw new IllegalStateException("Locked Mew preview must not be instantiated");
                if (!screen.clickSpeciesForSmokeTest("bulbasaur"))
                    throw new IllegalStateException("PokéC list could not return to Bulbasaur through its click handler");
                pokecUnlockedPreviewPhase = true;
                clientTicks = 0;
                return;
            }
            if (pokecUiPhase) {
                if (!(minecraft.screen instanceof PokecScreen screen))
                    throw new IllegalStateException("PokéC selector closed during focused UI render check");
                if (screen.catalogSizeForSmokeTest() != 151)
                    throw new IllegalStateException("PokéC UI did not load the complete 151-species list");
                if (!screen.conversionSlotsBelowListForSmokeTest())
                    throw new IllegalStateException("PokéC conversion slots must remain below the left Pokédex list");
                if (screen.previewScaleForSmokeTest("pikachu") <= screen.previewScaleForSmokeTest("onix"))
                    throw new IllegalStateException("PokéC preview should scale Pikachu larger than giant Onix");
                if (!screen.clickSpeciesForSmokeTest("mew"))
                    throw new IllegalStateException("PokéC list could not select Mew through its click handler");
                pokecSelectionPhase = true;
                clientTicks = 0;
                return;
            }
            NonNullList<net.minecraft.world.item.ItemStack> tab=NonNullList.create();
            ModItems.MINEMON_TAB.fillItemList(tab);
            long soulCount = tab.stream().filter(stack -> stack.getItem() instanceof br.com.petrickfaria.minemon.pokec.SoulItem).count();
            if (soulCount != 18) throw new IllegalStateException("Creative tab must expose all 18 elemental souls, found " + soulCount);
            for (var entry : ModItems.SOULS.entrySet()) {
                var item = entry.getValue().get();
                var id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
                if (id == null || minecraft.getModelManager().getModel(new ModelResourceLocation(id, "inventory")) == minecraft.getModelManager().getMissingModel())
                    throw new IllegalStateException("Missing elemental soul inventory model: " + entry.getKey());
                try (var texture = PokemonTreatClientSmokeTest.class.getResourceAsStream("/assets/minemon_gen1/textures/item/" + entry.getKey() + "_soul.png")) {
                    var image = texture == null ? null : javax.imageio.ImageIO.read(texture);
                    if (image == null || image.getWidth() != 256 || image.getHeight() != 256 || !image.getColorModel().hasAlpha())
                        throw new IllegalStateException("Missing transparent soul texture: " + entry.getKey());
                }
            }
            com.mojang.logging.LogUtils.getLogger().info("[SOUL CLIENT] PASS: all 18 elemental souls registered in creative tab with baked inventory models and transparent textures");
            long treatCount=tab.stream().filter(stack->stack.getItem() instanceof br.com.petrickfaria.minemon.item.PokemonTreatItem).count();
            if (treatCount != 18) throw new IllegalStateException("Creative tab exposes "+treatCount+" treats instead of 18");
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
            try (var texture=PokemonTreatClientSmokeTest.class.getResourceAsStream("/assets/minemon/textures/item/grass_treat.png")) {
                var image=texture==null?null:javax.imageio.ImageIO.read(texture);
                if (image==null || image.getWidth()!=16 || image.getHeight()!=16 || !image.getColorModel().hasAlpha())
                    throw new IllegalStateException("Grass Treat needs a transparent 16x16 pixel texture");
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
            var ball=ModItems.POKEBALL.get();
            var ballId=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(ball);
            if(ballId==null||!ballId.toString().equals("minemon:pokeball"))throw new IllegalStateException("Pokébola has wrong registry ID: "+ballId);
            if(ball.getMaxStackSize()!=64)throw new IllegalStateException("Empty Pokébolas must stack to 64");
            var occupiedBall=new net.minecraft.world.item.ItemStack(ball);
            var occupiedTag=new net.minecraft.nbt.CompoundTag();occupiedTag.put("MinemonStoredPokemon",new net.minecraft.nbt.CompoundTag());occupiedBall.setTag(occupiedTag);
            if(ball.getMaxStackSize(occupiedBall)!=1)throw new IllegalStateException("Occupied Pokébolas must be unstackable");
            if(tab.stream().filter(stack->stack.getItem()==ball).count()!=1)throw new IllegalStateException("Pokébola must appear exactly once in the Minemon creative tab");
            var ballModelId=new ModelResourceLocation(ballId,"inventory");
            if(minecraft.getModelManager().getModel(ballModelId)==minecraft.getModelManager().getMissingModel())throw new IllegalStateException("Missing baked Pokébola model: "+ballModelId);
            var ballName="item.minemon.pokeball";
            if(net.minecraft.client.resources.language.I18n.get(ballName).equals(ballName))throw new IllegalStateException("Missing Pokébola translation: "+ballName);
            try(var texture=PokemonTreatClientSmokeTest.class.getResourceAsStream("/assets/minemon/textures/item/pokeball.png")){
                var image=texture==null?null:javax.imageio.ImageIO.read(texture);
                if(image==null||image.getWidth()!=16||image.getHeight()!=16||((image.getRGB(0,0)>>>24)&255)!=0)throw new IllegalStateException("Pokébola texture must preserve the supplied 16x16 sprite with transparent corner pixels (image="+(image==null?"missing":image.getWidth()+"x"+image.getHeight()+", alpha="+image.getColorModel().hasAlpha()+", corner="+Integer.toHexString(image.getRGB(0,0)))+")");
                int upper=image.getRGB(8,4),lower=image.getRGB(7,11),center=image.getRGB(5,10);
                if(((upper>>16)&255)<150||((lower>>16)&255)<150||((lower>>8)&255)<150||((center>>16)&255)<180||((center>>8)&255)<180||((center)&255)<180)throw new IllegalStateException("Pokébola texture is missing its red/white halves or white center button");
            }
            var masterBall=ModItems.MASTER_BALL.get();
            var masterBallId=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(masterBall);
            if(masterBallId==null||!masterBallId.toString().equals("minemon:master_ball"))throw new IllegalStateException("Master Ball has wrong registry ID: "+masterBallId);
            if(tab.stream().filter(stack->stack.getItem()==masterBall).count()!=1)throw new IllegalStateException("Master Ball must appear exactly once in the Minemon creative tab");
            var masterBallModelId=new ModelResourceLocation(masterBallId,"inventory");
            if(minecraft.getModelManager().getModel(masterBallModelId)==minecraft.getModelManager().getMissingModel())throw new IllegalStateException("Missing baked Master Ball model: "+masterBallModelId);
            var masterBallName="item.minemon.master_ball";
            if(net.minecraft.client.resources.language.I18n.get(masterBallName).equals(masterBallName))throw new IllegalStateException("Missing Master Ball translation: "+masterBallName);
            try(var texture=PokemonTreatClientSmokeTest.class.getResourceAsStream("/assets/minemon/textures/item/master_ball.png")){
                var image=texture==null?null:javax.imageio.ImageIO.read(texture);
                if(image==null||image.getWidth()!=16||image.getHeight()!=16||!image.getColorModel().hasAlpha())throw new IllegalStateException("Master Ball needs its supplied transparent 16x16 sprite");
            }
            if (minecraft.player == null || minecraft.level == null) {
                var dataFile = java.nio.file.Path.of("saves/minemon-validation/level.dat").toFile();
                var tag = net.minecraft.nbt.NbtIo.readCompressed(dataFile);
                tag.getCompound("Data").putBoolean("confirmedExperimentalSettings", true);
                net.minecraft.nbt.NbtIo.writeCompressed(tag, dataFile);
                Object flows = Minecraft.class.getMethod("createWorldOpenFlows").invoke(minecraft);
                boolean invoked = false;
                for (var method : flows.getClass().getMethods()) if (method.getName().equals("loadLevel")) {
                    if (method.getParameterCount() == 2 && method.getParameterTypes()[1] == String.class) {
                        method.invoke(flows, minecraft.screen, "minemon-validation"); invoked = true; break;
                    }
                    if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == String.class) {
                        method.invoke(flows, "minemon-validation"); invoked = true; break;
                    }
                }
                if (!invoked) throw new IllegalStateException("Could not locate WorldOpenFlows.loadLevel for PokéC UI validation");
                worldOpening = true;
                worldWaitTicks = 0;
                return;
            }
            openPokecUi(minecraft);
        } catch (Exception failure) {
            throw new IllegalStateException("Treat client validation failed",failure);
        }
    }
    private static void openPokecUi(Minecraft minecraft) {
        pokecUiPhase = true;
        clientTicks = 0;
        var menu = new PokecMenu(minecraft.player.containerMenu.containerId + 1,
                minecraft.player.getInventory(), minecraft.player.blockPosition());
        minecraft.setScreen(new PokecScreen(menu, minecraft.player.getInventory(), Component.literal("PokéC")));
        // This standalone UI smoke uses the same starter unlock snapshot the server
        // sends for a fresh profile, so locked Mew and unlocked Bulbasaur are deterministic.
        PokecScreen.updateState(0, true, java.util.Set.of("bulbasaur", "charmander", "squirtle", "pikachu", "eevee"),
                java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), "bulbasaur", 0, 0,
                net.minecraft.world.item.ItemStack.EMPTY, net.minecraft.world.item.ItemStack.EMPTY, -1,
                "", "", "", "", 0, 0, 0, 0, false);
    }
    private PokemonTreatClientSmokeTest() {}
}
