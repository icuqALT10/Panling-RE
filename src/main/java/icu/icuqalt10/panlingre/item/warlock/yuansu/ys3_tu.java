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

public class ys3_tu extends Item {
    private static final float BARRIER_HEALTH = 300.0F;
    private static final int BARRIER_DURATION = 60 * 20;
    private static final float BARRIER_DIAMETER = 30.0F;
    private static final int COOLDOWN = 1800;
    private static final float COST = 80.0F;

    public ys3_tu() { super(new Properties().stacksTo(64).fireResistant()); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!YuansuData.hasPermission(player, "ys3")) return super.use(level, player, hand);
        if (!player.getData(ModAttachments.LINGQI).consume(player, COST)) {
            return InteractionResultHolder.fail(stack);
        }
        if (level instanceof ServerLevel serverLevel) {
            ys2_tu.createGroundBarrier(serverLevel, player, stack,
                    BARRIER_HEALTH, BARRIER_DURATION, BARRIER_DIAMETER);
            stack.consume(1, player);
            cooldown_remove.cd_remove(player, this, COOLDOWN);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.YS_TU, SoundSource.PLAYERS, 0.5F, 1.0F);
            player.displayClientMessage(Component.translatable("item.PanlingRE.ys3_tu.skill.success"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        if (!YuansuData.hasPermission(SafeClientAccess.getClientPlayer(), "ys3")) {
            super.appendHoverText(stack, context, tooltip, flag);
            return;
        }
        tooltip.add(Component.translatable("item.PanlingRE.lore.limit2"));
        tooltip.add(Component.empty());
        if (SafeClientAccess.isShiftPressed()) {
            tooltip.add(Component.translatable("item.PanlingRE.ys3_tu.skill1.2"));
            tooltip.add(Component.translatable("item.PanlingRE.ys3_tu.skill2",
                    Component.keybind("key.use").withStyle(ChatFormatting.GOLD),
                    cooldown_remove.getCooldownText(SafeClientAccess.getClientPlayer(), COOLDOWN),
                    LingQiData.getCostText(COST)));
            tooltip.add(Component.translatable("item.PanlingRE.ys3_tu.skill3"));
            tooltip.add(Component.translatable("item.PanlingRE.ys3_tu.skill4"));
        } else {
            tooltip.add(Component.translatable("item.PanlingRE.ys3_tu.skill1.1"));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
