package icu.icuqalt10.panlingre.instance.baihu;

import icu.icuqalt10.panlingre.instance.InstanceController;
import icu.icuqalt10.panlingre.instance.InstanceResult;
import icu.icuqalt10.panlingre.instance.InstanceSession;
import icu.icuqalt10.panlingre.network.SiShouMusicPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BaihuController implements InstanceController {
    private static final int PREPARATION_TICKS = 5 * 20;
    private static final int HUNT_TICKS = 35 * 20;
    private static final int ESCAPE_TICKS = 45 * 20;
    private static final int SUCCESS_DELAY_TICKS = 2 * 20;
    private static final double GOAL_RADIUS_SQR = 1.5 * 1.5;

    private static final Vec3 BASE_SECOND_STAGE_POS = new Vec3(-13.5, 34.0, 2684.5);
    private static final float SECOND_STAGE_Y_ROT = -155.0F;
    private static final float SECOND_STAGE_X_ROT = -20.0F;
    private static final Vec3 BASE_GOAL_POS = new Vec3(13.5, 96.0, 2691.0);

    private static final List<Vec3> BASE_HUNT_BARRIER_POSITIONS = List.of(
            new Vec3(14.0, 97.0, 2692.0),
            new Vec3(13.0, 97.0, 2692.0),
            new Vec3(13.0, 96.0, 2692.0),
            new Vec3(12.0, 96.0, 2692.0),
            new Vec3(11.0, 97.0, 2691.0),
            new Vec3(11.0, 97.0, 2690.0),
            new Vec3(11.0, 96.0, 2691.0),
            new Vec3(11.0, 96.0, 2690.0),
            new Vec3(12.0, 96.0, 2689.0),
            new Vec3(13.0, 97.0, 2689.0),
            new Vec3(13.0, 96.0, 2689.0),
            new Vec3(14.0, 97.0, 2689.0)
    );

    private static final List<Vec3> BASE_ESCAPE_BARRIER_POSITIONS = List.of(
            new Vec3(-14.0, 36.0, 2684.0),
            new Vec3(-15.0, 35.0, 2684.0),
            new Vec3(-15.0, 34.0, 2684.0),
            new Vec3(-14.0, 35.0, 2683.0),
            new Vec3(-14.0, 34.0, 2683.0),
            new Vec3(-13.0, 35.0, 2684.0),
            new Vec3(-13.0, 34.0, 2684.0),
            new Vec3(-14.0, 35.0, 2685.0),
            new Vec3(-14.0, 34.0, 2685.0)
    );

    private static final List<Vec3> BASE_SKELETON_POSITIONS = List.of(
            new Vec3(9.5, 95.0, 2685.5),
            new Vec3(3.5, 90.0, 2701.5),
            new Vec3(15.5, 76.0, 2695.5),
            new Vec3(-10.5, 65.0, 2685.5),
            new Vec3(3.5, 50.0, 2721.5),
            new Vec3(-0.5, 46.0, 2707.5),
            new Vec3(7.5, 49.0, 2679.5),
            new Vec3(1.5, 40.0, 2677.5)
    );

    private static final CompoundTag SKELETON_NBT = parseNbt("""
            {CanPickUpLoot:false,ArmorDropChances:[0f,0f,0f,0f],HandDropChances:[0f,0f],
            Health:10000000000.0f,Tags:["monster","baihu","skeleton"],CustomNameVisible:1b,
            CustomName:'{"translate":"plre.monster.west.tiger_skeleton"}',DeathLootTable:"plre:empty",
            attributes:[{id:"generic.max_health",base:40},{id:"generic.movement_speed",base:0},
            {id:"generic.minecraft:attack_damage",base:0},{id:"generic.armor",base:0d},
            {id:"generic.follow_range",base:100d},{id:"panlingre:arrow_damage",base:1},{id:"generic.knockback_resistance",base:1d}],
            ArmorItems:[{},{},{},{id:"minecraft:leather_helmet",count:1b,components:{unbreakable:{}}}],
            HandItems:[{id:"minecraft:bow",components:{unbreakable:{},enchantments:{punch:9}}},{}],
            Team:"monster",PersistenceRequired:1b,Invulnerable:1b,Glowing:1b,NoAI:1b}
            """);

    private final List<Skeleton> skeletons = new ArrayList<>();
    private final Map<BlockPos, BlockState> replacedBarrierBlocks = new LinkedHashMap<>();
    private ServerBossEvent bossBar;
    private Phase phase;
    private int remainingTicks;

    @Override
    public void start(InstanceSession session) {
        ServerPlayer player = session.player();
        bossBar = new ServerBossEvent(
                Component.translatable("instance.panlingre.baihu.countdown", 5),
                BossEvent.BossBarColor.WHITE,
                BossEvent.BossBarOverlay.PROGRESS
        );
        if (player != null) {
            bossBar.addPlayer(player);
            PacketDistributor.sendToPlayer(player, new SiShouMusicPayload(true));
        }
        spawnSkeletons(session);
        beginPreparation(session, Phase.FIRST_PREPARATION, "instance.panlingre.baihu.preparing.2");
    }

    @Override
    public void tick(InstanceSession session) {
        if (session.isEnding()) return;
        switch (phase) {
            case FIRST_PREPARATION, SECOND_PREPARATION -> tickPreparation(session);
            case HUNT -> tickHunt(session);
            case ESCAPE -> tickEscape(session);
            case SUCCESS_DELAY -> {
                remainingTicks--;
                if (remainingTicks <= 0) session.finish(InstanceResult.SUCCESS);
            }
        }
    }

    @Override
    public void stop(InstanceSession session, InstanceResult result) {
        if (bossBar != null) bossBar.removeAllPlayers();
        clearMovementBarriers(session.level());
        ServerPlayer player = session.player();
        if (player != null) PacketDistributor.sendToPlayer(player, new SiShouMusicPayload(false));
        for (Skeleton skeleton : skeletons) {
            if (!skeleton.isRemoved()) skeleton.discard();
        }
        skeletons.clear();
    }

    private void beginPreparation(InstanceSession session, Phase preparationPhase, String subtitleKey) {
        phase = preparationPhase;
        remainingTicks = PREPARATION_TICKS;
        placeMovementBarriers(
                session,
                preparationPhase == Phase.FIRST_PREPARATION
                        ? BASE_HUNT_BARRIER_POSITIONS
                        : BASE_ESCAPE_BARRIER_POSITIONS
        );
        ServerPlayer player = session.player();
        if (player != null) {
            player.connection.send(new ClientboundSetTitleTextPacket(
                    Component.translatable("instance.panlingre.baihu.preparing.1")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(subtitleKey)));
        }
        updatePreparationBar();
        playPreparationSound(session);
    }

    private void tickPreparation(InstanceSession session) {
        remainingTicks--;
        updatePreparationBar();
        if (remainingTicks > 0 && remainingTicks % 20 == 0) playPreparationSound(session);
        if (remainingTicks > 0) return;

        clearMovementBarriers(session.level());
        if (phase == Phase.FIRST_PREPARATION) {
            for (Skeleton skeleton : skeletons) {
                if (skeleton.isAlive()) skeleton.setInvulnerable(false);
            }
            phase = Phase.HUNT;
            remainingTicks = HUNT_TICKS;
            updateStageBar();
        } else {
            for (Skeleton skeleton : skeletons) {
                if (skeleton.isAlive()) {
                    skeleton.setInvulnerable(true);
                    skeleton.setNoAi(false);
                }
            }
            phase = Phase.ESCAPE;
            remainingTicks = ESCAPE_TICKS;
            updateStageBar();
        }
    }

    private void tickHunt(InstanceSession session) {
        remainingTicks--;
        updateStageBar();
        if (remainingTicks > 0) return;

        ServerPlayer player = session.player();
        ServerLevel level = session.level();
        if (player == null || level == null) {
            session.finish(InstanceResult.FAILURE);
            return;
        }
        Vec3 target = forSlot(session, BASE_SECOND_STAGE_POS);
        player.teleportTo(level, target.x, target.y, target.z, SECOND_STAGE_Y_ROT, SECOND_STAGE_X_ROT);
        beginPreparation(session, Phase.SECOND_PREPARATION, "instance.panlingre.baihu.preparing.3");
    }

    private void tickEscape(InstanceSession session) {
        ServerPlayer player = session.player();
        if (player == null) {
            session.finish(InstanceResult.FAILURE);
            return;
        }
        if (player.position().distanceToSqr(forSlot(session, BASE_GOAL_POS)) <= GOAL_RADIUS_SQR) {
            beginSuccess(session, player);
            return;
        }
        remainingTicks--;
        updateStageBar();
        if (remainingTicks <= 0) session.finish(InstanceResult.FAILURE);
    }

    private void beginSuccess(InstanceSession session, ServerPlayer player) {
        phase = Phase.SUCCESS_DELAY;
        remainingTicks = SUCCESS_DELAY_TICKS;
        bossBar.setName(Component.translatable("instance.panlingre.baihu.completed"));
        bossBar.setProgress(1.0F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        PacketDistributor.sendToPlayer(player, new SiShouMusicPayload(false));
    }

    private void spawnSkeletons(InstanceSession session) {
        ServerLevel level = session.level();
        if (level == null) return;
        double slotOffset = (double) session.slot() * session.definition().spacing();
        for (Vec3 basePosition : BASE_SKELETON_POSITIONS) {
            Skeleton skeleton = EntityType.SKELETON.create(level);
            if (skeleton == null) continue;
            CompoundTag entityNbt = skeleton.saveWithoutId(new CompoundTag());
            entityNbt.merge(SKELETON_NBT.copy());
            skeleton.load(entityNbt);
            skeleton.setPos(basePosition.add(slotOffset, 0.0, 0.0));
            if (level.addFreshEntity(skeleton)) skeletons.add(skeleton);
        }
    }

    private void placeMovementBarriers(InstanceSession session, List<Vec3> basePositions) {
        ServerLevel level = session.level();
        if (level == null) return;
        clearMovementBarriers(level);
        double slotOffset = (double) session.slot() * session.definition().spacing();
        for (Vec3 basePosition : basePositions) {
            BlockPos position = BlockPos.containing(basePosition.add(slotOffset, 0.0, 0.0));
            replacedBarrierBlocks.put(position, level.getBlockState(position));
            level.setBlock(position, Blocks.BARRIER.defaultBlockState(), 3);
        }
    }

    private void clearMovementBarriers(ServerLevel level) {
        if (level == null || replacedBarrierBlocks.isEmpty()) return;
        replacedBarrierBlocks.forEach((position, state) -> level.setBlock(position, state, 3));
        replacedBarrierBlocks.clear();
    }

    private void updatePreparationBar() {
        int seconds = Math.max(1, (remainingTicks + 19) / 20);
        bossBar.setName(Component.translatable("instance.panlingre.baihu.countdown", seconds));
        bossBar.setProgress(Math.max(0.0F, remainingTicks / (float) PREPARATION_TICKS));
    }

    private void updateStageBar() {
        int seconds = Math.max(0, (remainingTicks + 19) / 20);
        bossBar.setName(Component.translatable("instance.panlingre.baihu.ticks", seconds));
        int duration = phase == Phase.HUNT ? HUNT_TICKS : ESCAPE_TICKS;
        bossBar.setProgress(Math.max(0.0F, remainingTicks / (float) duration));
    }

    private static void playPreparationSound(InstanceSession session) {
        ServerLevel level = session.level();
        if (level == null) return;
        Vec3 center = session.center();
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.HOSTILE, 6.0F, 1.0F);
    }

    private static Vec3 forSlot(InstanceSession session, Vec3 basePosition) {
        return basePosition.add((double) session.slot() * session.definition().spacing(), 0.0, 0.0);
    }

    private static CompoundTag parseNbt(String snbt) {
        try {
            return TagParser.parseTag(snbt);
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid Baihu skeleton SNBT", exception);
        }
    }

    private enum Phase {
        FIRST_PREPARATION,
        HUNT,
        SECOND_PREPARATION,
        ESCAPE,
        SUCCESS_DELAY
    }
}
