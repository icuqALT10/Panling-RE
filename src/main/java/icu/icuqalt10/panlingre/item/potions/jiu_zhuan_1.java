package icu.icuqalt10.panlingre.item.potions;

import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import icu.icuqalt10.panlingre.item.CustomPelletItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class jiu_zhuan_1 extends CustomPelletItem {

    private final int cooldown = 100;

    public jiu_zhuan_1() {
        super(new Item.Properties()
                .stacksTo(64)
                .fireResistant()
        );
    }

    @Override
    protected int getParticleColor() {
        return 0xCD5CAB;
    }

    @Override
    protected String getEffectId() {
        return "jiu_zhuan_1";
    }

    @Override
    protected int getCooldownTicks() {
        return cooldown;
    }

    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

        tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit2"));
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("item.panlingre.jiu_zhuan_1.skill1", cooldown_remove.getCooldownText(SafeClientAccess.getClientPlayer(), cooldown)));
        tooltipComponents.add(Component.translatable("item.panlingre.jiu_zhuan_1.skill2"));
        tooltipComponents.add(Component.translatable("item.panlingre.jiu_zhuan_1.skill3"));
        tooltipComponents.add(Component.translatable("item.panlingre.jiu_zhuan_1.skill4"));
        tooltipComponents.add(Component.translatable("item.panlingre.jiu_zhuan_1.skill5"));
        tooltipComponents.add(Component.translatable("item.panlingre.jiu_zhuan_1.skill6"));
    }
}
