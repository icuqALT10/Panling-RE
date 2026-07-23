package icu.icuqalt10.panlingre.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

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
}
