package br.com.petrickfaria.minemon.menu;

import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Mesh coordinates already include cube inflation and hierarchical bind rotations. */
final class PreviewBounds {
    record Fit(float scale, double x, double y, double z) {}
    static Fit fit(String slug,float modelScale) {
        try (var stream=PreviewBounds.class.getResourceAsStream("/data/minemon_gen1/preview_bounds.json")) {
            if(stream==null)throw new IllegalStateException("Missing preview bounds");
            var a=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray(slug);
            double minX=a.get(0).getAsDouble(),minY=a.get(1).getAsDouble(),minZ=a.get(2).getAsDouble();
            double maxX=a.get(3).getAsDouble(),maxY=a.get(4).getAsDouble(),maxZ=a.get(5).getAsDouble();
            // Include depth at the preview's pitch and its small yaw excursion.
            double w=(maxX-minX)+(maxZ-minZ)*Math.sin(Math.toRadians(4));
            double h=(maxY-minY)*Math.cos(Math.toRadians(6))+(maxZ-minZ)*Math.sin(Math.toRadians(6));
            // Margin accommodates idle joint motion; scissor is a final containment guard.
            float scale=(float)Math.min(100,Math.min(232/Math.max(.2,w*modelScale*1.3),88/Math.max(.2,h*modelScale*1.3)));
            return new Fit(scale,(minX+maxX)*.5*modelScale,(minY+maxY)*.5*modelScale,(minZ+maxZ)*.5*modelScale);
        } catch (java.io.IOException e) { throw new IllegalStateException("Unable to read preview bounds",e); }
    }
    private PreviewBounds() {}
}
