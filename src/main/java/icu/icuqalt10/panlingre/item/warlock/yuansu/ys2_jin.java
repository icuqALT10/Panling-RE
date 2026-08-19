package icu.icuqalt10.panlingre.item.warlock.yuansu;

import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attachment.YuansuData;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ys2_jin extends Item {

    private final int cooldown = 80;
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
        if (!YuansuData.hasPermission(player, "ys2")) {
            return super.use(level, player, hand);
        }

        LingQiData data = player.getData(ModAttachments.LINGQI);
        // 如果灵气不足
        if (!data.consume(player, cost)) return InteractionResultHolder.fail(itemstack);

        // 释放技能
        if (!level.isClientSide) {
            float attack_damage = (float) (player.getAttributeValue(ModAttributes.MAGIC_DAMAGE) *3);

            Vec3 view = player.getViewVector(1.0F).normalize();

            // 与弩的多重射击相同：中间一发，两侧各偏转 10 度。
            Vec3 origin = player.getEyePosition().add(view.scale(0.8D)).add(0.0D, -0.5D, 0.0D);
            List<LivingEntity> targets = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(32.0D),
                    target -> JinLiRenEntity.isValidAttackTarget(player, target)));
            targets.removeIf(target -> angleTo(view, target.getEyePosition().subtract(origin)) > 30.0D);
            targets.sort(Comparator.<LivingEntity>comparingDouble(target -> angleTo(view,
                    target.getEyePosition().subtract(origin)))
                    .thenComparingDouble(player::distanceToSqr));
            targets = new ArrayList<>(targets.stream().limit(3).toList());
            Vec3 launchRight = view.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (launchRight.lengthSqr() < 0.01D) launchRight = new Vec3(1.0D, 0.0D, 0.0D);
            launchRight = launchRight.normalize();
            double[] launchOffsets = {-0.65D, 0.0D, 0.65D};
            List<LivingEntity> bladeTargets = new ArrayList<>(3);
            for (int bladeIndex = 0; !targets.isEmpty() && bladeIndex < 3; bladeIndex++) {
                bladeTargets.add(targets.get(bladeIndex % targets.size()));
            }

            // 同一目标会在同一 tick 被命中，逐刃调用 hurt 会被受伤保护合并掉。
            // 因此每个目标只由第一枚利刃结算一次总伤害，其余利刃仅保留视觉表现。
            Map<Integer, Integer> bladesPerTarget = new HashMap<>();
            for (LivingEntity target : bladeTargets) {
                bladesPerTarget.merge(target.getId(), 1, Integer::sum);
            }
            Set<Integer> settledTargets = new HashSet<>();
            for (int bladeIndex = 0; bladeIndex < bladeTargets.size(); bladeIndex++) {
                LivingEntity target = bladeTargets.get(bladeIndex);
                boolean dealsDamage = settledTargets.add(target.getId());
                double bladeDamage = dealsDamage
                        ? attack_damage * bladesPerTarget.get(target.getId())
                        : 0.0D;
                Vec3 launchPoint = origin.add(launchRight.scale(launchOffsets[bladeIndex]));
                JinLiRenEntity blade = JinLiRenEntity.createCurved(
                        level, player, target, bladeDamage, launchPoint);
                if (blade != null) level.addFreshEntity(blade);
            }
            if (targets.isEmpty()) {
                for (int bladeIndex = 0; bladeIndex < 3; bladeIndex++) {
                    Vec3 launchPoint = origin.add(launchRight.scale(launchOffsets[bladeIndex]));
                    JinLiRenEntity blade = new JinLiRenEntity(level, player, attack_damage);
                    blade.setPos(launchPoint);
                    blade.shoot(view.x, view.y, view.z, 2.25F, 0.0F);
                    level.addFreshEntity(blade);
                }
            }

            // 消耗
            itemstack.consume(1, player);
            // CD
            cooldown_remove.cd_remove(player, this, cooldown);
            // 音效
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.YS_JIN, SoundSource.PLAYERS, 0.5f, 1.0f);
            //播报
            player.displayClientMessage(Component.translatable("item.PanlingRE.ys2_jin.skill.success"), true);
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    private static double angleTo(Vec3 view, Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-8D) return 0.0D;
        return Math.toDegrees(Math.acos(Math.clamp(view.dot(direction.normalize()), -1.0D, 1.0D)));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        if (!YuansuData.hasPermission(SafeClientAccess.getClientPlayer(), "ys2")) {
            super.appendHoverText(stack, context, tooltip, flag);
            return;
        }
        // 检测 Shift 键
        if (SafeClientAccess.isShiftPressed()) {
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit2"));
            tooltip.add(Component.translatable("item.panlingre.ren_he_yuan.lore"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.ys2_jin.skill1.2"));
            tooltip.add(Component.translatable("item.PanlingRE.ys2_jin.skill2",
                    Component.keybind("key.use").withStyle(ChatFormatting.GOLD),
                    cooldown_remove.getCooldownText(SafeClientAccess.getClientPlayer(), cooldown),
                    LingQiData.getCostText(cost)));
            tooltip.add(Component.translatable("item.PanlingRE.ys2_jin.skill3"));
            tooltip.add(Component.translatable("item.PanlingRE.ys2_jin.skill4"));
        } else {
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit2"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.ys2_jin.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }
}
