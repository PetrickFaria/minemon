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
    private static final String COMPANION_HITS = "MinemonCompanionHitParticipants";
    private static final String LAST_COMPANION_HIT = "MinemonLastCompanionHit";
    private static final String COMPANION_HIT_TIMES = "MinemonCompanionHitTimes";
    public enum CaptureResult { INVALID, FAILED, EXPIRED, CAPTURED }
    private static final String MARKER = "MinemonWildEncounter";
    private static final String OWNER = "MinemonEncounterOwner";
    private static final String STATE = "MinemonEncounterState";
    private static final String DOWNED_AT = "MinemonEncounterDownedAt";
    private static final String LAST_IN_RANGE = "MinemonEncounterLastInRange";
    private static final String SPAWN_EGG_ENCOUNTER = "MinemonSpawnEggEncounter";
    private static final String REWARDED = "MinemonEncounterRewarded";
    private static final String LAST_CHECK = "MinemonWildSpawnCheck";
    private static final long CHECK_INTERVAL = 20L * 30L;
    private static final long EXPIRY = 20L * 60L * 5L;
    public static final int MAX_ACTIVE = 25, MAX_DOWNED = 10;
    private static final List<String> RARITIES = List.of("common", "uncommon", "rare", "legendary", "mythic");
    private static volatile java.util.Map<String, Double> rarityChances = java.util.Map.of(
            "common", .04D, "uncommon", .015D, "rare", .005D, "legendary", .001D, "mythic", .0003D);

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
                        "rarity.common=0.04", "rarity.uncommon=0.015", "rarity.rare=0.005",
                        "rarity.legendary=0.001", "rarity.mythic=0.0003");
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
    public static boolean isEncounter(MinemonCreature pokemon) {
        return pokemon.level.isClientSide ? pokemon.isWildEncounter() : pokemon.getPersistentData().getBoolean(MARKER);
    }
    public static boolean isDowned(MinemonCreature pokemon) {
        return isEncounter(pokemon) && (pokemon.level.isClientSide ? pokemon.isWildEncounterDowned()
                : "downed".equals(pokemon.getPersistentData().getString(STATE)));
    }
    public static UUID owner(MinemonCreature pokemon) {
        var data = pokemon.getPersistentData();
        return data.hasUUID(OWNER) ? data.getUUID(OWNER) : null;
    }

    public static void trackLoaded(MinemonCreature pokemon) {
        if (!isEncounter(pokemon) || !(pokemon.level instanceof ServerLevel level)) return;
        UUID owner = owner(pokemon);
        pokemon.setWildEncounter(true);
        pokemon.setWildEncounterOwner(owner);
        pokemon.setWildEncounterDowned(isDowned(pokemon));
        if (isDowned(pokemon)) pokemon.setEncounterDowned(true);
        if (owner != null) WildEncounterData.get(level).put(owner, pokemon.getUUID(), isDowned(pokemon) ? "downed"
                : pokemon.getPersistentData().getBoolean(SPAWN_EGG_ENCOUNTER) ? "spawn_egg" : "active");
    }

    /** Spawn-egg Pokémon enter the same wild defeat/capture lifecycle without being tamed or slot-counted. */
    public static void registerSpawnEggEncounter(MinemonCreature pokemon, ServerPlayer encounterPlayer) {
        if (pokemon.level.isClientSide || !(pokemon.level instanceof ServerLevel level) || pokemon.isTame()
                || encounterPlayer.level != level) return;
        var data = pokemon.getPersistentData();
        if (data.getBoolean(MARKER)) return;
        data.putBoolean(MARKER, true);
        data.putBoolean(SPAWN_EGG_ENCOUNTER, true);
        data.putUUID(OWNER, encounterPlayer.getUUID());
        data.putString(STATE, "spawn_egg");
        data.putLong(LAST_IN_RANGE, level.getGameTime());
        pokemon.setWildEncounter(true);
        pokemon.setWildEncounterOwner(encounterPlayer.getUUID());
        pokemon.setWildEncounterDowned(false);
        WildEncounterData.get(level).put(encounterPlayer.getUUID(), pokemon.getUUID(), "spawn_egg");
    }

    public static void untrackRemoved(MinemonCreature pokemon) {
        if (isEncounter(pokemon) && pokemon.level instanceof ServerLevel level)
            WildEncounterData.get(level).remove(owner(pokemon), pokemon.getUUID());
    }

    public static double configuredSpawnChance(ServerPlayer player,String slug) {
        if(player.level instanceof ServerLevel level&&!configLoaded)loadConfig(level);
        var species=PokecSpecies.get(slug);if(species==null)return 0;
        return speciesChances.getOrDefault(slug,rarityChances.getOrDefault(PokecEconomy.resolveRarity(species.rarity(),"common"),.05D));
    }
    public static void tickPlayer(ServerPlayer player) {
        if (!(player.level instanceof ServerLevel level)) return;
        if (!configLoaded) loadConfig(level);
        long now = level.getGameTime();
        WildEncounterData encounterData = WildEncounterData.get(level);
        int active = encounterData.count(player.getUUID(), "active");
        var pd = player.getPersistentData();
        List<PokecSpecies.Entry> candidates = PokecPlayerData.unlocked(player).stream()
                .map(PokecSpecies::get).filter(java.util.Objects::nonNull)
                .filter(entry -> ModEntities.SPECIES.containsKey(entry.slug()))
                .filter(entry -> PokecPlayerData.spawnEnabled(player,entry.slug()))
                .sorted(Comparator.comparingInt(PokecSpecies.Entry::number)).toList();
        // This method runs on each online player tick. Advance only species that
        // are currently unlocked and enabled in the normal spawn system.
        if (active < MAX_ACTIVE) {
            for (PokecSpecies.Entry candidate : candidates)
                SpawnChanceProgression.tick(pd, candidate.slug());
        }
        if (now - pd.getLong("MinemonWildSpawnAttempt") < CHECK_INTERVAL) return;
        pd.putLong("MinemonWildSpawnAttempt", now);
        if (active >= MAX_ACTIVE) return;
        if (candidates.isEmpty()) return;
        // One probability roll represents this 30-second check. Position retries below
        // only look for a safe place; they must not multiply the species spawn chance.
        PokecSpecies.Entry species = candidates.get(player.getRandom().nextInt(candidates.size()));
        double baseChance = configuredSpawnChance(player, species.slug());
        double chance = SpawnChanceProgression.chance(baseChance,
                SpawnChanceProgression.elapsedTicks(pd, species.slug()));
        if (player.getRandom().nextDouble() >= chance) return;
        for (int i = 0; i < 12; i++) {
            BlockPos pos = samplePosition(player, level, species);
            if (pos == null || !environmentAllows(species, level, pos)) continue;
            Entity created = ModEntities.SPECIES.get(species.slug()).get().create(level);
            if (!(created instanceof MinemonCreature pokemon)) { if (created != null) created.discard(); continue; }
            pokemon.moveTo(pos.getX() + .5D, pos.getY(), pos.getZ() + .5D, player.getRandom().nextFloat() * 360, 0);
            if (!level.noCollision(pokemon) || !safeEntityVolume(level, pokemon)) { pokemon.discard(); continue; }
            pokemon.setPokemonLevel(spawnLevel(species, player.getRandom()));
            var data = pokemon.getPersistentData();
            data.putBoolean(MARKER, true); data.putUUID(OWNER, player.getUUID()); data.putString(STATE, "active");
            pokemon.setWildEncounter(true);
            pokemon.setWildEncounterOwner(player.getUUID());
            data.putLong(LAST_CHECK, now);
            data.putLong(LAST_IN_RANGE, now);
            if (level.addFreshEntity(pokemon)) {
                encounterData.put(player.getUUID(), pokemon.getUUID(), "active");
                SpawnChanceProgression.reset(pd, species.slug());
            }
            else pokemon.discard();
            return;
        }
    }

    private static BlockPos samplePosition(ServerPlayer player, ServerLevel level, PokecSpecies.Entry species) {
        boolean aquatic = SpeciesBehaviorProfiles.waterCapable(species.slug());
        int x = player.blockPosition().getX() + player.getRandom().nextInt(151) - 75;
        int z = player.blockPosition().getZ() + player.getRandom().nextInt(151) - 75;
        int y = player.blockPosition().getY() + player.getRandom().nextInt(31) - 15;
        BlockPos pos = new BlockPos(x, y, z);
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) < 64 || !level.hasChunkAt(pos)) return null;
        if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) return null;
        if (level.getBlockState(pos).is(Blocks.LAVA) || level.getBlockState(pos).is(Blocks.FIRE)
                || level.getBlockState(pos).is(Blocks.SOUL_FIRE)) return null;
        if (aquatic ? !level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)
                : !level.getFluidState(pos).isEmpty()) return null;
        return pos;
    }

    private static boolean safeEntityVolume(ServerLevel level, MinemonCreature pokemon) {
        AABB box = pokemon.getBoundingBox().inflate(-0.01D);
        int minX = net.minecraft.util.Mth.floor(box.minX), maxX = net.minecraft.util.Mth.floor(box.maxX);
        int minY = net.minecraft.util.Mth.floor(box.minY), maxY = net.minecraft.util.Mth.floor(box.maxY);
        int minZ = net.minecraft.util.Mth.floor(box.minZ), maxZ = net.minecraft.util.Mth.floor(box.maxZ);
        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            if (!level.hasChunkAt(pos)) return false;
            if (level.getFluidState(pos).is(net.minecraft.tags.FluidTags.LAVA)
                    || level.getBlockState(pos).is(Blocks.FIRE) || level.getBlockState(pos).is(Blocks.SOUL_FIRE)) return false;
        }
        return true;
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
        int min = WildCaptureRules.minimumSpawnLevel(species.slug());
        int max = Math.max(min, Math.min(30, min + 15));
        return min + random.nextInt(max - min + 1);
    }

    public static boolean canDefeat(MinemonCreature wild, MinemonCreature attacker) {
        UUID owner = owner(wild);
        if (!isEncounter(wild) || isDowned(wild) || attacker == null || !attacker.isTame()
                || owner == null || !owner.equals(attacker.getOwnerUUID())) return false;
        ServerPlayer player = encounterPlayer(wild, owner);
        if (player == null) return false;
        return WildEncounterData.get((ServerLevel) wild.level).count(owner, "downed") < MAX_DOWNED;
    }

    public static MinemonCreature pokemonAttacker(net.minecraft.world.damagesource.DamageSource source) {
        if (source.getEntity() instanceof MinemonCreature pokemon) return pokemon;
        if (source.getEntity() instanceof br.com.petrickfaria.minemon.entity.ElementalProjectile projectile
                && projectile.getOwner() instanceof MinemonCreature pokemon) return pokemon;
        return null;
    }

    /** Remember only server-confirmed health loss, once per Pokémon and target. */
    public static void recordCompanionHit(net.minecraft.world.entity.LivingEntity target, MinemonCreature attacker) {
        if (target == null || attacker == null || target.level.isClientSide || !attacker.isTame()
                || attacker.isDefeated() || !attacker.isAlive()) return;
        var data = target.getPersistentData();
        net.minecraft.nbt.ListTag participants = data.getList(COMPANION_HITS, net.minecraft.nbt.Tag.TAG_STRING);
        String id = attacker.getUUID().toString();
        boolean found = false;
        for (int i = 0; i < participants.size(); i++) if (id.equals(participants.getString(i))) { found = true; break; }
        if (!found) participants.add(net.minecraft.nbt.StringTag.valueOf(id));
        data.put(COMPANION_HITS, participants);
        data.putString(LAST_COMPANION_HIT, id);
        net.minecraft.nbt.CompoundTag times = data.getCompound(COMPANION_HIT_TIMES);
        times.putLong(id, target.level.getGameTime());
        data.put(COMPANION_HIT_TIMES, times);
    }

    /** Awards one defeat reward to the latest valid hitter and a participation share to the others. */
    public static void awardCompanionExperience(net.minecraft.world.entity.Mob defeated,
                                                 net.minecraft.world.damagesource.DamageSource source) {
        if (defeated.level.isClientSide) return;
        var data = defeated.getPersistentData();
        if (data.getBoolean("MinemonCompanionXpHandled")) return;
        net.minecraft.nbt.ListTag participants = data.getList(COMPANION_HITS, net.minecraft.nbt.Tag.TAG_STRING);
        net.minecraft.nbt.CompoundTag hitTimes = data.getCompound(COMPANION_HIT_TIMES);
        long now = defeated.level.getGameTime();
        MinemonCreature finalHitter = null;
        String lastId = data.getString(LAST_COMPANION_HIT);
        if (defeated.level instanceof ServerLevel serverLevel && !lastId.isEmpty()) {
            try {
                var entity = serverLevel.getEntity(UUID.fromString(lastId));
                if (entity instanceof MinemonCreature pokemon && pokemon.isTame() && !pokemon.isDefeated()
                        && recentHit(hitTimes, lastId, now)) finalHitter = pokemon;
            } catch (IllegalArgumentException ignored) { }
        }
        // Legacy/direct death callers may not have passed through a projectile (for example
        // an older saved encounter); retain their real final hitter as a one-participant case.
        if (participants.isEmpty()) {
            MinemonCreature legacy = pokemonAttacker(source);
            if (legacy != null && legacy.isTame() && !legacy.isDefeated()) {
                recordCompanionHit(defeated, legacy);
                participants = data.getList(COMPANION_HITS, net.minecraft.nbt.Tag.TAG_STRING);
                // getCompound returns a detached empty tag when the key did not exist.
                // recordCompanionHit inserts a new tag into entity data, so refresh this
                // snapshot before validating timestamps and sharing the reward.
                hitTimes = data.getCompound(COMPANION_HIT_TIMES);
                finalHitter = legacy;
            }
        }
        if (participants.isEmpty()) return;
        int rewardHalfUnits = defeated instanceof MinemonCreature wildPokemon
                ? br.com.petrickfaria.minemon.entity.PokemonProgression.xpRewardHalfUnitsForWildRarity(
                        wildPokemon.getPokemonRarity(), br.com.petrickfaria.minemon.config.MinemonConfig.POKEMON_XP_PER_ZOMBIE.get())
                : (int)Math.min(Integer.MAX_VALUE, (long)Math.max(0, defeated.getExperienceReward()) * 2L);
        data.putBoolean("MinemonCompanionXpHandled", true);
        int totalAwarded = 0;
        UUID trainerId = finalHitter == null ? null : finalHitter.getOwnerUUID();
        for (int i = 0; i < participants.size(); i++) {
            try {
                UUID id = UUID.fromString(participants.getString(i));
                if (!recentHit(hitTimes, id.toString(), now)) continue;
                var entity = defeated.level instanceof ServerLevel serverLevel ? serverLevel.getEntity(id) : null;
                if (!(entity instanceof MinemonCreature participant) || !participant.isTame() || participant.isDefeated()) continue;
                int share = participant == finalHitter ? rewardHalfUnits : rewardHalfUnits / 10;
                if (share > 0) totalAwarded += participant.awardPokemonExperienceHalfUnits(share);
                if (participant == finalHitter) trainerId = participant.getOwnerUUID();
            } catch (IllegalArgumentException ignored) { }
        }
        double trainerShare = TrainerProgression.trainerShareForCompanionXpHalfUnits(totalAwarded);
        data.remove(COMPANION_HITS);
        data.remove(LAST_COMPANION_HIT);
        data.remove(COMPANION_HIT_TIMES);
        if (trainerShare > 0.0D && trainerId != null && defeated.level instanceof ServerLevel serverLevel) {
            UUID awardedTrainerId = trainerId;
            ServerPlayer trainer = serverLevel.getServer().getPlayerList().getPlayer(awardedTrainerId);
            if (trainer == null) trainer = serverLevel.players().stream()
                    .filter(candidate -> awardedTrainerId.equals(candidate.getUUID())).findFirst().orElse(null);
            if (trainer != null) PokecPlayerData.addTrainerXp(trainer, trainerShare);
            else TrainerXpLedger.queue(serverLevel, awardedTrainerId, trainerShare);
        }
    }

    private static boolean recentHit(net.minecraft.nbt.CompoundTag times, String id, long now) {
        if (id == null || id.isEmpty() || !times.contains(id)) return false;
        long last = times.getLong(id);
        return now >= last && now - last <= 600L;
    }

    public static boolean canDamage(MinemonCreature wild, net.minecraft.world.damagesource.DamageSource source) {
        MinemonCreature attacker = pokemonAttacker(source);
        return attacker != null && canDefeat(wild, attacker);
    }

    public static boolean hasDefeatedCapacity(MinemonCreature wild) {
        UUID owner = owner(wild);
        if (owner == null || !(wild.level instanceof ServerLevel level)) return false;
        ServerPlayer player = encounterPlayer(wild, owner);
        return player != null && WildEncounterData.get(level).count(owner, "downed") < MAX_DOWNED;
    }

    /** Find an owner in the player list, with a loaded-level fallback for in-world test transports. */
    private static ServerPlayer encounterPlayer(MinemonCreature wild, UUID owner) {
        if (!(wild.level instanceof ServerLevel level) || owner == null) return null;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (player != null) return player;
        return level.getEntitiesOfClass(ServerPlayer.class, wild.getBoundingBox().inflate(128.0D),
                candidate -> owner.equals(candidate.getUUID())).stream().findFirst().orElse(null);
    }

    public static boolean enterDowned(MinemonCreature wild, net.minecraft.world.damagesource.DamageSource source) {
        MinemonCreature attacker = pokemonAttacker(source);
        if (!canDefeat(wild, attacker)) return false;
        var data = wild.getPersistentData();
        data.putString(STATE, "downed"); data.putLong(DOWNED_AT, wild.level.getGameTime());
        data.putInt("MinemonWildCaptureAttempts", 0); data.putInt("MinemonWildTreats", 0);
        WildEncounterData.get((ServerLevel) wild.level).put(owner(wild), wild.getUUID(), "downed");
        wild.setHealth(1.0F); wild.setEncounterDowned(true);
        awardCompanionExperience(wild, source);
        return true;
    }

    public static void rewardAndDiscard(ServerLevel level, MinemonCreature pokemon, int percent) {
        var data = pokemon.getPersistentData();
        if (!isEncounter(pokemon) || data.getBoolean(REWARDED)) { pokemon.discard(); return; }
        data.putBoolean(REWARDED, true);
        WildEncounterData.get(level).remove(owner(pokemon), pokemon.getUUID());
        var reward = rewardStack(pokemon, percent);
        if (!reward.isEmpty()) level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level,
                pokemon.getX(), pokemon.getY(), pokemon.getZ(), reward));
        pokemon.discard();
    }

    private static net.minecraft.world.item.ItemStack rewardStack(MinemonCreature pokemon, int percent) {
        var preview = PokemonSoulConversion.preview(pokemon.getSpeciesSlug(), pokemon.getPokemonLevel());
        if (preview == null || preview.soulType() == null) return net.minecraft.world.item.ItemStack.EMPTY;
        int amount = WildCaptureRules.souls(preview.base(), percent);
        var item = br.com.petrickfaria.minemon.registry.ModItems.SOULS.get(preview.soulType().id());
        return item == null || amount <= 0 ? net.minecraft.world.item.ItemStack.EMPTY
                : new net.minecraft.world.item.ItemStack(item.get(), amount);
    }

    /** Encounter lifecycle is driven by the loaded entity, so no chunks are force-loaded. */
    public static void tickEncounter(MinemonCreature pokemon) {
        if (pokemon.level.isClientSide || !isEncounter(pokemon) || !(pokemon.level instanceof ServerLevel level)) return;
        var data = pokemon.getPersistentData();
        long now = level.getGameTime();
        UUID owner = owner(pokemon);
        ServerPlayer player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
        if (isDowned(pokemon)) {
            if (data.getBoolean("MinemonCapturePending")) return;
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
                || player.distanceToSqr(pokemon) > 36.0D
                || pokemon.getPersistentData().getBoolean("MinemonCapturePending")) return CaptureResult.INVALID;
        var data = pokemon.getPersistentData();
        int attempts = Math.max(0, data.getInt("MinemonWildCaptureAttempts"));
        if (attempts >= WildCaptureRules.MAX_ATTEMPTS) return CaptureResult.INVALID;
        int next = attempts + 1;
        data.putInt("MinemonWildCaptureAttempts", next);
        double chance = WildCaptureRules.chance(pokemon.getPokemonRarity(), data.getInt("MinemonWildTreats"));
        if (player.getRandom().nextDouble() < chance) return CaptureResult.CAPTURED;
        if (next >= WildCaptureRules.MAX_ATTEMPTS) {
            return CaptureResult.EXPIRED;
        }
        return CaptureResult.FAILED;
    }

    /** Commits a previously rolled capture only when its world animation has finished. */
    public static boolean completeCapture(MinemonCreature pokemon, ServerPlayer player) {
        if (!isDowned(pokemon) || !pokemon.getPersistentData().getBoolean("MinemonCapturePending")
                || !player.getUUID().equals(owner(pokemon)) || player.level != pokemon.level) return false;
        var data = pokemon.getPersistentData();
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(br.com.petrickfaria.minemon.registry.ModItems.POKEBALL.get()));
        pokemon.tame(player);
        pokemon.setEncounterDowned(false);
        pokemon.setWildEncounter(false);
        pokemon.setWildEncounterOwner(null);
        pokemon.setHealth(pokemon.getMaxHealth());
        data.remove("MinemonCapturePending");
        data.remove(MARKER); data.remove(OWNER); data.remove(STATE); data.remove(DOWNED_AT); data.remove(REWARDED);
        return true;
    }

    public static void markCapturePending(MinemonCreature pokemon) {
        pokemon.getPersistentData().putBoolean("MinemonCapturePending", true);
    }

    public static void abortCaptureAttempt(MinemonCreature pokemon) {
        var data=pokemon.getPersistentData();
        data.remove("MinemonCapturePending");
        data.putInt("MinemonWildCaptureAttempts",Math.max(0,data.getInt("MinemonWildCaptureAttempts")-1));
    }

    public static void cancelCapturePending(MinemonCreature pokemon, boolean expired) {
        pokemon.getPersistentData().remove("MinemonCapturePending");
        if (expired && pokemon.level instanceof ServerLevel level) rewardAndDiscard(level, pokemon, 50);
    }

    public static void finishCapture(MinemonCreature pokemon, ServerPlayer player) {
        PokecPlayerData.addTrainerXp(player,
                TrainerProgression.pokemonXp(pokemon.getSpeciesSlug(), pokemon.getPokemonLevel()) * 0.25D);
        var data = pokemon.getPersistentData();
        data.remove("MinemonWildCaptureAttempts"); data.remove("MinemonWildTreats");
        WildEncounterData.get((ServerLevel) pokemon.level).remove(player.getUUID(), pokemon.getUUID());
    }

    public static void restoreCaptureAfterStorageFailure(MinemonCreature pokemon, ServerPlayer player) {
        var data = pokemon.getPersistentData();
        data.remove("MinemonCapturePending");
        // The failed inventory transaction consumed neither a Pokébola nor an
        // attempt; return the encounter to the exact capture state it had before.
        data.putInt("MinemonWildCaptureAttempts", Math.max(0,
                data.getInt("MinemonWildCaptureAttempts") - 1));
        pokemon.setTame(false);
        pokemon.setOwnerUUID(null);
        data.putBoolean(MARKER, true);
        data.putUUID(OWNER, player.getUUID());
        data.putString(STATE, "downed");
        pokemon.setEncounterDowned(true);
    }

    public static boolean offerTreat(MinemonCreature pokemon, ServerPlayer player, net.minecraft.world.InteractionHand hand) {
        if (!isDowned(pokemon) || pokemon.getPersistentData().getBoolean("MinemonCapturePending")
                || !player.getUUID().equals(owner(pokemon)) || player.distanceToSqr(pokemon) > 36.0D) return false;
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
        if (!isDowned(pokemon) || pokemon.getPersistentData().getBoolean("MinemonCapturePending")
                || !player.getUUID().equals(owner(pokemon)) || player.distanceToSqr(pokemon) > 36.0D) return false;
        var data = pokemon.getPersistentData();
        if (data.getBoolean(REWARDED)) return false;
        data.putBoolean(REWARDED, true);
        WildEncounterData.get((ServerLevel) pokemon.level).remove(owner(pokemon), pokemon.getUUID());
        var reward = rewardStack(pokemon, 75);
        if (!reward.isEmpty() && !player.getInventory().add(reward)) {
            // A full inventory still receives the reward as a protected world drop.
            player.drop(reward, false);
        }
        pokemon.discard();
        return true;
    }

    private WildEncounterManager() { }
}
