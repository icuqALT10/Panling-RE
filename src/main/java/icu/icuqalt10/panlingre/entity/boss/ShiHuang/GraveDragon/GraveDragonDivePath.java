package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** A single captured landing point; every spine segment advances along the same backroll curve. */
final class GraveDragonDivePath {
    final Vec3 landing;
    final Vec3 origin, exitOffset;
    private final Matrix4f toModel;
    private final List<Node> nodes = new ArrayList<>();
    private final double[] spacing = new double[GraveDragonPose.spineBones().size()];
    private final double handoffLength, diveLength, recoveryLength;
    private record Node(Vec3 point, Quaternionf rotation, double distance) {}

    GraveDragonDivePath(Vec3 origin, float yaw, float scale, Vec3 landing) {
        this.landing = landing;
        this.origin = origin;
        var toWorld = GraveDragonPose.modelToEntity(yaw, 0, scale);
        toModel = new Matrix4f(toWorld).invert();
        var pose = GraveDragonPose.sample("air_dive_slam", 2.5, false);
        var spine = GraveDragonPose.spineBones();
        var centers = new ArrayList<Vec3>();
        var rotations = new ArrayList<Quaternionf>();
        for (String bone : spine) {
            var matrix = new Matrix4f(toWorld).mul(pose.matrices().get(bone));
            centers.add(vec(matrix.transformPosition(center(bone))));
            rotations.add(matrix.getUnnormalizedRotation(new Quaternionf()));
        }
        double[] at = new double[spine.size()];
        for (int i = spine.size() - 1; i >= 0; i--) {
            Vec3 end = centers.get(i);
            if (nodes.isEmpty()) append(end, rotations.get(i));
            else {
                Vec3 start = centers.get(i + 1);
                double handle = start.distanceTo(end) / 3;
                Vec3 a = start.add(forward(rotations.get(i + 1)).scale(handle));
                Vec3 b = end.subtract(forward(rotations.get(i)).scale(handle));
                for (int k = 1; k <= 12; k++) {
                    float t = k / 12f;
                    append(bezier(start, a, b, end, t), new Quaternionf(rotations.get(i + 1)).slerp(rotations.get(i), t));
                }
            }
            at[i] = nodes.getLast().distance;
        }
        handoffLength = nodes.getLast().distance;
        for (int i = 0; i < spacing.length; i++) spacing[i] = handoffLength - at[i];

        Vec3 start = centers.getFirst();
        Vec3 horizontal = landing.subtract(origin).subtract(start).multiply(1, 0, 1);
        if (horizontal.lengthSqr() < .001) horizontal = Vec3.directionFromRotation(0, yaw);
        Vec3 arrival = horizontal.normalize().add(0, -.45, 0).normalize();
        Quaternionf endRotation = new Quaternionf().rotationY((float)Math.atan2(-arrival.x, -arrival.z))
                .rotateX((float)Math.atan2(arrival.y, arrival.horizontalDistance()));
        // Center the mouth's lowest edge over the captured target.
        var headInverse = new Matrix4f(new Matrix4f(toWorld).mul(pose.matrices().get("head"))).invert();
        var mouth = GraveDragonPose.box(pose, "jaw", GraveDragonEntity.PART_BOUNDS[12], toWorld, Vec3.ZERO);
        var corners = new ArrayList<Vec3>();
        for (Vec3 corner : mouth.corners()) {
            var local = headInverse.transformPosition(vector(corner)).sub(center("head"));
            corners.add(vec(endRotation.transform(local)).scale(scale));
        }
        corners.sort(java.util.Comparator.comparingDouble(p -> p.y));
        Vec3 contact = corners.get(0).add(corners.get(1)).scale(.5);
        Vec3 end = landing.subtract(origin).subtract(contact).add(0, -1.15, 0);
        var initial = GraveDragonPose.sample("air_dive_slam", 0, false);
        var firstHead = new Matrix4f(toWorld).mul(initial.matrices().get("head"));
        Vec3 initialHead = vec(firstHead.transformPosition(center("head")));
        Vec3 ahead = landing.subtract(origin).add(horizontal.normalize().scale(22));
        Vec3 home = new Vec3(ahead.x, initialHead.y, ahead.z);
        exitOffset = home.subtract(initialHead);
        Vec3 initialDirection = vec(firstHead.transformDirection(new Vector3f(0, 0, -1))).normalize();
        Vec3 horizontalEnd = initialDirection.multiply(1, 0, 1).normalize();
        Vec3 normal = horizontalEnd.scale(-initialDirection.y / initialDirection.horizontalDistance()).add(0, 1, 0);
        Vec3 departure = forward(rotations.getFirst());
        double tangentLength = start.distanceTo(end) * 1.6;
        Vec3[] zero = quarticHandles(start, start, end, home, arrival.scale(tangentLength));
        Vec3[] one = quarticHandles(start, start.add(departure), end, home, arrival.scale(tangentLength));
        double lead = -zero[1].subtract(home).dot(normal) / one[1].subtract(zero[1]).dot(normal);
        lead = Mth.clamp(lead, .1, (start.y - end.y) * .8);
        Vec3 a = start.add(departure.scale(lead));
        Vec3[] handles = quarticHandles(start, a, end, home, arrival.scale(tangentLength));
        Quaternionf rotation = new Quaternionf(rotations.getFirst());
        double contactDistance = 0;
        for (int i = 1; i <= 500; i++) {
            double t = i / 500d;
            Vec3 tangent = bezier(a.subtract(start), handles[0].subtract(a), handles[1].subtract(handles[0]), home.subtract(handles[1]), t).normalize();
            rotation = transport(rotation, tangent);
            if (t <= .4) rotation.slerp(endRotation, (float)smooth((t - .3) / .1));
            else rotation = new Quaternionf().rotationY((float)Math.atan2(-tangent.x, -tangent.z))
                    .rotateX((float)Math.atan2(tangent.y, tangent.horizontalDistance()));
            append(quartic(start, a, handles[0], handles[1], home, t), rotation);
            if (i == 200) contactDistance = nodes.getLast().distance;
        }
        diveLength = contactDistance - handoffLength;
        recoveryLength = nodes.getLast().distance - contactDistance;
    }

    GraveDragonPose.Frame pose(GraveDragonPose.Frame base, double seconds) {
        if (seconds < 2.5) return base;
        var idle = GraveDragonPose.translate(GraveDragonPose.sample("idle_air", Math.min(3.2, seconds * 3.2 / 9.7), false),
                vec(toModel.transformDirection(vector(exitOffset))));
        if (seconds >= 9.7) return idle;
        double progress = Mth.clamp((seconds - 2.5) / .8, 0, 1);
        // Retain a nonzero 20-block/s tangent through impact instead of holding the pose.
        double advance = (1 - travel(1 - progress, 20 * .8 / diveLength)) * diveLength;
        // Keep impact speed, then finish the wider arc with zero speed and acceleration.
        double flight = Math.max(0, seconds - 3.3);
        double recoveryTime = Mth.clamp(flight / (3.4 + flight * (1 - 3.4 / 6.4)), 0, 1);
        double speed = 20 * 3.4 / recoveryLength;
        advance += (speed * recoveryTime + Math.pow(recoveryTime, 3) * ((10 - 6 * speed)
                + recoveryTime * ((8 * speed - 15) + recoveryTime * (6 - 3 * speed)))) * recoveryLength;
        var centers = new ArrayList<Vec3>();
        var rotations = new ArrayList<Quaternionf>();
        Quaternionf worldToModel = toModel.getUnnormalizedRotation(new Quaternionf());
        double settle = smoother((seconds - 6.8) / 2.9);
        if (seconds > 3.3) base = GraveDragonPose.blend(GraveDragonPose.sample("air_dive_slam", 3.3, false), idle, settle);
        for (int i = 0; i < spacing.length; i++) {
            Node sample = sample(handoffLength + advance - spacing[i]);
            Vec3 point = vec(toModel.transformPosition(vector(sample.point)));
            Quaternionf along = sample.rotation;
            if (i > 0) {
                // The mouth touches down; the thicker body rounds above the floor behind it.
                double offset = sample.distance - handoffLength - diveLength;
                double lift = 16 * Math.exp(-offset * offset / 200);
                double neck = Mth.clamp(spacing[i] / 12, 0, 1);
                double slope = -offset * lift / 100 * smooth(neck) - lift * 6 * neck * (1 - neck) / 12;
                along = transport(along, forward(along).add(0, slope, 0).normalize());
            }
            Quaternionf rotation = new Quaternionf(worldToModel).mul(along);
            if (settle > 0) {
                var target = idle.matrices().get(GraveDragonPose.spineBones().get(i));
                double delay = .4 * i / (spacing.length - 1);
                double release = smoother((seconds - 6.8 - delay) / (2.9 - delay));
                rotation.slerp(target.getUnnormalizedRotation(new Quaternionf()), (float)release);
            }
            centers.add(point); rotations.add(rotation);
        }
        return GraveDragonPose.curveSpine(base, centers, rotations, idle, 1 - settle);
    }

    Vec3 rootAt(double seconds) {
        return origin.add(exitOffset.scale(smooth((seconds - 3.3) / 4.5)));
    }

    GraveDragonPose.Frame relativePose(GraveDragonPose.Frame base, double seconds) {
        // Compensate only the planned root travel; wall corrections must also move the visible body.
        return GraveDragonPose.translate(pose(base, seconds), vec(toModel.transformDirection(vector(origin.subtract(rootAt(seconds))))));
    }

    /** Solve the two inner handles so u=.4 passes through the impact with its prescribed tangent. */
    private static Vec3[] quarticHandles(Vec3 start, Vec3 a, Vec3 contact, Vec3 home, Vec3 tangent) {
        double t = .4, u = 1 - t;
        double w2 = 6 * u * u * t * t, w3 = 4 * u * t * t * t;
        double d2 = 3 * u * t * (u - t), d3 = t * t * (3 * u - t);
        Vec3 position = contact.subtract(start.scale(u * u * u * u)).subtract(a.scale(4 * u * u * u * t)).subtract(home.scale(t * t * t * t));
        Vec3 direction = tangent.scale(.25).subtract(a.subtract(start).scale(u * u * u)).add(a.scale(3 * u * u * t)).subtract(home.scale(t * t * t));
        double determinant = w2 * d3 - w3 * d2;
        return new Vec3[]{position.scale(d3).subtract(direction.scale(w3)).scale(1 / determinant),
                direction.scale(w2).subtract(position.scale(d2)).scale(1 / determinant)};
    }

    private static Vec3 quartic(Vec3 p, Vec3 a, Vec3 b, Vec3 c, Vec3 q, double t) {
        double u = 1 - t;
        return p.scale(u * u * u * u).add(a.scale(4 * u * u * u * t)).add(b.scale(6 * u * u * t * t))
                .add(c.scale(4 * u * t * t * t)).add(q.scale(t * t * t * t));
    }

    private static double travel(double t, double speed) { return t * t * (3 - 2 * t) + speed * t * (1 - t) * (1 - t); }

    private void append(Vec3 point, Quaternionf rotation) {
        double length = nodes.isEmpty() ? 0 : nodes.getLast().distance + nodes.getLast().point.distanceTo(point);
        nodes.add(new Node(point, new Quaternionf(rotation), length));
    }

    private Node sample(double distance) {
        int lo = 0, hi = nodes.size() - 1;
        while (lo + 1 < hi) { int middle = (lo + hi) / 2; if (nodes.get(middle).distance < distance) lo = middle; else hi = middle; }
        Node a = nodes.get(lo), b = nodes.get(hi);
        float t = (float)Mth.clamp((distance - a.distance) / (b.distance - a.distance), 0, 1);
        return new Node(a.point.lerp(b.point, t), new Quaternionf(a.rotation).slerp(b.rotation, t), distance);
    }

    private static Quaternionf transport(Quaternionf rotation, Vec3 tangent) {
        return new Quaternionf().rotationTo(vector(forward(rotation)), vector(tangent)).mul(rotation);
    }

    private static Vec3 bezier(Vec3 p, Vec3 a, Vec3 b, Vec3 q, double t) {
        double u = 1 - t;
        return p.scale(u * u * u).add(a.scale(3 * u * u * t)).add(b.scale(3 * u * t * t)).add(q.scale(t * t * t));
    }

    private static double smooth(double t) { t = Mth.clamp(t, 0, 1); return t * t * (3 - 2 * t); }
    private static double smoother(double t) { t = Mth.clamp(t, 0, 1); return t * t * t * (10 + t * (-15 + 6 * t)); }
    private static Vec3 forward(Quaternionf rotation) { return vec(rotation.transform(new Vector3f(0, 0, -1))); }
    private static Vector3f center(String bone) {
        float[] bounds = GraveDragonEntity.PART_BOUNDS[Arrays.asList(GraveDragonEntity.PART_LABELS).indexOf(bone)];
        return new Vector3f(bounds[3], bounds[4], bounds[5]);
    }
    private static Vector3f vector(Vec3 v) { return new Vector3f((float)v.x, (float)v.y, (float)v.z); }
    private static Vec3 vec(Vector3f v) { return new Vec3(v.x, v.y, v.z); }
}
