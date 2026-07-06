package icu.icuqalt10.panlingre.entity.boss.PanGu;

import icu.icuqalt10.panlingre.entity.boss.PanGuEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class PostAttackBehaviorGoal extends Goal {
    private final PanGuEntity boss;
    private int wanderCooldown;

    public PostAttackBehaviorGoal(PanGuEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return boss.isInAttackCooldown() && boss.getTarget() != null;
    }

    @Override
    public void tick() {
        LivingEntity target = boss.getTarget();
        boss.getLookControl().setLookAt(target); // getMaxHeadYRot()=90已经在entity里限制了角度

        if (--wanderCooldown <= 0) {
            wanderCooldown = 20 + boss.getRandom().nextInt(20);
            Vec3 randomOffset = new Vec3(
                    (boss.getRandom().nextDouble() - 0.5) * 6,
                    0,
                    (boss.getRandom().nextDouble() - 0.5) * 6);
            Vec3 wanderTarget = boss.position().add(randomOffset);
            boss.getNavigation().moveTo(wanderTarget.x, wanderTarget.y, wanderTarget.z, 1.0);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }
}