package icu.icuqalt10.panlingre.client.gui;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.network.BaFangYiOpenPayload;
import icu.icuqalt10.panlingre.network.BaFangYiTeleportPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class BaFangYiSubScreen extends Screen {

    private static final ResourceLocation DEFAULT_ICON = ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/gui/teleport/default_icon.png");

    private final BaFangYiOpenPayload.BaFangYiMajorPayload major;
    private final List<BaFangYiOpenPayload.BaFangYiSubPayload> subs;
    private int scrollOffset = 0;

    private static final int BUTTON_WIDTH = 98;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;
    private static final int VISIBLE_ROWS = 5;
    private static final int ICON_SIZE = 16;
    private static final int SCROLL_BAR_WIDTH = 4;

    public BaFangYiSubScreen(BaFangYiOpenPayload.BaFangYiMajorPayload major) {
        super(major.title());
        this.major = major;
        this.subs = major.poses();
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
    private int totalRowWidth() { return 2 * BUTTON_WIDTH + BUTTON_GAP; }
    private int getStartX() { return (width - totalRowWidth()) / 2; }
    private int getPanelHeight() { return VISIBLE_ROWS * (BUTTON_HEIGHT + BUTTON_GAP); }
    private int getScrollBarX() { return getStartX() + totalRowWidth() + 6; }
    private int visibleItemCount() { return VISIBLE_ROWS * 2; }
    private int maxScroll() { return Math.max(0, (subs.size() + 1) / 2 - VISIBLE_ROWS); }

    private void rebuildButtons() {
        this.clearWidgets();
        int buttonStartY = getButtonStartY();
        int startX = getStartX();

        for (int i = scrollOffset * 2; i < subs.size(); i++) {
            int rowIndex = (i - scrollOffset * 2) / 2;
            if (rowIndex >= VISIBLE_ROWS) break;

            int col = i % 2;
            int y = buttonStartY + rowIndex * (BUTTON_HEIGHT + BUTTON_GAP);
            int x = startX + col * (BUTTON_WIDTH + BUTTON_GAP);
            int idx = i;
            this.addRenderableWidget(Button.builder(
                    Component.literal("  ").append(subs.get(i).title()), b -> onSubClick(idx))
                    .pos(x, y)
                    .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }
    }

    private void onSubClick(int index) {
        if (index >= 0 && index < subs.size()) {
            PacketDistributor.sendToServer(new BaFangYiTeleportPayload(major.id(), subs.get(index).id()));
            this.onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int titleY = getTitleY();
        graphics.drawCenteredString(font, title, width / 2, titleY, 0xFFFFFF);

        int buttonStartY = getButtonStartY();
        int startX = getStartX();

        for (int i = scrollOffset * 2; i < subs.size(); i++) {
            int rowIndex = (i - scrollOffset * 2) / 2;
            if (rowIndex >= VISIBLE_ROWS) break;
            int col = i % 2;
            int y = buttonStartY + rowIndex * (BUTTON_HEIGHT + BUTTON_GAP);
            int x = startX + col * (BUTTON_WIDTH + BUTTON_GAP);
            renderTexture(graphics, subs.get(i).texture(), x + 4, y + (BUTTON_HEIGHT - ICON_SIZE) / 2);
        }

        renderScrollBar(graphics, getScrollBarX(), buttonStartY, getPanelHeight(),
                subs.size(), visibleItemCount(), scrollOffset, maxScroll());

        graphics.drawCenteredString(font, Component.translatable("gui.panlingre.ba_fang_yi.back"), width / 2, height - 12, 0x666666);
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
                int visible = visibleItemCount();
                int thumbH = Math.max(15, sbHeight * visible / subs.size());
                int thumbY = sbTop + (sbHeight - thumbH) * scrollOffset / maxS;
                if (mouseY < thumbY) {
                    scrollOffset = Math.max(0, scrollOffset - VISIBLE_ROWS);
                } else if (mouseY > thumbY + thumbH) {
                    scrollOffset = Math.min(maxS, scrollOffset + VISIBLE_ROWS);
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
            Minecraft.getInstance().setScreen(new BaFangYiScreen());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
