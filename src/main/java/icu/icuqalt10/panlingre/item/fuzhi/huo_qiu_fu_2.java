package icu.icuqalt10.panlingre.item.fuzhi;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.entity.HuoQiuFuEntity;
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

public class huo_qiu_fu_2 extends FuZhiItem {

    public static final int CAST_TIME_TICKS = 15;
    public static final double FALIZHI_BONUS = 2.0D;
    private static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "huo_qiu_fu_2");

    public huo_qiu_fu_2() {
        super(60, 10.0f, CAST_TIME_TICKS, FALIZHI_BONUS, "huo_qiu_fu_2", 3, 4);
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
        float damage = (float) (player.getAttributeValue(ModAttributes.FALIZHI) * 2.5);
        HuoQiuFuEntity fireball = new HuoQiuFuEntity(level, player, damage);
        fireball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
        level.addFreshEntity(fireball);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltip, TooltipFlag flag) {

        // 检测Shift键
        if (SafeClientAccess.isShiftPressed()) {
            tooltip.add(Component.translatable("item.PanlingRE.lore.rare4"));
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit3"));
            tooltip.add(Component.translatable("item.PanlingRE.huo_qiu_fu_2.lore1"));
            tooltip.add(Component.translatable("item.PanlingRE.huo_qiu_fu_2.lore2"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.huo_qiu_fu_2.skill1.2"));
            tooltip.add(Component.translatable("item.PanlingRE.huo_qiu_fu_2.skill2"
                    ,Component.keybind("key.use").withStyle(ChatFormatting.GOLD),
                    cooldown_remove.getCooldownText(SafeClientAccess.getClientPlayer(), cooldown),
                    LingQiData.getCostText(cost)));
            tooltip.add(Component.translatable("item.PanlingRE.huo_qiu_fu_2.skill3"));
            tooltip.add(Component.translatable("item.PanlingRE.huo_qiu_fu_2.skill4"));
        } else {
            tooltip.add(Component.translatable("item.PanlingRE.lore.rare4"));
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit3"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.huo_qiu_fu_2.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }
}
