package icu.icuqalt10.panlingre.item.warlock.yuansu;

import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attachment.YuansuData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.entity.TuBarrierEntity;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModSounds;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ys2_tu extends Item {
    private static final float BARRIER_HEALTH = 100.0F;
    private static final int BARRIER_DURATION = 30 * 20;
    private static final float BARRIER_DIAMETER = 10.0F;

    private final int cooldown = 1200;
    private final float cost = 25.0F;

    public ys2_tu() {
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

        if (level instanceof ServerLevel serverLevel) {
            createBarrier(serverLevel, player, itemStack,
                    BARRIER_HEALTH, BARRIER_DURATION, BARRIER_DIAMETER);

            itemStack.consume(1, player);
            cooldown_remove.cd_remove(player, this, cooldown);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.YS_TU, SoundSource.PLAYERS, 0.5F, 1.0F);
            player.displayClientMessage(
                    Component.translatable("item.PanlingRE.ys2_tu.skill.success"), true);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    /** Shared entry point for ys3_tu and later earth barriers. */
    public static TuBarrierEntity createBarrier(ServerLevel level, Player owner, ItemStack stack,
                                                float health, int durationTicks, float diameter) {
        TuBarrierEntity barrier = new TuBarrierEntity(
                level, owner, stack, health, durationTicks, diameter);
        level.addFreshEntity(barrier);
        return barrier;
    }

    public static TuBarrierEntity createGroundBarrier(ServerLevel level, Player owner, ItemStack stack,
                                                      float health, int durationTicks, float diameter) {
        TuBarrierEntity barrier = new TuBarrierEntity(
                level, owner, stack, health, durationTicks, diameter, true);
        level.addFreshEntity(barrier);
        return barrier;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag flag) {
        if (!YuansuData.hasPermission(SafeClientAccess.getClientPlayer(), "ys2")) {
            super.appendHoverText(stack, context, tooltipComponents, flag);
            return;
        }

        if (SafeClientAccess.isShiftPressed()) {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit2"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.ys2_tu.skill1.2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ys2_tu.skill2",
                    Component.keybind("key.use").withStyle(ChatFormatting.GOLD),
                    cooldown_remove.getCooldownText(SafeClientAccess.getClientPlayer(), cooldown),
                    LingQiData.getCostText(cost)));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ys2_tu.skill3"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ys2_tu.skill4"));
        } else {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit2"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.ys2_tu.skill1.1"));
        }
        super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}
