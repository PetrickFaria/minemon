package br.com.petrickfaria.minemon.menu;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector4f;

/** Measures only vertices actually submitted by the species' animated idle pose. */
public final class PreviewMeshBounds {
    private static final VertexConsumer SINK=new VertexConsumer(){
        public VertexConsumer vertex(double x,double y,double z){return this;}
        public VertexConsumer color(int r,int g,int b,int a){return this;}
        public VertexConsumer uv(float u,float v){return this;}
        public VertexConsumer overlayCoords(int u,int v){return this;}
        public VertexConsumer uv2(int u,int v){return this;}
        public VertexConsumer normal(float x,float y,float z){return this;}
        public void endVertex(){}
        public void defaultColor(int r,int g,int b,int a){}
        public void unsetDefaultColor(){}
    };
    public static VertexConsumer sink(){return SINK;}
    private static PreviewMeshBounds active;
    private final double[] origin=new double[3],inverse=new double[9];
    private final java.util.List<float[]> points=new java.util.ArrayList<>();
    private double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX;
    private double maxX=Double.NEGATIVE_INFINITY,maxY=maxX,maxZ=maxX;
    public PreviewMeshBounds(Matrix4f transform){
        var o=new Vector4f(0,0,0,1);o.transform(transform);
        var a=new Vector4f(1,0,0,1);a.transform(transform);
        var b=new Vector4f(0,1,0,1);b.transform(transform);
        var c=new Vector4f(0,0,1,1);c.transform(transform);
        origin[0]=o.x();origin[1]=o.y();origin[2]=o.z();
        double ax=a.x()-o.x(),ay=a.y()-o.y(),az=a.z()-o.z();
        double bx=b.x()-o.x(),by=b.y()-o.y(),bz=b.z()-o.z();
        double cx=c.x()-o.x(),cy=c.y()-o.y(),cz=c.z()-o.z();
        double determinant=ax*(by*cz-bz*cy)+ay*(bz*cx-bx*cz)+az*(bx*cy-by*cx);
        if(Math.abs(determinant)<1.0E-12)throw new IllegalStateException("Singular preview transform");
        inverse[0]=(by*cz-bz*cy)/determinant;inverse[1]=(bz*cx-bx*cz)/determinant;inverse[2]=(bx*cy-by*cx)/determinant;
        inverse[3]=(cy*az-cz*ay)/determinant;inverse[4]=(cz*ax-cx*az)/determinant;inverse[5]=(cx*ay-cy*ax)/determinant;
        inverse[6]=(ay*bz-az*by)/determinant;inverse[7]=(az*bx-ax*bz)/determinant;inverse[8]=(ax*by-ay*bx)/determinant;
    }
    public static void begin(PreviewMeshBounds sample){active=sample;}
    public static void end(){active=null;}
    public static VertexConsumer wrap(VertexConsumer target){
        var sample=active;
        if(sample==null)return target;
        return new VertexConsumer(){
            private final float[][] quad=new float[4][3];
            private int vertices;
            public VertexConsumer vertex(double x,double y,double z){
                double vx=x-sample.origin[0],vy=y-sample.origin[1],vz=z-sample.origin[2];
                var m=sample.inverse;
                quad[vertices++]=new float[]{(float)(m[0]*vx+m[1]*vy+m[2]*vz),
                        (float)(m[3]*vx+m[4]*vy+m[5]*vz),(float)(m[6]*vx+m[7]*vy+m[8]*vz)};
                if(vertices==4){
                    vertices=0;
                    double ax=quad[1][0]-quad[0][0],ay=quad[1][1]-quad[0][1],az=quad[1][2]-quad[0][2];
                    double bx=quad[2][0]-quad[0][0],by=quad[2][1]-quad[0][1],bz=quad[2][2]-quad[0][2];
                    double cx=ay*bz-az*by,cy=az*bx-ax*bz,cz=ax*by-ay*bx;
                    // Alternate rigs collapsed by scale=0 submit degenerate
                    // quads. Their distant pivots must not shrink the preview.
                    if(cx*cx+cy*cy+cz*cz>1.0E-10)for(var v:quad){
                        sample.points.add(v);
                        sample.minX=Math.min(sample.minX,v[0]);sample.maxX=Math.max(sample.maxX,v[0]);
                        sample.minY=Math.min(sample.minY,v[1]);sample.maxY=Math.max(sample.maxY,v[1]);
                        sample.minZ=Math.min(sample.minZ,v[2]);sample.maxZ=Math.max(sample.maxZ,v[2]);
                    }
                }
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
        var initial=project(23,-10);
        double width=initial[3]-initial[0],height=initial[4]-initial[1];
        double maximumWidth=0,maximumHeight=0;
        // Project actual posed vertices. A rotated enclosing box contains empty
        // corners and can badly underestimate visible Charizard/body size.
        for(int yaw=0;yaw<360;yaw+=15)for(int pitch=-46;pitch<=34;pitch+=10){
            var bounds=project(yaw,pitch);
            maximumWidth=Math.max(maximumWidth,bounds[3]-bounds[0]);
            maximumHeight=Math.max(maximumHeight,bounds[4]-bounds[1]);
        }
        float scale=(float)Math.min(48.0/Math.max(width,height),Math.min(76.0/(maximumHeight*1.03),220.0/(maximumWidth*1.03)));
        double rx=(initial[0]+initial[3])/2,ry=(initial[1]+initial[4])/2,rz=(initial[2]+initial[5])/2;
        double yaw=Math.toRadians(23),pitch=Math.toRadians(-10);
        double y=Math.cos(pitch)*ry+Math.sin(pitch)*rz;
        double z=-Math.sin(pitch)*ry+Math.cos(pitch)*rz;
        return new PreviewBounds.Fit(scale,Math.cos(yaw)*rx-Math.sin(yaw)*z,y,Math.sin(yaw)*rx+Math.cos(yaw)*z);
    }
    private double[] project(double yawDegrees,double pitchDegrees){
        double yaw=Math.toRadians(yawDegrees),pitch=Math.toRadians(pitchDegrees);
        double cy=Math.cos(yaw),sy=Math.sin(yaw),cp=Math.cos(pitch),sp=Math.sin(pitch);
        double[] bounds={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        for(var p:points){
            double x=cy*p[0]+sy*p[2],z=-sy*p[0]+cy*p[2];
            double y=cp*p[1]-sp*z;z=sp*p[1]+cp*z;
            bounds[0]=Math.min(bounds[0],x);bounds[3]=Math.max(bounds[3],x);
            bounds[1]=Math.min(bounds[1],y);bounds[4]=Math.max(bounds[4],y);
            bounds[2]=Math.min(bounds[2],z);bounds[5]=Math.max(bounds[5],z);
        }
        return bounds;
    }
}
