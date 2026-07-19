package icu.icuqalt10.panlingre.item.warrior;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModEffects;
import icu.icuqalt10.panlingre.item.skill_trigger;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class tai_ji_jian extends SwordItem implements skill_trigger {

    public tai_ji_jian() {
        super(
                Tiers.DIAMOND,
                new Properties()
                        .stacksTo(1)
                        .fireResistant()
        );
    }

    //拦截扣除耐久 无法破坏
    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {}

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        builder.add(
                Attributes.ARMOR,
                new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "tai_ji_jian"),
                        15,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        builder.add(
                Attributes.MOVEMENT_SPEED,
                new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "tai_ji_jian"),
                        -0.15,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        builder.add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        BASE_ATTACK_DAMAGE_ID,
                        18.0,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        builder.add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        BASE_ATTACK_SPEED_ID,
                        -2.2,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        return builder.build();
    }

    @Override
    public boolean skill_use(Level level, Player player, ItemStack stack, int skillIndex) {
        //释放技能
        if (!level.isClientSide) {
            if (level.isDay()) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1,false, false,true));
                player.addEffect(new MobEffectInstance(ModEffects.jia_yu, 200, 1,false, false,true));
                player.displayClientMessage(Component.translatable("item.PanlingRE.tai_ji_jian.skill.success.day"), true);
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0,false, false,true));
                player.addEffect(new MobEffectInstance(ModEffects.jia_yu, 200, 3,false, false,true));
                player.displayClientMessage(Component.translatable("item.PanlingRE.tai_ji_jian.skill.success.night"), true);
            }

            //音效
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 0.5f,1.0f);
            }

        return true;
    }

    @Override
    public long getSkillCD(int skillIndex) {
        return 5000L;
    }

    @Override
    public String getSkillNameKey(int skillIndex) {
        return "item.PanlingRE.tai_ji_jian.skill1.2";
    }

    @Override
    public float getSkillLingQiCost(int skillIndex) {
        return 10;
    }

    @Override
    public String[] getSkillDescription(int skillIndex) {
        return new String[]{
                "item.PanlingRE.tai_ji_jian.skill3",
                "item.PanlingRE.tai_ji_jian.skill4",
                "item.PanlingRE.tai_ji_jian.skill5"
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

        // 检测Shift键
        if (SafeClientAccess.isShiftPressed()) {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare4"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit0"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.tai_ji_jian.lore1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.tai_ji_jian.lore2"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.tai_ji_jian.skill1.2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.tai_ji_jian.skill2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.tai_ji_jian.skill3"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.tai_ji_jian.skill4"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.tai_ji_jian.skill5"));
        } else {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare4"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit0"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.tai_ji_jian.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}
