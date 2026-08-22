package icu.icuqalt10.panlingre.instance.qinglong;

import icu.icuqalt10.panlingre.instance.InstanceController;
import icu.icuqalt10.panlingre.instance.InstanceManager;
import icu.icuqalt10.panlingre.instance.InstanceResult;
import icu.icuqalt10.panlingre.instance.InstanceSession;
import icu.icuqalt10.panlingre.network.SiShouMusicPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class QinglongController implements InstanceController {
    private static final int START_COUNTDOWN_TICKS = 5 * 20;
    private static final int QUESTION_TICKS = 20 * 20;
    private static final int ANSWER_DELAY_TICKS = 2 * 20;
    private static final int SUCCESS_DELAY_TICKS = 2 * 20;
    private static final int REQUIRED_CORRECT = 10;
    private static final int MAX_WRONG = 3;
    private static final List<Vec3> BASE_ANSWER_POSITIONS = List.of(
            new Vec3(8.5, 60.0, 2024.5),
            new Vec3(8.5, 60.0, 2010.5),
            new Vec3(-8.5, 60.0, 2010.5),
            new Vec3(-8.5, 60.0, 2024.5)
    );
    // Edit this SNBT to tune the four answer zombies. Position and custom name are applied afterwards.
    private static final CompoundTag ANSWER_ZOMBIE_NBT = parseZombieNbt("{CanPickUpLoot:false,ArmorDropChances:[0f,0f,0f,0f],HandDropChances:[0f,0f],Health:10000000000.0f,Tags:[\"monster\",\"instance\",\"dragon\"],CustomNameVisible:1b,DeathLootTable:\"empty\",attributes:[{id:\"generic.max_health\",base:24},{id:\"generic.movement_speed\",base:0.2},{id:\"generic.minecraft:attack_damage\",base:4},{id:\"generic.armor\",base:0d},{id:\"generic.follow_range\",base:50d}],ArmorItems:[{},{},{},{id:\"minecraft:leather_helmet\",count:1b,components:{unbreakable:{}}}],HandItems:[{},{}],Team:\"monster\",PersistenceRequired:1b,Glowing:1b}");
    private static final List<Question> QUESTIONS = createQuestions();

    private final List<Integer> order = new ArrayList<>();
    private final Map<UUID, Integer> answers = new HashMap<>();
    private final List<Zombie> zombies = new ArrayList<>();
    private ServerBossEvent bossBar;
    private int questionCursor;
    private int correct;
    private int wrong;
    private int remainingTicks;
    private int countdownTicks;
    private int nextQuestionDelayTicks;
    private int successDelayTicks;
    private boolean resolving;

    @Override
    public void start(InstanceSession session) {
        for (int i = 0; i < QUESTIONS.size(); i++) order.add(i);
        Collections.shuffle(order);
        ServerPlayer player = session.player();
        bossBar = new ServerBossEvent(
                Component.translatable("instance.panlingre.qinglong.countdown"),
                BossEvent.BossBarColor.GREEN,
                BossEvent.BossBarOverlay.PROGRESS
        );
        if (player != null) {
            player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("instance.panlingre.qinglong.preparing.1")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("instance.panlingre.qinglong.preparing.2", correct, wrong)));
            bossBar.addPlayer(player);
            PacketDistributor.sendToPlayer(player, new SiShouMusicPayload(true));
        }
        countdownTicks = START_COUNTDOWN_TICKS;
        updateCountdownBar();
    }

    @Override
    public void tick(InstanceSession session) {
        if (session.isEnding()) return;
        if (successDelayTicks > 0) {
            successDelayTicks--;
            if (successDelayTicks == 0) session.finish(InstanceResult.SUCCESS);
            return;
        }
        if (nextQuestionDelayTicks > 0) {
            nextQuestionDelayTicks--;
            if (nextQuestionDelayTicks == 0) beginQuestion(session);
            return;
        }
        if (resolving) return;
        if (countdownTicks > 0) {
            if (countdownTicks % 20 == 0) playAtCenter(session, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.HOSTILE, 4.0F, 1.0F);
            countdownTicks--;
            updateCountdownBar();
            if (countdownTicks == 0) beginQuestion(session);
            return;
        }
        remainingTicks--;
        bossBar.setProgress(Math.max(0.0F, remainingTicks / (float) QUESTION_TICKS));
        if (remainingTicks <= 0) resolve(session, -1);
    }

    @Override
    public void onEntityDeath(InstanceSession session, LivingEntity entity) {
        Integer answer = answers.get(entity.getUUID());
        if (answer != null && !resolving) resolve(session, answer);
    }

    @Override
    public void stop(InstanceSession session, InstanceResult result) {
        clearAnswers();
        if (bossBar != null) bossBar.removeAllPlayers();
        ServerPlayer player = session.player();
        if (player != null) PacketDistributor.sendToPlayer(player, new SiShouMusicPayload(false));
    }

    private void beginQuestion(InstanceSession session) {
        if (questionCursor >= order.size()) {
            session.finish(InstanceResult.FAILURE);
            return;
        }
        resolving = false;
        remainingTicks = QUESTION_TICKS;
        Question question = QUESTIONS.get(order.get(questionCursor));
        bossBar.setName(Component.translatable(question.questionKey));
        bossBar.setProgress(1.0F);

        ServerLevel level = session.level();
        if (level == null) {
            session.finish(InstanceResult.FAILURE);
            return;
        }
        playAtCenter(session, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4.0F, 1.0F);
        double slotOffset = (double) session.slot() * session.definition().spacing();
        List<Vec3> positions = BASE_ANSWER_POSITIONS.stream()
                .map(pos -> pos.add(slotOffset, 0.0, 0.0))
                .toList();
        List<Integer> randomizedAnswers = new ArrayList<>(List.of(0, 1, 2, 3));
        Collections.shuffle(randomizedAnswers);
        for (int position = 0; position < 4; position++) {
            int answer = randomizedAnswers.get(position);
            Zombie zombie = EntityType.ZOMBIE.create(level);
            if (zombie == null) {
                session.finish(InstanceResult.FAILURE);
                return;
            }
            CompoundTag entityNbt = zombie.saveWithoutId(new CompoundTag());
            entityNbt.merge(ANSWER_ZOMBIE_NBT.copy());
            zombie.load(entityNbt);
            zombie.setPos(positions.get(position));
            zombie.setCustomName(Component.translatable(question.answerKeys[answer]));
            zombie.setCustomNameVisible(true);
            level.addFreshEntity(zombie);
            zombies.add(zombie);
            answers.put(zombie.getUUID(), answer);
            InstanceManager.ownEntity(session, zombie);
        }
    }

    private void resolve(InstanceSession session, int selectedAnswer) {
        resolving = true;
        Question question = QUESTIONS.get(order.get(questionCursor));
        boolean isCorrect = selectedAnswer == question.correctAnswer;
        clearAnswers();

        ServerPlayer player = session.player();
        if (isCorrect) {
            correct++;
            playForPlayer(session, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (player != null) player.sendSystemMessage(Component.translatable("instance.panlingre.qinglong.correct", correct, wrong));
        } else {
            wrong++;
            playForPlayer(session, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (player != null) player.sendSystemMessage(Component.translatable("instance.panlingre.qinglong.wrong", correct, wrong));
        }

        if (correct >= REQUIRED_CORRECT) {
            bossBar.setName(Component.translatable("instance.panlingre.qinglong.completed"));
            bossBar.setProgress(1.0F);
            playForPlayer(session, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (player != null) PacketDistributor.sendToPlayer(player, new SiShouMusicPayload(false));
            successDelayTicks = SUCCESS_DELAY_TICKS;
        } else if (wrong >= MAX_WRONG) {
            session.finish(InstanceResult.FAILURE);
        } else {
            questionCursor++;
            nextQuestionDelayTicks = ANSWER_DELAY_TICKS;
        }
    }

    private void clearAnswers() {
        for (Zombie zombie : zombies) {
            InstanceManager.releaseEntity(zombie);
            if (!zombie.isRemoved()) zombie.discard();
        }
        zombies.clear();
        answers.clear();
    }

    private void updateCountdownBar() {
        int seconds = Math.max(1, (countdownTicks + 19) / 20);
        bossBar.setName(Component.translatable("instance.panlingre.qinglong.countdown", seconds));
        bossBar.setProgress(Math.max(0.0F, countdownTicks / (float) START_COUNTDOWN_TICKS));
    }

    private void playAtCenter(InstanceSession session, SoundEvent sound, SoundSource source, float volume, float pitch) {
        ServerLevel level = session.level();
        if (level == null) return;
        Vec3 center = session.center();
        level.playSound(null, center.x, center.y, center.z, sound, source, volume, pitch);
    }

    private void playForPlayer(InstanceSession session, SoundEvent sound, SoundSource source, float volume, float pitch) {
        ServerPlayer player = session.player();
        if (player == null) return;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, source, volume, pitch);
    }

    private static CompoundTag parseZombieNbt(String snbt) {
        try {
            return TagParser.parseTag(snbt);
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid Qinglong answer zombie SNBT", exception);
        }
    }

    private static List<Question> createQuestions() {
        int[] correct = {2, 0, 1, 3, 2, 3, 0, 1, 3, 2, 2, 1, 1, 3, 2, 1, 0, 2, 3, 0};
        List<Question> questions = new ArrayList<>(20);
        for (int number = 1; number <= 20; number++) {
            String prefix = "instance.panlingre.dragon.";
            String[] answers = new String[4];
            for (int answer = 0; answer < 4; answer++) {
                answers[answer] = prefix + "answer" + number + (char) ('a' + answer);
            }
            questions.add(new Question(prefix + "question" + number, answers, correct[number - 1]));
        }
        return List.copyOf(questions);
    }

    private record Question(String questionKey, String[] answerKeys, int correctAnswer) {}
}
