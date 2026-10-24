package br.com.petrickfaria.minemon.entity;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class SpeciesDefinitions {
 public record Definition(int nationalPokedexNumber,String slug,String model,String texture,String animation,String mode,float scale,float width,float height,double health,double attack,double speed,int eggBase,int eggSpots,String idle,String walk,String movementFallback,int attackTicks,Map<String,String> clips) {}
 public static final Map<String,Definition> ALL=load();
 private static Map<String,Definition> load() {
  Map<String,Definition> result=new LinkedHashMap<>();
  try(InputStream in=SpeciesDefinitions.class.getResourceAsStream("/data/minemon_gen1/runtime_species.json")) {
   if(in==null)throw new IllegalStateException("Missing runtime species manifest");
   for(JsonElement element:JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonArray()) {
    JsonObject o=element.getAsJsonObject();Map<String,String> clips=new HashMap<>();o.getAsJsonObject("clips").entrySet().forEach(e->clips.put(e.getKey(),e.getValue().getAsString()));
    String slug=str(o,"slug");
    CompatibleLocomotion.apply(slug,clips);
    float factor=slug.equals("seaking")?0.75F:1.0F;
    var profile=SpeciesBehaviorProfiles.profile(slug);
    boolean ground=profile==SpeciesBehaviorProfiles.Profile.GROUND||profile==SpeciesBehaviorProfiles.Profile.GROUND_FLIGHT
        ||profile==SpeciesBehaviorProfiles.Profile.WATER_VISITOR;
    String idle=ground?clips.getOrDefault("ground_idle",str(o,"idle")):str(o,"idle");
    String walk=ground?clips.getOrDefault("ground_walk",str(o,"walk")):str(o,"walk");
    String fallback=CompatibleLocomotion.groundGait(slug)?null:o.has("movement_fallback")&&!o.get("movement_fallback").isJsonNull()?o.get("movement_fallback").getAsString():null;
    Definition d=new Definition(o.get("nationalPokedexNumber").getAsInt(),str(o,"slug"),str(o,"model"),str(o,"texture"),str(o,"animation"),str(o,"mode"),o.get("scale").getAsFloat()*factor,o.get("width").getAsFloat()*factor,o.get("height").getAsFloat()*factor,o.get("health").getAsDouble(),o.get("attack").getAsDouble(),o.get("speed").getAsDouble(),o.get("eggBase").getAsInt(),o.get("eggSpots").getAsInt(),idle,walk,fallback,o.has("attackTicks")?o.get("attackTicks").getAsInt():0,Collections.unmodifiableMap(clips));
    if(result.put(d.slug(),d)!=null)throw new IllegalStateException("Duplicate species "+d.slug());
   }
  }catch(Exception e){throw new IllegalStateException("Could not load runtime species",e);}
  return Collections.unmodifiableMap(result);
 }
 private static String str(JsonObject o,String key){return o.get(key).getAsString();}
 public static Definition get(String slug){return ALL.get(slug);}
 private SpeciesDefinitions(){}
}
