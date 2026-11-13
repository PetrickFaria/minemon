package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.block.PokecBlock;
import br.com.petrickfaria.minemon.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.core.Direction;
import net.minecraftforge.event.TickEvent;

/** Real-client block model validation and screenshots for all four horizontal faces. */
final class PokemonBlockClientSmokeTest {
    private static final String[] VIEWS = {"front", "side", "back", "three-quarter"};
    private static int ticks;
    private static int view;
    private static boolean opening;
    private static boolean preparing;
    private static boolean done;
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
                stageView(server, player, 0);
            });
            return;
        }
        if (!viewReady || ticks < 30) return;
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
            net.minecraft.client.Screenshot.grab(mc.gameDirectory, "pokec-world-" + VIEWS[view] + ".png", mc.getMainRenderTarget(), message -> {});
            System.out.println("[POKÉC BLOCK CLIENT] captured " + VIEWS[view]);
            if (++view >= VIEWS.length) {
                java.nio.file.Files.writeString(java.nio.file.Path.of("client-validation-pokec-block.txt"),
                        "PASS POKÉC BLOCK: front, side, back and three-quarter in-world screenshots; all 8 baked orientations and all 10 block textures loaded.\n");
                done = true;
                mc.execute(mc::stop);
                return;
            }
            ticks = 0;
            viewReady = false;
            var uuid = mc.player.getUUID();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(uuid);
                if (player == null) throw new IllegalStateException("PokéC screenshot player vanished");
                var state = ModBlocks.POKEC.get().defaultBlockState();
                Direction facing = switch (view) { case 1 -> Direction.EAST; case 2 -> Direction.SOUTH; default -> Direction.NORTH; };
                var level = (net.minecraft.server.level.ServerLevel) player.level;
                level.setBlock(base, state.setValue(PokecBlock.HALF, DoubleBlockHalf.LOWER).setValue(PokecBlock.FACING, facing), 3);
                level.setBlock(base.above(), state.setValue(PokecBlock.HALF, DoubleBlockHalf.UPPER).setValue(PokecBlock.FACING, facing), 3);
                stageView(server, player, view);
                viewReady = true;
            });
        } catch (Exception failure) { throw new IllegalStateException("PokéC real-client model validation failed", failure); }
    }

    private static void stageView(net.minecraft.server.MinecraftServer server, net.minecraft.server.level.ServerPlayer player, int index) {
        int bx = base.getX(), by = base.getY(), bz = base.getZ();
        switch (index) {
            case 0 -> player.moveTo(bx + .5, by, bz + 3.5, 180.0F, 0.0F);
            case 1 -> player.moveTo(bx + 3.5, by, bz + .5, 90.0F, 0.0F);
            case 2 -> player.moveTo(bx + .5, by, bz - 3.5, 0.0F, 0.0F);
            default -> player.moveTo(bx + 2.5, by, bz + 3.5, 150.0F, 0.0F);
        }
        viewReady = true;
    }
    private PokemonBlockClientSmokeTest() {}
}
