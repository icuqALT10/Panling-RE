package icu.icuqalt10.panlingre.item.potions;

import icu.icuqalt10.panlingre.attribute.cooldown_remove;
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

public class liao_yu_1 extends PotionItem {

    public liao_yu_1() {
        super(new Properties()
                .stacksTo(64)
                .fireResistant()
                .component(DataComponents.POTION_CONTENTS, new PotionContents(
                        Optional.empty(),
                        Optional.of(16711680),
                        List.of(
                                new MobEffectInstance(MobEffects.HEAL, 1, 0,false,false,true)
                        ))));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.panlingre.liao_yu_1");
    }





    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        PotionContents potioncontents = stack.get(DataComponents.POTION_CONTENTS);
        if (potioncontents != null) {
            potioncontents.forEachEffect(entity::addEffect);
        }

        if (entity instanceof Player player) {
            cooldown_remove.cd_remove(player, this, 60);

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
