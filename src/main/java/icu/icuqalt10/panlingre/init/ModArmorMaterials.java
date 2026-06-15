package icu.icuqalt10.panlingre.init;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

public class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, PanlingRE.MODID);

    public static final Holder<ArmorMaterial> LEATHER = ARMOR_MATERIALS.register("leather", () -> {
        return new ArmorMaterial(
                new EnumMap<>(ArmorItem.Type.class),
                15, // 附魔等级
                SoundEvents.ARMOR_EQUIP_LEATHER, // 装备音效
                () -> Ingredient.of(Items.LEATHER), // 修复材料
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "leather"))),
                0.0F, // 韧性
                0.0F  // 击退抗性
        );
    });

    public static final Holder<ArmorMaterial> WARRIOR = ARMOR_MATERIALS.register("warrior", () -> {
        return new ArmorMaterial(
                new EnumMap<>(ArmorItem.Type.class),
                15, // 附魔等级
                SoundEvents.ARMOR_EQUIP_IRON, // 装备音效
                () -> Ingredient.of(Items.IRON_INGOT), // 修复材料
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "warrior"))),
                0.0F, // 韧性
                0.0F  // 击退抗性
        );
    });

    public static final Holder<ArmorMaterial> ARCHER = ARMOR_MATERIALS.register("archer", () -> {
        return new ArmorMaterial(
                new EnumMap<>(ArmorItem.Type.class),
                15, // 附魔等级
                SoundEvents.ARMOR_EQUIP_IRON, // 装备音效
                () -> Ingredient.of(Items.GOLD_INGOT), // 修复材料
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "archer"))),
                0.0F, // 韧性
                0.0F  // 击退抗性
        );
    });

    public static final Holder<ArmorMaterial> WARLOCK = ARMOR_MATERIALS.register("warlock", () -> {
        return new ArmorMaterial(
                new EnumMap<>(ArmorItem.Type.class),
                15, // 附魔等级
                SoundEvents.ARMOR_EQUIP_IRON, // 装备音效
                () -> Ingredient.of(Items.CAMPFIRE), // 修复材料
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "warlock"))),
                0.0F, // 韧性
                0.0F  // 击退抗性
        );
    });

    public static final Holder<ArmorMaterial> COMMON = ARMOR_MATERIALS.register("common", () -> {
        return new ArmorMaterial(
                new EnumMap<>(ArmorItem.Type.class),
                15, // 附魔等级
                SoundEvents.ARMOR_EQUIP_IRON, // 装备音效
                () -> Ingredient.of(Items.DIAMOND), // 修复材料
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "common"))),
                0.0F, // 韧性
                0.0F  // 击退抗性
        );
    });
}