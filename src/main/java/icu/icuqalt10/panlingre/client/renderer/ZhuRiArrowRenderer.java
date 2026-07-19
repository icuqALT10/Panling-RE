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

    // Outer layers: translucent + NO_CULL (dark shell, DE style)
    private static final RenderType BEAM_DARK = RenderType.create(
            "zhu_ri_beam_dark",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 256, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderType.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                    .setTextureState(new RenderType.TextureStateShard(TEX, false, false))
                    .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderType.NO_CULL)
                    .setLightmapState(RenderType.LIGHTMAP)
                    .setOverlayState(RenderType.OVERLAY)
                    .createCompositeState(false)
    );
    // Inner core: additive glow
    private static final RenderType BEAM_GLOW = RenderType.eyes(TEX);

    private static final int SIDES = 8;

    public ZhuRiArrowRenderer(EntityRendererProvider.Context c) { super(c); }
    @Override public ResourceLocation getTextureLocation(ZhuRiArrowEntity e) { return TEX; }

    @Override
    public void render(ZhuRiArrowEntity e, float yaw, float pt,
                       PoseStack ps, MultiBufferSource buf, int light) {
        float prog = e.progress(), decay = e.decay();
        boolean dec = e.decaying();
        if (prog <= 0 && !dec) return;

        Vec3 ep = e.getPosition(pt);
        List<Vec3> pts = sampleCurve(e, prog, decay, dec, ep);
        if (pts.size() < 2) return;

        float ga = dec ? Math.max(0f, 1f - decay) : 1f;

        // Outer aura (additive glow)
        tube(pts, ps, buf, BEAM_GLOW, 0.12f, 0xeba317,0,0,   (int)(30*ga));
        // Mid glow (additive glow)
        tube(pts, ps, buf, BEAM_GLOW, 0.05f, 0x2d,0,0,   (int)(60*ga));
        // Inner core (dark, translucent)
        tube(pts, ps, buf, BEAM_DARK, 0.015f,0x6f,0x02,0x02,(int)(180*ga));

        super.render(e, yaw, pt, ps, buf, light);
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
            float grad = 0.4f + 0.6f * segP;
            int a = (int)(alpha * grad);

            float[][] ring = new float[SIDES][3];
            for (int s = 0; s < SIDES; s++) {
                double ang = s * 2 * Math.PI / SIDES;
                double ca = Math.cos(ang), sa = Math.sin(ang);
                ring[s][0] = (float)(cp.x + w*(ca*B.x + sa*N.x));
                ring[s][1] = (float)(cp.y + w*(ca*B.y + sa*N.y));
                ring[s][2] = (float)(cp.z + w*(ca*B.z + sa*N.z));
            }
            rings.add(ring);
        }

        for (int i = 0; i < rings.size() - 1; i++) {
            float[][] r0 = rings.get(i);
            float[][] r1 = rings.get(i + 1);
            float segP = (float)i / (rings.size()-1);
            float grad = 0.4f + 0.6f * segP;
            int a = (int)(alpha * grad);
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
}
