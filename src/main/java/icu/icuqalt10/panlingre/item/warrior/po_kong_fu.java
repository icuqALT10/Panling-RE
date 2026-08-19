package icu.icuqalt10.panlingre.item.warrior;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModEffects;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class po_kong_fu extends Item implements skill_trigger {

    private static final ResourceLocation MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "po_kong_fu");

    private final int cooldown = 100;
    private final float cost = 25.0f;

    public po_kong_fu() {
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
                        24.0,
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
            Vec3 lookVec = player.getLookAngle();

            player.setDeltaMovement(lookVec.x * 1.5, 0, lookVec.z * 1.5);
            player.hurtMarked = true;

            AABB dashArea = player.getBoundingBox().expandTowards(lookVec.scale(4.0)).inflate(1.0);
            List<LivingEntity> targets = level.getEntitiesOfClass(
                    LivingEntity.class, dashArea, SkillHelper.combatTargetFilter(player));

            double baseDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float finalDamage = (float) (baseDamage * 1.25);

            for (LivingEntity living : targets) {
                living.addEffect(new MobEffectInstance(ModEffects.po_jia, 200, 1));
                living.hurt(level.damageSources().playerAttack(player), finalDamage);
                living.knockback(0.5, -lookVec.x, -lookVec.z);
            }

            //音效
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5f,1.0f);
            //播报
            player.displayClientMessage(Component.translatable("item.PanlingRE.po_kong_fu.skill.success"), true);
            }

        return true;
    }

    @Override
    public long getSkillCD(int skillIndex) {
        return cooldown * 50L;
    }

    @Override
    public String getSkillNameKey(int skillIndex) {
        return "item.PanlingRE.po_kong_fu.skill1.2";
    }

    @Override
    public float getSkillLingQiCost(int skillIndex) {
        return cost;
    }

    @Override
    public String[] getSkillDescription(int skillIndex) {
        return new String[]{
                "item.PanlingRE.po_kong_fu.skill3",
                "item.PanlingRE.po_kong_fu.skill4",
                "item.PanlingRE.po_kong_fu.skill5"
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltip, TooltipFlag flag) {

        // 检测Shift键
        if (SafeClientAccess.isShiftPressed()) {
            tooltip.add(Component.translatable("item.PanlingRE.lore.rare4"));
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit0"));
            tooltip.add(Component.translatable("item.PanlingRE.po_kong_fu.lore1"));
            tooltip.add(Component.translatable("item.PanlingRE.po_kong_fu.lore2"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.po_kong_fu.skill1.2"));
            tooltip.add(Component.translatable("item.PanlingRE.po_kong_fu.skill2", cooldown_remove.getCooldownText(SafeClientAccess.getClientPlayer(), cooldown),
                    LingQiData.getCostText(cost)));
            tooltip.add(Component.translatable("item.PanlingRE.po_kong_fu.skill3"));
            tooltip.add(Component.translatable("item.PanlingRE.po_kong_fu.skill4"));
            tooltip.add(Component.translatable("item.PanlingRE.po_kong_fu.skill5"));
        } else {
            tooltip.add(Component.translatable("item.PanlingRE.lore.rare4"));
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit0"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.po_kong_fu.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }
}
