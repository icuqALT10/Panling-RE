package icu.icuqalt10.panlingre.item.other;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.BlessData;
import icu.icuqalt10.panlingre.init.ModAttributes;
import net.minecraft.client.Minecraft;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class bless_shengshou extends Item implements ICurioItem {
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
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = HashMultimap.create();

        if (!(slotContext.entity() instanceof Player player)) {
            return modifiers;
        }

            modifiers.put(ModAttributes.MAX_LINGQI, new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "bless_qinglong"),
                    BlessData.hasBless(player, "qinglong") ? 5.0 : 0.0,
                    AttributeModifier.Operation.ADD_VALUE
            ));

            modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "bless_zhuque"),
                    BlessData.hasBless(player, "zhuque") ? 5.0 : 0.0,
                    AttributeModifier.Operation.ADD_VALUE
            ));

            modifiers.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "bless_baihu"),
                    BlessData.hasBless(player, "baihu") ? 0.25 : 0.0,
                    AttributeModifier.Operation.ADD_VALUE
            ));

            modifiers.put(ModAttributes.COOLDOWN_REMOVE, new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "bless_xuanwu"),
                    BlessData.hasBless(player, "xuanwu") ? 0.1 : 0.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ));

        return modifiers;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        tooltipComponents.add(Component.translatable("item.PanlingRE.bless_shengshou.lore1"));
        tooltipComponents.add(Component.translatable("item.PanlingRE.bless_shengshou.lore2"));
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("item.PanlingRE.bless_shengshou.skill"));

        if (FMLEnvironment.dist == Dist.CLIENT) {
            Player player = Minecraft.getInstance().player;

            if (player != null) {
                if (BlessData.hasBless(player, "qinglong"))
                    tooltipComponents.add(Component.translatable("item.PanlingRE.bless_shengshou.skill1"));

                if (BlessData.hasBless(player, "zhuque"))
                    tooltipComponents.add(Component.translatable("item.PanlingRE.bless_shengshou.skill2"));

                if (BlessData.hasBless(player, "baihu"))
                    tooltipComponents.add(Component.translatable("item.PanlingRE.bless_shengshou.skill3"));

                if (BlessData.hasBless(player, "xuanwu"))
                    tooltipComponents.add(Component.translatable("item.PanlingRE.bless_shengshou.skill4"));
            }
        }

        super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}