package icu.icuqalt10.panlingre.item.warlock.yuansu;

import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attachment.YuansuData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.init.ModAttachments;
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

public class ys2_huo extends Item {

    private final int cooldown = 1200;
    private final float cost = 25.0f;

    public ys2_huo() {
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
                        serverLevel, player, itemStack, ModSounds.YS_HUO,
                        0xFF5555, ys2_huo::applyTargetEffect);
            }

            itemStack.consume(1, player);
            cooldown_remove.cd_remove(player, this, cooldown);
            player.displayClientMessage(Component.translatable("item.PanlingRE.ys2_huo.skill.success"), true);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    public static void applyTargetEffect(LivingEntity target) {
        target.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_BOOST, 1200, 1));
        target.addEffect(new MobEffectInstance(
                MobEffects.DIG_SPEED, 1200, 1));
        target.addEffect(new MobEffectInstance(
                MobEffects.FIRE_RESISTANCE, 1200, 0));
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
            tooltipComponents.add(Component.translatable("item.PanlingRE.ys2_huo.skill1.2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ys2_huo.skill2"
                    ,Component.keybind("key.use").withStyle(ChatFormatting.GOLD),
                    cooldown_remove.getCooldownText(SafeClientAccess.getClientPlayer(), cooldown),
                    LingQiData.getCostText(cost)));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ys2_huo.skill3"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ys2_huo.skill4"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ys2_huo.skill5"));
        } else {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit2"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.ys2_huo.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}
