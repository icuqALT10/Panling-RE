package icu.icuqalt10.panlingre.client;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Locale;

/** Compact casting progress bar rendered immediately below the crosshair. */
public final class SkillCastOverlay implements LayeredDraw.Layer {
    public static final SkillCastOverlay INSTANCE = new SkillCastOverlay();

    private static final ResourceLocation EMPTY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            PanlingRE.MODID, "textures/gui/hud/casting_empty.png");
    private static final ResourceLocation FULL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            PanlingRE.MODID, "textures/gui/hud/casting_full.png");

    private static final int TEXTURE_WIDTH = 42;
    private static final int TEXTURE_HEIGHT = 18;
    private static final int BAR_WIDTH = TEXTURE_WIDTH / 2;
    private static final int BAR_HEIGHT = TEXTURE_HEIGHT / 2;

    private SkillCastOverlay() {
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null) return;

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        ClientSkillCastState.CastView cast =
                ClientSkillCastState.getCastView(minecraft.player, partialTick);
        if (cast == null) return;

        int x = graphics.guiWidth() / 2 - BAR_WIDTH / 2;
        int y = graphics.guiHeight() / 2 + 13;
        drawTexture(graphics, EMPTY_TEXTURE, x, y);

        int progressWidth = Mth.clamp(
                Math.round(BAR_WIDTH * cast.progress()), 0, BAR_WIDTH);
        if (progressWidth > 0) {
            graphics.enableScissor(x, y, x + progressWidth, y + BAR_HEIGHT);
            drawTexture(graphics, FULL_TEXTURE, x, y);
            graphics.disableScissor();
        }

        String seconds = String.format(Locale.ROOT, "%.1f", cast.remainingSeconds());
        int textWidth = Math.max(1, minecraft.font.width(seconds));
        float textScale = Math.min(1.0F, Math.min(
                (BAR_WIDTH - 2.0F) / textWidth,
                (BAR_HEIGHT - 1.0F) / minecraft.font.lineHeight));

        graphics.pose().pushPose();
        graphics.pose().translate(
                x + BAR_WIDTH / 2.0F,
                y + (BAR_HEIGHT - minecraft.font.lineHeight * textScale) / 2.0F,
                0.0F);
        graphics.pose().scale(textScale, textScale, 1.0F);
        graphics.drawCenteredString(minecraft.font, seconds, 0, 0, 0xFFFFFF);
        graphics.pose().popPose();
    }

    private static void drawTexture(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
        graphics.blit(texture, x, y, BAR_WIDTH, BAR_HEIGHT,
                0.0F, 0.0F, TEXTURE_WIDTH, TEXTURE_HEIGHT,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }
}
