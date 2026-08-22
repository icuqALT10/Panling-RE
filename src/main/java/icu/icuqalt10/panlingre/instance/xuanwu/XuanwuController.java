package icu.icuqalt10.panlingre.instance.xuanwu;

import icu.icuqalt10.panlingre.instance.InstanceController;
import icu.icuqalt10.panlingre.instance.InstanceResult;
import icu.icuqalt10.panlingre.instance.InstanceSession;
import icu.icuqalt10.panlingre.network.SiShouMusicPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class XuanwuController implements InstanceController {
    private static final int PREPARATION_TICKS = 5 * 20;
    private static final int COMBAT_TICKS = 20 * 20;
    private static final int COLLAPSE_TICKS = 60 * 20;
    private static final int WAVE_INTERVAL_TICKS = 5 * 20;
    private static final int COBBLESTONE_BREAK_TICKS = 10;
    private static final int SUCCESS_DELAY_TICKS = 2 * 20;

    /** Slot 0 positions: spiders x2, cave spiders x2, then two diagonal skeleton pairs. */
    private static final List<Vec3> BASE_MONSTER_POSITIONS = List.of(
            new Vec3(0.0, 61.0, 2992.0),
            new Vec3(0.0, 61.0, 3008.0),
            new Vec3(9.0, 61.0, 3000.0),
            new Vec3(-9.0, 61.0, 3000.0),
            new Vec3(6.0, 61.0, 3006.0),
            new Vec3(-6.0, 61.0, 2994.0),
            new Vec3(6.0, 61.0, 2994.0),
            new Vec3(-6.0, 61.0, 3006.0)
    );

    private static final CompoundTag SPIDER_NBT = parseNbt("""
            {CanPickUpLoot:false,ArmorDropChances:[0f,0f,0f,0f],HandDropChances:[0f,0f],
            Health:10000000000.0f,Tags:["monster","instance","xuanwu"],CustomNameVisible:1b,
            CustomName:'{"translate":"plre.monster.north.xuanwu"}',DeathLootTable:"empty",
            attributes:[{id:"generic.max_health",base:200},{id:"generic.movement_speed",base:0.2},
            {id:"generic.minecraft:attack_damage",base:15},{id:"generic.armor",base:0d},
            {id:"generic.follow_range",base:40d}],ArmorItems:[{},{},{},{}],HandItems:[{},{}],
            Team:"monster",PersistenceRequired:1b}
            """);

    private static final CompoundTag CAVE_SPIDER_NBT = parseNbt("""
            {CanPickUpLoot:false,ArmorDropChances:[0f,0f,0f,0f],HandDropChances:[0f,0f],
            Health:10000000000.0f,Tags:["monster","instance","xuanwu"],CustomNameVisible:1b,
            CustomName:'{"translate":"plre.monster.north.xuanwu"}',DeathLootTable:"empty",
            attributes:[{id:"generic.max_health",base:180},{id:"generic.movement_speed",base:0.3},
            {id:"generic.minecraft:attack_damage",base:12},{id:"generic.armor",base:0d},
            {id:"generic.follow_range",base:40d}],ArmorItems:[{},{},{},{}],HandItems:[{},{}],
            Team:"monster",PersistenceRequired:1b}
            """);

    private static final CompoundTag SKELETON_NBT = parseNbt("""
            {CanPickUpLoot:false,ArmorDropChances:[0f,0f,0f,0f],HandDropChances:[0f,0f],
            Health:10000000000.0f,Tags:["monster","instance","xuanwu"],CustomNameVisible:1b,
            CustomName:'{"translate":"plre.monster.north.xuanwu"}',DeathLootTable:"empty",
            attributes:[{id:"generic.max_health",base:150},{id:"generic.movement_speed",base:0.2},
            {id:"generic.minecraft:attack_damage",base:0},{id:"generic.armor",base:0d},
            {id:"generic.follow_range",base:40d},{id:"panlingre:arrow_damage",base:8}],
            ArmorItems:[{},{},{},{id:"minecraft:leather_helmet",count:1b,components:{unbreakable:{}}}],
            HandItems:[{id:"minecraft:bow",components:{unbreakable:{},enchantments:{punch:1}}},{}],
            Team:"monster",PersistenceRequired:1b}
            """);

    private final List<Mob> spawnedMonsters = new ArrayList<>();
    private final Map<BlockPos, BlockState> removedFloor = new LinkedHashMap<>();
    private final Map<BlockPos, PendingBreak> pendingBreaks = new LinkedHashMap<>();
    private ServerBossEvent bossBar;
    private Phase phase;
    private int remainingTicks;
    private int waveTicks;
    private int waveStep;
    private int nextBreakAnimationId = Integer.MIN_VALUE;

    @Override
    public void start(InstanceSession session) {
        ServerPlayer player = session.player();
        bossBar = new ServerBossEvent(
                Component.translatable("instance.panlingre.xuanwu.countdown", 5),
                BossEvent.BossBarColor.BLUE,
                BossEvent.BossBarOverlay.PROGRESS
        );
        if (player != null) {
            player.connection.send(new ClientboundSetTitleTextPacket(
                    Component.translatable("instance.panlingre.xuanwu.preparing.1")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.translatable("instance.panlingre.xuanwu.preparing.2")));
            bossBar.addPlayer(player);
            PacketDistributor.sendToPlayer(player, new SiShouMusicPayload(true));
        }
        phase = Phase.PREPARATION;
        remainingTicks = PREPARATION_TICKS;
        updatePreparationBar();
        playPreparationSound(session);
    }

    @Override
    public void tick(InstanceSession session) {
        if (session.isEnding()) return;
        switch (phase) {
            case PREPARATION -> tickPreparation(session);
            case COMBAT -> tickCombat(session);
            case COLLAPSE_PREPARATION -> tickCollapsePreparation(session);
            case COLLAPSE -> tickCollapse(session);
            case SUCCESS_DELAY -> {
                remainingTicks--;
                if (remainingTicks <= 0) session.finish(InstanceResult.SUCCESS);
            }
        }
    }

    @Override
    public void stop(InstanceSession session, InstanceResult result) {
        if (bossBar != null) bossBar.removeAllPlayers();
        restoreFloor(session);
        ServerPlayer player = session.player();
        if (player != null) PacketDistributor.sendToPlayer(player, new SiShouMusicPayload(false));
        for (Mob mob : spawnedMonsters) {
            if (!mob.isRemoved()) mob.discard();
        }
        spawnedMonsters.clear();
    }

    private void tickPreparation(InstanceSession session) {
        remainingTicks--;
        updatePreparationBar();
        if (remainingTicks % 20 == 0) playPreparationSound(session);
        if (remainingTicks <= 0) beginCombat(session);
    }

    private void beginCombat(InstanceSession session) {
        phase = Phase.COMBAT;
        remainingTicks = COMBAT_TICKS;
        waveTicks = WAVE_INTERVAL_TICKS;
        waveStep = 0;
        updateStageBar();
        spawnWave(session);
    }

    private void tickCombat(InstanceSession session) {
        remainingTicks--;
        updateStageBar();
        if (remainingTicks <= 0) {
            beginCollapsePreparation(session);
            return;
        }
        if (waveStep < 4 && --waveTicks <= 0) {
            spawnWave(session);
            waveTicks = WAVE_INTERVAL_TICKS;
        }
    }

    private void beginCollapsePreparation(InstanceSession session) {
        phase = Phase.COLLAPSE_PREPARATION;
        remainingTicks = PREPARATION_TICKS;
        for (Mob mob : spawnedMonsters) {
            if (!mob.isAlive()) continue;
            var followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
            if (followRange != null) followRange.setBaseValue(0.0);
            removeFloorUnderMob(session, mob);
        }
        ServerPlayer player = session.player();
        if (player != null) {
            player.connection.send(new ClientboundSetTitleTextPacket(
                    Component.translatable("instance.panlingre.xuanwu.preparing.1")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.translatable("instance.panlingre.xuanwu.preparing.3")));
        }
        updatePreparationBar();
        playPreparationSound(session);
    }

    private void tickCollapsePreparation(InstanceSession session) {
        tickPendingBreaks(session);
        scheduleCobblestonesBelowMonsters(session);
        remainingTicks--;
        updatePreparationBar();
        if (remainingTicks > 0 && remainingTicks % 20 == 0) playPreparationSound(session);
        if (remainingTicks <= 0) beginCollapse();
    }

    private void beginCollapse() {
        phase = Phase.COLLAPSE;
        remainingTicks = COLLAPSE_TICKS;
        updateStageBar();
    }

    private void tickCollapse(InstanceSession session) {
        tickPendingBreaks(session);
        scheduleCobblestonesBelowMonsters(session);
        scheduleCobblestoneBelowPlayer(session);
        remainingTicks--;
        updateStageBar();
        if (remainingTicks <= 0) beginSuccess(session);
    }

    private void beginSuccess(InstanceSession session) {
        restoreFloor(session);
        phase = Phase.SUCCESS_DELAY;
        remainingTicks = SUCCESS_DELAY_TICKS;
        bossBar.setName(Component.translatable("instance.panlingre.xuanwu.completed"));
        bossBar.setProgress(1.0F);
        ServerPlayer player = session.player();
        if (player != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            PacketDistributor.sendToPlayer(player, new SiShouMusicPayload(false));
        }
    }

    private void spawnWave(InstanceSession session) {
        int firstIndex;
        int count;
        if (waveStep == 0) {
            firstIndex = 0;
            count = 2;
        } else if (waveStep == 1) {
            firstIndex = 2;
            count = 2;
        } else {
            firstIndex = waveStep == 2 ? 4 : 6;
            count = 2;
        }
        for (int index = firstIndex; index < firstIndex + count; index++) {
            spawnMonster(session, index);
        }
        waveStep++;
    }

    private void spawnMonster(InstanceSession session, int index) {
        ServerLevel level = session.level();
        if (level == null) return;
        EntityType<? extends Mob> type = index < 2
                ? EntityType.SPIDER
                : index < 4 ? EntityType.CAVE_SPIDER : EntityType.SKELETON;
        Mob mob = type.create(level);
        if (mob == null) return;
        CompoundTag entityNbt = mob.saveWithoutId(new CompoundTag());
        entityNbt.merge(nbtFor(index).copy());
        mob.load(entityNbt);
        double slotOffset = (double) session.slot() * session.definition().spacing();
        mob.setPos(BASE_MONSTER_POSITIONS.get(index).add(slotOffset, 0.0, 0.0));
        if (level.addFreshEntity(mob)) spawnedMonsters.add(mob);
    }

    private void removeFloorUnderMob(InstanceSession session, Mob mob) {
        removeFloorBlock(session.level(), mob.blockPosition().below());
    }

    private void scheduleCobblestoneBelowPlayer(InstanceSession session) {
        ServerPlayer player = session.player();
        ServerLevel level = session.level();
        if (player == null || level == null) return;
        scheduleCobblestones(session, player.getBoundingBox(), player.getY());
    }

    private void scheduleCobblestonesBelowMonsters(InstanceSession session) {
        for (Mob mob : spawnedMonsters) {
            if (mob.isAlive()) scheduleCobblestones(session, mob.getBoundingBox(), mob.getY());
        }
    }

    private void scheduleCobblestones(InstanceSession session, AABB bounds, double feetY) {
        ServerPlayer player = session.player();
        ServerLevel level = session.level();
        if (level == null) return;
        int minX = (int) Math.floor(bounds.minX + 1.0E-4);
        int maxX = (int) Math.floor(bounds.maxX - 1.0E-4);
        int minZ = (int) Math.floor(bounds.minZ + 1.0E-4);
        int maxZ = (int) Math.floor(bounds.maxZ - 1.0E-4);
        int y = (int) Math.floor(feetY - 0.01);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos position = new BlockPos(x, y, z);
                if (!level.getBlockState(position).is(Blocks.COBBLESTONE)
                        || pendingBreaks.containsKey(position)) continue;
                int animationId = nextBreakAnimationId++;
                pendingBreaks.put(position, new PendingBreak(animationId, COBBLESTONE_BREAK_TICKS));
                if (player != null) {
                    player.connection.send(new ClientboundBlockDestructionPacket(animationId, position, 0));
                }
            }
        }
    }

    private void tickPendingBreaks(InstanceSession session) {
        ServerPlayer player = session.player();
        ServerLevel level = session.level();
        if (level == null) return;
        Iterator<Map.Entry<BlockPos, PendingBreak>> iterator = pendingBreaks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, PendingBreak> entry = iterator.next();
            PendingBreak pending = entry.getValue();
            pending.remainingTicks--;
            if (pending.remainingTicks <= 0) {
                if (player != null) {
                    player.connection.send(new ClientboundBlockDestructionPacket(
                            pending.animationId, entry.getKey(), -1));
                }
                removeFloorBlock(level, entry.getKey());
                iterator.remove();
            } else if (player != null) {
                int progress = Math.min(9, COBBLESTONE_BREAK_TICKS - pending.remainingTicks);
                player.connection.send(new ClientboundBlockDestructionPacket(
                        pending.animationId, entry.getKey(), progress));
            }
        }
    }

    private void removeFloorBlock(ServerLevel level, BlockPos position) {
        if (level == null || level.getBlockState(position).isAir()) return;
        BlockState removedState = level.getBlockState(position);
        removedFloor.putIfAbsent(position.immutable(), removedState);
        level.setBlock(position, Blocks.AIR.defaultBlockState(), 3);
        if (removedState.is(Blocks.COBBLESTONE)) {
            level.levelEvent(2001, position, Block.getId(removedState));
        }
    }

    private void restoreFloor(InstanceSession session) {
        ServerLevel level = session.level();
        ServerPlayer player = session.player();
        if (player != null) {
            pendingBreaks.forEach((position, pending) -> player.connection.send(
                    new ClientboundBlockDestructionPacket(pending.animationId, position, -1)));
        }
        pendingBreaks.clear();
        if (level != null) {
            removedFloor.forEach((position, state) -> level.setBlock(position, state, 3));
        }
        removedFloor.clear();
    }

    private void updatePreparationBar() {
        int seconds = Math.max(1, (remainingTicks + 19) / 20);
        bossBar.setName(Component.translatable("instance.panlingre.xuanwu.countdown", seconds));
        bossBar.setProgress(Math.max(0.0F, remainingTicks / (float) PREPARATION_TICKS));
    }

    private void updateStageBar() {
        int seconds = Math.max(0, (remainingTicks + 19) / 20);
        bossBar.setName(Component.translatable("instance.panlingre.xuanwu.ticks", seconds));
        int duration = phase == Phase.COMBAT ? COMBAT_TICKS : COLLAPSE_TICKS;
        bossBar.setProgress(Math.max(0.0F, remainingTicks / (float) duration));
    }

    private static void playPreparationSound(InstanceSession session) {
        ServerLevel level = session.level();
        if (level == null) return;
        Vec3 center = session.center();
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    private static CompoundTag nbtFor(int index) {
        if (index < 2) return SPIDER_NBT;
        if (index < 4) return CAVE_SPIDER_NBT;
        return SKELETON_NBT;
    }

    private static CompoundTag parseNbt(String snbt) {
        try {
            return TagParser.parseTag(snbt);
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid Xuanwu monster SNBT", exception);
        }
    }

    private enum Phase {
        PREPARATION,
        COMBAT,
        COLLAPSE_PREPARATION,
        COLLAPSE,
        SUCCESS_DELAY
    }

    private static final class PendingBreak {
        private final int animationId;
        private int remainingTicks;

        private PendingBreak(int animationId, int remainingTicks) {
            this.animationId = animationId;
            this.remainingTicks = remainingTicks;
        }
    }
}
