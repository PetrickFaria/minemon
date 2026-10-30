package br.com.petrickfaria.minemon.registry;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.menu.RocketTradeMenu;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

/** Server-persisted Rocket challenge and challenge-gated villager exchange. */
@Mod.EventBusSubscriber(modid = Minemon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RocketChallengeManager {
    private static final String COMPLETE = "MinemonRocketChallengeComplete";
    private static final String REWARDED = "MinemonRocketChallengeRewarded";
    private static final String ACTIVE = "MinemonRocketActiveRattata";
    private static final String COOLDOWN = "MinemonRocketRetryAt";

    public static boolean isChallengeComplete(Villager villager) {
        return villager != null && villager.getVillagerData().getProfession() == ModVillagers.ROCKET.get()
                && villager.getPersistentData().getBoolean(COMPLETE);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || !(player.level instanceof ServerLevel level) || player.tickCount % 10 != 0) return;
        List<Villager> nearby = level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(5.0D),
                villager -> villager.isAlive() && villager.getVillagerData().getProfession() == ModVillagers.ROCKET.get());
        for (Villager villager : nearby) activateIfReady(level, villager, player);
    }

    private static void activateIfReady(ServerLevel level, Villager villager, ServerPlayer player) {
        var data = villager.getPersistentData();
        if (data.getBoolean(COMPLETE)) return;
        if (data.hasUUID(ACTIVE)) {
            Entity active = level.getEntity(data.getUUID(ACTIVE));
            if (active != null && active.isAlive()) return;
            data.remove(ACTIVE);
            data.putLong(COOLDOWN, level.getGameTime() + 200L);
            return;
        }
        if (level.getGameTime() < data.getLong(COOLDOWN)) return;
        spawnChallengeRattata(level, villager, player);
    }

    /** Shared by the proximity handler and focused GameTests. */
    public static MinemonCreature spawnChallengeRattata(ServerLevel level, Villager villager, ServerPlayer player) {
        if (isChallengeComplete(villager) || villager.getPersistentData().hasUUID(ACTIVE)) return null;
        var rattataType = ModEntities.SPECIES.get("rattata");
        if (rattataType == null) return null;
        MinemonCreature rattata = rattataType.get().create(level);
        if (rattata == null) return null;
        rattata.moveTo(villager.getX() + 1.5D, villager.getY(), villager.getZ() + 0.5D,
                player.getYRot(), 0.0F);
        var maxHealth = rattata.getAttribute(Attributes.MAX_HEALTH);
        var damage = rattata.getAttribute(Attributes.ATTACK_DAMAGE);
        if (maxHealth != null) maxHealth.setBaseValue(br.com.petrickfaria.minemon.entity.PokemonProgression.healthForRarity(rattata.getPokemonRarity(),1));
        if (damage != null) damage.setBaseValue(br.com.petrickfaria.minemon.entity.PokemonProgression.damageForRarity(rattata.getPokemonRarity(),1));
        rattata.setHealth(rattata.getMaxHealth());
        rattata.setRocketSpecial(true);
        rattata.setRocketVillager(villager.getUUID());
        rattata.getPersistentData().putBoolean("RocketRattata", true);
        rattata.setCustomName(Component.literal("Rattata Rocket"));
        rattata.setCustomNameVisible(true);
        rattata.setPersistenceRequired();
        rattata.setTarget(player);
        if (!level.addFreshEntity(rattata)) return null;
        villager.getPersistentData().putUUID(ACTIVE, rattata.getUUID());
        return rattata;
    }

    @SubscribeEvent
    public static void onSpecialRattataDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof MinemonCreature rattata) || !rattata.isRocketSpecial()
                || !(rattata.level instanceof ServerLevel level)) return;
        UUID villagerId = rattata.getPersistentData().hasUUID("RocketVillager")
                ? rattata.getPersistentData().getUUID("RocketVillager") : null;
        Entity entity = villagerId == null ? null : level.getEntity(villagerId);
        if (!(entity instanceof Villager villager)) return;
        var data = villager.getPersistentData();
        data.remove(ACTIVE);
        if (!(event.getSource().getEntity() instanceof ServerPlayer)) {
            data.putLong(COOLDOWN, level.getGameTime() + 200L);
            return;
        }
        if (data.getBoolean(COMPLETE)) return;
        data.putBoolean(COMPLETE, true);
        if (!data.getBoolean(REWARDED)) {
            data.putBoolean(REWARDED, true);
            villager.spawnAtLocation(new ItemStack(ModItems.POKE_COIN.get(), 1), 0.2F);
        }
    }

    @SubscribeEvent
    public static void onRocketVillagerInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager)
                || villager.getVillagerData().getProfession() != ModVillagers.ROCKET.get()) return;
        // Leave the client event uncancelled so its normal entity-interaction packet
        // reaches the server; consume and replace the interaction on the server only.
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        event.setCanceled(true);
        if (!isChallengeComplete(villager)) {
            player.sendSystemMessage(Component.literal("Derrote o Rattata Rocket para liberar as trocas."));
            return;
        }
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inventory, user) -> new RocketTradeMenu(id, inventory, villager),
                Component.literal("Trocas Rocket")), buffer -> buffer.writeInt(villager.getId()));
    }

    private RocketChallengeManager() { }
}
