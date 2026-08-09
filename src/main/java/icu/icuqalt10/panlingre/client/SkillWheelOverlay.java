package icu.icuqalt10.panlingre.client;

import com.mojang.math.Axis;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.network.SkillWheelPayload;
import icu.icuqalt10.panlingre.skill.ClientSkillState;
import icu.icuqalt10.panlingre.skill.SkillData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;

public class SkillWheelOverlay implements LayeredDraw.Layer {

    public static final SkillWheelOverlay INSTANCE = new SkillWheelOverlay();

    private static final ResourceLocation WHEEL_BG =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/gui/skill/background.png");
    private static final ResourceLocation LINE_BG =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/gui/skill/line.png");
    private static final ResourceLocation SKILL_BG =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/gui/skill/skill_background.png");
    private static final ResourceLocation SKILL_SEL =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/gui/skill/skill_selection.png");

    private static final int BG_SIZE = 36;
    private static final float RING_OUTER = 75f;
    private static final float RING_INNER = 16f;
    private static final int ICON_SIZE = 20;
    private static final float ICON_RADIUS = 65f;

    public boolean active;
    private int hoveredIndex = -1;

    public void open() {
        if (ClientSkillState.getSkillCount() == 0) return;
        active = true;
        hoveredIndex = -1;
        Minecraft.getInstance().mouseHandler.releaseMouse();
    }

    public void close() {
        active = false;
        if (hoveredIndex >= 0 && hoveredIndex < ClientSkillState.getSkillCount()) {
            var skill = ClientSkillState.getAvailableSkills().get(hoveredIndex);
            ClientSkillState.selectSkill(hoveredIndex);
            if (ClientSkillState.isActivateMode()
                    && ClientSkillState.getCooldownProgress(skill) <= 0) {
                PacketDistributor.sendToServer(
                        new SkillWheelPayload(skill.itemId(), skill.skillIndex()));
            }
        }
        Minecraft.getInstance().mouseHandler.grabMouse();
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || !active) return;

        Player player = mc.player;
        if (player == null || mc.screen != null || mc.mouseHandler.isMouseGrabbed()) {
            close();
            return;
        }

        int count = ClientSkillState.getSkillCount();
        if (count <= 0) { close(); return; }
        var skills = ClientSkillState.getAvailableSkills();

        int cX = g.guiWidth() / 2;
        int cY = g.guiHeight() / 2;

        // --- 鼠标角度 → 高亮技能 ---
        double mouseX = mc.mouseHandler.xpos();
        double mouseY = mc.mouseHandler.ypos();
        double guiScale = mc.getWindow().getGuiScale();
        double dx = mouseX / guiScale - cX;
        double dy = mouseY / guiScale - cY;

        double raw = Math.atan2(dy, dx);
        if (raw < 0) raw += 2 * Math.PI;
        double top = (raw + Math.PI / 2) % (2 * Math.PI);
        hoveredIndex = (int) Math.round(top / (2 * Math.PI / count)) % count;

        // --- 半透明遮罩 ---
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), 0x60000000);

        // --- 分割线 (line.png 纹理，从指针位置向外延伸) ---
        int lineH = (int)RING_OUTER;
        for (int i = 0; i < count; i++) {
            double alpha = -Math.PI / 2 + i * 2 * Math.PI / count;
            double theta = Math.atan2(-Math.cos(alpha), Math.sin(alpha));

            g.pose().pushPose();
            g.pose().translate(cX, cY, 0);
            g.pose().mulPose(Axis.ZP.rotation((float) theta));
            g.blit(LINE_BG, -8, 0, 0, 0, 16, lineH, 16, 64);
            g.pose().popPose();
        }

        // --- 中央背景（在分割线上层） ---
        g.blit(WHEEL_BG, cX - BG_SIZE / 2, cY - BG_SIZE / 2, 0, 0, BG_SIZE, BG_SIZE, BG_SIZE, BG_SIZE);

        // --- 技能图标 ---
        for (int i = 0; i < count; i++) {
            double a = -Math.PI / 2 + i * 2 * Math.PI / count;
            int ix = (int) (cX + ICON_RADIUS * Math.cos(a)) - ICON_SIZE / 2;
            int iy = (int) (cY + ICON_RADIUS * Math.sin(a)) - ICON_SIZE / 2;

            var slot = skills.get(i);
            g.blit(SKILL_BG, ix, iy, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

            // 选中高光在图标下层
            if (i == hoveredIndex) {
                g.blit(SKILL_SEL, ix, iy, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            }

            if (slot.data().icon() != null) {
                g.blit(slot.data().icon(), ix + 2, iy + 2, 16, 16, 0, 0, 32, 32, 32, 32);
            } else {
                g.renderItem(slot.source(), ix + 2, iy + 2);
            }
        }

        // --- 技能信息 ---
        if (hoveredIndex >= 0 && hoveredIndex < count) {
            renderSkillInfo(g, skills.get(hoveredIndex), cX, cY);
        }

        String modeKey = ClientSkillState.isActivateMode() ? "overlay.mode.1" : "overlay.mode.2";
        g.drawCenteredString(Minecraft.getInstance().font, Component.translatable(modeKey), cX, cY + (int) RING_OUTER + 10, 0xCCCCCC);
    }

    private void renderSkillInfo(GuiGraphics g, ClientSkillState.SkillSlot slot, int cx, int cy) {
        SkillData data = slot.data();
        Player player = Minecraft.getInstance().player;
        int y = cy - (int) RING_OUTER - 30;

        g.drawCenteredString(Minecraft.getInstance().font, Component.translatable(data.name()), cx, y - 26, 0xFFFFFF);

        // 灵气和冷却合并为一行
        long base = data.cooldown();
        long reduced = player != null ? ClientSkillState.getReducedCooldown(player, base) : base;
        String combined = Component.translatable("overlay.lingqi").getString() + String.format("%.1f", data.lingqiCost())
                + " | " + Component.translatable("overlay.cd").getString() + String.format("%.1fs", reduced / 1000f);
        g.drawCenteredString(Minecraft.getInstance().font, combined, cx, y - 16, 0xFFFFFF);

        // 3行自定义描述
        String[] descriptions = null;
        if (slot.source().getItem() instanceof icu.icuqalt10.panlingre.item.skill_trigger trigger) {
            descriptions = trigger.getSkillDescription(slot.skillIndex());
        }
        for (int i = 0; i < 3; i++) {
            String key = (descriptions != null && i < descriptions.length && descriptions[i] != null)
                ? descriptions[i] : "";
            g.drawCenteredString(Minecraft.getInstance().font, Component.translatable(key), cx, y - 4 + i * 10, 0xAAAAAA);
        }
    }
}
