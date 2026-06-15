package icu.icuqalt10.panlingre.item.potions;

import icu.icuqalt10.panlingre.item.CustomPelletItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class hui_chun_2 extends CustomPelletItem {

    public hui_chun_2() {
        super(new Item.Properties()
                .stacksTo(64)
                .fireResistant()
        );
    }

    @Override
    protected int getParticleColor() {
        return 0xF82423;
    }

    @Override
    protected String getEffectId() {
        return "hui_chun_2";
    }

    @Override
    protected int getCooldownTicks() {
        return 100;
    }

    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

        tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit2"));
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("item.panlingre.hui_chun_2.skill1"));
        tooltipComponents.add(Component.translatable("item.panlingre.hui_chun_2.skill2"));
        tooltipComponents.add(Component.translatable("item.panlingre.hui_chun_2.skill3"));
    }
}
