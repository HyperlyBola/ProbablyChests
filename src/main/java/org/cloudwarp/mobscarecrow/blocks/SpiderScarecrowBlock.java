package org.cloudwarp.mobscarecrow.blocks;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.cloudwarp.mobscarecrow.blockentities.MobScarecrowBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class SpiderScarecrowBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    public static final DirectionProperty FACING;
    protected static final VoxelShape NORTH_SHAPE;
    protected static final VoxelShape EAST_SHAPE;
    public SpiderScarecrowBlock(Settings settings){
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, net.minecraft.world.BlockView world, BlockPos pos, ShapeContext context) {
        Direction direction = (Direction)state.get(FACING);
        if(direction == Direction.NORTH || direction == Direction.SOUTH){
            return NORTH_SHAPE;
        }else{
            return EAST_SHAPE;
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        //world.syncWorldEvent(player, (Boolean)state.get(OPEN) ? this.getCloseSoundEventId() : this.getOpenSoundEventId(), pos, 0);
        // TODO: PLAY SOUND
        return ActionResult.success(world.isClient);
    }

    @Override
    public PistonBehavior getPistonBehavior(BlockState state) {
        return PistonBehavior.DESTROY;
    }


    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return (BlockState)this.getDefaultState().with(Properties.HORIZONTAL_FACING, ctx.getPlayerFacing().getOpposite());
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return  new MobScarecrowBlockEntity(pos, state);
    }
    static {
        FACING = HorizontalFacingBlock.FACING;
        NORTH_SHAPE = Stream.of(
                Block.createCuboidShape(4, 8, 6, 12, 16, 10),
                Block.createCuboidShape(7, 0, 7, 9, 8, 9)
        ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();
        EAST_SHAPE =Stream.of(
                Block.createCuboidShape(6, 8, 4, 10, 16, 12),
                Block.createCuboidShape(7, 0, 7, 9, 8, 9)
        ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();
    }
}