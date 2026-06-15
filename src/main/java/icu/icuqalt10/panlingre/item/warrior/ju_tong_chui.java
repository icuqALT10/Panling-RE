package icu.icuqalt10.panlingre.item.warrior;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.init.ModAttachments;
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

public class ju_tong_chui extends Item {

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
                        ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "ju_tong_chui"),
                        10.0,
                        AttributeModifier.Operation.ADD_VALUE
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
                        -2.4,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        return builder.build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        LingQiData data = player.getData(ModAttachments.LINGQI);
        float cost = 20.0f;
        //如果灵气不足
        if (!data.consume(player,cost)) return InteractionResultHolder.fail(itemstack);
            //释放技能
        if (!level.isClientSide) {
            int duration = (int) (player.getAttributeValue(Attributes.ARMOR) * 0.5 * 20);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0,false,false,true));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 600, 0,false,false,true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1200, 0,false,false,true));

            //cd
            cooldown_remove.cd_remove(player,this,400);
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

            return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());

    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

        // 检测Shift键
        if (SafeClientAccess.isShiftPressed()) {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare3"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit0"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ju_tong_chui.lore1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ju_tong_chui.lore2"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.ju_tong_chui.skill1.2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ju_tong_chui.skill2"
                    ,Component.keybind("key.use").withStyle(ChatFormatting.GOLD)));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ju_tong_chui.skill3"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ju_tong_chui.skill4"));
        } else {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare3"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit0"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.ju_tong_chui.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}
