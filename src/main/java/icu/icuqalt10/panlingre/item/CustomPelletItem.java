package icu.icuqalt10.panlingre.item;

import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.entity.CustomPelletEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public abstract class CustomPelletItem extends Item {

    public CustomPelletItem(Properties properties) {
        super(properties);
    }

    protected abstract String getEffectId();

    protected abstract int getCooldownTicks();

    protected abstract int getParticleColor();

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide) {
            CustomPelletEntity entity = new CustomPelletEntity(level, player);
            entity.setItem(itemstack.copy());

            entity.setColor(this.getParticleColor());

            entity.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.4F, 1.0F);
            level.addFreshEntity(entity);
        }
        //cd
        cooldown_remove.cd_remove(player,this,getCooldownTicks());

        if (!player.getAbilities().instabuild) {
            itemstack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    @Override
    public abstract void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag);
}