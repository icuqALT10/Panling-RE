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

public class zhan_lang extends ArmorItem {

    public zhan_lang(Holder<ArmorMaterial> material, Type type) {
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
                            ResourceLocation.withDefaultNamespace("zhan_lang_helmet"),
                            4,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("zhan_lang_helmet"),
                            4,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("zhan_lang_helmet"),
                            2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("zhan_lang_helmet"),
                            0.5,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.CHEST) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("zhan_lang_chestplate"),
                            8,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("zhan_lang_chestplate"),
                            7,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("zhan_lang_chestplate"),
                            3,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("zhan_lang_chestplate"),
                            1,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.LEGS) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("zhan_lang_leggings"),
                            6,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("zhan_lang_leggings"),
                            6,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("zhan_lang_leggings"),
                            3,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("zhan_lang_leggings"),
                            0.75,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.FEET) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("zhan_lang_boots"),
                            4,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("zhan_lang_boots"),
                            3,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("zhan_lang_boots"),
                            2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            ResourceLocation.withDefaultNamespace("zhan_lang_boots"),
                            0.25,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }

        return builder.build();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit0"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.zhan_lang.lore1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.zhan_lang.lore2"));

            super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}