package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;

/**
 * 地面形态的寻路：走地 A*，节点按墓龙真实的身体轮廓判定。
 * 具体的采样逻辑见 {@link GraveDragonBodyPathing}（与空中形态共用）。
 */
public final class GraveDragonPathNavigation extends GroundPathNavigation {

    public GraveDragonPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new GraveDragonBodyPathing.Ground();
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }
}
