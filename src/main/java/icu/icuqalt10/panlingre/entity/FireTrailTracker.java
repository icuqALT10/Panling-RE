package icu.icuqalt10.panlingre.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 火焰轨迹跟踪器 - 服务端和客户端通用
 * 用于跟踪龙卷风经过的位置，供逻辑检测使用
 */
public class FireTrailTracker {

    public static class TrailData {
        public final BlockPos pos;
        public final BlockState originalState;
        public int remainingTicks;

        public TrailData(BlockPos pos, BlockState originalState, int ticks) {
            this.pos = pos;
            this.originalState = originalState;
            this.remainingTicks = ticks;
        }
    }

    // 服务端和客户端各自维护独立的轨迹数据
    private static final Map<BlockPos, TrailData> ACTIVE_TRAILS = new HashMap<>();

    /**
     * 添加一个火焰轨迹位置
     * @param pos 方块位置
     * @param originalState 原始方块状态（可为 null）
     * @param ticks 持续时间（20 ticks = 1秒）
     */
    public static void addTrail(BlockPos pos, BlockState originalState, int ticks) {
        ACTIVE_TRAILS.put(pos, new TrailData(pos, originalState, ticks));
    }

    /**
     * 每 tick 更新轨迹数据（需要在适当的地方调用）
     */
    public static void tick() {
        if (ACTIVE_TRAILS.isEmpty()) return;

        Iterator<Map.Entry<BlockPos, TrailData>> iterator = ACTIVE_TRAILS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, TrailData> entry = iterator.next();
            TrailData trail = entry.getValue();
            trail.remainingTicks--;
            if (trail.remainingTicks <= 0) {
                iterator.remove();
            }
        }
    }

    /**
     * 检查指定位置是否在活跃的火焰轨迹中
     * @param pos 要检查的方块位置
     * @return 如果该位置在龙卷风经过的轨迹中返回 true
     */
    public static boolean isPositionInTrail(BlockPos pos) {
        return ACTIVE_TRAILS.containsKey(pos);
    }

    /**
     * 检查玩家是否站在火焰轨迹上
     * @param entity 要检查的玩家
     * @return 如果玩家脚下的任何位置在龙卷风轨迹中返回 true
     */
    public static boolean isEntityInTrail(Entity entity) {
        BlockPos entityPos = entity.blockPosition();

        // 检查玩家脚下的位置
        if (isPositionInTrail(entityPos)) {
            return true;
        }

        // 检查玩家脚下一格的位置
        if (isPositionInTrail(entityPos.below())) {
            return true;
        }

        // 检查玩家周围的位置（玩家碰撞箱可能跨越多个方块）
        BlockPos[] checkPositions = getBlockPos(entity);

        for (BlockPos pos : checkPositions) {
            if (isPositionInTrail(pos)) {
                return true;
            }
        }

        return false;
    }

    private static BlockPos @NotNull [] getBlockPos(Entity entity) {
        double x = entity.getX();
        double z = entity.getZ();
        double y = entity.getY();

        BlockPos[] checkPositions = {
            BlockPos.containing(x + 0.3, y, z + 0.3),
            BlockPos.containing(x + 0.3, y, z - 0.3),
            BlockPos.containing(x - 0.3, y, z + 0.3),
            BlockPos.containing(x - 0.3, y, z - 0.3),
            BlockPos.containing(x + 0.3, y - 1, z + 0.3),
            BlockPos.containing(x + 0.3, y - 1, z - 0.3),
            BlockPos.containing(x - 0.3, y - 1, z + 0.3),
            BlockPos.containing(x - 0.3, y - 1, z - 0.3)
        };
        return checkPositions;
    }

    /**
     * 获取指定位置轨迹的剩余时间
     * @param pos 要检查的方块位置
     * @return 剩余 tick 数，如果不在轨迹中返回 0
     */
    public static int getTrailRemainingTicks(BlockPos pos) {
        TrailData trail = ACTIVE_TRAILS.get(pos);
        return trail != null ? trail.remainingTicks : 0;
    }

    /**
     * 获取所有活跃的轨迹数据（供渲染器使用）
     */
    public static Map<BlockPos, TrailData> getActiveTrails() {
        return ACTIVE_TRAILS;
    }

    /**
     * 清空所有轨迹数据
     */
    public static void clear() {
        ACTIVE_TRAILS.clear();
    }
}
