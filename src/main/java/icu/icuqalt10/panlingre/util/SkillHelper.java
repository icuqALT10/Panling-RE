package icu.icuqalt10.panlingre.util;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.FreezeData;
import icu.icuqalt10.panlingre.entity.PanLingEntities;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.network.SyncFreezeDataPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class SkillHelper {

    private static final ResourceLocation FREEZE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "freeze_movement");
    private static final ResourceLocation FREEZE_JUMP_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "freeze_jump");

    /**
     * 获取实体前方一个长方体区域内的所有玩家（精确版本）
     *
     * @param source 施法实体
     * @param width  宽度（左右方向）
     * @param height 高度（上下方向）
     * @param length 长度（前方距离）
     * @return 区域内的玩家列表
     */

    /**
     * 获取实体前方一个长方体区域内的所有生物实体（精确版本）
     */
    public static List<LivingEntity> getLivingEntitiesInFront(LivingEntity source, double width, double height, double length) {
        Level world = source.level();
        if (world.isClientSide) {
            return List.of();
        }

        Vec3 origin = source.position();
        float yaw = source.getYRot();
        double rad = Math.toRadians(yaw);

        Vec3 forward = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
        Vec3 right = new Vec3(Math.cos(rad), 0, Math.sin(rad));
        Vec3 up = new Vec3(0, 1, 0);

        Vec3 center = origin.add(forward.scale(length / 2.0));

        double halfLength = length / 2.0;
        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;

        double maxRadius = Math.sqrt(halfLength * halfLength + halfWidth * halfWidth + halfHeight * halfHeight);
        AABB bounds = new AABB(
                center.x - maxRadius, center.y - maxRadius, center.z - maxRadius,
                center.x + maxRadius, center.y + maxRadius, center.z + maxRadius
        );

        return world.getEntitiesOfClass(LivingEntity.class, bounds).stream()
                .filter(entity -> entity != source)
                .filter(entity -> {
                    Vec3 entityPos = entity.position();
                    Vec3 relativePos = entityPos.subtract(center);

                    double projForward = relativePos.dot(forward);
                    double projRight = relativePos.dot(right);
                    double projUp = relativePos.dot(up);

                    return Math.abs(projForward) <= halfLength
                            && Math.abs(projRight) <= halfWidth
                            && Math.abs(projUp) <= halfHeight;
                })
                .toList();
    }

    /**
     * 冻结指定实体，使其完全无法行动
     *
     * @param entity   要冻结的实体
     * @param duration 冻结持续时间（tick）
     */
    public static void freezeEntity(LivingEntity entity, int duration) {
        if (entity == null || entity.level().isClientSide) {
            return;
        }

        //实体处于无敌 不冻结
        if (entity.isInvulnerable()) return;

        //实体为有AI的生物 且NoAI=false 消除AI
        if (entity instanceof Mob mob) {
            //没有AI 不继续执行
            if (mob.isNoAi()) return;

            mob.setNoAi(true);
        }

        // 使用附件数据存储冻结状态
        FreezeData freezeData = entity.getData(ModAttachments.FREEZE_DATA.get());
        freezeData.setFrozen(true);
        freezeData.setDuration(duration);

        //如果是特殊生物 触发其冻结附加条件
        if (entity instanceof PanLingEntities mob) mob.whenFroozen();

        // 添加属性修改器
        entity.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(
                new AttributeModifier(FREEZE_MODIFIER_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
        );
        entity.getAttribute(Attributes.JUMP_STRENGTH).addTransientModifier(
                new AttributeModifier(FREEZE_JUMP_MODIFIER_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
        );

        // 同步到客户端（追踪者 + 自己）
        if (entity.level() instanceof ServerLevel) {
            var packet = new SyncFreezeDataPayload(entity.getId(), true, duration);
            PacketDistributor.sendToPlayersTrackingEntity(entity, packet);
            if (entity instanceof ServerPlayer sp) {
                PacketDistributor.sendToPlayer(sp, packet);
            }
        }
    }

    /**
     * 解除实体的冻结状态
     */
    public static void unfreezeEntity(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide) {
            return;
        }

        FreezeData freezeData = entity.getData(ModAttachments.FREEZE_DATA.get());
        freezeData.setFrozen(false);
        freezeData.setDuration(0);

        //恢复AI
        if (entity instanceof Mob mob) mob.setNoAi(false);

        //如果是特殊生物 触发其冻结附加条件
        if (entity instanceof PanLingEntities mob) mob.whenUnFroozen();

        // 移除属性修改器
        entity.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(FREEZE_MODIFIER_ID);
        entity.getAttribute(Attributes.JUMP_STRENGTH).removeModifier(FREEZE_JUMP_MODIFIER_ID);

        // 同步到客户端（追踪者 + 自己）
        if (entity.level() instanceof ServerLevel) {
            var packet = new SyncFreezeDataPayload(entity.getId(), false, 0);
            PacketDistributor.sendToPlayersTrackingEntity(entity, packet);
            if (entity instanceof ServerPlayer sp) {
                PacketDistributor.sendToPlayer(sp, packet);
            }
        }
    }

    /**
     * 检查实体是否被冻结
     */
    public static boolean isFrozen(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        FreezeData freezeData = entity.getData(ModAttachments.FREEZE_DATA.get());
        return freezeData.isFrozen();
    }
}