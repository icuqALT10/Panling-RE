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

public class tian_shen_2 extends CustomPelletItem {

    private final int cooldown = 100;

    public tian_shen_2() {
        super(new Item.Properties()
                .stacksTo(64)
                .fireResistant()
        );
    }

    @Override
    protected int getParticleColor() {
        return 0x932423;
    }

    @Override
    protected String getEffectId() {
        return "tian_shen_2";
    }

    @Override
    protected int getCooldownTicks() {
        return cooldown;
    }

    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltip, TooltipFlag flag) {

        tooltip.add(Component.translatable("item.PanlingRE.lore.limit2"));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("item.panlingre.tian_shen_2.skill1", cooldown_remove.getCooldownText(SafeClientAccess.getClientPlayer(), cooldown)));
        tooltip.add(Component.translatable("item.panlingre.tian_shen_2.skill2"));
        tooltip.add(Component.translatable("item.panlingre.tian_shen_2.skill3"));
        tooltip.add(Component.translatable("item.panlingre.tian_shen_2.skill4"));
    }
}
