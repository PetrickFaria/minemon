package br.com.petrickfaria.minemon.pokec;

import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.entity.SpeciesBehaviorProfiles;
import br.com.petrickfaria.minemon.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Server authority for player-bound wild encounters. Encounter metadata lives on the Pokémon entity. */
public final class WildEncounterManager {
    public enum CaptureResult { INVALID, FAILED, EXPIRED, CAPTURED }
    private static final String MARKER = "MinemonWildEncounter";
    private static final String OWNER = "MinemonEncounterOwner";
    private static final String STATE = "MinemonEncounterState";
    private static final String DOWNED_AT = "MinemonEncounterDownedAt";
    private static final String LAST_IN_RANGE = "MinemonEncounterLastInRange";
    private static final String REWARDED = "MinemonEncounterRewarded";
    private static final String LAST_CHECK = "MinemonWildSpawnCheck";
    private static final long CHECK_INTERVAL = 20L * 30L;
    private static final long EXPIRY = 20L * 60L * 5L;
    public static final int MAX_ACTIVE = 5, MAX_DOWNED = 3;
    private static final List<String> RARITIES = List.of("common", "uncommon", "rare", "legendary", "mythic");
    private static volatile java.util.Map<String, Double> rarityChances = java.util.Map.of(
            "common", .10D, "uncommon", .045D, "rare", .015D, "legendary", .003D, "mythic", .001D);

    private static volatile java.util.Map<String, Double> speciesChances = java.util.Map.of();
    private static volatile boolean configLoaded;

    public static synchronized void loadConfig(ServerLevel level) {
        Path file = level.getServer().getServerDirectory().toPath().resolve("config/minemon/wild-spawn.properties");
        var values = new java.util.LinkedHashMap<String, Double>(rarityChances);
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                var lines = List.of("# Chance per 30-second player check before environment modifiers.",
                        "# Values are probabilities from 0 to 1; species can override with species.<slug>=value.",
                        "rarity.common=0.10", "rarity.uncommon=0.045", "rarity.rare=0.015",
                        "rarity.legendary=0.003", "rarity.mythic=0.001");
                Files.write(file, lines);
            }
            var rarity = new java.util.HashMap<String, Double>();
            var species = new java.util.HashMap<String, Double>();
            for (String line : Files.readAllLines(file)) {
                String trimmed = line.strip();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) continue;
                String[] pair = trimmed.split("=", 2);
                try {
                    double value = Double.parseDouble(pair[1].strip());
                    if (!Double.isFinite(value) || value < 0 || value > 1) continue;
                    String key = pair[0].strip().toLowerCase(Locale.ROOT);
                    if (key.startsWith("rarity.")) rarity.put(key.substring(7), value);
                    else if (key.startsWith("species.") && PokecSpecies.get(key.substring(8)) != null) species.put(key.substring(8), value);
                } catch (NumberFormatException ignored) { }
            }
            for (String key : RARITIES) values.put(key, rarity.getOrDefault(key, values.get(key)));
            rarityChances = java.util.Map.copyOf(values);
            speciesChances = java.util.Map.copyOf(species);
            configLoaded = true;
        } catch (IOException error) {
            throw new IllegalStateException("Unable to load wild-spawn config " + file, error);
        }
    }
    public static boolean isEncounter(MinemonCreature pokemon) { return pokemon.getPersistentData().getBoolean(MARKER); }
    public static boolean isDowned(MinemonCreature pokemon) {
        return isEncounter(pokemon) && "downed".equals(pokemon.getPersistentData().getString(STATE));
    }
    public static UUID owner(MinemonCreature pokemon) {
        var data = pokemon.getPersistentData();
        return data.hasUUID(OWNER) ? data.getUUID(OWNER) : null;
    }

    public static void tickPlayer(ServerPlayer player) {
        if (!(player.level instanceof ServerLevel level)) return;
        if (!configLoaded) loadConfig(level);
        long now = level.getGameTime();
        WildEncounterData encounterData = WildEncounterData.get(level);
        int active = encounterData.count(player.getUUID(), "active");
        var pd = player.getPersistentData();
        if (now - pd.getLong("MinemonWildSpawnAttempt") < CHECK_INTERVAL) return;
        pd.putLong("MinemonWildSpawnAttempt", now);
        if (active >= MAX_ACTIVE || player.getRandom().nextDouble() >= .35D) return;
        List<PokecSpecies.Entry> candidates = PokecPlayerData.unlocked(player).stream()
                .map(PokecSpecies::get).filter(java.util.Objects::nonNull)
                .filter(entry -> ModEntities.SPECIES.containsKey(entry.slug()))
                .sorted(Comparator.comparingInt(PokecSpecies.Entry::number)).toList();
        if (candidates.isEmpty()) return;
        for (int i = 0; i < 16; i++) {
            PokecSpecies.Entry species = candidates.get(player.getRandom().nextInt(candidates.size()));
            double chance = speciesChances.getOrDefault(species.slug(), rarityChances.getOrDefault(
                    PokecEconomy.resolveRarity(species.rarity(), "common"), .05D));
            if (player.getRandom().nextDouble() >= chance * .35D) continue;
            BlockPos pos = samplePosition(player, level, species);
            if (pos == null || !environmentAllows(species, level, pos)) continue;
            Entity created = ModEntities.SPECIES.get(species.slug()).get().create(level);
            if (!(created instanceof MinemonCreature pokemon)) { if (created != null) created.discard(); continue; }
            pokemon.moveTo(pos.getX() + .5D, pos.getY(), pos.getZ() + .5D, player.getRandom().nextFloat() * 360, 0);
            pokemon.setPokemonLevel(spawnLevel(species, player.getRandom()));
            var data = pokemon.getPersistentData();
            data.putBoolean(MARKER, true); data.putUUID(OWNER, player.getUUID()); data.putString(STATE, "active");
            data.putLong(LAST_CHECK, now);
            data.putLong(LAST_IN_RANGE, now);
            if (level.addFreshEntity(pokemon)) encounterData.put(player.getUUID(), pokemon.getUUID(), "active");
            else pokemon.discard();
            return;
        }
    }

    private static BlockPos samplePosition(ServerPlayer player, ServerLevel level, PokecSpecies.Entry species) {
        boolean aquatic = SpeciesBehaviorProfiles.waterCapable(species.slug());
        for (int n = 0; n < 12; n++) {
            int x = player.blockPosition().getX() + player.getRandom().nextInt(151) - 75;
            int z = player.blockPosition().getZ() + player.getRandom().nextInt(151) - 75;
            int y = player.blockPosition().getY() + player.getRandom().nextInt(31) - 15;
            BlockPos pos = new BlockPos(x, y, z);
            if (player.distanceToSqr(Vec3.atCenterOf(pos)) < 64 || !level.hasChunkAt(pos)) continue;
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) continue;
            if (level.getBlockState(pos).is(Blocks.LAVA) || level.getBlockState(pos).is(Blocks.FIRE)
                    || level.getBlockState(pos).is(Blocks.SOUL_FIRE)) continue;
            if (aquatic ? !level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)
                    : !level.getFluidState(pos).isEmpty()) continue;
            return pos;
        }
        return null;
    }

    private static boolean environmentAllows(PokecSpecies.Entry species, ServerLevel level, BlockPos pos) {
        boolean aquatic = SpeciesBehaviorProfiles.waterCapable(species.slug());
        if (aquatic) {
            int water = 0;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                if (level.getFluidState(pos.offset(dx, 0, dz)).is(net.minecraft.tags.FluidTags.WATER)) water++;
            return water >= 5;
        }
        if (level.getFluidState(pos.below()).is(net.minecraft.tags.FluidTags.LAVA)) return false;
        String slug = species.slug();
        if (slug.equals("geodude") || slug.equals("graveler") || slug.equals("golem"))
            return level.getBlockState(pos.below()).isSolidRender(level, pos.below());
        if (slug.equals("oddish") || slug.equals("gloom") || slug.equals("vileplume"))
            return level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK) || level.getBlockState(pos.below()).is(Blocks.FERN);
        if (slug.equals("gastly") || slug.equals("haunter") || slug.equals("gengar")) return level.getMaxLocalRawBrightness(pos) <= 7;
        return level.getBlockState(pos.below()).isSolidRender(level, pos.below());
    }

    private static int spawnLevel(PokecSpecies.Entry species, net.minecraft.util.RandomSource random) {
        int min = species.isEvolution() ? PokemonEvolutionRules.requiredLevel(species.evolvesFrom()) : 1;
        if (!PokemonEvolutionRules.nextForms(species.slug()).isEmpty()) min = 1;
        int max = Math.max(min, Math.min(30, min + 15));
        return min + random.nextInt(max - min + 1);
    }

    public static boolean canDefeat(MinemonCreature wild, MinemonCreature attacker) {
        UUID owner = owner(wild);
        if (!isEncounter(wild) || isDowned(wild) || !attacker.isTame() || owner == null || !owner.equals(attacker.getOwnerUUID())) return false;
        ServerPlayer player = wild.level instanceof ServerLevel server ? server.getServer().getPlayerList().getPlayer(owner) : null;
        if (player == null) return false;
        return WildEncounterData.get((ServerLevel) wild.level).count(owner, "downed") < MAX_DOWNED;
    }

    public static boolean hasDefeatedCapacity(MinemonCreature wild) {
        UUID owner = owner(wild);
        if (owner == null || !(wild.level instanceof ServerLevel level)) return false;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        return player != null && WildEncounterData.get(level).count(owner, "downed") < MAX_DOWNED;
    }

    public static boolean enterDowned(MinemonCreature wild, MinemonCreature attacker) {
        if (!canDefeat(wild, attacker)) return false;
        var data = wild.getPersistentData();
        data.putString(STATE, "downed"); data.putLong(DOWNED_AT, wild.level.getGameTime());
        data.putInt("MinemonWildCaptureAttempts", 0); data.putInt("MinemonWildTreats", 0);
        WildEncounterData.get((ServerLevel) wild.level).put(owner(wild), wild.getUUID(), "downed");
        wild.setHealth(1.0F); wild.setEncounterDowned(true);
        ServerPlayer player = wild.level instanceof ServerLevel server ? server.getServer().getPlayerList().getPlayer(owner(wild)) : null;
        if (player != null) PokecPlayerData.addTrainerXp(player, TrainerProgression.pokemonXp(wild.getSpeciesSlug(), wild.getPokemonLevel()));
        return true;
    }

    public static void rewardAndDiscard(ServerLevel level, MinemonCreature pokemon, int percent) {
        var data = pokemon.getPersistentData();
        if (!isEncounter(pokemon) || data.getBoolean(REWARDED)) { pokemon.discard(); return; }
        data.putBoolean(REWARDED, true);
        WildEncounterData.get(level).remove(owner(pokemon), pokemon.getUUID());
        var preview = PokemonSoulConversion.preview(pokemon.getSpeciesSlug(), pokemon.getPokemonLevel());
        if (preview != null && preview.soulType() != null) {
            int amount = WildCaptureRules.souls(preview.base(), percent);
            var item = br.com.petrickfaria.minemon.registry.ModItems.SOULS.get(preview.soulType().id());
            if (item != null && amount > 0) level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level,
                    pokemon.getX(), pokemon.getY(), pokemon.getZ(), new net.minecraft.world.item.ItemStack(item.get(), amount)));
        }
        pokemon.discard();
    }

    /** Encounter lifecycle is driven by the loaded entity, so no chunks are force-loaded. */
    public static void tickEncounter(MinemonCreature pokemon) {
        if (pokemon.level.isClientSide || !isEncounter(pokemon) || !(pokemon.level instanceof ServerLevel level)) return;
        var data = pokemon.getPersistentData();
        long now = level.getGameTime();
        UUID owner = owner(pokemon);
        ServerPlayer player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
        if (isDowned(pokemon)) {
            if (now - data.getLong(DOWNED_AT) >= EXPIRY) rewardAndDiscard(level, pokemon, 25);
            return;
        }
        boolean inRange = player != null && player.level == level
                && Math.abs(player.getX() - pokemon.getX()) <= 75.0D
                && Math.abs(player.getZ() - pokemon.getZ()) <= 75.0D
                && Math.abs(player.getY() - pokemon.getY()) <= 15.0D;
        if (inRange) data.putLong(LAST_IN_RANGE, now);
        else if (now - data.getLong(LAST_IN_RANGE) >= EXPIRY) {
            WildEncounterData.get(level).remove(owner, pokemon.getUUID());
            pokemon.discard();
        }
    }

    /** Called only by the server when the owner uses an empty ball on their downed encounter. */
    public static CaptureResult attemptCapture(MinemonCreature pokemon, ServerPlayer player) {
        if (!isDowned(pokemon) || !player.getUUID().equals(owner(pokemon))
                || player.distanceToSqr(pokemon) > 36.0D) return CaptureResult.INVALID;
        var data = pokemon.getPersistentData();
        int attempts = Math.max(0, data.getInt("MinemonWildCaptureAttempts"));
        if (attempts >= WildCaptureRules.MAX_ATTEMPTS) return CaptureResult.INVALID;
        int next = attempts + 1;
        data.putInt("MinemonWildCaptureAttempts", next);
        double chance = WildCaptureRules.chance(pokemon.getPokemonRarity(), data.getInt("MinemonWildTreats"));
        if (player.getRandom().nextDouble() < chance) {
            double captureXp = TrainerProgression.pokemonXp(pokemon.getSpeciesSlug(), pokemon.getPokemonLevel()) * .25D;
            player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(br.com.petrickfaria.minemon.registry.ModItems.POKEBALL.get()));
            pokemon.tame(player);
            pokemon.setEncounterDowned(false);
            pokemon.setHealth(pokemon.getMaxHealth());
            data.remove(MARKER); data.remove(OWNER); data.remove(STATE); data.remove(DOWNED_AT); data.remove(REWARDED);
            return CaptureResult.CAPTURED;
        }
        if (next >= WildCaptureRules.MAX_ATTEMPTS) {
            rewardAndDiscard((ServerLevel) pokemon.level, pokemon, 50);
            return CaptureResult.EXPIRED;
        }
        return CaptureResult.FAILED;
    }

    public static void finishCapture(MinemonCreature pokemon, ServerPlayer player) {
        PokecPlayerData.addTrainerXp(player, TrainerProgression.pokemonXp(pokemon.getSpeciesSlug(), pokemon.getPokemonLevel()) * .25D);
        var data = pokemon.getPersistentData();
        data.remove("MinemonWildCaptureAttempts"); data.remove("MinemonWildTreats");
        WildEncounterData.get((ServerLevel) pokemon.level).remove(player.getUUID(), pokemon.getUUID());
    }

    public static void restoreCaptureAfterStorageFailure(MinemonCreature pokemon, ServerPlayer player) {
        pokemon.setTame(false);
        pokemon.setOwnerUUID(null);
        pokemon.getPersistentData().putBoolean(MARKER, true);
        pokemon.getPersistentData().putUUID(OWNER, player.getUUID());
        pokemon.getPersistentData().putString(STATE, "downed");
        pokemon.setEncounterDowned(true);
    }

    public static boolean offerTreat(MinemonCreature pokemon, ServerPlayer player, net.minecraft.world.InteractionHand hand) {
        if (!isDowned(pokemon) || !player.getUUID().equals(owner(pokemon)) || player.distanceToSqr(pokemon) > 36.0D) return false;
        var stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof br.com.petrickfaria.minemon.item.PokemonTreatItem) || stack.isEmpty()) return false;
        var data = pokemon.getPersistentData();
        int count = Math.max(0, data.getInt("MinemonWildTreats"));
        if (count >= WildCaptureRules.MAX_TREATS) return false;
        data.putInt("MinemonWildTreats", count + 1);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return true;
    }

    public static boolean loot(MinemonCreature pokemon, ServerPlayer player) {
        if (!isDowned(pokemon) || !player.getUUID().equals(owner(pokemon)) || player.distanceToSqr(pokemon) > 36.0D) return false;
        rewardAndDiscard((ServerLevel) pokemon.level, pokemon, 75);
        return true;
    }

    private WildEncounterManager() { }
}
