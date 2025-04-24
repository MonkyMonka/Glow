package com.monka.glow.block.custom;

import com.monka.glow.block.ModBlocks;
import com.monka.glow.particle.ModParticles;
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
import java.util.ArrayList;
import java.util.List;

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
//        if (random.nextFloat() < 0.012 && level.isEmptyBlock(pos.below()))
//            level.setBlockAndUpdate(pos.below(), ModBlocks.GLOWSTONE_PRISM.get().defaultBlockState());

//        if (random.nextFloat() < 0.05 && state.getValue(THICKNESS) == GlowstonePrismThickness.BASE && level.getBlockState(pos.above()).is(ModBlocks.GLOWSTONE_PRISM) && level.getBlockState(pos.above(2)).is(ModBlocks.GLOWSTONE_PRISM)) {
//            spawnFallingPrism(state, level, pos);
//        }
    }

    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        if (shouldFall(level, pos)) {
            level.scheduleTick(pos, this, 1);
        } else applyThicknessGradient(level, getGlowstoneColumn(level, pos));

        return state;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    }

    public static void applyThicknessGradient(LevelAccessor level, List<BlockPos> column) {
        int size = column.size();
        if (size == 0) return;

        if (size <= 3) {
            for (int i = 0; i < size; i++) {
                GlowstonePrismThickness thickness = (i == size - 1)
                        ? GlowstonePrismThickness.TIP
                        : GlowstonePrismThickness.MIDDLE;

                updateIfNeeded(level, column.get(i), thickness);
            }
            
            return;
        }

        if (size == 4) {
            GlowstonePrismThickness[] pattern = {
                    GlowstonePrismThickness.BASE,
                    GlowstonePrismThickness.MIDDLE,
                    GlowstonePrismThickness.MIDDLE,
                    GlowstonePrismThickness.TIP
            };

            for (int i = 0; i < 4; i++)
                updateIfNeeded(level, column.get(i), pattern[i]);

            return;
        }

        int baseCount = Math.max(1, (int) (size * 0.4));
        int middleCount = Math.max(1, (int) (size * 0.4));

        if (baseCount + middleCount >= size)
            middleCount = size - baseCount - 1;

        for (int i = 0; i < size; i++) {
            GlowstonePrismThickness thickness;
            if (i < baseCount) thickness = GlowstonePrismThickness.BASE;
            else if (i < baseCount + middleCount)     thickness = GlowstonePrismThickness.MIDDLE;
            else thickness = GlowstonePrismThickness.TIP;
            updateIfNeeded(level, column.get(i), thickness);
        }
    }

    private static void updateIfNeeded(LevelAccessor level, BlockPos pos, GlowstonePrismThickness thickness) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.GLOWSTONE_PRISM) && state.getValue(GlowstonePrismBlock.THICKNESS) != thickness) {
            level.setBlock(pos, state.setValue(GlowstonePrismBlock.THICKNESS, thickness), 3);
        }
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
        if (this.shouldFall(level, pos)) {
            spawnFallingPrism(level, pos);
        }
    }

    public static List<BlockPos> getGlowstoneColumn(BlockGetter level, BlockPos origin) {
        BlockPos.MutableBlockPos mutablePos = origin.mutable();
        int originY = origin.getY();

        int minY = originY;
        while (level.getBlockState(mutablePos.set(origin.getX(), minY - 1, origin.getZ())).is(ModBlocks.GLOWSTONE_PRISM)) {
            minY--;
        }

        int maxY = originY;
        while (level.getBlockState(mutablePos.set(origin.getX(), maxY + 1, origin.getZ())).is(ModBlocks.GLOWSTONE_PRISM)) {
            maxY++;
        }

        boolean bottomSupported = level.getBlockState(mutablePos.set(origin.getX(), minY - 1, origin.getZ()))
                .isFaceSturdy(level, mutablePos, Direction.UP);
        boolean topSupported = level.getBlockState(mutablePos.set(origin.getX(), maxY + 1, origin.getZ()))
                .isFaceSturdy(level, mutablePos, Direction.DOWN);

        List<BlockPos> column = new ArrayList<>(maxY - minY + 1);

        if (bottomSupported && topSupported) {
            int up = minY;
            int down = maxY;

            while (up <= down) {
                if (up != down) {
                    column.add(BlockPos.containing(origin.getX(), up, origin.getZ()));
                    column.add(BlockPos.containing(origin.getX(), down, origin.getZ()));
                } else {
                    column.add(BlockPos.containing(origin.getX(), up, origin.getZ())); // center (odd count)
                }
                up++;
                down--;
            }

        } else if (bottomSupported) {
            for (int y = minY; y <= maxY; y++) {
                column.add(BlockPos.containing(origin.getX(), y, origin.getZ()));
            }
        } else if (topSupported) {
            for (int y = maxY; y >= minY; y--) {
                column.add(BlockPos.containing(origin.getX(), y, origin.getZ()));
            }
        } else {
            for (int y = minY; y <= maxY; y++) {
                column.add(BlockPos.containing(origin.getX(), y, origin.getZ()));
            }
        }

        return column;
    }

    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    public boolean shouldFall(LevelReader level, BlockPos pos) {
        List<BlockPos> column = getGlowstoneColumn(level, pos);
        if (column.isEmpty() || !column.contains(pos)) return true;

        BlockPos end1 = column.getFirst();
        BlockPos end2 = column.getLast();

        BlockPos bottom = end1.getY() <= end2.getY() ? end1 : end2;
        BlockPos top = end1.getY() >  end2.getY() ? end1 : end2;

        boolean bottomSupported =
                level.getBlockState(bottom.below()).isFaceSturdy(level, bottom.below(), Direction.UP);
        boolean topSupported =
                level.getBlockState(top.above()).isFaceSturdy(level, top.above(), Direction.DOWN);

        return !bottomSupported && !topSupported;
    }

    public DamageSource getFallDamageSource(Entity entity) {
        return entity.damageSources().fallingBlock(entity);
    }

    public static void spawnFallingPrism(ServerLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = origin.mutable();

        while (true) {
            BlockState state = level.getBlockState(cursor);
            if (!state.is(ModBlocks.GLOWSTONE_PRISM)) break;

            FallingBlockEntity falling = FallingBlockEntity.fall(level, cursor, state);
            falling.dropItem = false;

            cursor.move(Direction.DOWN);
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

        return this.defaultBlockState().setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
    }

    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, 1);
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
        return state.getValue(THICKNESS) == GlowstonePrismThickness.BASE;
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
