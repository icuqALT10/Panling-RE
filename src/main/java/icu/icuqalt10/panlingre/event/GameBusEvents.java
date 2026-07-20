package icu.icuqalt10.panlingre.event;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.BlessData;
import icu.icuqalt10.panlingre.attachment.FreezeData;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.entity.FireTrailTracker;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.network.SyncBlessPayload;
import icu.icuqalt10.panlingre.player.check;
import icu.icuqalt10.panlingre.util.Shockwave;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = PanlingRE.MODID)
public class GameBusEvents {

    // ==================== 技能调度器核心数据结构 ====================
    private static final List<RunnableTask> SKILL_TASKS = new ArrayList<>();

    public static void queueTask(int delayTicks, Runnable runnable) {
        SKILL_TASKS.add(new RunnableTask(delayTicks, runnable));
    }

    private static class RunnableTask {
        int ticks;
        Runnable runnable;
        RunnableTask(int ticks, Runnable runnable) {
            this.ticks = ticks;
            this.runnable = runnable;
        }
    }
    // =============================================================

    // ==================== 震动波管理器 ====================
    private static final Map<LivingEntity, List<Shockwave>> ENTITY_SHOCKWAVES = new HashMap<>();

    public static void addShockwave(LivingEntity entity, Shockwave wave) {
        ENTITY_SHOCKWAVES.computeIfAbsent(entity, k -> new ArrayList<>()).add(wave);
    }
    // =============================================================

    // 定义标签常量
    public static final TagKey<Item> WARRIOR_TAG =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "zhiye/warrior"));
    public static final TagKey<Item> ARCHER_TAG =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "zhiye/archer"));
    public static final TagKey<Item> WARLOCK_TAG =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "zhiye/warlock"));
    public static final TagKey<EntityType<?>> CantKnockAway_TAG =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "cant_knockaway"));

    //玩家进入服务器
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) {
            player.getData(ModAttachments.LINGQI).sync(player);
        }
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            BlessData currentData = serverPlayer.getData(ModAttachments.BLESS.get());
            PacketDistributor.sendToPlayer(serverPlayer, new SyncBlessPayload(currentData));
        }
    }

    //玩家重生时
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) {
            player.getData(ModAttachments.LINGQI).sync(player);
        }
    }


    //装备变更 职业限制 手持 防具
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Player player)) return;

        ItemStack newStack = event.getTo();
        if (newStack.isEmpty()) return;

        if (newStack.is(WARRIOR_TAG) && !check.zhiye_check(player, "panlingre:warrior")){
            player.displayClientMessage(Component.translatable("zhiye.cant_use.0"),false);
            player.setItemSlot(event.getSlot(), ItemStack.EMPTY);player.drop(newStack.copy(), true);}

        else if (newStack.is(ARCHER_TAG) && !check.zhiye_check(player, "panlingre:archer")){
            player.displayClientMessage(Component.translatable("zhiye.cant_use.1"),false);
            player.setItemSlot(event.getSlot(), ItemStack.EMPTY);player.drop(newStack.copy(), true);}

        else if (newStack.is(WARLOCK_TAG) && !check.zhiye_check(player, "panlingre:warlock")){
            player.displayClientMessage(Component.translatable("zhiye.cant_use.2"),false);
            player.setItemSlot(event.getSlot(), ItemStack.EMPTY);player.drop(newStack.copy(), true);}
    }
    //饰品栏变更 职业限制 饰品栏
    @SubscribeEvent
    public static void onCurioChange(CurioChangeEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Player player)) return;

        String slotIdentifier = event.getIdentifier();
        ItemStack newStack = event.getTo();
        if (newStack.isEmpty()) return;

        if (newStack.is(WARRIOR_TAG) && !check.zhiye_check(player, "panlingre:warrior")){
            player.displayClientMessage(Component.translatable("zhiye.cant_use.0"), false);
            CuriosApi.getCuriosInventory(player).ifPresent(inv -> {
                inv.setEquippedCurio(slotIdentifier, event.getSlotIndex(), ItemStack.EMPTY);});
            player.drop(newStack.copy(), true);
        }

        else if (newStack.is(ARCHER_TAG) && !check.zhiye_check(player, "panlingre:archer")){
            player.displayClientMessage(Component.translatable("zhiye.cant_use.1"),false);
            CuriosApi.getCuriosInventory(player).ifPresent(inv -> {
                inv.setEquippedCurio(slotIdentifier, event.getSlotIndex(), ItemStack.EMPTY);});
            player.drop(newStack.copy(), true);
        }

        else if (newStack.is(WARLOCK_TAG) && !check.zhiye_check(player, "panlingre:warlock")){
            player.displayClientMessage(Component.translatable("zhiye.cant_use.2"),false);
            CuriosApi.getCuriosInventory(player).ifPresent(inv -> {
                inv.setEquippedCurio(slotIdentifier, event.getSlotIndex(), ItemStack.EMPTY);});
            player.drop(newStack.copy(), true);
        }
    }

    //玩家tick
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide && player.tickCount % 10 == 0) {
                // 检查玩家是否被冻结
                FreezeData freezeData = player.getData(ModAttachments.FREEZE_DATA.get());

                LingQiData data = player.getData(ModAttachments.LINGQI);
                float max = (float) player.getAttributeValue(ModAttributes.MAX_LINGQI);

                //每10刻恢复2.5%（如果没有被冻结）
                if (!freezeData.isFrozen() && data.getCurrent() < max) {
                    data.setCurrent(data.getCurrent() + max * 0.025f, player);
                }

                //同步灵气条
                data.setCurrent(Math.min(data.getCurrent(),max),player);
                data.sync(player);
        }
    }

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

    // 处理 Boss 技能的延迟任务队列机制 + 火焰轨迹跟踪
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // 更新延迟任务队列
        Iterator<RunnableTask> iterator = SKILL_TASKS.iterator();
        while (iterator.hasNext()) {
            RunnableTask task = iterator.next();
            task.ticks--;
            if (task.ticks <= 0) {
                task.runnable.run();
                iterator.remove();
            }
        }

        // 更新服务端火焰轨迹数据
        FireTrailTracker.tick();

        // 更新震动波（来自物品技能等非实体来源）
        if (!ENTITY_SHOCKWAVES.isEmpty()) {
            Iterator<Map.Entry<LivingEntity, List<Shockwave>>> waveIter = ENTITY_SHOCKWAVES.entrySet().iterator();
            while (waveIter.hasNext()) {
                Map.Entry<LivingEntity, List<Shockwave>> entry = waveIter.next();
                LivingEntity entity = entry.getKey();
                List<Shockwave> waves = entry.getValue();
                if (!entity.isAlive() || entity.isRemoved()) {
                    waveIter.remove();
                    continue;
                }
                if (entity.level() instanceof ServerLevel serverLevel) {
                    waves.removeIf(wave -> !wave.tick(serverLevel, entity));
                }
                if (waves.isEmpty()) {
                    waveIter.remove();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();

        // 处理“滚地雷”苦力怕的 3秒生命周期、玩家碰撞检测、击飞与精准伤害
        if (entity instanceof Creeper creeper && !creeper.level().isClientSide()) {
            // 检查是否带有滚地雷标签
            if (creeper.getPersistentData().contains("GundileiTicks")) {
                int remainingTicks = creeper.getPersistentData().getInt("GundileiTicks") - 1;

                // 1. 超过 3 秒直接凭空消失 (discard 不触发任何死亡动画和掉落)
                if (remainingTicks <= 0) {
                    creeper.discard();
                    return;
                }
                creeper.getPersistentData().putInt("GundileiTicks", remainingTicks);

                // 2. 检测周围是否有碰到的玩家 (碰撞箱略微放大 0.3 格做碰撞区域)
                List<LivingEntity> bumpedEntyties = creeper.level().getEntitiesOfClass(
                        LivingEntity.class,
                        creeper.getBoundingBox().inflate(0.3)
                );

                if (!bumpedEntyties.isEmpty()) {
                    float damage = creeper.getPersistentData().getInt("GundileiDamage");
                    boolean SuccessCheck = false;
                    ServerLevel serverLevel = (ServerLevel) creeper.level();

                    for (LivingEntity bumpedentity : bumpedEntyties) {
                        if (bumpedentity == creeper) continue;
                        if (creeper.getTeam() != null && creeper.getTeam() == bumpedentity.getTeam()) continue;

                        // 扣除精确的 10 点爆炸伤害
                        bumpedentity.hurt(serverLevel.damageSources().explosion(creeper, creeper), damage);

                        // 计算击飞向量 (由苦力怕指向玩家的方向，给予 XZ 方向冲量，并给予稳定的向上速度)
                        if (entity.getType().is(CantKnockAway_TAG)) {
                            Vec3 moveDirection = bumpedentity.position().subtract(creeper.position()).normalize().scale(1.4);
                            bumpedentity.setDeltaMovement(moveDirection.x, 0.65, moveDirection.z);
                            bumpedentity.hurtMarked = true;
                        }

                        SuccessCheck = true;
                    }

                    if (!SuccessCheck) return;

                    // 纯视觉和声音的爆炸效果（不会真正破坏地形，也不会产生原版的窒息/火焰伤害）
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, creeper.getX(), creeper.getY(), creeper.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                    serverLevel.playSound(null, creeper.getX(), creeper.getY(), creeper.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0F, 1.0F);

                    // 触发爆炸后滚地雷立刻退场
                    creeper.discard();
                }
            }
        }
    }

}