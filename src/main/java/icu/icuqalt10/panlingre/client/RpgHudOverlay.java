package icu.icuqalt10.panlingre.client;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.init.ModAttributes;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class RpgHudOverlay implements LayeredDraw.Layer {
    // 路径前缀缩写
    private static final String HUD_PATH = "textures/gui/hud/";
    private static final ResourceLocation HEALTH_E = loc("health_empty.png");
    private static final ResourceLocation HEALTH_F = loc("health_full.png");
    private static final ResourceLocation HUNGRY_E = loc("hungry_empty.png");
    private static final ResourceLocation HUNGRY_F = loc("hungry_full.png");
    private static final ResourceLocation LINGQI_E = loc("lingqi_empty.png");
    private static final ResourceLocation LINGQI_F = loc("lingqi_full.png");

    // 图标路径
    private static final ResourceLocation ARMOR_ICON = loc("armor.png");
    private static final ResourceLocation MANA_ICON = loc("falizhi.png");
    private static final ResourceLocation OXYGEN_ICON = loc("oxygen.png");

    private static ResourceLocation loc(String name) {
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, HUD_PATH + name);
    }

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        Player player = mc.player;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;

        // 坐标对齐基准
        int leftX = centerX - 91;
        int rightX = centerX + 10;
        int rowBottomY = screenHeight - 39; // 下行 (生命、饥饿)
        int rowTopY = screenHeight - 49;    // 上行 (灵气、杂项)

        // --- 1. 左下：生命值 + 伤害吸收 (§e 为黄色代码) ---
        float abs = player.getAbsorptionAmount();
        String hpBase = String.format("%.0f/%.0f", player.getHealth(), player.getMaxHealth());
        String hpExtra = abs > 0 ? " §e+" + (int)abs : "";
        drawBarBottom(guiGraphics, leftX, rowBottomY, player.getHealth(), player.getMaxHealth(),
                HEALTH_E, HEALTH_F, hpBase + hpExtra);

        // --- 2. 右下：饥饿值 + 饱和度 ---
        float sat = player.getFoodData().getSaturationLevel();
        String foodBase = String.format("%d/20", player.getFoodData().getFoodLevel());
        String foodExtra = sat > 0 ? " §e+" + (int)sat : "";
        drawBarBottom(guiGraphics, rightX, rowBottomY, (float)player.getFoodData().getFoodLevel(), 20f,
                HUNGRY_E, HUNGRY_F, foodBase + foodExtra);

        // --- 3. 右上：灵气值 (通过 Attachment 获取) ---
        float currentLq = LingQiData.ClientLingQiData.getCurrent();
        float maxLq = LingQiData.ClientLingQiData.getMax();
        drawBarTop(guiGraphics, rightX, rowTopY, currentLq, maxLq, LINGQI_E, LINGQI_F,
                String.format("%.0f/%.0f", currentLq, maxLq));

        // --- 4. 左上：图标数值组 (缩小显示) ---
        // 护甲
        drawIconValue(guiGraphics, leftX, rowTopY, ARMOR_ICON, String.valueOf(player.getArmorValue()));
        // 法力
        float mana = (float)player.getAttributeValue(ModAttributes.FALIZHI);
        drawIconValue(guiGraphics, leftX + 30, rowTopY, MANA_ICON, String.format("%.2f", mana));
        // 氧气
        if (player.getAirSupply() < player.getMaxAirSupply()) {
            drawIconValue(guiGraphics, leftX + 65, rowTopY, OXYGEN_ICON, String.valueOf(player.getAirSupply() / 20));
        }
    }

    private void drawBarBottom(GuiGraphics g, int x, int y, float cur, float max, ResourceLocation e, ResourceLocation f, String text) {
        g.blit(e, x, y, 0, 0, 81, 9, 81, 9);
        int w = (int)(Math.min(cur / max, 1.0f) * 81);
        if (w > 0) g.blit(f, x, y, 0, 0, w, 9, 81, 9);

        renderScaledText(g, text, x, y, 81);
    }

    private void drawBarTop(GuiGraphics g, int x, int y, float cur, float max, ResourceLocation e, ResourceLocation f, String text) {
        g.blit(e, x, y, 0, 0, 81, 9, 81, 9);
        int w = (int)(Math.min(cur / max, 1.0f) * 81);
        if (w > 0) g.blit(f, x, y, 0, 0, w, 9, 81, 9);

        renderScaledText(g, text, x, y+1, 81);
    }

    private void drawIconValue(GuiGraphics g, int x, int y, ResourceLocation icon, String val) {
        g.blit(icon, x, y, 0, 0, 9, 9, 9, 9);
        renderScaledText(g, val, x + 11, y + 1, 0); // 0表示不居中，直接靠图标排
    }

    private void renderScaledText(GuiGraphics g, String text, int x, int y, int barWidth) {
        g.pose().pushPose();
        g.pose().scale((float) 0.7, (float) 0.7, 1.0f);
        int tw = Minecraft.getInstance().font.width(text);

        float tx = (barWidth > 0) ? (x + (barWidth - tw * (float) 0.7) / 2f) / (float) 0.7 : x / (float) 0.7;
        float ty = (y + (9 - 8 * (float) 0.7) / 2f) / (float) 0.7;

        g.drawString(Minecraft.getInstance().font, text, (int)tx, (int)ty, 0xFFFFFF, true);
        g.pose().popPose();
    }
}