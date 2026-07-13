package icu.icuqalt10.panlingre.entity.boss.PanGu;

import icu.icuqalt10.panlingre.entity.boss.PanGuEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class PostAttackBehaviorGoal extends Goal {
    private final PanGuEntity boss;

    public PostAttackBehaviorGoal(PanGuEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return (boss.getActionState() == PanGuEntity.ActionState.IDLE_OR_WALK || boss.getActionState() == PanGuEntity.ActionState.ATTACK_COOLDOWN)
                && boss.isInAttackCooldown();
    }

    @Override
    public void tick() {
        Player nearestPlayer = boss.level().getNearestPlayer(boss.getX(), boss.getY(), boss.getZ(), 80.0D, false);

        if (nearestPlayer != null) {
            boss.getLookControl().setLookAt(nearestPlayer, 80.0F, 40.0F);

            double distSq = boss.distanceToSqr(nearestPlayer);

            if (distSq > 900.0D) {
                if (boss.getNavigation().isDone()) {
                    boss.getNavigation().moveTo(nearestPlayer, 1.0D);
                }
            } else {
                if (boss.getNavigation().isDone() && boss.attackCooldown > 5) {
                    steerWanderAroundPlayer(nearestPlayer);
                }
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    private void steerWanderAroundPlayer(Player targetPlayer) {
        // 计算从 Boss 指向最近玩家的水平向量
        Vec3 toTarget = new Vec3(targetPlayer.getX() - boss.getX(), 0, targetPlayer.getZ() - boss.getZ()).normalize();

        // 随机左右绕圈
        boolean goLeft = boss.getRandom().nextBoolean();
        Vec3 walkDirection = goLeft ?
                new Vec3(-toTarget.z, 0, toTarget.x) :
                new Vec3(toTarget.z, 0, -toTarget.x);

        // 计算切线目的地
        double stride = 4.0 + boss.getRandom().nextDouble() * 2.0;
        Vec3 destPos = boss.position().add(walkDirection.scale(stride));

        boss.getNavigation().moveTo(destPos.x, destPos.y, destPos.z, 0.75D);
    }
}