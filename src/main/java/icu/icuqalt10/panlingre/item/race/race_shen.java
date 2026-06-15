package icu.icuqalt10.panlingre.item.race;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import icu.icuqalt10.panlingre.PanlingRE;
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
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class race_shen extends Item implements ICurioItem {
    public race_shen() {
        super(
                new Properties()
                        .stacksTo(1)
                        .fireResistant()
        );
    }

    //绑定诅咒效果
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        return slotContext.entity() instanceof Player player && player.isCreative();
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = HashMultimap.create();

            modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "race_shen"),
                    5,
                    AttributeModifier.Operation.ADD_VALUE
            ));

        return modifiers;
    }

    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

            tooltipComponents.add(Component.translatable("item.PanlingRE.race_shen.lore1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.race_shen.lore2"));

        super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}
