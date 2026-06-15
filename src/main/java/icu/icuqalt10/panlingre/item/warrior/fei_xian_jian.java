package icu.icuqalt10.panlingre.item.warrior;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.entity.FeiXianJianZhenEntity;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModEffects;
import icu.icuqalt10.panlingre.init.ModEntities;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

public class fei_xian_jian extends Item {

    public fei_xian_jian() {
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
                        ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "fei_xian_jian"),
                        20,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        builder.add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        BASE_ATTACK_DAMAGE_ID,
                        22.0,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        builder.add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        BASE_ATTACK_SPEED_ID,
                        -1.8,
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
        float cost = 30.0f;
        //如果灵气不足
        if (!data.consume(player,cost)) return InteractionResultHolder.fail(itemstack);
        //释放技能
        if (!level.isClientSide) {
            FeiXianJianZhenEntity entity = new FeiXianJianZhenEntity(ModEntities.FEI_XIAN_JIAN_ZHEN.get(), level);
            entity.moveTo(player.getX(), player.getY(), player.getZ());
            entity.setOwner(player);
            entity.setSummonerArmor((float) player.getArmorValue());

            level.addFreshEntity(entity);

            //cd
            cooldown_remove.cd_remove(player,this,200);
            //音效
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ANVIL_DESTROY, SoundSource.PLAYERS, 0.5f,1.0f);
            //播报
            player.displayClientMessage(Component.translatable("item.PanlingRE.fei_xian_jian.skill.success"), true);
            }

            return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());

    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

        // 检测Shift键
        if (SafeClientAccess.isShiftPressed()) {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare5"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit0"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.fei_xian_jian.lore1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.fei_xian_jian.lore2"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.fei_xian_jian.skill1.2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.fei_xian_jian.skill2"
                    ,Component.keybind("key.use").withStyle(ChatFormatting.GOLD)));
            tooltipComponents.add(Component.translatable("item.PanlingRE.fei_xian_jian.skill3"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.fei_xian_jian.skill4"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.fei_xian_jian.skill5"));
        } else {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare5"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit0"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.fei_xian_jian.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}
