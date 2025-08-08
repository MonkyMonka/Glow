package com.monka.glow.glowstone;

import com.monka.glow.GlowRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public class GlowstonePrismBlock extends Block implements Fallable, SimpleWaterloggedBlock {
    public static final VoxelShape BASE_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    public static final VoxelShape MIDDLE_SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);
    public static final VoxelShape TIP_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<GlowstonePrismThickness> THICKNESS = EnumProperty.create("thickness", GlowstonePrismThickness.class);

    public GlowstonePrismBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(THICKNESS, GlowstonePrismThickness.TIP).setValue(WATERLOGGED, false));
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
            else if (i < baseCount + middleCount) thickness = GlowstonePrismThickness.MIDDLE;
            else thickness = GlowstonePrismThickness.TIP;
            updateIfNeeded(level, column.get(i), thickness);
        }
    }

    private static void updateIfNeeded(LevelAccessor level, BlockPos pos, GlowstonePrismThickness thickness) {
        BlockState state = level.getBlockState(pos);

        if (state.is(GlowRegistry.GLOWSTONE_PRISM.get()) && state.getValue(GlowstonePrismBlock.THICKNESS) != thickness)
            level.setBlock(pos, state.setValue(GlowstonePrismBlock.THICKNESS, thickness), 3);
    }

    public static List<BlockPos> getGlowstoneColumn(BlockGetter level, BlockPos origin) {
        BlockPos.MutableBlockPos mutablePos = origin.mutable();
        int originY = origin.getY();

        int minY = originY;
        while (level.getBlockState(mutablePos.set(origin.getX(), minY - 1, origin.getZ())).is(GlowRegistry.GLOWSTONE_PRISM.get()))
            minY--;

        int maxY = originY;
        while (level.getBlockState(mutablePos.set(origin.getX(), maxY + 1, origin.getZ())).is(GlowRegistry.GLOWSTONE_PRISM.get()))
            maxY++;


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

        } else if (topSupported) {
            for (int y = maxY; y >= minY; y--)
                column.add(BlockPos.containing(origin.getX(), y, origin.getZ()));
        } else {
            for (int y = minY; y <= maxY; y++)
                column.add(BlockPos.containing(origin.getX(), y, origin.getZ()));
        }

        return column;
    }

    public static void spawnFallingPrism(ServerLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos mutablePos = origin.mutable();

        while (true) {
            BlockState state = level.getBlockState(mutablePos);
            if (!state.is(GlowRegistry.GLOWSTONE_PRISM.get())) break;

            FallingGlowstoneEntity fallingGlowstone = FallingGlowstoneEntity.fall(level, mutablePos, state);
            fallingGlowstone.setHurtsEntities(2, 40);
            fallingGlowstone.disableDrop();

            mutablePos.move(Direction.DOWN);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(THICKNESS, WATERLOGGED);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = level;
            if (random.nextFloat() < 0.03 && level.isEmptyBlock(pos.below()))
                level.setBlockAndUpdate(pos.below(), GlowRegistry.GLOWSTONE_PRISM.get().defaultBlockState());

            if (random.nextFloat() < 0.01 && level.getBlockState(pos.above()).is(GlowRegistry.GLOWSTONE_PRISM.get()) && level.getBlockState(pos.above(2)).is(GlowRegistry.GLOWSTONE_PRISM.get()))
                spawnFallingPrism(serverLevel, pos);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        if (shouldFall(level, pos)) {
            level.scheduleTick(pos, this, 2);
        } else applyThicknessGradient(level, getGlowstoneColumn(level, pos));

        return state;
    }

    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level instanceof ServerLevel serverLevel) {
            BlockPos pos = hit.getBlockPos();
            if (projectile.mayInteract(level, pos) && projectile.getDeltaMovement().length() > 0.6) {
                level.destroyBlock(pos, true);
                spawnFallingPrism(serverLevel, pos);
            }
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (shouldFall(level, pos)) spawnFallingPrism(level, pos);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, 2);
    }

    public boolean shouldFall(LevelReader level, BlockPos pos) {
        List<BlockPos> column = getGlowstoneColumn(level, pos);
        if (column.isEmpty() || !column.contains(pos)) return true;

        BlockPos end1 = column.get(0);
        BlockPos end2 = column.get((column.size() - 1));

        BlockPos bottom = end1.getY() <= end2.getY() ? end1 : end2;
        BlockPos top = end1.getY() > end2.getY() ? end1 : end2;

        boolean bottomSupported =
                level.getBlockState(bottom.below()).isFaceSturdy(level, bottom.below(), Direction.UP);
        boolean topSupported =
                level.getBlockState(top.above()).isFaceSturdy(level, top.above(), Direction.DOWN);

        return !bottomSupported && !topSupported;
    }

    @Override
    public DamageSource getFallDamageSource(Entity entity) {
        return entity.damageSources().fallingBlock(entity);
    }

    @Override
    public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity fallingBlock) {
        if (!fallingBlock.isSilent()) {
            if (level instanceof ServerLevel serverLevel) {
                double distance = Math.sqrt(pos.distSqr(fallingBlock.getStartPos()));
                int size = fallingBlock.getBlockState().getValue(THICKNESS).getSize() + 1;
                serverLevel.playSound(null, pos, SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.BLOCKS, (float) size, (float) 5 / size);
                serverLevel.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, (float) size, (float) 0.25 / size);

                serverLevel.sendParticles(GlowRegistry.GLOWSTONE_DUST.get(), pos.getX() + 0.5 + level.random.nextFloat() - level.random.nextFloat(), pos.getY() + 1 + level.random.nextFloat() - level.random.nextFloat(), pos.getZ() + 0.5 + level.random.nextFloat() - level.random.nextFloat(), (int) (distance * 25 * size * level.random.nextFloat()), 0, 0, 0, 0.25 * size * level.random.nextFloat());
                serverLevel.sendParticles(ParticleTypes.SMALL_FLAME, pos.getX() + 0.5 + level.random.nextFloat() - level.random.nextFloat(), pos.getY() + level.random.nextFloat() - level.random.nextFloat() + 1, pos.getZ() + 0.5 + level.random.nextFloat() - level.random.nextFloat(), (int) (distance * 25 * size * level.random.nextFloat()), 0, 0, 0, 0.1 * size * level.random.nextFloat());
                if (size > 1) serverLevel.sendParticles(ParticleTypes.FLASH, pos.getX() + level.random.nextFloat() - level.random.nextFloat() + 0.5, pos.getY() + level.random.nextFloat() - level.random.nextFloat() + 1, pos.getZ() + level.random.nextFloat() - level.random.nextFloat() + 0.5, 1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction direction = Direction.DOWN;
        double d0 = (double) pos.getX() + 0.5 - random.nextInt(-40, 40) * 0.01;
        double d1 = (double) pos.getY() + 0.5 - (double) (random.nextFloat() * 0.1F);
        double d2 = (double) pos.getZ() + 0.5 - random.nextInt(-40, 40) * 0.01;
        double d3 = 0.4F - (random.nextFloat() + random.nextFloat()) * 0.5F;
        if (random.nextInt(1) == 0) {
            level.addParticle(GlowRegistry.GLOWSTONE_DUST.get(), d0 + (double) direction.getStepX() * d3, d1 + (double) direction.getStepY() * d3, d2 + (double) direction.getStepZ() * d3, random.nextGaussian() * 0.007, random.nextGaussian() * 0.007, random.nextGaussian() * 0.007);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        return this.defaultBlockState().setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        GlowstonePrismThickness glowstonePrismThickness = state.getValue(THICKNESS);

        if (glowstonePrismThickness == GlowstonePrismThickness.TIP) {
            return TIP_SHAPE;
        } else if (glowstonePrismThickness == GlowstonePrismThickness.MIDDLE) {
            return MIDDLE_SHAPE;
        } else {
            return BASE_SHAPE;
        }
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType pathComputationType) {
        return false;
    }
}
