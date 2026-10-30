package br.com.petrickfaria.minemon.entity;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Per-species measurements from selected geometry and native idle poses. */
public final class ModelMetrics {
    private static final JsonObject DATA=load();
    private static JsonObject load(){
        try(var in=ModelMetrics.class.getResourceAsStream("/data/minemon_gen1/model_metrics.json")){
            if(in==null)throw new IllegalStateException("Missing model measurements");
            return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(java.io.IOException e){throw new IllegalStateException(e);}
    }
    private static double metric(String slug,String key){return DATA.getAsJsonObject(slug).get(key).getAsDouble();}
    public static float width(String slug,float scale){return (float)(metric(slug,"width")*scale);}
    public static float height(String slug,float scale){return (float)(metric(slug,"height")*scale);}
    public static double top(String slug,float scale){return metric(slug,"top")*scale;}
    public static double floatOffset(String slug,float scale){return slug.equals("abra")||slug.equals("porygon")?-metric(slug,"bottom")*scale:0;}
    private ModelMetrics(){}
}
