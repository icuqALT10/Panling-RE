package icu.icuqalt10.panlingre.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

public record BlockSet(ServerLevel level) {

    public void doClone(BlockPos srcMin,BlockPos srcMax,BlockPos destOrigin) {
        // 计算坐标偏移量
        int offsetX = destOrigin.getX() - srcMin.getX();
        int offsetY = destOrigin.getY() - srcMin.getY();
        int offsetZ = destOrigin.getZ() - srcMin.getZ();

        // 遍历源区域内的每一个坐标
        for (BlockPos srcPos : BlockPos.betweenClosed(srcMin, srcMax)) {
            // 计算对应的目标方块坐标
            BlockPos destPos = new BlockPos(
                    srcPos.getX() + offsetX,
                    srcPos.getY() + offsetY,
                    srcPos.getZ() + offsetZ
            );
            // 抓取源方块的状态
            BlockState state = level.getBlockState(srcPos);

            // 过滤空气
            if (state.isAir()) continue;

            // 将方块写入目标位置
            // 参数 3 是特殊标志位（Flag），代表：更新方块并同步给客户端
            level.setBlock(destPos, state, 3);

            // 如果源方块带有容器或特殊数据
            BlockEntity srcTag = level.getBlockEntity(srcPos);
            if (srcTag != null) {
                BlockEntity destTag = level.getBlockEntity(destPos);
                if (destTag != null) {
                    // 将旧容器的数据标签复制给新容器
                    destTag.loadWithComponents(srcTag.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
                    destTag.setChanged(); // 标记更新
                }
            }
        }
    }

    public void doFill(BlockPos minPos, BlockPos maxPos, BlockState fillState) {
        for (BlockPos currentPos : BlockPos.betweenClosed(minPos, maxPos)) {
            level.setBlock(currentPos, fillState, 3);
        }
    }

    public void doFill(BlockPos minPos, BlockPos maxPos, Block block) {
        doFill(minPos, maxPos, block.defaultBlockState());
    }
}
