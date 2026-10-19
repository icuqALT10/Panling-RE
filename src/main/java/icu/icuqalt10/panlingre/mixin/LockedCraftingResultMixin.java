package icu.icuqalt10.panlingre.mixin;

import icu.icuqalt10.panlingre.attachment.ItemUnlockData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

/** Covers both the player's 2x2 grid and the crafting table. */
@Mixin(ResultContainer.class)
public abstract class LockedCraftingResultMixin implements RecipeCraftingHolder {
    @Override
    public boolean setRecipeUsed(Level level, ServerPlayer player, RecipeHolder<?> recipe) {
        return ItemUnlockData.canUse(player, recipe.value().getResultItem(level.registryAccess()))
                && RecipeCraftingHolder.super.setRecipeUsed(level, player, recipe);
    }
}
