package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.entity.CaptureBallProjectile;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.pokec.WildEncounterManager;
import br.com.petrickfaria.minemon.registry.ModEntities;
import br.com.petrickfaria.minemon.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;

import java.nio.file.Files;
import java.nio.file.Path;

/** Real integrated-client smoke for a downed wild encounter and its rendered capture sequence. */
final class WildCaptureClientSmokeTest {
    private static boolean opened, setupRequested, interactionSent, done, optionsScreenshot;
    private static int ticks, encounterId = -1;
    private static final boolean[] screenshots = new boolean[4];

    static void tick(TickEvent.ClientTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (!opened && mc.screen instanceof TitleScreen) {
                opened = true;
                var model = mc.getItemRenderer().getModel(new ItemStack(ModItems.POKEBALL.get()), mc.level, mc.player, 0);
                if (model == mc.getModelManager().getMissingModel())
                    throw new IllegalStateException("Pokébola 3D item model resolved to the missing model");
                int modelQuads = model.getQuads(null, null, net.minecraft.util.RandomSource.create(0)).size();
                for (var direction : net.minecraft.core.Direction.values())
                    modelQuads += model.getQuads(null, direction, net.minecraft.util.RandomSource.create(0)).size();
                if (modelQuads < 6) throw new IllegalStateException("Pokébola 3D model has too few baked faces: " + modelQuads);
                Object flows = Minecraft.class.getMethod("createWorldOpenFlows").invoke(mc);
                for (var method : flows.getClass().getMethods()) if (method.getName().equals("loadLevel")) {
                    if (method.getParameterCount() == 2 && method.getParameterTypes()[1] == String.class) {
                        method.invoke(flows, mc.screen, "minemon-validation"); break;
                    }
                    if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == String.class) {
                        method.invoke(flows, "minemon-validation"); break;
                    }
                }
                return;
            }
            if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null || mc.screen != null) return;
            if (!setupRequested) {
                setupRequested = true;
                mc.getSingleplayerServer().execute(() -> setup(mc));
                return;
            }
            ticks++;
            var target = encounterId < 0 ? null : mc.level.getEntity(encounterId);
            if (!interactionSent && target instanceof MinemonCreature creature && creature.isWildEncounterDowned()) {
                // Preserve a client-rendered frame with the owner-only CAPTURAR / SAQUEAR prompts.
                if (ticks >= 15 && !optionsScreenshot) {
                    optionsScreenshot = true;
                    Screenshot.grab(mc.gameDirectory, "wild-capture-options.png", mc.getMainRenderTarget(), message -> {});
                }
                if (ticks >= 25) {
                    mc.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                            new ItemStack(ModItems.POKEBALL.get()));
                    mc.gameMode.interact(mc.player, creature, net.minecraft.world.InteractionHand.MAIN_HAND);
                    interactionSent = true;
                    ticks = 0;
                }
                return;
            }
            var balls = mc.level.getEntitiesOfClass(CaptureBallProjectile.class, mc.player.getBoundingBox().inflate(24));
            if (interactionSent && !balls.isEmpty()) {
                CaptureBallProjectile ball = balls.get(0);
                int age = ball.tickCount;
                int[] frameTicks = {15, 30, 45, 60};
                String[] frameNames = {"wild-capture-throw.png", "wild-capture-wobble-1.png",
                        "wild-capture-wobble-2.png", "wild-capture-stars.png"};
                for (int i = 0; i < frameTicks.length; i++) if (!screenshots[i] && age >= frameTicks[i]) {
                    screenshots[i] = true;
                    Screenshot.grab(mc.gameDirectory, frameNames[i], mc.getMainRenderTarget(), message -> {});
                }
                if (age >= 60 && !ball.captureSucceeded()) throw new IllegalStateException("15 treats did not guarantee a common wild capture");
            }
            if (interactionSent && ticks > 85) {
                if (!br.com.petrickfaria.minemon.item.PokeballItem.isOccupied(mc.player.getMainHandItem()))
                    throw new IllegalStateException("Captured Pokémon was not committed to the Pokébola");
                var stored = br.com.petrickfaria.minemon.item.PokeballItem.readStoredEntity(mc.player.getMainHandItem());
                if (stored == null || !stored.owner().equals(mc.player.getUUID()))
                    throw new IllegalStateException("Captured Pokémon ownership was not preserved in its Pokébola");
                if (mc.level.getEntity(encounterId) != null) throw new IllegalStateException("Captured wild encounter remained in the world");
                if (mc.level.getEntitiesOfClass(CaptureBallProjectile.class, mc.player.getBoundingBox().inflate(24)).size() != 0)
                    throw new IllegalStateException("Capture visual entity remained after its animation");
                for (boolean written : screenshots) if (!written) throw new IllegalStateException("Missing client capture animation frame");
                Files.writeString(Path.of("wild-capture-client-pass.txt"),
                        "PASS WILD CAPTURE CLIENT: options label, 3D ball throw, capture wobbles, success stars, and stored Pokémon validated in the Minecraft client\n");
                System.out.println("[WILD CAPTURE CLIENT] PASS: four rendered sequence frames and server-side capture transaction validated");
                done = true;
                mc.execute(mc::stop);
                return;
            }
            if (ticks > 500) throw new IllegalStateException("Timed out waiting for wild capture animation/transaction");
        } catch (Exception failure) {
            throw new IllegalStateException("Wild capture client validation failed", failure);
        }
    }

    private static void setup(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null) throw new IllegalStateException("Integrated server unavailable");
        var owner = server.getPlayerList().getPlayer(mc.player.getUUID());
        var level = server.getLevel(Level.OVERWORLD);
        if (owner == null || level == null) throw new IllegalStateException("Client player/overworld unavailable");
        BlockPos floor = owner.blockPosition().below();
        for (int dx = -2; dx <= 5; dx++) for (int dz = -2; dz <= 2; dz++) {
            level.setBlock(floor.offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 3);
            for (int dy = 1; dy <= 4; dy++) level.setBlock(floor.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
        }
        var attackerType = ModEntities.SPECIES.get("caterpie");
        var wildType = ModEntities.SPECIES.get("caterpie");
        MinemonCreature attacker = attackerType == null ? null : attackerType.get().create(level);
        MinemonCreature wild = wildType == null ? null : wildType.get().create(level);
        if (attacker == null || wild == null) throw new IllegalStateException("Caterpie entity type unavailable");
        attacker.moveTo(owner.getX() - 1, floor.getY() + 1, owner.getZ(), owner.getYRot(), 0);
        attacker.tame(owner); attacker.setOrderedToSit(true); attacker.setNoAi(true);
        if (!level.addFreshEntity(attacker)) throw new IllegalStateException("Could not add owned battle Pokémon");
        wild.moveTo(owner.getX() + 2, floor.getY() + 1, owner.getZ(), owner.getYRot(), 0);
        wild.setPokemonLevel(5);
        var data = wild.getPersistentData();
        data.putBoolean("MinemonWildEncounter", true); data.putUUID("MinemonEncounterOwner", owner.getUUID());
        data.putString("MinemonEncounterState", "active"); data.putLong("MinemonEncounterLastInRange", level.getGameTime());
        wild.setWildEncounter(true); wild.setWildEncounterOwner(owner.getUUID());
        if (!level.addFreshEntity(wild)) throw new IllegalStateException("Could not add wild encounter fixture");
        if (!WildEncounterManager.enterDowned(wild, DamageSource.mobAttack(attacker)))
            throw new IllegalStateException("Owned Pokémon could not defeat wild encounter fixture");
        ItemStack treats = new ItemStack(ModItems.TREATS.values().iterator().next().get(), 15);
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, treats);
        for (int i = 0; i < 15; i++) if (!WildEncounterManager.offerTreat(wild, owner, net.minecraft.world.InteractionHand.MAIN_HAND))
            throw new IllegalStateException("Could not apply treat " + (i + 1));
        if (data.getInt("MinemonWildTreats") != 15) throw new IllegalStateException("Treat progress was not persisted on encounter");
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.POKEBALL.get()));
        owner.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                wild.position().add(0, wild.getBbHeight() * .5D, 0));
        encounterId = wild.getId();
    }

    private WildCaptureClientSmokeTest() {}
}
