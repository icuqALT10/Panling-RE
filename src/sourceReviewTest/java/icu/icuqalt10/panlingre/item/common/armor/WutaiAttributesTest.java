package icu.icuqalt10.panlingre.item.common.armor;

import icu.icuqalt10.panlingre.attachment.ZhiyeData;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.init.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;

@GameTestHolder("panlingre")
@PrefixGameTestTemplate(false)
public class WutaiAttributesTest {
    @GameTest(template = "empty", templateNamespace = "panlingre")
    public static void allProfessionCombinationsMergeEachArmorSlot(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (var item : List.of(ModItems.wutai_helmet, ModItems.wutai_chestplate,
                ModItems.wutai_leggings, ModItems.wutai_boots)) {
            ItemStack stack = new ItemStack(item.get());
            for (int mask = 0; mask < 8; mask++) {
                int warrior = mask & 1;
                int archer = (mask >> 1) & 1;
                int warlock = (mask >> 2) & 1;
                player.setData(ModAttachments.ZHIYE.get(),
                        new ZhiyeData(warrior != 0, archer != 0, warlock != 0));
                stack.inventoryTick(helper.getLevel(), player, 0, false);
                var modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
                var keys = new HashSet<>();
                for (var entry : modifiers.modifiers()) {
                    helper.assertTrue(keys.add(List.of(entry.attribute(), entry.modifier().id())),
                            "Duplicate attribute modifier for " + item.getId() + ", mask " + mask);
                }
                checkAmount(helper, modifiers, Attributes.ARMOR, warrior * 50 + archer * 25 + warlock * 20);
                checkAmount(helper, modifiers, Attributes.MAX_HEALTH, warrior * 50 + archer * 25 + warlock * 30);
                checkAmount(helper, modifiers, ModAttributes.MAX_LINGQI, warrior * 30 + archer * 30 + warlock * 25);
                checkAmount(helper, modifiers, Attributes.ATTACK_DAMAGE, warrior * 10);
                checkAmount(helper, modifiers, ModAttributes.ARROW_DAMAGE, archer * 10);
                boolean chest = item == ModItems.wutai_chestplate;
                checkAmount(helper, modifiers, ModAttributes.MAGIC_DAMAGE, warlock * (chest ? 10 : 5));
                checkAmount(helper, modifiers, ModAttributes.FALIZHI,
                        warrior * (chest ? 15 : 10) + (archer + warlock) * (chest ? 12 : 8));
                double speed = item == ModItems.wutai_boots ? (archer + warlock) * 0.25
                        : item == ModItems.wutai_helmet ? archer * 0.25 : 0;
                checkAmount(helper, modifiers, Attributes.MOVEMENT_SPEED, speed);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "panlingre")
    public static void equippedBonusesUpdateAndAreRemoved(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setData(ModAttachments.ZHIYE.get(), new ZhiyeData(true, true, true));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.wutai_helmet.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.wutai_chestplate.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.wutai_leggings.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.wutai_boots.get()));
        player.tick();
        player.tick();
        helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) == 380, "Full set must add all three professions");
        player.setData(ModAttachments.ZHIYE.get(), new ZhiyeData(false, true, false));
        player.tick();
        player.tick();
        helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) == 100, "Profession switch must remove old bonuses");
        for (var slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            player.setItemSlot(slot, ItemStack.EMPTY);
        }
        player.tick();
        helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) == 0, "Unequipping must remove bonuses");
        helper.succeed();
    }

    private static void checkAmount(GameTestHelper helper, ItemAttributeModifiers modifiers,
                                    Holder<Attribute> attribute, double expected) {
        double actual = modifiers.modifiers().stream().filter(entry -> entry.attribute().equals(attribute))
                .mapToDouble(entry -> entry.modifier().amount()).sum();
        helper.assertTrue(Math.abs(actual - expected) < 0.000001,
                attribute + ": expected " + expected + ", got " + actual);
    }
}
