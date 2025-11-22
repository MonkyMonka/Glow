package com.monka.glow.glowstone;

import com.monka.glow.GlowRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class FallingGlowstoneEntity extends FallingBlockEntity {


    public FallingGlowstoneEntity(EntityType<? extends FallingBlockEntity> entityType, Level level) {
        super(entityType, level);
    }

    private FallingGlowstoneEntity(Level level, double x, int y, double z, BlockState blockState) {
        this(EntityType.FALLING_BLOCK, level);
        this.blockState = blockState;
        this.blocksBuilding = true;
        this.setPos(x, y, z);
        this.setDeltaMovement(Vec3.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setStartPos(this.blockPosition());
    }


    @Override
    public void tick() {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(GlowRegistry.GLOWSTONE_DUST.get(),
                    getX() + level().random.nextFloat() - level().random.nextFloat(),
                    getY() + level().random.nextFloat() - level().random.nextFloat(),
                    getZ() + level().random.nextFloat() - level().random.nextFloat(),
                    blockState.getValue(GlowstonePrismBlock.THICKNESS).getSize()*4, 0, 0, 0, 0);
        }

        super.tick();
    }

    public static FallingGlowstoneEntity fall(Level level, BlockPos pos, BlockState state) {
        FallingGlowstoneEntity fallingGlowstone = new FallingGlowstoneEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, state.hasProperty(BlockStateProperties.WATERLOGGED) ? state.setValue(BlockStateProperties.WATERLOGGED, false) : state);
        level.setBlockAndUpdate(pos, state.getFluidState().createLegacyBlock());
        level.addFreshEntity(fallingGlowstone);
        return fallingGlowstone;
    }
}
