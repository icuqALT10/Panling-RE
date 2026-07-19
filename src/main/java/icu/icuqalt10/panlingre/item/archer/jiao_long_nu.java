package icu.icuqalt10.panlingre.item.archer;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.init.ModEffects;
import icu.icuqalt10.panlingre.item.skill_trigger;
import icu.icuqalt10.panlingre.mixin.AbstractArrowMixin;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class jiao_long_nu extends CrossbowItem implements skill_trigger {

    public jiao_long_nu() {
        super(
                new Properties()
                        .stacksTo(1)
                        .fireResistant()
        );
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "jiao_long_nu");

        builder.add(
                ModAttributes.ARROW_DAMAGE,
                new AttributeModifier(
                        UID,
                        14,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );
        builder.add(
                Attributes.MOVEMENT_SPEED,
                new AttributeModifier(
                        UID,
                        0.25,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        return builder.build();
    }

    //技能 skill_1
    @Override
    public boolean skill_use(Level level, Player player, ItemStack stack, int skillIndex) {

        //释放技能
        if (!level.isClientSide) {
            float[] angles = {-10.0F, 0.0F, 10.0F};

            for (float angleOffset : angles) {
                Arrow arrowEntity = new Arrow(level, player, new ItemStack(Items.ARROW), stack);

                arrowEntity.shootFromRotation(player, player.getXRot(), player.getYRot() + angleOffset, 0.0F, 3.0F, 0.0F);

                arrowEntity.setBaseDamage(
                        (arrowEntity.getDeltaMovement().length() / 4.5) * (2.0 + player.getAttributeValue(ModAttributes.ARROW_DAMAGE) * 0.75));

                arrowEntity.pickup = AbstractArrow.Pickup.DISALLOWED;
                arrowEntity.setCritArrow(true);
                arrowEntity.addTag("panlingre:skill_arrow");

                ((AbstractArrowMixin) arrowEntity).invokeSetPierceLevel((byte) 3);

                arrowEntity.addEffect(new MobEffectInstance(ModEffects.po_jia, 200, 1));

                level.addFreshEntity(arrowEntity);
            }

            //音效
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.5f,1.0f);
            //播报
            player.displayClientMessage(Component.translatable("item.PanlingRE.jiao_long_nu.skill.success"), true);
        }

        return true;
    }

    @Override
    public long getSkillCD(int skillIndex) {
        return 500L;
    }

    @Override
    public String getSkillNameKey(int skillIndex) {
        return "item.PanlingRE.jiao_long_nu.skill1.2";
    }

    @Override
    public float getSkillLingQiCost(int skillIndex) {
        return 10;
    }

    @Override
    public String[] getSkillDescription(int skillIndex) {
        return new String[]{
                "item.PanlingRE.jiao_long_nu.skill3",
                "item.PanlingRE.jiao_long_nu.skill4",
                "item.PanlingRE.jiao_long_nu.skill5"
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

        // 检测Shift键
        if (SafeClientAccess.isShiftPressed()) {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare4"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.jiao_long_nu.lore1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.jiao_long_nu.lore2"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.jiao_long_nu.skill1.2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.jiao_long_nu.skill2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.jiao_long_nu.skill3"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.jiao_long_nu.skill4"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.jiao_long_nu.skill5"));
        } else {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare4"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit1"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.jiao_long_nu.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}
