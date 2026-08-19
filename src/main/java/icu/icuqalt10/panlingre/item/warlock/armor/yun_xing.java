package icu.icuqalt10.panlingre.item.warlock.armor;

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

public class yun_xing extends ArmorItem {

    private static final ResourceLocation HELMET_ID = ResourceLocation.withDefaultNamespace("yun_xing_helmet");
    private static final ResourceLocation CHESTPLATE_ID = ResourceLocation.withDefaultNamespace("yun_xing_chestplate");
    private static final ResourceLocation LEGGINGS_ID = ResourceLocation.withDefaultNamespace("yun_xing_leggings");
    private static final ResourceLocation BOOTS_ID = ResourceLocation.withDefaultNamespace("yun_xing_boots");

    public yun_xing(Holder<ArmorMaterial> material, Type type) {
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
                            12,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            HELMET_ID,
                            15,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAGIC_DAMAGE,
                    new AttributeModifier(
                            HELMET_ID,
                            2.5,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            HELMET_ID,
                            4,
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
                            12,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            15,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAGIC_DAMAGE,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            5,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            6,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.LEGS) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            14,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            12,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            15,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAGIC_DAMAGE,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            2.5,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            4,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.FEET) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            BOOTS_ID,
                            12,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(
                            BOOTS_ID,
                            0.25,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            BOOTS_ID,
                            15,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAGIC_DAMAGE,
                    new AttributeModifier(
                            BOOTS_ID,
                            2.5,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            BOOTS_ID,
                            4,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }

        return builder.build();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {

            tooltip.add(Component.translatable("item.PanlingRE.lore.rare5"));
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit2"));
            tooltip.add(Component.translatable("item.PanlingRE.yun_xing.lore1"));
            tooltip.add(Component.translatable("item.PanlingRE.yun_xing.lore2"));
            tooltip.add(Component.translatable("item.PanlingRE.yun_xing.lore3"));

            super.appendHoverText(stack, context, tooltip, flag);
    }
}