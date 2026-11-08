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
    private static final java.util.List<String> PREVIEW_SPECIES=java.util.List.of(
            "caterpie","charmander","charizard","onix","moltres","squirtle","gastly");
    private static final java.util.Set<String> PREVIEW_SCREENSHOTS=java.util.Set.of("caterpie","charmander","charizard","onix","moltres");
    private static final int PREVIEW_SELECTIONS=21;
    private static int clientTicks;
    private static boolean done;
    private static boolean worldOpening;
    private static boolean pokecUiPhase;
    private static boolean pokecSelectionPhase;
    private static boolean pokecUnlockedPreviewPhase;
    private static boolean pokeballPhase;
    private static boolean pokecPreviewSequence;
    private static int previewSwitchIndex;
    private static int previewStableFrames;
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
                        creature.moveTo(owner.getX() + 2.0D, owner.getY(), owner.getZ(), owner.getYRot(), 0.0F);
                        creature.setOrderedToSit(true);
                        creature.setNoAi(true);
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
                                    + "PASS POKÉC PREVIEW: 21 repeated species selections used species idle clips; standardized Caterpie/Charmander/Charizard/Onix/Moltres bounds; consistent 23° three-quarter yaw and 10° downward tilt.\n"
                                    + "PASS POKÉC SCROLLBAR: conventional wheel, track click, drag outside the track in both directions, boundary clamping, release, and selection preservation.\n"
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
            if(pokecPreviewSequence){
                if(!(minecraft.screen instanceof PokecScreen screen))throw new IllegalStateException("PokéC closed during repeated preview selection check");
                String slug=PREVIEW_SPECIES.get(previewSwitchIndex%PREVIEW_SPECIES.size());
                if(!screen.selectedSpeciesForSmokeTest(slug)){
                    if(!screen.selectPreviewSpeciesForSmokeTest(slug))throw new IllegalStateException("Could not switch preview to "+slug);
                    previewStableFrames=0;clientTicks=0;return;
                }
                if(!screen.previewExistsForSmokeTest(slug)){
                    if(++clientTicks>80)throw new IllegalStateException("Preview entity was not created for "+slug);
                    return;
                }
                String expected=br.com.petrickfaria.minemon.entity.SpeciesDefinitions.get(slug).idle();
                String selected=screen.previewAnimationForSmokeTest(slug);
                if(!expected.equals(selected)){
                    if(++clientTicks>80)throw new IllegalStateException("PokéC preview did not initialize the species idle animation for "+slug+": expected="+expected+" observed="+selected);
                    return;
                }
                if(++previewStableFrames<3)return;
                if(PREVIEW_SCREENSHOTS.contains(slug)&&previewSwitchIndex<PREVIEW_SPECIES.size())
                    net.minecraft.client.Screenshot.grab(minecraft.gameDirectory,"pokec-preview-"+slug+".png",minecraft.getMainRenderTarget(),message->{});
                previewSwitchIndex++;previewStableFrames=0;clientTicks=0;
                if(previewSwitchIndex>=PREVIEW_SELECTIONS){
                    pokecPreviewSequence=false;pokeballPhase=true;pokeballFixtureEntityId=-1;clientTicks=0;
                }
                return;
            }
            if (pokecUnlockedPreviewPhase) {
                if (!(minecraft.screen instanceof PokecScreen screen) || !screen.selectedSpeciesForSmokeTest("bulbasaur")
                        || !screen.previewExistsForSmokeTest("bulbasaur"))
                    throw new IllegalStateException("Unlocked Bulbasaur preview was not instantiated after selection");
                net.minecraft.client.Screenshot.grab(minecraft.gameDirectory,"pokec-unlocked.png",minecraft.getMainRenderTarget(),message->{});
                if(!"animation.bulbasaur.ground_idle".equals(screen.previewAnimationForSmokeTest("bulbasaur")))
                    throw new IllegalStateException("Bulbasaur preview did not select its own idle clip");
                float[] orientation=screen.previewOrientationForSmokeTest();
                if(Math.abs(orientation[0]-23)>0.01||Math.abs(orientation[1]+4)>0.01)
                    throw new IllegalStateException("PokéC preview initial three-quarter orientation was not reset consistently");
                for(String slug:java.util.List.of("caterpie","charmander","charizard","onix","moltres"))
                    if(Math.abs(screen.previewNormalizedSizeForSmokeTest(slug)-76.0)>0.001)
                        throw new IllegalStateException("PokéC preview size was not standardized for "+slug);
                if(!screen.dragPreviewForSmokeTest())throw new IllegalStateException("Pokec drag did not rotate both axes");
                int initialSelection=screen.selectedSpeciesForSmokeTest("bulbasaur")?1:0;
                if(!screen.dragListScrollbarForSmokeTest(true,true)||screen.listScrollOffsetForSmokeTest()!=0)
                    throw new IllegalStateException("PokéC scrollbar could not be dragged outside the track back to its start");
                if(!screen.scrollListForSmokeTest(-1)||screen.listScrollOffsetForSmokeTest()<=0
                        ||!screen.scrollListForSmokeTest(1)||screen.listScrollOffsetForSmokeTest()!=0)
                    throw new IllegalStateException("PokéC conventional mouse wheel failed to move and return the species list");
                if(!screen.dragListScrollbarForSmokeTest(true,false)||screen.listScrollOffsetForSmokeTest()!=screen.listScrollMaxForSmokeTest())
                    throw new IllegalStateException("PokéC track click and drag did not reach the list end");
                if(!screen.scrollListForSmokeTest(1)||screen.listScrollOffsetForSmokeTest()>=screen.listScrollMaxForSmokeTest()
                        ||!screen.scrollListForSmokeTest(-1)||screen.listScrollOffsetForSmokeTest()!=screen.listScrollMaxForSmokeTest())
                    throw new IllegalStateException("PokéC conventional mouse wheel failed near the list boundaries");
                if(!screen.selectedSpeciesForSmokeTest("bulbasaur")||initialSelection!=1)
                    throw new IllegalStateException("PokéC scrollbar interaction changed the selected list element");
                pokecPreviewSequence=true;previewSwitchIndex=0;previewStableFrames=0;
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
                net.minecraft.client.Screenshot.grab(minecraft.gameDirectory,"pokec-locked.png",minecraft.getMainRenderTarget(),message->{});
                if (!screen.clickSpeciesForSmokeTest("bulbasaur"))
                    throw new IllegalStateException("PokéC list could not return to Bulbasaur through its click handler");
                pokecUnlockedPreviewPhase = true;
                clientTicks = 0;
                return;
            }
            if (pokecUiPhase) {
                if (!(minecraft.screen instanceof PokecScreen screen)) {
                    if(clientTicks<200)return;
                    throw new IllegalStateException("PokéC server menu did not open during focused UI render check");
                }
                if (screen.catalogSizeForSmokeTest() != 151)
                    throw new IllegalStateException("PokéC UI did not load the complete 151-species list");
                if (!screen.conversionPanelFitsAboveInventoryForSmokeTest())
                    throw new IllegalStateException("PokéC conversion panel must remain above the player inventory");
                if (!screen.inventorySlotsForSmokeTest())
                    throw new IllegalStateException("PokéC must show all 27 main inventory slots and 9 hotbar slots in vanilla order");
                if (screen.previewScaleForSmokeTest("pikachu") <= screen.previewScaleForSmokeTest("onix"))
                    throw new IllegalStateException("PokéC preview should scale Pikachu larger than giant Onix");
                if (!screen.clickSpeciesForSmokeTest("mew"))
                    throw new IllegalStateException("PokéC list could not select Mew through its click handler");
                pokecSelectionPhase = true;
                clientTicks = 0;
                return;
            }
            try(var vanilla=minecraft.getResourceManager().getResource(new net.minecraft.resources.ResourceLocation("minecraft","textures/item/bone.png")).orElseThrow().open()) {
                var reference=javax.imageio.ImageIO.read(java.nio.file.Path.of("../tools/reference/minecraft-1.19.2-bone.png").toFile());
                var original=javax.imageio.ImageIO.read(vanilla);
                for(int y=0;y<16;y++)for(int x=0;x<16;x++)if(reference.getRGB(x,y)!=original.getRGB(x,y))throw new IllegalStateException("Treat reference is not the exact vanilla 1.19.2 bone");
            }
            NonNullList<net.minecraft.world.item.ItemStack> tab=NonNullList.create();
            ModItems.MINEMON_TAB.fillItemList(tab);
            var eggStack=ModItems.BREEDING_EGG.get().getDefaultInstance();
            var eggModel=minecraft.getItemRenderer().getModel(eggStack,null,null,0);
            if(eggModel==minecraft.getModelManager().getMissingModel()
                    ||tab.stream().noneMatch(s->s.is(eggStack.getItem())))
                throw new IllegalStateException("Breeding egg inventory model or creative entry missing");
            var eggQuads=new java.util.ArrayList<net.minecraft.client.renderer.block.model.BakedQuad>();
            eggQuads.addAll(eggModel.getQuads(null,null,net.minecraft.util.RandomSource.create(0)));
            for(var side:net.minecraft.core.Direction.values())
                eggQuads.addAll(eggModel.getQuads(null,side,net.minecraft.util.RandomSource.create(0)));
            if(eggQuads.stream().noneMatch(q->q.getTintIndex()==0)
                    ||eggQuads.stream().noneMatch(q->!q.isTinted()))
                throw new IllegalStateException("Egg must separate elemental marks from untinted white shell");
            for(String type:java.util.List.of("fire","flying")){
                eggStack.getOrCreateTagElement("BlockEntityTag").putString("EggElement",type);
                if(minecraft.getItemColors().getColor(eggStack,0)!=br.com.petrickfaria.minemon.entity.EggAppearance.color(type)
                        ||minecraft.getItemColors().getColor(eggStack,1)!=0xFFFFFF)
                    throw new IllegalStateException("Egg saved elemental tint did not reach the client: "+type);
            }
            System.out.println("[EGG CLIENT] PASS: baked egg, white shell, elemental marks and saved item tint");
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
                if (image==null || image.getWidth()!=64 || image.getHeight()!=64 || !image.getColorModel().hasAlpha())
                    throw new IllegalStateException("PokéCoin texture must preserve the supplied 64x64 transparent PNG");
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
        var server=minecraft.getSingleplayerServer();
        if(server==null)throw new IllegalStateException("Pokec UI smoke requires the prepared integrated server");
        var uuid=minecraft.player.getUUID();
        server.execute(()->{
            var player=server.getPlayerList().getPlayer(uuid);
            if(player==null)throw new IllegalStateException("Pokec test player not loaded on server");
            player.getPersistentData().remove("minemon_gen1_pokec");
            var pos=player.blockPosition().west(2);
            var level=(net.minecraft.server.level.ServerLevel)player.level;
            for(String slug:PREVIEW_SPECIES)br.com.petrickfaria.minemon.pokec.PokecPlayerData.unlock(player,slug);
            var state=br.com.petrickfaria.minemon.registry.ModBlocks.POKEC.get().defaultBlockState();
            level.setBlock(pos,state,3);
            level.setBlock(pos.above(),state.setValue(br.com.petrickfaria.minemon.block.PokecBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),3);
            state.use(level,player,net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false));
        });
    }
    private PokemonTreatClientSmokeTest() {}
}
