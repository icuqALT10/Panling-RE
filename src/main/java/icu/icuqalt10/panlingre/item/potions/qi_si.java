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

public class qi_si extends CustomPelletItem {

    private final int cooldown = 400;

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
        return cooldown;
    }

    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltip, TooltipFlag flag) {

        tooltip.add(Component.translatable("item.PanlingRE.lore.limit2"));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("item.panlingre.qi_si.skill1", cooldown_remove.getCooldownText(SafeClientAccess.getClientPlayer(), cooldown)));
        tooltip.add(Component.translatable("item.panlingre.qi_si.skill2"));
    }
}
