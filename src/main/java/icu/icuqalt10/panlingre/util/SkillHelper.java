package icu.icuqalt10.panlingre.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import icu.icuqalt10.panlingre.entity.MultipartEntity;
import icu.icuqalt10.panlingre.entity.OrientedBoundingBox;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.effect.MobEffectInstance;

public class SkillHelper {

    /** Applies an effect to the living multipart root when a selector returns a child. */
    public static boolean addEffectToTarget(Entity target, MobEffectInstance effect) {
        LivingEntity living = MultipartEntity.livingRoot(target);
        return living != null && living.addEffect(new MobEffectInstance(effect));
    }

    /** 范围技能专用：子部件归并到父实体，并按 UUID 去重。 */
    public static List<LivingEntity> getMultipartTargets(LivingEntity source, AABB area) {
        return MultipartEntity.collectTargets(source.level(), area, source).stream()
                .filter(combatTargetFilter(source)).toList();
    }

    /**
     * 选出 source前方 width格宽 height格高 length格长的范围内的实体
     * 排除自己、非活体、无敌、创造/旁观玩家，以及释放者同队伍的实体（仅当释放者有队伍时）。
     */
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

        List<LivingEntity> candidates = MultipartEntity.collectTargets(world, bounds, source);
        return candidates.stream()
                .filter(entity -> entity != source)
                .filter(entity -> insideForwardBox(entity, center, forward, right, up,
                        halfLength, halfWidth, halfHeight))
                .toList();
    }

    /**
     * 判断实体是否落在以 {@code center} 为中心、沿 {@code forward} 方向延伸的前方长方体区域内。
     *
     * <p>任一部位与技能范围相交即可命中；候选列表已按主体去重。
     */
    private static boolean insideForwardBox(LivingEntity entity, Vec3 center, Vec3 forward, Vec3 right, Vec3 up,
                                            double halfLength, double halfWidth, double halfHeight) {
        AABB area = new AABB(-halfWidth, -halfHeight, -halfLength, halfWidth, halfHeight, halfLength);
        for (Entity part : MultipartEntity.targetParts(entity)) {
            if (part == null || part.isRemoved()) continue;
            var box = SkillTargeting.box(part);
            var local = new OrientedBoundingBox(inBasis(box.center.subtract(center), right, up, forward),
                    inBasis(box.axisX, right, up, forward), inBasis(box.axisY, right, up, forward),
                    inBasis(box.axisZ, right, up, forward), box.halfExtents);
            if (local.intersects(area)) return true;
        }
        return false;
    }

    private static Vec3 inBasis(Vec3 vector, Vec3 right, Vec3 up, Vec3 forward) {
        return new Vec3(vector.dot(right), vector.dot(up), vector.dot(forward));
    }

    /**
     * 技能伤害的目标筛选谓词：
     * 排除自己、非活体、无敌、创造/旁观玩家，以及释放者同队伍的实体（仅当释放者有队伍时）。
     */
    public static Predicate<LivingEntity> combatTargetFilter(LivingEntity source) {
        return target -> {
            if (target.is(source)) return false;
            if (!target.isAlive()) return false;
            if (!(target instanceof MultipartEntity) && !target.isAttackable()) return false;
            if (!(target instanceof MultipartEntity) && target.isInvulnerable()) return false;
            if (target instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
            if (source.getTeam() != null && target.getTeam() == source.getTeam()) return false;
            return true;
        };
    }

    /**
     * 治疗、增益和保护技能的友方目标筛选：释放者有队伍时只接受同队活体，
     * 没有队伍时接受附近所有活体。旁观玩家不作为有效目标。
     */
    public static Predicate<LivingEntity> friendlyTargetFilter(LivingEntity source) {
        return target -> target.isAlive()
                && (!(target instanceof Player player) || !player.isSpectator())
                && (source.getTeam() == null || target.getTeam() == source.getTeam());
    }

    /** 玩家始终优先，同类型目标再按距释放者由近到远排列。 */
    public static Comparator<LivingEntity> friendlyTargetComparator(LivingEntity source) {
        return Comparator.comparingInt((LivingEntity target) -> target instanceof Player ? 0 : 1)
                .thenComparingDouble(target -> {
                    var contact = SkillTargeting.nearest(target, source.position());
                    if (contact == null) return Double.POSITIVE_INFINITY;
                    return contact.part() == target ? source.distanceToSqr(target)
                            : contact.point().distanceToSqr(source.position());
                });
    }
}
