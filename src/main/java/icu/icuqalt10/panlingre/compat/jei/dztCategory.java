package icu.icuqalt10.panlingre.compat.jei;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.init.ModBlocks;
import icu.icuqalt10.panlingre.recipe.dztRecipe;
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

public class dztCategory implements IRecipeCategory<dztRecipe> {
    private final IDrawable background;
    private final IDrawable icon;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/gui/dzt.png");

    public dztCategory(IGuiHelper guiHelper) {
        background = guiHelper.createDrawable(TEXTURE,0, 0, 176, 79);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.dzt.get()));
    }

    @Override
    public RecipeType<dztRecipe> getRecipeType() {
        return PanlingREJeiPlugin.DZT_TYPE;
    }

    @Override
    public Component getTitle() {
        return ModBlocks.dzt.get().getName();
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
    public void setRecipe(IRecipeLayoutBuilder builder, dztRecipe recipe, IFocusGroup focuses) {

        builder.addSlot(RecipeIngredientRole.INPUT, 62, 17)
                .addItemStacks(StackUtils.expandWithCount(recipe.getIngredientAt(0),recipe.getCountAt(0)));
        builder.addSlot(RecipeIngredientRole.INPUT, 62, 35)
                .addItemStacks(StackUtils.expandWithCount(recipe.getIngredientAt(1),recipe.getCountAt(1)));
        builder.addSlot(RecipeIngredientRole.INPUT, 62, 53)
                .addItemStacks(StackUtils.expandWithCount(recipe.getIngredientAt(2),recipe.getCountAt(2)));

        builder.addSlot(RecipeIngredientRole.OUTPUT, 98, 35)
                .addItemStack(recipe.getResultItem(RegistryAccess.EMPTY));
    }

    @Override
    public boolean isHandled(dztRecipe recipe) {
        return !recipe.isSpecial();
    }

}
