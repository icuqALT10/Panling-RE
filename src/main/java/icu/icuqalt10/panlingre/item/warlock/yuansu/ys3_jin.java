package icu.icuqalt10.panlingre.item.warlock.yuansu;

import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attachment.YuansuData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.entity.Ys3JinTornadoEntity;
import icu.icuqalt10.panlingre.entity.Ys3DomainEntity;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.init.ModSounds;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ys3_jin extends Item {

    private final int cooldown = 600;
    private final float cost = 65.0f;

    public ys3_jin() {
        super(
                new Properties()
                        .stacksTo(64)
                        .fireResistant()
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (!YuansuData.hasPermission(player, "ys3")) {
            return super.use(level, player, hand);
        }

        LingQiData data = player.getData(ModAttachments.LINGQI);
        // 如果灵气不足
        if (!data.consume(player, cost)) return InteractionResultHolder.fail(itemstack);

        // 释放技能
        if (level instanceof ServerLevel serverLevel) {
            float attackDamage = (float) (player.getAttributeValue(ModAttributes.MAGIC_DAMAGE) * 2);
            Vec3 ground = Ys3DomainEntity.findGroundPosition(serverLevel, player);

            serverLevel.addFreshEntity(new Ys3JinTornadoEntity(serverLevel, player, ground,
                    itemstack, attackDamage));

            // If the caster is airborne, force a quick descent and clear accumulated fall damage.
            if (!player.onGround()) {
                player.setDeltaMovement(player.getDeltaMovement().x, -1.5D, player.getDeltaMovement().z);
                player.resetFallDistance();
            }

            // 消耗
            itemstack.consume(1, player);
            // CD
            cooldown_remove.cd_remove(player, this, cooldown);
            // 音效
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.YS_JIN, SoundSource.PLAYERS, 0.5f, 1.0f);
            //播报
            player.displayClientMessage(Component.translatable("item.PanlingRE.ys3_jin.skill.success"), true);
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        if (!YuansuData.hasPermission(SafeClientAccess.getClientPlayer(), "ys3")) {
            super.appendHoverText(stack, context, tooltip, flag);
            return;
        }
        // 检测 Shift 键
        if (SafeClientAccess.isShiftPressed()) {
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit2"));
            tooltip.add(Component.translatable("item.panlingre.ren_he_yuan.lore"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.ys3_jin.skill1.2"));
            tooltip.add(Component.translatable("item.PanlingRE.ys3_jin.skill2",
                    Component.keybind("key.use").withStyle(ChatFormatting.GOLD),
                    cooldown_remove.getCooldownText(SafeClientAccess.getClientPlayer(), cooldown),
                    LingQiData.getCostText(cost)));
            tooltip.add(Component.translatable("item.PanlingRE.ys3_jin.skill3"));
            tooltip.add(Component.translatable("item.PanlingRE.ys3_jin.skill4"));
        } else {
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit2"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.ys3_jin.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }
}
