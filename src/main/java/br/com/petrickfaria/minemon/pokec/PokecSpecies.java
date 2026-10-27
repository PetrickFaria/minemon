package br.com.petrickfaria.minemon.pokec;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Stable Pokédex catalog used by the terminal; type1 is the accepted soul type. */
public final class PokecSpecies {
    public record Entry(int number,String slug,String name,List<String> types,String rarity,String evolvesFrom) {
        public String primaryType(){return types.get(0);}
        public boolean isEvolution(){return evolvesFrom!=null&&!evolvesFrom.isBlank();}
    }
    public static final Map<String,Entry> ALL=load();
    private static Map<String,Entry> load(){
        try(var in=PokecSpecies.class.getResourceAsStream("/data/minemon_gen1/pokec_species.json")){
            if(in==null)throw new IllegalStateException("missing PokéC species metadata");
            var out=new LinkedHashMap<String,Entry>();
            for(var el:JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonArray()){
                var o=el.getAsJsonObject();var typeList=new ArrayList<String>();o.getAsJsonArray("types").forEach(t->typeList.add(t.getAsString()));
                var e=new Entry(o.get("number").getAsInt(),o.get("slug").getAsString(),o.get("name").getAsString(),List.copyOf(typeList),o.get("rarity").getAsString(),o.get("evolvesFrom").isJsonNull()?null:o.get("evolvesFrom").getAsString());
                if(out.put(e.slug(),e)!=null)throw new IllegalStateException("duplicate PokéC species: "+e.slug());
            }
            if(out.size()!=151)throw new IllegalStateException("PokéC catalog must contain all 151 species; found "+out.size());
            return Collections.unmodifiableMap(out);
        }catch(Exception e){throw new IllegalStateException("Unable to load PokéC species data",e);}
    }
    public static Entry get(String slug){return ALL.get(slug);}
    public static List<Entry> ordered(){return ALL.values().stream().sorted(Comparator.comparingInt(Entry::number)).toList();}
    private PokecSpecies(){}
}
