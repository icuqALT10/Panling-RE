package icu.icuqalt10.panlingre.client.renderer;

import com.mojang.blaze3d.vertex.*;
import icu.icuqalt10.panlingre.entity.ZhuRiArrowEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class ZhuRiArrowRenderer extends EntityRenderer<ZhuRiArrowEntity> {
    private static final ResourceLocation TEX =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private static final RenderType TRAIL =
            RenderType.entityTranslucentEmissive(TEX, false);

    private static final int SIDES = 8;

    public ZhuRiArrowRenderer(EntityRendererProvider.Context c) { super(c); }
    @Override public ResourceLocation getTextureLocation(ZhuRiArrowEntity e) { return TEX; }

    @Override
    public void render(ZhuRiArrowEntity e, float yaw, float pt,
                       PoseStack ps, MultiBufferSource buf, int light) {
        float prog = e.progress(pt), decay = e.decay(pt);
        boolean dec = e.decaying();
        if (prog <= 0 && !dec) return;

        if (!dec) {
            Vec3 tangent = ZhuRiArrowEntity.cubicTangent(
                    prog, e.p0(), e.p1(), e.p2(), e.p3());
            if (tangent.lengthSqr() > 1.0E-7D) {
                Vec3 direction = tangent.normalize();
                renderAirCone(direction, ps, buf);
            }
        }

        Vec3 ep = e.getPosition(pt);
        List<Vec3> pts = sampleCurve(e, prog, decay, dec, ep);
        if (pts.size() < 2) return;

        float ga = dec ? Math.max(0f, 1f - decay) : 1f;

        // setColor takes separate 0..255 channels.  Passing 0xeba317 as the red
        // channel used to truncate to 0x17 on common buffer implementations,
        // which is why shader packs showed a very dark red beam.
        tube(pts, ps, buf, TRAIL, 0.055F,
                0x6F, 0x02, 0x02, (int)(150 * ga));

        super.render(e, yaw, pt, ps, buf, light);
    }

    private void renderAirCone(Vec3 direction, PoseStack poseStack, MultiBufferSource buffers) {
        Vec3 binormal = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (binormal.lengthSqr() < 1.0E-7D) binormal = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        binormal = binormal.normalize();
        Vec3 normal = direction.cross(binormal).normalize();
        Vec3 base = direction.scale(-0.66D);
        Vec3 tip = direction.scale(1.44D);
        double radius = 0.22D;
        VertexConsumer consumer = buffers.getBuffer(TRAIL);
        PoseStack.Pose pose = poseStack.last();
        int sides = 5;
        for (int i = 0; i < sides; i++) {
            double a = Math.PI * 2.0D * i / sides;
            double b = Math.PI * 2.0D * (i + 1) / sides;
            Vec3 first = base.add(binormal.scale(Math.cos(a) * radius))
                    .add(normal.scale(Math.sin(a) * radius));
            Vec3 second = base.add(binormal.scale(Math.cos(b) * radius))
                    .add(normal.scale(Math.sin(b) * radius));
            coneVertex(consumer, pose, first, 25);
            coneVertex(consumer, pose, second, 25);
            coneVertex(consumer, pose, tip, 95);
            coneVertex(consumer, pose, tip, 95);
            coneVertex(consumer, pose, second, 25);
            coneVertex(consumer, pose, first, 25);
            coneVertex(consumer, pose, tip, 95);
            coneVertex(consumer, pose, tip, 95);
        }
    }

    private void coneVertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point, int alpha) {
        consumer.addVertex(pose, (float)point.x, (float)point.y, (float)point.z)
                .setColor(0x6F, 0x02, 0x02, alpha).setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private List<Vec3> sampleCurve(ZhuRiArrowEntity e, float prog, float decay,
                                    boolean dec, Vec3 ep) {
        Vec3 P0 = e.p0(), P1 = e.p1(), P2 = e.p2(), P3 = e.p3();
        if (P0 == null || P3 == null) return List.of();

        float tMin, tMax;
        if (dec) { tMin = decay; tMax = 1f; }
        else     { tMin = 0f;    tMax = prog; }
        if (tMax <= tMin || tMax <= 0) return List.of();

        List<Vec3> pts = new ArrayList<>();
        float step = 0.006f;
        for (float t = tMin; t <= tMax + step/2; t += step) {
            float tc = Math.min(t, tMax);
            pts.add(ZhuRiArrowEntity.cubicBezier(tc, P0, P1, P2, P3).subtract(ep));
            if (tc >= tMax) break;
        }
        return pts;
    }

    private void tube(List<Vec3> pts, PoseStack ps, MultiBufferSource buf,
                      RenderType type, float w, int cr, int cg, int cb, int alpha) {
        if (alpha <= 0 || pts.size() < 2) return;
        VertexConsumer vc = buf.getBuffer(type);
        PoseStack.Pose pose = ps.last();
        int lb = LightTexture.FULL_BRIGHT;

        List<float[][]> rings = new ArrayList<>();
        for (int i = 0; i < pts.size(); i++) {
            Vec3 cp = pts.get(i);
            Vec3 next = pts.get(Math.min(i+1, pts.size()-1));
            Vec3 prev = pts.get(Math.max(i-1, 0));
            Vec3 tan = next.subtract(prev);
            double tl = tan.length();
            if (tl < 1e-5) continue;

            V3 T = new V3(tan.x/tl, tan.y/tl, tan.z/tl);
            V3 B = cross(T, new V3(0,1,0));
            if (B.lenSq() < 1e-5) B = cross(T, new V3(1,0,0));
            B = B.nrm();
            V3 N = cross(T, B).nrm();

            float segP = (float)i / (pts.size() - 1);
            // Gradient along beam: darker at start → brighter at end (like DE guardian beam)
            float taper = smoothStep(Math.min(1.0F, segP / 0.45F));

            float[][] ring = new float[SIDES][3];
            for (int s = 0; s < SIDES; s++) {
                double ang = s * 2 * Math.PI / SIDES;
                double ca = Math.cos(ang), sa = Math.sin(ang);
                ring[s][0] = (float)(cp.x + w*taper*(ca*B.x + sa*N.x));
                ring[s][1] = (float)(cp.y + w*taper*(ca*B.y + sa*N.y));
                ring[s][2] = (float)(cp.z + w*taper*(ca*B.z + sa*N.z));
            }
            rings.add(ring);
        }

        for (int i = 0; i < rings.size() - 1; i++) {
            float[][] r0 = rings.get(i);
            float[][] r1 = rings.get(i + 1);
            float segP = (float)i / (rings.size()-1);
            float taper = smoothStep(Math.min(1.0F, segP / 0.45F));
            int a = (int)(alpha * taper);
            for (int s = 0; s < SIDES; s++) {
                int sn = (s + 1) % SIDES;
                float[] a0 = r0[s],  a1 = r0[sn];
                float[] b0 = r1[s],  b1 = r1[sn];
                quad(vc, pose, a0,a1,b1,b0, cr,cg,cb,a, lb);
            }
        }
    }

    private void quad(VertexConsumer vc, PoseStack.Pose pose,
                      float[] a, float[] b, float[] c, float[] d,
                      int cr, int cg, int cb, int alpha, int lb) {
        vc.addVertex(pose, a[0],a[1],a[2]).setColor(cr,cg,cb,alpha).setUv(0,0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lb).setNormal(pose,0,1,0);
        vc.addVertex(pose, b[0],b[1],b[2]).setColor(cr,cg,cb,alpha).setUv(0,0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lb).setNormal(pose,0,1,0);
        vc.addVertex(pose, c[0],c[1],c[2]).setColor(cr,cg,cb,alpha).setUv(0,0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lb).setNormal(pose,0,1,0);
        vc.addVertex(pose, d[0],d[1],d[2]).setColor(cr,cg,cb,alpha).setUv(0,0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lb).setNormal(pose,0,1,0);
    }

    private record V3(double x, double y, double z) {
        V3 nrm() { double l=Math.sqrt(x*x+y*y+z*z); return l<1e-9?this:new V3(x/l,y/l,z/l); }
        double lenSq() { return x*x+y*y+z*z; }
    }
    private static V3 cross(V3 a, V3 b) {
        return new V3(a.y*b.z - a.z*b.y, a.z*b.x - a.x*b.z, a.x*b.y - a.y*b.x);
    }
    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }
}
