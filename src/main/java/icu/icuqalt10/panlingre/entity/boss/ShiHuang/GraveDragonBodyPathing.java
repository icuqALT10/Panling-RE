package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

import java.util.Set;

/**
 * 让墓龙的寻路（地面与空中）都按它**真实的身体**判定节点，而不是主体那个 1cm 的锚点盒子。
 *
 * <p>{@link WalkNodeEvaluator} 是按 {@code getBbWidth()}/{@code getBbHeight()} 来扫描节点的，
 * 对被强行缩成 1cm 的多节实体会得出"哪里都能走"的结论，AI 于是往 1 格缝隙、往墙里规划路线，
 * 每一步再被 {@code GraveDragonEntity#move} 的 OBB 判定否掉——表现就是贴着墙反复磨、不会绕路。
 *
 * <p>身体范围**每个 tick 从当前 OBB 现算**（{@code GraveDragonEntity#updateBodyFootprint}），
 * 不是固定常量：颈部摆动、抬头、张嘴都会改变真实轮廓。
 *
 * <p>直接照身体尺寸扫描是不可行的：{@code getPathTypeWithinMobBB} 会遍历
 * {@code entityWidth × entityHeight × entityDepth}，对这条龙是 47×31×47 ≈ 6.8 万次方块查询
 * **每个节点**，而一次寻路要访问上千个节点，直接把服务端卡住。所以这里改成**稀疏采样**：
 * 在身体当前轮廓上取横向 3 列 × 纵向 5 段 × 高度 4 层共 60 个点，开销固定，与身体多大无关。
 * 采样点按龙的当前朝向旋转，因此不需要把 46 格长的身体当成一个 46 格半径的圆。
 *
 * <p>关键复用点：原版的 {@link FlyNodeEvaluator} 是 {@link WalkNodeEvaluator} 的**子类**，
 * 所以空中形态可以沿用完全相同的身体采样，只是换成 3D 飞行节点评估器——这正是"把直线路径
 * 改成绕障碍的曲线"所依赖的 A*。
 *
 * <p>{@code move()} 里的 OBB 检查始终是权威判定，这里只负责让 AI 别规划出身体过不去的路线。
 */
final class GraveDragonBodyPathing {
    /** 横向 3 列、纵向 5 段、高度 4 层：约 60 次方块查询/节点，与身体多大无关。 */
    private static final int COLUMNS = 3;
    private static final int SEGMENTS = 5;
    private static final int HEIGHTS = 4;

    private GraveDragonBodyPathing() {
    }

    /** How many block lookups one node costs, for diagnostics and tests. */
    static int sampleCount() {
        return COLUMNS * SEGMENTS * HEIGHTS;
    }

    /**
     * 在 vanilla 的"节点自己那一格"判定之外，额外要求身体当前的轮廓也能站得住。
     *
     * <p>采样点直接加进 {@code getPathTypeWithinMobBB} 返回的类型集合里，后续由
     * {@link WalkNodeEvaluator#getPathTypeOfMob} 按 vanilla 的规则归并（栅栏、危险方块等），
     * 所以不需要复制那一整套逻辑。
     */
    private static Set<PathType> addBodySamples(WalkNodeEvaluator evaluator, Mob mob, PathfindingContext context,
                                                int x, int y, int z, Set<PathType> types) {
        if (!(mob instanceof GraveDragonEntity dragon) || !dragon.bodyFootprintValid()) return types;

        // The footprint was measured in the body's own frame, so rotate it to the heading the
        // collision boxes use; otherwise a 46-block body would have to be treated as a
        // 46-block-radius circle.
        double angle = -dragon.yBodyRot * Math.PI / 180.0;
        double cos = Math.cos(angle), sin = Math.sin(angle);
        double minX = dragon.bodyMinX(), maxX = dragon.bodyMaxX();
        double minZ = dragon.bodyMinZ(), maxZ = dragon.bodyMaxZ();
        double maxY = dragon.bodyMaxY();

        for (int level = 0; level < HEIGHTS; level++) {
            double localY = level == 0 ? 0.0 : level == HEIGHTS - 1 ? maxY + 1.0 : maxY * level / (HEIGHTS - 1.0);
            int sampleY = y + (int) Math.floor(localY);
            for (int column = 0; column < COLUMNS; column++) {
                double localX = minX + (maxX - minX) * column / (COLUMNS - 1.0);
                for (int segment = 0; segment < SEGMENTS; segment++) {
                    double localZ = minZ + (maxZ - minZ) * segment / (SEGMENTS - 1.0);
                    types.add(evaluator.getPathType(context,
                            x + (int) Math.round(localX * cos + localZ * sin),
                            sampleY,
                            z + (int) Math.round(-localX * sin + localZ * cos)));
                }
            }
        }
        return types;
    }

    /** 地面形态：走地节点评估器 + 身体轮廓。 */
    static final class Ground extends WalkNodeEvaluator {
        @Override
        public Set<PathType> getPathTypeWithinMobBB(PathfindingContext context, int x, int y, int z) {
            return addBodySamples(this, this.mob, context, x, y, z, super.getPathTypeWithinMobBB(context, x, y, z));
        }
    }

    /** 空中形态：3D 飞行节点评估器 + 同一套身体轮廓采样。 */
    static final class Flight extends FlyNodeEvaluator {
        @Override
        public Set<PathType> getPathTypeWithinMobBB(PathfindingContext context, int x, int y, int z) {
            return addBodySamples(this, this.mob, context, x, y, z, super.getPathTypeWithinMobBB(context, x, y, z));
        }
    }
}
