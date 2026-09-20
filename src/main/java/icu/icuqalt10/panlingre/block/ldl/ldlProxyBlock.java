package icu.icuqalt10.panlingre.block.ldl;

import icu.icuqalt10.panlingre.attachment.ZhiyeData;
import icu.icuqalt10.panlingre.world.inventory.ldlMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ldlProxyBlock extends Block {
    public ldlProxyBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && ZhiyeData.has(player, ZhiyeData.Profession.WARLOCK)) {
            BlockPos masterPos = null;
            for (BlockPos neighborPos : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
                if (level.getBlockState(neighborPos).getBlock() instanceof ldl) {
                    masterPos = neighborPos.immutable();
                    break;
                }
            }

            if (masterPos != null) {
                final BlockPos finalMasterPos = masterPos;
                serverPlayer.openMenu(new SimpleMenuProvider((id, inv, p) ->
                                new ldlMenu(id, inv, ContainerLevelAccess.create(level, finalMasterPos)),
                                Component.translatable("block.panlingre.ldl")),
                        buf -> buf.writeBlockPos(finalMasterPos));
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            for (BlockPos masterPos : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
                if (level.getBlockState(masterPos).getBlock() instanceof ldl) {
                    // Let the master perform the actual drop.  In creative
                    // mode this must be false, otherwise the 3x3 structure
                    // incorrectly drops an item.
                    level.destroyBlock(masterPos, !player.isCreative());
                    break;
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
                if (level.getBlockState(p).getBlock() instanceof ldl) {
                    // A proxy has no loot of its own.  If the master is being
                    // removed by a player, playerWillDestroy above already
                    // performed the correctly gated drop.
                    level.destroyBlock(p, false);
                    break;
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
