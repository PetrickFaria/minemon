package br.com.petrickfaria.minemon.menu;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector4f;

/** Measures only vertices actually submitted by the species' animated idle pose. */
public final class PreviewMeshBounds {
    private static PreviewMeshBounds active;
    private final Matrix4f inverse;
    private double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX;
    private double maxX=Double.NEGATIVE_INFINITY,maxY=maxX,maxZ=maxX;
    public PreviewMeshBounds(Matrix4f transform){inverse=transform.copy();inverse.invert();}
    public static void begin(PreviewMeshBounds sample){active=sample;}
    public static void end(){active=null;}
    public static VertexConsumer wrap(VertexConsumer target){
        var sample=active;
        if(sample==null)return target;
        return new VertexConsumer(){
            public VertexConsumer vertex(double x,double y,double z){
                var p=new Vector4f((float)x,(float)y,(float)z,1);p.transform(sample.inverse);
                sample.minX=Math.min(sample.minX,p.x());sample.maxX=Math.max(sample.maxX,p.x());
                sample.minY=Math.min(sample.minY,p.y());sample.maxY=Math.max(sample.maxY,p.y());
                sample.minZ=Math.min(sample.minZ,p.z());sample.maxZ=Math.max(sample.maxZ,p.z());
                target.vertex(x,y,z);return this;
            }
            public VertexConsumer color(int r,int g,int b,int a){target.color(r,g,b,a);return this;}
            public VertexConsumer uv(float u,float v){target.uv(u,v);return this;}
            public VertexConsumer overlayCoords(int u,int v){target.overlayCoords(u,v);return this;}
            public VertexConsumer uv2(int u,int v){target.uv2(u,v);return this;}
            public VertexConsumer normal(float x,float y,float z){target.normal(x,y,z);return this;}
            public void endVertex(){target.endVertex();}
            public void defaultColor(int r,int g,int b,int a){target.defaultColor(r,g,b,a);}
            public void unsetDefaultColor(){target.unsetDefaultColor();}
        };
    }
    PreviewBounds.Fit fit(){
        if(!Double.isFinite(minX)||maxX-minX<.001||maxY-minY<.001)return null;
        double w=maxX-minX,h=maxY-minY,d=maxZ-minZ;
        double yaw=Math.toRadians(23),pitch=Math.toRadians(10);
        double width=Math.cos(yaw)*w+Math.sin(yaw)*d;
        double height=Math.cos(pitch)*h+Math.sin(pitch)*(Math.sin(yaw)*w+Math.cos(yaw)*d);
        double[] maximum=PreviewBounds.maximumRotatedExtents(w,h,d);
        float scale=(float)Math.min(48.0/Math.max(width,height),Math.min(76.0/maximum[1],220.0/maximum[0]));
        return new PreviewBounds.Fit(scale,(minX+maxX)/2,(minY+maxY)/2,(minZ+maxZ)/2);
    }
}
