package icu.icuqalt10.panlingre.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StackUtils {

    public static List<ItemStack> expandWithCount(Ingredient ingredient, int count) {
        return Arrays.stream(ingredient.getItems())
                .map(stack -> {
                    ItemStack copy = stack.copy();
                    copy.setCount(count);
                    return copy;
                })
                .collect(Collectors.toList());

    }
}