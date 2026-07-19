package icu.icuqalt10.panlingre.util;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.network.ShockwaveUpdatePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Shockwave {
    private final Vec3 center;                  // 震波发源点
    private final List<Ray> rays = new ArrayList<>(); // 36条射线
    private final Set<UUID> hitEntities = new HashSet<>(); // 受击免疫标记
    private final int maxAge;                   // 震波最大存活时间（tick）
    private int age = 0;                        // 当前存活时间

    private PlayerTeam team = null;
    private float damage = 0;

    private long blockedMask1;
    private long blockedMask2;

    // 获取boss列表 对boss不击飞
    public static final TagKey<EntityType<?>> BOSS_TAG =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "boss"));

    /**
     * 初始化震动波
     * @param center 发源点绝对坐标
     * @param maxAge 扩散持续的 tick 数（例如 40 tick 代表持续 2 秒）
     */
    public Shockwave(Vec3 center, int maxAge, float damage) {
        this.center = center;
        this.maxAge = maxAge;
        this.damage = damage;
        // 围绕中心点，每隔 5 度生成一条水平方向的单位向量射线
        for (int angle = 0; angle < 360; angle += 5) {
            double rad = Math.toRadians(angle);
            Vec3 dir = new Vec3(Math.cos(rad), 0, Math.sin(rad));
            rays.add(new Ray(dir));
        }
    }
    public Shockwave(Vec3 center, int maxAge, float damage, PlayerTeam team) {
        this.center = center;
        this.maxAge = maxAge;
        this.damage = damage;
        if (team != null) {
            this.team = team;
        }
        // 围绕中心点，每隔 5 度生成一条水平方向的单位向量射线
        for (int angle = 0; angle < 360; angle += 5) {
            double rad = Math.toRadians(angle);
            Vec3 dir = new Vec3(Math.cos(rad), 0, Math.sin(rad));
            rays.add(new Ray(dir));
        }
    }
    /**
     * 震动波的核心驱动方法。完全自己管自己。
     * @param level 服务端世界
     * @param attacker 释放此技能的实体（用于判定伤害来源）
     * @return true 如果震动波依然存活；false 如果震动波已结束，外部应该将其从列表中移除
     */
    public boolean tick(ServerLevel level, LivingEntity attacker) {
        this.age++;
        if (this.age >= this.maxAge) {
            return false; // 到时间了，自我销毁
        }
        // 扩散波速：每 tick 扩散 1.2 格
        double currentRadius = this.age * 1.2;
        // ==========================================
        // 第一条路：负责粒子的视觉提示（保留手电筒山体阴影）
        // ==========================================
        for (Ray ray : this.rays) {
            if (ray.isBlocked) continue;
            Vec3 rayPos = this.center.add(ray.direction.scale(currentRadius));
            BlockPos blockPos = BlockPos.containing(rayPos);
            if(level.getBlockState(blockPos).isSolidRender(level, blockPos)){
                ray.isBlocked=true;
            }
        }
// 循环结束以后发送一次
        updateBlockedMask();

        PacketDistributor.sendToPlayersTrackingEntity(
                attacker,
                new ShockwaveUpdatePayload(
                        center,
                        age,
                        blockedMask1,
                        blockedMask2
                ));
        // 如果释放者是玩家，玩家自己不追踪自己，所以需要额外发包给释放者
        if (attacker instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp,
                    new ShockwaveUpdatePayload(center, age, blockedMask1, blockedMask2));
        }

        // ==========================================
        // 第二条路：实际上的判定（数学圆判定 + 中心点到实体的视线遮挡检查）
        // ==========================================
        double thickness = 1.4;
        double innerR = currentRadius - thickness;
        double outerR = currentRadius + thickness;
        AABB aabb = new AABB(
                this.center.x - outerR, this.center.y - 2.0, this.center.z - outerR,
                this.center.x + outerR, this.center.y + 3.5, this.center.z + outerR
        );
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, aabb)) {
            if (this.hitEntities.contains(entity.getUUID())) continue;
            if (this.team != null && entity.getTeam() == this.team) continue;
            double yDiff = entity.getY() - this.center.y;
            if (yDiff < -1.0 || yDiff > 2.5) continue;
            Vec3 centerToEntityHoriz = new Vec3(entity.getX() - this.center.x, 0, entity.getZ() - this.center.z);
            double horizontalDist = centerToEntityHoriz.length();
            if (horizontalDist >= innerR && horizontalDist <= outerR) {
                // 3. 山体视线遮挡检查
                Vec3 startPos = this.center.add(0, 0.5, 0);
                Vec3 endPos = new Vec3(entity.getX(), entity.getY() + 1.0, entity.getZ());
                HitResult raycast = level.clip(new ClipContext(
                        startPos,
                        endPos,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        attacker
                ));
                if (raycast.getType() == HitResult.Type.MISS) {
                    this.hitEntities.add(entity.getUUID());
                    entity.hurt(level.damageSources().mobAttack(attacker), this.damage);
                    if (!entity.getType().is(BOSS_TAG)) {
                        Vec3 launchDir = centerToEntityHoriz.normalize();
                        entity.setDeltaMovement(launchDir.scale(1.5).add(0, 0.5, 0));
                        entity.hurtMarked = true;
                    }
                }
            }
        }
        return true; // 还没到期，继续存活
    }

    private void updateBlockedMask(){

        blockedMask1 = 0;
        blockedMask2 = 0;


        for(int i=0;i<rays.size();i++){

            if(!rays.get(i).isBlocked)
                continue;


            if(i < 64){

                blockedMask1 |= (1L << i);

            }else{

                blockedMask2 |= (1L << (i-64));

            }
        }
    }

    /**
     * 内部私有类，用于存放单条射线的数据
     */
    private static class Ray {
        public final Vec3 direction;
        public boolean isBlocked = false;
        public Ray(Vec3 direction) {
            this.direction = direction;
        }
    }
}