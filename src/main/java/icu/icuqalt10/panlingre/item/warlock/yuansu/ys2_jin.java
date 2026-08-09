package icu.icuqalt10.panlingre.item.warlock.yuansu;

import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.entity.JinLiRenEntity;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.init.ModSounds;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public class ys2_jin extends Item {

    private final int cooldown = 60;
    private final float cost = 10.0f;

    public ys2_jin() {
        super(
                new Properties()
                        .stacksTo(64)
                        .fireResistant()
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        LingQiData data = player.getData(ModAttachments.LINGQI);
        // 如果灵气不足
        if (!data.consume(player, cost)) return InteractionResultHolder.fail(itemstack);

        // 释放技能
        if (!level.isClientSide) {
            float attack_damage = (float) (player.getAttributeValue(ModAttributes.MAGIC_DAMAGE) *5);

            Vec3 view = player.getViewVector(1.0F);
            Vec3 up = player.getUpVector(1.0F);

            // 与弩的多重射击相同：中间一发，两侧各偏转 10 度。
            for (float angle : new float[]{0.0F, -10.0F, 10.0F}) {
                Vector3f direction = view.toVector3f().rotate(
                        new Quaternionf().setAngleAxis(
                                angle * (float)(Math.PI / 180.0D), up.x, up.y, up.z));

                JinLiRenEntity blade = new JinLiRenEntity(level, player, attack_damage, 2);
                blade.shoot(direction.x(), direction.y(), direction.z(), 1.5F, 0.0F);
                level.addFreshEntity(blade);
            }

            // 消耗
            itemstack.consume(1, player);
            // CD
            cooldown_remove.cd_remove(player, this, cooldown);
            // 音效
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.YS_JIN, SoundSource.PLAYERS, 0.5f, 1.0f);
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag flag) {
        // 检测 Shift 键
        if (SafeClientAccess.isShiftPressed()) {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare2"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.ys2_jin.skill1.2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ys2_jin.skill2",
                    Component.keybind("key.use").withStyle(ChatFormatting.GOLD),
                    cooldown_remove.getCooldownText(SafeClientAccess.getClientPlayer(), cooldown),
                    LingQiData.getCostText(cost)));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ys2_jin.skill3"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ys2_jin.skill4"));
        } else {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare2"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.ys2_jin.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}
