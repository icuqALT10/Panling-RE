package icu.icuqalt10.panlingre.compat.jei;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.init.ModBlocks;
import icu.icuqalt10.panlingre.recipe.LdlRecipe;
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

public class ldlCategory implements IRecipeCategory<LdlRecipe> {
    private final IDrawable background;
    private final IDrawable icon;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/gui/ldl.png");

    public ldlCategory(IGuiHelper guiHelper) {
        background = guiHelper.createDrawable(TEXTURE,0, 0, 176, 42);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.ldl.get()));
    }

    @Override
    public RecipeType<LdlRecipe> getRecipeType() {
        return PanlingREJeiPlugin.LDL_TYPE;
    }

    @Override
    public Component getTitle() {
        return ModBlocks.ldl.get().getName();
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
    public void setRecipe(IRecipeLayoutBuilder builder, LdlRecipe recipe, IFocusGroup focuses) {

        builder.addSlot(RecipeIngredientRole.INPUT, 44, 19)
                .addItemStacks(StackUtils.expandWithCount(recipe.getIngredientAt(0),recipe.getCountAt(0)));
        builder.addSlot(RecipeIngredientRole.INPUT, 62, 19)
                .addItemStacks(StackUtils.expandWithCount(recipe.getIngredientAt(1),recipe.getCountAt(1)));
        builder.addSlot(RecipeIngredientRole.INPUT, 80, 19)
                .addItemStacks(StackUtils.expandWithCount(recipe.getIngredientAt(2),recipe.getCountAt(2)));
        builder.addSlot(RecipeIngredientRole.INPUT, 98, 19)
                .addItemStacks(StackUtils.expandWithCount(recipe.getIngredientAt(3),recipe.getCountAt(3)));
        builder.addSlot(RecipeIngredientRole.INPUT, 116, 19)
                .addItemStacks(StackUtils.expandWithCount(recipe.getIngredientAt(4),recipe.getCountAt(4)));

        builder.addSlot(RecipeIngredientRole.OUTPUT, 80, 0)
                .addItemStack(recipe.getResultItem(RegistryAccess.EMPTY));
    }

    @Override
    public boolean isHandled(LdlRecipe recipe) {
        return !recipe.isSpecial();
    }

}
