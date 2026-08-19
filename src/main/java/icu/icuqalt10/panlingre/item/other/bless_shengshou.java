package icu.icuqalt10.panlingre.item.other;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.BlessData;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class bless_shengshou extends Item implements ICurioItem {
    private static final ResourceLocation QINGLONG_ID =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "bless_qinglong");
    private static final ResourceLocation ZHUQUE_ID =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "bless_zhuque");
    private static final ResourceLocation BAIHU_ID =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "bless_baihu");
    private static final ResourceLocation XUANWU_ID =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "bless_xuanwu");

    public bless_shengshou() {
        super(
                new Properties()
                        .stacksTo(1)
                        .fireResistant()
        );
    }

    // 绑定诅咒效果
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        return slotContext.entity() instanceof Player player && player.isCreative();
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player)) {
            return HashMultimap.create();
        }

        return createModifiers(player);
    }

    private static Multimap<Holder<Attribute>, AttributeModifier> createModifiers(Player player) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = HashMultimap.create();

        modifiers.put(ModAttributes.MAX_LINGQI, new AttributeModifier(
                QINGLONG_ID,
                BlessData.hasBless(player, "qinglong") ? 5.0 : 0.0,
                AttributeModifier.Operation.ADD_VALUE
        ));

        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(
                ZHUQUE_ID,
                BlessData.hasBless(player, "zhuque") ? 5.0 : 0.0,
                AttributeModifier.Operation.ADD_VALUE
        ));

        modifiers.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(
                BAIHU_ID,
                BlessData.hasBless(player, "baihu") ? 0.25 : 0.0,
                AttributeModifier.Operation.ADD_VALUE
        ));

        modifiers.put(ModAttributes.COOLDOWN_REMOVE, new AttributeModifier(
                XUANWU_ID,
                BlessData.hasBless(player, "xuanwu") ? 0.1 : 0.0,
                AttributeModifier.Operation.ADD_VALUE
        ));

        return modifiers;
    }

    /**
     * Curios only evaluates an equipped stack's attribute modifiers when its equipment state
     * changes. Bless data can change without moving the stack, so replace the modifiers here.
     */
    public static void refreshAttributes(Player player) {
        removeModifier(player, ModAttributes.MAX_LINGQI, QINGLONG_ID);
        removeModifier(player, Attributes.MAX_HEALTH, ZHUQUE_ID);
        removeModifier(player, Attributes.KNOCKBACK_RESISTANCE, BAIHU_ID);
        removeModifier(player, ModAttributes.COOLDOWN_REMOVE, XUANWU_ID);

        boolean equipped = CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.isEquipped(
                        stack -> stack.getItem() instanceof bless_shengshou))
                .orElse(false);
        if (!equipped) {
            return;
        }

        createModifiers(player).forEach((attribute, modifier) -> {
            var instance = player.getAttribute(attribute);
            if (instance != null) {
                instance.addOrUpdateTransientModifier(modifier);
            }
        });
    }

    private static void removeModifier(Player player, Holder<Attribute> attribute, ResourceLocation id) {
        var instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.PanlingRE.bless_shengshou.lore1"));
        tooltip.add(Component.translatable("item.PanlingRE.bless_shengshou.lore2"));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("item.PanlingRE.bless_shengshou.skill"));

        Player player = SafeClientAccess.getClientPlayer();
        if (player != null) {
            if (BlessData.hasBless(player, "qinglong"))
                tooltip.add(Component.translatable("item.PanlingRE.bless_shengshou.skill1"));

            if (BlessData.hasBless(player, "zhuque"))
                tooltip.add(Component.translatable("item.PanlingRE.bless_shengshou.skill2"));

            if (BlessData.hasBless(player, "baihu"))
                tooltip.add(Component.translatable("item.PanlingRE.bless_shengshou.skill3"));

            if (BlessData.hasBless(player, "xuanwu"))
                tooltip.add(Component.translatable("item.PanlingRE.bless_shengshou.skill4"));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }
}
