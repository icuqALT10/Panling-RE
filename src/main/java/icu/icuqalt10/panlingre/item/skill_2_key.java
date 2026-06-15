package icu.icuqalt10.panlingre.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface skill_2_key {
    boolean skill_2_trigger(Level level, Player player, ItemStack stack);

    default long getCD_2(String skillId) {
        return switch (skillId) {
            case "cd.panlingre:chi_tong_lu.skill_2" -> 1000L;
            default -> 1000L;
        };
    }
}
