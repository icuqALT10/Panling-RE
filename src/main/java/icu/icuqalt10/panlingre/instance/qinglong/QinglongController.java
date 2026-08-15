package icu.icuqalt10.panlingre.instance.qinglong;

import icu.icuqalt10.panlingre.instance.InstanceController;
import icu.icuqalt10.panlingre.instance.InstanceManager;
import icu.icuqalt10.panlingre.instance.InstanceResult;
import icu.icuqalt10.panlingre.instance.InstanceSession;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class QinglongController implements InstanceController {
    private static final int QUESTION_TICKS = 20 * 20;
    private static final int REQUIRED_CORRECT = 10;
    private static final int MAX_WRONG = 3;
    private static final char[] LABELS = {'A', 'B', 'C', 'D'};
    private static final List<Vec3> BASE_ANSWER_POSITIONS = List.of(
            new Vec3(12.5, 60.0, 2027.5),
            new Vec3(12.5, 60.0, 2005.5),
            new Vec3(-11.5, 60.0, 2005.5),
            new Vec3(-11.5, 60.0, 2027.5)
    );
    private static final List<Question> QUESTIONS = createQuestions();

    private final List<Integer> order = new ArrayList<>();
    private final Map<UUID, Integer> answers = new HashMap<>();
    private final List<Skeleton> skeletons = new ArrayList<>();
    private ServerBossEvent bossBar;
    private int questionCursor;
    private int correct;
    private int wrong;
    private int remainingTicks;
    private boolean resolving;

    @Override
    public void start(InstanceSession session) {
        for (int i = 0; i < QUESTIONS.size(); i++) order.add(i);
        Collections.shuffle(order);
        ServerPlayer player = session.player();
        bossBar = new ServerBossEvent(
                Component.translatable("instance.panlingre.qinglong.preparing"),
                BossEvent.BossBarColor.GREEN,
                BossEvent.BossBarOverlay.PROGRESS
        );
        if (player != null) bossBar.addPlayer(player);
        beginQuestion(session);
    }

    @Override
    public void tick(InstanceSession session) {
        if (resolving || session.isEnding()) return;
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
        double slotOffset = (double) session.slot() * session.definition().spacing();
        List<Vec3> positions = BASE_ANSWER_POSITIONS.stream()
                .map(pos -> pos.add(slotOffset, 0.0, 0.0))
                .toList();
        List<Integer> randomizedAnswers = new ArrayList<>(List.of(0, 1, 2, 3));
        Collections.shuffle(randomizedAnswers);
        for (int position = 0; position < 4; position++) {
            int answer = randomizedAnswers.get(position);
            Skeleton skeleton = EntityType.SKELETON.create(level);
            if (skeleton == null) {
                session.finish(InstanceResult.FAILURE);
                return;
            }
            skeleton.setPos(positions.get(position));
            skeleton.setNoAi(true);
            skeleton.setSilent(true);
            skeleton.setPersistenceRequired();
            skeleton.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            skeleton.setCustomName(Component.literal(LABELS[answer] + ". ").append(Component.translatable(question.answerKeys[answer])));
            skeleton.setCustomNameVisible(true);
            level.addFreshEntity(skeleton);
            skeletons.add(skeleton);
            answers.put(skeleton.getUUID(), answer);
            InstanceManager.ownEntity(session, skeleton);
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
            if (player != null) player.sendSystemMessage(Component.translatable("instance.panlingre.qinglong.correct", correct, wrong));
        } else {
            wrong++;
            if (player != null) player.sendSystemMessage(Component.translatable("instance.panlingre.qinglong.wrong", correct, wrong));
        }

        if (correct >= REQUIRED_CORRECT) {
            session.finish(InstanceResult.SUCCESS);
        } else if (wrong >= MAX_WRONG) {
            session.finish(InstanceResult.FAILURE);
        } else {
            questionCursor++;
            beginQuestion(session);
        }
    }

    private void clearAnswers() {
        for (Skeleton skeleton : skeletons) {
            InstanceManager.releaseEntity(skeleton);
            if (!skeleton.isRemoved()) skeleton.discard();
        }
        skeletons.clear();
        answers.clear();
    }

    private static List<Question> createQuestions() {
        int[] correct = {2, 0, 1, 2, 2, 3, 0, 1, 3, 2, 2, 1, 1, 3, 2, 1, 0, 2, 3, 0};
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
