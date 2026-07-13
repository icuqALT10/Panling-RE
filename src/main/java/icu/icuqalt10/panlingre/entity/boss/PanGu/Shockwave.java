package icu.icuqalt10.panlingre.entity.boss.PanGu;

import icu.icuqalt10.panlingre.network.ShockwaveUpdatePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
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

    private long blockedMask1;
    private long blockedMask2;

    /**
     * 初始化震动波
     * @param center 发源点绝对坐标
     * @param maxAge 扩散持续的 tick 数（例如 40 tick 代表持续 2 秒）
     */
    public Shockwave(Vec3 center, int maxAge) {
        this.center = center;
        this.maxAge = maxAge;
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

        // ==========================================
        // 第二条路：实际上的判定（数学圆判定 + 中心点到玩家的视线遮挡检查）
        // ==========================================
        // 判定厚度：设为 1.4，略大于每tick扩散的1.2速度，防止玩家因为移动速度过快在tick缝隙中“闪灵”越过判定线
        double thickness = 1.4;
        for (ServerPlayer player : level.players()) {
            // 已经刮过的玩家不再重复吃伤害
            if (this.hitEntities.contains(player.getUUID())) continue;
            // 1. 垂直高度判定（对应原先 AABB 的高度上下限范围）
            double yDiff = player.getY() - this.center.y;
            if (yDiff < -1.0 || yDiff > 2.5) continue;
            // 2. 水平距离判定（真正的数学圆：只看 XZ 轴的直线距离）
            Vec3 centerToPlayerHoriz = new Vec3(player.getX() - this.center.x, 0, player.getZ() - this.center.z);
            double horizontalDist = centerToPlayerHoriz.length();
            // 如果玩家刚好踩在这一 tick 扩散到的圆环厚度内
            if (horizontalDist >= currentRadius - thickness && horizontalDist <= currentRadius + thickness) {
                // 3. 【核心判定】山体视线遮挡检查（从震波圆心拉一条射线到玩家躯干）
                Vec3 startPos = this.center.add(0, 0.5, 0); // 发源点略微抬高半格，防止贴地卡进方块
                Vec3 endPos = new Vec3(player.getX(), player.getY() + 1.0, player.getZ()); // 目标定在玩家的腰部/胸口
                net.minecraft.world.phys.HitResult raycast = level.clip(new net.minecraft.world.level.ClipContext(
                        startPos,
                        endPos,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER, // 检查碰撞箱
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        attacker
                ));
                // 如果射线没有撞到任何方块（MISS），说明中途没有山脉阻挡，玩家暴露在震波中！
                if (raycast.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                    this.hitEntities.add(player.getUUID());
                    // 造成伤害
                    player.hurt(level.damageSources().mobAttack(attacker), 20.0F);
                    // 物理击飞
                    Vec3 launchDir = centerToPlayerHoriz.normalize();
                    player.setDeltaMovement(launchDir.scale(1.5).add(0, 0.5, 0));
                    player.hurtMarked = true; // 强制同步位移
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