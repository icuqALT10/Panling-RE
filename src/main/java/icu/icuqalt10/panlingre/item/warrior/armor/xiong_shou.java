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

public class xiong_shou extends ArmorItem {

    public xiong_shou(Holder<ArmorMaterial> material, Type type) {
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
                            ResourceLocation.withDefaultNamespace("xiong_shou_helmet"),
                            8,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("xiong_shou_helmet"),
                            8,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("xiong_shou_helmet"),
                            3,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("xiong_shou_helmet"),
                            2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("xiong_shou_helmet"),
                            1,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("xiong_shou_helmet"),
                            0.075,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.CHEST) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("xiong_shou_chestplate"),
                            16,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("xiong_shou_chestplate"),
                            14,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("xiong_shou_chestplate"),
                            5,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("xiong_shou_chestplate"),
                            3,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("xiong_shou_chestplate"),
                            2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("xiong_shou_chestplate"),
                            0.075,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.LEGS) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("xiong_shou_leggings"),
                            12,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("xiong_shou_leggings"),
                            12,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("xiong_shou_leggings"),
                            4,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("xiong_shou_leggings"),
                            2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("xiong_shou_leggings"),
                            1,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("xiong_shou_leggings"),
                            0.075,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.FEET) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("xiong_shou_boots"),
                            8,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("xiong_shou_boots"),
                            6,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("xiong_shou_boots"),
                            3,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("xiong_shou_boots"),
                            2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("xiong_shou_boots"),
                            1,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("xiong_shou_boots"),
                            0.075,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }

        return builder.build();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare3"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit0"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.xiong_shou.lore1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.xiong_shou.lore2"));

            super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}