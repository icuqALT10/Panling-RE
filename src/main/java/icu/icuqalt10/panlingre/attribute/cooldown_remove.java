package icu.icuqalt10.panlingre.attribute;

import icu.icuqalt10.panlingre.init.ModAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public class cooldown_remove {
        public static void cd_remove(Player player, Item item, double cd) {
            double cd_remove = 2 - player.getAttributeValue(ModAttributes.COOLDOWN_REMOVE);
            player.getCooldowns().addCooldown(item, (int) (cd * cd_remove));
        }
        public static long skill_cd_remove(Player player,long time) {
            double cd_remove = 2 - player.getAttributeValue(ModAttributes.COOLDOWN_REMOVE);
            return (long)(time * cd_remove);
        }
}
