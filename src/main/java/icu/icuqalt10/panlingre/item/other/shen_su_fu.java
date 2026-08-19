package icu.icuqalt10.panlingre.item.other;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.init.ModAttributes;
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

public class shen_su_fu extends Item implements ICurioItem {

    private static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "shen_su_fu");

    public shen_su_fu() {
        super(
                new Properties()
                        .stacksTo(1)
                        .fireResistant()
        );
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = HashMultimap.create();

        modifiers.put(ModAttributes.COOLDOWN_REMOVE, new AttributeModifier(
                MODIFIER_ID,
                0.15,
                AttributeModifier.Operation.ADD_VALUE
        ));

        modifiers.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(
                MODIFIER_ID,
                0.2,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        ));

        return modifiers;
    }

    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltip, TooltipFlag flag) {

        tooltip.add(Component.translatable("item.PanlingRE.lore.rare4"));
        tooltip.add(Component.translatable("item.PanlingRE.lore.limit3"));
        tooltip.add(Component.translatable("item.PanlingRE.shen_su_fu.lore1"));
        tooltip.add(Component.translatable("item.PanlingRE.shen_su_fu.lore2"));

        super.appendHoverText(stack, context, tooltip, flag);
    }
}
