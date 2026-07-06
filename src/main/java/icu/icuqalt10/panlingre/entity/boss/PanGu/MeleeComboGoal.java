package icu.icuqalt10.panlingre.entity.boss.PanGu;

import icu.icuqalt10.panlingre.entity.boss.PanGuEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class MeleeComboGoal extends Goal {
    private final PanGuEntity boss;
    private int ticksLeft;

    public MeleeComboGoal(PanGuEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean isInterruptable() {
        return false; // 攻击过程中不允许被其他Goal打断
    }

    @Override
    public boolean canUse() {
        boolean cd = boss.isInAttackCooldown();
        boolean atk = boss.isAttacking();
        LivingEntity target = boss.getTarget();
        boolean dist = target != null && boss.distanceTo(target) < 5.0;
        boolean result = !cd && !atk && dist;
        return result;
    }

    @Override
    public void start() {
        LivingEntity target = boss.getTarget();
        boss.setAttacking(true);
        boss.setAttackTarget(target);
        boss.beginAttack(PanGuEntity.AttackKind.MELEE);
        boss.getNavigation().stop();

        Vec3 toTarget = target.position().subtract(boss.position());
        float yaw = (float) Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z));
        boss.setYRot(yaw);
        boss.setYBodyRot(yaw);

        boolean heavy = boss.getRandom().nextBoolean();
        ticksLeft = heavy ? 35 : 45; // 略大于动画长度,留余量
        boss.triggerAnim("attack_controller", heavy ? "attack.heavy" : "attack.combo");
    }

    @Override
    public void tick() {
        ticksLeft--;
    }

    @Override
    public boolean canContinueToUse() {
        return ticksLeft > 0 && !boss.cooldownStartedThisAttack;
    }

    @Override
    public void stop() {
        boss.setAttacking(false);
        boss.endAttack();
        boss.startAttackCooldown();
    }
}