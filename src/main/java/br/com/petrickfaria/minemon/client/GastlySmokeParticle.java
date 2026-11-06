package br.com.petrickfaria.minemon.client;

import br.com.petrickfaria.minemon.entity.SpeciesDefinitions;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** GeckoLib/Forge client adapter for Cobblemon 1.8.2's ghostly_smoke emitter. */
public final class GastlySmokeParticle extends TextureSheetParticle {
    private static final java.util.concurrent.atomic.AtomicInteger LARGE_COUNT=new java.util.concurrent.atomic.AtomicInteger(),SMALL_COUNT=new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger FAR_RENDER_COUNT=new java.util.concurrent.atomic.AtomicInteger();
    private static volatile double MAX_RENDER_DISTANCE_FOR_VALIDATION;
    private static volatile SpriteSet LARGE_SPRITES,SMALL_SPRITES;
    public static int largeSpawnsForValidation(){return LARGE_COUNT.get();}
    public static int smallSpawnsForValidation(){return SMALL_COUNT.get();}
    public static int farRendersForValidation(){return FAR_RENDER_COUNT.get();}
    public static double maxRenderDistanceForValidation(){return MAX_RENDER_DISTANCE_FOR_VALIDATION;}
    private static final java.util.concurrent.atomic.AtomicInteger SPAWN_COUNT = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger NATIVE_EMITTER_COUNT = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger NATIVE_SOURCE_SAMPLE_COUNT = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger NATIVE_TICK_COUNT = new java.util.concurrent.atomic.AtomicInteger();
    private static volatile double MAX_NATIVE_EMITTER_DRIFT;
    private static volatile double MAX_NATIVE_SOURCE_DISTANCE_FROM_GASTLY;
    private final SpriteSet sprites;
    private final net.minecraft.world.entity.Entity owner;
    private final int slot;
    private final boolean small;
    private final boolean followsOwner;
    private final double emitterX, emitterY, emitterZ;

    private GastlySmokeParticle(ClientLevel level, double x, double y, double z,
                                double xd, double yd, double zd, SpriteSet sprites,boolean small,
                                net.minecraft.world.entity.Entity owner,int slot,boolean followsOwner) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;
        this.emitterX=x;this.emitterY=y;this.emitterZ=z;
        this.owner=owner;this.slot=slot;this.small=small;this.followsOwner=followsOwner;
        // Particle's default constructor adds random velocity. Keep both the
        // native locator sprites and attached aura sprites within their compact
        // source radius. GeckoLib's xd/yd/zd are motion values, never entity IDs.
        this.xd = this.yd = this.zd = 0;
        this.lifetime = br.com.petrickfaria.minemon.entity.GastlyAura.LIFETIME; // Native maximum lifetime: 0.8 s.
        this.gravity = 0.0F;
        this.friction = 0.96F;
        this.hasPhysics = false;
        this.setAlpha(br.com.petrickfaria.minemon.entity.GastlyAura.OPACITY);
        this.setSpriteFromAge(sprites);
        this.setColor(0xBE / 255.0F, 0x81 / 255.0F, 0xE8 / 255.0F);
        this.updateSourceSize(0.0F);
        if(followsOwner)this.followBody();
    }

    public static int spawnCountForValidation() {
        return SPAWN_COUNT.get();
    }
    public static int nativeEmitterCountForValidation(){return NATIVE_EMITTER_COUNT.get();}
    public static int nativeSourceSampleCountForValidation(){return NATIVE_SOURCE_SAMPLE_COUNT.get();}
    public static int nativeEmitterTickCountForValidation(){return NATIVE_TICK_COUNT.get();}
    public static double maxNativeEmitterDriftForValidation(){return MAX_NATIVE_EMITTER_DRIFT;}
    public static double maxNativeSourceDistanceFromGastlyForValidation(){return MAX_NATIVE_SOURCE_DISTANCE_FROM_GASTLY;}

    @Override
    public void tick() {
        super.tick();
        if(followsOwner){
            if(owner==null || owner.isRemoved()){remove();return;}
            followBody();
        }else if(Boolean.getBoolean("minemon.clientSmokeTest")){
            NATIVE_TICK_COUNT.incrementAndGet();
            double dx=x-emitterX,dy=y-emitterY,dz=z-emitterZ;
            MAX_NATIVE_EMITTER_DRIFT=Math.max(MAX_NATIVE_EMITTER_DRIFT,Math.sqrt(dx*dx+dy*dy+dz*dz));
        }
        if (this.removed) return;
        float progress = (float) this.age / (float) this.lifetime;
        this.setSpriteFromAge(this.sprites);
        this.setColor(lerp(progress, 0xBE, 0x7B) / 255.0F,
                lerp(progress, 0x81, 0x23) / 255.0F,
                lerp(progress, 0xE8, 0xD6) / 255.0F);
        // Cobblemon's variable.shrink curve controls the sprite size. Its
        // eight native nodes expand quickly and taper slightly near the end.
        this.updateSourceSize(progress);
    }

    private void updateSourceSize(float progress) {
        float[] nodes = {0.0F, 0.81F, 1.0F, 1.0F, 1.0F, 1.0F, 0.98F, 0.90F};
        float position = Math.min(1.0F, Math.max(0.0F, progress)) * (nodes.length - 1);
        int low = (int) position;
        int high = Math.min(nodes.length - 1, low + 1);
        float sample = nodes[low] + (nodes[high] - nodes[low]) * (position - low);
        this.quadSize = (small?br.com.petrickfaria.minemon.entity.GastlyAura.SMALL_SIZE:br.com.petrickfaria.minemon.entity.GastlyAura.LARGE_SIZE) * SpeciesDefinitions.get("gastly").scale() * sample;
    }

    private void followBody(){ followBody(1.0F); }
    private void followBody(float partialTick){
        if(owner==null)return;
        float scale=SpeciesDefinitions.get("gastly").scale();
        double angle=slot*2.3999632297+owner.tickCount*.012;
        // Contract the original radial orbit uniformly. The cutout core pass and the
        // post-particle redraw provide the same depth rule wherever a billboard crosses it.
        double bodyWidth=br.com.petrickfaria.minemon.entity.ModelMetrics.bodyWidth("gastly")*scale;
        double radius=br.com.petrickfaria.minemon.entity.GastlyAura.particleOrbitRadius(bodyWidth);
        double center=br.com.petrickfaria.minemon.entity.ModelMetrics.bodyCenterY("gastly")*scale;
        if(owner instanceof br.com.petrickfaria.minemon.entity.MinemonCreature pokemon && pokemon.isDefeated())center+=br.com.petrickfaria.minemon.entity.ModelMetrics.defeatedOffset("gastly",scale);
        double height=br.com.petrickfaria.minemon.entity.ModelMetrics.bodyHeight("gastly")*scale;
        net.minecraft.world.phys.Vec3 renderPosition=owner.getPosition(partialTick);
        double px=renderPosition.x+Math.cos(angle)*radius,pz=renderPosition.z+Math.sin(angle)*radius;
        double py=renderPosition.y+center+((slot%3)-.65)*height*.32*br.com.petrickfaria.minemon.entity.GastlyAura.RADIAL_DISTRIBUTION_SCALE;
        this.xo=this.x;this.yo=this.y;this.zo=this.z;setPos(px,py,pz);
    }
    private static int lerp(float t, int from, int to) {
        return Math.round(from + (to - from) * Math.min(1.0F, Math.max(0.0F, t)));
    }

    private static final ParticleRenderType TRANSLUCENT_WITHOUT_DEPTH_WRITES = new ParticleRenderType() {
        @Override
        public void begin(com.mojang.blaze3d.vertex.BufferBuilder buffer,
                          net.minecraft.client.renderer.texture.TextureManager textureManager) {
            ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT.begin(buffer, textureManager);
            // Keep the normal depth test, but let the opaque Gastly core redraw over its aura.
            com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
        }

        @Override
        public void end(com.mojang.blaze3d.vertex.Tesselator tesselator) {
            try {
                ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT.end(tesselator);
            } finally {
                com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
            }
        }

        @Override
        public String toString() {
            return "GASTLY_TRANSLUCENT_WITHOUT_DEPTH_WRITES";
        }
    };

    @Override
    public ParticleRenderType getRenderType() {
        return TRANSLUCENT_WITHOUT_DEPTH_WRITES;
    }

    static boolean auraSpritesReady(){return LARGE_SPRITES!=null&&SMALL_SPRITES!=null;}
    static void beginAuraFrame(com.mojang.blaze3d.vertex.BufferBuilder buffer,
                               net.minecraft.client.renderer.texture.TextureManager textures){
        TRANSLUCENT_WITHOUT_DEPTH_WRITES.begin(buffer,textures);
    }
    static void endAuraFrame(com.mojang.blaze3d.vertex.Tesselator tesselator){
        TRANSLUCENT_WITHOUT_DEPTH_WRITES.end(tesselator);
    }
    static void renderAuraParticle(br.com.petrickfaria.minemon.entity.MinemonCreature owner,int slot,
                                  boolean small,int age,float partialTick,net.minecraft.client.Camera camera,
                                  com.mojang.blaze3d.vertex.VertexConsumer buffer,double distanceSqr){
        SpriteSet sprites=small?SMALL_SPRITES:LARGE_SPRITES;
        if(sprites==null||!(owner.level instanceof ClientLevel level))return;
        GastlySmokeParticle particle=new GastlySmokeParticle(level,owner.getX(),owner.getY(),owner.getZ(),
                0.0D,0.0D,0.0D,sprites,small,owner,slot,true);
        particle.age=Math.max(0,Math.min(age,particle.lifetime-1));
        particle.setSpriteFromAge(sprites);
        float progress=Math.min(1.0F,Math.max(0.0F,(age+partialTick)/(float)particle.lifetime));
        particle.setColor(lerp(progress,0xBE,0x7B)/255.0F,lerp(progress,0x81,0x23)/255.0F,
                lerp(progress,0xE8,0xD6)/255.0F);
        particle.updateSourceSize(progress);
        particle.followBody(partialTick);
        particle.xo=particle.x;particle.yo=particle.y;particle.zo=particle.z;
        particle.render(buffer,camera,partialTick);
        SPAWN_COUNT.incrementAndGet();
        (small?SMALL_COUNT:LARGE_COUNT).incrementAndGet();
        if(distanceSqr>=32.0D*32.0D)FAR_RENDER_COUNT.incrementAndGet();
        MAX_RENDER_DISTANCE_FOR_VALIDATION=Math.max(MAX_RENDER_DISTANCE_FOR_VALIDATION,Math.sqrt(distanceSqr));
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final boolean small;
        public Provider(SpriteSet sprites){this(sprites,false);}
        public Provider(SpriteSet sprites,boolean small) {
            this.sprites = sprites;this.small=small;
            if(small)SMALL_SPRITES=sprites;else LARGE_SPRITES=sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd) {
            NATIVE_EMITTER_COUNT.incrementAndGet();
            if(Boolean.getBoolean("minemon.clientSmokeTest")){
                double closest=Double.POSITIVE_INFINITY;
                for(var entity:level.entitiesForRendering())if(entity instanceof br.com.petrickfaria.minemon.entity.MinemonCreature pokemon
                        &&pokemon.getSpeciesSlug().equals("gastly"))
                    closest=Math.min(closest,entity.distanceToSqr(x,y,z));
                if(Double.isFinite(closest)){
                    NATIVE_SOURCE_SAMPLE_COUNT.incrementAndGet();
                    MAX_NATIVE_SOURCE_DISTANCE_FROM_GASTLY=Math.max(
                            MAX_NATIVE_SOURCE_DISTANCE_FROM_GASTLY,Math.sqrt(closest));
                }
            }
            GastlySmokeParticle particle=new GastlySmokeParticle(level, x, y, z, xd, yd, zd,
                    this.sprites,small,null,0,false);
            if(Boolean.getBoolean("minemon.clientSmokeTest")){
                double dx=particle.x-emitterX,dy=particle.y-emitterY,dz=particle.z-emitterZ;
                MAX_NATIVE_EMITTER_DRIFT=Math.max(MAX_NATIVE_EMITTER_DRIFT,Math.sqrt(dx*dx+dy*dy+dz*dz));
            }
            return particle;
        }
    }
}
