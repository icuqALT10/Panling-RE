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
    private static final int VISIBLE_ROWS = 3;
    private static final int ICON_SIZE = 16;

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

    private void rebuildButtons() {
        this.clearWidgets();
        int titleY = (int) (height * 0.3);
        int buttonStartY = titleY + 30;
        int totalRowWidth = 2 * BUTTON_WIDTH + BUTTON_GAP;
        int startX = (width - totalRowWidth) / 2;

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

        int titleY = (int) (height * 0.3);
        graphics.drawCenteredString(font, title, width / 2, titleY, 0xFFFFFF);

        int buttonStartY = titleY + 30;
        int totalRowWidth = 2 * BUTTON_WIDTH + BUTTON_GAP;
        int startX = (width - totalRowWidth) / 2;

        for (int i = scrollOffset * 2; i < subs.size(); i++) {
            int rowIndex = (i - scrollOffset * 2) / 2;
            if (rowIndex >= VISIBLE_ROWS) break;
            int col = i % 2;
            int y = buttonStartY + rowIndex * (BUTTON_HEIGHT + BUTTON_GAP);
            int x = startX + col * (BUTTON_WIDTH + BUTTON_GAP);
            renderTexture(graphics, subs.get(i).texture(), x + 4, y + (BUTTON_HEIGHT - ICON_SIZE) / 2);
        }

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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int totalItems = subs.size();
        int maxFirstRow = Math.max(0, (totalItems + 1) / 2 - VISIBLE_ROWS);
        if (scrollY < 0) scrollOffset = Math.min(scrollOffset + 1, maxFirstRow);
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
