package icu.icuqalt10.panlingre.compat.jei;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.client.gui.dztScreen;
import icu.icuqalt10.panlingre.client.gui.ldlScreen;
import icu.icuqalt10.panlingre.client.gui.zftScreen;
import icu.icuqalt10.panlingre.init.ModBlocks;
import icu.icuqalt10.panlingre.init.ModMenus;
import icu.icuqalt10.panlingre.recipe.LdlRecipe;
import icu.icuqalt10.panlingre.recipe.dztRecipe;
import icu.icuqalt10.panlingre.recipe.zftRecipe;
import icu.icuqalt10.panlingre.world.inventory.dztMenu;
import icu.icuqalt10.panlingre.world.inventory.ldlMenu;
import icu.icuqalt10.panlingre.world.inventory.zftMenu;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

@JeiPlugin
public class PanlingREJeiPlugin implements IModPlugin {

    public static final RecipeType<LdlRecipe> LDL_TYPE = RecipeType.create(PanlingRE.MODID, "ldl_crafting", LdlRecipe.class);
    public static final RecipeType<zftRecipe> ZFT_TYPE = RecipeType.create(PanlingRE.MODID, "zft_crafting", zftRecipe.class);
    public static final RecipeType<dztRecipe> DZT_TYPE = RecipeType.create(PanlingRE.MODID, "dzt_crafting", dztRecipe.class);

    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new ldlCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new zftCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new dztCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ClientLevel level = Minecraft.getInstance().level;

        if (level != null) {
            RecipeManager recipeManager = level.getRecipeManager();
            PanlingRERecipes modRecipes = new PanlingRERecipes(recipeManager);

            registration.addRecipes(LDL_TYPE, modRecipes.getLdlRecipes());
            registration.addRecipes(ZFT_TYPE, modRecipes.getzftRecipes());
            registration.addRecipes(DZT_TYPE, modRecipes.getdztRecipes());
        }
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(ldlMenu.class, ModMenus.ldl_menu.get(), LDL_TYPE, 0, 5, 9, 36);
        registration.addRecipeTransferHandler(zftMenu.class, ModMenus.zft_menu.get(), ZFT_TYPE, 0, 7, 9, 36);
        registration.addRecipeTransferHandler(dztMenu.class, ModMenus.dzt_menu.get(), DZT_TYPE, 0, 3, 9, 36);

    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {

        registration.addRecipeClickArea(ldlScreen.class, 62, 35, 10, 10,
                LDL_TYPE);

        registration.addRecipeClickArea(zftScreen.class, 114, 35, 20, 20,
                ZFT_TYPE);

        registration.addRecipeClickArea(dztScreen.class, 80, 35, 16, 16,
                DZT_TYPE);

    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ldl.get()), LDL_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.zft.get()), ZFT_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.dzt.get()), DZT_TYPE);

    }
    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime jeiRuntime) {
        var recipeManager = jeiRuntime.getRecipeManager();

        recipeManager.hideRecipeCategory(RecipeTypes.GRINDSTONE);
        recipeManager.hideRecipeCategory(RecipeTypes.ANVIL);
        recipeManager.hideRecipeCategory(RecipeTypes.BLASTING);
        recipeManager.hideRecipeCategory(RecipeTypes.BREWING);
        recipeManager.hideRecipeCategory(RecipeTypes.CAMPFIRE_COOKING);
        recipeManager.hideRecipeCategory(RecipeTypes.COMPOSTING);
        recipeManager.hideRecipeCategory(RecipeTypes.FUELING);
        recipeManager.hideRecipeCategory(RecipeTypes.INFORMATION);
        recipeManager.hideRecipeCategory(RecipeTypes.SMELTING);
        recipeManager.hideRecipeCategory(RecipeTypes.SMITHING);
        recipeManager.hideRecipeCategory(RecipeTypes.SMOKING);
        recipeManager.hideRecipeCategory(RecipeTypes.STONECUTTING);
    }
}
