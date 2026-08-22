package icu.icuqalt10.panlingre.client.gui;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.world.inventory.FuZhiBagMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class FuZhiBagScreen extends AbstractContainerScreen<FuZhiBagMenu> {
    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            PanlingRE.MODID, "textures/gui/fu_zhi_bag.png");
    private static final int TITLE_COLOR = 0xE7D28A;
    private static final int INVENTORY_LABEL_COLOR = 0xC6C2CA;
    private static final float COUNT_SCALE_PER_EXTRA_DIGIT = 0.15F;
    private static final float MIN_COUNT_SCALE = 0.55F;

    public FuZhiBagScreen(FuZhiBagMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 8;
        this.titleLabelY = 8;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND_TEXTURE, this.leftPos, this.topPos,
                0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title,
                this.titleLabelX, this.titleLabelY, TITLE_COLOR, true);
        graphics.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, INVENTORY_LABEL_COLOR, true);
    }

    @Override
    protected void renderSlotContents(GuiGraphics graphics, ItemStack stack, Slot slot,
                                      @Nullable String countString) {
        if (slot.index < FuZhiBagMenu.BAG_SLOT_COUNT && !stack.isEmpty()) {
            super.renderSlotContents(graphics, stack, slot, null);
            long count = menu.getStoredCount(slot.getContainerSlot());
            if (count > 1) renderBagCount(graphics, slot, count);
            return;
        }
        super.renderSlotContents(graphics, stack, slot, countString);
    }

    private void renderBagCount(GuiGraphics graphics, Slot slot, long count) {
        String text = Long.toString(count);
        float scale = Math.max(
                MIN_COUNT_SCALE,
                1.0F - (text.length() - 1) * COUNT_SCALE_PER_EXTRA_DIGIT
        );

        graphics.pose().pushPose();
        graphics.pose().translate(slot.x + 17.0F, slot.y + 18.0F, 200.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(this.font, text, -this.font.width(text),
                -this.font.lineHeight, 0xFFFFFF, true);
        graphics.pose().popPose();
    }

}
