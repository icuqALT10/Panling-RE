package icu.icuqalt10.panlingre.item.warrior;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.entity.FeiXianJianZhenEntity;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModComponents;
import icu.icuqalt10.panlingre.init.ModEntities;
import icu.icuqalt10.panlingre.item.skill_trigger;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ding_hai_shen_zhen extends SwordItem implements skill_trigger {

    public ding_hai_shen_zhen() {
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
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        boolean isPowered = stack.getOrDefault(ModComponents.IS_POWERED.get(), false);

        builder.add(
                Attributes.ARMOR,
                new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "ding_hai_shen_zhen"),
                        isPowered ? -50 : 10.0,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        if (!isPowered) {
            builder.add(
                    Attributes.ENTITY_INTERACTION_RANGE,
                    new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "ding_hai_shen_zhen"),
                            3.0,
                            AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
            );
        }

        builder.add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        BASE_ATTACK_DAMAGE_ID,
                        isPowered ? 15 : 40,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        builder.add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        BASE_ATTACK_SPEED_ID,
                        isPowered ? 100.0 : -2.6,
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
            stack.set(ModComponents.IS_POWERED.get(), true);
            stack.set(ModComponents.POWERED_TIMER.get(), level.getGameTime());

            //音效
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5f,1.0f);
            //播报
            player.displayClientMessage(Component.translatable("item.PanlingRE.ding_hai_shen_zhen.skill.success"), true);
            }

        return true;
    }
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && stack.getOrDefault(ModComponents.IS_POWERED.get(), false)) {
            long startTime = stack.getOrDefault(ModComponents.POWERED_TIMER.get(), 0L);
            if (level.getGameTime() - startTime > 200) {
                stack.set(ModComponents.IS_POWERED.get(), false);
                if (entity instanceof Player player) {
                    player.displayClientMessage(Component.translatable("item.PanlingRE.ding_hai_shen_zhen.skill.expired"), true);
                }
            }
        }
    }

    @Override
    public long getSkillCD(int skillIndex) {
        return 20000L;
    }

    @Override
    public String getSkillNameKey(int skillIndex) {
        return "item.PanlingRE.ding_hai_shen_zhen.skill1.2";
    }

    @Override
    public float getSkillLingQiCost(int skillIndex) {
        return 50;
    }

    @Override
    public String[] getSkillDescription(int skillIndex) {
        return new String[]{
                "item.PanlingRE.ding_hai_shen_zhen.skill3",
                "item.PanlingRE.ding_hai_shen_zhen.skill4",
                "item.PanlingRE.ding_hai_shen_zhen.skill5"
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

        // 检测Shift键
        if (SafeClientAccess.isShiftPressed()) {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare6"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit0"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ding_hai_shen_zhen.lore1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ding_hai_shen_zhen.lore2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ding_hai_shen_zhen.lore3"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ding_hai_shen_zhen.lore4"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.ding_hai_shen_zhen.skill1.2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ding_hai_shen_zhen.skill2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ding_hai_shen_zhen.skill3"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ding_hai_shen_zhen.skill4"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ding_hai_shen_zhen.skill5"));
        } else {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare6"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit0"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.ding_hai_shen_zhen.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}
