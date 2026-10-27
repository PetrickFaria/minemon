package br.com.petrickfaria.minemon.pokec;

import net.minecraftforge.fml.loading.FMLPaths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Reloadable flat-key TOML economy at config/minemon/pokec-economy.toml. */
public final class PokecEconomy {
    private static final Map<String,Integer> BASE_COINS=Map.of("common",100,"uncommon",250,"rare",600,"very_rare",1500,"legendary",4000);
    private static final Map<String,Integer> BASE_SOULS=Map.of("common",8,"uncommon",16,"rare",32,"very_rare",64,"legendary",128);
    public static final String[] STARTERS={"bulbasaur","charmander","squirtle","pikachu","eevee"};
    private static volatile PokecEconomy active=new PokecEconomy(BASE_COINS,BASE_SOULS,Map.of(),Map.of(),Set.of(STARTERS),true);
    private final Map<String,Integer> rarityCoins,raritySouls,speciesCoins,speciesSouls;
    private final Set<String> starters;
    private final boolean firstPurchaseFree;
    private PokecEconomy(Map<String,Integer> c,Map<String,Integer> s,Map<String,Integer> sc,Map<String,Integer> ss,Set<String> starters,boolean free){
        rarityCoins=Map.copyOf(c);raritySouls=Map.copyOf(s);speciesCoins=Map.copyOf(sc);speciesSouls=Map.copyOf(ss);this.starters=Set.copyOf(starters);firstPurchaseFree=free;
    }
    public static PokecEconomy get(){return active;}
    public int coins(String slug){var e=PokecSpecies.get(slug);return Math.max(0,speciesCoins.getOrDefault(slug,rarityCoins.getOrDefault(e==null?"common":e.rarity(),100)));}
    public int souls(String slug){var e=PokecSpecies.get(slug);return Math.max(0,speciesSouls.getOrDefault(slug,raritySouls.getOrDefault(e==null?"common":e.rarity(),8)));}
    public boolean isStarter(String slug){return starters.contains(slug);}
    public Set<String> starters(){return starters;}
    public boolean firstPurchaseFree(){return firstPurchaseFree;}

    public static synchronized void load(){
        Path file=FMLPaths.CONFIGDIR.get().resolve("minemon/pokec-economy.toml");
        try{
            Files.createDirectories(file.getParent());if(!Files.exists(file))writeDefaults(file);
            Map<String,String> values=new HashMap<>();
            for(String line:Files.readAllLines(file,StandardCharsets.UTF_8)){
                String t=line.strip();if(t.isEmpty()||t.startsWith("#"))continue;int at=t.indexOf('=');if(at<1)continue;
                String value=t.substring(at+1).split("#",2)[0].strip().replace("\"","");values.put(t.substring(0,at).strip(),value);
            }
            Map<String,Integer> coins=new HashMap<>(BASE_COINS),souls=new HashMap<>(BASE_SOULS),speciesCoins=new HashMap<>(),speciesSouls=new HashMap<>();
            for(String rarity:BASE_COINS.keySet()){
                coins.put(rarity,validInt(values.get("rarity."+rarity+".coins"),coins.get(rarity)));
                souls.put(rarity,validInt(values.get("rarity."+rarity+".souls"),souls.get(rarity)));
            }
            Set<String> starters=new HashSet<>(Arrays.asList(STARTERS));String configured=values.get("starters");
            if(configured!=null){starters.clear();for(String slug:configured.replace("[","").replace("]","").split(",")){slug=slug.strip().toLowerCase(Locale.ROOT);if(PokecSpecies.get(slug)!=null)starters.add(slug);}if(starters.isEmpty())starters.addAll(Arrays.asList(STARTERS));}
            for(var e:PokecSpecies.ALL.values()){
                String prefix="species."+e.slug()+".";String rarity=values.getOrDefault(prefix+"rarity",e.rarity());
                if(!BASE_COINS.containsKey(rarity))rarity=e.rarity();
                if(values.containsKey(prefix+"coins"))speciesCoins.put(e.slug(),validInt(values.get(prefix+"coins"),coins.get(rarity)));
                if(values.containsKey(prefix+"souls"))speciesSouls.put(e.slug(),validInt(values.get(prefix+"souls"),souls.get(rarity)));
                // A rarity override changes both defaults unless an explicit species override is present.
                if(!rarity.equals(e.rarity())){speciesCoins.putIfAbsent(e.slug(),coins.get(rarity));speciesSouls.putIfAbsent(e.slug(),souls.get(rarity));}
            }
            active=new PokecEconomy(coins,souls,speciesCoins,speciesSouls,starters,validBoolean(values.get("firstPurchaseFree"),true));
        }catch(Exception e){System.err.println("[Minemon] PokéC config load failed; safe defaults remain active: "+e);}
    }
    private static int validInt(String raw,int fallback){try{int n=Integer.parseInt(raw);return n<0?fallback:n;}catch(Exception e){return fallback;}}
    private static boolean validBoolean(String raw,boolean fallback){if("true".equalsIgnoreCase(raw))return true;if("false".equalsIgnoreCase(raw))return false;return fallback;}
    private static void writeDefaults(Path file)throws IOException{
        StringBuilder out=new StringBuilder("# PokéC economy. Changes apply after /pokec reload.\n# Flat keys are valid TOML; per-species price and soul overrides are optional.\n");
        out.append("firstPurchaseFree = true\nstarters = [\"bulbasaur\", \"charmander\", \"squirtle\", \"pikachu\", \"eevee\"]\n\n");
        for(String r:BASE_COINS.keySet())out.append("rarity.").append(r).append(".souls = ").append(BASE_SOULS.get(r)).append("\nrarity.").append(r).append(".coins = ").append(BASE_COINS.get(r)).append('\n');
        out.append("\n# Initial classification for every Gen I species.\n");
        for(var e:PokecSpecies.ordered())out.append("species.").append(e.slug()).append(".rarity = \"").append(e.rarity()).append("\"\n");
        Files.writeString(file,out,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
    }
    private PokecEconomy(){this(BASE_COINS,BASE_SOULS,Map.of(),Map.of(),Set.of(STARTERS),true);}
}
