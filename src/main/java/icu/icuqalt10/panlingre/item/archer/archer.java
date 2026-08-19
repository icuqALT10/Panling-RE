package icu.icuqalt10.panlingre.item.archer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class archer extends Item implements ICurioItem {
    public archer() {
        super(
                new Properties()
                        .stacksTo(1)
                        .fireResistant()
        );
    }

    //绑定诅咒效果
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        return slotContext.entity() instanceof Player player && player.isCreative();
    }

    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltip, TooltipFlag flag) {

            tooltip.add(Component.translatable("item.PanlingRE.archer.lore1"));

        super.appendHoverText(stack, context, tooltip, flag);
    }
}
