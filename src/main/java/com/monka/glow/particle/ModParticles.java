package com.monka.glow.particle;

import com.monka.glow.Glow;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Glow.MODID);

    public static final Supplier<SimpleParticleType> GLOWSTONE_DUST = registerParticle("glowstone_dust");

    public static Supplier<SimpleParticleType> registerParticle(String name) {
        return PARTICLE_TYPES.register(name, () -> new SimpleParticleType(false));
    }

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}
