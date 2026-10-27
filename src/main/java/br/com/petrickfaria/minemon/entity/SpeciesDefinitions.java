package br.com.petrickfaria.minemon.entity;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class SpeciesDefinitions {
 public record Definition(int nationalPokedexNumber,String slug,String name,String model,String texture,String animation,String mode,float scale,float width,float height,double health,double attack,double speed,int eggBase,int eggSpots,String idle,String walk,String movementFallback,int attackTicks,Map<String,String> clips) {}
 public static final Map<String,Definition> ALL=load();
 private static Map<String,Definition> load() {
  Map<String,Definition> result=new LinkedHashMap<>();
  try(InputStream in=SpeciesDefinitions.class.getResourceAsStream("/data/minemon_gen1/runtime_species.json")) {
   if(in==null)throw new IllegalStateException("Missing runtime species manifest");
   for(JsonElement element:JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonArray()) {
    JsonObject o=element.getAsJsonObject();Map<String,String> clips=new HashMap<>();o.getAsJsonObject("clips").entrySet().forEach(e->clips.put(e.getKey(),e.getValue().getAsString()));
    String slug=str(o,"slug");
    CompatibleLocomotion.apply(slug,clips);
    clips.put("night_sleep","animation."+slug+".minemon_night_sleep");
    if(slug.equals("vaporeon")){
     clips.put("night_sleep_land","animation.vaporeon.minemon_night_sleep_land");
     clips.put("night_sleep_water","animation.vaporeon.minemon_night_sleep_water");
    }
    // Jynx/Magmar/Victreebel factors were derived from the selected geo mesh:
    // sum cube volume and vertical extent, balanced geometrically against the
    // approved Electabuzz/Rhydon render bounds. The source manifest stays raw.
    float factor=switch(slug){
     case "seaking" -> 0.75F;
     case "dewgong" -> 0.50F;
     case "kangaskhan" -> 0.75F;
     case "snorlax" -> 0.75F*0.90F;
     case "venusaur" -> 0.75F*0.90F;
     case "jynx" -> 0.965F/o.get("scale").getAsFloat();
     case "magmar" -> 1.47F/o.get("scale").getAsFloat();
     case "victreebel" -> (1.46F*0.85F*0.90F)/o.get("scale").getAsFloat();
     default -> 1.0F;
    };
    var profile=SpeciesBehaviorProfiles.profile(slug);
    boolean ground=profile==SpeciesBehaviorProfiles.Profile.GROUND||profile==SpeciesBehaviorProfiles.Profile.GROUND_FLIGHT
        ||profile==SpeciesBehaviorProfiles.Profile.GROUND_FLOAT_CYCLE||profile==SpeciesBehaviorProfiles.Profile.WATER_VISITOR;
    String idle=ground?clips.getOrDefault("ground_idle",str(o,"idle")):str(o,"idle");
    String walk=ground?clips.getOrDefault("ground_walk",str(o,"walk")):str(o,"walk");
    String fallback=CompatibleLocomotion.groundGait(slug)?null:o.has("movement_fallback")&&!o.get("movement_fallback").isJsonNull()?o.get("movement_fallback").getAsString():null;
    float width=o.get("width").getAsFloat()*factor,height=o.get("height").getAsFloat()*factor;
    if(slug.equals("jynx")||slug.equals("magmar")){width=1.05F;height=2.25F;}
    else if(slug.equals("victreebel")){width=1.10F*0.85F*0.90F;height=1.70F*0.85F*0.90F;}
    Definition d=new Definition(o.get("nationalPokedexNumber").getAsInt(),str(o,"slug"),str(o,"name"),str(o,"model"),str(o,"texture"),str(o,"animation"),str(o,"mode"),o.get("scale").getAsFloat()*factor,width,height,o.get("health").getAsDouble(),o.get("attack").getAsDouble(),o.get("speed").getAsDouble(),o.get("eggBase").getAsInt(),o.get("eggSpots").getAsInt(),idle,walk,fallback,o.has("attackTicks")?o.get("attackTicks").getAsInt():0,Collections.unmodifiableMap(clips));
    if(result.put(d.slug(),d)!=null)throw new IllegalStateException("Duplicate species "+d.slug());
   }
  }catch(Exception e){throw new IllegalStateException("Could not load runtime species",e);}
  return Collections.unmodifiableMap(result);
 }
 private static String str(JsonObject o,String key){return o.get(key).getAsString();}
 public static Definition get(String slug){return ALL.get(slug);}
 private SpeciesDefinitions(){}
}
