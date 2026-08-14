package icu.icuqalt10.panlingre.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import icu.icuqalt10.panlingre.entity.YsMuHealingEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class        YsMuHealingRenderer extends EntityRenderer<YsMuHealingEntity> {
    private static final ResourceLocation WHITE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final RenderType TRAIL = RenderType.entityTranslucentEmissive(WHITE, false);
    private static final int SIDES = 8;

    public YsMuHealingRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(YsMuHealingEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int light) {
        float progress = entity.progress();
        float decay = entity.decay();
        boolean decaying = entity.decaying();
        Vec3 entityPos = entity.getPosition(partialTick);

        List<Vec3> points = sampleCurve(entity, progress, decay, decaying, entityPos);
        if (points.size() >= 2) {
            float alpha = decaying ? Math.max(0.0F, 1.0F - decay) : 1.0F;
            int color = entity.getTrailColor();
            tube(points, poseStack, buffers, 0.0275F,
                    color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF,
                    (int) (230 * alpha));
        }

        // Only yaw around the world Y axis. The texture's top therefore always stays upright.
        if (!decaying) {
            Vec3 tangent = YsMuHealingEntity.cubicTangent(progress,
                    entity.p0(), entity.p1(), entity.p2(), entity.p3());
            poseStack.pushPose();
            if (tangent.lengthSqr() > 1.0E-6) {
                float itemYaw = (float) Math.toDegrees(Math.atan2(tangent.x, tangent.z));
                poseStack.mulPose(Axis.YP.rotationDegrees(itemYaw));
            }
            poseStack.scale(0.55F, 0.55F, 0.55F);
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    entity.getItem(), ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, poseStack, buffers, entity.level(), entity.getId());
            poseStack.popPose();
        }

        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    private List<Vec3> sampleCurve(YsMuHealingEntity entity, float progress, float decay,
                                   boolean decaying, Vec3 entityPos) {
        float min = decaying ? decay : 0.0F;
        float max = decaying ? 1.0F : progress;
        if (max <= min || max <= 0.0F) return List.of();

        List<Vec3> points = new ArrayList<>();
        float step = 0.006F;
        for (float t = min; t <= max + step / 2.0F; t += step) {
            float sample = Math.min(t, max);
            points.add(YsMuHealingEntity.cubicBezier(sample,
                    entity.p0(), entity.p1(), entity.p2(), entity.p3()).subtract(entityPos));
            if (sample >= max) break;
        }
        return points;
    }

    private void tube(List<Vec3> points, PoseStack poseStack, MultiBufferSource buffers,
                      float width, int red, int green, int blue, int alpha) {
        if (alpha <= 0 || points.size() < 2) return;
        VertexConsumer consumer = buffers.getBuffer(TRAIL);
        PoseStack.Pose pose = poseStack.last();
        List<float[][]> rings = new ArrayList<>();

        for (int i = 0; i < points.size(); i++) {
            Vec3 current = points.get(i);
            Vec3 next = points.get(Math.min(i + 1, points.size() - 1));
            Vec3 previous = points.get(Math.max(i - 1, 0));
            Vec3 tangent = next.subtract(previous);
            double tangentLength = tangent.length();
            if (tangentLength < 1.0E-5) continue;

            Vec3 direction = tangent.scale(1.0 / tangentLength);
            Vec3 binormal = direction.cross(new Vec3(0.0, 1.0, 0.0));
            if (binormal.lengthSqr() < 1.0E-5) {
                binormal = direction.cross(new Vec3(1.0, 0.0, 0.0));
            }
            binormal = binormal.normalize();
            Vec3 normal = direction.cross(binormal).normalize();
            float pathProgress = i / (points.size() - 1.0F);
            float taper = smoothStep(Math.min(1.0F, pathProgress / 0.45F));

            float[][] ring = new float[SIDES][3];
            for (int side = 0; side < SIDES; side++) {
                double angle = side * 2.0 * Math.PI / SIDES;
                Vec3 offset = binormal.scale(Math.cos(angle) * width * taper)
                        .add(normal.scale(Math.sin(angle) * width * taper));
                Vec3 vertex = current.add(offset);
                ring[side][0] = (float) vertex.x;
                ring[side][1] = (float) vertex.y;
                ring[side][2] = (float) vertex.z;
            }
            rings.add(ring);
        }

        for (int i = 0; i < rings.size() - 1; i++) {
            float pathProgress = i / (rings.size() - 1.0F);
            float taper = smoothStep(Math.min(1.0F, pathProgress / 0.45F));
            int segmentAlpha = (int) (alpha * taper);
            float[][] first = rings.get(i);
            float[][] second = rings.get(i + 1);
            for (int side = 0; side < SIDES; side++) {
                int nextSide = (side + 1) % SIDES;
                quad(consumer, pose, first[side], first[nextSide],
                        second[nextSide], second[side], red, green, blue, segmentAlpha);
            }
        }
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private void quad(VertexConsumer consumer, PoseStack.Pose pose,
                      float[] a, float[] b, float[] c, float[] d,
                      int red, int green, int blue, int alpha) {
        vertex(consumer, pose, a, red, green, blue, alpha);
        vertex(consumer, pose, b, red, green, blue, alpha);
        vertex(consumer, pose, c, red, green, blue, alpha);
        vertex(consumer, pose, d, red, green, blue, alpha);
    }

    private void vertex(VertexConsumer consumer, PoseStack.Pose pose, float[] point,
                        int red, int green, int blue, int alpha) {
        consumer.addVertex(pose, point[0], point[1], point[2])
                .setColor(red, green, blue, alpha)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(YsMuHealingEntity entity) {
        return WHITE;
    }
}
