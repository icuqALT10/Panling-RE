package icu.icuqalt10.panlingre.entity.boss.PanGu;

import icu.icuqalt10.panlingre.entity.boss.PanGuEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class ApproachTargetGoal extends Goal {
    private final PanGuEntity boss;

    public ApproachTargetGoal(PanGuEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = boss.getTarget();
        return target != null && !boss.isInAttackCooldown() && !boss.isAttacking()
                && boss.distanceTo(target) > 5.0;
    }

    @Override
    public void tick() {
        LivingEntity target = boss.getTarget();
        if (target == null) return;
        boss.getLookControl().setLookAt(target);
        boss.getNavigation().moveTo(target, 1.0);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }
}