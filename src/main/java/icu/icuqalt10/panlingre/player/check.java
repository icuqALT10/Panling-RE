package icu.icuqalt10.panlingre.player;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

public class check {

    public static boolean zhiye_check(Player player,String targetId) {
        return CuriosApi.getCuriosInventory(player).map(inventory -> {
            var stacksHandler = inventory.getCurios().get("zhiye");
            if (stacksHandler == null) return false;

            var stacks = stacksHandler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    String currentId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    if (currentId.equals(targetId)) return true;
                }
            }
            return false;
        }).orElse(false);
    }

    public static boolean race_check(Player player,String targetId) {
        return CuriosApi.getCuriosInventory(player).map(inventory -> {
            var stacksHandler = inventory.getCurios().get("race");
            if (stacksHandler == null) return false;

            var stacks = stacksHandler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    String currentId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    if (currentId.equals(targetId)) return true;
                }
            }
            return false;
        }).orElse(false);
    }
}
