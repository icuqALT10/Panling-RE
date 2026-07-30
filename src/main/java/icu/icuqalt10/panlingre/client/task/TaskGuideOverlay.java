package icu.icuqalt10.panlingre.client.task;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.event.ModKeyBindings;
import icu.icuqalt10.panlingre.task.TaskGuideData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;

@EventBusSubscriber(modid = PanlingRE.MODID, value = Dist.CLIENT)
public final class TaskGuideOverlay implements LayeredDraw.Layer {
    public static final TaskGuideOverlay INSTANCE = new TaskGuideOverlay();
    private static final ResourceLocation WAYPOINT = ResourceLocation.fromNamespaceAndPath(
            PanlingRE.MODID, "textures/gui/task/waypoint.png"
    );
    private static final int ICON_TEXTURE_SIZE = 20;
    private static final int ICON_DISPLAY_SIZE = 12;
    private static final int EDGE_MARGIN = ICON_DISPLAY_SIZE / 2;
    private static final double WAYPOINT_RANGE = 9999.0;
    private static final float HINT_SCALE = 0.7F;
    private static Matrix4f worldModelView;
    private static Matrix4f worldProjection;
    private static Vec3 renderedCameraPosition;

    private TaskGuideOverlay() {
    }

    @SubscribeEvent
    public static void captureWorldProjection(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        worldModelView = new Matrix4f(event.getModelViewMatrix());
        worldProjection = new Matrix4f(event.getProjectionMatrix());
        renderedCameraPosition = event.getCamera().getPosition();
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) {
            return;
        }
        ClientTaskGuideState.task().ifPresent(task -> {
            renderTitle(graphics, minecraft, task);
            ClientTaskGuideState.entry().ifPresent(entry -> {
                if (ClientTaskGuideState.isGuidanceVisible()
                        && entry.target().isPresent()) {
                    renderWaypoint(graphics, minecraft, entry.target().get().pos());
                }
            });
        });
    }

    private static void renderTitle(GuiGraphics graphics, Minecraft minecraft, TaskGuideData task) {
        int maxWidth = Math.min(220, Math.max(80, minecraft.getWindow().getGuiScaledWidth() / 3));
        List<FormattedCharSequence> lines = minecraft.font.split(task.title(), maxWidth);
        int right = minecraft.getWindow().getGuiScaledWidth() - 8;
        int bottom = minecraft.getWindow().getGuiScaledHeight() - 40 + minecraft.font.lineHeight * 2;
        int hintHeight = Mth.ceil(minecraft.font.lineHeight * HINT_SCALE);
        int titleBottom = bottom - hintHeight - 2;
        int firstY = titleBottom - Math.max(0, lines.size() - 1) * minecraft.font.lineHeight;

        for (int index = 0; index < lines.size(); index++) {
            FormattedCharSequence line = lines.get(index);
            int x = right - minecraft.font.width(line);
            graphics.drawString(minecraft.font, line, x, firstY + index * minecraft.font.lineHeight, 0xFFFFFF, true);
        }

        FormattedCharSequence hint = Component.translatable(
                "overlay.panlingre.task_guide.toggle_hint",
                ModKeyBindings.TASK_GUIDE_TOGGLE.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.GOLD)
        ).getVisualOrderText();
        graphics.pose().pushPose();
        graphics.pose().scale(HINT_SCALE, HINT_SCALE, 1.0F);
        int hintX = (int)((right - minecraft.font.width(hint) * HINT_SCALE) / HINT_SCALE);
        int hintY = (int)(bottom / HINT_SCALE);
        graphics.drawString(minecraft.font, hint, hintX, hintY, 0xFFFFFF, true);
        graphics.pose().popPose();
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
        Vector3f forwardVector = camera.getLookVector();
        float left = dot(offset, leftVector);
        float forward = dot(offset, forwardVector);
        float right = -left;

        ScreenProjection projection = projectToScreen(targetPosition, width, height);
        if (projection == null) {
            return;
        }
        float projectedX = projection.x();
        float projectedY = projection.y();
        boolean inFront = projection.inFront();

        boolean onScreen = inFront
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
            if (inFront) {
                directionX = projectedX - centerX;
                directionY = projectedY - centerY;
            } else {
                // 后半球不能把水平分量反转，否则目标刚越过 90° 就会跳到屏幕另一侧。
                // 使用 -forward 作为向下分量，使标记从左右边缘连续滑向屏幕下边；
                // 正后方时 directionX 为 0，标记自然位于屏幕底部中央。
                directionX = right;
                directionY = -forward;
            }

            float horizontalScale = (centerX - EDGE_MARGIN) / Math.max(0.001F, Math.abs(directionX));
            float verticalScale = (centerY - EDGE_MARGIN) / Math.max(0.001F, Math.abs(directionY));
            float scale = Math.min(horizontalScale, verticalScale);
            markerX = Mth.clamp(centerX + directionX * scale, EDGE_MARGIN, width - EDGE_MARGIN);
            markerY = Mth.clamp(centerY + directionY * scale, EDGE_MARGIN, height - EDGE_MARGIN);
            rotation = (float)Math.toDegrees(Math.atan2(directionY, directionX)) - 90.0F;
        }

        float fadeStart = (float)WAYPOINT_RANGE - 10.0F;
        float alpha = distance > fadeStart
                ? (float)((WAYPOINT_RANGE - distance) / 10.0)
                : 1.0F;
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

    private static ScreenProjection projectToScreen(Vec3 target, int width, int height) {
        if (worldModelView == null || worldProjection == null || renderedCameraPosition == null) {
            return null;
        }

        Vec3 relative = target.subtract(renderedCameraPosition);
        Vector4f clip = new Vector4f(
                (float)relative.x,
                (float)relative.y,
                (float)relative.z,
                1.0F
        );
        worldModelView.transform(clip);
        worldProjection.transform(clip);

        boolean inFront = clip.w > 0.0F;
        float inverseW = 1.0F / Math.max(0.0001F, Math.abs(clip.w));
        float normalizedX = clip.x * inverseW;
        float normalizedY = clip.y * inverseW;
        return new ScreenProjection(
                (normalizedX + 1.0F) * 0.5F * width,
                (1.0F - normalizedY) * 0.5F * height,
                inFront
        );
    }

    private record ScreenProjection(float x, float y, boolean inFront) {
    }
}
