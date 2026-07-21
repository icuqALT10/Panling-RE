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

    private void rebuildButtons() {
        this.clearWidgets();
        int titleY = (int) (height * 0.3);
        int buttonStartY = titleY + 30;
        int buttonX = (width - BUTTON_WIDTH) / 2;

        for (int i = scrollOffset; i < cachedMajors.size(); i++) {
            int visualIndex = i - scrollOffset;
            if (visualIndex >= VISIBLE_COUNT) break;

            int y = buttonStartY + visualIndex * (BUTTON_HEIGHT + BUTTON_GAP);
            int idx = i;
            BaFangYiOpenPayload.BaFangYiMajorPayload major = cachedMajors.get(i);
            this.addRenderableWidget(Button.builder(
                    Component.literal("  ").append(major.title()), b -> onMajorClick(idx))
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

        int titleY = (int) (height * 0.3);
        graphics.drawCenteredString(font, title, width / 2, titleY, 0xFFFFFF);

        int buttonStartY = titleY + 30;
        int buttonX = (width - BUTTON_WIDTH) / 2;

        for (int i = scrollOffset; i < cachedMajors.size(); i++) {
            int visualIndex = i - scrollOffset;
            if (visualIndex >= VISIBLE_COUNT) break;
            int y = buttonStartY + visualIndex * (BUTTON_HEIGHT + BUTTON_GAP);
            renderTexture(graphics, cachedMajors.get(i).texture(), buttonX + 4, y + (BUTTON_HEIGHT - ICON_SIZE) / 2);
        }

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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, cachedMajors.size() - VISIBLE_COUNT);
        if (scrollY < 0) scrollOffset = Math.min(scrollOffset + 1, maxScroll);
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
