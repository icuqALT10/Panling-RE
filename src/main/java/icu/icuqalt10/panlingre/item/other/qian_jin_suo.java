package icu.icuqalt10.panlingre.item.other;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class qian_jin_suo extends Item implements ICurioItem {
    public qian_jin_suo() {
        super(
                new Properties()
                        .stacksTo(1)
                        .fireResistant()
        );
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = HashMultimap.create();

        modifiers.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "qian_jin_suo"),
                0.5,
                AttributeModifier.Operation.ADD_VALUE
        ));

        return modifiers;
    }

    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

        tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare2"));
        tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit3"));
        tooltipComponents.add(Component.translatable("item.PanlingRE.qian_jin_suo.lore1"));
        tooltipComponents.add(Component.translatable("item.PanlingRE.qian_jin_suo.lore2"));
        tooltipComponents.add(Component.translatable("item.PanlingRE.qian_jin_suo.lore3"));

        super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}
