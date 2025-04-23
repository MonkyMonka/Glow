package com.monka.glow.world;

import com.monka.glow.Glow;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class GlowFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Glow.MODID);

    public static final Supplier<Feature<NoneFeatureConfiguration>> GLOWSTONE = FEATURES.register("glowstone",
            () -> new GlowstoneFeature(NoneFeatureConfiguration.CODEC));

}
