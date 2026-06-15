package icu.icuqalt10.panlingre.item.common.armor;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

public class chu_xin extends ArmorItem {

    public chu_xin(Holder<ArmorMaterial> material, Type type) {
        super(material, type, new Properties()
                .attributes(createModifiers(type))
                .stacksTo(1));
    }

    private static ItemAttributeModifiers createModifiers(Type type) {
        EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(type.getSlot());
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        if(slot==EquipmentSlotGroup.HEAD) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("chu_xin_helmet"),
                            2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.CHEST) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("chu_xin_chestplate"),
                            4,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("chu_xin_chestplate"),
                            4,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.LEGS) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("chu_xin_leggings"),
                            3,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.FEET) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("chu_xin_boots"),
                            2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("chu_xin_boots"),
                            0.05,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE), slot);
        }

        return builder.build();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit3"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.chu_xin.lore1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.chu_xin.lore2"));

            super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}