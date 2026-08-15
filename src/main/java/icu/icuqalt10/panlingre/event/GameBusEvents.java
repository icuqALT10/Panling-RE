package icu.icuqalt10.panlingre.event;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.entity.FireTrailTracker;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.init.ModEffects;
import icu.icuqalt10.panlingre.looktip.LookTipLoader;
import icu.icuqalt10.panlingre.player.check;
import icu.icuqalt10.panlingre.task.TaskGuideLoader;
import icu.icuqalt10.panlingre.task.TaskGuideService;
import icu.icuqalt10.panlingre.util.Shockwave;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
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

    //LookTip
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new LookTipLoader());
        event.addListener(new TaskGuideLoader());
    }

    //取消踩田
    @SubscribeEvent
    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        event.setCanceled(true);
    }

    //玩家进入服务器
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) {
            player.getData(ModAttachments.LINGQI).sync(player);
        }
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            TaskGuideService.syncActive(serverPlayer);
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
                LingQiData data = player.getData(ModAttachments.LINGQI);
                float max = (float) player.getAttributeValue(ModAttributes.MAX_LINGQI);

                float before = data.getCurrent();

                //每10t恢复（如果没有被冻结）
                if (!player.hasEffect(ModEffects.freeze) && data.getCurrent() < max) {
                    float recovery = (float) player.getAttributeValue(ModAttributes.LING_QI_RECOVERY) * 0.5f;
                    data.setCurrent(data.getCurrent() + max * recovery, player);
                }

                //仅在灵气值实际变化时才同步，避免每10t无意义发包
                if (data.getCurrent() != before) {
                    data.sync(player);
                }
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

        // NoAI mobs do not move, so they need an explicit collision check to press plates beneath them.
        // 这些实体不会移动，无需每 tick 检查，每 20 tick（1秒）扫一次即可。
        if (entity instanceof Mob mob && mob.isNoAi() && !mob.level().isClientSide() && mob.tickCount % 20 == 0) {
            AABB box = mob.getBoundingBox();
            BlockPos min = BlockPos.containing(box.minX + 1.0E-7, box.minY + 1.0E-7, box.minZ + 1.0E-7);
            BlockPos max = BlockPos.containing(box.maxX - 1.0E-7, box.minY + 1.0E-7, box.maxZ - 1.0E-7);

            for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                BlockState state = mob.level().getBlockState(pos);
                if (state.getBlock() instanceof BasePressurePlateBlock) {
                    state.entityInside(mob.level(), pos, mob);
                }
            }
        }

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
                    Entity damageOwner = creeper;
                    if (creeper.getPersistentData().hasUUID("GundileiOwner")) {
                        Entity storedOwner = serverLevel.getEntity(creeper.getPersistentData().getUUID("GundileiOwner"));
                        if (storedOwner != null) {
                            damageOwner = storedOwner;
                        }
                    }

                    for (LivingEntity bumpedentity : bumpedEntyties) {
                        if (bumpedentity == creeper) continue;
                        if (creeper.getTeam() != null && creeper.getTeam() == bumpedentity.getTeam()) continue;

                        // 扣除精确的 10 点爆炸伤害
                        bumpedentity.hurt(serverLevel.damageSources().explosion(creeper, damageOwner), damage);

                        // 计算击飞向量 (由苦力怕指向玩家的方向，给予 XZ 方向冲量，并给予稳定的向上速度)
                        if (!entity.getType().is(CantKnockAway_TAG)) {
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

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (event.getEntity() instanceof Creeper
                && attacker != null
                && attacker.getType().is(EntityTypeTags.SKELETONS)) {
            event.getDrops().removeIf(drop -> drop.getItem().is(ItemTags.CREEPER_DROP_MUSIC_DISCS));
        }
    }

}
