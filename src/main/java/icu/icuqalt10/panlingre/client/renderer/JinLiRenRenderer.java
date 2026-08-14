package icu.icuqalt10.panlingre.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.JinLiRenEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public class JinLiRenRenderer extends EntityRenderer<JinLiRenEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            PanlingRE.MODID, "textures/entity/jin_liren.png");

    // 0.3375 is exactly 75% of the old 0.45 half-size.
    private static final float HALF_SIZE = 0.3375F;
    private static final float HALF_DEPTH = 0.035F / 3.0F;
    private static final int TEXTURE_SIZE = 16;
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final RenderType TRAIL = RenderType.entityTranslucentEmissive(WHITE, false);
    private static final int TRAIL_COLOR = 0xE3D4D1;
    private static final int TRAIL_SIDES = 8;
    private static final float TRAIL_WIDTH = 0.09F;
    private static final String[] ALPHA_MASK = {
            "............###.",
            "..........#####.",
            ".........#####..",
            ".......#######..",
            "......########..",
            ".....########...",
            "....#########...",
            "....########....",
            "...########.....",
            "...########.....",
            ".....#####......",
            "....#####.......",
            "...#####........",
            "..#####.........",
            ".####...........",
            ".##............."
    };

    public JinLiRenRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(JinLiRenEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (entity.isCurved()) renderCurveTrail(entity, partialTick, poseStack, buffer);
        else renderTrail(entity, poseStack, buffer);
        if (entity.decaying()) {
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(
                Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(
                Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        // Keep the previous orientation.
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
        // The texture's boxed broad tip is the leading edge.
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));

        VertexConsumer vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        int light = LightTexture.FULL_BRIGHT;

        renderFace(vertices, pose, HALF_DEPTH, light, false);
        renderFace(vertices, pose, -HALF_DEPTH, light, true);
        renderPixelEdges(vertices, pose, light);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void renderTrail(JinLiRenEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        Vec3 previous = new Vec3(entity.xOld, entity.yOld, entity.zOld).subtract(entity.position());
        if (previous.lengthSqr() < 0.0001D) return;
        VertexConsumer vertices = buffer.getBuffer(TRAIL);
        PoseStack.Pose pose = poseStack.last();
        int red = TRAIL_COLOR >> 16 & 0xFF;
        int green = TRAIL_COLOR >> 8 & 0xFF;
        int blue = TRAIL_COLOR & 0xFF;
        Vec3 direction = previous.normalize();
        Vec3 binormal = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (binormal.lengthSqr() < 1.0E-6D) binormal = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        binormal = binormal.normalize();
        Vec3 normal = direction.cross(binormal).normalize();
        for (int side = 0; side < TRAIL_SIDES; side++) {
            int next = (side + 1) % TRAIL_SIDES;
            double a = side * Math.PI * 2.0D / TRAIL_SIDES;
            double b = next * Math.PI * 2.0D / TRAIL_SIDES;
            Vec3 o0 = binormal.scale(Math.cos(a) * TRAIL_WIDTH).add(normal.scale(Math.sin(a) * TRAIL_WIDTH));
            Vec3 o1 = binormal.scale(Math.cos(b) * TRAIL_WIDTH).add(normal.scale(Math.sin(b) * TRAIL_WIDTH));
            trailVertex(vertices, pose, o0, red, green, blue, 220);
            trailVertex(vertices, pose, o1, red, green, blue, 220);
            trailVertex(vertices, pose, previous.add(o1), red, green, blue, 0);
            trailVertex(vertices, pose, previous.add(o0), red, green, blue, 0);
        }
    }

    private static void renderCurveTrail(JinLiRenEntity entity, float partialTick, PoseStack poseStack,
                                         MultiBufferSource buffer) {
        float max = entity.progress(partialTick);
        float decay = entity.decay(partialTick);
        float min = entity.decaying() ? decay : 0.0F;
        if (max <= min || max <= 0.0F) return;
        Vec3 entityPos = entity.getPosition(partialTick);
        List<Vec3> points = new ArrayList<>();
        for (float t = min; t <= max + 0.006F; t += 0.006F) {
            float sample = Math.min(t, max);
            points.add(JinLiRenEntity.cubicBezier(sample, entity.p0(), entity.p1(),
                    entity.p2(), entity.p3()).subtract(entityPos));
            if (sample >= max) break;
        }
        if (points.size() < 2) return;
        int alpha = (int) (210.0F * (entity.decaying() ? 1.0F - decay : 1.0F));
        curveTube(points, poseStack, buffer, TRAIL, 0.06F,
                0xE3, 0xD4, 0xD1, alpha);
    }

    /** Same sampled-ring tube construction as ZhuRiArrowRenderer. */
    private static void curveTube(List<Vec3> points, PoseStack poseStack,
                                  MultiBufferSource buffer, RenderType renderType,
                                  float width, int red, int green, int blue, int alpha) {
        VertexConsumer vertices = buffer.getBuffer(renderType);
        PoseStack.Pose pose = poseStack.last();
        List<Vec3[]> rings = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            Vec3 current = points.get(i);
            Vec3 next = points.get(Math.min(i + 1, points.size() - 1));
            Vec3 previous = points.get(Math.max(i - 1, 0));
            Vec3 tangent = next.subtract(previous);
            if (tangent.lengthSqr() < 1.0E-7D) continue;
            Vec3 direction = tangent.normalize();
            Vec3 binormal = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (binormal.lengthSqr() < 1.0E-6D) binormal = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
            binormal = binormal.normalize();
            Vec3 normal = direction.cross(binormal).normalize();
            float pathProgress = i / (points.size() - 1.0F);
            float taper = smoothStep(Math.min(1.0F, pathProgress / 0.45F));
            Vec3[] ring = new Vec3[TRAIL_SIDES];
            for (int side = 0; side < TRAIL_SIDES; side++) {
                double angle = side * Math.PI * 2.0D / TRAIL_SIDES;
                ring[side] = current.add(binormal.scale(Math.cos(angle) * width * taper))
                        .add(normal.scale(Math.sin(angle) * width * taper));
            }
            rings.add(ring);
        }
        for (int i = 0; i < rings.size() - 1; i++) {
            float pathProgress = i / (rings.size() - 1.0F);
            float taper = smoothStep(Math.min(1.0F, pathProgress / 0.45F));
            int segmentAlpha = (int) (alpha * taper);
            Vec3[] first = rings.get(i), second = rings.get(i + 1);
            for (int side = 0; side < TRAIL_SIDES; side++) {
                int next = (side + 1) % TRAIL_SIDES;
                trailVertex(vertices, pose, first[side], red, green, blue, segmentAlpha);
                trailVertex(vertices, pose, first[next], red, green, blue, segmentAlpha);
                trailVertex(vertices, pose, second[next], red, green, blue, segmentAlpha);
                trailVertex(vertices, pose, second[side], red, green, blue, segmentAlpha);
            }
        }
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static void trailVertex(VertexConsumer vertices, PoseStack.Pose pose, Vec3 point,
                                    int red, int green, int blue, int alpha) {
        vertices.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .setColor(red, green, blue, alpha).setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static void renderFace(VertexConsumer vertices, PoseStack.Pose pose,
                                   float z, int light, boolean reverse) {
        if (!reverse) {
            vertex(vertices, pose, -HALF_SIZE, -HALF_SIZE, z, 0.0F, 1.0F, light, 0, 0, 1);
            vertex(vertices, pose,  HALF_SIZE, -HALF_SIZE, z, 1.0F, 1.0F, light, 0, 0, 1);
            vertex(vertices, pose,  HALF_SIZE,  HALF_SIZE, z, 1.0F, 0.0F, light, 0, 0, 1);
            vertex(vertices, pose, -HALF_SIZE,  HALF_SIZE, z, 0.0F, 0.0F, light, 0, 0, 1);
        } else {
            vertex(vertices, pose, -HALF_SIZE,  HALF_SIZE, z, 0.0F, 0.0F, light, 0, 0, -1);
            vertex(vertices, pose,  HALF_SIZE,  HALF_SIZE, z, 1.0F, 0.0F, light, 0, 0, -1);
            vertex(vertices, pose,  HALF_SIZE, -HALF_SIZE, z, 1.0F, 1.0F, light, 0, 0, -1);
            vertex(vertices, pose, -HALF_SIZE, -HALF_SIZE, z, 0.0F, 1.0F, light, 0, 0, -1);
        }
    }

    /** Extrudes every exposed opaque-pixel edge, like Minecraft's generated item model. */
    private static void renderPixelEdges(VertexConsumer vertices, PoseStack.Pose pose, int light) {
        float pixel = HALF_SIZE * 2.0F / TEXTURE_SIZE;
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                if (!opaque(x, y)) continue;

                float x0 = -HALF_SIZE + x * pixel;
                float x1 = x0 + pixel;
                float y1 = HALF_SIZE - y * pixel;
                float y0 = y1 - pixel;
                float u = (x + 0.5F) / TEXTURE_SIZE;
                float v = (y + 0.5F) / TEXTURE_SIZE;

                if (!opaque(x - 1, y)) side(vertices, pose, x0, y0, x0, y1, u, v, light, -1, 0);
                if (!opaque(x + 1, y)) side(vertices, pose, x1, y1, x1, y0, u, v, light,  1, 0);
                if (!opaque(x, y - 1)) side(vertices, pose, x0, y1, x1, y1, u, v, light,  0, 1);
                if (!opaque(x, y + 1)) side(vertices, pose, x1, y0, x0, y0, u, v, light,  0,-1);
            }
        }
    }

    private static void side(VertexConsumer vertices, PoseStack.Pose pose,
                             float x0, float y0, float x1, float y1,
                             float u, float v, int light, float nx, float ny) {
        vertex(vertices, pose, x0, y0, -HALF_DEPTH, u, v, light, nx, ny, 0);
        vertex(vertices, pose, x1, y1, -HALF_DEPTH, u, v, light, nx, ny, 0);
        vertex(vertices, pose, x1, y1,  HALF_DEPTH, u, v, light, nx, ny, 0);
        vertex(vertices, pose, x0, y0,  HALF_DEPTH, u, v, light, nx, ny, 0);
    }

    private static boolean opaque(int x, int y) {
        return x >= 0 && x < TEXTURE_SIZE && y >= 0 && y < TEXTURE_SIZE
                && ALPHA_MASK[y].charAt(x) == '#';
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose,
                               float x, float y, float z, float u, float v, int light,
                               float nx, float ny, float nz) {
        vertices.addVertex(pose, x, y, z)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    @Override
    public ResourceLocation getTextureLocation(JinLiRenEntity entity) {
        return TEXTURE;
    }
}
