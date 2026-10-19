package icu.icuqalt10.panlingre.compat.jei;

import icu.icuqalt10.panlingre.attachment.ItemUnlockData;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.item.RecipeLockedItem;
import icu.icuqalt10.panlingre.recipe.LdlRecipe;
import icu.icuqalt10.panlingre.recipe.dztRecipe;
import icu.icuqalt10.panlingre.recipe.zftRecipe;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** One JEI runtime's visibility state; discarded when JEI reloads or disconnects. */
public final class LockedItemJeiVisibility {
    private final IJeiRuntime runtime;
    private final List<ItemStack> lockedItems;
    private final Set<ItemStack> hiddenItems = new HashSet<>();
    private List<LdlRecipe> hiddenLdl = List.of();
    private List<zftRecipe> hiddenZft = List.of();
    private List<dztRecipe> hiddenDzt = List.of();
    private List<RecipeHolder<CraftingRecipe>> hiddenCrafting = List.of();
    private ItemUnlockData lastUnlocks;
    private Player lastPlayer;

    public LockedItemJeiVisibility(IJeiRuntime runtime) {
        this.runtime = runtime;
        this.lockedItems = runtime.getIngredientManager().getAllItemStacks().stream()
                .filter(stack -> stack.getItem() instanceof RecipeLockedItem).toList();
    }

    public void refresh(@Nullable Player player) {
        ItemUnlockData unlocks = player == null ? ItemUnlockData.EMPTY : player.getData(ModAttachments.ITEM_UNLOCKS);
        if (lastUnlocks == unlocks && lastPlayer == player) return;
        lastUnlocks = unlocks;
        lastPlayer = player;

        var ingredients = runtime.getIngredientManager();
        List<ItemStack> restore = lockedItems.stream()
                .filter(hiddenItems::contains).filter(stack -> ItemUnlockData.canUse(player, stack)).toList();
        List<ItemStack> hide = lockedItems.stream()
                .filter(stack -> !hiddenItems.contains(stack) && !ItemUnlockData.canUse(player, stack)).toList();
        if (!restore.isEmpty()) ingredients.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, restore);
        if (!hide.isEmpty()) ingredients.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hide);
        hiddenItems.removeAll(restore);
        hiddenItems.addAll(hide);

        // A missing player is only possible during loading/disconnect; hide every restricted output.
        var registries = player == null ? net.minecraft.core.RegistryAccess.EMPTY : player.registryAccess();
        hiddenLdl = refreshRecipes(PanlingREJeiPlugin.LDL_TYPE, hiddenLdl,
                recipe -> recipe.getResultItem(registries), player);
        hiddenZft = refreshRecipes(PanlingREJeiPlugin.ZFT_TYPE, hiddenZft,
                recipe -> recipe.getResultItem(registries), player);
        hiddenDzt = refreshRecipes(PanlingREJeiPlugin.DZT_TYPE, hiddenDzt,
                recipe -> recipe.getResultItem(registries), player);
        hiddenCrafting = refreshRecipes(RecipeTypes.CRAFTING, hiddenCrafting,
                recipe -> recipe.value().getResultItem(registries), player);
    }

    private <T> List<T> refreshRecipes(RecipeType<T> type, List<T> previouslyHidden,
                                       Function<T, ItemStack> output, @Nullable Player player) {
        var recipes = runtime.getRecipeManager();
        List<T> hidden = recipes.createRecipeLookup(type).includeHidden().get()
                .filter(recipe -> !ItemUnlockData.canUse(player, output.apply(recipe))).toList();
        List<T> restore = previouslyHidden.stream().filter(recipe -> !hidden.contains(recipe)).toList();
        if (!restore.isEmpty()) recipes.unhideRecipes(type, restore);
        if (!hidden.isEmpty()) recipes.hideRecipes(type, hidden);
        return hidden;
    }
}
