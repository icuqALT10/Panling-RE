package icu.icuqalt10.panlingre.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import icu.icuqalt10.panlingre.entity.Ys3DomainEntity;
import icu.icuqalt10.panlingre.entity.Ys3MuDomainEntity;
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

class Ys3DomainRenderer<T extends Ys3DomainEntity> extends EntityRenderer<T> {
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final RenderType EMISSIVE = RenderType.entityTranslucentEmissive(WHITE, false);
    private static final int TUBE_SIDES = 8;
    private static final int RING_SEGMENTS = 128;
    private static final int BEAM_SEGMENTS = 28;
    private static final int BEAM_COUNT = 20;
    private final int red;
    private final int green;
    private final int blue;

    protected Ys3DomainRenderer(EntityRendererProvider.Context context, int color) {
        super(context);
        red = color >> 16 & 0xFF;
        green = color >> 8 & 0xFF;
        blue = color & 0xFF;
    }

    @Override
    public void render(T entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int light) {
        if (entity.isActive()) renderDomain(entity, partialTick, poseStack, buffers);

        float flatten = smoothStep(entity.flattenProgress(partialTick));
        float expand = smoothStep(entity.expandProgress(partialTick));
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.06D, 0.0D);
        float spinAge = Math.max(0.0F, entity.tickCount + partialTick - Ys3DomainEntity.LAND_AND_FLATTEN_TICKS);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F * flatten));
        poseStack.mulPose(Axis.ZP.rotationDegrees(spinAge * 7.2F));
        float scale = 1.0F + (entity.getItemScale() - 1.0F) * expand;
        float thicknessScale = 1.0F + 3.0F * expand;
        poseStack.scale(scale, scale, thicknessScale);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                entity.getItem(), ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, poseStack, buffers, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    private void renderDomain(T entity, float partialTick,
                              PoseStack poseStack, MultiBufferSource buffers) {
        List<Vec3> ring = new ArrayList<>(RING_SEGMENTS);
        for (int i = 0; i < RING_SEGMENTS; i++) {
            double angle = Math.PI * 2.0D * i / RING_SEGMENTS;
            ring.add(new Vec3(Math.cos(angle) * entity.getDomainRadius(),
                    0.08D, Math.sin(angle) * entity.getDomainRadius()));
        }
        tubePath(ring, true, poseStack, buffers, 0.045F, 120);
        FacetedOctahedronRenderer.render(new Vec3(0.0D, 5.0D, 0.0D), 0.5D,
                red << 16 | green << 8 | blue, 150, poseStack, buffers);

        double trailLength = 0.48D;
        double cycleLength = 1.0D + trailLength;
        double time = (entity.tickCount + partialTick) * 0.085D;
        for (int i = 0; i < BEAM_COUNT; i++) {
            double rawTravel = time + i * cycleLength / BEAM_COUNT;
            long cycle = (long) Math.floor(rawTravel / cycleLength);
            double travel = rawTravel - cycle * cycleLength;
            double startT = Math.max(0.0D, travel - trailLength);
            double endT = Math.min(1.0D, travel);
            if (endT - startT < 1.0E-4D) continue;

            long seed = ((long) entity.getId() << 32) ^ cycle * 0x9E3779B97F4A7C15L ^ i * 1013L;
            double angle = unit(seed) * Math.PI * 2.0D;
            double minimumRadius = 1.5D;
            double maximumRadius = entity.getDomainRadius() - 0.15D;
            double radius = Math.sqrt(minimumRadius * minimumRadius
                    + unit(seed + 1L) * (maximumRadius * maximumRadius - minimumRadius * minimumRadius));
            Vec3 start = new Vec3(Math.cos(angle) * radius, 0.12D, Math.sin(angle) * radius);
            Vec3 control = new Vec3(start.x * 0.58D, 2.8D + unit(seed + 2L) * 1.2D,
                    start.z * 0.58D);
            Vec3 end = new Vec3(0.0D, 5.0D, 0.0D);
            tubePath(sampleQuadratic(start, control, end, startT, endT), false,
                    poseStack, buffers, 0.04F, 190);
        }
    }

    private List<Vec3> sampleQuadratic(Vec3 start, Vec3 control, Vec3 end,
                                       double startT, double endT) {
        int count = Math.max(2, (int) Math.ceil(BEAM_SEGMENTS * (endT - startT)));
        List<Vec3> result = new ArrayList<>(count + 1);
        for (int i = 0; i <= count; i++) {
            double t = startT + (endT - startT) * i / count;
            double mt = 1.0D - t;
            result.add(start.scale(mt * mt).add(control.scale(2.0D * mt * t)).add(end.scale(t * t)));
        }
        return result;
    }

    private void tubePath(List<Vec3> points, boolean closed, PoseStack poseStack,
                          MultiBufferSource buffers, float width, int alpha) {
        if (points.size() < 2) return;
        VertexConsumer consumer = buffers.getBuffer(EMISSIVE);
        PoseStack.Pose pose = poseStack.last();
        List<Vec3[]> rings = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++) {
            int previous = closed ? (i - 1 + points.size()) % points.size() : Math.max(i - 1, 0);
            int next = closed ? (i + 1) % points.size() : Math.min(i + 1, points.size() - 1);
            Vec3 direction = points.get(next).subtract(points.get(previous));
            if (direction.lengthSqr() < 1.0E-8D) continue;
            direction = direction.normalize();
            Vec3 binormal = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (binormal.lengthSqr() < 1.0E-8D) binormal = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
            binormal = binormal.normalize();
            Vec3 normal = direction.cross(binormal).normalize();
            float pathProgress = closed ? 1.0F : i / (points.size() - 1.0F);
            float taper = closed ? 1.0F : smoothStep(Math.min(1.0F, pathProgress / 0.4F));
            Vec3[] crossSection = new Vec3[TUBE_SIDES];
            for (int side = 0; side < TUBE_SIDES; side++) {
                double angle = Math.PI * 2.0D * side / TUBE_SIDES;
                crossSection[side] = points.get(i)
                        .add(binormal.scale(Math.cos(angle) * width * taper))
                        .add(normal.scale(Math.sin(angle) * width * taper));
            }
            rings.add(crossSection);
        }

        int segmentCount = closed ? rings.size() : rings.size() - 1;
        for (int i = 0; i < segmentCount; i++) {
            Vec3[] first = rings.get(i);
            Vec3[] second = rings.get((i + 1) % rings.size());
            float progress = closed ? 1.0F : i / (rings.size() - 1.0F);
            int segmentAlpha = closed ? alpha : (int) (alpha * smoothStep(Math.min(1.0F, progress / 0.4F)));
            for (int side = 0; side < TUBE_SIDES; side++) {
                int next = (side + 1) % TUBE_SIDES;
                vertex(consumer, pose, first[side], segmentAlpha);
                vertex(consumer, pose, first[next], segmentAlpha);
                vertex(consumer, pose, second[next], segmentAlpha);
                vertex(consumer, pose, second[side], segmentAlpha);
            }
        }
    }

    private static double unit(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return (value & ((1L << 53) - 1L)) / (double) (1L << 53);
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point, int alpha) {
        consumer.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .setColor(red, green, blue, alpha).setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) { return WHITE; }

    @Override
    public boolean shouldRender(T entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) { return true; }
}

public class Ys3MuDomainRenderer extends Ys3DomainRenderer<Ys3MuDomainEntity> {
    public Ys3MuDomainRenderer(EntityRendererProvider.Context context) {
        super(context, 0x66FF88);
    }
}
