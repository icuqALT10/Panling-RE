package icu.icuqalt10.panlingre.entity.boss.PanGu;

import icu.icuqalt10.panlingre.entity.boss.PanGuEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class Heavy2Goal extends Goal {
    private final PanGuEntity boss;
    private int ticksLeft = 50;

    public Heavy2Goal(PanGuEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean isInterruptable() {
        return false; // 攻击过程中不允许被其他Goal打断
    }

    @Override
    public boolean canUse() {
        if (boss.getActionState() != PanGuEntity.ActionState.IDLE_OR_WALK
                && boss.getActionState() != PanGuEntity.ActionState.ATTACK_COOLDOWN) return false;
        LivingEntity target = boss.getTarget();
        return !boss.isInAttackCooldown() && !boss.isAttacking() && target != null && boss.distanceTo(target) >= 5.0 && boss.distanceTo(target) <= 15.0 && boss.LastAttackIsCommon;
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

        ticksLeft = 50;
        int random = boss.getRandom().nextInt(100);
        if (random >= 0 && random <= 29) {
            boss.triggerAnim("action_controller","attack.heavy2");
        }
        else if (random >= 30 && random <= 64) {
            boss.triggerAnim("action_controller","attack.skill1");
        }
        else if (random >= 65 && random <= 99) {
            boss.triggerAnim("action_controller","attack.skill2");
        }
    }

    @Override
    public void tick() {
        ticksLeft--;
    }

    @Override
    public boolean canContinueToUse() {
        return boss.getActionState() == PanGuEntity.ActionState.ATTACKING
                && ticksLeft > 0
                && !boss.cooldownStartedThisAttack;
    }

    @Override
    public void stop() {
        if (boss.getActionState() == PanGuEntity.ActionState.ATTACKING) {
            boss.setAttacking(false);
            boss.endAttack();
            boss.startAttackCooldown();
        }
    }
}