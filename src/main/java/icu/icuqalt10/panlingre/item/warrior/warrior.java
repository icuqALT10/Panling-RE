package icu.icuqalt10.panlingre.item.warrior;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class warrior extends Item implements ICurioItem {
    public warrior() {
        super(
                new Item.Properties()
                        .stacksTo(1)
                        .fireResistant()
        );
    }

    //绑定诅咒效果
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        return slotContext.entity() instanceof Player player && player.isCreative();
    }
/*
    //法力值转化为近战伤害
    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = HashMultimap.create();

        LivingEntity entity = slotContext.entity();
        if (entity == null) return HashMultimap.create();

        var customAttrInst = entity.getAttribute(ModAttributes.FALIZHI);

            modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "falizhi"),
                    customAttrInst.getValue(),
                    AttributeModifier.Operation.ADD_VALUE
            ));

        return modifiers;
    }

    //每tick强制更新
    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();

        if (entity.level().isClientSide) return;

        var manaAttr = entity.getAttribute(ModAttributes.FALIZHI);
        if (manaAttr == null) return;

        double currentMana = manaAttr.getValue();

        var attackAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "falizhi");

            AttributeModifier existing = attackAttr.getModifier(modifierId);

            if (existing == null || existing.amount() != currentMana) {
                attackAttr.removeModifier(modifierId);

                attackAttr.addTransientModifier(new AttributeModifier(
                        modifierId,
                        currentMana,
                        AttributeModifier.Operation.ADD_VALUE
                ));
            }
        }
    }
*/
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

            tooltipComponents.add(Component.translatable("item.PanlingRE.warrior.lore1"));

        super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}
