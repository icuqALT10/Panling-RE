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
        if (boss.getActionState() != PanGuEntity.ActionState.IDLE_OR_WALK
        && boss.getActionState() != PanGuEntity.ActionState.ATTACK_COOLDOWN) return false;
        LivingEntity target = boss.getTarget();
        return !boss.isInAttackCooldown() && !boss.isAttacking() && (target != null && boss.distanceTo(target) < 10.0);
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

        if (boss.SkillPhase2Triggered) frenzied();
        else common();

        boss.setActionState(PanGuEntity.ActionState.ATTACKING);
    }

    private void common() {
        int random = boss.getRandom().nextInt(100);
        if (random >= 0 && random <= 29) {
            ticksLeft = 35;
            boss.startAnimation("attack.heavy");
        }
        else if (random >= 30 && random <= 59) {
            ticksLeft = 45;
            boss.startAnimation("attack.combo");
        }
        else if (random >= 60 && random <= 79) {
            ticksLeft = 50;
            boss.startAnimation("attack.skill3");
        }
        else if (random >= 80 && random <= 99) {
            ticksLeft = 50;
            boss.startAnimation("attack.skill4");
        }
    }

    private void frenzied() {
        int random = boss.getRandom().nextInt(100);
        if (random >= 0 && random <= 29) {
            ticksLeft = 35;
            boss.startAnimation("attack.skill3");
        }
        else if (random >= 30 && random <= 59) {
            ticksLeft = 45;
            boss.startAnimation("attack.skill4");
        }
        else if (random >= 60 && random <= 79) {
            ticksLeft = 50;
            boss.startAnimation("attack.heavy");
        }
        else if (random >= 80 && random <= 99) {
            ticksLeft = 50;
            boss.startAnimation("attack.combo");
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
            boss.setAttacking(false);
            boss.endAttack();
            boss.startAttackCooldown();
    }
}