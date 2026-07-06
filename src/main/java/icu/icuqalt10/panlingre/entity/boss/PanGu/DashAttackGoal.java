package icu.icuqalt10.panlingre.entity.boss.PanGu;

import icu.icuqalt10.panlingre.entity.boss.PanGuEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class DashAttackGoal extends Goal {
    private final PanGuEntity boss;
    private int totalTicks; // 整个attack.throw动画长度,3s=60tick

    private static final double MIN_DISTANCE = 5.0;
    private static final double MAX_DISTANCE = 15.0;

    public DashAttackGoal(PanGuEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean isInterruptable() {
        return false; // 攻击过程中不允许被其他Goal打断
    }

    @Override
    public boolean canUse() {
        if (boss.isInAttackCooldown() || boss.isAttacking()) return false;
        LivingEntity target = boss.getTarget();
        if (target == null) return false;
        double dist = boss.distanceTo(target);
        if (dist < MIN_DISTANCE || dist > MAX_DISTANCE) return false;
        return !isBlocked(target);
    }

    private boolean isBlocked(LivingEntity target) {
        HitResult result = boss.level().clip(new ClipContext(
                boss.position().add(0, boss.getBbHeight() / 2, 0),
                target.position().add(0, 0.5, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
        return result.getType() != HitResult.Type.MISS;
    }

    @Override
    public void start() {
        LivingEntity target = boss.getTarget();
        boss.setAttacking(true);
        boss.setAttackTarget(target);
        boss.beginAttack(PanGuEntity.AttackKind.DASH);
        boss.getNavigation().stop();
        totalTicks = 45;
        boss.getLookControl().setLookAt(target);
        boss.triggerAnim("attack_controller", "attack.throw");
    }

    @Override
    public void tick() {
        LivingEntity target = boss.getTarget();
        if (target != null && !boss.isDashMoving()) {
            // 起手阶段强制转身正对目标,而不只是转头
            Vec3 toTarget = target.position().subtract(boss.position());
            float yaw = (float) Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z));
            boss.setYRot(yaw);
            boss.setYBodyRot(yaw);
        }
        totalTicks--;
    }

    @Override
    public boolean canContinueToUse() {
        return totalTicks > 0 && !boss.cooldownStartedThisAttack;
    }

    @Override
    public void stop() {
        boss.setAttacking(false);
        boss.endAttack();
        boss.startAttackCooldown();
    }
}