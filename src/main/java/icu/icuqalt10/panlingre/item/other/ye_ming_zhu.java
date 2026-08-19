package icu.icuqalt10.panlingre.item.other;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.init.ModAttributes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class ye_ming_zhu extends Item implements ICurioItem {
    public ye_ming_zhu() {
        super(
                new Properties()
                        .stacksTo(1)
                        .fireResistant()
        );
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();

        if (entity.level().isClientSide) return;

        entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0,false,false,true));

    }

    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltip, TooltipFlag flag) {

        tooltip.add(Component.translatable("item.PanlingRE.lore.rare2"));
        tooltip.add(Component.translatable("item.PanlingRE.lore.limit3"));
        tooltip.add(Component.translatable("item.PanlingRE.ye_ming_zhu.lore1"));
        tooltip.add(Component.translatable("item.PanlingRE.ye_ming_zhu.lore2"));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("item.PanlingRE.ye_ming_zhu.skill1"));
        tooltip.add(Component.translatable("item.PanlingRE.ye_ming_zhu.skill2"));

        super.appendHoverText(stack, context, tooltip, flag);
    }
}
