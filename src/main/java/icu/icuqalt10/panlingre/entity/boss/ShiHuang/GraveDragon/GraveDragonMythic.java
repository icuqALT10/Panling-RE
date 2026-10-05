package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import icu.icuqalt10.panlingre.entity.OrientedBoundingBox;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Geometry shared by the new attacks' selection, server hits and client effects. */
public final class GraveDragonMythic {
    public static final double FAN_RANGE = 30;
    public static final double FAN_HALF_ANGLE = Math.PI / 6;
    public static final double RIFT_RANGE = 80;
    public static final double RIFT_SPEED = GraveDragonFireballEntity.SPEED * 20;
    public static final double FIRE_RING_RADIUS = 24;
    public static final double FIRE_RING_START_RADIUS = 2;
    public static final double FIRE_RING_HALF_WIDTH = 1.5;
    public static final double TAIL_SLAM_RADIUS = 6;
    public static final double BITE_STEP = 10.0 / 3;
    public static final double FLANK_RANGE = 8;

    public record Fan(Vec3 origin, Vec3 forward, double range) { }
    public record Flank(OrientedBoundingBox box, Vec3 direction, Vec3 surface) { }

    public static Vec3 forward(float yaw) {
        double angle = Math.toRadians(yaw);
        return new Vec3(-Math.sin(angle), 0, Math.cos(angle));
    }

    public static Vec3 ground(GraveDragonEntity dragon, Vec3 point) {
        var hit = dragon.level().clip(new ClipContext(point.add(0, 2, 0), point.add(0, -48, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dragon));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : point;
    }

    public static Vec3 part(GraveDragonEntity dragon, String name, double seconds, int index,
                            float yaw, float pitch, Vec3 origin) {
        return GraveDragonPose.box(GraveDragonPose.sample(name, seconds, false),
                GraveDragonEntity.PART_LABELS[index], GraveDragonEntity.PART_BOUNDS[index],
                GraveDragonPose.modelToEntity(yaw, pitch, dragon.getScale()), origin).center;
    }

    public static Fan fan(GraveDragonEntity dragon, double seconds, float yaw, Vec3 origin) {
        var source = GraveDragonBreath.geometry(GraveDragonPose.sample("ground_fan_breath", seconds, false),
                GraveDragonPose.modelToEntity(yaw, dragon.bodyPitch(), dragon.getScale()), origin, "ground_fan_breath");
        return new Fan(source.mouth(), source.direction(),
                Math.min(FAN_RANGE, Math.max(0, (seconds - 1.7) * 20 * 6)));
    }

    public static Vec3 fireRingCenter(GraveDragonEntity dragon, float yaw, Vec3 origin) {
        var source = GraveDragonBreath.geometry(GraveDragonPose.sample("air_open_fire_ring", 1.75, false),
                GraveDragonPose.modelToEntity(yaw, dragon.bodyPitch(), dragon.getScale()), origin, "air_open_fire_ring");
        return ground(dragon, source.mouth());
    }

    public static boolean fanHits(GraveDragonEntity dragon, Fan fan, Player player) {
        Vec3 offset = player.getBoundingBox().getCenter().subtract(fan.origin());
        double distance = offset.length();
        return distance <= fan.range() && offset.dot(fan.forward()) >= distance * Math.cos(FAN_HALF_ANGLE)
                && clear(dragon, fan.origin(), player.getBoundingBox().getCenter());
    }

    public static Vec3 fanPosition(GraveDragonEntity dragon, double seconds, float yaw, Player player) {
        Fan fan = fan(dragon, seconds, yaw, dragon.position());
        return dragon.position().add(player.position().subtract(fan.origin().add(fan.forward().scale(15))).multiply(1, 0, 1));
    }

    public static double biteTravel(double seconds) {
        return Mth.clamp((seconds - 1.15) * 20, 0, 3) * BITE_STEP
                + Mth.clamp((seconds - 1.95) * 20, 0, 3) * BITE_STEP;
    }

    public static Flank flank(GraveDragonEntity dragon, String action, float yaw, Vec3 origin) {
        Vec3 forward = forward(yaw);
        Vec3 side = forward(yaw + (action.endsWith("_l") ? -90 : 90));
        var frame = GraveDragonPose.sample(action, 2.15, false);
        var transform = GraveDragonPose.modelToEntity(yaw, 0, dragon.getScale());
        double front = Double.NEGATIVE_INFINITY, back = Double.POSITIVE_INFINITY, edge = 0;
        for (int i = 0; i <= 14; i++) {
            if (i == 11 || i == 12) continue; // Torso through the hind-leg roots, excluding jaws/head.
            var box = GraveDragonPose.box(frame, GraveDragonEntity.PART_LABELS[i],
                    GraveDragonEntity.PART_BOUNDS[i], transform, Vec3.ZERO);
            for (Vec3 corner : box.corners()) {
                double along = corner.dot(forward);
                front = Math.max(front, along);
                back = Math.min(back, along);
                edge = Math.max(edge, corner.dot(side));
            }
        }
        Vec3 middle = origin.add(forward.scale((front + back) / 2)).add(0, 2, 0);
        double reach = edge + FLANK_RANGE;
        return new Flank(new OrientedBoundingBox(middle.add(side.scale(reach / 2)), side,
                new Vec3(0, 1, 0), forward, new Vec3(reach / 2, 2, (front - back) / 2)),
                side, middle.add(side.scale(edge)));
    }

    public static boolean flankHits(GraveDragonEntity dragon, Flank flank, Player player) {
        Vec3 target = player.getBoundingBox().getCenter();
        double along = Mth.clamp(target.subtract(flank.surface()).dot(flank.box().axisZ),
                -flank.box().halfExtents.z, flank.box().halfExtents.z);
        Vec3 source = flank.surface().add(flank.box().axisZ.scale(along));
        return flank.box().intersects(player.getBoundingBox()) && clear(dragon, source, target);
    }

    public static Vec3 riftDirection(float yaw, int ray) {
        return forward(yaw + (ray - 1) * 30);
    }

    public static List<Vec3> riftOrigins(GraveDragonEntity dragon, float yaw, Vec3 origin) {
        Vec3 left = part(dragon, "ground_rift_fan", 2.1, 26, yaw, dragon.bodyPitch(), origin);
        Vec3 right = part(dragon, "ground_rift_fan", 2.1, 39, yaw, dragon.bodyPitch(), origin);
        return List.of(ground(dragon, left), ground(dragon, left.add(right).scale(.5)), ground(dragon, right));
    }

    public static boolean areaHits(GraveDragonEntity dragon, Vec3 center, double radius, Player player) {
        Vec3 offset = player.position().subtract(center);
        return offset.x * offset.x + offset.z * offset.z <= radius * radius
                && Math.abs(offset.y) < 2.5 && clear(dragon, center.add(0, .8, 0), player.getBoundingBox().getCenter());
    }

    public static double fireRingRadius(double seconds) {
        return Math.min(FIRE_RING_RADIUS, FIRE_RING_START_RADIUS + Math.max(0, seconds - 1.75) * 12);
    }

    public static float tailPitch(GraveDragonEntity dragon, Vec3 target, float yaw, double impact, Vec3 origin) {
        var box = GraveDragonPose.box(GraveDragonPose.sample("air_tail_pierce", impact, false),
                GraveDragonEntity.PART_LABELS[20], GraveDragonEntity.PART_BOUNDS[20],
                GraveDragonPose.modelToEntity(yaw, 0, dragon.getScale()), Vec3.ZERO);
        Vec3 aim = target.subtract(origin).normalize();
        double desired = Math.atan2(aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z));
        double current = Math.atan2(box.axisZ.y, Math.sqrt(box.axisZ.x * box.axisZ.x + box.axisZ.z * box.axisZ.z));
        float pitch = (float) Mth.clamp(Math.toDegrees(current - desired), -65, 65);
        // A shallow tail angle would require putting the locomotion root below the floor.
        while (pitch < 65) {
            Vec3 tip = part(dragon, "air_tail_pierce", impact, 20, yaw, pitch, Vec3.ZERO);
            if (tip.y + tailMinimumY(dragon, impact, yaw, pitch, 0) <= 1.2) break;
            pitch = Math.min(65, pitch + 1);
        }
        return pitch;
    }

    public static Vec3 tailDestination(GraveDragonEntity dragon, Vec3 target, float yaw, float pitch, double impact) {
        Vec3 tip = part(dragon, "air_tail_pierce", impact, 20, yaw, pitch, Vec3.ZERO);
        return target.add(0, .8, 0).subtract(tip);
    }

    public static double tailMinimumY(GraveDragonEntity dragon, double seconds, float yaw, float pitch, double floor) {
        var frame = GraveDragonPose.sample("air_tail_pierce", seconds, false);
        var transform = GraveDragonPose.modelToEntity(yaw, pitch, dragon.getScale());
        double lowest = 0;
        for (int i = 0; i < GraveDragonEntity.PART_LABELS.length; i++) {
            var box = GraveDragonPose.box(frame, GraveDragonEntity.PART_LABELS[i], GraveDragonEntity.PART_BOUNDS[i], transform, Vec3.ZERO);
            lowest = Math.min(lowest, box.enclosingAabb().minY);
        }
        return floor + .05 - lowest;
    }

    public static boolean clear(GraveDragonEntity dragon, Vec3 from, Vec3 to) {
        return dragon.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, dragon)).getType() == HitResult.Type.MISS;
    }

    private GraveDragonMythic() { }
}
