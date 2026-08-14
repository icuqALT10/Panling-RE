package icu.icuqalt10.panlingre.item.warlock.yuansu;

import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attachment.YuansuData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModEffects;
import icu.icuqalt10.panlingre.init.ModSounds;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ys2_shui extends Item {

    private final int cooldown = 1200;
    private final float cost = 25.0f;

    public ys2_shui() {
        super(
                new Properties()
                        .stacksTo(64)
                        .fireResistant()
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!YuansuData.hasPermission(player, "ys2")) {
            return super.use(level, player, hand);
        }

        LingQiData data = player.getData(ModAttachments.LINGQI);
        if (!data.consume(player, cost)) {
            return InteractionResultHolder.fail(itemStack);
        }

        if (!level.isClientSide) {
            if (level instanceof ServerLevel serverLevel) {
                Ys2HealingSkill.execute(
                        serverLevel, player, itemStack, ModSounds.YS_SHUI,
                        0x55FFFF, ys2_shui::applyTargetEffect);
            }

            itemStack.consume(1, player);
            cooldown_remove.cd_remove(player, this, cooldown);
            player.displayClientMessage(Component.translatable("item.PanlingRE.ys2_shui.skill.success"), true);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    public static void applyTargetEffect(LivingEntity target) {
        target.addEffect(new MobEffectInstance(
                MobEffects.HEAL, 1200, 1));
        target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED, 1200, 1));
        target.addEffect(new MobEffectInstance(
                ModEffects.ling_qi_recovery, 1200, 2));
        target.removeEffect(MobEffects.POISON);
        target.removeEffect(MobEffects.WITHER);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        if (!YuansuData.hasPermission(SafeClientAccess.getClientPlayer(), "ys2")) {
            super.appendHoverText(stack, context, tooltipComponents, flag);
            return;
        }

        // 检测Shift键
        if (SafeClientAccess.isShiftPressed()) {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit2"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.ys2_shui.skill1.2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ys2_shui.skill2"
                    ,Component.keybind("key.use").withStyle(ChatFormatting.GOLD),
                    cooldown_remove.getCooldownText(SafeClientAccess.getClientPlayer(), cooldown),
                    LingQiData.getCostText(cost)));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ys2_shui.skill3"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ys2_shui.skill4"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ys2_shui.skill5"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ys2_shui.skill6"));
        } else {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit2"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.ys2_shui.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}
