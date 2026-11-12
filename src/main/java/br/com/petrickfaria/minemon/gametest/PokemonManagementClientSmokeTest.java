package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.menu.BlackListScreen;
import br.com.petrickfaria.minemon.menu.PokemonInfoMenu;
import br.com.petrickfaria.minemon.menu.PokemonInfoScreen;
import br.com.petrickfaria.minemon.block.PokecBlock;
import br.com.petrickfaria.minemon.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import br.com.petrickfaria.minemon.menu.PokecScreen;
import br.com.petrickfaria.minemon.entity.MobTargetCategories;
import br.com.petrickfaria.minemon.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Focused real-client check for the Pokémon status and registry-backed Black List screens. */
public final class PokemonManagementClientSmokeTest {
    private static int ticks;
    private static boolean openingWorld;
    private static boolean done;
    private static volatile int entityId = -1;
    private static int phase;
    private static boolean blacklistPreparationRequested;
    private static volatile int dewgongId=-1,gastlyId=-1;
    private static volatile boolean closedEyesSeen,openEyesSeen,awakeBlinkSeen;
    private static volatile int dewgongRenderObservations;
    private static volatile String lastDewgongEyeObservation="not rendered";
    private static int auraState,auraLarge,auraSmall,auraFrames;
    private static void nineFixtures(Minecraft minecraft){
        minecraft.player.closeContainer();minecraft.setScreen(null);minecraft.player.setYRot(0);minecraft.player.setXRot(10);
        br.com.petrickfaria.minemon.client.MinemonRenderer.validationObserver=(entity,model)->{
            if(!entity.getSpeciesSlug().equals("dewgong"))return;
            dewgongRenderObservations++;
            var left=model.getAnimationProcessor().getBone("eyelid_left");var right=model.getAnimationProcessor().getBone("eyelid_right");
            var leftEye=model.getAnimationProcessor().getBone("eye_left");var rightEye=model.getAnimationProcessor().getBone("eye_right");
            if(left==null||right==null||leftEye==null||rightEye==null)throw new IllegalStateException("Dewgong eye bones missing");
            // The eyelid cubes are children of the eye bones in this rig, so the
            // parents must stay rendered; their closed-lid meshes cover the open-eye planes.
            lastDewgongEyeObservation="sleep="+entity.isSleeping()+", eyeHidden="+leftEye.isHidden()+"/"+rightEye.isHidden()
                    +", lidHidden="+left.isHidden()+"/"+right.isHidden()+", lidZ="+left.getPositionZ()+"/"+right.getPositionZ();
            boolean closed=!leftEye.isHidden()&&!rightEye.isHidden()&&!left.isHidden()&&!right.isHidden()
                    &&Math.abs(left.getPositionZ())<.001&&Math.abs(right.getPositionZ())<.001;
            boolean open=!leftEye.isHidden()&&!rightEye.isHidden()&&!left.isHidden()&&!right.isHidden()
                    &&Math.abs(left.getPositionZ()+.1)<.001&&Math.abs(right.getPositionZ()+.1)<.001;
            if(entity.isSleeping()&&closed)closedEyesSeen=true;
            if(!entity.isSleeping()&&open)openEyesSeen=true;
            boolean blinkClosed=!leftEye.isHidden()&&!rightEye.isHidden()&&!left.isHidden()&&!right.isHidden()
                    &&Math.abs(left.getPositionZ())<.001&&Math.abs(right.getPositionZ())<.001;
            if(!entity.isSleeping()&&openEyesSeen&&blinkClosed)awakeBlinkSeen=true;
        };
        var ownerId=minecraft.player.getUUID();
        minecraft.getSingleplayerServer().execute(()->{
            var owner=minecraft.getSingleplayerServer().getPlayerList().getPlayer(ownerId);var level=(net.minecraft.server.level.ServerLevel)owner.level;
            // The fixture is meant to validate the rendered sleep pose. At
            // daytime tickNightRest correctly wakes natural sleepers, so use
            // an actual nighttime world clock before adding Dewgong.
            level.setDayTime(13000);
            var dewgong=ModEntities.SPECIES.get("dewgong").get().create(level);dewgong.tame(owner);
            // Registration restores the managed mode, whose default is Follow;
            // use Sentar so that it does not correctly wake this sleep-pose fixture.
            dewgong.setOrderedToSit(true);dewgong.setNoAi(true);dewgong.setNoGravity(true);
            dewgong.moveTo(owner.getX(),owner.getY(),owner.getZ()+3,180,0);dewgong.setSleeping(true);level.addFreshEntity(dewgong);dewgongId=dewgong.getId();
            // Count five large and seven small aura draws per visible Gastly.
            // Remove naturally spawned Gastlys from this small visual arena so
            // another entity cannot double the fixture's particle totals.
            level.getEntitiesOfClass(br.com.petrickfaria.minemon.entity.MinemonCreature.class,
                    owner.getBoundingBox().inflate(48.0D),candidate->candidate.getSpeciesSlug().equals("gastly"))
                    .forEach(net.minecraft.world.entity.Entity::discard);
            var gastly=ModEntities.SPECIES.get("gastly").get().create(level);gastly.tame(owner);gastly.setNoAi(true);gastly.setNoGravity(true);
            gastly.moveTo(owner.getX()+1.5,owner.getY()+1,owner.getZ()+3.5,180,0);level.addFreshEntity(gastly);gastlyId=gastly.getId();
        });
    }
    private static void changeAuraState(Minecraft minecraft,int state){
        minecraft.getSingleplayerServer().execute(()->{
            var level=minecraft.getSingleplayerServer().overworld();var entity=level.getEntity(gastlyId);
            if(entity instanceof br.com.petrickfaria.minemon.entity.MinemonCreature gas){
                if(state==1)gas.setSleeping(true);
                else if(state==2)gas.setOrderedToSit(true);
                else if(state==3)gas.die(net.minecraft.world.damagesource.DamageSource.GENERIC);
            }
        });
    }

    private static void validateBlacklistFilters(PokemonInfoScreen parent,BlackListScreen screen,Minecraft minecraft)throws Exception{
        ResourceLocation zombie=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(net.minecraft.world.entity.EntityType.ZOMBIE);
        ResourceLocation cow=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(net.minecraft.world.entity.EntityType.COW);
        var creeper=new ResourceLocation("minecraft","creeper");
        if(screen.rowCountForSmokeTest()==0||!screen.containsTypeForSmokeTest(zombie)||!screen.containsTypeForSmokeTest(cow)
                ||!screen.selectedForSmokeTest(zombie)||screen.selectedForSmokeTest(cow)||screen.selectedForSmokeTest(creeper))
            throw new IllegalStateException("Blacklist did not load registered hostile/passive defaults");

        // Click the Sentar button underneath the overlay. The click may reach a
        // Blacklist row, but must never change the Pokémon's sit mode.
        if(parent.pokemonForSmokeTest()==null||!parent.pokemonForSmokeTest().isOrderedToSit())
            throw new IllegalStateException("Fixture is not seated before modal-input check");
        boolean consumed=parent.mouseClicked(parent.getGuiLeft()+120,parent.getGuiTop()+184,0);
        if(!consumed||!parent.pokemonForSmokeTest().isOrderedToSit()||!parent.hasBlackListOverlay())
            throw new IllegalStateException("Blacklist click leaked into the status screen behind it");

        Set<ResourceLocation> all=new HashSet<>();
        for(MobTargetCategories.Origin source:MobTargetCategories.Origin.values()){
            if(!screen.selectOriginForSmokeTest(source)||screen.originForSmokeTest()!=source)
                throw new IllegalStateException("Source filter did not update immediately: "+source);
            Set<ResourceLocation> bySource=new HashSet<>();
            for(MobTargetCategories.Category behavior:MobTargetCategories.Category.values()){
                if(!screen.selectCategoryForSmokeTest(behavior)||screen.categoryForSmokeTest()!=behavior)
                    throw new IllegalStateException("Behavior filter did not update immediately: "+behavior);
                if(screen.firstRowForSmokeTest()!=0)throw new IllegalStateException("Changing a filter did not reset the scroll position");
                for(ResourceLocation id:screen.visibleIdsForSmokeTest()){
                    var type=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(id);
                    if(type==null||MobTargetCategories.origin(id)!=source||MobTargetCategories.category(id,type)!=behavior)
                        throw new IllegalStateException("Combined source/behavior filter admitted an invalid mob: "+id);
                    if(!all.add(id)||!bySource.add(id))throw new IllegalStateException("Mob appeared in multiple filter partitions: "+id);
                }
            }
            long expected=screen.allIdsForSmokeTest().stream().filter(id->MobTargetCategories.origin(id)==source).count();
            if(bySource.size()!=expected)
                throw new IllegalStateException("Source filter did not retain its complete behavior partition: "+source);
        }
        if(all.size()!=screen.rowCountForSmokeTest()||!all.contains(cow)||!all.contains(zombie)||!all.contains(creeper))
            throw new IllegalStateException("Combined Blacklist filters did not partition every registered mob");

        // Preserve per-mob choices while changing filters and sort modes.
        boolean zombieSelected=screen.selectedForSmokeTest(zombie),cowSelected=screen.selectedForSmokeTest(cow);
        screen.selectOriginForSmokeTest(MobTargetCategories.Origin.MINEMON);
        screen.selectCategoryForSmokeTest(MobTargetCategories.Category.PASSIVE);
        screen.selectOrderingForSmokeTest(BlackListScreen.Ordering.ALPHABETICAL);
        List<String> names=screen.visibleNamesForSmokeTest();
        List<String> sortedNames=new ArrayList<>(names);
        sortedNames.sort(String.CASE_INSENSITIVE_ORDER);
        if(screen.orderingForSmokeTest()!=BlackListScreen.Ordering.ALPHABETICAL||!names.equals(sortedNames))
            throw new IllegalStateException("A ordering is not alphabetical by displayed translated name");
        screen.selectOrderingForSmokeTest(BlackListScreen.Ordering.NUMERIC);
        List<ResourceLocation> numeric=screen.visibleIdsForSmokeTest();
        int previous=0;
        for(ResourceLocation id:numeric){
            int number=MobTargetCategories.nationalDexNumber(id);
            if(number<0||number<=previous)throw new IllegalStateException("N ordering is not strictly Pokédex numeric: "+id+" #"+number);
            previous=number;
        }
        if(numeric.size()!=151||!numeric.get(0).getPath().equals("bulbasaur")||!numeric.get(numeric.size()-1).getPath().equals("mew"))
            throw new IllegalStateException("Minemon N ordering did not retain the complete 001–151 sequence");
        if(screen.selectedForSmokeTest(zombie)!=zombieSelected||screen.selectedForSmokeTest(cow)!=cowSelected)
            throw new IllegalStateException("Changing filters or ordering altered mob permissions");
        // N without a published number uses deterministic registry-ID order.
        var stable=MobTargetCategories.numericIdOrder();
        if(stable.compare(new ResourceLocation("alpha","mob"),new ResourceLocation("zeta","mob"))>=0)
            throw new IllegalStateException("Numeric ordering fallback is not stable by registry ID");
    }


    public static void tick(TickEvent.ClientTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END
                || !Boolean.getBoolean("minemon.clientSmokeTest")
                || !Boolean.getBoolean("minemon.clientSmokePokemonManagement")) return;
        Minecraft minecraft = Minecraft.getInstance();
        try {
            if (openingWorld) {
                if (minecraft.player != null && minecraft.level != null) {
                    openingWorld = false;
                    phase = 1;
                } else if (++ticks > 600) throw new IllegalStateException("Timed out opening the prepared test world");
                return;
            }
            if (phase == 0) {
                if (++ticks < 20 || minecraft.player != null) return;
                var dataFile = Path.of("saves/minemon-validation/level.dat").toFile();
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
                if (!invoked) throw new IllegalStateException("Could not open prepared test world");
                openingWorld = true;
                ticks = 0;
                return;
            }
            if (phase == 1) {
                if (minecraft.getSingleplayerServer() == null || minecraft.player == null || minecraft.level == null) return;
                UUID ownerId = minecraft.player.getUUID();
                phase = 2;
                minecraft.getSingleplayerServer().execute(() -> {
                    var owner = minecraft.getSingleplayerServer().getPlayerList().getPlayer(ownerId);
                    if (owner == null || !(owner.level instanceof net.minecraft.server.level.ServerLevel level)) return;
                    // This real-client UI smoke world also runs ordinary mob
                    // spawning. Keep the player alive so an incidental slime
                    // cannot close the container while the PokéC screen is tested.
                    owner.setGameMode(GameType.CREATIVE);
                    var type = ModEntities.SPECIES.get("pikachu");
                    var pokemon = type == null ? null : type.get().create(level);
                    if (pokemon == null) return;
                    pokemon.setNoAi(true);
                    pokemon.setNoGravity(true);
                    pokemon.tame(owner);
                    pokemon.moveTo(owner.getX() + 2.0D, owner.getY(), owner.getZ(), owner.getYRot(), 0.0F);
                    if (level.addFreshEntity(pokemon)) entityId = pokemon.getId();
                });
                return;
            }
            if (phase == 2) {
                if (entityId < 0) return;
                var target = minecraft.level.getEntity(entityId);
                if (!(target instanceof br.com.petrickfaria.minemon.entity.MinemonCreature)) {
                    if (++ticks > 300) throw new IllegalStateException("Owned Pikachu was not tracked by the client");
                    return;
                }
                minecraft.setScreen(null);
                minecraft.gameMode.interact(minecraft.player, target, net.minecraft.world.InteractionHand.MAIN_HAND);
                phase = 3;
                ticks = 0;
                return;
            }
            if (phase == 3) {
                if (!(minecraft.screen instanceof PokemonInfoScreen screen)) {
                    if (++ticks > 180) throw new IllegalStateException("Right-clicking the owned Pokémon did not open PokemonInfoScreen");
                    return;
                }
                if (!(minecraft.player.containerMenu instanceof PokemonInfoMenu)
                        || !screen.headerForSmokeTest().equals("025 - Pikachu")
                        || screen.evolveButtonAvailableForSmokeTest())
                    throw new IllegalStateException("Pokémon status screen header, ownership menu, or level gate is incorrect");
                minecraft.getSingleplayerServer().execute(() -> {
                    var entity = minecraft.getSingleplayerServer().overworld().getEntity(entityId);
                    if (entity instanceof br.com.petrickfaria.minemon.entity.MinemonCreature pokemon)
                        pokemon.awardPokemonExperience(pokemon.getPokemonXpToNextLevel());
                });
                phase = 4;
                ticks = 0;
                return;
            }
            if (phase == 4) {
                if (!(minecraft.screen instanceof PokemonInfoScreen screen)
                        || screen.pokemonForSmokeTest() == null || screen.pokemonForSmokeTest().getPokemonLevel() != 2) {
                    if (++ticks > 120) throw new IllegalStateException("Pokémon status XP/level state did not sync to the client");
                    return;
                }
                minecraft.gameMode.handleInventoryButtonClick(minecraft.player.containerMenu.containerId, 1);
                phase = 5;
                ticks = 0;
                return;
            }
            if (phase == 5) {
                if (!(minecraft.screen instanceof PokemonInfoScreen screen)
                        || screen.pokemonForSmokeTest() == null || !screen.pokemonForSmokeTest().isOrderedToSit()) {
                    if (++ticks > 120) throw new IllegalStateException("Sentar command did not set the sit state");
                    return;
                }
                minecraft.gameMode.handleInventoryButtonClick(minecraft.player.containerMenu.containerId, 0);
                phase = 6;
                ticks = 0;
                return;
            }
            if (phase == 6) {
                if (!(minecraft.screen instanceof PokemonInfoScreen screen)
                        || screen.pokemonForSmokeTest() == null || screen.pokemonForSmokeTest().isOrderedToSit()) {
                    if (++ticks > 120) {
                        String screenName = minecraft.screen == null ? "null" : minecraft.screen.getClass().getSimpleName();
                        var clientPokemon = minecraft.level == null ? null : minecraft.level.getEntity(entityId);
                        String state = clientPokemon instanceof br.com.petrickfaria.minemon.entity.MinemonCreature current
                                ? " sit=" + current.isOrderedToSit() + " alive=" + current.isAlive()
                                  + " pokemonPos=" + current.position() + " playerPos=" + minecraft.player.position()
                                  + " distanceSqr=" + current.distanceToSqr(minecraft.player)
                                : " pokemon=untracked";
                        throw new IllegalStateException("Seguir command did not clear the sit state; screen="
                                + screenName + state);
                    }
                    return;
                }
                minecraft.gameMode.handleInventoryButtonClick(minecraft.player.containerMenu.containerId, 2);
                phase = 7;
                ticks = 0;
                return;
            }
            if (phase == 7) {
                if (!(minecraft.screen instanceof PokemonInfoScreen screen) || screen.pokemonForSmokeTest() == null) {
                    if (++ticks > 120) throw new IllegalStateException("Pokémon status screen closed before Black List validation");
                    return;
                }
                var pokemon = screen.pokemonForSmokeTest();
                if (!blacklistPreparationRequested) {
                    if (!pokemon.isFreeMode()) {
                        if (++ticks > 120) throw new IllegalStateException("Livre button did not activate free mode");
                        return;
                    }
                    minecraft.gameMode.handleInventoryButtonClick(minecraft.player.containerMenu.containerId, 1);
                    blacklistPreparationRequested = true;
                    ticks = 0;
                    return;
                }
                if (!pokemon.isOrderedToSit()) {
                    if (++ticks > 120) throw new IllegalStateException("Pokémon did not sit before Black List validation");
                    return;
                }
                screen.mouseClicked(screen.getGuiLeft() + 128, screen.getGuiTop() + 207, 0);
                phase = 11;
                ticks = 0;
                return;
            }
            if (phase == 11) {
                if (!(minecraft.screen instanceof PokemonInfoScreen parent) || !parent.hasBlackListOverlay()) {
                    if (++ticks > 120) throw new IllegalStateException("Black List overlay did not remain open inside the status screen; screen="
                            + (minecraft.screen == null ? "null" : minecraft.screen.getClass().getSimpleName()));
                    return;
                }
                BlackListScreen screen = parent.blackListOverlayForSmokeTest();
                if (screen == null) throw new IllegalStateException("Black List overlay reference was missing");
                if (!(minecraft.player.containerMenu instanceof PokemonInfoMenu))
                    throw new IllegalStateException("Black List overlay closed the Pokémon status menu on the client: menu="
                            + minecraft.player.containerMenu.getClass().getSimpleName() + " screen="
                            + minecraft.screen.getClass().getSimpleName());
                validateBlacklistFilters(parent,screen,minecraft);
                screen.selectOriginForSmokeTest(MobTargetCategories.Origin.VANILLA);
                screen.selectCategoryForSmokeTest(MobTargetCategories.Category.HOSTILE);
                if(screen.maxFirstRowForSmokeTest()<1)throw new IllegalStateException("Blacklist fixture did not contain a large enough list");
                if(!screen.dragScrollbarForSmokeTest(true,true)||screen.firstRowForSmokeTest()!=0
                        ||!screen.dragScrollbarForSmokeTest(true,false)||screen.firstRowForSmokeTest()!=screen.maxFirstRowForSmokeTest())
                    throw new IllegalStateException("Blacklist scrollbar click-and-drag did not clamp and release correctly");
                if(!parent.mouseScrolled(parent.getGuiLeft()+120,parent.getGuiTop()+184,-1)||!parent.hasBlackListOverlay())
                    throw new IllegalStateException("Blacklist did not capture wheel input over the screen behind it");
                if(!parent.keyPressed(256,0,0)||parent.hasBlackListOverlay())
                    throw new IllegalStateException("Escape did not close the Blacklist overlay");
                if(!parent.mouseClicked(parent.getGuiLeft()+45,parent.getGuiTop()+184,0))
                    throw new IllegalStateException("The status screen did not regain button input after closing the Blacklist");
                Files.writeString(Path.of("client-validation-pokemon-management.txt"),
                        "PASS: owned Pokémon status screen, Pokédex header, XP level sync, Seguir/Sentar controls; combined behavior/source filters, N/A ordering, settings preservation, modal click/wheel capture, close and input restoration.\n"
                                + "PASS BLACKLIST SCROLLBAR: large registry list, clamped limits, release, and preserved mob permissions.\n");
                nineFixtures(minecraft);phase=8;ticks=0;return;
            }
            if(phase==8){
                if(!closedEyesSeen){if(++ticks>240)throw new IllegalStateException("Rendered sleeping Dewgong did not close both eyes; renders="+dewgongRenderObservations+"; "+lastDewgongEyeObservation);return;}
                minecraft.getSingleplayerServer().execute(()->{var e=minecraft.getSingleplayerServer().overworld().getEntity(dewgongId);if(e instanceof br.com.petrickfaria.minemon.entity.MinemonCreature p)p.setSleeping(false);});
                phase=9;ticks=0;return;
            }
            if(phase==9){
                if(!openEyesSeen||!awakeBlinkSeen){if(++ticks>320)throw new IllegalStateException("Rendered Dewgong did not reopen eyes and blink awake");return;}
                phase=10;ticks=0;auraState=0;return;
            }
            if(phase==10){
                if(minecraft.level.getEntity(gastlyId)==null){if(++ticks>200)throw new IllegalStateException("Gastly aura fixture was not tracked");return;}
                ticks++;
                if(ticks==32){
                    auraLarge=br.com.petrickfaria.minemon.client.GastlySmokeParticle.largeSpawnsForValidation();
                    auraSmall=br.com.petrickfaria.minemon.client.GastlySmokeParticle.smallSpawnsForValidation();
                    auraFrames=br.com.petrickfaria.minemon.client.GastlyCoreRenderHook.auraFramesForValidation();
                }
                if(ticks==64){
                    int large=br.com.petrickfaria.minemon.client.GastlySmokeParticle.largeSpawnsForValidation()-auraLarge;
                    int small=br.com.petrickfaria.minemon.client.GastlySmokeParticle.smallSpawnsForValidation()-auraSmall;
                    int frames=br.com.petrickfaria.minemon.client.GastlyCoreRenderHook.auraFramesForValidation()-auraFrames;
                    if(frames<=0||large!=frames*5||small!=frames*7)
                        throw new IllegalStateException("Gastly real-client aura render rate mismatch in state "+auraState
                                +": frames="+frames+" large="+large+" small="+small);
                    auraState++;ticks=0;
                    if(auraState<4){changeAuraState(minecraft,auraState);return;}
                    Files.writeString(Path.of("client-validation-pokemon-management.txt"),"PASS NINE VISUAL CONTRACTS: rendered Dewgong eyes closed asleep, open and blinking awake; real Gastly aura rendered exactly five large and seven small particles per rendered frame while awake, sleeping, sitting and defeated. Aesthetic comparison with the historical reference remains manual.\n",java.nio.file.StandardOpenOption.APPEND);
                    br.com.petrickfaria.minemon.client.MinemonRenderer.validationObserver=null;
                    var server = minecraft.getSingleplayerServer();
                    UUID ownerId = minecraft.player.getUUID();
                    server.execute(() -> {
                        var owner = server.getPlayerList().getPlayer(ownerId);
                        if (owner == null) throw new IllegalStateException("PokéC test player missing on the server");

                        // The real menu stays open only while its terminal exists and is in reach.
                        var level = owner.level;
                        var pos = owner.blockPosition().west(2);
                        var terminal = ModBlocks.POKEC.get().defaultBlockState();
                        level.setBlock(pos, terminal, Block.UPDATE_ALL);
                        level.setBlock(pos.above(), terminal.setValue(PokecBlock.HALF, DoubleBlockHalf.UPPER),
                                Block.UPDATE_ALL);
                        terminal.use(level, owner, InteractionHand.MAIN_HAND,
                                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                    });
                    phase=12;ticks=0;return;
                }
            }
            if(phase==12){
                if(!(minecraft.screen instanceof PokecScreen pokec)){
                    if(++ticks>120)throw new IllegalStateException("PokéC test screen did not open");
                    return;
                }
                if(pokec.catalogSizeForSmokeTest()!=151||pokec.listScrollMaxForSmokeTest()<1)
                    throw new IllegalStateException("PokéC scrollbar test requires the full 151-species list");
                if(!pokec.dragListScrollbarForSmokeTest(false,false)
                        ||pokec.listScrollOffsetForSmokeTest()!=pokec.listScrollMaxForSmokeTest())
                    throw new IllegalStateException("Clicking and dragging the visible PokéC thumb center did not move the list to the bottom");
                if(!pokec.dragListScrollbarForSmokeTest(false,true)||pokec.listScrollOffsetForSmokeTest()!=0)
                    throw new IllegalStateException("Dragging the visible PokéC thumb center did not return to the top");
                if(!pokec.dragListScrollbarForSmokeTest(true,false)
                        ||pokec.listScrollOffsetForSmokeTest()!=pokec.listScrollMaxForSmokeTest())
                    throw new IllegalStateException("Clicking and dragging the PokéC track did not clamp at the bottom");
                if(!pokec.scrollListForSmokeTest(1)||pokec.listScrollOffsetForSmokeTest()>=pokec.listScrollMaxForSmokeTest())
                    throw new IllegalStateException("PokéC mouse wheel did not scroll the list back toward the top");
                Files.writeString(Path.of("client-validation-pokemon-management.txt"),
                        "PASS POKÉC SCROLLBAR: direct visible-thumb center click, vertical drag, no species selection, top/bottom clamps and mouse-wheel scrolling; GUI scale="
                                +pokec.listUiScaleForSmokeTest()+".\n",java.nio.file.StandardOpenOption.APPEND);
                done=true;System.out.println("[POKEMON MANAGEMENT CLIENT] PASS: status, Blacklist filters/modal input, PokéC scrollbar and visual checks");minecraft.execute(minecraft::stop);
            }
        } catch (Exception failure) {
            throw new IllegalStateException("Pokémon management client validation failed", failure);
        }
    }

    private PokemonManagementClientSmokeTest() { }
}
