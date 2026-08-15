package icu.icuqalt10.panlingre.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

public class SkillHelper {
    public static List<LivingEntity> getLivingEntitiesInFront(LivingEntity source, double width, double height, double length) {
        Level world = source.level();
        if (world.isClientSide) {
            return List.of();
        }

        Vec3 origin = source.position();
        double rad = Math.toRadians(source.getYRot());
        Vec3 forward = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
        Vec3 right = new Vec3(Math.cos(rad), 0, Math.sin(rad));
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 center = origin.add(forward.scale(length / 2.0));

        double halfLength = length / 2.0;
        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;
        double maxRadius = Math.sqrt(halfLength * halfLength + halfWidth * halfWidth + halfHeight * halfHeight);
        AABB bounds = new AABB(
                center.x - maxRadius, center.y - maxRadius, center.z - maxRadius,
                center.x + maxRadius, center.y + maxRadius, center.z + maxRadius
        );

        return world.getEntitiesOfClass(LivingEntity.class, bounds).stream()
                .filter(entity -> entity != source)
                .filter(entity -> {
                    Vec3 relativePos = entity.position().subtract(center);
                    double projForward = relativePos.dot(forward);
                    double projRight = relativePos.dot(right);
                    double projUp = relativePos.dot(up);
                    return Math.abs(projForward) <= halfLength
                            && Math.abs(projRight) <= halfWidth
                            && Math.abs(projUp) <= halfHeight;
                })
                .toList();
    }

    /**
     * 技能伤害的目标筛选谓词：
     * 排除自己、非活体、无敌、创造/旁观玩家，以及释放者同队伍的实体（仅当释放者有队伍时）。
     */
    public static Predicate<LivingEntity> combatTargetFilter(LivingEntity source) {
        return target -> {
            if (target == source) return false;
            if (!target.isAlive()) return false;
            if (target.isInvulnerable()) return false;
            if (target instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
            if (source.getTeam() != null && target.getTeam() == source.getTeam()) return false;
            return true;
        };
    }
}
