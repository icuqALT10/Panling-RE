package icu.icuqalt10.panlingre.item.warlock.yuansu;

import icu.icuqalt10.panlingre.util.SkillTargeting;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
            List<SkillTargeting.Target> targets = SkillTargeting.aimed(player,
                    player.getBoundingBox().inflate(32), player.getEyePosition(), view, 32, 30).stream()
                    .filter(target -> JinLiRenEntity.isValidAttackTarget(player, target.part())).limit(3).toList();
            Vec3 launchRight = view.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (launchRight.lengthSqr() < 0.01D) launchRight = new Vec3(1.0D, 0.0D, 0.0D);
            launchRight = launchRight.normalize();
            double[] launchOffsets = {-0.65D, 0.0D, 0.65D};
            List<SkillTargeting.Target> bladeTargets = new ArrayList<>(3);
            for (int bladeIndex = 0; !targets.isEmpty() && bladeIndex < 3; bladeIndex++) {
                bladeTargets.add(targets.get(bladeIndex % targets.size()));
            }

            // 同一目标的三刃共享一次总伤害，由实际命中的利刃结算。
            Map<Integer, Integer> bladesPerTarget = new HashMap<>();
            for (SkillTargeting.Target target : bladeTargets) {
                bladesPerTarget.merge(target.root().getId(), 1, Integer::sum);
            }
            Map<Integer, JinLiRenEntity> damageCarriers = new HashMap<>();
            for (int bladeIndex = 0; bladeIndex < bladeTargets.size(); bladeIndex++) {
                SkillTargeting.Target target = bladeTargets.get(bladeIndex);
                double bladeDamage = attack_damage * bladesPerTarget.get(target.root().getId());
                Vec3 launchPoint = origin.add(launchRight.scale(launchOffsets[bladeIndex]));
                JinLiRenEntity blade = JinLiRenEntity.createCurved(
                        level, player, target.part(), target.point(), bladeDamage, launchPoint);
                if (blade != null) {
                    JinLiRenEntity carrier = damageCarriers.putIfAbsent(target.root().getId(), blade);
                    if (carrier != null) blade.shareDamageWith(carrier);
                    level.addFreshEntity(blade);
                }
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
