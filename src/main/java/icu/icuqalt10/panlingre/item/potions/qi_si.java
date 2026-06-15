package icu.icuqalt10.panlingre.item.potions;

import icu.icuqalt10.panlingre.item.CustomPelletItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class qi_si extends CustomPelletItem {

    public qi_si() {
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
        return "qi_si";
    }

    @Override
    protected int getCooldownTicks() {
        return 200;
    }

    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

        tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit2"));
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("item.panlingre.qi_si.skill1"));
        tooltipComponents.add(Component.translatable("item.panlingre.qi_si.skill2"));
        tooltipComponents.add(Component.translatable("item.panlingre.qi_si.skill3"));
    }
}
