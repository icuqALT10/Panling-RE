package icu.icuqalt10.panlingre.client;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.skill.ClientSkillState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;

public class SkillIconOverlay implements LayeredDraw.Layer {

    private static final ResourceLocation SKILL_BG =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/gui/skill/skill_background.png");
    private static final ResourceLocation SKILL_SEL =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/gui/skill/skill_selection.png");

    private static final int ICON = 22;
    private static final int GAP = 1;
    private static final int STEP = ICON + GAP;
    private static final int COLUMNS = 6;

    @Override
    public void render(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (mc.screen != null) return;

        var skills = ClientSkillState.getAvailableSkills();
        if (skills.isEmpty()) return;

        int screenH = mc.getWindow().getGuiScaledHeight();
        int selIdx = ClientSkillState.getSelectedIndex();

        for (int i = 0; i < skills.size(); i++) {
            int col = i / COLUMNS;
            int row = i % COLUMNS;
            int visible = Math.min(skills.size() - col * COLUMNS, COLUMNS);
            int x = 3 + col * STEP;
            int y = (screenH - visible * STEP) / 2 + row * STEP;

            var slot = skills.get(i);
            g.blit(SKILL_BG, x, y, 0, 0, ICON, ICON, ICON, ICON);

            if (i == selIdx) {
                g.blit(SKILL_SEL, x, y, 0, 0, ICON, ICON, ICON, ICON);
            }

            if (slot.data().icon() != null) {
                g.blit(slot.data().icon(), x + 2, y + 2, 18, 18, 0, 0, 32, 32, 32, 32);
            } else {
                g.renderItem(slot.source(), x + 3, y + 3);
            }

            float cd = ClientSkillState.getCooldownProgress(skills.get(i));
            if (cd > 0) {
                int h = (int) (ICON * cd);
                g.fill(x, y, x + ICON, y + h, 0x80FFFFFF);
            }
        }
    }
}
