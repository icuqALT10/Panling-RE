package icu.icuqalt10.panlingre.compat.jei;

import icu.icuqalt10.panlingre.init.ModRecipes;
import icu.icuqalt10.panlingre.recipe.LdlRecipe;
import icu.icuqalt10.panlingre.recipe.dztRecipe;
import icu.icuqalt10.panlingre.recipe.zftRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class PanlingRERecipes {
    private final RecipeManager recipeManager;

    public PanlingRERecipes(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }

    public List<LdlRecipe> getLdlRecipes() {
        return recipeManager.getAllRecipesFor(ModRecipes.LDL_TYPE.get()).stream().map(RecipeHolder::value).toList();
    }

    public List<zftRecipe> getzftRecipes() {
        return recipeManager.getAllRecipesFor(ModRecipes.ZFT_TYPE.get()).stream().map(RecipeHolder::value).toList();
    }

    public List<dztRecipe> getdztRecipes() {
        return recipeManager.getAllRecipesFor(ModRecipes.DZT_TYPE.get()).stream().map(RecipeHolder::value).toList();
    }

}
