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

public class long_lin extends ArmorItem {

    public long_lin(Holder<ArmorMaterial> material, Type type) {
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
                            ResourceLocation.withDefaultNamespace("long_lin_helmet"),
                            16,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("long_lin_helmet"),
                            12,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("long_lin_helmet"),
                            8,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("long_lin_helmet"),
                            6,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("long_lin_helmet"),
                            4,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("long_lin_helmet"),
                            0.2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.CHEST) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("long_lin_chestplate"),
                            24,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("long_lin_chestplate"),
                            18,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("long_lin_chestplate"),
                            10,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("long_lin_chestplate"),
                            7,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("long_lin_chestplate"),
                            6,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("long_lin_chestplate"),
                            0.2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.LEGS) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("long_lin_leggings"),
                            20,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("long_lin_leggings"),
                            14,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("long_lin_leggings"),
                            11,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("long_lin_leggings"),
                            6,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("long_lin_leggings"),
                            4,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("long_lin_leggings"),
                            0.2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.FEET) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("long_lin_boots"),
                            16,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("long_lin_boots"),
                            10,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("long_lin_boots"),
                            8,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("long_lin_boots"),
                            6,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("long_lin_boots"),
                            4,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("long_lin_boots"),
                            0.2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }

        return builder.build();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare5"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit0"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.long_lin.lore1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.long_lin.lore2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.long_lin.lore3"));

            super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}