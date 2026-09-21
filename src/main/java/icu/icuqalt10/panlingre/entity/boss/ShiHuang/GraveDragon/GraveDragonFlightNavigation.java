package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;

/**
 * 空中形态的寻路：原版的 3D 飞行 A*（{@code FlyNodeEvaluator}），节点按墓龙真实的身体轮廓判定。
 *
 * <p>这条路径就是用户要的「直线被挡时把路线改成曲线」——不需要自己写避障：A* 会在三维网格上
 * 绕过障碍，龙头沿着它飞，身体再通过脊柱链式跟随摆过去。
 *
 * <p>能这么直接复用的原因是 {@code FlyNodeEvaluator} 继承了 {@code WalkNodeEvaluator}，
 * 所以身体轮廓采样（{@link GraveDragonBodyPathing}）原样可用。
 */
public final class GraveDragonFlightNavigation extends FlyingPathNavigation {

    public GraveDragonFlightNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new GraveDragonBodyPathing.Flight();
        // 飞行不需要开门/浮水这些走地规则。
        this.nodeEvaluator.setCanPassDoors(false);
        this.nodeEvaluator.setCanFloat(false);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }
}
