package icu.icuqalt10.panlingre.block.zft;

import com.mojang.serialization.MapCodec;
import icu.icuqalt10.panlingre.block.ldl.ldlEntity;
import icu.icuqalt10.panlingre.world.inventory.ldlMenu;
import icu.icuqalt10.panlingre.world.inventory.zftMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class zft extends HorizontalDirectionalBlock implements EntityBlock {
    public static final EnumProperty<ChestType> PART = BlockStateProperties.CHEST_TYPE;

    public zft(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, ChestType.SINGLE));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(zft::new);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();

        BlockPos leftPos = pos.relative(facing.getCounterClockWise());
        BlockPos rightPos = pos.relative(facing.getClockWise());

        if (level.getBlockState(leftPos).canBeReplaced(context) &&
                level.getBlockState(rightPos).canBeReplaced(context)) {
            return this.defaultBlockState().setValue(FACING, facing).setValue(PART, ChestType.SINGLE);
        }

        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);

            level.setBlock(pos.relative(facing.getClockWise()),
                    state.setValue(PART, ChestType.LEFT), 3);

            level.setBlock(pos.relative(facing.getCounterClockWise()),
                    state.setValue(PART, ChestType.RIGHT), 3);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            ChestType part = state.getValue(PART);
            Direction facing = state.getValue(FACING);
            BlockPos middlePos = getMiddlePos(pos, part, facing);

            level.destroyBlock(middlePos, !player.isCreative());
            level.destroyBlock(middlePos.relative(facing.getCounterClockWise()), false);
            level.destroyBlock(middlePos.relative(facing.getClockWise()), false);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private BlockPos getMiddlePos(BlockPos pos, ChestType part, Direction facing) {
        if (part == ChestType.RIGHT) return pos.relative(facing.getClockWise());
        if (part == ChestType.LEFT) return pos.relative(facing.getCounterClockWise());
        return pos;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(PART) == ChestType.SINGLE ? super.getShadeBrightness(state, level, pos) : 1.0F;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new zftEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(PART) == ChestType.SINGLE ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ChestType part = state.getValue(PART);
            Direction facing = state.getValue(FACING);

            BlockPos middlePos = getMiddlePos(pos, part, facing);
            BlockState middleState = level.getBlockState(middlePos);

            if (middleState.is(this)) {
                serverPlayer.openMenu(new SimpleMenuProvider((id, inv, p) ->
                                new zftMenu(id, inv, ContainerLevelAccess.create(level, middlePos)),
                                Component.translatable("block.panlingre.zft")),
                        buf -> buf.writeBlockPos(middlePos));
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }
}