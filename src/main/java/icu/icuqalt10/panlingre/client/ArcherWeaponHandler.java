package icu.icuqalt10.panlingre.client;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.init.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingGetProjectileEvent;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid = PanlingRE.MODID)
public class ArcherWeaponHandler {

    //给玩家自带无限
    @SubscribeEvent
    public static void onGetProjectile(LivingGetProjectileEvent event) {
        if (event.getEntity() instanceof Player player) {
            CuriosApi.getCuriosInventory(player)
                    .flatMap(handler -> handler.findFirstCurio(stack -> stack.is(ModItems.archer.get()))).ifPresent(result -> {
                event.setProjectileItemStack(new ItemStack(Items.ARROW));
            });
        }
    }

    //让箭矢吃箭矢强度  伤害恒定
    @SubscribeEvent
    public static void onArrowJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (event.getEntity() instanceof AbstractArrow arrow) {

            if (arrow.getTags().contains("panlingre:skill_arrow")) return;

            if (arrow.getOwner() instanceof LivingEntity entity) {
                //箭矢不能被捡起
                arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                //计算 设置箭矢伤害
                arrow.setBaseDamage((arrow.getDeltaMovement().length() / 4.5) * (2.0 + entity.getAttributeValue(ModAttributes.ARROW_DAMAGE)));
            }
        }
    }
}