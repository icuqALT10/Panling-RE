package icu.icuqalt10.panlingre.item.warrior;

import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.item.skill_trigger;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class tao_mu_jian extends SwordItem implements skill_trigger {

    public tao_mu_jian() {
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
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        BASE_ATTACK_DAMAGE_ID,
                        6.0,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        builder.add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        BASE_ATTACK_SPEED_ID,
                        -2.4,
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
                AABB area = player.getBoundingBox().inflate(5.0);
                List<Entity> entities = level.getEntities(player, area);

                for (Entity entity : entities) {
                    if (entity.isAttackable() && entity.isAlive() && entity.getType() != EntityType.PLAYER) {
                        if (entity instanceof LivingEntity livingEntity) {
                                livingEntity.addEffect(new MobEffectInstance(
                                        MobEffects.MOVEMENT_SLOWDOWN, 100, 1
                                ));
                                //粒子效果
                                if (level instanceof ServerLevel serverLevel) {

                                    serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                                            livingEntity.getX(), livingEntity.getY() + 1, livingEntity.getZ(),
                                            10, 0.2, 0.2, 0.2, 0.1);
                                }
                        }
                    }
                }
            //音效
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.5f,1.0f);
            //播报
            player.displayClientMessage(Component.translatable("item.PanlingRE.tao_mu_jian.skill.success"), true);
            }

        return true;
    }

    @Override
    public long getSkillCD(int skillIndex) {
        return 3000L;
    }

    @Override
    public String getSkillNameKey(int skillIndex) {
        return "item.PanlingRE.tao_mu_jian.skill1.2";
    }

    @Override
    public float getSkillLingQiCost(int skillIndex) {
        return 5;
    }

    @Override
    public String[] getSkillDescription(int skillIndex) {
        return new String[]{
                "item.PanlingRE.tao_mu_jian.skill3"
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

        // 检测Shift键
        if (SafeClientAccess.isShiftPressed()) {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit0"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.tao_mu_jian.lore1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.tao_mu_jian.lore2"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.tao_mu_jian.skill1.2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.tao_mu_jian.skill2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.tao_mu_jian.skill3"));
        } else {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit0"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.tao_mu_jian.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}
