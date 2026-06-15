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
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class hun_yuan_2 extends PotionItem {

    public hun_yuan_2() {
        super(new Properties()
                .stacksTo(64)
                .fireResistant()
                .component(DataComponents.POTION_CONTENTS, new PotionContents(
                        Optional.empty(),
                        Optional.of(14981690),
                        List.of(
                                new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 24000, 0,false,false,true),
                                new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 24000, 0,false,false,true),
                                new MobEffectInstance(MobEffects.ABSORPTION, 24000, 0,false,false,true)
                        ))));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.panlingre.hun_yuan_2");
    }


    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        PotionContents potioncontents = stack.get(DataComponents.POTION_CONTENTS);
        if (potioncontents != null) {
            potioncontents.forEachEffect(entity::addEffect);
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
