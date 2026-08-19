package icu.icuqalt10.panlingre.instance.zhuque;

import icu.icuqalt10.panlingre.entity.HuoQiuFuEntity;
import icu.icuqalt10.panlingre.instance.InstanceController;
import icu.icuqalt10.panlingre.instance.InstanceResult;
import icu.icuqalt10.panlingre.instance.InstanceSession;
import icu.icuqalt10.panlingre.network.SiShouMusicPayload;
import icu.icuqalt10.panlingre.network.particle.ZhuqueMeteorWarningPayload;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ZhuqueController implements InstanceController {
    private static final int START_COUNTDOWN_TICKS = 5 * 20;
    private static final int SURVIVAL_TICKS = 180 * 20;
    private static final int WAVE_INTERVAL_TICKS = 5 * 20;
    private static final int METEOR_INTERVAL_TICKS = 5 * 20;
    private static final int TWO_METEORS_REMAINING_TICKS = 90 * 20;
    private static final int THREE_METEORS_REMAINING_TICKS = 30 * 20;
    private static final int MONSTER_OVERLOAD_LIMIT = 20;
    private static final float OVERLOAD_FIRE_DAMAGE = 30.0F;
    private static final int OVERLOAD_FIRE_SECONDS = 20;
    private static final float METEOR_DAMAGE = 5.0F;
    private static final double METEOR_SPAWN_Y = 80.0;
    private static final double METEOR_INITIAL_VELOCITY = -0.25;
    private static final int METEOR_RANDOM_RADIUS = 13;
    private static final float METEOR_WARNING_RADIUS = 4.0F;

    /** Slot 0 gameplay center. Other slots add slot * spacing to X. */
    private static final Vec3 BASE_CENTER = new Vec3(0.0, 62.0, 2250.0);

    /** Slot 0 spawn positions, ordered as four pairs in the repeating spawn sequence. */
    private static final List<Vec3> BASE_MONSTER_POSITIONS = List.of(
            new Vec3(12.0, 61.0, 2250.0),
            new Vec3(-12.0, 61.0, 2250.0),
            new Vec3(0.0, 61.0, 2262.0),
            new Vec3(0.0, 61.0, 2238.0),
            new Vec3(9.0, 61.0, 2259.0),
            new Vec3(9.0, 61.0, 2241.0),
            new Vec3(-9.0, 61.0, 2259.0),
            new Vec3(-9.0, 61.0, 2241.0)
    );

    private static final List<EntityType<? extends Mob>> MONSTER_TYPES = List.of(
            EntityType.ZOMBIE, EntityType.ZOMBIE,
            EntityType.MAGMA_CUBE, EntityType.MAGMA_CUBE,
            EntityType.BLAZE, EntityType.BLAZE, EntityType.BLAZE, EntityType.BLAZE
    );

    private static final CompoundTag MAGMA_CUBE_NBT = parseNbt("""
            {CanPickUpLoot:false,ArmorDropChances:[0f,0f,0f,0f],HandDropChances:[0f,0f],Health:10000000000.0f,
            Tags:["monster","instance","zhuque","magma_cube"],CustomNameVisible:1b,
            CustomName:'{"translate":"plre.monster.south.bird_magma_cube"}',DeathLootTable:"plre:empty",
            attributes:[{id:"generic.max_health",base:45},{id:"generic.movement_speed",base:0.2},
            {id:"generic.minecraft:attack_damage",base:4},{id:"generic.armor",base:0d},
            {id:"generic.follow_range",base:40d}],ArmorItems:[{},{},{},{}],HandItems:[{},{}],
            Team:"monster",PersistenceRequired:0b,Size:3}
            """);

    private static final CompoundTag ZOMBIE_NBT = parseNbt("""
            {CanPickUpLoot:false,ArmorDropChances:[0f,0f,0f,0f],HandDropChances:[0f,0f],Health:10000000000.0f,
            Tags:["monster","instance","zhuque","zombie"],CustomNameVisible:1b,
            CustomName:'{"translate":"plre.monster.south.bird_zombie"}',DeathLootTable:"plre:empty",
            attributes:[{id:"generic.max_health",base:60},{id:"generic.movement_speed",base:0.2},
            {id:"generic.minecraft:attack_damage",base:4},{id:"generic.armor",base:0d},
            {id:"generic.follow_range",base:40d}],ArmorItems:[{},{},{},
            {id:"iron_helmet",count:1b,components:{unbreakable:{}}}],
            HandItems:[{id:"golden_sword",count:1b,components:{unbreakable:{},
            enchantments:{fire_aspect:1,knockback:1}}},{}],Team:"monster",PersistenceRequired:0b}
            """);

    private static final CompoundTag BLAZE_NBT = parseNbt("""
            {CanPickUpLoot:false,ArmorDropChances:[0f,0f,0f,0f],HandDropChances:[0f,0f],Health:10000000000.0f,
            Tags:["monster","instance","zhuque","blaze"],CustomNameVisible:1b,
            CustomName:'{"translate":"plre.monster.south.bird_blaze"}',DeathLootTable:"plre:empty",
            attributes:[{id:"generic.max_health",base:35},{id:"generic.movement_speed",base:0.2},
            {id:"generic.minecraft:attack_damage",base:0},{id:"generic.armor",base:0d},
            {id:"generic.follow_range",base:40d}],ArmorItems:[{},{},{},{}],HandItems:[{},{}],
            Team:"monster",PersistenceRequired:0b}
            """);

    private final List<Entity> spawnedEntities = new ArrayList<>();
    private ServerBossEvent bossBar;
    private int startCountdownTicks;
    private int remainingTicks;
    private int waveTicks;
    private int waveStep;
    private int meteorTicks;

    @Override
    public void start(InstanceSession session) {
        ServerPlayer player = session.player();
        bossBar = new ServerBossEvent(
                Component.translatable("instance.panlingre.zhuque.countdown", 5),
                BossEvent.BossBarColor.RED,
                BossEvent.BossBarOverlay.PROGRESS
        );
        if (player != null) {
            player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("instance.panlingre.zhuque.preparing.1")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("instance.panlingre.zhuque.preparing.2")));
            bossBar.addPlayer(player);
            PacketDistributor.sendToPlayer(player, new SiShouMusicPayload(true));
        }
        startCountdownTicks = START_COUNTDOWN_TICKS;
        updateStartCountdown();
        playAtCenter(session, SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);
    }

    @Override
    public void tick(InstanceSession session) {
        if (session.isEnding()) return;
        if (startCountdownTicks > 0) {
            startCountdownTicks--;
            updateStartCountdown();
            if (startCountdownTicks % 20 == 0) playAtCenter(session, SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);
            if (startCountdownTicks == 0) beginSurvival(session);
            return;
        }

        remainingTicks--;
        updateSurvivalBar();
        if (remainingTicks <= 0) {
            bossBar.setName(Component.translatable("instance.panlingre.zhuque.completed"));
            bossBar.setProgress(1.0F);
            session.finish(InstanceResult.SUCCESS);
            return;
        }

        waveTicks--;
        if (waveTicks <= 0) {
            runWave(session);
            waveTicks = WAVE_INTERVAL_TICKS;
        }

        meteorTicks--;
        if (meteorTicks <= 0) {
            spawnMeteors(session, meteorCount());
            meteorTicks = METEOR_INTERVAL_TICKS;
        }
    }

    @Override
    public void stop(InstanceSession session, InstanceResult result) {
        if (bossBar != null) bossBar.removeAllPlayers();
        ServerPlayer player = session.player();
        if (player != null) PacketDistributor.sendToPlayer(player, new SiShouMusicPayload(false));
        for (Entity entity : spawnedEntities) {
            if (!entity.isRemoved()) entity.discard();
        }
        spawnedEntities.clear();
    }

    private void beginSurvival(InstanceSession session) {
        remainingTicks = SURVIVAL_TICKS;
        waveTicks = WAVE_INTERVAL_TICKS;
        waveStep = 0;
        meteorTicks = METEOR_INTERVAL_TICKS;
        updateSurvivalBar();
        runWave(session);
        spawnMeteors(session, 1);
    }

    private int meteorCount() {
        if (remainingTicks <= THREE_METEORS_REMAINING_TICKS) return 3;
        if (remainingTicks <= TWO_METEORS_REMAINING_TICKS) return 2;
        return 1;
    }

    private void runWave(InstanceSession session) {
        ServerLevel level = session.level();
        ServerPlayer player = session.player();
        if (level == null || player == null) return;

        if (countZhuqueMonsters(session) > MONSTER_OVERLOAD_LIMIT) {
            player.igniteForSeconds(OVERLOAD_FIRE_SECONDS);
            player.hurt(level.damageSources().onFire(), OVERLOAD_FIRE_DAMAGE);
            return;
        }

        double slotOffset = (double) session.slot() * session.definition().spacing();
        int firstSpawnIndex = waveStep * 2;
        for (int index = firstSpawnIndex; index < firstSpawnIndex + 2; index++) {
            Mob mob = MONSTER_TYPES.get(index).create(level);
            if (mob == null) continue;
            CompoundTag entityNbt = mob.saveWithoutId(new CompoundTag());
            entityNbt.merge(nbtFor(index).copy());
            mob.load(entityNbt);
            mob.setPos(BASE_MONSTER_POSITIONS.get(index).add(slotOffset, 0.0, 0.0));
            if (level.addFreshEntity(mob)) spawnedEntities.add(mob);
        }
        waveStep = (waveStep + 1) % 4;
    }

    private int countZhuqueMonsters(InstanceSession session) {
        ServerLevel level = session.level();
        if (level == null) return 0;
        Vec3 center = gameplayCenter(session);
        double radius = session.definition().spacing() * 0.5;
        AABB bounds = AABB.ofSize(center, radius * 2.0, radius * 2.0, radius * 2.0);
        double radiusSqr = radius * radius;
        return level.getEntitiesOfClass(
                Monster.class,
                bounds,
                monster -> monster.isAlive()
                        && monster.getTags().contains("zhuque")
                        && monster.position().distanceToSqr(center) <= radiusSqr
        ).size();
    }

    private void spawnMeteor(InstanceSession session) {
        ServerLevel level = session.level();
        ServerPlayer player = session.player();
        if (level == null || player == null) return;

        Vec3 center = gameplayCenter(session);
        BlockPos landingBlock = randomLandingBlock(level, center, level.random);
        if (landingBlock == null) return;
        double targetX = landingBlock.getX() + 0.5;
        double targetY = landingBlock.getY() + 0.05;
        double targetZ = landingBlock.getZ() + 0.5;

        PlayerTeam monsterTeam = level.getScoreboard().getPlayerTeam("monster");
        HuoQiuFuEntity meteor = new HuoQiuFuEntity(
                level, targetX, METEOR_SPAWN_Y, targetZ, METEOR_DAMAGE, monsterTeam
        );
        meteor.setDeltaMovement(0.0, METEOR_INITIAL_VELOCITY, 0.0);
        if (level.addFreshEntity(meteor)) spawnedEntities.add(meteor);

        int fallTicks = estimateFallTicks(METEOR_SPAWN_Y, targetY, METEOR_INITIAL_VELOCITY);
        PacketDistributor.sendToPlayer(
                player,
                new ZhuqueMeteorWarningPayload(
                        new Vec3(targetX, targetY, targetZ),
                        METEOR_WARNING_RADIUS,
                        fallTicks
                )
        );
    }

    private void spawnMeteors(InstanceSession session, int count) {
        playAtCenter(session, SoundEvents.GHAST_WARN, 4.0F, 1.0F);
        for (int index = 0; index < count; index++) {
            spawnMeteor(session);
        }
    }

    @Nullable
    private static BlockPos randomLandingBlock(ServerLevel level, Vec3 center, RandomSource random) {
        int centerX = (int) Math.floor(center.x);
        int centerZ = (int) Math.floor(center.z);
        for (int attempt = 0; attempt < 32; attempt++) {
            int offsetX = random.nextIntBetweenInclusive(-METEOR_RANDOM_RADIUS, METEOR_RANDOM_RADIUS);
            int offsetZ = random.nextIntBetweenInclusive(-METEOR_RANDOM_RADIUS, METEOR_RANDOM_RADIUS);
            if (offsetX * offsetX + offsetZ * offsetZ > METEOR_RANDOM_RADIUS * METEOR_RANDOM_RADIUS) continue;
            BlockPos landingBlock = landingBlockAt(level, centerX + offsetX, centerZ + offsetZ);
            if (landingBlock != null) return landingBlock;
        }

        // The random attempts should normally be enough. Exhaust the circle as a safe fallback so
        // a meteor is never aimed at a pillar merely because the random samples were unlucky.
        for (int offsetX = -METEOR_RANDOM_RADIUS; offsetX <= METEOR_RANDOM_RADIUS; offsetX++) {
            for (int offsetZ = -METEOR_RANDOM_RADIUS; offsetZ <= METEOR_RANDOM_RADIUS; offsetZ++) {
                if (offsetX * offsetX + offsetZ * offsetZ > METEOR_RANDOM_RADIUS * METEOR_RANDOM_RADIUS) continue;
                BlockPos landingBlock = landingBlockAt(level, centerX + offsetX, centerZ + offsetZ);
                if (landingBlock != null) return landingBlock;
            }
        }
        return null;
    }

    @Nullable
    private static BlockPos landingBlockAt(ServerLevel level, int x, int z) {
        int landingY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int surfaceY = landingY - 1;
        if (surfaceY != 60 && surfaceY != 61) return null;
        return new BlockPos(x, landingY, z);
    }

    /** Mirrors HuoQiuFuEntity's gravity and final 0.98 velocity drag closely enough for the warning lifetime. */
    private static int estimateFallTicks(double startY, double targetY, double initialVelocity) {
        double y = startY;
        double velocity = initialVelocity;
        for (int tick = 1; tick <= 200; tick++) {
            velocity -= 0.04;
            y += velocity;
            velocity *= 0.98;
            if (y <= targetY) return tick;
        }
        return 200;
    }

    private void updateStartCountdown() {
        int seconds = Math.max(1, (startCountdownTicks + 19) / 20);
        bossBar.setName(Component.translatable("instance.panlingre.zhuque.countdown", seconds));
        bossBar.setProgress(Math.max(0.0F, startCountdownTicks / (float) START_COUNTDOWN_TICKS));
    }

    private void updateSurvivalBar() {
        int seconds = Math.max(0, (remainingTicks + 19) / 20);
        bossBar.setName(Component.translatable("instance.panlingre.zhuque.ticks", seconds));
        bossBar.setProgress(Math.max(0.0F, remainingTicks / (float) SURVIVAL_TICKS));
    }

    private static Vec3 gameplayCenter(InstanceSession session) {
        return BASE_CENTER.add((double) session.slot() * session.definition().spacing(), 0.0, 0.0);
    }

    private static void playAtCenter(InstanceSession session, net.minecraft.sounds.SoundEvent sound,
                                     float volume, float pitch) {
        ServerLevel level = session.level();
        if (level == null) return;
        Vec3 center = gameplayCenter(session);
        level.playSound(null, center.x, center.y, center.z, sound, SoundSource.HOSTILE, volume, pitch);
    }

    private static CompoundTag nbtFor(int spawnIndex) {
        if (spawnIndex < 2) return ZOMBIE_NBT;
        if (spawnIndex < 4) return MAGMA_CUBE_NBT;
        return BLAZE_NBT;
    }

    private static CompoundTag parseNbt(String snbt) {
        try {
            return TagParser.parseTag(snbt);
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid Zhuque monster SNBT", exception);
        }
    }
}
