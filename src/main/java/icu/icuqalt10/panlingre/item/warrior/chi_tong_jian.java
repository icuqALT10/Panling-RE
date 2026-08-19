package icu.icuqalt10.panlingre.item.warrior;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.item.skill_trigger;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import icu.icuqalt10.panlingre.util.SkillHelper;
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
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class chi_tong_jian extends SwordItem implements skill_trigger {

    private static final ResourceLocation MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "chi_tong_jian");

    private final int cooldown = 200;
    private final float cost = 20.0f;

    public chi_tong_jian() {
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
                Attributes.MOVEMENT_SPEED,
                new AttributeModifier(
                        MODIFIER_ID,
                        0.2,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        builder.add(
                Attributes.ARMOR,
                new AttributeModifier(
                        MODIFIER_ID,
                        -10,
                        AttributeModifier.Operation.ADD_VALUE
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
                        -2,
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
                AABB area = player.getBoundingBox().inflate(3.0);
                List<LivingEntity> entities = level.getEntitiesOfClass(
                        LivingEntity.class, area, SkillHelper.combatTargetFilter(player));

                float attack_damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5);

                for (LivingEntity entity : entities) {
                    entity.hurt(player.damageSources().playerAttack(player), attack_damage);
                }
            //音效
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.5f,1.0f);
            //播报
            player.displayClientMessage(Component.translatable("item.PanlingRE.chi_tong_jian.skill.success"), true);
            }

        return true;
    }

    @Override
    public long getSkillCD(int skillIndex) {
        return cooldown * 50L;
    }

    @Override
    public String getSkillNameKey(int skillIndex) {
        return "item.PanlingRE.chi_tong_jian.skill1.2";
    }

    @Override
    public float getSkillLingQiCost(int skillIndex) {
        return cost;
    }

    @Override
    public String[] getSkillDescription(int skillIndex) {
        return new String[]{
                "item.PanlingRE.chi_tong_jian.skill3"
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltip, TooltipFlag flag) {

        // 检测Shift键
        if (SafeClientAccess.isShiftPressed()) {
            tooltip.add(Component.translatable("item.PanlingRE.lore.rare3"));
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit0"));
            tooltip.add(Component.translatable("item.PanlingRE.chi_tong_jian.lore1"));
            tooltip.add(Component.translatable("item.PanlingRE.chi_tong_jian.lore2"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.chi_tong_jian.skill1.2"));
            tooltip.add(Component.translatable("item.PanlingRE.chi_tong_jian.skill2", cooldown_remove.getCooldownText(SafeClientAccess.getClientPlayer(), cooldown),
                    LingQiData.getCostText(cost)));
            tooltip.add(Component.translatable("item.PanlingRE.chi_tong_jian.skill3"));
        } else {
            tooltip.add(Component.translatable("item.PanlingRE.lore.rare3"));
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit0"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.chi_tong_jian.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }
}
