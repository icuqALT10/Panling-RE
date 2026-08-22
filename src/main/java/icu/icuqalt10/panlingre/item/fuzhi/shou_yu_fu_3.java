package icu.icuqalt10.panlingre.item.fuzhi;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class shou_yu_fu_3 extends FuZhiItem {

    public static final int CAST_TIME_TICKS = 60;
    public static final double FALIZHI_BONUS = 1.0D;
    private static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "shou_yu_fu_3");

    public shou_yu_fu_3() {
        super(400, 20.0f, CAST_TIME_TICKS, FALIZHI_BONUS, "shou_yu_fu_3", 3, 4);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        builder.add(
                ModAttributes.FALIZHI,
                new AttributeModifier(
                        MODIFIER_ID,
                        FALIZHI_BONUS,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        return builder.build();
    }

    @Override
    protected void applyEffect(Level level, Player player) {
        float healAmount = (float) player.getAttributeValue(ModAttributes.FALIZHI) * 2f;
        player.heal(healAmount);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 2));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltip, TooltipFlag flag) {

        // 检测Shift键
        if (SafeClientAccess.isShiftPressed()) {
            tooltip.add(Component.translatable("item.PanlingRE.lore.rare5"));
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit3"));
            tooltip.add(Component.translatable("item.PanlingRE.shou_yu_fu_3.lore1"));
            tooltip.add(Component.translatable("item.PanlingRE.shou_yu_fu_3.lore2"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.shou_yu_fu_3.skill1.2"));
            tooltip.add(Component.translatable("item.PanlingRE.shou_yu_fu_3.skill2"
                    ,Component.keybind("key.use").withStyle(ChatFormatting.GOLD),
                    cooldown_remove.getCooldownText(SafeClientAccess.getClientPlayer(), cooldown),
                    LingQiData.getCostText(cost)));
            tooltip.add(Component.translatable("item.PanlingRE.shou_yu_fu_3.skill3"));
            tooltip.add(Component.translatable("item.PanlingRE.shou_yu_fu_3.skill4"));
        } else {
            tooltip.add(Component.translatable("item.PanlingRE.lore.rare5"));
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit3"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.shou_yu_fu_3.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }
}
