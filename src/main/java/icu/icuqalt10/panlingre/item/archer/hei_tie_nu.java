package icu.icuqalt10.panlingre.item.archer;

import icu.icuqalt10.panlingre.attachment.LingQiData;

import icu.icuqalt10.panlingre.attribute.cooldown_remove;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.item.skill_trigger;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class hei_tie_nu extends HiddenEnchantedCrossbowItem implements skill_trigger {

    private static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "hei_tie_nu");

    private final int cooldown = 10;
    private final float cost = 5.0f;

    public hei_tie_nu() {
        super(
                new Properties()
                        .stacksTo(1)
                        .fireResistant(), 3, 0
        );
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        builder.add(
                ModAttributes.ARROW_DAMAGE,
                new AttributeModifier(
                        MODIFIER_ID,
                        5.0,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );
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
                        -0.1,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        return builder.build();
    }

    //快速装填
    public static int getChargeDuration(ItemStack stack, LivingEntity entity) {
        // 原版公式：基础时间(20tick) - 快速装填等级 * 5
        return 5;
    }

    //技能 skill_1
    @Override
    public boolean skill_use(Level level, Player player, ItemStack stack, int skillIndex) {

        //释放技能
        if (!level.isClientSide) {
            Arrow arrowEntity = new Arrow(level, player, new ItemStack(Items.ARROW), stack);

            arrowEntity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 0.0F);

            arrowEntity.setBaseDamage(
                    (arrowEntity.getDeltaMovement().length() / 4.5) * (2.0 + player.getAttributeValue(ModAttributes.ARROW_DAMAGE) * 0.75));

            arrowEntity.pickup = AbstractArrow.Pickup.DISALLOWED; // 不可拾取
            arrowEntity.setCritArrow(true); // 暴击
            arrowEntity.addTag("panlingre:skill_arrow"); //标记为技能箭

            level.addFreshEntity(arrowEntity);

            //音效
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.5f,1.0f);
            //播报
            player.displayClientMessage(Component.translatable("item.PanlingRE.hei_tie_nu.skill.success"), true);
        }

        return true;
    }

    @Override
    public long getSkillCD(int skillIndex) {
        return cooldown * 50L;
    }

    @Override
    public String getSkillNameKey(int skillIndex) {
        return "item.PanlingRE.hei_tie_nu.skill1.2";
    }

    @Override
    public float getSkillLingQiCost(int skillIndex) {
        return cost;
    }

    @Override
    public String[] getSkillDescription(int skillIndex) {
        return new String[]{
                "item.PanlingRE.hei_tie_nu.skill3"
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltip, TooltipFlag flag) {

        // 检测Shift键
        if (SafeClientAccess.isShiftPressed()) {
            tooltip.add(Component.translatable("item.PanlingRE.lore.rare2"));
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit1"));
            tooltip.add(Component.translatable("item.PanlingRE.hei_tie_nu.lore1"));
            tooltip.add(Component.translatable("item.PanlingRE.hei_tie_nu.lore2"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.hei_tie_nu.skill1.2"));
            tooltip.add(Component.translatable("item.PanlingRE.hei_tie_nu.skill2", cooldown_remove.getCooldownText(SafeClientAccess.getClientPlayer(), cooldown),
                    LingQiData.getCostText(cost)));
            tooltip.add(Component.translatable("item.PanlingRE.hei_tie_nu.skill3"));
        } else {
            tooltip.add(Component.translatable("item.PanlingRE.lore.rare2"));
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit1"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.hei_tie_nu.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }
}
