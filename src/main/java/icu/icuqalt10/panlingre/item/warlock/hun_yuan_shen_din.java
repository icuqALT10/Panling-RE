package icu.icuqalt10.panlingre.item.warlock;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.init.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class hun_yuan_shen_din extends Item implements ICurioItem {

    public hun_yuan_shen_din() {
        super(
                new Properties()
                        .stacksTo(1)
                        .fireResistant()
        );
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = HashMultimap.create();

        LivingEntity entity = slotContext.entity();

        modifiers.put(ModAttributes.MAGIC_DAMAGE, new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "hun_yuan_shen_din"),
                20,
                AttributeModifier.Operation.ADD_VALUE
        ));

        return modifiers;
    }
}
