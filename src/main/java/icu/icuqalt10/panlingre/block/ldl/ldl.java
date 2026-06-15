package icu.icuqalt10.panlingre.block.ldl;

import com.mojang.serialization.MapCodec;
import icu.icuqalt10.panlingre.init.ModBlocks;
import icu.icuqalt10.panlingre.player.check;
import icu.icuqalt10.panlingre.world.inventory.ldlMenu;
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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ldl extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(ldl::new);
    }

    public ldl(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos center = context.getClickedPos();

        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos checkPos = center.offset(x, y, z);
                    if (!level.getBlockState(checkPos).canBeReplaced(context)) {
                        return null;
                    }
                }
            }
        }
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y <= 2; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        level.setBlock(pos.offset(x, y, z), ModBlocks.ldl_proxy.get().defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 2, 1))) {
                if (level.getBlockState(p).getBlock() instanceof ldlProxyBlock) {
                    level.removeBlock(p, false);
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ldlEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && check.zhiye_check(player, "panlingre:warlock")) {
            serverPlayer.openMenu(new SimpleMenuProvider((id, inv, p) ->
                            new ldlMenu(id, inv, ContainerLevelAccess.create(level, pos)),
                            Component.translatable("block.panlingre.ldl")),
                    buf -> buf.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }
}