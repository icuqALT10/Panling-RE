package icu.icuqalt10.panlingre.item.common.armor;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.ZhiyeData;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.util.thread.EffectiveSide;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class wutai extends ArmorItem {

    private static final ResourceLocation HELMET_ID = ResourceLocation.withDefaultNamespace("wutai_helmet");
    private static final ResourceLocation CHESTPLATE_ID = ResourceLocation.withDefaultNamespace("wutai_chestplate");
    private static final ResourceLocation LEGGINGS_ID = ResourceLocation.withDefaultNamespace("wutai_leggings");
    private static final ResourceLocation BOOTS_ID = ResourceLocation.withDefaultNamespace("wutai_boots");

    public wutai(Holder<ArmorMaterial> material, Type type) {
        super(material, type, new Properties()
                .attributes(createModifiers(type, new ZhiyeData()))
                .stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide && entity instanceof Player player) {
            ItemAttributeModifiers modifiers = createModifiers(type, player.getData(ModAttachments.ZHIYE.get()));
            // 只在职业属性变化时更新组件，装备检测会自动移除旧加成并应用新加成。
            if (!modifiers.equals(stack.get(DataComponents.ATTRIBUTE_MODIFIERS))) {
                stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
            }
        }
    }

    private static ItemAttributeModifiers createModifiers(Type type, ZhiyeData zhiye) {
        EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(type.getSlot());
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        ResourceLocation id = switch (type) {
            case HELMET -> HELMET_ID;
            case CHESTPLATE -> CHESTPLATE_ID;
            case LEGGINGS -> LEGGINGS_ID;
            case BOOTS -> BOOTS_ID;
            case BODY -> throw new IllegalArgumentException("wutai only supports player armor slots");
        };

        //addBaseModifiers(builder, slot);
        // 职业权限彼此独立，同时开启时叠加所有对应加成。
        if (zhiye.warrior()) {
            addWarriorModifiers(builder, slot, id);
        }
        if (zhiye.archer()) {
            addArcherModifiers(builder, slot, id);
        }
        if (zhiye.warlock()) {
            addWarlockModifiers(builder, slot, id);
        }
        return mergeModifiers(builder.build());
    }

    private static ItemAttributeModifiers mergeModifiers(ItemAttributeModifiers modifiers) {
        Map<ItemAttributeModifiers.Entry, Double> amounts = new LinkedHashMap<>();
        for (var entry : modifiers.modifiers()) {
            var modifier = entry.modifier();
            var key = new ItemAttributeModifiers.Entry(entry.attribute(),
                    new AttributeModifier(modifier.id(), 0, modifier.operation()), entry.slot());
            // 同一属性、ID、部位和运算方式只生成一个修饰符，避免实际生效时相互覆盖。
            amounts.merge(key, modifier.amount(), modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    ? (left, right) -> (1 + left) * (1 + right) - 1 : Double::sum);
        }
        var builder = ItemAttributeModifiers.builder();
        amounts.forEach((entry, amount) -> builder.add(entry.attribute(),
                new AttributeModifier(entry.modifier().id(), amount, entry.modifier().operation()), entry.slot()));
        return builder.build();
    }

    @EventBusSubscriber(modid = PanlingRE.MODID, value = Dist.CLIENT)
    public static class ClientAttributes {
        @SubscribeEvent
        public static void onAttributeModifiers(ItemAttributeModifierEvent event) {
            // 单人游戏的服务端仍使用物品组件，不能套用本地查看者的职业。
            if (!EffectiveSide.get().isClient() || !(event.getItemStack().getItem() instanceof wutai armor)) return;
            Player player = SafeClientAccess.getClientPlayer();
            if (player == null) return;

            var modifiers = createModifiers(armor.type, player.getData(ModAttachments.ZHIYE.get()));
            event.clearModifiers();
            modifiers.modifiers().forEach(entry -> event.addModifier(entry.attribute(), entry.modifier(), entry.slot()));
        }
    }
/*
    private static void addBaseModifiers(ItemAttributeModifiers.Builder builder, EquipmentSlotGroup slot) {
        if(slot==EquipmentSlotGroup.HEAD) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            HELMET_ID,
                            2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }

        else if(slot==EquipmentSlotGroup.CHEST) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            4,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            4,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }

        else if(slot==EquipmentSlotGroup.LEGS) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            3,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }

        else if(slot==EquipmentSlotGroup.FEET) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            BOOTS_ID,
                            2,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(
                            BOOTS_ID,
                            0.05,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE), slot);
        }

    }
*/
    private static void addWarriorModifiers(ItemAttributeModifiers.Builder builder,
                                            EquipmentSlotGroup slot, ResourceLocation id) {

        if(slot==EquipmentSlotGroup.HEAD) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            HELMET_ID,
                            50,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            HELMET_ID,
                            50,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            HELMET_ID,
                            30,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            HELMET_ID,
                            10,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            HELMET_ID,
                            10,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(
                            HELMET_ID,
                            0.25,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.CHEST) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            50,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            50,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            30,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            10,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            15,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            0.25,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.LEGS) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            50,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            50,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            30,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            10,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            10,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            0.25,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.FEET) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            BOOTS_ID,
                            50,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            BOOTS_ID,
                            50,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            BOOTS_ID,
                            30,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            BOOTS_ID,
                            10,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            BOOTS_ID,
                            10,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(
                            BOOTS_ID,
                            0.25,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }

    }

    private static void addArcherModifiers(ItemAttributeModifiers.Builder builder,
                                           EquipmentSlotGroup slot, ResourceLocation id) {

        if(slot==EquipmentSlotGroup.HEAD) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            HELMET_ID,
                            25,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            HELMET_ID,
                            25,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(
                            HELMET_ID,
                            0.25,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            HELMET_ID,
                            30,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.ARROW_DAMAGE,
                    new AttributeModifier(
                            HELMET_ID,
                            10,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            HELMET_ID,
                            8,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.CHEST) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            25,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            25,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            30,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.ARROW_DAMAGE,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            10,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            12,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.LEGS) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            25,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            25,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            30,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.ARROW_DAMAGE,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            10,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            8,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.FEET) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            BOOTS_ID,
                            25,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            BOOTS_ID,
                            25,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(
                            BOOTS_ID,
                            0.25,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            BOOTS_ID,
                            30,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.ARROW_DAMAGE,
                    new AttributeModifier(
                            BOOTS_ID,
                            10,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            BOOTS_ID,
                            8,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }

    }

    private static void addWarlockModifiers(ItemAttributeModifiers.Builder builder,
                                            EquipmentSlotGroup slot, ResourceLocation id) {

        if(slot==EquipmentSlotGroup.HEAD) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            HELMET_ID,
                            20,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            HELMET_ID,
                            30,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            HELMET_ID,
                            25,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAGIC_DAMAGE,
                    new AttributeModifier(
                            HELMET_ID,
                            5,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            HELMET_ID,
                            8,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.CHEST) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            20,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            30,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            25,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAGIC_DAMAGE,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            10,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            CHESTPLATE_ID,
                            12,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.LEGS) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            20,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            30,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            25,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAGIC_DAMAGE,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            5,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            LEGGINGS_ID,
                            8,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }
        else if(slot==EquipmentSlotGroup.FEET) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(
                            BOOTS_ID,
                            20,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            BOOTS_ID,
                            30,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(
                            BOOTS_ID,
                            0.25,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE), slot);
            builder.add(ModAttributes.MAX_LINGQI,
                    new AttributeModifier(
                            BOOTS_ID,
                            25,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.MAGIC_DAMAGE,
                    new AttributeModifier(
                            BOOTS_ID,
                            5,
                            AttributeModifier.Operation.ADD_VALUE), slot);
            builder.add(ModAttributes.FALIZHI,
                    new AttributeModifier(
                            BOOTS_ID,
                            8,
                            AttributeModifier.Operation.ADD_VALUE), slot);
        }

    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {

        EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(type.getSlot());

            tooltip.add(Component.translatable("item.PanlingRE.lore.rare6"));
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit3"));

        if(slot==EquipmentSlotGroup.HEAD) {
            tooltip.add(Component.translatable("item.PanlingRE.wutai_helmet.lore1"));
            tooltip.add(Component.translatable("item.PanlingRE.wutai_helmet.lore2"));
            tooltip.add(Component.translatable("item.PanlingRE.wutai_helmet.lore3"));
        }

        if(slot==EquipmentSlotGroup.CHEST) {
            tooltip.add(Component.translatable("item.PanlingRE.wutai_chestplate.lore1"));
            tooltip.add(Component.translatable("item.PanlingRE.wutai_chestplate.lore2"));
        }

        if(slot==EquipmentSlotGroup.LEGS) {
            tooltip.add(Component.translatable("item.PanlingRE.wutai_leggings.lore1"));
            tooltip.add(Component.translatable("item.PanlingRE.wutai_leggings.lore2"));
        }

        if(slot==EquipmentSlotGroup.FEET) {
            tooltip.add(Component.translatable("item.PanlingRE.wutai_boots.lore1"));
            tooltip.add(Component.translatable("item.PanlingRE.wutai_boots.lore2"));
        }

            super.appendHoverText(stack, context, tooltip, flag);
    }
}
