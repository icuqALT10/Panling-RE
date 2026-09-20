package icu.icuqalt10.panlingre.entity;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;

/** A small, immutable OBB used by multipart hit detection. */
public final class OrientedBoundingBox {
    public final Vec3 center, axisX, axisY, axisZ, halfExtents;

    public OrientedBoundingBox(Vec3 center, Vec3 axisX, Vec3 axisY, Vec3 axisZ, Vec3 halfExtents) {
        this.center = center;
        this.axisX = axisX.normalize(); this.axisY = axisY.normalize(); this.axisZ = axisZ.normalize();
        this.halfExtents = halfExtents;
    }

    public static OrientedBoundingBox axisAligned(Vec3 center, double width, double height, double depth) {
        return new OrientedBoundingBox(center, new Vec3(1,0,0), new Vec3(0,1,0), new Vec3(0,0,1),
                new Vec3(width/2, height/2, depth/2));
    }

    public AABB enclosingAabb() {
        double x = Math.abs(axisX.x)*halfExtents.x + Math.abs(axisY.x)*halfExtents.y + Math.abs(axisZ.x)*halfExtents.z;
        double y = Math.abs(axisX.y)*halfExtents.x + Math.abs(axisY.y)*halfExtents.y + Math.abs(axisZ.y)*halfExtents.z;
        double z = Math.abs(axisX.z)*halfExtents.x + Math.abs(axisY.z)*halfExtents.y + Math.abs(axisZ.z)*halfExtents.z;
        return new AABB(center.x-x, center.y-y, center.z-z, center.x+x, center.y+y, center.z+z);
    }

    public Vec3[] corners() {
        Vec3 x = axisX.scale(halfExtents.x), y = axisY.scale(halfExtents.y), z = axisZ.scale(halfExtents.z);
        Vec3[] c = new Vec3[8]; int n = 0;
        for (int sx=-1;sx<=1;sx+=2) for (int sy=-1;sy<=1;sy+=2) for (int sz=-1;sz<=1;sz+=2)
            c[n++] = center.add(x.scale(sx)).add(y.scale(sy)).add(z.scale(sz));
        return c;
    }

    public double distanceToSqr(Vec3 point) {
        Vec3 d = point.subtract(center);
        double dx = clamp(d.dot(axisX), -halfExtents.x, halfExtents.x);
        double dy = clamp(d.dot(axisY), -halfExtents.y, halfExtents.y);
        double dz = clamp(d.dot(axisZ), -halfExtents.z, halfExtents.z);
        Vec3 nearest = center.add(axisX.scale(dx)).add(axisY.scale(dy)).add(axisZ.scale(dz));
        return nearest.distanceToSqr(point);
    }
    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    public OrientedBoundingBox move(Vec3 offset) {
        return new OrientedBoundingBox(center.add(offset), axisX, axisY, axisZ, halfExtents);
    }

    /** Padding expands local faces, never the empty corners of the world envelope. */
    public OrientedBoundingBox inflate(double x, double y, double z) {
        return new OrientedBoundingBox(center, axisX, axisY, axisZ,
                new Vec3(Math.max(0, halfExtents.x + x), Math.max(0, halfExtents.y + y), Math.max(0, halfExtents.z + z)));
    }

    public boolean contains(Vec3 point) {
        Vec3 d = point.subtract(center);
        return Math.abs(d.dot(axisX)) <= halfExtents.x + 1e-9
                && Math.abs(d.dot(axisY)) <= halfExtents.y + 1e-9
                && Math.abs(d.dot(axisZ)) <= halfExtents.z + 1e-9;
    }

    /** Segment/slab intersection in local coordinates, including inside starts. */
    public Optional<Vec3> clip(Vec3 from, Vec3 to) {
        Vec3 start = from.subtract(center), delta = to.subtract(from);
        Vec3[] axes = {axisX, axisY, axisZ};
        double enter = 0, exit = 1;
        for (int i = 0; i < 3; i++) {
            double s = start.dot(axes[i]), d = delta.dot(axes[i]), h = half(i);
            if (Math.abs(d) < 1e-12) {
                if (s < -h || s > h) return Optional.empty();
                continue;
            }
            double a = (-h - s) / d, b = (h - s) / d;
            enter = Math.max(enter, Math.min(a, b));
            exit = Math.min(exit, Math.max(a, b));
            if (enter > exit) return Optional.empty();
        }
        return Optional.of(from.add(delta.scale(enter)));
    }

    /** Separating-axis test against a vanilla axis-aligned box. */
    public boolean intersects(AABB box) {
        Vec3 bc = new Vec3((box.minX+box.maxX)/2, (box.minY+box.maxY)/2, (box.minZ+box.maxZ)/2);
        double[] b = {(box.maxX-box.minX)/2, (box.maxY-box.minY)/2, (box.maxZ-box.minZ)/2};
        Vec3[] a = {axisX, axisY, axisZ};
        double[][] r = new double[3][3];
        for (int i=0;i<3;i++) { r[i][0]=a[i].x; r[i][1]=a[i].y; r[i][2]=a[i].z; }
        Vec3 d = bc.subtract(center);
        double eps = 1.0e-7;
        for (int i=0;i<3;i++) {
            double ra = half(i), rb = b[0]*Math.abs(r[i][0])+b[1]*Math.abs(r[i][1])+b[2]*Math.abs(r[i][2]);
            if (Math.abs(d.dot(a[i])) > ra+rb+eps) return false;
        }
        for (int j=0;j<3;j++) {
            double ra = halfExtents.x*Math.abs(r[0][j])+halfExtents.y*Math.abs(r[1][j])+halfExtents.z*Math.abs(r[2][j]);
            double dt = j == 0 ? d.x : j == 1 ? d.y : d.z;
            if (Math.abs(dt) > ra+b[j]+eps) return false;
        }
        for (int i=0;i<3;i++) for (int j=0;j<3;j++) {
            Vec3 cross = a[i].cross(new Vec3(j==0?1:0,j==1?1:0,j==2?1:0));
            double len = cross.length(); if (len < eps) continue; cross = cross.scale(1/len);
            double ra = halfExtents.x*Math.abs(cross.dot(axisX))+halfExtents.y*Math.abs(cross.dot(axisY))+halfExtents.z*Math.abs(cross.dot(axisZ));
            double rb = b[0]*Math.abs(cross.x)+b[1]*Math.abs(cross.y)+b[2]*Math.abs(cross.z);
            if (Math.abs(d.dot(cross)) > ra+rb+eps) return false;
        }
        return true;
    }
    private double half(int i) { return i==0?halfExtents.x:i==1?halfExtents.y:halfExtents.z; }
}
