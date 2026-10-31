package br.com.petrickfaria.minemon.registry;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.menu.RocketTradeMenu;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

/** Persistent per-villager Rocket challenge progression and gated exchanges. */
@Mod.EventBusSubscriber(modid = Minemon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RocketChallengeManager {
    private static final String INITIALIZED = "MinemonRocketProgressionInitialized";
    private static final String LEVEL = "MinemonRocketLevel";
    private static final String TRADES = "MinemonRocketTradesThisLevel";
    private static final String UNLOCKED = "MinemonRocketTradesUnlocked";
    private static final String ACTIVE = "MinemonRocketActiveRattata"; // Retained for existing-world UUID compatibility.
    private static final String COOLDOWN = "MinemonRocketRetryAt";
    private static final String REWARDED = "MinemonRocketChallengeRewarded";
    private static final String VICTORY = "MinemonRocketChallengeVictory";

    private static final String[] SPECIES = {
            "zubat", "rattata", "ekans", "koffing", "golbat",
            "raticate", "arbok", "koffing", "kangaskhan", "mewtwo"
    };
    private static final int[] HEALTH = {10, 14, 18, 22, 26, 30, 34, 38, 42, 46};
    private static final int[] DAMAGE = {4, 6, 8, 10, 12, 14, 16, 18, 20, 22};
    private static final int[] TRADES_TO_NEXT = {5, 15, 25, 35, 45, 55, 65, 75, 85};

    public static int getLevel(Villager villager) {
        if (!isRocket(villager)) return 0;
        initialize(villager);
        return villager.getPersistentData().getInt(LEVEL);
    }

    public static int getTradesThisLevel(Villager villager) {
        if (!isRocket(villager)) return 0;
        initialize(villager);
        return villager.getPersistentData().getInt(TRADES);
    }

    public static int requiredTradesForNextLevel(int level) {
        return level >= 1 && level <= TRADES_TO_NEXT.length ? TRADES_TO_NEXT[level - 1] : 0;
    }

    public static String speciesForLevel(int level) {
        return level >= 1 && level <= SPECIES.length ? SPECIES[level - 1] : "";
    }

    public static int healthForLevel(int level) {
        return level >= 1 && level <= HEALTH.length ? HEALTH[level - 1] : 0;
    }

    public static int damageForLevel(int level) {
        return level >= 1 && level <= DAMAGE.length ? DAMAGE[level - 1] : 0;
    }

    public static boolean isChallengeComplete(Villager villager) {
        if (!isRocket(villager)) return false;
        initialize(villager);
        return villager.getPersistentData().getBoolean(UNLOCKED);
    }

    private static boolean isRocket(Villager villager) {
        return villager != null && villager.getVillagerData().getProfession() == ModVillagers.ROCKET.get();
    }

    /** One-time migration retains access for villagers that beat the former single Rattata challenge. */
    private static void initialize(Villager villager) {
        var data = villager.getPersistentData();
        if (data.getBoolean(INITIALIZED)) return;
        boolean completedLegacyChallenge = data.getBoolean("MinemonRocketChallengeComplete");
        data.putInt(LEVEL, 1);
        data.putInt(TRADES, 0);
        data.putBoolean(UNLOCKED, completedLegacyChallenge);
        data.putBoolean(INITIALIZED, true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || !(player.level instanceof ServerLevel level) || player.tickCount % 10 != 0) return;
        List<Villager> nearby = level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(5.0D),
                villager -> villager.isAlive() && isRocket(villager));
        for (Villager villager : nearby) activateIfReady(level, villager, player);
    }

    private static void activateIfReady(ServerLevel level, Villager villager, ServerPlayer player) {
        initialize(villager);
        var data = villager.getPersistentData();
        if (data.getBoolean(UNLOCKED)) return;
        if (data.hasUUID(ACTIVE)) return; // An unloaded entity may still be alive; never create a duplicate.
        if (level.getGameTime() < data.getLong(COOLDOWN)) return;
        spawnChallengePokemon(level, villager, player);
    }

    /** Creates the pending challenge for the villager's independent Rocket level. */
    public static MinemonCreature spawnChallengePokemon(ServerLevel level, Villager villager, ServerPlayer player) {
        if (!isRocket(villager) || player == null) return spawnFailure("profession/player guard");
        initialize(villager);
        var data = villager.getPersistentData();
        if (data.getBoolean(UNLOCKED) || data.hasUUID(ACTIVE)) return spawnFailure("already unlocked/active");

        int currentLevel = data.getInt(LEVEL);
        String species = speciesForLevel(currentLevel);
        var type = ModEntities.SPECIES.get(species);
        if (type == null) return spawnFailure("unregistered species " + species);
        MinemonCreature challenge = type.get().create(level);
        if (challenge == null) return spawnFailure("entity factory returned null for " + species);

        if (!moveToSafeSpawn(level, villager, challenge, player.getYRot())) {
            challenge.discard();
            return spawnFailure("no safe candidate; species=" + species + " villager=" + villager.blockPosition());
        }

        var maxHealth = challenge.getAttribute(Attributes.MAX_HEALTH);
        var attackDamage = challenge.getAttribute(Attributes.ATTACK_DAMAGE);
        if (maxHealth != null) maxHealth.setBaseValue(healthForLevel(currentLevel));
        if (attackDamage != null) attackDamage.setBaseValue(damageForLevel(currentLevel));
        challenge.setHealth(challenge.getMaxHealth());
        challenge.setRocketSpecial(true);
        challenge.setRocketVillager(villager.getUUID());
        challenge.getPersistentData().putInt("RocketChallengeLevel", currentLevel);
        challenge.setCustomName(Component.literal(species.substring(0, 1).toUpperCase(Locale.ROOT)
                + species.substring(1) + " Rocket"));
        challenge.setCustomNameVisible(true);
        challenge.setPersistenceRequired();
        challenge.setTarget(player);
        if (!level.addFreshEntity(challenge)) return spawnFailure("addFreshEntity rejected " + species);
        data.putUUID(ACTIVE, challenge.getUUID());
        data.putBoolean(VICTORY, false);
        return challenge;
    }

    private static boolean moveToSafeSpawn(ServerLevel level, Villager villager, MinemonCreature challenge, float yaw) {
        // Large species can overlap the villager even when the center is 1.5 blocks away.
        // Search a small ring so the challenge uses the first clear, dry position beside its trainer.
        double[][] offsets = {
                {2.25D, 0.0D}, {-2.25D, 0.0D}, {0.0D, 2.25D}, {0.0D, -2.25D},
                {2.25D, 2.25D}, {2.25D, -2.25D}, {-2.25D, 2.25D}, {-2.25D, -2.25D},
                {3.5D, 0.0D}, {-3.5D, 0.0D}, {0.0D, 3.5D}, {0.0D, -3.5D}
        };
        int fluid = 0, noFloor = 0, collision = 0;
        for (double[] offset : offsets) {
            double x = villager.getX() + offset[0];
            double y = villager.getY();
            double z = villager.getZ() + offset[1];
            challenge.moveTo(x, y, z, yaw, 0.0F);
            BlockPos pos = challenge.blockPosition();
            if (!level.getFluidState(pos).isEmpty()) { fluid++; continue; }
            if (!challenge.isFlyingSpecies() && !level.getBlockState(pos.below()).isSolidRender(level, pos.below())) { noFloor++; continue; }
            if (level.noCollision(challenge)) return true;
            collision++;
        }
        if (Boolean.getBoolean("minemon.debugRocketSpawns"))
            System.out.println("[ROCKET SPAWN DEBUG] candidates rejected fluid=" + fluid + " noFloor=" + noFloor
                    + " collision=" + collision + " flying=" + challenge.isFlyingSpecies());
        return false;
    }

    private static MinemonCreature spawnFailure(String reason) {
        if (Boolean.getBoolean("minemon.debugRocketSpawns")) System.out.println("[ROCKET SPAWN DEBUG] " + reason);
        return null;
    }

    /** Records exactly one server-confirmed output pickup from the Rocket exchange menu. */
    public static void recordCompletedTrade(Villager villager, ServerPlayer player) {
        if (!isRocket(villager) || player == null || player.level != villager.level) return;
        initialize(villager);
        var data = villager.getPersistentData();
        if (!data.getBoolean(UNLOCKED)) return;
        int currentLevel = data.getInt(LEVEL);
        if (currentLevel >= 10) return;
        int trades = data.getInt(TRADES) + 1;
        int needed = requiredTradesForNextLevel(currentLevel);
        if (trades < needed) {
            data.putInt(TRADES, trades);
            return;
        }
        data.putInt(LEVEL, currentLevel + 1);
        data.putInt(TRADES, 0);
        data.putBoolean(UNLOCKED, false);
        data.putBoolean(VICTORY, false);
        data.remove(COOLDOWN);
    }

    @SubscribeEvent
    public static void onSpecialPokemonDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof MinemonCreature challenge) || !challenge.isRocketSpecial()
                || !(challenge.level instanceof ServerLevel level)) return;
        UUID villagerId = challenge.getPersistentData().hasUUID("RocketVillager")
                ? challenge.getPersistentData().getUUID("RocketVillager") : null;
        Entity entity = villagerId == null ? null : level.getEntity(villagerId);
        if (!(entity instanceof Villager villager) || !isRocket(villager)) return;
        var data = villager.getPersistentData();
        if (!data.hasUUID(ACTIVE) || !data.getUUID(ACTIVE).equals(challenge.getUUID())) return;
        data.remove(ACTIVE);
        if (!(event.getSource().getEntity() instanceof ServerPlayer)) {
            data.putLong(COOLDOWN, level.getGameTime() + 200L);
            return;
        }
        if (data.getBoolean(UNLOCKED)) return;
        data.putBoolean(VICTORY, true);
        data.putBoolean(UNLOCKED, true);
        data.remove(COOLDOWN);
        if (!data.getBoolean(REWARDED)) {
            data.putBoolean(REWARDED, true);
            villager.spawnAtLocation(new ItemStack(ModItems.POKE_COIN.get(), 1), 0.2F);
        }
    }

    @SubscribeEvent
    public static void onRocketVillagerInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager) || !isRocket(villager)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        event.setCanceled(true);
        if (!isChallengeComplete(villager)) {
            int level = getLevel(villager);
            player.sendSystemMessage(Component.literal("Derrote o " + speciesForLevel(level).toUpperCase(Locale.ROOT)
                    + " Rocket para liberar as trocas do nível " + level + "."));
            return;
        }
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inventory, user) -> new RocketTradeMenu(id, inventory, villager),
                Component.literal("Trocas Rocket — Lv. " + getLevel(villager))),
                buffer -> buffer.writeInt(villager.getId()));
    }

    private RocketChallengeManager() { }
}
