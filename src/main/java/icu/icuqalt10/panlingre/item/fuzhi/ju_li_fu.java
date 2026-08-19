package icu.icuqalt10.panlingre.item.fuzhi;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModAttributes;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ju_li_fu extends Item{

    private static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "ju_li_fu");

    private final int cooldown = 600;
    private final float cost = 15.0f;
    public ju_li_fu() {
        super(
                new Properties()
                        .stacksTo(64)
                        .fireResistant()
        );
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        builder.add(
                ModAttributes.FALIZHI,
                new AttributeModifier(
                        MODIFIER_ID,
                        1.0,
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
        //如果灵气不足
        if (!data.consume(player,cost)) return InteractionResultHolder.fail(itemstack);
        //释放技能
        if (!level.isClientSide) {
            int duration = (int) (5 * player.getAttributeValue(ModAttributes.FALIZHI) * 20);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0,false,false,true));

            //消耗
            itemstack.consume(1, player);
            //cd
            cooldown_remove.cd_remove(player, this, cooldown);
            //音效
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5f,1.0f);
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());

    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltip, TooltipFlag flag) {

        // 检测Shift键
        if (SafeClientAccess.isShiftPressed()) {
            tooltip.add(Component.translatable("item.PanlingRE.lore.rare3"));
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit3"));
            tooltip.add(Component.translatable("item.PanlingRE.ju_li_fu.lore1"));
            tooltip.add(Component.translatable("item.PanlingRE.ju_li_fu.lore2"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.ju_li_fu.skill1.2"));
            tooltip.add(Component.translatable("item.PanlingRE.ju_li_fu.skill2"
                    ,Component.keybind("key.use").withStyle(ChatFormatting.GOLD),
                    cooldown_remove.getCooldownText(SafeClientAccess.getClientPlayer(), cooldown),
                    LingQiData.getCostText(cost)));
            tooltip.add(Component.translatable("item.PanlingRE.ju_li_fu.skill3"));
        } else {
            tooltip.add(Component.translatable("item.PanlingRE.lore.rare3"));
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit3"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.ju_li_fu.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }
}
