package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.registry.ModBlocks;
import br.com.petrickfaria.minemon.registry.ModVillagers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraftforge.event.TickEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** Renders true vanilla villagers in the Rocket profession for every 1.19.2 biome type. */
final class RocketVillagerClientSmokeTest {
    private static final List<VillagerType> TYPES = List.of(VillagerType.PLAINS, VillagerType.DESERT,
            VillagerType.JUNGLE, VillagerType.SAVANNA, VillagerType.SNOW, VillagerType.SWAMP, VillagerType.TAIGA);
    private static boolean opened;
    private static boolean setupRequested;
    private static boolean screenshotRequested;
    private static boolean completed;
    private static int titleTicks;
    private static int renderTicks;
    private static int diagnosticStage;
    private static UUID localPlayer;

    static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !Boolean.getBoolean("minemon.clientSmokeTest")
                || !Boolean.getBoolean("minemon.clientSmokeRocketProfession") || completed) return;
        Minecraft minecraft = Minecraft.getInstance();
        try {
            if (diagnosticStage == 0) {
                System.out.println("[ROCKET CLIENT] tick handler active; screen="
                        + (minecraft.screen == null ? "null" : minecraft.screen.getClass().getSimpleName()));
                diagnosticStage = 1;
            }
            if (!opened && minecraft.screen instanceof TitleScreen) {
                if (++titleTicks < 20) return;
                Path levelFile = Path.of("saves/minemon-validation/level.dat");
                var levelTag = net.minecraft.nbt.NbtIo.readCompressed(levelFile.toFile());
                levelTag.getCompound("Data").putBoolean("confirmedExperimentalSettings", true);
                net.minecraft.nbt.NbtIo.writeCompressed(levelTag, levelFile.toFile());
                Object flows = Minecraft.class.getMethod("createWorldOpenFlows").invoke(minecraft);
                boolean invoked = false;
                for (var method : flows.getClass().getMethods()) {
                    if (!method.getName().equals("loadLevel")) continue;
                    if (method.getParameterCount() == 2 && method.getParameterTypes()[1] == String.class) {
                        method.invoke(flows, minecraft.screen, "minemon-validation"); invoked = true; break;
                    }
                    if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == String.class) {
                        method.invoke(flows, "minemon-validation"); invoked = true; break;
                    }
                }
                if (!invoked) throw new IllegalStateException("Could not open the prepared Rocket profession world");
                minecraft.options.hideGui = true;
                opened = true;
                System.out.println("[ROCKET CLIENT] requested prepared world load");
                return;
            }

            if (!opened || minecraft.level == null || minecraft.player == null || minecraft.getSingleplayerServer() == null)
                return;

            if (diagnosticStage == 1) {
                System.out.println("[ROCKET CLIENT] world ready; player=" + minecraft.player.blockPosition()
                        + ", server=" + minecraft.getSingleplayerServer().isRunning());
                diagnosticStage = 2;
            }
            minecraft.player.setYRot(180.0F);
            minecraft.player.yRotO = 180.0F;
            minecraft.player.setXRot(0.0F);

            if (!setupRequested) {
                setupRequested = true;
                localPlayer = minecraft.player.getUUID();
                minecraft.getSingleplayerServer().execute(() -> {
                    System.out.println("[ROCKET CLIENT] server fixture setup started");
                    setupWorld(minecraft.getSingleplayerServer().overworld());
                    System.out.println("[ROCKET CLIENT] server fixture setup complete");
                });
                return;
            }

            if (++renderTicks < 50) return;
            if (diagnosticStage == 2) {
                System.out.println("[ROCKET CLIENT] checking rendered villagers after " + renderTicks + " ticks");
                diagnosticStage = 3;
            }
            var visible = minecraft.level.getEntitiesOfClass(Villager.class,
                    new net.minecraft.world.phys.AABB(minecraft.player.blockPosition()).inflate(12.0D),
                    villager -> villager.getVillagerData().getProfession() == ModVillagers.ROCKET.get());
            if (visible.size() != TYPES.size())
                throw new IllegalStateException("Expected seven visible Rocket villagers, got " + visible.size());
            for (VillagerType type : TYPES) {
                if (visible.stream().noneMatch(v -> v.getVillagerData().getType() == type))
                    throw new IllegalStateException("Rocket uniform was not retained for villager type " + type);
            }
            for (String path : List.of(
                    "/assets/minemon_gen1/textures/entity/villager/profession/rocket.png",
                    "/assets/minemon_gen1/textures/entity/villager/profession/rocket.png.mcmeta",
                    "/assets/minemon_gen1/textures/entity/zombie_villager/profession/rocket.png",
                    "/assets/minemon_gen1/textures/pokemon/rattata_rocket.png")) {
                try (var input = RocketVillagerClientSmokeTest.class.getResourceAsStream(path)) {
                    if (input == null) throw new IllegalStateException("Missing Rocket villager visual resource " + path);
                    if (path.endsWith(".png") && javax.imageio.ImageIO.read(input) == null)
                        throw new IllegalStateException("Unreadable Rocket villager texture " + path);
                }
            }
            String translated = net.minecraft.client.resources.language.I18n.get("entity.minecraft.villager.rocket");
            if (translated.equals("entity.minecraft.villager.rocket"))
                translated = net.minecraft.client.resources.language.I18n.get("entity.minecraft.villager.minemon_gen1.rocket");
            if (translated.equals("entity.minecraft.villager.rocket") || translated.equals("entity.minecraft.villager.minemon_gen1.rocket"))
                throw new IllegalStateException("Rocket villager profession translation was not loaded");

            if (screenshotRequested) return;
            screenshotRequested = true;
            System.out.println("[ROCKET CLIENT] requesting screenshot callback");
            Screenshot.grab(minecraft.gameDirectory, "rocket-profession-biome-variants.png",
                    minecraft.getMainRenderTarget(), message -> {
                        try {
                            Files.writeString(Path.of("client-validation-rocket.txt"),
                                    "PASS ROCKET CLIENT: real vanilla Rocket villagers rendered for all seven 1.19.2 biome types; "
                                            + "Rocket and zombie-villager uniform textures, hat metadata, and profession translation loaded.\n");
                            completed = true;
                            System.out.println("[ROCKET CLIENT] PASS marker written; stopping client");
                            minecraft.execute(minecraft::stop);
                        } catch (Exception failure) {
                            throw new IllegalStateException("Could not write Rocket client smoke result", failure);
                        }
                    });
        } catch (Exception failure) {
            throw new IllegalStateException("Rocket villager client validation failed", failure);
        }
    }

    private static void setupWorld(ServerLevel level) {
        Player player = level.getPlayerByUUID(localPlayer);
        if (player == null) throw new IllegalStateException("Local test player was not found on the server");
        BlockPos origin = player.blockPosition();
        int x = origin.getX(), y = origin.getY(), z = origin.getZ();
        level.setDayTime(1000L);
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
        for (int dx = -5; dx <= 5; dx++) for (int dz = 2; dz <= 11; dz++)
            level.setBlock(new BlockPos(x + dx, y - 1, z + dz), Blocks.STONE.defaultBlockState(), 3);
        BlockPos workstation = new BlockPos(x, y, z + 3);
        level.setBlock(workstation, ModBlocks.ROCKET_BLOCK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH), 3);
        player.teleportTo(x + 0.5D, y, z + 11.0D);
        player.setYRot(180.0F);
        player.yRotO = 180.0F;
        for (int i = 0; i < TYPES.size(); i++) {
            Villager villager = EntityType.VILLAGER.create(level);
            if (villager == null) throw new IllegalStateException("Could not create vanilla villager");
            villager.setVillagerData(villager.getVillagerData().setType(TYPES.get(i)).setProfession(VillagerProfession.NONE));
            villager.setVillagerData(villager.getVillagerData().setProfession(ModVillagers.ROCKET.get()));
            // This fixture validates the profession uniform only; challenge behavior
            // is exercised by RocketChallengeGameTests.
            villager.getPersistentData().putBoolean("MinemonRocketChallengeComplete", true);
            villager.setNoAi(true);
            villager.setInvulnerable(true);
            villager.moveTo(x - 3 + i + 0.5D, y, z + 6.0D, 0.0F, 0.0F);
            if (!level.addFreshEntity(villager)) throw new IllegalStateException("Could not add Rocket villager to the render fixture");
        }
    }

    private RocketVillagerClientSmokeTest() {}
}
