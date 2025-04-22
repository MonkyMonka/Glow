package com.monka.glow.block.custom;

import com.monka.glow.block.ModBlocks;
import com.monka.glow.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
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

    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        GlowstonePrismThickness glowstonePrismThicknessDown = calculateGlowstonePrismThickness(level, pos, Direction.DOWN);

        return state.setValue(THICKNESS, glowstonePrismThicknessDown);
    }

    public GlowstonePrismThickness getThicker (GlowstonePrismThickness thickness1, GlowstonePrismThickness thickness2) {
        if (thickness1.getSize() > thickness2.getSize())
        {
            return  thickness1;
        } else {
            return  thickness2;
        }
    }

    private static GlowstonePrismThickness calculateGlowstonePrismThickness(LevelReader level, BlockPos pos, Direction dir) {
        BlockState blockstate = level.getBlockState(pos.relative(dir));

        if (blockstate.is(ModBlocks.GLOWSTONE_PRISM)) {
            GlowstonePrismThickness glowstonePrismThickness = blockstate.getValue(THICKNESS);
            if (glowstonePrismThickness == GlowstonePrismThickness.TIP) {
                return GlowstonePrismThickness.MIDDLE;
            } else {
                return GlowstonePrismThickness.BASE;
            }
        } else {
            return GlowstonePrismThickness.TIP;
        }
        //ternary
        //condition ? valueIfTrue : valueIfFalse;
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
            level.destroyBlock(pos, true);
        } else {
            spawnFallingPrism(state, level, pos);
        }
    }

    private static boolean isTip(BlockState state) {
        if (!state.is(ModBlocks.GLOWSTONE_PRISM)) {
            return false;
        } else {
            GlowstonePrismThickness glowstonePrismThickness = state.getValue(THICKNESS);
            return glowstonePrismThickness == GlowstonePrismThickness.TIP;
        }
    }

    private static void spawnFallingPrism(BlockState state, ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pos.mutable();

        for(BlockState blockstate = state; blockstate.is(ModBlocks.GLOWSTONE_PRISM); blockstate = level.getBlockState(blockpos$mutableblockpos)) {
            FallingBlockEntity fallingblockentity = FallingBlockEntity.fall(level, blockpos$mutableblockpos, blockstate);
            if (isTip(state)) {
                int i = Math.max(1 + pos.getY() - blockpos$mutableblockpos.getY(), 6);
                float f = 1.0F * (float)i;
                fallingblockentity.setHurtsEntities(f, 40);
                break;
            }

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
        double d3 = (double) (0.4F - (random.nextFloat() + random.nextFloat()) * 0.5F);
        if (random.nextInt(1) == 0) {
            level.addParticle(ModParticles.GLOWSTONE_DUST.get(), d0 + (double) direction.getStepX() * d3, d1 + (double) direction.getStepY() * d3, d2 + (double) direction.getStepZ() * d3, random.nextGaussian() * 0.007, random.nextGaussian() * 0.007, random.nextGaussian() * 0.007);
        }
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return this.defaultBlockState().setValue(WATERLOGGED, world.getFluidState(pos).getType() == Fluids.WATER);
    }

    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
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
