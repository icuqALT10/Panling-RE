package icu.icuqalt10.panlingre.item.warrior;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModEffects;
import icu.icuqalt10.panlingre.item.skill_trigger;
import icu.icuqalt10.panlingre.network.ShockwaveUpdatePayload;
import icu.icuqalt10.panlingre.network.particle.ParticleLighting;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class yu_ru_yi extends Item implements skill_trigger {

    public yu_ru_yi() {
        super(
                new Properties()
                        .stacksTo(1)
                        .fireResistant()
        );
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        builder.add(
                Attributes.MOVEMENT_SPEED,
                new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "yu_ru_yi"),
                        0.25,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        builder.add(
                Attributes.ARMOR,
                new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "yu_ru_yi"),
                        -15,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        builder.add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        BASE_ATTACK_DAMAGE_ID,
                        29.0,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        builder.add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        BASE_ATTACK_SPEED_ID,
                        -2.6,
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
            double baseDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float finalDamage = (float) (baseDamage * 1.5);

            BlockHitResult hitResult = level.clip(new net.minecraft.world.level.ClipContext(
                    player.getEyePosition(),
                    player.getEyePosition().add(player.getLookAngle().scale(6.0)),
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    player
            ));
            Vec3 targetPos = hitResult.getLocation();

            AABB damageArea = AABB.ofSize(targetPos, 8.0, 8.0, 8.0);
            java.util.List<LivingEntity> targets = level.getEntities(
                    EntityTypeTest.forClass(LivingEntity.class),
                    damageArea,
                    e -> e != player
            );

            for (LivingEntity target : targets) {
                if(target.isAttackable()) {
                    target.addEffect(new MobEffectInstance(ModEffects.po_jia, 200, 3));
                    target.hurt(level.damageSources().playerAttack(player), finalDamage);
                }
            }

            // 4. 局部视觉效果：仅发送给半径16格内的玩家
            if (level instanceof ServerLevel serverLevel) {
                double radiusSq = 16.0 * 16.0;

                //闪电粒子
                ParticleLighting particlePayload = new ParticleLighting(targetPos);

                for (ServerPlayer sp : serverLevel.players()) {
                    if (sp.distanceToSqr(targetPos) <= radiusSq) {
                        //发送闪击粒子
                        PacketDistributor.sendToPlayer(sp, particlePayload);
                    }
                }
            }
            //播报
            player.displayClientMessage(Component.translatable("item.PanlingRE.yu_ru_yi.skill.success"), true);
            }

        return true;

    }

    @Override
    public long getSkillCD(int skillIndex) {
        return 5000L;
    }

    @Override
    public String getSkillNameKey(int skillIndex) {
        return "item.PanlingRE.yu_ru_yi.skill1.2";
    }

    @Override
    public float getSkillLingQiCost(int skillIndex) {
        return 30;
    }

    @Override
    public String[] getSkillDescription(int skillIndex) {
        return new String[]{
                "item.PanlingRE.yu_ru_yi.skill3",
                "item.PanlingRE.yu_ru_yi.skill4",
                "item.PanlingRE.yu_ru_yi.skill5"
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

        // 检测Shift键
        if (SafeClientAccess.isShiftPressed()) {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare5"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit0"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.yu_ru_yi.lore1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.yu_ru_yi.lore2"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.yu_ru_yi.skill1.2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.yu_ru_yi.skill2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.yu_ru_yi.skill3"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.yu_ru_yi.skill4"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.yu_ru_yi.skill5"));
        } else {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare5"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit0"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.yu_ru_yi.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}
