package icu.icuqalt10.panlingre.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

final class FacetedOctahedronRenderer {
    private static final ResourceLocation WHITE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final RenderType FACES = RenderType.entityTranslucentEmissive(WHITE, false);

    private FacetedOctahedronRenderer() { }

    static void render(Vec3 center, double radius, int color, int alpha,
                       PoseStack poseStack, MultiBufferSource buffers) {
        Vec3[] vertices = {
                center.add(0.0D, radius, 0.0D), center.add(0.0D, -radius, 0.0D),
                center.add(radius, 0.0D, 0.0D), center.add(0.0D, 0.0D, radius),
                center.add(-radius, 0.0D, 0.0D), center.add(0.0D, 0.0D, -radius)
        };
        int[][] faces = {
                {0, 3, 2}, {0, 4, 3}, {0, 5, 4}, {0, 2, 5},
                {1, 2, 3}, {1, 3, 4}, {1, 4, 5}, {1, 5, 2}
        };
        VertexConsumer consumer = buffers.getBuffer(FACES);
        PoseStack.Pose pose = poseStack.last();
        int baseRed = color >> 16 & 0xFF;
        int baseGreen = color >> 8 & 0xFF;
        int baseBlue = color & 0xFF;
        for (int i = 0; i < faces.length; i++) {
            Vec3 a = vertices[faces[i][0]], b = vertices[faces[i][1]], c = vertices[faces[i][2]];
            Vec3 normal = b.subtract(a).cross(c.subtract(a)).normalize();
            float shade = 0.68F + (i % 4) * 0.09F;
            int red = Math.min(255, Math.round(baseRed * shade));
            int green = Math.min(255, Math.round(baseGreen * shade));
            int blue = Math.min(255, Math.round(baseBlue * shade));
            vertex(consumer, pose, a, normal, red, green, blue, alpha);
            vertex(consumer, pose, b, normal, red, green, blue, alpha);
            vertex(consumer, pose, c, normal, red, green, blue, alpha);
            vertex(consumer, pose, c, normal, red, green, blue, alpha);
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point, Vec3 normal,
                               int red, int green, int blue, int alpha) {
        consumer.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .setColor(red, green, blue, alpha).setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }
}
