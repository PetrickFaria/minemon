package br.com.petrickfaria.minemon.pokec;

import net.minecraftforge.fml.loading.FMLPaths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Reloadable flat-key TOML economy at config/minemon/pokec-economy.toml. */
public final class PokecEconomy {
    private static final Map<String,Integer> BASE_COINS = Map.of(
            "common", 5, "uncommon", 5, "rare", 5, "legendary", 5, "mythic", 5);
    private static final Map<String,Integer> BASE_SOULS = Map.of(
            "common", 8, "uncommon", 16, "rare", 32, "legendary", 128, "mythic", 128);
    public static final String[] STARTERS = {"bulbasaur", "charmander", "squirtle", "pikachu", "eevee"};
    private static volatile PokecEconomy active = new PokecEconomy(BASE_COINS, BASE_SOULS, Map.of(), Map.of(), Map.of(), Set.of(STARTERS), true);
    private final Map<String,Integer> rarityCoins, raritySouls, speciesCoins, speciesSouls;
    private final Map<String,String> speciesRarities;
    private final Set<String> starters;
    private final boolean firstPurchaseFree;

    private PokecEconomy(Map<String,Integer> coins, Map<String,Integer> souls, Map<String,Integer> speciesCoinOverrides,
                         Map<String,Integer> speciesSoulOverrides, Map<String,String> rarities,
                         Set<String> starters, boolean firstPurchaseFree) {
        this.rarityCoins = Map.copyOf(coins);
        this.raritySouls = Map.copyOf(souls);
        this.speciesCoins = Map.copyOf(speciesCoinOverrides);
        this.speciesSouls = Map.copyOf(speciesSoulOverrides);
        this.speciesRarities = Map.copyOf(rarities);
        this.starters = Set.copyOf(starters);
        this.firstPurchaseFree = firstPurchaseFree;
    }

    public static PokecEconomy get() { return active; }
    public int coins(String slug) {
        var species = PokecSpecies.get(slug);
        int configured = speciesCoins.getOrDefault(slug, rarityCoins.getOrDefault(rarity(slug), 5));
        return enforceFixedCoinPrice(configured);
    }
    static int enforceFixedCoinPrice(int configured) { return 5; }
    public int souls(String slug) {
        var species = PokecSpecies.get(slug);
        return Math.max(0, speciesSouls.getOrDefault(slug, raritySouls.getOrDefault(species == null ? "common" : rarity(slug), 8)));
    }
    public boolean isStarter(String slug) { return starters.contains(slug); }
    public Set<String> starters() { return starters; }
    public boolean firstPurchaseFree() { return firstPurchaseFree; }
    public String rarity(String slug) {
        var species = PokecSpecies.get(slug);
        return speciesRarities.getOrDefault(slug, species == null ? "common" : species.rarity());
    }

    static String resolveRarity(String configured, String fallback) {
        String value = configured == null ? fallback : configured;
        if ("very_rare".equals(value)) return "rare";
        if ("mythical".equals(value) || "legendary_mythical".equals(value)) return "mythic";
        return BASE_COINS.containsKey(value) ? value : ("very_rare".equals(fallback) ? "rare" : fallback);
    }

    public static synchronized void load() {
        Path file = FMLPaths.CONFIGDIR.get().resolve("minemon/pokec-economy.toml");
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) writeDefaults(file);
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            Map<String,String> values = parse(lines);
            boolean migratePrices = !"3".equals(values.get("economyVersion"))
                    || values.entrySet().stream().anyMatch(entry -> entry.getKey().endsWith(".coins") && !"5".equals(entry.getValue()));
            boolean migrateRarities = !"2".equals(values.get("rarityVersion"));
            Map<String,Integer> coins = new HashMap<>(BASE_COINS);
            Map<String,Integer> souls = new HashMap<>(BASE_SOULS);
            Map<String,Integer> speciesCoins = new HashMap<>(), speciesSouls = new HashMap<>();
            Map<String,String> rarities = new HashMap<>();

            for (String rarity : BASE_COINS.keySet()) {
                if (!migratePrices) coins.put(rarity, validInt(values.get("rarity." + rarity + ".coins"), 5));
                souls.put(rarity, validInt(values.get("rarity." + rarity + ".souls"), BASE_SOULS.get(rarity)));
            }

            Set<String> starters = new HashSet<>(Arrays.asList(STARTERS));
            String configuredStarters = values.get("starters");
            if (configuredStarters != null) {
                starters.clear();
                for (String slug : configuredStarters.replace("[", "").replace("]", "").split(",")) {
                    slug = slug.strip().toLowerCase(Locale.ROOT);
                    if (PokecSpecies.get(slug) != null) starters.add(slug);
                }
                if (starters.isEmpty()) starters.addAll(Arrays.asList(STARTERS));
            }

            for (var species : PokecSpecies.ALL.values()) {
                String prefix = "species." + species.slug() + ".";
                String configuredRarity = values.get(prefix + "rarity");
                String rarity = resolveRarity(configuredRarity, species.rarity());
                if (migrateRarities && "mew".equals(species.slug())
                        && ("legendary".equals(configuredRarity) || "very_rare".equals(configuredRarity))) rarity = "mythic";
                rarities.put(species.slug(), rarity);
                if (!migratePrices && values.containsKey(prefix + "coins"))
                    speciesCoins.put(species.slug(), validInt(values.get(prefix + "coins"), 5));
                if (values.containsKey(prefix + "souls"))
                    speciesSouls.put(species.slug(), validInt(values.get(prefix + "souls"), souls.get(rarity)));
                if (migrateRarities && "very_rare".equals(configuredRarity))
                    speciesSouls.putIfAbsent(species.slug(), validInt(values.get(prefix + "souls"), 64));
                if (!rarity.equals(species.rarity())) speciesSouls.putIfAbsent(species.slug(), souls.get(rarity));
            }

            if (migratePrices || migrateRarities) {
                List<String> migrated = new ArrayList<>();
                migrated.add("# One-time migration: species prices are 5 PokéCoins; legacy rarity tiers are normalized.");
                migrated.add("economyVersion = 3");
                migrated.add("rarityVersion = 2");
                for (String line : lines) {
                    String trimmed = line.strip();
                    if (trimmed.startsWith("economyVersion") || trimmed.startsWith("rarityVersion")) continue;
                    if (trimmed.startsWith("#")) {
                        migrated.add(line);
                        continue;
                    }
                    int equals = line.indexOf('=');
                    if (equals <= 0) { migrated.add(line); continue; }
                    String key = line.substring(0, equals).strip();
                    if (migratePrices && key.endsWith(".coins")) migrated.add(key + " = 5");
                    else if (migrateRarities && key.startsWith("species.") && key.endsWith(".rarity")) {
                        String slug = key.substring("species.".length(), key.length() - ".rarity".length());
                        migrated.add(key + " = \"" + rarities.getOrDefault(slug, "common") + "\"");
                    } else migrated.add(line);
                }
                Set<String> present = new HashSet<>();
                for (String line : migrated) {
                    int equals = line.indexOf('=');
                    if (equals > 0) present.add(line.substring(0, equals).strip());
                }
                for (var species : PokecSpecies.ordered()) {
                    String coinKey = "species." + species.slug() + ".coins";
                    String rarityKey = "species." + species.slug() + ".rarity";
                    if (!present.contains(coinKey)) migrated.add(coinKey + " = 5");
                    if (!present.contains(rarityKey)) migrated.add(rarityKey + " = \"" + rarities.get(species.slug()) + "\"");
                }
                Files.write(file, migrated, StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            }

            active = new PokecEconomy(coins, souls, speciesCoins, speciesSouls, rarities, starters,
                    validBoolean(values.get("firstPurchaseFree"), true));
        } catch (Exception e) {
            System.err.println("[Minemon] PokéC config load failed; safe defaults remain active: " + e);
        }
    }

    private static Map<String,String> parse(List<String> lines) {
        Map<String,String> values = new HashMap<>();
        for (String line : lines) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            int at = trimmed.indexOf('=');
            if (at < 1) continue;
            values.put(trimmed.substring(0, at).strip(), trimmed.substring(at + 1).split("#", 2)[0].strip().replace("\"", ""));
        }
        return values;
    }

    private static int validInt(String raw, int fallback) {
        try { int value = Integer.parseInt(raw); return value < 0 ? fallback : value; }
        catch (Exception ignored) { return fallback; }
    }
    private static boolean validBoolean(String raw, boolean fallback) {
        if ("true".equalsIgnoreCase(raw)) return true;
        if ("false".equalsIgnoreCase(raw)) return false;
        return fallback;
    }

    private static void writeDefaults(Path file) throws IOException {
        StringBuilder out = new StringBuilder("# PokéC economy. Changes apply after /pokec reload.\neconomyVersion = 3\nrarityVersion = 2\nfirstPurchaseFree = true\n");
        out.append("starters = [\"bulbasaur\", \"charmander\", \"squirtle\", \"pikachu\", \"eevee\"]\n\n");
        for (String rarity : BASE_COINS.keySet())
            out.append("rarity.").append(rarity).append(".souls = ").append(BASE_SOULS.get(rarity))
                    .append("\nrarity.").append(rarity).append(".coins = 5\n");
        out.append("\n# Every Pokémon has a fixed price of exactly 5 PokéCoins.\n");
        for (var species : PokecSpecies.ordered())
            out.append("species.").append(species.slug()).append(".rarity = \"").append(species.rarity())
                    .append("\"\nspecies.").append(species.slug()).append(".coins = 5\n");
        Files.writeString(file, out, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }

    private PokecEconomy() { this(BASE_COINS, BASE_SOULS, Map.of(), Map.of(), Map.of(), Set.of(STARTERS), true); }
}
