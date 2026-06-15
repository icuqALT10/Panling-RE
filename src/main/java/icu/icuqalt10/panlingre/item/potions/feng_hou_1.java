package icu.icuqalt10.panlingre.item.potions;

import icu.icuqalt10.panlingre.item.CustomPelletItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class feng_hou_1 extends CustomPelletItem {

    public feng_hou_1() {
        super(new Item.Properties()
                .stacksTo(64)
                .fireResistant()
        );
    }

    @Override
    protected int getParticleColor() {
        return 0x430A09;
    }

    @Override
    protected String getEffectId() {
        return "feng_hou_1";
    }

    @Override
    protected int getCooldownTicks() {
        return 30;
    }

    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

        tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit2"));
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("item.panlingre.feng_hou_1.skill1"));
        tooltipComponents.add(Component.translatable("item.panlingre.feng_hou_1.skill2"));
        tooltipComponents.add(Component.translatable("item.panlingre.feng_hou_1.skill3"));
    }
}
