package br.com.petrickfaria.minemon.menu;

import com.google.gson.JsonParser;
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
            double w=maxX-minX,h=maxY-minY,d=maxZ-minZ;
            double projectedWidth,projectedHeight;
            double[] extents=maximumRotatedExtents(w,h,d);
            projectedWidth=extents[0];projectedHeight=extents[1];
            // Match each model's largest projected dimension to the same visual
            // target. The 12 px safety margin fits the 88 px preview height,
            // including the full supported rotation range and idle articulation.
            float scale=(float)(76.0/Math.max(.2,Math.max(projectedWidth,projectedHeight)*modelScale));
            var center=PIVOTS.getAsJsonObject(slug).getAsJsonArray("center");
            // GeoBuilder reflects X; fixed body yaw 180 avoids a second Z reflection.
            return new Fit(scale,-center.get(0).getAsDouble()*modelScale,center.get(1).getAsDouble()*modelScale,center.get(2).getAsDouble()*modelScale);
        }
    }
    static float rotationScale(String slug,float modelScale,float approvedScale,float yaw,float pitch) {
        return approvedScale;
    }
    static double normalizedLargestExtent(String slug,float modelScale){
        var a=BOUNDS.getAsJsonArray(slug);
        double width=a.get(3).getAsDouble()-a.get(0).getAsDouble();
        double height=a.get(4).getAsDouble()-a.get(1).getAsDouble();
        double depth=a.get(5).getAsDouble()-a.get(2).getAsDouble();
        double[] extents=maximumRotatedExtents(width,height,depth);
        return Math.max(extents[0],extents[1])*modelScale*fit(slug,modelScale).scale();
    }
    static double[] maximumRotatedExtents(double width,double height,double depth) {
        double maxWidth=0,maxHeight=0;
        // Preview yaw is unrestricted; pitch is clamped to [-46°, 34°]
        // after the fixed six-degree viewing tilt.
        for(int yaw=0;yaw<360;yaw+=5) {
            double radians=Math.toRadians(yaw);
            double xExtent=Math.abs(Math.cos(radians))*width+Math.abs(Math.sin(radians))*depth;
            double zExtent=Math.abs(Math.sin(radians))*width+Math.abs(Math.cos(radians))*depth;
            maxWidth=Math.max(maxWidth,xExtent);
            for(int pitch=-46;pitch<=34;pitch+=2) {
                double tilt=Math.toRadians(pitch);
                maxHeight=Math.max(maxHeight,Math.abs(Math.cos(tilt))*height+Math.abs(Math.sin(tilt))*zExtent);
            }
        }
        return new double[]{maxWidth,maxHeight};
    }
    private PreviewBounds() {}
}
