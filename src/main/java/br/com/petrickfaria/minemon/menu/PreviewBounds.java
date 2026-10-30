package br.com.petrickfaria.minemon.menu;

import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Mesh coordinates already include cube inflation and hierarchical bind rotations. */
final class PreviewBounds {
    private static final com.google.gson.JsonObject BOUNDS=load("preview_bounds.json");
    private static final com.google.gson.JsonObject PIVOTS=load("preview_body_pivots.json");
    private static com.google.gson.JsonObject load(String file){
        try(var in=PreviewBounds.class.getResourceAsStream("/data/minemon_gen1/"+file)){
            if(in==null)throw new IllegalStateException("Missing preview bounds");
            return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(java.io.IOException e){throw new IllegalStateException(e);}
    }
    record Fit(float scale, double x, double y, double z) {}
    static Fit fit(String slug,float modelScale) {
        {
            var a=BOUNDS.getAsJsonArray(slug);
            double minX=a.get(0).getAsDouble(),minY=a.get(1).getAsDouble(),minZ=a.get(2).getAsDouble();
            double maxX=a.get(3).getAsDouble(),maxY=a.get(4).getAsDouble(),maxZ=a.get(5).getAsDouble();
            double w=(maxX-minX)+(maxZ-minZ)*Math.sin(Math.toRadians(4));
            double h=(maxY-minY)*Math.cos(Math.toRadians(6))+(maxZ-minZ)*Math.sin(Math.toRadians(6));
            float scale=(float)Math.min(100,Math.min(232/Math.max(.2,w*modelScale*1.3),88/Math.max(.2,h*modelScale*1.3)));
            var center=PIVOTS.getAsJsonObject(slug).getAsJsonArray("center");
            // GeoBuilder reflects X; fixed body yaw 180 avoids a second Z reflection.
            return new Fit(scale,-center.get(0).getAsDouble()*modelScale,center.get(1).getAsDouble()*modelScale,center.get(2).getAsDouble()*modelScale);
        }
    }
    static float rotationScale(String slug,float modelScale,float approvedScale,float yaw,float pitch) {
        return approvedScale;
    }
    private PreviewBounds() {}
}
