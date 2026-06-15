package icu.icuqalt10.panlingre.compat.jei;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.init.ModBlocks;
import icu.icuqalt10.panlingre.recipe.zftRecipe;
import icu.icuqalt10.panlingre.util.StackUtils;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class zftCategory implements IRecipeCategory<zftRecipe> {
    private final IDrawable background;
    private final IDrawable icon;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/gui/zft.png");

    public zftCategory(IGuiHelper guiHelper) {
        background = guiHelper.createDrawable(TEXTURE,0, 0, 176, 79);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.zft.get()));
    }

    @Override
    public RecipeType<zftRecipe> getRecipeType() {
        return PanlingREJeiPlugin.ZFT_TYPE;
    }

    @Override
    public Component getTitle() {
        return ModBlocks.zft.get().getName();
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, zftRecipe recipe, IFocusGroup focuses) {

        builder.addSlot(RecipeIngredientRole.INPUT, 8, 7)
                .addItemStacks(StackUtils.expandWithCount(recipe.getIngredientAt(0),recipe.getCountAt(0)));
        builder.addSlot(RecipeIngredientRole.INPUT, 59, 34)
                .addItemStacks(StackUtils.expandWithCount(recipe.getIngredientAt(1),recipe.getCountAt(1)));
        builder.addSlot(RecipeIngredientRole.INPUT, 59, 5)
                .addItemStacks(StackUtils.expandWithCount(recipe.getIngredientAt(2),recipe.getCountAt(2)));
        builder.addSlot(RecipeIngredientRole.INPUT, 89, 28)
                .addItemStacks(StackUtils.expandWithCount(recipe.getIngredientAt(3),recipe.getCountAt(3)));
        builder.addSlot(RecipeIngredientRole.INPUT, 80, 58)
                .addItemStacks(StackUtils.expandWithCount(recipe.getIngredientAt(4),recipe.getCountAt(4)));
        builder.addSlot(RecipeIngredientRole.INPUT, 38, 58)
                .addItemStacks(StackUtils.expandWithCount(recipe.getIngredientAt(5),recipe.getCountAt(5)));
        builder.addSlot(RecipeIngredientRole.INPUT, 29, 28)
                .addItemStacks(StackUtils.expandWithCount(recipe.getIngredientAt(6),recipe.getCountAt(6)));

        builder.addSlot(RecipeIngredientRole.OUTPUT, 144, 35)
                .addItemStack(recipe.getResultItem(RegistryAccess.EMPTY));
    }

    @Override
    public boolean isHandled(zftRecipe recipe) {
        return !recipe.isSpecial();
    }

}
