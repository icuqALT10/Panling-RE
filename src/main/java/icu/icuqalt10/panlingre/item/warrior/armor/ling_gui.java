package icu.icuqalt10.panlingre.item.warrior.armor;

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

public class ling_gui extends ArmorItem {

    public ling_gui(Holder<ArmorMaterial> material, Type type) {
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
                            ResourceLocation.withDefaultNamespace("ling_gui_helmet"),
                            12,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("ling_gui_helmet"),
                            10,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("ling_gui_helmet"),
                            5,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("ling_gui_helmet"),
                            4,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("ling_gui_helmet"),
                            2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("ling_gui_helmet"),
                            0.1,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.CHEST) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("ling_gui_chestplate"),
                            20,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("ling_gui_chestplate"),
                            16,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("ling_gui_chestplate"),
                            7,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("ling_gui_chestplate"),
                            5,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("ling_gui_chestplate"),
                            4,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("ling_gui_chestplate"),
                            0.1,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.LEGS) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("ling_gui_leggings"),
                            16,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("ling_gui_leggings"),
                            14,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("ling_gui_leggings"),
                            6,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("ling_gui_leggings"),
                            4,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("ling_gui_leggings"),
                            2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("ling_gui_leggings"),
                            0.1,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.FEET) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("ling_gui_boots"),
                            12,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("ling_gui_boots"),
                            8,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("ling_gui_boots"),
                            5,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("ling_gui_boots"),
                            4,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("ling_gui_boots"),
                            2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("ling_gui_boots"),
                            0.1,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }

        return builder.build();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare4"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit0"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ling_gui.lore1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ling_gui.lore2"));

            super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}