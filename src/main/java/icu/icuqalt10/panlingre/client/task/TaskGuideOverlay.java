package icu.icuqalt10.panlingre.client.task;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.task.TaskGuideData;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public final class TaskGuideOverlay implements LayeredDraw.Layer {
    public static final TaskGuideOverlay INSTANCE = new TaskGuideOverlay();
    private static final ResourceLocation WAYPOINT = ResourceLocation.fromNamespaceAndPath(
            PanlingRE.MODID, "textures/gui/task/waypoint.png"
    );
    private static final int ICON_TEXTURE_SIZE = 20;
    private static final int ICON_DISPLAY_SIZE = 12;
    private static final int EDGE_MARGIN = 16;
    private static final double WAYPOINT_RANGE = 100.0;

    private TaskGuideOverlay() {
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) {
            return;
        }
        ClientTaskGuideState.task().ifPresent(task -> {
            renderTitle(graphics, minecraft, task);
            if (task.type().hasPosition() && task.target().isPresent()) {
                renderWaypoint(graphics, minecraft, task.target().get().pos());
            }
        });
    }

    private static void renderTitle(GuiGraphics graphics, Minecraft minecraft, TaskGuideData task) {
        int maxWidth = Math.min(220, Math.max(80, minecraft.getWindow().getGuiScaledWidth() / 3));
        List<FormattedCharSequence> lines = minecraft.font.split(task.title(), maxWidth);
        int right = minecraft.getWindow().getGuiScaledWidth() - 8;
        int bottom = minecraft.getWindow().getGuiScaledHeight() - 40 + minecraft.font.lineHeight * 2;
        int firstY = bottom - Math.max(0, lines.size() - 1) * minecraft.font.lineHeight;

        for (int index = 0; index < lines.size(); index++) {
            FormattedCharSequence line = lines.get(index);
            int x = right - minecraft.font.width(line);
            graphics.drawString(minecraft.font, line, x, firstY + index * minecraft.font.lineHeight, 0xFFFFFF, true);
        }
    }

    private static void renderWaypoint(
            GuiGraphics graphics,
            Minecraft minecraft,
            TaskGuideData.ExactPosition target
    ) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 targetPosition = new Vec3(target.x() + 0.5, target.y() + 1.5, target.z() + 0.5);
        Vec3 offset = targetPosition.subtract(camera.getPosition());
        double distance = offset.length();
        if (distance > WAYPOINT_RANGE || distance < 3.0) {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        float centerX = width * 0.5F;
        float centerY = height * 0.5F;

        Vector3f leftVector = camera.getLeftVector();
        Vector3f upVector = camera.getUpVector();
        Vector3f forwardVector = camera.getLookVector();
        float left = dot(offset, leftVector);
        float up = dot(offset, upVector);
        float forward = dot(offset, forwardVector);
        float right = -left;

        double fov = minecraft.options.fov().get();
        float focalLength = (float)(height / (2.0 * Math.tan(Math.toRadians(fov) * 0.5)));
        float projectedX = centerX;
        float projectedY = centerY;
        if (forward > 0.01F) {
            projectedX += right / forward * focalLength;
            projectedY -= up / forward * focalLength;
        }

        boolean onScreen = forward > 0.01F
                && projectedX >= EDGE_MARGIN
                && projectedX <= width - EDGE_MARGIN
                && projectedY >= EDGE_MARGIN
                && projectedY <= height - EDGE_MARGIN;

        float markerX;
        float markerY;
        float rotation = 0.0F;
        if (onScreen) {
            markerX = projectedX;
            markerY = projectedY;
        } else {
            float directionX;
            float directionY;
            if (forward > 0.01F) {
                directionX = projectedX - centerX;
                directionY = projectedY - centerY;
            } else {
                directionX = -right;
                directionY = up;
                if (Math.abs(directionX) + Math.abs(directionY) < 0.001F) {
                    directionY = 1.0F;
                }
            }

            float horizontalScale = (centerX - EDGE_MARGIN) / Math.max(0.001F, Math.abs(directionX));
            float verticalScale = (centerY - EDGE_MARGIN) / Math.max(0.001F, Math.abs(directionY));
            float scale = Math.min(horizontalScale, verticalScale);
            markerX = Mth.clamp(centerX + directionX * scale, EDGE_MARGIN, width - EDGE_MARGIN);
            markerY = Mth.clamp(centerY + directionY * scale, EDGE_MARGIN, height - EDGE_MARGIN);
            rotation = (float)Math.toDegrees(Math.atan2(directionY, directionX)) - 90.0F;
        }

        float alpha = distance > 90.0 ? (float)((100.0 - distance) / 10.0) : 1.0F;
        alpha = Mth.clamp(alpha, 0.0F, 1.0F);
        RenderSystem.enableBlend();
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.pose().pushPose();
        graphics.pose().translate(markerX, markerY, 0.0F);
        if (!onScreen) {
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(rotation));
        }
        float iconScale = (float)ICON_DISPLAY_SIZE / ICON_TEXTURE_SIZE;
        graphics.pose().scale(iconScale, iconScale, 1.0F);
        graphics.blit(
                WAYPOINT,
                -ICON_TEXTURE_SIZE / 2,
                -ICON_TEXTURE_SIZE / 2,
                0,
                0,
                ICON_TEXTURE_SIZE,
                ICON_TEXTURE_SIZE,
                ICON_TEXTURE_SIZE,
                ICON_TEXTURE_SIZE
        );
        graphics.pose().popPose();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        String distanceText = Math.round(distance) + "m";
        int textColor = ((int)(alpha * 255.0F) << 24) | 0xFFFFFF;
        int textX = Math.round(markerX) - minecraft.font.width(distanceText) / 2;
        int textY = Math.round(markerY) + ICON_DISPLAY_SIZE / 2;
        graphics.drawString(minecraft.font, distanceText, textX, textY, textColor, true);
        RenderSystem.disableBlend();
    }

    private static float dot(Vec3 vector, Vector3f direction) {
        return (float)(vector.x * direction.x() + vector.y * direction.y() + vector.z * direction.z());
    }
}
