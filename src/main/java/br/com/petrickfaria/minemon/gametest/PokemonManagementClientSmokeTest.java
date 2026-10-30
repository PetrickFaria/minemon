package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.menu.BlackListScreen;
import br.com.petrickfaria.minemon.menu.PokemonInfoMenu;
import br.com.petrickfaria.minemon.menu.PokemonInfoScreen;
import br.com.petrickfaria.minemon.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Focused real-client check for the Pokémon status and registry-backed Black List screens. */
public final class PokemonManagementClientSmokeTest {
    private static int ticks;
    private static boolean openingWorld;
    private static boolean done;
    private static volatile int entityId = -1;
    private static int phase;
    private static volatile int dewgongId=-1,gastlyId=-1;
    private static volatile boolean closedEyesSeen,openEyesSeen,awakeBlinkSeen;
    private static int auraState,auraLarge,auraSmall;
    private static void nineFixtures(Minecraft minecraft){
        minecraft.player.closeContainer();minecraft.setScreen(null);minecraft.player.setYRot(0);minecraft.player.setXRot(10);
        br.com.petrickfaria.minemon.client.MinemonRenderer.validationObserver=(entity,model)->{
            if(!entity.getSpeciesSlug().equals("dewgong"))return;
            var left=model.getAnimationProcessor().getBone("eyelid_left");var right=model.getAnimationProcessor().getBone("eyelid_right");
            if(left==null||right==null)throw new IllegalStateException("Dewgong eyelids missing");
            boolean closed=Math.abs(left.getPositionZ())<.001 && Math.abs(right.getPositionZ())<.001;
            boolean open=Math.abs(left.getPositionZ()+.1)<.001 && Math.abs(right.getPositionZ()+.1)<.001;
            if(entity.isSleeping()&&closed)closedEyesSeen=true;
            if(!entity.isSleeping()&&open)openEyesSeen=true;
            if(!entity.isSleeping()&&openEyesSeen&&closed)awakeBlinkSeen=true;
        };
        var ownerId=minecraft.player.getUUID();
        minecraft.getSingleplayerServer().execute(()->{
            var owner=minecraft.getSingleplayerServer().getPlayerList().getPlayer(ownerId);var level=(net.minecraft.server.level.ServerLevel)owner.level;
            level.setDayTime(1000);
            var dewgong=ModEntities.SPECIES.get("dewgong").get().create(level);dewgong.tame(owner);dewgong.setNoAi(true);dewgong.setNoGravity(true);
            dewgong.moveTo(owner.getX(),owner.getY(),owner.getZ()+3,180,0);dewgong.setSleeping(true);level.addFreshEntity(dewgong);dewgongId=dewgong.getId();
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
                if (!(minecraft.screen instanceof BlackListScreen screen)) {
                    if (++ticks > 120) throw new IllegalStateException("Black List button did not open the registry selector");
                    return;
                }
                ResourceLocation zombie = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(net.minecraft.world.entity.EntityType.ZOMBIE);
                ResourceLocation cow = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(net.minecraft.world.entity.EntityType.COW);
                if (screen.rowCountForSmokeTest() == 0 || !screen.containsTypeForSmokeTest(zombie)
                        || !screen.containsTypeForSmokeTest(cow) || !screen.selectedForSmokeTest(zombie)
                        || screen.selectedForSmokeTest(cow))
                    throw new IllegalStateException("Black List did not load registered hostile/passive mob defaults");
                var creeper=new ResourceLocation("minecraft","creeper");
                if(screen.selectedForSmokeTest(creeper))throw new IllegalStateException("Creeper was selected by default");
                java.util.Set<ResourceLocation> all=new java.util.HashSet<>();
                int panelWidth=Math.min(430,minecraft.getWindow().getGuiScaledWidth()-12);
                int panelHeight=Math.min(390,minecraft.getWindow().getGuiScaledHeight()-12);
                int left=(minecraft.getWindow().getGuiScaledWidth()-panelWidth)/2;
                int top=(minecraft.getWindow().getGuiScaledHeight()-panelHeight)/2;
                for(int tab=0;tab<3;tab++){
                    screen.mouseClicked(left+14+(tab+.5)*(panelWidth-28)/3.0,top+35,0);
                    if(screen.firstRowForSmokeTest()!=0)throw new IllegalStateException("Filter did not reset scroll");
                    for(var id:screen.visibleIdsForSmokeTest())if(!all.add(id))throw new IllegalStateException("Mob duplicated between tabs: "+id);
                    screen.mouseScrolled(left+20,top+60,-1);
                }
                if(all.size()!=screen.rowCountForSmokeTest() || !all.contains(cow) || !all.contains(zombie) || !all.contains(creeper))
                    throw new IllegalStateException("Blacklist tabs discarded registered mobs");
                if(!screen.selectedForSmokeTest(zombie) || screen.selectedForSmokeTest(cow))throw new IllegalStateException("Filter changed selections");
                Files.writeString(Path.of("client-validation-pokemon-management.txt"),
                        "PASS: owned Pokémon status screen, Pokédex header, XP level sync, Seguir/Sentar controls, and registry-backed Black List defaults; all three filter tabs clicked, complete disjoint mob partition, scrolling, preserved selections and unselected Creeper\n");
                nineFixtures(minecraft);phase=8;ticks=0;return;
            }
            if(phase==8){
                if(!closedEyesSeen){if(++ticks>240)throw new IllegalStateException("Rendered sleeping Dewgong did not close both eyes");return;}
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
                if(ticks==32){auraLarge=br.com.petrickfaria.minemon.client.GastlySmokeParticle.largeSpawnsForValidation();auraSmall=br.com.petrickfaria.minemon.client.GastlySmokeParticle.smallSpawnsForValidation();}
                if(ticks==64){
                    int large=br.com.petrickfaria.minemon.client.GastlySmokeParticle.largeSpawnsForValidation()-auraLarge;
                    int small=br.com.petrickfaria.minemon.client.GastlySmokeParticle.smallSpawnsForValidation()-auraSmall;
                    if(large!=10||small!=14)throw new IllegalStateException("Gastly real-client aura rate mismatch in state "+auraState+": large="+large+" small="+small);
                    auraState++;ticks=0;
                    if(auraState<4){changeAuraState(minecraft,auraState);return;}
                    Files.writeString(Path.of("client-validation-pokemon-management.txt"),"PASS NINE VISUAL CONTRACTS: rendered Dewgong eyes closed asleep, open and blinking awake; real Gastly particle providers emitted five large and seven small per16ticks while awake, sleeping, sitting and defeated. Aesthetic comparison with the supplied reference remains manual.\n",java.nio.file.StandardOpenOption.APPEND);
                    br.com.petrickfaria.minemon.client.MinemonRenderer.validationObserver=null;
                    done=true;System.out.println("[POKEMON MANAGEMENT CLIENT] PASS: status, filters, rendered Dewgong eyes, and permanent two-size Gastly aura");minecraft.execute(minecraft::stop);
                }
            }
        } catch (Exception failure) {
            throw new IllegalStateException("Pokémon management client validation failed", failure);
        }
    }

    private PokemonManagementClientSmokeTest() { }
}
