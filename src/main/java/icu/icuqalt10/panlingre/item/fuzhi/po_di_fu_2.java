package icu.icuqalt10.panlingre.item.fuzhi;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.entity.PoDiFuEntity;
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

public class po_di_fu_2 extends Item{

    private static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "po_di_fu_2");

    private final int cooldown = 60;
    private final float cost = 10.0f;
    public po_di_fu_2() {
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
                        2.0,
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

            PoDiFuEntity projectile = new PoDiFuEntity(level, player, 2.5f);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(projectile);

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
            tooltip.add(Component.translatable("item.PanlingRE.lore.rare4"));
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit3"));
            tooltip.add(Component.translatable("item.PanlingRE.po_di_fu_2.lore1"));
            tooltip.add(Component.translatable("item.PanlingRE.po_di_fu_2.lore2"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.po_di_fu_2.skill1.2"));
            tooltip.add(Component.translatable("item.PanlingRE.po_di_fu_2.skill2"
                    ,Component.keybind("key.use").withStyle(ChatFormatting.GOLD),
                    cooldown_remove.getCooldownText(SafeClientAccess.getClientPlayer(), cooldown),
                    LingQiData.getCostText(cost)));
            tooltip.add(Component.translatable("item.PanlingRE.po_di_fu_2.skill3"));
        } else {
            tooltip.add(Component.translatable("item.PanlingRE.lore.rare4"));
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit3"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.po_di_fu_2.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }
}
