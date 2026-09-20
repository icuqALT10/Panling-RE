package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

import java.util.EnumSet;
import java.util.Set;

/**
 * 让墓龙的寻路按它**真实的身体**判定节点能不能站，而不是按主体那个 1cm 的锚点盒子。
 *
 * <p>{@link WalkNodeEvaluator} 是按 {@code getBbWidth()}/{@code getBbHeight()} 来扫描节点的，
 * 对被强行缩成 1cm 的多节实体会得出"哪里都能走"的结论，AI 于是往 1 格缝隙、往墙里规划路线，
 * 每一步再被 {@code GraveDragonEntity#move} 的 OBB 判定否掉——表现就是贴着墙反复磨、不会绕路。
 *
 * <p>直接照身体尺寸扫描是不可行的：{@code getPathTypeWithinMobBB} 会遍历
 * {@code entityWidth × entityHeight × entityDepth}，对这条龙是 47×31×47 ≈ 6.8 万次方块查询
 * **每个节点**，而一次寻路要访问上千个节点，直接把服务端卡住。所以这里改成**稀疏采样**：
 * 只在身体长条的轮廓上取横向 3 列 × 纵向 5 段 × 高度 4 层共 60 个点，开销固定，与身体多大无关。
 * 采样点按龙的当前朝向旋转，因此不需要把 46 格长的身体当成一个 46 格半径的圆。
 *
 * <p>{@code move()} 里的 OBB 检查始终是权威判定，这里只负责让 AI 别规划出身体过不去的路线。
 */
public final class GraveDragonPathNavigation extends GroundPathNavigation {

    public GraveDragonPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new BodyAwareWalkNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    /**
     * 在 vanilla 的"节点自己那一格"判定之外，额外要求身体的轮廓也能站得住。
     *
     * <p>采样点直接加进 {@code getPathTypeWithinMobBB} 返回的类型集合里，后续由
     * {@link WalkNodeEvaluator#getPathTypeOfMob} 按 vanilla 的规则归并（栅栏、危险方块等），
     * 所以不需要复制那一整套逻辑。
     */
    static final class BodyAwareWalkNodeEvaluator extends WalkNodeEvaluator {
        private static final float HALF_WIDTH = GraveDragonEntity.pathingHalfWidth();
        private static final float NOSE = GraveDragonEntity.pathingNose();
        private static final float TAIL = GraveDragonEntity.pathingTail();
        private static final float HEIGHT = GraveDragonEntity.pathingHeight();

        /** 横向 3 列 × 纵向 5 段 × 高度 4 层，约 60 次方块查询/节点，与身体多大无关。 */
        private static final float[] LOCAL_X = {-HALF_WIDTH, 0.0F, HALF_WIDTH};
        private static final float[] LOCAL_Z = {-NOSE, -NOSE * 0.75F, -NOSE * 0.5F, -NOSE * 0.25F, TAIL};
        private static final float[] LOCAL_Y = {0.0F, HEIGHT * 0.5F, HEIGHT, HEIGHT + 1.0F};

        @Override
        public Set<PathType> getPathTypeWithinMobBB(PathfindingContext context, int x, int y, int z) {
            Set<PathType> types = EnumSet.noneOf(PathType.class);
            // Keep vanilla's verdict for the node itself (doors, rails, the anchor column).
            types.addAll(super.getPathTypeWithinMobBB(context, x, y, z));
            if (this.mob == null) return types;

            // The footprint is measured in the body's own frame, so rotate it to the heading the
            // collision boxes use: modelToEntity(yaw) = R_y(-yaw) * modelToEntity(0).
            float scale = this.mob.getScale();
            double angle = -this.mob.yBodyRot * Math.PI / 180.0;
            double cos = Math.cos(angle), sin = Math.sin(angle);
            for (float dy : LOCAL_Y) {
                int sy = y + (int) Math.floor(dy);
                for (float lx : LOCAL_X) {
                    for (float lz : LOCAL_Z) {
                        double sx = lx * scale, sz = lz * scale;
                        types.add(this.getPathType(context,
                                x + (int) Math.round(sx * cos + sz * sin),
                                sy,
                                z + (int) Math.round(-sx * sin + sz * cos)));
                    }
                }
            }
            return types;
        }

        /** How many block lookups one node costs, for diagnostics and tests. */
        static int sampleCount() {
            return LOCAL_X.length * LOCAL_Z.length * LOCAL_Y.length;
        }
    }
}
