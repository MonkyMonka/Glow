package com.monka.glow.block.custom;

import com.monka.glow.Glow;
import com.monka.glow.block.ModBlocks;
import com.monka.glow.particle.ModParticles;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class GlowstonePrismBlock extends Block implements Fallable, SimpleWaterloggedBlock {
    public static final VoxelShape BASE_SHAPE;
    public static final VoxelShape MIDDLE_SHAPE;
    public static final VoxelShape TIP_SHAPE;
    public static final BooleanProperty WATERLOGGED;
    public static final EnumProperty<GlowstonePrismThickness> THICKNESS;

    public GlowstonePrismBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(THICKNESS, GlowstonePrismThickness.TIP).setValue(WATERLOGGED, false));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(THICKNESS, WATERLOGGED);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < 0.012 && level.isEmptyBlock(pos.below()))
            level.setBlockAndUpdate(pos.below(), ModBlocks.GLOWSTONE_PRISM.get().defaultBlockState());

//        if (random.nextFloat() < 0.05 && state.getValue(THICKNESS) == GlowstonePrismThickness.BASE && level.getBlockState(pos.above()).is(ModBlocks.GLOWSTONE_PRISM) && level.getBlockState(pos.above(2)).is(ModBlocks.GLOWSTONE_PRISM)) {
//            spawnFallingPrism(state, level, pos);
//        }
    }

    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        if (!canSurvive(state, level, pos)) {
            if (!level.isClientSide())
                spawnFallingPrism(state, (ServerLevel) level, pos);
            return state;
        }

        GlowstonePrismThickness glowstonePrismThicknessDown = calculateGlowstonePrismThickness(level, pos);

        return state.setValue(THICKNESS, glowstonePrismThicknessDown);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);

        level.setBlockAndUpdate(pos, state.setValue(THICKNESS, calculateGlowstonePrismThickness(level, pos)));
    }

    public static GlowstonePrismThickness getThicker(GlowstonePrismThickness thickness1, GlowstonePrismThickness thickness2) {
        if (thickness1.getSize() > thickness2.getSize()) {
            return  thickness1;
        } else {
            return  thickness2;
        }
    }

    public static GlowstonePrismThickness calculateGlowstonePrismThickness(LevelAccessor level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockPos below = pos.below();

        BlockState stateAbove = level.getBlockState(above);
        BlockState stateBelow = level.getBlockState(below);

        boolean isAbove = stateAbove.is(ModBlocks.GLOWSTONE_PRISM);
        boolean isBelow = stateBelow.is(ModBlocks.GLOWSTONE_PRISM);

        GlowstonePrismThickness thicknessAbove = isAbove ? stateAbove.getValue(THICKNESS) : GlowstonePrismThickness.TIP;
        GlowstonePrismThickness thicknessBelow = isBelow ? stateBelow.getValue(THICKNESS) : GlowstonePrismThickness.TIP;

        boolean sturdyAbove = stateAbove.isFaceSturdy(level, above, Direction.DOWN) && !isAbove;
        boolean sturdyBelow = stateBelow.isFaceSturdy(level, below, Direction.UP) && !isBelow;

        if (isAbove && !isBelow) {
            if (thicknessAbove == GlowstonePrismThickness.TIP) {
                return sturdyBelow ? GlowstonePrismThickness.MIDDLE : GlowstonePrismThickness.TIP;
            }

            return sturdyBelow ? GlowstonePrismThickness.BASE : GlowstonePrismThickness.TIP;
        }

        if (isBelow && !isAbove) {
            if (thicknessBelow == GlowstonePrismThickness.TIP) {
                return sturdyAbove ? GlowstonePrismThickness.MIDDLE : GlowstonePrismThickness.TIP;
            }

            return sturdyAbove ? GlowstonePrismThickness.BASE : GlowstonePrismThickness.TIP;
        }

        if (isBelow) {
            if (thicknessAbove != thicknessBelow) {
                GlowstonePrismThickness thicker = getThicker(thicknessAbove, thicknessBelow);

                switch (thicker) {
                    case BASE -> {
                        return thicker == thicknessAbove ? raiseThickness(thicknessBelow) : raiseThickness(thicknessAbove);
                    }
                    case MIDDLE -> {
                        return GlowstonePrismThickness.MIDDLE;
                    }
                }

            }
        }

        return thicknessAbove == GlowstonePrismThickness.MIDDLE ? GlowstonePrismThickness.TIP : thicknessAbove;
    }

    public static GlowstonePrismThickness raiseThickness(GlowstonePrismThickness thickness) {
        if (thickness == GlowstonePrismThickness.TIP) {
            return GlowstonePrismThickness.MIDDLE;
        }

        return GlowstonePrismThickness.BASE;
    }

    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.isClientSide) {
            BlockPos blockpos = hit.getBlockPos();
            if (projectile.mayInteract(level, blockpos) && projectile.mayBreak(level) && projectile.getDeltaMovement().length() > 0.6) {
                level.destroyBlock(blockpos, true);
            }
        }
    }

    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!this.canSurvive(state, level, pos)) {
            spawnFallingPrism(state, level, pos);
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isValidGlowstonePrismPlacement(level, pos, Direction.DOWN);
    }

    private static boolean isValidGlowstonePrismPlacement(LevelReader level, BlockPos pos, Direction dir) {
        BlockPos blockpos = pos.relative(dir.getOpposite());
        BlockState blockstate = level.getBlockState(blockpos);
        return blockstate.isFaceSturdy(level, blockpos, dir) || blockstate.is(ModBlocks.GLOWSTONE_PRISM);
    }

    private static boolean isTip(BlockState state) {
        if (!state.is(ModBlocks.GLOWSTONE_PRISM)) {
            return false;
        } else {
            GlowstonePrismThickness glowstonePrismThickness = state.getValue(THICKNESS);
            return glowstonePrismThickness == GlowstonePrismThickness.TIP;
        }
    }

    public DamageSource getFallDamageSource(Entity entity) {
        return entity.damageSources().fallingBlock(entity);
    }

    private static void spawnFallingPrism(BlockState state, ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pos.mutable();

        for(BlockState blockstate = state; level.getBlockState(pos.below()).isAir() && blockstate.is(ModBlocks.GLOWSTONE_PRISM); blockstate = level.getBlockState(blockpos$mutableblockpos)) {
            FallingBlockEntity fallingblockentity = FallingBlockEntity.fall(level, blockpos$mutableblockpos, blockstate);
            fallingblockentity.dropItem = false;
            blockpos$mutableblockpos.move(Direction.DOWN);
        }
    }

    public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity fallingBlock) {
        if (!fallingBlock.isSilent()) {
            level.levelEvent(1045, pos, 0);
        }

    }

    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction direction = Direction.DOWN;
        double d0 = (double) pos.getX() + 0.5 - random.nextInt(-40, 40) * 0.01;
        double d1 = (double) pos.getY() + 0.5 - (double) (random.nextFloat() * 0.1F);
        double d2 = (double) pos.getZ() + 0.5 - random.nextInt(-40, 40) * 0.01;
        double d3 = 0.4F - (random.nextFloat() + random.nextFloat()) * 0.5F;
        if (random.nextInt(1) == 0) {
            level.addParticle(ModParticles.GLOWSTONE_DUST.get(), d0 + (double) direction.getStepX() * d3, d1 + (double) direction.getStepY() * d3, d2 + (double) direction.getStepZ() * d3, random.nextGaussian() * 0.007, random.nextGaussian() * 0.007, random.nextGaussian() * 0.007);
        }
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return this.defaultBlockState().setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER).setValue(THICKNESS, calculateGlowstonePrismThickness(level, pos));
    }

    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.setBlockAndUpdate(pos, state.setValue(THICKNESS, calculateGlowstonePrismThickness(level, pos)));
        super.onPlace(state, level, pos, oldState, movedByPiston);
    }

    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        GlowstonePrismThickness glowstonePrismThickness = state.getValue(THICKNESS);

        if (glowstonePrismThickness == GlowstonePrismThickness.TIP) {
            return TIP_SHAPE;
        } else if (glowstonePrismThickness == GlowstonePrismThickness.MIDDLE) {
            return MIDDLE_SHAPE;
        } else {
            return BASE_SHAPE;
        }
    }

    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    protected boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    static {
        THICKNESS = EnumProperty.create("thickness", GlowstonePrismThickness.class);
        WATERLOGGED = BlockStateProperties.WATERLOGGED;
        TIP_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
        MIDDLE_SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);
        BASE_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    }
}
