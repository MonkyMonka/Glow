package com.monka.glow;

import com.monka.glow.glowstone.GlowstoneFeature;
import com.monka.glow.glowstone.GlowstonePrismBlock;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class GlowRegistry {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Glow.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Glow.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Glow.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Glow.MODID);

    public static final RegistryObject<Block> GLOWSTONE_PRISM = registerBlock("glowstone_prism",
            () -> new GlowstonePrismBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .instrument(NoteBlockInstrument.PLING)
                    .strength(0.3F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 15)
                    .isRedstoneConductor((state, level, pos) -> false)
            )
    );

    public static final Supplier<SimpleParticleType> GLOWSTONE_DUST = PARTICLE_TYPES.register("glowstone_dust",
            () -> new SimpleParticleType(false));

    public static final Supplier<Feature<NoneFeatureConfiguration>> GLOWSTONE = FEATURES.register("glowstone",
            () -> new GlowstoneFeature(NoneFeatureConfiguration.CODEC));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        ITEMS.register(name, () -> new BlockItem(toReturn.get(), new Item.Properties()));
        return toReturn;
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        PARTICLE_TYPES.register(bus);
        FEATURES.register(bus);
    }
}
