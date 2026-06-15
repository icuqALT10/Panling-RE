package icu.icuqalt10.panlingre.item.potions;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class he_ding_dan extends PotionItem {

    public he_ding_dan() {
        super(new Properties()
                .stacksTo(64)
                .fireResistant()
                .component(DataComponents.POTION_CONTENTS, new PotionContents(
                        Optional.empty(),
                        Optional.of(4393481),
                        List.of(
                                new MobEffectInstance(MobEffects.HARM, 1, 9)
                        ))));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.panlingre.he_ding_dan");
    }

    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

        tooltipComponents.add(Component.translatable("item.panlingre.he_ding_dan.lore"));

        super.appendHoverText(stack, context, tooltipComponents, flag);
    }


    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {

        if (!level.isClientSide) {
            entity.kill();
        }

        if (entity instanceof Player player) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        entity.gameEvent(net.minecraft.world.level.gameevent.GameEvent.DRINK);

        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(itemstack);
        }

        return ItemUtils.startUsingInstantly(level, player, hand);
    }
}
