package br.com.petrickfaria.minemon.menu;

import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Mesh coordinates already include cube inflation and hierarchical bind rotations. */
final class PreviewBounds {
    private static final com.google.gson.JsonObject BOUNDS=load();
    private static com.google.gson.JsonObject load(){
        try(var in=PreviewBounds.class.getResourceAsStream("/data/minemon_gen1/preview_bounds.json")){
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
            double diameter=Math.sqrt(Math.pow(maxX-minX,2)+Math.pow(maxY-minY,2)+Math.pow(maxZ-minZ,2));
            float scale=(float)Math.min(100,88/Math.max(.2,diameter*modelScale*1.3));
            return new Fit(scale,(minX+maxX)*.5*modelScale,(minY+maxY)*.5*modelScale,(minZ+maxZ)*.5*modelScale);
        }
    }
    static float rotationScale(String slug,float modelScale,float approvedScale,float yaw,float pitch) {
        return approvedScale;
    }
    private PreviewBounds() {}
}
