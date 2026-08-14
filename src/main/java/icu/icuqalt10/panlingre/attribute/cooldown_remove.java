package icu.icuqalt10.panlingre.attribute;

import icu.icuqalt10.panlingre.init.ModAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class cooldown_remove {
        public static void cd_remove(Player player, Item item, double cd) {
            player.getCooldowns().addCooldown(item, (int) apply(player, cd));
        }
        public static long skill_cd_remove(Player player,long time) {
            return (long) apply(player, time);
        }

        /**
         * Applies the player's cooldown multiplier without converting the unit.
         * Callers may therefore use ticks, milliseconds, or seconds consistently.
         */
        public static double apply(Player player, double baseCooldown) {
            double multiplier = 1 - player.getAttributeValue(ModAttributes.COOLDOWN_REMOVE);
            return baseCooldown * multiplier;
        }

        public static Component getCooldownText(@Nullable Player player, int cooldownTicks) {
            double reducedTicks = player == null ? cooldownTicks : apply(player, cooldownTicks);
            String seconds = BigDecimal.valueOf(reducedTicks / 20.0)
                    .setScale(2, RoundingMode.HALF_UP)
                    .stripTrailingZeros()
                    .toPlainString();
            return Component.literal(seconds).withStyle(ChatFormatting.GOLD);
        }
}
