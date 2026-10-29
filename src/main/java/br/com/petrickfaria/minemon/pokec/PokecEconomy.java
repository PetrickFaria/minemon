package br.com.petrickfaria.minemon.pokec;

import net.minecraftforge.fml.loading.FMLPaths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Reloadable PokéC economy and soul-yield settings in config/minemon/pokec-economy.toml. */
public final class PokecEconomy {
    private static final Map<String,Integer> DEFAULT_COINS = Map.of(
            "common",10,"uncommon",20,"rare",30,"legendary",40,"mythic",50);
    private static final Map<String,Integer> DEFAULT_UNLOCK_SOULS = Map.of(
            "common",25,"uncommon",50,"rare",75,"legendary",100,"mythic",150);
    private static final Map<String,Integer> DEFAULT_RARITY_SOULS = Map.of(
            "common",1,"uncommon",2,"rare",3,"legendary",4,"mythic",5);
    public static final String[] STARTERS = {"bulbasaur","charmander","squirtle","pikachu","eevee"};
    private static volatile PokecEconomy active = defaults();

    private final Map<String,Integer> rarityCoins, raritySouls, rarityYieldSouls, speciesCoins, speciesSouls;
    private final Map<String,String> speciesRarities;
    private final Set<String> starters;
    private final boolean firstPurchaseFree;
    private final int initialStageSouls, intermediateStageSouls, finalStageSouls;
    private final int[] levelSouls;
    private final int deathMinPercent, deathMaxPercent, releaseMinPercent, releaseMaxPercent;

    private PokecEconomy(Map<String,Integer> coins, Map<String,Integer> unlockSouls, Map<String,Integer> rarityYieldSouls,
                         Map<String,Integer> speciesCoinOverrides, Map<String,Integer> speciesSoulOverrides,
                         Map<String,String> rarities, Set<String> starters, boolean firstPurchaseFree,
                         int initial, int intermediate, int terminal, int[] levels,
                         int deathMin, int deathMax, int releaseMin, int releaseMax) {
        this.rarityCoins=Map.copyOf(coins); this.raritySouls=Map.copyOf(unlockSouls);
        this.rarityYieldSouls=Map.copyOf(rarityYieldSouls); this.speciesCoins=Map.copyOf(speciesCoinOverrides);
        this.speciesSouls=Map.copyOf(speciesSoulOverrides); this.speciesRarities=Map.copyOf(rarities);
        this.starters=Set.copyOf(starters); this.firstPurchaseFree=firstPurchaseFree;
        this.initialStageSouls=initial; this.intermediateStageSouls=intermediate; this.finalStageSouls=terminal;
        this.levelSouls=levels.clone(); this.deathMinPercent=deathMin; this.deathMaxPercent=deathMax;
        this.releaseMinPercent=releaseMin; this.releaseMaxPercent=releaseMax;
    }
    private static PokecEconomy defaults() {
        return new PokecEconomy(DEFAULT_COINS,DEFAULT_UNLOCK_SOULS,DEFAULT_RARITY_SOULS,Map.of(),Map.of(),Map.of(),
                Set.of(STARTERS),true,1,2,3,new int[]{1,2,3,4,5},50,100,75,100);
    }
    public static PokecEconomy get(){return active;}
    public int coins(String slug){return Math.max(0,speciesCoins.getOrDefault(slug,rarityCoins.getOrDefault(rarity(slug),10)));}
    /** Compatibility helper retained for callers in older tests and integrations. */
    static int enforceFixedCoinPrice(int configured){return Math.max(0,configured);}
    public int souls(String slug){return Math.max(0,speciesSouls.getOrDefault(slug,raritySouls.getOrDefault(rarity(slug),25)));}
    public boolean isStarter(String slug){return starters.contains(slug);}
    public Set<String> starters(){return starters;}
    public boolean firstPurchaseFree(){return firstPurchaseFree;}
    public String rarity(String slug){var species=PokecSpecies.get(slug);return speciesRarities.getOrDefault(slug,species==null?"common":species.rarity());}
    public int stageSouls(PokemonSoulConversion.Stage stage){return switch(stage){case INITIAL->initialStageSouls;case INTERMEDIATE->intermediateStageSouls;case FINAL->finalStageSouls;};}
    public int raritySouls(String rarity){return rarityYieldSouls.getOrDefault(resolveRarity(rarity,"common"),1);}
    public int levelSouls(int level){return levelSouls[Math.min(4,Math.max(1,(Math.max(1,level)+1)/2)-1)];}
    public int deathMinPercent(){return deathMinPercent;} public int deathMaxPercent(){return deathMaxPercent;}
    public int releaseMinPercent(){return releaseMinPercent;} public int releaseMaxPercent(){return releaseMaxPercent;}

    static String resolveRarity(String configured,String fallback){
        String value=(configured==null?fallback:configured).strip().toLowerCase(Locale.ROOT);
        // Legacy labels are migrated explicitly to the closest supported tier; Very Rare remains Rare.
        if("very_rare".equals(value))return "rare";
        if("mythical".equals(value)||"legendary_mythical".equals(value))return "mythic";
        return DEFAULT_COINS.containsKey(value)?value:(DEFAULT_COINS.containsKey(fallback)?fallback:"common");
    }

    public static synchronized void load(){
        Path file=FMLPaths.CONFIGDIR.get().resolve("minemon/pokec-economy.toml");
        try{
            Files.createDirectories(file.getParent()); if(!Files.exists(file))writeDefaults(file);
            List<String> lines=Files.readAllLines(file,StandardCharsets.UTF_8); Map<String,String> values=parse(lines);
            boolean migrateEconomy=!"4".equals(values.get("economyVersion"));
            boolean migrateRarities=!"3".equals(values.get("rarityVersion"))
                    || values.entrySet().stream().anyMatch(entry -> entry.getKey().startsWith("species.")
                    && entry.getKey().endsWith(".rarity") && !entry.getValue().equals(resolveRarity(entry.getValue(),"common")));
            Map<String,Integer> coins=new HashMap<>(DEFAULT_COINS), unlockSouls=new HashMap<>(DEFAULT_UNLOCK_SOULS), rarityYield=new HashMap<>(DEFAULT_RARITY_SOULS);
            for(String r:DEFAULT_COINS.keySet()){
                int configuredCoins=validInt(values.get("rarity."+r+".coins"),DEFAULT_COINS.get(r),0,1000000);
                coins.put(r,migrateEconomy&&configuredCoins==5?DEFAULT_COINS.get(r):configuredCoins);
                int configuredSouls=validInt(values.get("rarity."+r+".souls"),DEFAULT_UNLOCK_SOULS.get(r),0,1000000);
                int legacyDefault=switch(r){case "common"->8;case "uncommon"->16;case "rare"->32;case "legendary","mythic"->128;default->-1;};
                unlockSouls.put(r,migrateEconomy&&configuredSouls==legacyDefault?DEFAULT_UNLOCK_SOULS.get(r):configuredSouls);
                rarityYield.put(r,validInt(values.get("yield.rarity."+r+".souls"),DEFAULT_RARITY_SOULS.get(r),0,1000));
            }
            Set<String> starters=new HashSet<>(Arrays.asList(STARTERS));
            if(values.containsKey("starters")){starters.clear();for(String slug:values.get("starters").replace("[","").replace("]","").split(",")){slug=slug.strip().toLowerCase(Locale.ROOT);if(PokecSpecies.get(slug)!=null)starters.add(slug);}if(starters.isEmpty())starters.addAll(Arrays.asList(STARTERS));}
            Map<String,Integer> speciesCoins=new HashMap<>(),speciesSouls=new HashMap<>();Map<String,String> rarities=new HashMap<>();
            for(var species:PokecSpecies.ALL.values()){
                String prefix="species."+species.slug()+"."; String configured=values.get(prefix+"rarity");
                String rarity=resolveRarity(configured,species.rarity());
                // The old catalog marked Mew as Legendary. Explicitly classify it as Mythic on migration.
                if(migrateRarities&&"mew".equals(species.slug())&&("legendary".equals(configured)||"very_rare".equals(configured)||configured==null))rarity="mythic";
                rarities.put(species.slug(),rarity);
                String rawCoin=values.get(prefix+"coins");
                int defaultPrice=coins.get(rarity);
                if(rawCoin!=null){int old=validInt(rawCoin,defaultPrice,0,1000000);speciesCoins.put(species.slug(),migrateEconomy&&old==5?defaultPrice:old);}
                String rawSouls=values.get(prefix+"souls");
                if(rawSouls!=null)speciesSouls.put(species.slug(),validInt(rawSouls,unlockSouls.get(rarity),0,1000000));
            }
            int initial=validInt(values.get("yield.stage.initial"),1,0,1000), intermediate=validInt(values.get("yield.stage.intermediate"),2,0,1000), terminal=validInt(values.get("yield.stage.final"),3,0,1000);
            int[] levels={validInt(values.get("yield.level.1-2"),1,0,1000),validInt(values.get("yield.level.3-4"),2,0,1000),validInt(values.get("yield.level.5-6"),3,0,1000),validInt(values.get("yield.level.7-8"),4,0,1000),validInt(values.get("yield.level.9-10"),5,0,1000)};
            int deathMin=validPercent(values.get("yield.death.minimumPercent"),50),deathMax=validPercent(values.get("yield.death.maximumPercent"),100);
            int releaseMin=validPercent(values.get("yield.release.minimumPercent"),75),releaseMax=validPercent(values.get("yield.release.maximumPercent"),100);
            if(deathMin>deathMax){deathMin=50;deathMax=100;} if(releaseMin>releaseMax){releaseMin=75;releaseMax=100;}
            boolean free=validBoolean(values.get("firstPurchaseFree"),true);
            active=new PokecEconomy(coins,unlockSouls,rarityYield,speciesCoins,speciesSouls,rarities,starters,free,initial,intermediate,terminal,levels,deathMin,deathMax,releaseMin,releaseMax);
            if(migrateEconomy||migrateRarities||!values.containsKey("yield.stage.initial"))writeMigrated(file,lines,values,migrateEconomy,migrateRarities,rarities,coins);
        }catch(Exception e){System.err.println("[Minemon] PokéC config load failed; safe defaults remain active: "+e);}
    }
    private static Map<String,String> parse(List<String> lines){Map<String,String> out=new HashMap<>();for(String line:lines){String s=line.strip();if(s.isEmpty()||s.startsWith("#"))continue;int at=s.indexOf('=');if(at>0)out.put(s.substring(0,at).strip(),s.substring(at+1).split("#",2)[0].strip().replace("\"",""));}return out;}
    private static int validInt(String raw,int fallback,int min,int max){try{int n=Integer.parseInt(raw);return n<min||n>max?fallback:n;}catch(Exception ignored){return fallback;}}
    private static int validPercent(String raw,int fallback){return validInt(raw,fallback,0,100);}
    private static boolean validBoolean(String raw,boolean fallback){return "true".equalsIgnoreCase(raw)?true:"false".equalsIgnoreCase(raw)?false:fallback;}
    private static void writeDefaults(Path file)throws IOException{List<String> lines=new ArrayList<>();lines.add("# PokéC economy and soul yield. Edits apply after /pokec reload.");lines.add("economyVersion = 4");lines.add("rarityVersion = 3");lines.add("firstPurchaseFree = true");lines.add("starters = [\"bulbasaur\", \"charmander\", \"squirtle\", \"pikachu\", \"eevee\"]");lines.add("");appendDefaults(lines);Files.write(file,lines,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);}
    private static void appendDefaults(List<String> lines){
        lines.add("# Permanent species unlock costs and PokéCoin purchase prices by rarity.");
        for(String r:List.of("common","uncommon","rare","legendary","mythic")){lines.add("rarity."+r+".souls = "+DEFAULT_UNLOCK_SOULS.get(r));lines.add("rarity."+r+".coins = "+DEFAULT_COINS.get(r));lines.add("yield.rarity."+r+".souls = "+DEFAULT_RARITY_SOULS.get(r));}
        lines.add("");lines.add("# Soul base components; result defaults to 3–13.");lines.add("yield.stage.initial = 1");lines.add("yield.stage.intermediate = 2");lines.add("yield.stage.final = 3");
        lines.add("yield.level.1-2 = 1");lines.add("yield.level.3-4 = 2");lines.add("yield.level.5-6 = 3");lines.add("yield.level.7-8 = 4");lines.add("yield.level.9-10 = 5");
        lines.add("yield.death.minimumPercent = 50");lines.add("yield.death.maximumPercent = 100");lines.add("yield.release.minimumPercent = 75");lines.add("yield.release.maximumPercent = 100");
        lines.add("");lines.add("# Species rarity is explicit and can be customized per species.");
        for(var s:PokecSpecies.ordered())lines.add("species."+s.slug()+".rarity = \""+s.rarity()+"\"");
    }
    private static void writeMigrated(Path file,List<String> old,Map<String,String> values,boolean migrateEconomy,boolean migrateRarities,Map<String,String> rarities,Map<String,Integer> coins)throws IOException{
        List<String> out=new ArrayList<>();Set<String> written=new HashSet<>();
        for(String line:old){String t=line.strip();if(t.startsWith("economyVersion")||t.startsWith("rarityVersion"))continue;if(t.startsWith("#")){out.add(line);continue;}int at=line.indexOf('=');if(at<1){out.add(line);continue;}String key=line.substring(0,at).strip();String value=line.substring(at+1).split("#",2)[0].strip();
            if(migrateRarities&&key.startsWith("species.")&&key.endsWith(".rarity")){String slug=key.substring(8,key.length()-7);out.add(key+" = \""+rarities.getOrDefault(slug,"common")+"\"");}
            else if(migrateEconomy&&key.endsWith(".coins")&&"5".equals(value)){String rarity="common";if(key.startsWith("species.")){String slug=key.substring(8,key.length()-6);rarity=rarities.getOrDefault(slug,"common");}else if(key.startsWith("rarity."))rarity=key.substring(7,key.length()-6);out.add(key+" = "+coins.getOrDefault(rarity,10));}
            else if(migrateEconomy&&key.startsWith("rarity.")&&key.endsWith(".souls")){String rarity=key.substring(7,key.length()-6);int legacy=switch(rarity){case "common"->8;case "uncommon"->16;case "rare"->32;case "legendary","mythic"->128;default->-1;};if(validInt(value,-2,0,1000000)==legacy)out.add(key+" = "+DEFAULT_UNLOCK_SOULS.getOrDefault(rarity,25));else out.add(line);}
            else out.add(line);written.add(key);
        }
        out.add(0,"economyVersion = 4");out.add(1,"rarityVersion = 3");
        List<String> defaults=new ArrayList<>();appendDefaults(defaults);for(String line:defaults){int at=line.indexOf('=');if(at>0){String key=line.substring(0,at).strip();if(!written.contains(key)){out.add(line);written.add(key);}}else if(!line.isBlank())out.add(line);}
        Files.write(file,out,StandardCharsets.UTF_8,StandardOpenOption.WRITE,StandardOpenOption.TRUNCATE_EXISTING);
    }
    private PokecEconomy() { this(DEFAULT_COINS,DEFAULT_UNLOCK_SOULS,DEFAULT_RARITY_SOULS,Map.of(),Map.of(),Map.of(),Set.of(STARTERS),true,1,2,3,new int[]{1,2,3,4,5},50,100,75,100); }
}
