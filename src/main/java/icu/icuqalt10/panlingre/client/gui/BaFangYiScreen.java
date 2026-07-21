package icu.icuqalt10.panlingre.client.gui;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.network.BaFangYiOpenPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class BaFangYiScreen extends Screen {

    private static final ResourceLocation DEFAULT_ICON = ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/gui/teleport/default_icon.png");
    private static List<BaFangYiOpenPayload.BaFangYiMajorPayload> cachedMajors = new ArrayList<>();

    private int scrollOffset = 0;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;
    private static final int VISIBLE_COUNT = 5;
    private static final int ICON_SIZE = 16;
    private static final int SCROLL_BAR_WIDTH = 4;

    public BaFangYiScreen() {
        super(Component.translatable("plre.gui.ba_fang_yi.title"));
    }

    public static void openWith(List<BaFangYiOpenPayload.BaFangYiMajorPayload> majors) {
        cachedMajors = new ArrayList<>(majors);
        Minecraft.getInstance().setScreen(new BaFangYiScreen());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        rebuildButtons();
    }

    private int getTitleY() { return (int) (height * 0.3); }
    private int getButtonStartY() { return getTitleY() + 30; }
    private int getButtonX() { return (width - BUTTON_WIDTH) / 2; }
    private int getPanelHeight() { return VISIBLE_COUNT * (BUTTON_HEIGHT + BUTTON_GAP); }
    private int getScrollBarX() { return getButtonX() + BUTTON_WIDTH + 6; }
    private int maxScroll() { return Math.max(0, cachedMajors.size() - VISIBLE_COUNT); }

    private void rebuildButtons() {
        this.clearWidgets();
        int buttonX = getButtonX();
        int buttonStartY = getButtonStartY();

        for (int i = scrollOffset; i < cachedMajors.size(); i++) {
            int visualIndex = i - scrollOffset;
            if (visualIndex >= VISIBLE_COUNT) break;

            int y = buttonStartY + visualIndex * (BUTTON_HEIGHT + BUTTON_GAP);
            int idx = i;
            this.addRenderableWidget(Button.builder(
                    Component.literal("  ").append(cachedMajors.get(i).title()), b -> onMajorClick(idx))
                    .pos(buttonX, y)
                    .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }
    }

    private void onMajorClick(int index) {
        if (index >= 0 && index < cachedMajors.size()) {
            Minecraft.getInstance().setScreen(new BaFangYiSubScreen(cachedMajors.get(index)));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int titleY = getTitleY();
        graphics.drawCenteredString(font, title, width / 2, titleY, 0xFFFFFF);

        int buttonStartY = getButtonStartY();
        int buttonX = getButtonX();

        for (int i = scrollOffset; i < cachedMajors.size(); i++) {
            int visualIndex = i - scrollOffset;
            if (visualIndex >= VISIBLE_COUNT) break;
            int y = buttonStartY + visualIndex * (BUTTON_HEIGHT + BUTTON_GAP);
            renderTexture(graphics, cachedMajors.get(i).texture(), buttonX + 4, y + (BUTTON_HEIGHT - ICON_SIZE) / 2);
        }

        renderScrollBar(graphics, getScrollBarX(), buttonStartY, getPanelHeight(),
                cachedMajors.size(), VISIBLE_COUNT, scrollOffset, maxScroll());

        if (cachedMajors.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("gui.panlingre.teleport.empty"), width / 2, height / 2, 0x888888);
        }
        graphics.drawCenteredString(font, Component.translatable("gui.panlingre.teleport.tip"), width / 2, height - 12, 0x666666);
    }

    private void renderTexture(GuiGraphics graphics, String texturePath, int x, int y) {
        if (texturePath != null && !texturePath.isEmpty()) {
            try {
                ResourceLocation loc = ResourceLocation.parse(texturePath);
                graphics.blit(loc, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
                return;
            } catch (Exception ignored) {}
        }
        graphics.blit(DEFAULT_ICON, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

    private void renderScrollBar(GuiGraphics graphics, int x, int top, int height,
                                  int totalItems, int visible, int offset, int maxScroll) {
        if (totalItems <= visible) return;
        int bottom = top + height;
        int thumbH = Math.max(15, height * visible / totalItems);
        int thumbY = top + (height - thumbH) * offset / maxScroll;

        // Track
        graphics.fill(x, top, x + SCROLL_BAR_WIDTH, bottom, 0xFF1E1E1E);
        // Thumb
        graphics.fill(x, thumbY, x + SCROLL_BAR_WIDTH, thumbY + thumbH, 0xFF646464);
        // Thumb top highlight
        graphics.fill(x, thumbY, x + SCROLL_BAR_WIDTH, thumbY + 1, 0xFF808080);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
            int sbX = getScrollBarX();
            int sbTop = getButtonStartY();
            int sbHeight = getPanelHeight();
            int maxS = maxScroll();
            if (maxS > 0 && mouseX >= sbX && mouseX <= sbX + SCROLL_BAR_WIDTH
                    && mouseY >= sbTop && mouseY <= sbTop + sbHeight) {
                int thumbH = Math.max(15, sbHeight * VISIBLE_COUNT / cachedMajors.size());
                int thumbY = sbTop + (sbHeight - thumbH) * scrollOffset / maxS;
                if (mouseY < thumbY) {
                    scrollOffset = Math.max(0, scrollOffset - VISIBLE_COUNT);
                } else if (mouseY > thumbY + thumbH) {
                    scrollOffset = Math.min(maxS, scrollOffset + VISIBLE_COUNT);
                }
                rebuildButtons();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxS = maxScroll();
        if (scrollY < 0) scrollOffset = Math.min(scrollOffset + 1, maxS);
        else if (scrollY > 0) scrollOffset = Math.max(scrollOffset - 1, 0);
        rebuildButtons();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
