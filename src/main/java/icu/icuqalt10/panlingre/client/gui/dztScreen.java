package icu.icuqalt10.panlingre.client.gui;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.world.inventory.dztMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

public class dztScreen extends AbstractContainerScreen<dztMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/gui/dzt.png");
    private static final ResourceLocation ARROW_UP =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/gui/dzt_arrow_up.png");
    private static final ResourceLocation ARROW_UP_HIGHLIGHTED =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/gui/dzt_arrow_up_highlighted.png");
    private static final ResourceLocation ARROW_DOWN =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/gui/dzt_arrow_down.png");
    private static final ResourceLocation ARROW_DOWN_HIGHLIGHTED =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/gui/dzt_arrow_down_highlighted.png");

    private static final int ARROW_X = 117;
    private static final int UP_ARROW_Y = 34;
    private static final int DOWN_ARROW_Y = 45;
    private static final int ARROW_WIDTH = 10;
    private static final int ARROW_HEIGHT = 8;

    public dztScreen(dztMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.titleLabelX = 76;
        this.titleLabelY = 6;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - 176) / 2;
        int y = (this.height - 166) / 2;

        graphics.blit(TEXTURE, x, y, 0, 0, 176, 166, 256, 256);

        boolean active = this.menu.hasMultipleRecipes();
        boolean upHovered = active && isOverArrow(mouseX, mouseY, UP_ARROW_Y);
        boolean downHovered = active && isOverArrow(mouseX, mouseY, DOWN_ARROW_Y);

        ResourceLocation upTexture = upHovered ? ARROW_UP_HIGHLIGHTED : ARROW_UP;
        ResourceLocation downTexture = downHovered ? ARROW_DOWN_HIGHLIGHTED : ARROW_DOWN;
        graphics.blit(upTexture, x + ARROW_X, y + UP_ARROW_Y, 3, 4, ARROW_WIDTH, ARROW_HEIGHT, 32, 32);
        graphics.blit(downTexture, x + ARROW_X, y + DOWN_ARROW_Y, 3, 20, ARROW_WIDTH, ARROW_HEIGHT, 32, 32);
    }

    private boolean isOverArrow(double mouseX, double mouseY, int arrowY) {
        return mouseX >= this.leftPos + ARROW_X
                && mouseX < this.leftPos + ARROW_X + ARROW_WIDTH
                && mouseY >= this.topPos + arrowY
                && mouseY < this.topPos + arrowY + ARROW_HEIGHT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.menu.hasMultipleRecipes()) {
            int menuButton = -1;
            if (isOverArrow(mouseX, mouseY, UP_ARROW_Y)) {
                menuButton = dztMenu.SELECT_PREVIOUS_RECIPE;
            } else if (isOverArrow(mouseX, mouseY, DOWN_ARROW_Y)) {
                menuButton = dztMenu.SELECT_NEXT_RECIPE;
            }

            if (menuButton >= 0 && this.menu.clickMenuButton(this.minecraft.player, menuButton)) {
                this.minecraft.getSoundManager()
                        .play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, menuButton);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
