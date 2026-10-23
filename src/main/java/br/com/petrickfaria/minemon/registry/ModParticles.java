package br.com.petrickfaria.minemon.registry;

import br.com.petrickfaria.minemon.Minemon;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Client-rendered Cobblemon particle adaptations used by species models. */
public final class ModParticles {
    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Minemon.MOD_ID);
    public static final RegistryObject<SimpleParticleType> GASTLY_GHOSTLY_SMOKE =
            PARTICLE_TYPES.register("gastly_ghostly_smoke", () -> new SimpleParticleType(false));

    private ModParticles() {}
}
