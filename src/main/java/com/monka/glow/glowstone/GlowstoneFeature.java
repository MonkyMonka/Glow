package com.monka.glow.glowstone;

import com.mojang.serialization.Codec;
import com.monka.glow.GlowRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;

public class GlowstoneFeature extends Feature<NoneFeatureConfiguration> {
    public GlowstoneFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> featurePlaceContext) {
        WorldGenLevel level = featurePlaceContext.level();
        RandomSource random = featurePlaceContext.random();
        BlockPos origin = featurePlaceContext.origin();

        if (!level.isEmptyBlock(origin)) {
            BlockState stateAbove = level.getBlockState(origin.above());
            if (!stateAbove.is(Blocks.NETHERRACK) && !stateAbove.is(Blocks.BASALT) && !stateAbove.is(Blocks.BLACKSTONE)) {
                return false;
            } else {
                int x = origin.getX();
                int y = origin.getY();
                int z = origin.getZ();
                int spread = random.nextInt(1, 4);

                Iterable<BlockPos> close = BlockPos.betweenClosed(x - spread, y, z - spread, x + spread, y, z + spread);

                for (BlockPos pos : close) {
                    if (random.nextFloat() < 0.8 && !level.isEmptyBlock(pos.above())) {
                        int height = random.nextInt(6) + 2;
                        if (!stateAbove.is(Blocks.NETHERRACK) && !stateAbove.is(Blocks.BASALT) && !stateAbove.is(Blocks.BLACKSTONE)) continue;

                        ArrayList<BlockPos> blockPositions = new ArrayList<>();
                        BlockPos topPos = pos;
                        boolean startedPlacing = false;
                        for (int i = 0; i < height; i++) {
                            BlockPos glowstonePos = pos.below(i);

                            if (!level.isEmptyBlock(glowstonePos)) {
                                if (startedPlacing) break;
                                topPos = glowstonePos;
                                continue;
                            }

                            level.setBlock(glowstonePos, GlowRegistry.GLOWSTONE_PRISM.get().defaultBlockState(), 3);
                            blockPositions.add(glowstonePos);
                            startedPlacing = true;
                        }

                        GlowstonePrismBlock.applyThicknessGradient(level, blockPositions);
                        if (startedPlacing) level.setBlock(topPos, GlowRegistry.GLOWSTONE_PRISM.get().defaultBlockState().setValue(GlowstonePrismBlock.THICKNESS, GlowstonePrismThickness.BASE), 2);
                    }
                }
            }

            return true;
        }

        return false;
    }
}
