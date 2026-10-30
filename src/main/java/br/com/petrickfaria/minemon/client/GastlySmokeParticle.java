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
    private static final java.util.concurrent.atomic.AtomicInteger SPAWN_COUNT = new java.util.concurrent.atomic.AtomicInteger();
    private final SpriteSet sprites;

    private GastlySmokeParticle(ClientLevel level, double x, double y, double z,
                                double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        SPAWN_COUNT.incrementAndGet();
        this.sprites = sprites;
        // Particle's default constructor adds random velocity. Keep the inward
        // aura trajectory inside its compact source radius instead of scattering.
        this.xd = xd;this.yd = yd;this.zd = zd;
        this.lifetime = 16; // Native maximum lifetime: 0.8 s.
        this.gravity = 0.0F;
        this.friction = 0.96F;
        this.hasPhysics = false;
        this.setAlpha(0.75F);
        this.setSpriteFromAge(sprites);
        this.setColor(0xBE / 255.0F, 0x81 / 255.0F, 0xE8 / 255.0F);
        this.updateSourceSize(0.0F);
    }

    public static int spawnCountForValidation() {
        return SPAWN_COUNT.get();
    }

    @Override
    public void tick() {
        super.tick();
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
        this.quadSize = 0.85F * SpeciesDefinitions.get("gastly").scale() * sample;
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

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new GastlySmokeParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}
