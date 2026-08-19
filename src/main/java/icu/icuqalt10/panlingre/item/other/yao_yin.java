package icu.icuqalt10.panlingre.item.other;

import icu.icuqalt10.panlingre.player.check;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class yao_yin extends Item {

    public yao_yin() {
        super(
                new Item.Properties()
                        .stacksTo(64)
                        .fireResistant()
        );
    }

    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Player player = SafeClientAccess.getClientPlayer();
        if (player != null && check.race_check(player, "panlingre:race_yao")) {
            tooltip.add(Component.translatable("item.panlingre.yao_yin.lore2"));
        } else {
            tooltip.add(Component.translatable("item.panlingre.yao_yin.lore1.1"));
            tooltip.add(Component.translatable("item.panlingre.yao_yin.lore1.2"));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }

}
