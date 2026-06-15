package icu.icuqalt10.panlingre.item.archer.armor;

import icu.icuqalt10.panlingre.init.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

public class feng_xing extends ArmorItem {

    public feng_xing(Holder<ArmorMaterial> material, Type type) {
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
                            ResourceLocation.withDefaultNamespace("feng_xing_helmet"),
                            2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("feng_xing_helmet"),
                            0.1,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("feng_xing_helmet"),
                            1,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.ARROW_DAMAGE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("feng_xing_helmet"),
                            1,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.CHEST) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("feng_xing_chestplate"),
                            4,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("feng_xing_chestplate"),
                            4,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("feng_xing_chestplate"),
                            2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.ARROW_DAMAGE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("feng_xing_chestplate"),
                            2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.LEGS) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("feng_xing_leggings"),
                            3,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("feng_xing_leggings"),
                            2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("feng_xing_leggings"),
                            1,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.ARROW_DAMAGE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("feng_xing_leggings"),
                            1.5,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.FEET) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("feng_xing_boots"),
                            2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("feng_xing_boots"),
                            0.1,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("feng_xing_boots"),
                            1,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.ARROW_DAMAGE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("feng_xing_boots"),
                            0.5,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }

        return builder.build();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.feng_xing.lore1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.feng_xing.lore2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.feng_xing.lore3"));

            super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}