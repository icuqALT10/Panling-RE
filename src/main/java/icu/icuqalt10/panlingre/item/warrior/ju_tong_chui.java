package icu.icuqalt10.panlingre.item.warrior;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.item.skill_trigger;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ju_tong_chui extends Item implements skill_trigger {

    private static final ResourceLocation MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "ju_tong_chui");

    private final int cooldown = 400;
    private final float cost = 20.0f;

    public ju_tong_chui() {
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
                Attributes.ARMOR,
                new AttributeModifier(
                        MODIFIER_ID,
                        10.0,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        builder.add(
                Attributes.MOVEMENT_SPEED,
                new AttributeModifier(
                        MODIFIER_ID,
                        -0.15,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        builder.add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        BASE_ATTACK_DAMAGE_ID,
                        14.0,
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
            int duration = (int) (player.getAttributeValue(Attributes.ARMOR) * 0.5 * 20);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0,false,false,true));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 600, 0,false,false,true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1200, 0,false,false,true));

            //音效
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f,1.0f);
            //播报
            player.displayClientMessage(Component.translatable("item.PanlingRE.ju_tong_chui.skill.success"), true);
            //粒子效果
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        player.getX(), player.getY() + 1, player.getZ(),
                        10, 0.2, 0.2, 0.2, 0.1);
            }
            }

        return true;
    }

    @Override
    public long getSkillCD(int skillIndex) {
        return cooldown * 50L;
    }

    @Override
    public String getSkillNameKey(int skillIndex) {
        return "item.PanlingRE.ju_tong_chui.skill1.2";
    }

    @Override
    public float getSkillLingQiCost(int skillIndex) {
        return cost;
    }

    @Override
    public String[] getSkillDescription(int skillIndex) {
        return new String[]{
                "item.PanlingRE.ju_tong_chui.skill3",
                "item.PanlingRE.ju_tong_chui.skill4"
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltip, TooltipFlag flag) {

        // 检测Shift键
        if (SafeClientAccess.isShiftPressed()) {
            tooltip.add(Component.translatable("item.PanlingRE.lore.rare3"));
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit0"));
            tooltip.add(Component.translatable("item.PanlingRE.ju_tong_chui.lore1"));
            tooltip.add(Component.translatable("item.PanlingRE.ju_tong_chui.lore2"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.ju_tong_chui.skill1.2"));
            tooltip.add(Component.translatable("item.PanlingRE.ju_tong_chui.skill2", cooldown_remove.getCooldownText(SafeClientAccess.getClientPlayer(), cooldown),
                    LingQiData.getCostText(cost)));
            tooltip.add(Component.translatable("item.PanlingRE.ju_tong_chui.skill3"));
            tooltip.add(Component.translatable("item.PanlingRE.ju_tong_chui.skill4"));
        } else {
            tooltip.add(Component.translatable("item.PanlingRE.lore.rare3"));
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit0"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.ju_tong_chui.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }
}
