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
    public static int largeSpawnsForValidation(){return LARGE_COUNT.get();}
    public static int smallSpawnsForValidation(){return SMALL_COUNT.get();}
    private static final java.util.concurrent.atomic.AtomicInteger SPAWN_COUNT = new java.util.concurrent.atomic.AtomicInteger();
    private final SpriteSet sprites;
    private final net.minecraft.world.entity.Entity owner;
    private final int slot;
    private final boolean small;

    private GastlySmokeParticle(ClientLevel level, double x, double y, double z,
                                double xd, double yd, double zd, SpriteSet sprites,boolean small) {
        super(level, x, y, z, xd, yd, zd);
        SPAWN_COUNT.incrementAndGet();
        this.sprites = sprites;
        // Particle's default constructor adds random velocity. Keep the inward
        // aura trajectory inside its compact source radius instead of scattering.
        this.xd = this.yd = this.zd = 0;
        this.owner=level.getEntity((int)xd);this.slot=(int)yd;this.small=small;
        (small?SMALL_COUNT:LARGE_COUNT).incrementAndGet();
        this.lifetime = br.com.petrickfaria.minemon.entity.GastlyAura.LIFETIME; // Native maximum lifetime: 0.8 s.
        this.gravity = 0.0F;
        this.friction = 0.96F;
        this.hasPhysics = false;
        this.setAlpha(br.com.petrickfaria.minemon.entity.GastlyAura.OPACITY);
        this.setSpriteFromAge(sprites);
        this.setColor(0xBE / 255.0F, 0x81 / 255.0F, 0xE8 / 255.0F);
        this.updateSourceSize(0.0F);
        this.followBody();
    }

    public static int spawnCountForValidation() {
        return SPAWN_COUNT.get();
    }

    @Override
    public void tick() {
        super.tick();
        if(owner==null || owner.isRemoved()){remove();return;}
        followBody();
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

    private void followBody(){
        if(owner==null)return;
        float scale=SpeciesDefinitions.get("gastly").scale();
        double angle=slot*2.3999632297+owner.tickCount*.012;
        double radius=.55*br.com.petrickfaria.minemon.entity.ModelMetrics.bodyWidth("gastly")*scale;
        double center=br.com.petrickfaria.minemon.entity.ModelMetrics.bodyCenterY("gastly")*scale;
        if(owner instanceof br.com.petrickfaria.minemon.entity.MinemonCreature pokemon && pokemon.isDefeated())center+=br.com.petrickfaria.minemon.entity.ModelMetrics.defeatedOffset("gastly",scale);
        double height=br.com.petrickfaria.minemon.entity.ModelMetrics.bodyHeight("gastly")*scale;
        double px=owner.getX()+Math.cos(angle)*radius,pz=owner.getZ()+Math.sin(angle)*radius;
        double py=owner.getY()+center+((slot%3)-.65)*height*.32;
        this.xo=this.x;this.yo=this.y;this.zo=this.z;setPos(px,py,pz);
    }
    private static int lerp(float t, int from, int to) {
        return Math.round(from + (to - from) * Math.min(1.0F, Math.max(0.0F, t)));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final boolean small;
        public Provider(SpriteSet sprites){this(sprites,false);}
        public Provider(SpriteSet sprites,boolean small) {
            this.sprites = sprites;this.small=small;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new GastlySmokeParticle(level, x, y, z, xd, yd, zd, this.sprites,small);
        }
    }
}
