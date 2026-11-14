package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.block.PokecBlock;
import br.com.petrickfaria.minemon.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.client.CameraType;
import net.minecraftforge.event.TickEvent;

/** Validates the baked PokéC models and photographs the placed block in the real client. */
final class PokemonBlockClientSmokeTest {
    private record View(String name, double x, double eyeY, double z) {}

    // Offsets are relative to the center of the north-facing PokéC at ground level.
    private static final View[] VIEWS = {
            new View("1-frente", .75, 1.45, -4.8),
            new View("2-lado-esquerdo", 4.8, 1.35, 0),
            new View("3-lado-direito", -4.8, 1.35, 0),
            new View("4-traseira", 0, 1.45, 4.8),
            new View("5-frontal-diagonal", -3.6, 2.65, -3.6),
            new View("6-traseira-diagonal", 3.6, 2.65, 3.6),
            new View("7-escala-jogador", .75, 1.55, -6.3)
    };
    private static int ticks;
    private static int view;
    private static boolean opening;
    private static boolean preparing;
    private static boolean done;
    private static ArmorStand camera;
    private static boolean captureRequested;
    private static volatile boolean captureFinished;
    private static volatile net.minecraft.core.BlockPos base;
    private static volatile boolean viewReady;

    static void tick(TickEvent.ClientTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (++ticks == 1) System.out.println("[POKÉC BLOCK CLIENT] screenshot and baked-model validation started");
        if (mc.player == null || mc.level == null) {
            if (opening) {
                if (ticks > 600) throw new IllegalStateException("Timed out loading the PokéC model validation world");
                return;
            }
            opening = true;
            try {
                Object flows = Minecraft.class.getMethod("createWorldOpenFlows").invoke(mc);
                boolean invoked = false;
                for (var method : flows.getClass().getMethods()) if (method.getName().equals("loadLevel")) {
                    if (method.getParameterCount() == 2 && method.getParameterTypes()[1] == String.class) {
                        method.invoke(flows, mc.screen, "minemon-validation"); invoked = true; break;
                    }
                    if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == String.class) {
                        method.invoke(flows, "minemon-validation"); invoked = true; break;
                    }
                }
                if (!invoked) throw new IllegalStateException("Could not open the prepared Minemon client world");
            } catch (Exception failure) { throw new IllegalStateException("PokéC block screenshot setup failed", failure); }
            return;
        }
        var server = mc.getSingleplayerServer();
        if (server == null) throw new IllegalStateException("PokéC screenshots require the prepared integrated server");
        if (!preparing) {
            preparing = true;
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            var playerId = mc.player.getUUID();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(playerId);
                if (player == null) throw new IllegalStateException("PokéC screenshot player is not loaded");
                var level = (net.minecraft.server.level.ServerLevel) player.level;
                player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                player.setHealth(player.getMaxHealth());
                level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,
                        new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(96)).forEach(net.minecraft.world.entity.Entity::discard);
                base = player.blockPosition().offset(0, 0, 5);
                level.setBlock(base, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(base.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(base, ModBlocks.POKEC.get().defaultBlockState().setValue(PokecBlock.HALF, DoubleBlockHalf.LOWER), 3);
                level.setBlock(base.above(), ModBlocks.POKEC.get().defaultBlockState().setValue(PokecBlock.HALF, DoubleBlockHalf.UPPER), 3);
                level.setDayTime(6000);
                player.connection.teleport(base.getX() + 1.8, base.getY(), base.getZ() + .5, 180, 0);
                viewReady = true;
            });
            return;
        }
        if (!viewReady || mc.screen != null) return;
        if (camera == null) {
            camera = new ArmorStand(mc.level, 0, 0, 0);
            stageView(mc);
            ticks = 0;
            return;
        }
        if (ticks < 75) return;
        try {
            for (String facing : new String[]{"north", "east", "south", "west"}) {
                for (String half : new String[]{"lower", "upper"}) {
                    var modelId = new ModelResourceLocation(new ResourceLocation("minemon_gen1", "pokec"),
                            "facing=" + facing + ",half=" + half);
                    if (mc.getModelManager().getModel(modelId) == mc.getModelManager().getMissingModel())
                        throw new IllegalStateException("Missing PokéC baked model " + modelId);
                }
            }
            for (String texture : new String[]{"pokec_dark", "pokec_red", "pokec_white", "pokec_gray", "pokec_side",
                    "pokec_vent", "pokec_glow", "pokec_button", "pokec_pokeball", "pokec_pokeball_red",
                    "pokec_pokeball_white", "pokec_screen"}) {
                var id = new ResourceLocation("minemon_gen1", "textures/block/" + texture + ".png");
                if (mc.getResourceManager().getResource(id).isEmpty()) throw new IllegalStateException("Missing PokéC block texture " + id);
            }
            String filename = "pokec-print-" + VIEWS[view].name() + ".png";
            var output = mc.gameDirectory.toPath().resolve("screenshots").resolve(filename);
            if (!captureRequested) {
                java.nio.file.Files.deleteIfExists(output);
                captureRequested = true;
                net.minecraft.client.Screenshot.grab(mc.gameDirectory, filename, mc.getMainRenderTarget(),
                        message -> captureFinished = true);
                return;
            }
            if (!captureFinished) return;
            if (!java.nio.file.Files.isRegularFile(output) || java.nio.file.Files.size(output) == 0)
                throw new IllegalStateException("Missing PokéC screenshot: " + filename);
            System.out.println("[POKÉC BLOCK CLIENT] captured " + filename);
            if (++view >= VIEWS.length) {
                java.nio.file.Files.writeString(java.nio.file.Path.of("client-validation-pokec-block.txt"),
                        "PASS POKÉC BLOCK: seven in-world screenshots, eight baked models and twelve textures.\n");
                done = true;
                mc.setCameraEntity(mc.player);
                mc.execute(mc::stop);
                return;
            }
            ticks = 0;
            captureRequested = false;
            captureFinished = false;
            stageView(mc);
            if (view == 1 || view == 2) {
                var uuid = mc.player.getUUID();
                double playerZ = base.getZ() + (view == 1 ? 1.8 : .5);
                server.execute(() -> {
                    var player = server.getPlayerList().getPlayer(uuid);
                    if (player == null) throw new IllegalStateException("PokéC screenshot player vanished");
                    player.connection.teleport(base.getX() + 1.8, base.getY(), playerZ, 180, 0);
                });
            }
        } catch (Exception failure) { throw new IllegalStateException("PokéC real-client model validation failed", failure); }
    }

    private static void stageView(Minecraft mc) {
        View shot = VIEWS[view];
        double targetX = view == 0 || view == 6 ? .6 : 0;
        double dx = targetX - shot.x();
        double dz = -shot.z();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(shot.eyeY() - 1, Math.hypot(dx, dz)));
        camera.moveTo(base.getX() + .5 + shot.x(), base.getY() + shot.eyeY() - camera.getEyeHeight(),
                base.getZ() + .5 + shot.z(), yaw, pitch);
        camera.xRotO = pitch;
        camera.yRotO = yaw;
        camera.setYHeadRot(yaw);
        camera.yHeadRotO = yaw;
        mc.setCameraEntity(camera);
    }

    private PokemonBlockClientSmokeTest() {}
}
