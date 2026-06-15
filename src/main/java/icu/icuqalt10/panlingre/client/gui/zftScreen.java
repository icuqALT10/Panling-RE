package icu.icuqalt10.panlingre.client.gui;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.world.inventory.zftMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class zftScreen extends AbstractContainerScreen<zftMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/gui/zft.png");

    public zftScreen(zftMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.titleLabelX = 8;
        this.titleLabelY = 73;
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
    }
}