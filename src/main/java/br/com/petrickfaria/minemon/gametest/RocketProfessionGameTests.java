package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.registry.ModBlocks;
import br.com.petrickfaria.minemon.registry.ModItems;
import br.com.petrickfaria.minemon.registry.ModVillagers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(Minemon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RocketProfessionGameTests {
    private static void check(GameTestHelper helper, boolean value, String message) {
        if (!value) helper.fail(message);
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            for (int y = 1; y < 7; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        }
    }

    private static Villager unemployed(GameTestHelper helper, BlockPos position) {
        Villager villager = helper.spawn(EntityType.VILLAGER, position);
        villager.setVillagerData(villager.getVillagerData().setType(VillagerType.PLAINS)
                .setProfession(VillagerProfession.NONE));
        return villager;
    }

    private static void awaitProfession(GameTestHelper helper, Villager villager, BlockPos workstation, int remaining) {
        if (villager.getVillagerData().getProfession() == ModVillagers.ROCKET.get()) {
            var jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
            check(helper, jobSite.isPresent() && jobSite.get().pos().equals(workstation),
                    "Rocket profession must retain the claimed workstation POI; jobSite=" + jobSite
                            + ", potential=" + villager.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE));
            check(helper, villager.getOffers().isEmpty(), "Rocket villagers must have no trade offers in this version");
            CompoundTag saved = new CompoundTag();
            villager.saveWithoutId(saved);
            Villager reloaded = EntityType.VILLAGER.create(helper.getLevel());
            check(helper, reloaded != null, "vanilla villager must be constructible for the persistence check");
            reloaded.load(saved);
            check(helper, reloaded.getVillagerData().getProfession() == ModVillagers.ROCKET.get(),
                    "Rocket profession must survive vanilla villager NBT save/load");
            helper.succeed();
            return;
        }
        if (remaining <= 0) {
            helper.fail("adult unemployed villager did not naturally claim the accessible Rocket workstation; profession="
                    + villager.getVillagerData().getProfession() + ", jobSite="
                    + villager.getBrain().getMemory(MemoryModuleType.JOB_SITE));
            return;
        }
        helper.runAfterDelay(10, () -> awaitProfession(helper, villager, workstation, remaining - 10));
    }

    @GameTest(template = "test_arena", batch = "rocket_natural", timeoutTicks = 500)
    public static void adultUnemployedVillagerClaimsRocketWorkstationNaturally(GameTestHelper helper) {
        floor(helper);
        helper.getLevel().setDayTime(5000L);
        BlockPos workstation = new BlockPos(6, 1, 6);
        helper.setBlock(workstation, ModBlocks.ROCKET_BLOCK.get());
        helper.setBlock(workstation.below(), Blocks.STONE);
        Villager villager = unemployed(helper, new BlockPos(6, 1, 8));
        BlockPos absoluteWorkstation = helper.absolutePos(workstation);
        helper.runAfterDelay(10, () -> {
            var poi = helper.getLevel().getPoiManager().getType(absoluteWorkstation);
            check(helper, poi.isPresent() && poi.get().value() == ModVillagers.ROCKET_POI.get(),
                    "Rocket block state must be indexed as the registered Rocket POI; found " + poi);
            awaitProfession(helper, villager, absoluteWorkstation, 340);
        });
    }

    @GameTest(template = "test_arena", batch = "rocket_single_station", timeoutTicks = 500)
    public static void oneStationIsNotClaimedByTwoVillagers(GameTestHelper helper) {
        floor(helper);
        helper.getLevel().setDayTime(5000L);
        BlockPos workstation = new BlockPos(6, 1, 6);
        helper.setBlock(workstation, ModBlocks.ROCKET_BLOCK.get());
        helper.setBlock(workstation.below(), Blocks.STONE);
        Villager first = unemployed(helper, new BlockPos(5, 1, 8));
        Villager second = unemployed(helper, new BlockPos(7, 1, 8));
        helper.runAfterDelay(300, () -> {
            List<Villager> rocketWorkers = List.of(first, second).stream()
                    .filter(v -> v.getVillagerData().getProfession() == ModVillagers.ROCKET.get()).toList();
            check(helper, rocketWorkers.size() == 1,
                    "a one-ticket Rocket POI must have exactly one owner; claimed=" + rocketWorkers.size());
            var claimed = rocketWorkers.get(0).getBrain().getMemory(MemoryModuleType.JOB_SITE);
            check(helper, claimed.isPresent() && claimed.get().pos().equals(helper.absolutePos(workstation)),
                    "the sole Rocket villager must remember this workstation");
            helper.succeed();
        });
    }

    private static void awaitStationRelease(GameTestHelper helper, Villager villager, BlockPos workstation, int remaining) {
        var jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        if (jobSite.isEmpty() || !jobSite.get().pos().equals(workstation)) {
            check(helper, helper.getLevel().getPoiManager().getType(workstation).isEmpty(),
                    "breaking the Rocket block must remove its registered POI");
            helper.succeed();
            return;
        }
        if (remaining <= 0) {
            helper.fail("vanilla villager brain retained a job-site memory after the Rocket workstation was removed: " + jobSite);
            return;
        }
        helper.runAfterDelay(10, () -> awaitStationRelease(helper, villager, workstation, remaining - 10));
    }

    @GameTest(template = "test_arena", batch = "rocket_removal", timeoutTicks = 1000)
    public static void removingRocketBlockReleasesTheVanillaJobSiteMemory(GameTestHelper helper) {
        floor(helper);
        helper.getLevel().setDayTime(5000L);
        BlockPos workstation = new BlockPos(6, 1, 6);
        helper.setBlock(workstation, ModBlocks.ROCKET_BLOCK.get());
        helper.setBlock(workstation.below(), Blocks.STONE);
        Villager villager = unemployed(helper, new BlockPos(6, 1, 8));
        helper.runAfterDelay(10, () -> {
            if (villager.getVillagerData().getProfession() == ModVillagers.ROCKET.get()) {
                helper.setBlock(workstation, Blocks.AIR);
                awaitStationRelease(helper, villager, helper.absolutePos(workstation), 400);
                return;
            }
            awaitProfessionForRemoval(helper, villager, workstation, 350);
        });
    }

    private static void awaitProfessionForRemoval(GameTestHelper helper, Villager villager, BlockPos workstation, int remaining) {
        if (villager.getVillagerData().getProfession() == ModVillagers.ROCKET.get()) {
            helper.setBlock(workstation, Blocks.AIR);
            awaitStationRelease(helper, villager, helper.absolutePos(workstation), 400);
            return;
        }
        if (remaining <= 0) {
            helper.fail("villager did not claim the Rocket block before its station-removal test");
            return;
        }
        helper.runAfterDelay(10, () -> awaitProfessionForRemoval(helper, villager, workstation, remaining - 10));
    }

    @GameTest(template = "test_arena", batch = "rocket_exclusions", timeoutTicks = 150)
    public static void existingProfessionsBabiesAndNitwitsAreNotConverted(GameTestHelper helper) {
        floor(helper);
        helper.getLevel().setDayTime(5000L);
        BlockPos workstation = new BlockPos(6, 1, 6);
        helper.setBlock(workstation, ModBlocks.ROCKET_BLOCK.get());
        helper.setBlock(workstation.below(), Blocks.STONE);
        Villager farmer = helper.spawn(EntityType.VILLAGER, new BlockPos(4, 1, 8));
        farmer.setVillagerData(farmer.getVillagerData().setProfession(VillagerProfession.FARMER));
        Villager nitwit = helper.spawn(EntityType.VILLAGER, new BlockPos(6, 1, 9));
        nitwit.setVillagerData(nitwit.getVillagerData().setProfession(VillagerProfession.NITWIT));
        Villager baby = unemployed(helper, new BlockPos(8, 1, 8));
        baby.setBaby(true);
        helper.runAfterDelay(100, () -> {
            check(helper, farmer.getVillagerData().getProfession() != ModVillagers.ROCKET.get(),
                    "an existing profession must not be replaced by Rocket; actual="
                            + farmer.getVillagerData().getProfession());
            check(helper, nitwit.getVillagerData().getProfession() == VillagerProfession.NITWIT,
                    "nitwits must never acquire a job-site profession");
            check(helper, baby.isBaby() && baby.getVillagerData().getProfession() == VillagerProfession.NONE,
                    "villager babies must remain unemployed");
            helper.succeed();
        });
    }

    @GameTest(template = "test_arena", batch = "rocket_inaccessible", timeoutTicks = 400)
    public static void inaccessibleRocketBlockIsNotClaimed(GameTestHelper helper) {
        floor(helper);
        helper.getLevel().setDayTime(5000L);
        BlockPos workstation = new BlockPos(5, 1, 5);
        for (int x = 3; x <= 7; x++) for (int y = 1; y <= 4; y++) {
            helper.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
            helper.setBlock(new BlockPos(x, y, 7), Blocks.STONE);
        }
        for (int z = 3; z <= 7; z++) for (int y = 1; y <= 4; y++) {
            helper.setBlock(new BlockPos(3, y, z), Blocks.STONE);
            helper.setBlock(new BlockPos(7, y, z), Blocks.STONE);
        }
        helper.setBlock(workstation, ModBlocks.ROCKET_BLOCK.get());
        Villager villager = unemployed(helper, new BlockPos(5, 1, 9));
        helper.runAfterDelay(300, () -> {
            check(helper, villager.getVillagerData().getProfession() == VillagerProfession.NONE,
                    "a Rocket workstation without an accessible path must not be claimed");
            check(helper, villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).isEmpty(),
                    "an inaccessible workstation must not be stored as the villager's job-site memory");
            helper.succeed();
        });
    }

    @GameTest(template = "test_arena", timeoutTicks = 30)
    public static void rocketBlockProfessionAndPoiAreRegistered(GameTestHelper helper) {
        BlockState state = ModBlocks.ROCKET_BLOCK.get().defaultBlockState();
        check(helper, state.getMaterial().isSolid(), "Rocket workstation must be a normal solid block");
        check(helper, ModItems.ROCKET_BLOCK.get().getDefaultInstance().is(ModBlocks.ROCKET_BLOCK.get().asItem()),
                "Rocket workstation must have its own inventory item");
        check(helper, new net.minecraft.resources.ResourceLocation(Minemon.MOD_ID, "rocket")
                        .equals(net.minecraftforge.registries.ForgeRegistries.POI_TYPES.getKey(ModVillagers.ROCKET_POI.get())),
                "Rocket point of interest must be registered under the Minemon Rocket key");
        check(helper, new net.minecraft.resources.ResourceLocation(Minemon.MOD_ID, "rocket")
                        .equals(net.minecraftforge.registries.ForgeRegistries.VILLAGER_PROFESSIONS.getKey(ModVillagers.ROCKET.get())),
                "Rocket profession must be registered under the Minemon Rocket key");
        helper.succeed();
    }

    private RocketProfessionGameTests() {}
}
