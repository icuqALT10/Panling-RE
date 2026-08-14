package icu.icuqalt10.panlingre.event;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.init.ModItems;
import icu.icuqalt10.panlingre.item.archer.tian_xing_jian;
import icu.icuqalt10.panlingre.network.TianXingTargetPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingGetProjectileEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

@EventBusSubscriber(modid = PanlingRE.MODID)
public class ArcherWeaponHandler {

    //箭矢命中/落地时
    @SubscribeEvent
    public static void onArrowImpact(ProjectileImpactEvent event) {
        Entity projectile = event.getProjectile();
        Level level = projectile.level();

        if (!level.isClientSide && projectile instanceof AbstractArrow arrow) {
            if(arrow.getTags().contains("panlingre:zhong_chui_arrow")) {
                triggerArrowExplosion(level, arrow,10f);
                arrow.discard();
            }
            if(arrow.getTags().contains("panlingre:bei_dou_arrow")) {
                triggerArrowExplosion(level, arrow,5f);
                arrow.discard();
            }
        }
    }
    //箭矢爆炸效果
    private static void triggerArrowExplosion(Level level, AbstractArrow arrow,float multiplied) {

        if (level instanceof ServerLevel serverLevel) {

            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    arrow.getX(), arrow.getY(), arrow.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        double radius = 2.0D;
        AABB area = arrow.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area);

        Entity owner = arrow.getOwner();

        for (LivingEntity target : targets) {

            if (owner != null && (target.is(owner) || (owner instanceof LivingEntity livingOwner && livingOwner.isAlliedTo(target)))) {
                continue;
            }

            float explosionDamage = (float) (arrow.getBaseDamage() * multiplied);
            target.hurt(level.damageSources().explosion(arrow, owner), explosionDamage);
        }
    }

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

            ItemStack firedFromWeapon = arrow.getWeaponItem();
            if (firedFromWeapon != null && firedFromWeapon.getItem() instanceof CrossbowItem) {
                arrow.setCritArrow(false);
            }

            if (arrow.getOwner() instanceof LivingEntity entity) {
                //箭矢不能被捡起
                arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                //计算 设置箭矢伤害
                double damage = (arrow.getDeltaMovement().length() / 4.5)
                        * (2.0 + entity.getAttributeValue(ModAttributes.ARROW_DAMAGE));
                if (entity instanceof ServerPlayer player && hasActiveSniperQuiver(player)) {
                    applySniperTrajectory(player, arrow);
                    tian_xing_jian.notifySniperShot(player);
                }
                arrow.setBaseDamage(damage);
            }
        }
    }

    private static boolean hasActiveSniperQuiver(ServerPlayer player) {
        return tian_xing_jian.isSniperActive(player);
    }

    private static void applySniperTrajectory(ServerPlayer player, AbstractArrow arrow) {
        TianXingTargetPayload.LockedTarget lock = TianXingTargetPayload.getRecent(player);
        if (lock == null) return;

        Entity entity = player.level().getEntity(lock.entityId());
        if (!(entity instanceof LivingEntity target)
                || !tian_xing_jian.isValidSniperTarget(player, target)) return;

        Vec3 eye = player.getEyePosition();
        Vec3 targetPoint = target.getBoundingBox().getCenter();
        Vec3 toTarget = targetPoint.subtract(eye);
        double distance = toTarget.length();
        if (distance > 128.0D || distance < 1.0E-4D
                || player.getLookAngle().normalize().dot(toTarget.normalize())
                < tian_xing_jian.SNIPER_MIN_DOT) {
            return;
        }

        setBallisticVelocity(arrow, targetPoint);
    }

    /**
     * Solves the vanilla arrow's discrete 0.99 drag / 0.05 gravity steps so that its
     * position lands on the selected point after the chosen number of ticks.
     */
    private static void setBallisticVelocity(AbstractArrow arrow, Vec3 targetPoint) {
        Vec3 displacement = targetPoint.subtract(arrow.position());
        double speed = arrow.getDeltaMovement().length();
        if (speed < 1.0E-4D) return;

        int flightTicks = Math.max(1, Math.min(100,
                (int) Math.ceil(displacement.length() / speed)));
        double drag = 0.99D;
        double dragSum = (1.0D - Math.pow(drag, flightTicks)) / (1.0D - drag);
        double gravitySum = 0.0D;
        for (int tick = 1; tick < flightTicks; tick++) {
            gravitySum += (1.0D - Math.pow(drag, tick)) / (1.0D - drag);
        }

        arrow.setDeltaMovement(
                displacement.x / dragSum,
                (displacement.y + 0.05D * gravitySum) / dragSum,
                displacement.z / dragSum);
    }
}
