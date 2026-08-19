package icu.icuqalt10.panlingre.item.warlock.yuansu;

import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attachment.YuansuData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.init.ModSounds;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ys2_mu extends Item {
    private final int cooldown = 400;
    private final float cost = 20.0f;

    public ys2_mu() {
        super(new Properties().stacksTo(64).fireResistant());
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
                float heal_value = (float) (player.getAttributeValue(ModAttributes.MAGIC_DAMAGE) *2);

                Ys2HealingSkill.execute(
                        serverLevel, player, itemStack, ModSounds.YS_MU,
                        0x00AAAA, heal_value, ys2_mu::applyTargetEffect);
            }

            itemStack.consume(1, player);
            cooldown_remove.cd_remove(player, this, cooldown);
            player.displayClientMessage(Component.translatable("item.PanlingRE.ys2_mu.skill.success"), true);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    public static void applyTargetEffect(LivingEntity target, float healValue) {
        target.heal(healValue);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        if (!YuansuData.hasPermission(SafeClientAccess.getClientPlayer(), "ys2")) {
            super.appendHoverText(stack, context, tooltip, flag);
            return;
        }

        if (SafeClientAccess.isShiftPressed()) {
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit2"));
            tooltip.add(Component.translatable("item.panlingre.ren_he_yuan.lore"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.ys2_mu.skill1.2"));
            tooltip.add(Component.translatable("item.PanlingRE.ys2_mu.skill2",
                    Component.keybind("key.use").withStyle(ChatFormatting.GOLD),
                    cooldown_remove.getCooldownText(SafeClientAccess.getClientPlayer(), cooldown),
                    LingQiData.getCostText(cost)));
            tooltip.add(Component.translatable("item.PanlingRE.ys2_mu.skill3"));
            tooltip.add(Component.translatable("item.PanlingRE.ys2_mu.skill4"));
        } else {
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit2"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.ys2_mu.skill1.1"));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
