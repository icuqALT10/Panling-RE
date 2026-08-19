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

    private static final ResourceLocation HELMET_ID = ResourceLocation.withDefaultNamespace("xiong_shou_helmet");
    private static final ResourceLocation CHESTPLATE_ID = ResourceLocation.withDefaultNamespace("xiong_shou_chestplate");
    private static final ResourceLocation LEGGINGS_ID = ResourceLocation.withDefaultNamespace("xiong_shou_leggings");
    private static final ResourceLocation BOOTS_ID = ResourceLocation.withDefaultNamespace("xiong_shou_boots");

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
                            HELMET_ID,
                            8,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            HELMET_ID,
                            8,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            HELMET_ID,
                            3,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            HELMET_ID,
                            2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            HELMET_ID,
                            1,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(
                            HELMET_ID,
                            0.075,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.CHEST) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            16,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            14,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            5,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            3,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            0.075,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.LEGS) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            12,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            12,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            4,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            1,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            0.075,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.FEET) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            BOOTS_ID,
                            8,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            BOOTS_ID,
                            6,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            BOOTS_ID,
                            3,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            BOOTS_ID,
                            2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            BOOTS_ID,
                            1,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(
                            BOOTS_ID,
                            0.075,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }

        return builder.build();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {

            tooltip.add(Component.translatable("item.PanlingRE.lore.rare3"));
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit0"));
            tooltip.add(Component.translatable("item.PanlingRE.xiong_shou.lore1"));
            tooltip.add(Component.translatable("item.PanlingRE.xiong_shou.lore2"));

            super.appendHoverText(stack, context, tooltip, flag);
    }
}