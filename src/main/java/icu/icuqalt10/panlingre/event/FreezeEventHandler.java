package icu.icuqalt10.panlingre.event;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.FreezeData;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.util.SkillHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = PanlingRE.MODID)
public class FreezeEventHandler {

    /**
     * 每tick更新冻结状态
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity entity && !entity.level().isClientSide) {
            FreezeData freezeData = entity.getData(ModAttachments.FREEZE_DATA.get());

            if (freezeData.isFrozen()) {
                freezeData.tick();

                // 时间到了自动解冻
                if (!freezeData.isFrozen()) {
                    SkillHelper.unfreezeEntity(entity);
                }
            }
        }
    }

    /**
     * 阻止被冻结的实体造成伤害
     */
    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        // 检查伤害来源是否是被冻结的实体
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            FreezeData freezeData = attacker.getData(ModAttachments.FREEZE_DATA.get());
            if (freezeData.isFrozen()) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * 阻止玩家在冻结状态下攻击实体
     */
    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        FreezeData freezeData = player.getData(ModAttachments.FREEZE_DATA.get());
        if (freezeData.isFrozen()) {
            event.setCanceled(true);
        }
    }

    /**
     * 阻止被冻结的玩家使用物品（右键）
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        FreezeData freezeData = player.getData(ModAttachments.FREEZE_DATA.get());
        if (freezeData.isFrozen()) {
            event.setCanceled(true);
        }
    }

    /**
     * 阻止被冻结的玩家使用物品（右键方块）
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        FreezeData freezeData = player.getData(ModAttachments.FREEZE_DATA.get());
        if (freezeData.isFrozen()) {
            event.setCanceled(true);
        }
    }

    /**
     * 阻止被冻结的玩家左键点击
     */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        FreezeData freezeData = player.getData(ModAttachments.FREEZE_DATA.get());
        if (freezeData.isFrozen()) {
            event.setCanceled(true);
        }
    }

    /**
     * 阻止被冻结的实体跳跃
     */
    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        FreezeData freezeData = entity.getData(ModAttachments.FREEZE_DATA.get());
        if (freezeData.isFrozen()) {
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0, 0.0, 1.0));
        }
    }
}