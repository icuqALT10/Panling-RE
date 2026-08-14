package icu.icuqalt10.panlingre.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import icu.icuqalt10.panlingre.entity.TuBarrierEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class TuBarrierRenderer extends EntityRenderer<TuBarrierEntity> {
    private static final ResourceLocation WHITE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final RenderType CORE = RenderType.entityTranslucentEmissive(WHITE, false);
    private static final RenderType GLOW = RenderType.eyes(WHITE);
    private static final int TUBE_SIDES = 8;
    private static final int RING_SEGMENTS = 96;
    private static final int BEAM_SEGMENTS = 24;

    public TuBarrierRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TuBarrierEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int light) {
        if (entity.isActive()) {
            renderBarrierLines(entity, partialTick, poseStack, buffers, light);
        }

        poseStack.pushPose();
        if (entity.isGroundStyle()) {
            double localBaseY = entity.getBarrierBaseY() - entity.getPosition(partialTick).y + 0.06D;
            float flatten = smoothStep(entity.getFlattenProgress(partialTick));
            float expand = smoothStep(entity.getExpandProgress(partialTick));
            float spinAge = Math.max(0.0F, entity.tickCount + partialTick - 5.0F);
            poseStack.translate(0.0D, localBaseY, 0.0D);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F * flatten));
            poseStack.mulPose(Axis.ZP.rotationDegrees(spinAge * 7.2F));
            float itemScale = 1.0F + 34.0F * expand;
            float thicknessScale = 1.0F + 3.0F * expand;
            poseStack.scale(itemScale, itemScale, thicknessScale);
        } else {
            poseStack.translate(0.0, 0.5, 0.0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            poseStack.scale(2.25F, 2.25F, 2.25F);
        }
        Minecraft.getInstance().getItemRenderer().renderStatic(
                entity.getItem(), ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, poseStack, buffers, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    private void renderBarrierLines(TuBarrierEntity entity, float partialTick,
                                    PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        double radius = entity.getDiameter() * 0.5;
        double localBaseY = entity.getBarrierBaseY() - entity.getPosition(partialTick).y + 0.05;
        float health = entity.getBarrierHealthPercentage();
        int glowAlpha = Math.round(18.0F * health);
        int coreAlpha = Math.round(64.0F * health);

        List<Vec3> ring = new ArrayList<>(RING_SEGMENTS);
        for (int i = 0; i < RING_SEGMENTS; i++) {
            double angle = Math.PI * 2.0 * i / RING_SEGMENTS;
            ring.add(new Vec3(
                    Math.cos(angle) * radius,
                    localBaseY,
                    Math.sin(angle) * radius));
        }
        tubePath(ring, true, poseStack, buffers, GLOW, 0.075F, glowAlpha, LightTexture.FULL_BRIGHT);
        tubePath(ring, true, poseStack, buffers, CORE, 0.025F, coreAlpha, LightTexture.FULL_BRIGHT);

        int beamCount = Math.max(1, Math.round(entity.getDiameter()));
        if (entity.isGroundStyle()) {
            int groundBeamCount = 30;
            Vec3 convergence = new Vec3(0.0D, localBaseY + 10.0D, 0.0D);
            for (int i = 0; i < groundBeamCount; i++) {
                renderGroundFlowingBeam(entity, partialTick, i, groundBeamCount,
                        radius, localBaseY, convergence, poseStack, buffers, coreAlpha);
            }
            renderPolygonalSphere(convergence, poseStack, buffers, coreAlpha);
        } else {
            for (int i = 0; i < beamCount; i++) {
                double angle = Math.PI * 2.0 * i / beamCount;
                Vec3 flowOrigin = Vec3.ZERO;
                Vec3 end = new Vec3(
                        Math.cos(angle) * radius,
                        localBaseY,
                        Math.sin(angle) * radius);
                Vec3 radialDirection = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
                Vec3 control = radialDirection.scale(radius * 0.7)
                        .add(0.0, flowOrigin.y + (end.y - flowOrigin.y) * 0.35 + 0.25, 0.0);
                renderFlowingBeam(entity, partialTick, i, end, control, flowOrigin,
                        poseStack, buffers, coreAlpha);
            }
        }
    }

    private void renderGroundFlowingBeam(TuBarrierEntity entity, float partialTick,
                                         int beamIndex, int beamCount, double domainRadius,
                                         double localBaseY, Vec3 end, PoseStack poseStack,
                                         MultiBufferSource buffers, int coreAlpha) {
        double trailLength = 0.48D;
        double cycleLength = 1.0D + trailLength;
        double rawTravel = (entity.tickCount + partialTick) * 0.085D
                + beamIndex * cycleLength / beamCount;
        long cycle = (long) Math.floor(rawTravel / cycleLength);
        double travel = rawTravel - cycle * cycleLength;
        double startT = Math.max(0.0D, travel - trailLength);
        double endT = Math.min(1.0D, travel);
        if (endT - startT < 1.0E-4D) return;

        long seed = ((long) entity.getId() << 32) ^ cycle * 0x9E3779B97F4A7C15L
                ^ beamIndex * 1013L;
        double angle = unit(seed) * Math.PI * 2.0D;
        double minimumRadius = 1.5D;
        double maximumRadius = domainRadius - 0.15D;
        double radius = Math.sqrt(minimumRadius * minimumRadius
                + unit(seed + 1L) * (maximumRadius * maximumRadius - minimumRadius * minimumRadius));
        Vec3 start = new Vec3(Math.cos(angle) * radius, localBaseY + 0.12D,
                Math.sin(angle) * radius);
        Vec3 control = new Vec3(start.x * 0.58D,
                localBaseY + 5.6D + unit(seed + 2L) * 1.8D, start.z * 0.58D);
        renderFlowSegment(startT, endT, start, control, end,
                poseStack, buffers, coreAlpha);
    }

    private void renderPolygonalSphere(Vec3 center, PoseStack poseStack,
                                       MultiBufferSource buffers, int alpha) {
        FacetedOctahedronRenderer.render(center, 0.5D, 0xFFAA00,
                Math.max(80, alpha * 2), poseStack, buffers);
    }

    private void renderFlowingBeam(TuBarrierEntity entity, float partialTick, int beamIndex,
                                   Vec3 start, Vec3 control, Vec3 end,
                                   PoseStack poseStack, MultiBufferSource buffers, int coreAlpha) {
        double trailLength = 0.65;
        double cycleLength = 1.0 + trailLength;
        long seed = (long) entity.getId() * 31L + (long) beamIndex * 1013L;
        double randomOffset = ((seed * 1103515245L + 12345L) & 0xFFFFL)
                / 65536.0 * cycleLength;
        double travel = ((entity.tickCount + partialTick) * 0.14 + randomOffset) % cycleLength;
        double startT = Math.max(0.0, travel - trailLength);
        double endT = Math.min(1.0, travel);
        renderFlowSegment(startT, endT, start, control, end,
                poseStack, buffers, coreAlpha);
    }

    private void renderFlowSegment(double startT, double endT,
                                   Vec3 start, Vec3 control, Vec3 end,
                                   PoseStack poseStack, MultiBufferSource buffers,
                                   int coreAlpha) {
        if (endT - startT < 1.0E-4) return;
        List<Vec3> beam = sampleQuadraticRange(start, control, end, startT, endT);
        tubePath(beam, false, poseStack, buffers, CORE,
                0.025F, coreAlpha, LightTexture.FULL_BRIGHT);
    }

    private List<Vec3> sampleQuadraticRange(Vec3 start, Vec3 control, Vec3 end,
                                            double startT, double endT) {
        int segmentCount = Math.max(2, (int) Math.ceil(BEAM_SEGMENTS * (endT - startT)));
        List<Vec3> points = new ArrayList<>(segmentCount + 1);
        for (int i = 0; i <= segmentCount; i++) {
            double t = startT + (endT - startT) * i / segmentCount;
            double mt = 1.0 - t;
            points.add(start.scale(mt * mt)
                    .add(control.scale(2.0 * mt * t))
                    .add(end.scale(t * t)));
        }
        return points;
    }

    private void tubePath(List<Vec3> points, boolean closed, PoseStack poseStack,
                          MultiBufferSource buffers, RenderType renderType,
                          float width, int alpha, int packedLight) {
        tubePath(points, closed, !closed, poseStack, buffers, renderType, width, alpha, packedLight);
    }

    private void tubePath(List<Vec3> points, boolean closed, boolean taperOpenStart,
                          PoseStack poseStack, MultiBufferSource buffers, RenderType renderType,
                          float width, int alpha, int packedLight) {
        if (alpha <= 0 || points.size() < 2) return;
        VertexConsumer consumer = buffers.getBuffer(renderType);
        PoseStack.Pose pose = poseStack.last();
        List<Vec3[]> rings = new ArrayList<>(points.size());

        for (int i = 0; i < points.size(); i++) {
            int previousIndex = closed ? (i - 1 + points.size()) % points.size() : Math.max(i - 1, 0);
            int nextIndex = closed ? (i + 1) % points.size() : Math.min(i + 1, points.size() - 1);
            Vec3 direction = points.get(nextIndex).subtract(points.get(previousIndex));
            if (direction.lengthSqr() < 1.0E-8) continue;
            direction = direction.normalize();
            Vec3 binormal = direction.cross(new Vec3(0.0, 1.0, 0.0));
            if (binormal.lengthSqr() < 1.0E-8) {
                binormal = direction.cross(new Vec3(1.0, 0.0, 0.0));
            }
            binormal = binormal.normalize();
            Vec3 normal = direction.cross(binormal).normalize();
            float pathProgress = closed ? 1.0F : i / (points.size() - 1.0F);
            float taper = closed || !taperOpenStart ? 1.0F
                    : smoothStep(Math.min(1.0F, pathProgress / 0.45F));

            Vec3[] ring = new Vec3[TUBE_SIDES];
            for (int side = 0; side < TUBE_SIDES; side++) {
                double angle = Math.PI * 2.0 * side / TUBE_SIDES;
                Vec3 offset = binormal.scale(Math.cos(angle) * width * taper)
                        .add(normal.scale(Math.sin(angle) * width * taper));
                ring[side] = points.get(i).add(offset);
            }
            rings.add(ring);
        }

        int segmentCount = closed ? rings.size() : rings.size() - 1;
        for (int i = 0; i < segmentCount; i++) {
            Vec3[] first = rings.get(i);
            Vec3[] second = rings.get((i + 1) % rings.size());
            float pathProgress = closed ? 1.0F : i / (rings.size() - 1.0F);
            float taper = closed || !taperOpenStart ? 1.0F
                    : smoothStep(Math.min(1.0F, pathProgress / 0.45F));
            int segmentAlpha = (int) (alpha * taper);
            for (int side = 0; side < TUBE_SIDES; side++) {
                int next = (side + 1) % TUBE_SIDES;
                vertex(consumer, pose, first[side], segmentAlpha, packedLight);
                vertex(consumer, pose, first[next], segmentAlpha, packedLight);
                vertex(consumer, pose, second[next], segmentAlpha, packedLight);
                vertex(consumer, pose, second[side], segmentAlpha, packedLight);
            }
        }
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static double unit(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return (value & ((1L << 53) - 1L)) / (double) (1L << 53);
    }

    private void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point,
                        int alpha, int packedLight) {
        consumer.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .setColor(0xFF, 0xAA, 0x00, alpha)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(TuBarrierEntity entity) {
        return WHITE;
    }

    @Override
    public boolean shouldRender(TuBarrierEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        return true;
    }
}
