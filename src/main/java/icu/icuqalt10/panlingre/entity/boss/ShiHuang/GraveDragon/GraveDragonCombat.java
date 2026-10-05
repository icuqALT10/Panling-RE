package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import icu.icuqalt10.panlingre.entity.OrientedBoundingBox;
import icu.icuqalt10.panlingre.network.GraveDragonActionPayload;
import icu.icuqalt10.panlingre.network.GraveDragonFireFieldPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import icu.icuqalt10.panlingre.init.ModEffects;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Server authority for attacks. The exported pose and the same OBBs used for picking drive melee damage. */
final class GraveDragonCombat {
    private static final float SOUND_VOLUME = 5F;
    private final GraveDragonEntity dragon;
    private GraveDragonActions.Action action;
    private double previousSeconds;
    private long sequence;
    private GraveDragonActionPayload activation;
    private GraveDragonFireField fireField;
    private Vec3 previousOrigin;
    private Vec3 actionOrigin;
    private Vec3 lockedDirection = Vec3.ZERO;
    private Vec3 coilDirection = Vec3.ZERO;
    private long recoveryUntil;
    private long nextStepSoundAt;
    private long turnUntil;
    private String queuedAction;
    private GraveDragonReposition.Plan reposition;
    private ServerPlayer turnTarget;
    private Vec3 turnDirection = Vec3.ZERO;
    private float turnStartYaw;
    private float actionYaw;
    private float comboFromYaw;
    private float comboToYaw;
    private double diveGroundY;
    private Vec3 tailFrom;
    private Vec3 tailTo;
    private float tailPitch;
    private double tailGroundY;
    private Vec3 returnPoint;
    private int airSkillsUsed;
    private boolean pendingGroundPause;
    private boolean phaseRequested;
    private boolean phaseDone;
    private boolean retreatRequested;
    private boolean retreatReady;
    private final Map<String, Set<Integer>> hitByWindow = new HashMap<>();
    private final Map<String, Long> lastDamageTick = new HashMap<>();
    private final ArrayDeque<String> recentGroundSkills = new ArrayDeque<>();
    private final Set<String> recentAirSkills = new HashSet<>();

    GraveDragonCombat(GraveDragonEntity dragon) {
        this.dragon = dragon;
    }

    boolean active() { return action != null; }
    boolean retreatReady() { return retreatReady; }
    boolean phaseDone() { return phaseDone; }
    boolean controlsMovement() {
        long now = dragon.level().getGameTime();
        return reposition != null || returnPoint != null || pendingGroundPause || queuedAction != null || turnUntil > 0 && turnUntil >= now;
    }
    void onFormChanged(boolean flying) {
        returnPoint = null;
        if (flying) {
            airSkillsUsed = 0;
            recentAirSkills.clear();
            recentGroundSkills.addLast("takeoff");
            if (recentGroundSkills.size() > 5) recentGroundSkills.removeFirst();
        }
        pendingGroundPause = !flying;
        dragon.setDeltaMovement(Vec3.ZERO);
        dragon.getNavigation().stop();
    }

    void announceAnimation(String name) { sendActivation(name, List.of()); }

    void sendFireField(ServerPlayer player) {
        if (fireField != null)
            PacketDistributor.sendToPlayer(player,
                    new GraveDragonFireFieldPayload(dragon.getId(), dragon.animationStart(), fireField));
    }

    GraveDragonActionPayload trackingPayload() {
        GraveDragonActionPayload current = activation;
        return new GraveDragonActionPayload(dragon.getId(), current == null ? dragon.animation() : current.action(),
                ++sequence, current == null ? dragon.animationStart() : current.startTick(),
                current == null ? 0 : current.seed(), current == null ? List.of() : current.points(),
                dragon.blendFrom(), dragon.blendFromSeconds());
    }

    void save(CompoundTag tag) {
        tag.putBoolean("Phase50Requested", phaseRequested);
        tag.putBoolean("Phase50Done", phaseDone);
        tag.putBoolean("RetreatRequested", retreatRequested);
        tag.putBoolean("RetreatReady", retreatReady);
        tag.putLong("CombatSequence", sequence);
        if (action != null) tag.putString("CombatAction", action.name());
        else if (reposition != null) tag.putString("CombatAction", reposition.action());
        tag.putLong("CombatRecoveryUntil", recoveryUntil);
        tag.putInt("CombatAirSkills", airSkillsUsed);
        ListTag groundHistory = new ListTag();
        for (String name : recentGroundSkills) groundHistory.add(StringTag.valueOf(name));
        tag.put("CombatGroundHistory", groundHistory);
        ListTag airHistory = new ListTag();
        for (String name : recentAirSkills) airHistory.add(StringTag.valueOf(name));
        tag.put("CombatAirHistory", airHistory);
    }

    void load(CompoundTag tag) {
        phaseRequested = tag.getBoolean("Phase50Requested");
        phaseDone = tag.getBoolean("Phase50Done");
        retreatRequested = tag.getBoolean("RetreatRequested");
        retreatReady = tag.getBoolean("RetreatReady");
        sequence = tag.getLong("CombatSequence");
        if (retreatRequested) phaseRequested = false;
        recoveryUntil = tag.getLong("CombatRecoveryUntil");
        airSkillsUsed = tag.getInt("CombatAirSkills");
        recentGroundSkills.clear();
        for (Tag entry : tag.getList("CombatGroundHistory", Tag.TAG_STRING))
            recentGroundSkills.addLast(entry.getAsString());
        recentAirSkills.clear();
        for (Tag entry : tag.getList("CombatAirHistory", Tag.TAG_STRING))
            recentAirSkills.add(entry.getAsString());
        // A saved action cannot resume its hit sweep safely after an arbitrary unload gap.
        // Close it and retain its phase state.
        if (tag.contains("CombatAction")) {
            dragon.beginCombatAnimation(dragon.flying() ? "idle_air" : "idle_ground");
            recoveryUntil = Math.max(recoveryUntil, dragon.level().getGameTime() + 30);
        }
        pendingGroundPause = !dragon.flying();
    }

    void requestThresholds() {
        if (dragon.isDeadOrDying()) return;
        float health = dragon.getHealth() / dragon.getMaxHealth();
        if (health <= .05F && !retreatRequested) {
            retreatRequested = true;
            phaseRequested = false;
            action = null;
            dragon.setCombatPitch(0);
            fireField = null;
            hitByWindow.clear();
            returnPoint = null;
            pendingGroundPause = false;
            dragon.getNavigation().stop();
            dragon.setDeltaMovement(Vec3.ZERO);
            dragon.setMoving(false);
            dragon.setHealth(dragon.getMaxHealth() * .05F);
            dragon.setInvulnerable(true);
            if (dragon.tombController() != null) dragon.tombController().dragonRetreating(dragon);
            queuedAction = null;
            reposition = null;
            turnTarget = null;
            turnUntil = 0;
        } else if (health <= .50F && !phaseRequested && !phaseDone && !retreatRequested) {
            phaseRequested = true;
            dragon.setHealth(dragon.getMaxHealth() * .50F);
            dragon.setInvulnerable(true);
            queuedAction = null;
            reposition = null;
            turnTarget = null;
            turnUntil = 0;
        }
    }

    void tick() {
        long now = dragon.level().getGameTime();
        requestThresholds();
        if (dragon.isDeadOrDying()) { cancel(); return; }
        if (action != null) {
            tickActive(now);
            return;
        }
        if (reposition != null) {
            tickReposition(now);
            return;
        }
        if (!dragon.flying() && !dragon.isTransitioningForm() && !phaseRequested && !retreatRequested
                && (returnPoint != null || horizontalDistanceSqr(dragon.position(), dragon.combatSpawnPosition()) > 100 * 100)) {
            tickReturnHome(now);
            return;
        }
        if (dragon.flying()) {
            dragon.getNavigation().stop();
            dragon.setDeltaMovement(Vec3.ZERO);
        }
        if (turnUntil > now && dragon.animationSeconds(0) < GraveDragonPose.duration(dragon.animation())) {
            tickTurn(now);
            return;
        }
        if (turnUntil > 0 && turnUntil == now) tickTurn(now);
        if (queuedAction != null) {
            String selected = queuedAction;
            queuedAction = null;
            turnUntil = 0;
            ServerPlayer target = turnTarget;
            turnTarget = null;
            if (target != null && target.isAlive() && target.distanceToSqr(dragon) <= 10000) {
                float yaw = (float) Math.toDegrees(Math.atan2(-turnDirection.x, turnDirection.z));
                dragon.setYRot(yaw);
                dragon.yBodyRot = yaw;
                dragon.yHeadRot = yaw;
                if (selected.equals("phase50_firefield") || available(selected, target, false))
                    begin(selected, pointsFor(selected, target), false, target);
                else idleBeforeNextAttack(now);
            }
            return;
        }
        if (retreatRequested) {
            if (!retreatReady) {
                if (dragon.flying() || dragon.isTransitioningForm()) {
                    if (!dragon.isRetreatLanding()) dragon.beginRetreatLanding();
                } else begin("dying_thrash", List.of(), true, null);
            }
            return;
        }
        if (phaseRequested) {
            if (!dragon.flying()) dragon.requestCombatForm(GraveDragonEntity.Form.AIR);
            else if (!dragon.isTransitioningForm() && now >= recoveryUntil) {
                List<Vec3> points = fieldPoints();
                if (!points.isEmpty()) {
                    ServerPlayer target = nearestPlayer(100);
                    if (target == null) begin("phase50_firefield", points, true, null);
                    else turnBefore("phase50_firefield", target);
                }
            }
            return;
        }
        if (dragon.isTransitioningForm()) return;
        if (pendingGroundPause && !dragon.flying()) {
            pendingGroundPause = false;
            startGroundPause(now);
            return;
        }
        if (now < recoveryUntil) return;
        if (dragon.flying() && airSkillsUsed >= 3) {
            airSkillsUsed = 0;
            dragon.requestCombatForm(GraveDragonEntity.Form.GROUND);
            return;
        }
        ServerPlayer target = nearestPlayer(100);
        Selection selected = target == null ? null : select(target);
        if (selected == null) {
            idleBeforeNextAttack(now);
            return;
        }
        if (selected.action().name().equals("takeoff")) {
            dragon.requestCombatForm(GraveDragonEntity.Form.AIR);
            return;
        }
        if (selected.turn() && !dragon.flying()) {
            reposition = GraveDragonReposition.plan(dragon, selected.action(), target);
            if (reposition == null) {
                idleBeforeNextAttack(now);
                return;
            }
            turnUntil = 0;
            dragon.getNavigation().stop();
            dragon.setDeltaMovement(Vec3.ZERO);
            dragon.setMoving(false);
            dragon.beginCombatAnimation(reposition.animation());
            previousSeconds = -1.0 / 20;
            dragon.refreshCombatPose();
            sendActivation(reposition.animation(), List.of(reposition.target()));
        } else if (selected.turn()) turnBefore(selected.action().name(), target);
        else begin(selected.action().name(), pointsFor(selected.action().name(), target), false, target);
    }

    private void begin(String name, List<Vec3> points, boolean keepRecovery, ServerPlayer target) {
        GraveDragonActions.Action definition = GraveDragonActions.ALL.get(name);
        if (definition == null) throw new IllegalArgumentException(name);
        if (name.equals("phase50_firefield")) {
            phaseRequested = false;
            phaseDone = true;
            airSkillsUsed = 1;
        }
        if (!keepRecovery && airAction(name)) airSkillsUsed++;
        if (!keepRecovery || name.equals("phase50_firefield")) {
            if (dragon.flying()) recentAirSkills.add(name);
            else {
                recentGroundSkills.addLast(historyName(name));
                if (recentGroundSkills.size() > 5) recentGroundSkills.removeFirst();
            }
        }
        turnUntil = 0;
        pendingGroundPause = false;
        action = definition;
        fireField = null;
        actionYaw = dragon.yBodyRot;
        comboFromYaw = comboToYaw = actionYaw;
        previousSeconds = -1.0 / 20.0;
        previousOrigin = dragon.position();
        actionOrigin = dragon.position();
        tailFrom = null;
        tailTo = null;
        if (name.equals("air_tail_pierce")) tailGroundY = dragon.combatGroundLevel();
        if (name.equals("ground_coil")) coilDirection = randomHorizontalDirection();
        returnPoint = null;
        if (name.equals("air_dive_slam")) diveGroundY = dragon.combatGroundLevel();
        hitByWindow.clear();
        dragon.beginCombatAnimation(name);
        dragon.refreshCombatPose();
        Vec3 aim = name.equals("ground_charge") || name.equals("ground_double_bite") ? forward()
                : points.isEmpty() ? forward() : points.getFirst().subtract(headCenter());
        aim = aim.multiply(1, 0, 1);
        lockedDirection = aim.lengthSqr() < 1.0E-6 ? forward() : aim.normalize();
        dragon.getNavigation().stop();
        dragon.setDeltaMovement(Vec3.ZERO);
        dragon.setMoving(false);
        sendActivation(name, points);
    }

    private void turnBefore(String name, ServerPlayer target) {
        Vec3 direction = target.position().subtract(dragon.position()).multiply(1, 0, 1).normalize();
        if (direction.lengthSqr() < .001) direction = forward();
        float desired = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float difference = Mth.wrapDegrees(desired - dragon.yBodyRot);
        if (Math.abs(difference) < 8) {
            begin(name, pointsFor(name, target), false, target);
            return;
        }
        queuedAction = name;
        turnTarget = target;
        startTurn(direction, difference);
    }

    private void startTurn(Vec3 direction, float difference) {
        turnDirection = direction;
        turnStartYaw = dragon.yBodyRot;
        turnUntil = dragon.level().getGameTime() + 5;
        dragon.getNavigation().stop();
        dragon.setDeltaMovement(Vec3.ZERO);
        String turn = (dragon.flying() ? "turn_air_" : "turn_ground_")
                + (difference > 0 ? "left" : "right");
        dragon.beginCombatAnimation(turn);
        previousSeconds = -1.0 / 20;
        sendActivation(turn, List.of());
    }

    private void tickTurn(long now) {
        float desired = (float) Math.toDegrees(Math.atan2(-turnDirection.x, turnDirection.z));
        float original = dragon.yBodyRot;
        float progress = Mth.clamp((now - turnUntil + 5) / 5F, 0, 1);
        float yaw = turnStartYaw + Mth.wrapDegrees(desired - turnStartYaw) * progress;
        int steps = Math.max(1, (int) Math.ceil(Math.abs(Mth.wrapDegrees(yaw - original)) / 2));
        boolean fits = true;
        for (int i = 1; i <= steps; i++) {
            float facing = original + Mth.wrapDegrees(yaw - original) * i / steps;
            if (!dragon.combatPoseFitsAt(dragon.animation(), dragon.animationSeconds(0), facing, dragon.bodyPitch(),
                    dragon.position(), dragon.flying() ? Double.NEGATIVE_INFINITY : dragon.getY())) {
                fits = false;
                break;
            }
        }
        if (!fits) {
            queuedAction = null;
            reposition = null;
            turnTarget = null;
            turnUntil = 0;
            recoveryUntil = Math.max(recoveryUntil, now + 20);
            returnPoint = null;
            dragon.beginCombatAnimation(dragon.flying() ? "idle_air" : "idle_ground");
            sendActivation(dragon.animation(), List.of());
        } else {
            dragon.setYRot(yaw);
            dragon.yBodyRot = yaw;
            dragon.yHeadRot = yaw;
        }
        turnStepSounds();
        dragon.refreshCombatPose();
    }

    /** Finish the fixed turn/translation on the clip's final tick, then attack the locked point. */
    private void tickReposition(long now) {
        var plan = reposition;
        double progress = Mth.clamp((double) (now - plan.startedAt()) / plan.ticks(), 0, 1);
        Vec3 destination = plan.from().lerp(plan.to(), progress);
        float yaw = plan.fromYaw() + Mth.wrapDegrees(plan.yaw() - plan.fromYaw()) * (float) progress;
        Vec3 before = dragon.position();
        float previousYaw = dragon.yBodyRot;
        int steps = Math.max(1, (int) Math.ceil(Math.max(before.distanceTo(destination) / .25,
                Math.abs(Mth.wrapDegrees(yaw - previousYaw)) / 2)));
        // Small steps keep a fast reposition from skipping a thin wall between its endpoints.
        for (int step = 1; step <= steps; step++) {
            double fraction = (double) step / steps;
            Vec3 position = before.lerp(destination, fraction);
            float facing = previousYaw + Mth.wrapDegrees(yaw - previousYaw) * (float) fraction;
            BlockPos floor = BlockPos.containing(position.x, position.y - .1, position.z);
            if (!dragon.level().getBlockState(floor).isFaceSturdy(dragon.level(), floor, Direction.UP)
                    || !dragon.combatPoseFitsAt(plan.animation(), dragon.animationSeconds(0),
                    facing, dragon.bodyPitch(), position, plan.from().y)) {
                reposition = null;
                idleBeforeNextAttack(now);
                return;
            }
            dragon.setPos(position);
            dragon.setYRot(facing);
            dragon.yBodyRot = facing;
            dragon.yHeadRot = facing;
        }
        dragon.setDeltaMovement(Vec3.ZERO);
        dragon.refreshCombatPose();
        turnStepSounds();
        if (progress >= 1) {
            reposition = null;
            begin(plan.action(), pointsFor(plan.action(), plan.target()), false, null);
        }
    }

    private void startGroundPause(long now) {
        idleBeforeNextAttack(now);
    }

    private void idleBeforeNextAttack(long now) {
        pendingGroundPause = false;
        recoveryUntil = now + (dragon.flying() ? 20 : 20 + dragon.getRandom().nextInt(21));
        dragon.getNavigation().stop();
        dragon.setDeltaMovement(Vec3.ZERO);
        dragon.setMoving(false);
        String idle = dragon.flying() ? "idle_air" : "idle_ground";
        // Retrying selection extends the rest, not the animation's starting time.
        if (!idle.equals(dragon.animation())) {
            dragon.beginCombatAnimation(idle);
            dragon.refreshCombatPose();
            sendActivation(idle, List.of());
        }
    }

    private void stepSound(long now) {
        if (now < nextStepSoundAt) return;
        nextStepSoundAt = now + 10;
        dragon.refreshCombatPose();
        for (int index : new int[]{26, 39, 52, 65}) {
            Vec3 foot = dragon.getWorldParts()[index].getOrientedBox().center;
            BlockPos floor = BlockPos.containing(foot.x, dragon.getY() - .1, foot.z);
            dragon.level().playSound(null, foot.x, foot.y, foot.z,
                    dragon.level().getBlockState(floor).getSoundType(dragon.level(), floor, dragon).getStepSound(),
                    SoundSource.HOSTILE, SOUND_VOLUME, .7F);
        }
    }

    private void turnStepSounds() {
        if (dragon.flying()) return;
        double seconds = dragon.animationSeconds(0);
        if (GraveDragonActions.crossed(previousSeconds, seconds, .4)
                || GraveDragonActions.crossed(previousSeconds, seconds, 1.6)) {
            nextStepSoundAt = 0;
            stepSound(dragon.level().getGameTime());
        }
        previousSeconds = seconds;
    }

    private void tickWalk(long now) {
        Vec3 before = dragon.position();
        Vec3 step = forward().scale(.12 * dragon.getAttributeValue(Attributes.MOVEMENT_SPEED) / .25);
        Vec3 next = before.add(step);
        double seconds = dragon.animationSeconds(0) % GraveDragonPose.duration("run");
        if (!supported(next) || !dragon.combatPoseFitsAt("run", seconds,
                dragon.yBodyRot, dragon.bodyPitch(), next, before.y)) {
            dragon.beginCombatAnimation("idle_ground");
            sendActivation(dragon.animation(), List.of());
        } else {
            dragon.setPos(next);
            stepSound(now);
        }
        dragon.setDeltaMovement(Vec3.ZERO);
        dragon.refreshCombatPose();
    }

    private void tickReturnHome(long now) {
        // Once back inside the arena, resume attack selection instead of walking
        // all the way to the old random waypoint through repeated walk/pause cycles.
        if (horizontalDistanceSqr(dragon.position(), dragon.combatSpawnPosition()) <= 100 * 100) {
            returnPoint = null;
            turnUntil = 0;
            idleBeforeNextAttack(now);
            return;
        }
        if (now < recoveryUntil) return;
        if (returnPoint == null) {
            queuedAction = null;
            reposition = null;
            turnTarget = null;
            turnUntil = 0;
            pendingGroundPause = false;
            returnPoint = findReturnPoint();
            if (returnPoint == null) {
                dragon.beginCombatAnimation("idle_ground");
                sendActivation("idle_ground", List.of());
                recoveryUntil = now + 20;
                return;
            }
        }
        if (turnUntil > now && dragon.animationSeconds(0) < GraveDragonPose.duration(dragon.animation())) {
            tickTurn(now);
            return;
        }
        if (turnUntil > 0 && turnUntil == now) {
            tickTurn(now);
            if (returnPoint == null) return;
        }
        Vec3 toward = returnPoint.subtract(dragon.position()).multiply(1, 0, 1);
        turnUntil = 0;
        if (toward.lengthSqr() <= 1) {
            returnPoint = null;
            turnUntil = 0;
            pendingGroundPause = true;
            return;
        }
        Vec3 direction = toward.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float difference = Mth.wrapDegrees(yaw - dragon.yBodyRot);
        if (Math.abs(difference) >= 8) {
            startTurn(direction, difference);
            return;
        }
        if (!dragon.animation().equals("run")) {
            dragon.beginCombatAnimation("run");
            sendActivation("run", List.of());
        }
        tickWalk(now);
        if (!dragon.animation().equals("run")) {
            returnPoint = null;
            recoveryUntil = now + 20;
        }
    }

    private Vec3 findReturnPoint() {
        Vec3 home = dragon.combatSpawnPosition();
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = dragon.getRandom().nextDouble() * Math.PI * 2;
            double radius = Math.sqrt(dragon.getRandom().nextDouble()) * 80;
            Vec3 point = ground(new Vec3(home.x + Math.cos(angle) * radius, dragon.getY(),
                    home.z + Math.sin(angle) * radius));
            if (!supported(point) || Math.abs(point.y - dragon.getY()) > 1) continue;
            Vec3 travel = point.subtract(dragon.position()).multiply(1, 0, 1);
            float yaw = (float) Math.toDegrees(Math.atan2(-travel.x, travel.z));
            if (!clearLine(headCenter(), headCenter().add(travel))) continue;
            boolean fits = true;
            int steps = Math.max(1, (int) Math.ceil(travel.length() / 3));
            for (int step = 0; step <= steps && fits; step++) {
                Vec3 origin = dragon.position().add(travel.scale((double) step / steps));
                fits = dragon.combatPoseFitsAt("run", 0, yaw, 0, origin, dragon.getY())
                        && dragon.combatPoseFitsAt("run", .5, yaw, 0, origin, dragon.getY());
            }
            if (fits) return point;
        }
        return null;
    }

    private void sendActivation(String name, List<Vec3> points) {
        activation = new GraveDragonActionPayload(dragon.getId(), name, ++sequence,
                dragon.animationStart(), dragon.getRandom().nextLong(), List.copyOf(points),
                dragon.blendFrom(), dragon.blendFromSeconds());
        PacketDistributor.sendToPlayersTrackingEntity(dragon, activation);
    }

    private void tickActive(long now) {
        // Target movement, death or logout cannot truncate a clip already in progress.
        double seconds = dragon.animationSeconds(0);
        if (action.name().equals("ground_double_bite")) tickComboTurn(seconds, 1.35, 2);
        if (action.name().equals("air_tail_pierce")) tickComboTurn(seconds, 2.15, 2.75);
        dragon.setYRot(actionYaw);
        dragon.yBodyRot = actionYaw;
        dragon.yHeadRot = actionYaw;
        if (action.name().equals("air_dive_slam")) tickDiveSlam(seconds);
        if (action.name().equals("ground_double_bite")) {
            if (seconds >= 1.2 && seconds < 1.35 || seconds >= 2 && seconds < 2.15)
                moveAttack(lockedDirection.scale(GraveDragonMythic.BITE_STEP), seconds, dragon.getY());
            if (GraveDragonActions.crossed(previousSeconds, seconds, 1.2)
                    || GraveDragonActions.crossed(previousSeconds, seconds, 2))
                dragon.level().playSound(null, headCenter().x, headCenter().y, headCenter().z,
                        SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, SOUND_VOLUME, 1.2F);
        }
        if (action.name().equals("air_tail_pierce")) tickTailPierce(seconds);
        if (action.name().equals("air_thunder_trail")) {
            for (double charge : new double[]{1, 4.2, 7.4}) {
                if (GraveDragonActions.crossed(previousSeconds, seconds, charge)) {
                    ServerPlayer target = nearestPlayer(100);
                    sendActivation(action.name(), target == null ? List.of() : lightningPoints(target));
                }
            }
        }
        if ((action.name().startsWith("ground_flank_press") && GraveDragonActions.crossed(previousSeconds, seconds, 2.15))
                || action.name().equals("ground_rift_fan") && GraveDragonActions.crossed(previousSeconds, seconds, 2.1))
            dragon.level().playSound(null, dragon.getX(), dragon.getY(), dragon.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, SOUND_VOLUME, .7F);
        if (action.name().equals("air_open_fire_ring") && GraveDragonActions.crossed(previousSeconds, seconds, 1.75))
            dragon.level().playSound(null, headCenter().x, headCenter().y, headCenter().z,
                    SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, SOUND_VOLUME, .7F);
        if ((action.name().equals("roar") || action.name().equals("roar_air"))
                && (GraveDragonActions.crossed(previousSeconds, seconds, .9)
                || GraveDragonActions.crossed(previousSeconds, seconds, 1.9)))
            dragon.level().playSound(null, headCenter().x, headCenter().y, headCenter().z, SoundEvents.ENDER_DRAGON_GROWL,
                    SoundSource.HOSTILE, SOUND_VOLUME, .8F);
        if (action.name().startsWith("ground_claw_")
                && GraveDragonActions.crossed(previousSeconds, seconds, 1)) {
            int index = switch (action.name()) {
                case "ground_claw_front_l" -> 26;
                case "ground_claw_front_r" -> 39;
                case "ground_claw_back_l" -> 52;
                default -> 65;
            };
            Vec3 paw = dragon.getWorldParts()[index].getOrientedBox().center;
            dragon.level().playSound(null, paw.x, paw.y, paw.z, SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.HOSTILE, SOUND_VOLUME, .7F);
        }
        if (action.name().equals("ground_coil") && seconds >= 3 && seconds < 9) {
            List<Vec3> boundary = coilBoundary();
            ServerPlayer target = null;
            double best = Double.MAX_VALUE;
            for (ServerPlayer player : targetingPlayers()) {
                double distance = player.distanceToSqr(dragon);
                if (!insideCoil(player, boundary) && distance < best) { target = player; best = distance; }
            }
            Vec3 toward = target == null ? coilDirection
                    : target.position().subtract(coilCenter()).multiply(1, 0, 1);
            Vec3 step = toward.normalize().scale(target == null ? .5 : Math.min(.5, toward.length()));
            Vec3 before = dragon.position();
            // This clip's toes cross the floor plane. Keep support and wall
            // checks, without treating that authored contact as a blocked move.
            int steps = Math.max(1, (int) Math.ceil(step.length() / .25));
            for (int i = 1; i <= steps; i++) {
                Vec3 next = before.add(step.scale((double) i / steps));
                if (!supported(next) || !dragon.combatPoseFitsAt(action.name(), seconds,
                        actionYaw, dragon.bodyPitch(), next, before.y)) break;
                dragon.setPos(next);
            }
            if (horizontalDistanceSqr(before, dragon.position()) > .0001) stepSound(now);
            else if (target == null) coilDirection = randomHorizontalDirection();
        }
        boolean chargeBlocked = false;
        if (action.name().equals("ground_charge") && seconds >= 2 && seconds < 6.5) {
            Vec3 before = dragon.position();
            Vec3 step = lockedDirection.scale(1.0);
            chargeBlocked = headTouchesBlock(step);
            if (!chargeBlocked) {
                dragon.move(MoverType.SELF, step);
                chargeBlocked = horizontalDistanceSqr(dragon.position().subtract(before), step) > 1.0E-6;
                if (chargeBlocked) dragon.setPos(before);
                else stepSound(now);
            }
        }
        dragon.refreshCombatPose();
        for (int i = 0; i < action.windows().size(); i++) {
            GraveDragonActions.Window window = action.windows().get(i);
            if (window.active(previousSeconds, seconds)) processWindow(i, window, Math.max(window.from(), previousSeconds), Math.min(window.to(), seconds), now);
        }
        previousSeconds = seconds;
        previousOrigin = dragon.position();
        if (chargeBlocked) {
            transition("ground_charge_wall", dragon.position());
            return;
        }
        if (seconds < action.duration()) return;
        if (action.name().equals("ground_charge")) {
            transition("ground_charge_nowall", dragon.position());
            return;
        }
        if (action.name().equals("dying_thrash")) {
            retreatReady = true;
            action = null;
            if (dragon.tombController() != null && dragon.tombController().dragonDyingFinished(dragon)) return;
            dragon.setInvulnerable(false);
            dragon.hurt(dragon.damageSources().genericKill(), Float.MAX_VALUE);
            return;
        }
        int remaining = action.recovery();
        if (action.name().equals("phase50_firefield")) dragon.setInvulnerable(false);
        finish(now, remaining);
    }

    private void tickComboTurn(double seconds, double start, double end) {
        if (seconds < start || seconds > end) return;
        if (GraveDragonActions.crossed(previousSeconds, seconds, start)) {
            comboFromYaw = comboToYaw = actionYaw;
            ServerPlayer target = nearestPlayer(100);
            if (target != null) {
                Vec3 point = target.position();
                Vec3 toward = point.subtract(dragon.position()).multiply(1, 0, 1);
                if (toward.lengthSqr() > .001)
                    comboToYaw = (float) Math.toDegrees(Math.atan2(-toward.x, toward.z));
                if (action.name().equals("air_tail_pierce"))
                    sendActivation(action.name(), List.of(activation.points().getFirst(), ground(point)));
            }
        }
        float progress = (float) Mth.clamp((seconds - start) / (end - start), 0, 1);
        actionYaw = comboFromYaw + Mth.wrapDegrees(comboToYaw - comboFromYaw) * progress;
        if (action.name().equals("ground_double_bite")) lockedDirection = GraveDragonMythic.forward(actionYaw);
    }

    private void tickDiveSlam(double seconds) {
        if (seconds < 2.5) return;
        double floor = diveGroundY;
        if (seconds < 3.9) {
            ServerPlayer target = nearestPlayer(100);
            if (target != null) {
                Vec3 toward = target.position().subtract(dragon.position()).multiply(1, 0, 1);
                if (toward.lengthSqr() > .01) {
                    float previousYaw = actionYaw;
                    float previousPitch = dragon.bodyPitch();
                    actionYaw = (float) Math.toDegrees(Math.atan2(-toward.x, toward.z));
                    dragon.setYRot(actionYaw);
                    dragon.yBodyRot = actionYaw;
                    dragon.yHeadRot = actionYaw;
                    double height = Math.max(1, headCenter().y - target.getY());
                    float pitch = (float) -Math.toDegrees(Math.atan2(toward.length(), height));
                    double fade = Mth.clamp((3.9 - seconds) / .4, 0, 1);
                    dragon.setCombatPitch(pitch * (float) fade);
                    if (!dragon.combatPoseFitsAt(action.name(), seconds, actionYaw, dragon.bodyPitch(), dragon.position(), floor)) {
                        actionYaw = previousYaw;
                        dragon.setYRot(previousYaw);
                        dragon.yBodyRot = previousYaw;
                        dragon.yHeadRot = previousYaw;
                        dragon.setCombatPitch(previousPitch);
                        dragon.refreshCombatPose();
                    } else {
                        dragon.refreshCombatPose();
                        dragon.move(MoverType.SELF, toward.normalize().scale(Math.min(.5, toward.length())));
                        double below = dragon.combatGroundLevel();
                        if (Math.abs(below - dragon.getY()) > .001 || supported(dragon.position()))
                            floor = diveGroundY = below;
                    }
                }
            }
            double progress = (seconds - 2.5) / 1.4;
            Vec3 position = new Vec3(dragon.getX(), Mth.lerp(progress, actionOrigin.y, floor), dragon.getZ());
            if (dragon.combatPoseFitsAt(action.name(), seconds, actionYaw, dragon.bodyPitch(), position, floor))
                dragon.setPos(position);
        } else {
            dragon.setCombatPitch(0);
            double progress = Mth.clamp((seconds - 4.4) / (action.duration() - 4.4), 0, 1);
            double airY = floor == actionOrigin.y ? actionOrigin.y : floor + 12;
            Vec3 position = new Vec3(dragon.getX(), Mth.lerp(progress, floor, airY), dragon.getZ());
            if (dragon.combatPoseFitsAt(action.name(), seconds, actionYaw, 0, position, floor)) dragon.setPos(position);
        }
    }

    /** Scripted translations are split into wall-tested steps; authored floor contact is allowed. */
    private void moveAttack(Vec3 delta, double seconds, double floor) {
        Vec3 start = dragon.position();
        int steps = Math.max(1, (int) Math.ceil(delta.length() / .25));
        for (int i = 1; i <= steps; i++) {
            Vec3 next = start.add(delta.scale((double) i / steps));
            if (!dragon.flying() && !supported(next)) break;
            if (!dragon.combatPoseFitsAt(action.name(), seconds, actionYaw, dragon.bodyPitch(), next, floor)) break;
            dragon.setPos(next);
        }
        dragon.setDeltaMovement(Vec3.ZERO);
        if (!dragon.flying() && start.distanceToSqr(dragon.position()) > .0001) stepSound(dragon.level().getGameTime());
        dragon.refreshCombatPose();
    }

    private void tickTailPierce(double seconds) {
        int shot = seconds < 2.75 ? 0 : 1;
        double start = shot == 0 ? 1.25 : 2.75;
        double impact = shot == 0 ? 2 : 3.5;
        if (seconds < start) return;
        if (GraveDragonActions.crossed(previousSeconds, seconds, start)) {
            Vec3 target = activation.points().get(shot);
            tailPitch = GraveDragonMythic.tailPitch(dragon, target, actionYaw, impact, actionOrigin);
            tailFrom = dragon.position();
            tailTo = GraveDragonMythic.tailDestination(dragon, target, actionYaw, tailPitch, impact);
        }
        if (tailTo == null) return;
        double progress = Mth.clamp((seconds - start) / (impact - start), 0, 1);
        double recovery = seconds <= 3.7 ? 1 : Mth.clamp((5 - seconds) / 1.3, 0, 1);
        float previousPitch = dragon.bodyPitch(0);
        dragon.setCombatPitch(tailPitch * (float) Math.min(1, progress * 3) * (float) recovery);
        Vec3 goal = seconds <= 3.7 ? tailFrom.lerp(tailTo, progress) : actionOrigin.lerp(tailTo, recovery);
        double minimumY = GraveDragonMythic.tailMinimumY(dragon, seconds, actionYaw, dragon.bodyPitch(), tailGroundY);
        // Keep the visible pose clear between ticks as well as at their endpoints.
        for (int i = 1; i < 8; i++) {
            double partial = i / 8.0;
            double required = GraveDragonMythic.tailMinimumY(dragon, Mth.lerp(partial, previousSeconds, seconds), actionYaw,
                    Mth.lerp((float) partial, previousPitch, dragon.bodyPitch()), tailGroundY);
            minimumY = Math.max(minimumY, dragon.getY() + (required - dragon.getY()) / partial);
        }
        goal = new Vec3(goal.x, Math.max(goal.y, minimumY), goal.z);
        moveAttack(goal.subtract(dragon.position()), seconds, tailGroundY);
        if (GraveDragonActions.crossed(previousSeconds, seconds, impact - .15)) {
            Vec3 point = dragon.getWorldParts()[20].getOrientedBox().center;
            dragon.level().playSound(null, point.x, point.y, point.z, SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.HOSTILE, SOUND_VOLUME, 1F);
        }
    }

    private void transition(String name, Vec3 point) {
        action = GraveDragonActions.ALL.get(name);
        if (name.equals("ground_charge_wall")) {
            point = GraveDragonBreath.geometry(dragon).mouth();
            dragon.level().playSound(null, point.x, point.y, point.z, SoundEvents.GENERIC_EXPLODE,
                    SoundSource.HOSTILE, SOUND_VOLUME, .7F);
        }
        dragon.beginBlendedCombatAnimation(name);
        dragon.refreshCombatPose();
        previousSeconds = -1.0 / 20.0;
        previousOrigin = dragon.position();
        dragon.setDeltaMovement(Vec3.ZERO);
        sendActivation(name, List.of(point));
    }

    private boolean headTouchesBlock(Vec3 step) {
        for (int index : new int[]{11, 12, 74, 75, 76, 77, 78}) {
            OrientedBoundingBox box = dragon.getWorldParts()[index].getOrientedBox().move(step);
            for (var shape : dragon.level().getBlockCollisions(dragon, box.enclosingAabb()))
                for (AABB block : shape.toAabbs()) if (box.intersects(block)) return true;
        }
        return false;
    }

    private void finish(long now, int recovery) {
        dragon.setCombatPitch(0);
        double lastHit = action.windows().stream().mapToDouble(GraveDragonActions.Window::to).max().orElse(0);
        int elapsedSinceHit = Math.max(0, (int) Math.round((action.duration() - lastHit) * 20));
        int extra = Math.max(0, recovery - elapsedSinceHit);
        recoveryUntil = Math.max(recoveryUntil, now + extra);
        action = null;
        pendingGroundPause = false;
        fireField = null;
        dragon.setMoving(false);
        dragon.setDeltaMovement(Vec3.ZERO);
        if (!dragon.flying() && !retreatRequested && !phaseRequested) {
            hitByWindow.clear();
            startGroundPause(now);
            return;
        }
        String idle = dragon.flying() ? "idle_air" : "idle_ground";
        dragon.beginCombatAnimation(idle);
        dragon.refreshCombatPose();
        sendActivation(idle, List.of());
        activation = null;
        hitByWindow.clear();
    }

    private void cancel() {
        if (action == null) return;
        action = null;
        dragon.setCombatPitch(0);
        dragon.setMoving(false);
        fireField = null;
        hitByWindow.clear();
        dragon.beginCombatAnimation(dragon.flying() ? "idle_air" : "idle_ground");
        dragon.refreshCombatPose();
        long now = dragon.level().getGameTime();
        recoveryUntil = Math.max(recoveryUntil, now + 30);
        sendActivation(dragon.animation(), List.of());
        activation = null;
    }

    void interruptForFreeze() {
        // Attack history and air combo count are recorded by begin(), so keep them.
        if (action != null && action.name().equals("phase50_firefield")) dragon.setInvulnerable(false);
        action = null;
        fireField = null;
        dragon.setCombatPitch(0);
        tailFrom = tailTo = null;
        hitByWindow.clear();
        lastDamageTick.clear();
        queuedAction = null;
        reposition = null;
        turnTarget = null;
        returnPoint = null;
        turnUntil = 0;
        pendingGroundPause = false;
        dragon.beginCombatAnimation(dragon.flying() ? "idle_air" : "idle_ground");
        dragon.refreshCombatPose();
        sendActivation(dragon.animation(), List.of());
        activation = null;
    }

    void recoverFromFreeze() {
        recoveryUntil = dragon.level().getGameTime() + 20;
        dragon.beginCombatAnimation(dragon.flying() ? "idle_air" : "idle_ground");
        sendActivation(dragon.animation(), List.of());
    }

    private void processWindow(int index, GraveDragonActions.Window window, double from, double to, long now) {
        String key = action.name() + ':' + index;
        switch (window.part()) {
            case "tail_slam" -> strikeTailSlam(key, window.damage(), now);
            case "rift" -> {
                if (GraveDragonActions.crossed(previousSeconds, dragon.animationSeconds(0), window.from()))
                    launchRift(window.damage());
            }
            case "fan_breath" -> {
                if (dragon.animationSeconds(0) < window.to()) strikeFan(key, window.damage(), now);
            }
            case "fire_ring" -> {
                if (dragon.animationSeconds(0) < window.to()) strikeFireRing(key, window.damage(), now, from, to);
            }
            case "thunder_trail" -> {
                if (GraveDragonActions.crossed(previousSeconds, dragon.animationSeconds(0), window.from()))
                    strikeThunderTrail(key, window.damage(), now);
            }
            case "lightning" -> strikeLightning(now);
            case "firezone" -> launchScheduledFireball();
            case "firefield" -> strikeFields(now);
            case "fireball" -> launchScheduledFireball();
            case "roar" -> roar();
            case "landing" -> strikeLanding(key + ":landing", window.damage(), now);
            case "breath" -> strikeBreath(key, window.damage(), window.repeatTicks(), now);
            case "charge" -> strikeCharge(key, window.damage(), now);
            case "flank_l", "flank_r" -> {
                if (GraveDragonActions.crossed(previousSeconds, dragon.animationSeconds(0), window.from())) {
                    var flank = GraveDragonMythic.flank(dragon, action.name(), actionYaw, dragon.position());
                    for (ServerPlayer player : nearbyPlayers(100))
                        if (GraveDragonMythic.flankHits(dragon, flank, player))
                            damage(player, key, window.damage(), 0, now, flank.surface());
                }
            }
            default -> strikeParts(key, window.part(), window.damage(), window.repeatTicks(), now, from, to);
        }
    }

    private void damage(ServerPlayer player, String key, float amount, int interval, long now) {
        damage(player, key, amount, interval, now, dragon.position());
    }

    private void damage(ServerPlayer player, String key, float amount, int interval, long now, Vec3 source) {
        if (interval == 0 && !hitByWindow.computeIfAbsent(key, ignored -> new HashSet<>()).add(player.getId())) return;
        if (interval > 0) {
            String intervalKey = key + ':' + player.getId();
            long last = lastDamageTick.getOrDefault(intervalKey, Long.MIN_VALUE / 2);
            if (now - last < interval) return;
            lastDamageTick.put(intervalKey, now);
        }
        if (!player.hurt(dragon.damageSources().mobAttack(dragon), amount)) return;
        if (key.contains("flank")) {
            Vec3 side = GraveDragonMythic.flank(dragon, action.name(), actionYaw, dragon.position()).direction();
            double speed = player.onGround() ? 4.3 : 2.75;
            player.setDeltaMovement(side.x * speed, 2, side.z * speed);
            player.hurtMarked = true;
            return;
        }
        if (key.contains("breath") || key.contains("firefield") || key.contains("fire_ring")) {
            int seconds = action != null && (action.name().equals("ground_turn_breath")
                    || action.name().equals("phase50_firefield")) ? 15 : 10;
            if (!dragon.headBroken()) seconds *= 2;
            player.igniteForSeconds(seconds);
        }
        if (key.contains("charge") || key.contains("coil") || key.contains("landing") || key.contains("flank") || key.contains("rift")
                || key.contains("tail") || key.contains("dustdive")) {
            if (key.contains("coil") || key.contains("charge")) {
                Vec3 center = key.contains("coil") ? coilCenter() : dragon.chargeBounds().getCenter();
                Vec3 away = player.position().subtract(center).multiply(1, 0, 1).normalize();
                if (away.lengthSqr() < .001) away = forward();
                // Vanilla drag/gravity: about 10 blocks high and 20 blocks outward on flat ground.
                double speed = player.onGround() ? 2.95 : 1.9;
                player.setDeltaMovement(away.x * speed, 1.35, away.z * speed);
                player.hurtMarked = true;
                return;
            }
            Vec3 away = player.position().subtract(source).multiply(1, 0, 1).normalize();
            if (away.lengthSqr() < .001) away = forward();
            player.push(away.x * 1.8, .8, away.z * 1.8);
        }
    }

    private void strikeParts(String key, String group, float amount, int interval, long now, double from, double to) {
        List<ServerPlayer> players = nearbyPlayers(100);
        if (players.isEmpty()) return;
        var transform = GraveDragonPose.modelToEntity(dragon.yBodyRot, dragon.bodyPitch(), dragon.getScale());
        var first = dragon.poseAt(from);
        var last = dragon.poseAt(to);
        double travel = 0;
        for (int i = 0; i < GraveDragonEntity.PART_LABELS.length; i++) {
            if (!GraveDragonActions.damagingPart(group, i)) continue;
            var start = GraveDragonPose.box(first, GraveDragonEntity.PART_LABELS[i],
                    GraveDragonEntity.PART_BOUNDS[i], transform, previousOrigin).corners();
            var end = GraveDragonPose.box(last, GraveDragonEntity.PART_LABELS[i],
                    GraveDragonEntity.PART_BOUNDS[i], transform, dragon.position()).corners();
            for (int corner = 0; corner < start.length; corner++)
                travel = Math.max(travel, start[corner].distanceTo(end[corner]));
        }
        // A three-tick swipe can travel several blocks in one tick. Sample by
        // travelled distance, not just start/middle/end, so thin player boxes are not skipped between samples.
        int samples = Math.max(8, (int) Math.ceil(travel / .2));
        Set<Integer> contacted = new HashSet<>();
        for (int sample = 0; sample <= samples; sample++) {
            double fraction = (double) sample / samples;
            double phase = Mth.lerp(fraction, from, to);
            Vec3 origin = previousOrigin.lerp(dragon.position(), fraction);
            var frame = sample == 0 ? first : sample == samples ? last : dragon.poseAt(phase);
            for (int i = 0; i < GraveDragonEntity.PART_LABELS.length; i++) {
                if (!GraveDragonActions.damagingPart(group, i)) continue;
                OrientedBoundingBox box = GraveDragonPose.box(frame, GraveDragonEntity.PART_LABELS[i],
                        GraveDragonEntity.PART_BOUNDS[i], transform, origin);
                AABB envelope = box.enclosingAabb();
                for (ServerPlayer player : players) {
                    if (contacted.contains(player.getId())) continue;
                    AABB target = player.getBoundingBox().move(
                            Mth.lerp(fraction, player.xo, player.getX()) - player.getX(),
                            Mth.lerp(fraction, player.yo, player.getY()) - player.getY(),
                            Mth.lerp(fraction, player.zo, player.getZ()) - player.getZ());
                    if (envelope.intersects(target) && box.intersects(target)) {
                        contacted.add(player.getId());
                        damage(player, key, amount, interval, now, box.center);
                    }
                }
            }
            if (contacted.size() == players.size()) break;
        }
    }

    private void strikeCharge(String key, float amount, long now) {
        AABB box = dragon.chargeBounds();
        AABB swept = box.minmax(box.move(previousOrigin.subtract(dragon.position())));
        for (ServerPlayer player : nearbyPlayers(100))
            if (swept.intersects(player.getBoundingBox())) damage(player, key, amount, 1, now);
    }

    private Vec3 randomHorizontalDirection() {
        double angle = dragon.getRandom().nextDouble() * Math.PI * 2;
        return new Vec3(Math.cos(angle), 0, Math.sin(angle));
    }

    private List<Vec3> coilBoundary() {
        List<Vec3> points = new ArrayList<>(21);
        points.add(dragon.getWorldParts()[11].getOrientedBox().center);
        points.add(dragon.getWorldParts()[21].getOrientedBox().center);
        for (int i = 10; i >= 0; i--) points.add(dragon.getWorldParts()[i].getOrientedBox().center);
        for (int i = 13; i <= 20; i++) points.add(dragon.getWorldParts()[i].getOrientedBox().center);
        return points;
    }

    private Vec3 coilCenter() {
        List<Vec3> points = coilBoundary();
        Vec3 center = Vec3.ZERO;
        for (Vec3 point : points) center = center.add(point);
        return center.scale(1.0 / points.size());
    }

    private boolean insideCoil(ServerPlayer player, List<Vec3> boundary) {
        AABB target = player.getBoundingBox();
        Vec3 point = target.getCenter();
        boolean inside = false;
        for (int i = 0, j = boundary.size() - 1; i < boundary.size(); j = i++) {
            Vec3 a = boundary.get(i), b = boundary.get(j);
            if ((a.z > point.z) != (b.z > point.z)
                    && point.x < (b.x - a.x) * (point.z - a.z) / (b.z - a.z) + a.x) inside = !inside;
        }
        return inside;
    }

    private void strikeLanding(String key, float amount, long now) {
        Vec3 point;
        if (action.name().equals("ground_slam") || action.name().equals("ground_tailroll"))
            point = dragon.position();
        else if (action.name().equals("air_dive_slam"))
            point = ground(dragon.position());
        else point = activation.points().isEmpty() ? headCenter() : activation.points().getFirst();
        dragon.level().playSound(null, point.x, point.y, point.z, SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE, SOUND_VOLUME, .8F);
        double radius = action.name().equals("air_dive_slam") ? 20 : 10;
        for (ServerPlayer player : nearbyPlayers(45)) {
            if (player.position().multiply(1, 0, 1).distanceToSqr(point.multiply(1, 0, 1)) <= radius * radius
                    && Math.abs(player.getY() - point.y) < 2.5) damage(player, key, amount, 0, now);
        }
    }

    private void strikeBreath(String key, float amount, int interval, long now) {
        GraveDragonBreath.Flow flow = GraveDragonBreath.flow(dragon, action.name(), dragon.animationSeconds(0));
        for (ServerPlayer player : nearbyPlayers(50)) {
            if (GraveDragonBreath.hits(dragon, flow, player)) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 2));
                damage(player, key, dragon.headBroken() ? amount : amount * 2, interval, now);
            }
        }
    }

    private void strikeTailSlam(String key, float amount, long now) {
        if (!GraveDragonActions.crossed(previousSeconds, dragon.animationSeconds(0), 1.5)) return;
        Vec3 point = GraveDragonMythic.ground(dragon, dragon.getWorldParts()[20].getOrientedBox().center);
        dragon.level().playSound(null, point.x, point.y, point.z, SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE, SOUND_VOLUME, .7F);
        for (ServerPlayer player : nearbyPlayers(100))
            if (GraveDragonMythic.areaHits(dragon, point, GraveDragonMythic.TAIL_SLAM_RADIUS, player))
                damage(player, key, amount, 0, now, point);
    }

    private void launchRift(float amount) {
        List<Vec3> origins = activation.points();
        for (int ray = 0; ray < 3; ray++) {
            Vec3 direction = GraveDragonMythic.riftDirection(actionYaw, ray);
            var rock = new GraveDragonRockEntity(dragon.level(), dragon, direction, amount);
            rock.setPos(origins.get(ray).add(0, .01, 0));
            dragon.level().addFreshEntity(rock);
        }
    }

    private void strikeFan(String key, float amount, long now) {
        var fan = GraveDragonMythic.fan(dragon, dragon.animationSeconds(0), dragon.yBodyRot, dragon.position());
        for (ServerPlayer player : nearbyPlayers(100))
            if (GraveDragonMythic.fanHits(dragon, fan, player)) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 2));
                damage(player, key, dragon.headBroken() ? amount : amount * 2, 1, now);
            }
    }

    private void strikeFireRing(String key, float amount, long now, double from, double to) {
        Vec3 center = activation.points().getFirst();
        double first = GraveDragonMythic.fireRingRadius(from);
        double last = GraveDragonMythic.fireRingRadius(to);
        for (ServerPlayer player : nearbyPlayers(100)) {
            double distance = Math.sqrt(horizontalDistanceSqr(center, player.position()));
            if (distance >= GraveDragonMythic.FIRE_RING_START_RADIUS
                    && distance + GraveDragonMythic.FIRE_RING_HALF_WIDTH >= first
                    && distance - GraveDragonMythic.FIRE_RING_HALF_WIDTH <= last
                    && GraveDragonMythic.areaHits(dragon, center, last + GraveDragonMythic.FIRE_RING_HALF_WIDTH, player)) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 2));
                damage(player, key, dragon.headBroken() ? amount : amount * 2, 1, now);
            }
        }
    }

    private void strikeThunderTrail(String key, float amount, long now) {
        dragon.level().playSound(null, headCenter().x, headCenter().y, headCenter().z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, SOUND_VOLUME, 1F);
        Set<Integer> contacted = new HashSet<>();
        for (Vec3 point : activation.points()) {
            dragon.level().playSound(null, point.x, point.y, point.z, SoundEvents.GENERIC_EXPLODE,
                    SoundSource.HOSTILE, SOUND_VOLUME, .65F);
            for (ServerPlayer player : nearbyPlayers(100))
                if (!contacted.contains(player.getId()) && GraveDragonMythic.areaHits(dragon, point, 5, player)) {
                    contacted.add(player.getId());
                    damage(player, key, amount, 0, now, point);
                }
        }
    }

    private void strikeLightning(long now) {
        if (!GraveDragonActions.crossed(previousSeconds, dragon.animationSeconds(0), 3.2)) return;
        dragon.level().playSound(null, headCenter().x, headCenter().y, headCenter().z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, SOUND_VOLUME, 1F);
        List<ServerPlayer> players = nearbyPlayers(110);
        for (int i = 0; i < activation.points().size(); i++) {
                Vec3 point = activation.points().get(i);
                dragon.level().playSound(null, point.x, point.y, point.z, SoundEvents.GENERIC_EXPLODE,
                        SoundSource.HOSTILE, SOUND_VOLUME, .65F);
                for (ServerPlayer player : players) {
                    if (horizontalDistanceSqr(player.position(), point) > 25) continue;
                    String intervalKey = "lightning:" + player.getId();
                    long last = lastDamageTick.getOrDefault(intervalKey, Long.MIN_VALUE / 2);
                    if (now - last < 12) continue;
                    lastDamageTick.put(intervalKey, now);
                    damage(player, "lightning", 30, 0, now);
                }
        }
    }

    private void strikeFields(long now) {
        if (fireField == null) {
            var flow = GraveDragonBreath.flow(dragon, action.name(), dragon.animationSeconds(0));
            if (flow.ground() == null) return;
            fireField = GraveDragonFireField.spread(dragon.level(), dragon, flow.ground());
            PacketDistributor.sendToPlayersTrackingEntity(dragon,
                    new GraveDragonFireFieldPayload(dragon.getId(), dragon.animationStart(), fireField));
        }
        for (ServerPlayer player : ((ServerLevel)dragon.level()).players()) {
            if (!player.isAlive() || player.isSpectator() || player.isCreative()) continue;
            if (fireField.intersects(player.getBoundingBox())) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 2));
                damage(player, "firefield", dragon.headBroken() ? 3 : 6, 1, now);
            }
        }
    }

    private void launchScheduledFireball() {
        double[] times = action.name().equals("ground_firezone")
                ? new double[]{.7, 2.2, 3.7, 5.2, 6.7} : new double[]{.7, 2.2, 3.7};
        for (int i = 0; i < times.length; i++)
            if (GraveDragonActions.crossed(previousSeconds, dragon.animationSeconds(0), times[i]))
                launchFireball(i);
    }

    private void launchFireball(int shot) {
        GraveDragonBreath.Geometry breath = GraveDragonBreath.geometry(dragon);
        Vec3 mouth = breath.mouth();
        Vec3 direction = GraveDragonFireballEntity.firingDirection(dragon, action.name(), shot,
                dragon.yBodyRot, dragon.position(), mouth);
        boolean blue = !dragon.headBroken();
        float damage = action.name().equals("ground_firezone") ? (blue ? 25 : 15) : (blue ? 40 : 20);
        GraveDragonFireballEntity ball = new GraveDragonFireballEntity(dragon.level(), dragon, direction, damage, blue);
        ball.setPos(mouth.x, mouth.y, mouth.z);
        dragon.level().addFreshEntity(ball);
        dragon.level().playSound(null, mouth.x, mouth.y, mouth.z, SoundEvents.ENDER_DRAGON_SHOOT,
                SoundSource.HOSTILE, SOUND_VOLUME, .8F);
    }

    private void roar() {
        Set<Integer> affected = hitByWindow.computeIfAbsent("roar", ignored -> new HashSet<>());
        for (ServerPlayer player : nearbyPlayers(80)) {
            if (player.distanceToSqr(dragon) <= 80 * 80 && affected.add(player.getId()))
                player.addEffect(new MobEffectInstance(ModEffects.freeze, 140, 0));
        }
    }

    private List<Vec3> pointsFor(String name, ServerPlayer target) {
        return pointsFor(name, target.position());
    }

    private List<Vec3> pointsFor(String name, Vec3 targetPoint) {
        if (name.equals("air_thunder_trail")) return List.of();
        if (name.equals("air_tail_pierce")) return List.of(ground(targetPoint), ground(targetPoint));
        if (name.equals("ground_rift_fan"))
            return GraveDragonMythic.riftOrigins(dragon, dragon.yBodyRot, dragon.position());
        if (name.equals("ground_tail_stab")) return List.of(ground(GraveDragonMythic.part(dragon, name, 1.5, 20,
                dragon.yBodyRot, dragon.bodyPitch(), dragon.position())));
        if (name.equals("air_open_fire_ring")) return List.of(
                GraveDragonMythic.fireRingCenter(dragon, dragon.yBodyRot, dragon.position()));
        Vec3 head = headCenter();
        Vec3 forward = targetPoint.subtract(head).multiply(1, 0, 1).normalize();
        if (forward.lengthSqr() < .001) forward = forward();
        Vec3 side = new Vec3(forward.z, 0, -forward.x);
        if (name.equals("air_coil_lightning")) {
            return lightningPoints(targetPoint);
        }
        if (name.equals("phase50_firefield")) return fieldPoints();
        if (name.equals("ground_firezone")) return List.of(targetPoint);
        if (name.equals("air_fireball3")) return List.of(targetPoint);
        if (name.equals("ground_charge")) {
            double distance = Math.min(45, Math.max(20, head.distanceTo(targetPoint)));
            return List.of(ground(new Vec3(head.x + forward.x * distance, targetPoint.y, head.z + forward.z * distance)));
        }
        if (name.equals("air_dive_slam")) return List.of(targetPoint);
        return List.of(targetPoint);
    }

    private List<Vec3> fieldPoints() {
        GraveDragonBreath.Geometry breath = GraveDragonBreath.geometry(dragon);
        Vec3 direction = breath.direction().multiply(1, 0, 1).normalize();
        if (direction.lengthSqr() < .001) direction = forward();
        Vec3 point = breath.mouth().add(direction.scale(20));
        return List.of(ground(new Vec3(point.x, dragon.combatGroundLevel(), point.z)));
    }

    private List<Vec3> lightningPoints(ServerPlayer target) { return lightningPoints(target.position()); }

    private List<Vec3> lightningPoints(Vec3 target) {
        Vec3 center = ground(target);
        List<Vec3> points = new ArrayList<>(11);
        points.add(center);
        for (int i = 0; i < 120 && points.size() < 11; i++) {
            double angle = dragon.getRandom().nextDouble() * Math.PI * 2;
            double radius = Math.sqrt(dragon.getRandom().nextDouble()) * 30;
            Vec3 point = ground(center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius));
            // Five-block warning/hit circles must not overlap, including the one at the player's feet.
            if (points.stream().anyMatch(existing -> horizontalDistanceSqr(existing, point) < 100)) continue;
            if (supported(point) && clearLine(point.add(0, 2, 0), point.add(0, 12, 0))) points.add(point);
        }
        return points;
    }

    private boolean supported(Vec3 point) {
        BlockPos block = BlockPos.containing(point.x, point.y - .1, point.z);
        return dragon.level().getBlockState(block).isFaceSturdy(dragon.level(), block, Direction.UP);
    }

    private Vec3 ground(Vec3 point) {
        // Scan down from the target's floor, never from the highest column block: the tomb's
        // roof is above the fight and must not become a lightning/fire/landing position.
        int top = Mth.floor(point.y + .5) - 1;
        BlockPos.MutableBlockPos support = new BlockPos.MutableBlockPos();
        for (int y = top; y >= Math.max(dragon.level().getMinBuildHeight(), top - 16); y--) {
            support.set(Mth.floor(point.x), y, Mth.floor(point.z));
            if (dragon.level().getBlockState(support).isFaceSturdy(dragon.level(), support, Direction.UP))
                return new Vec3(point.x, y + 1, point.z);
        }
        return point;
    }

    private record Selection(GraveDragonActions.Action action, boolean turn) { }

    private Selection select(ServerPlayer target) {
        List<GraveDragonActions.Action> choices = actionPool(target, false);
        boolean turn = false;
        if (dragon.flying()) {
            if (choices.isEmpty()) {
                // The air combo may turn in place, but never repositions between attacks.
                choices = actionPool(target, true);
                turn = true;
            }
        } else if (choices.stream().noneMatch(candidate -> !candidate.name().equals("takeoff"))) {
            // No ground attack can connect from here: select first, then reposition.
            // Takeoff is a form transition, not an escape from an empty attack pool.
            choices = new ArrayList<>();
            turn = true;
            for (var candidate : GraveDragonActions.ALL.values()) {
                String name = candidate.name();
                if (airAction(name) || name.equals("takeoff") || name.equals("phase50_firefield")
                        || name.equals("dying_thrash") || name.equals("ground_charge_wall")
                        || name.equals("ground_charge_nowall") || recentGroundSkills.contains(historyName(name))) continue;
                if (partsReady(name)) choices.add(candidate);
            }
        }
        int total = choices.stream().mapToInt(candidate -> weight(candidate.name())).sum();
        if (total == 0) return null;
        int roll = dragon.getRandom().nextInt(total);
        for (var candidate : choices)
            if ((roll -= weight(candidate.name())) < 0) return new Selection(candidate, turn);
        return null;
    }

    private List<GraveDragonActions.Action> actionPool(ServerPlayer target, boolean turn) {
        List<GraveDragonActions.Action> choices = new ArrayList<>();
        for (GraveDragonActions.Action candidate : GraveDragonActions.ALL.values()) {
            String name = candidate.name();
            if (name.equals("ground_charge_wall") || name.equals("ground_charge_nowall")
                    || name.equals("phase50_firefield") || name.equals("dying_thrash")) continue;
            if (dragon.flying() != airAction(name) || (dragon.flying() ? recentAirSkills : recentGroundSkills).contains(historyName(name))) continue;
            if (available(name, target, turn)) choices.add(candidate);
        }
        return choices;
    }

    private static int weight(String name) {
        return switch (name) {
            case "ground_double_bite", "ground_tail_stab", "ground_flank_press_l", "ground_flank_press_r", "air_tail_pierce" -> 5;
            case "ground_rift_fan", "ground_fan_breath", "air_open_fire_ring", "air_thunder_trail" -> 3;
            case "ground_charge", "ground_claw_front_l", "ground_claw_front_r",
                 "ground_claw_back_l", "ground_claw_back_r", "ground_slam", "ground_tailsweep" -> 5;
            case "ground_turn_sweep", "ground_turn_breath", "ground_tailroll", "ground_firezone",
                 "air_coil_lightning", "air_fireball3" -> 3;
            case "roar", "roar_air", "takeoff" -> 2;
            default -> 1;
        };
    }

    private static String historyName(String name) {
        return name.startsWith("ground_flank_press_") ? "ground_flank_press" : name;
    }

    private boolean partsReady(String name) {
        String required = switch (name) {
            case "ground_claw_front_l" -> "front_l";
            case "ground_claw_front_r" -> "front_r";
            case "ground_claw_back_l" -> "hind_l";
            case "ground_claw_back_r" -> "hind_r";
            case "ground_tail_stab", "air_tail_pierce" -> "tail";
            default -> "";
        };
        if (name.equals("ground_rift_fan")) return !dragon.partBroken("front_l") && !dragon.partBroken("front_r");
        return required.isEmpty() || !dragon.partBroken(required);
    }

    private boolean available(String name, ServerPlayer target, boolean turn) {
        if (!partsReady(name)) return false;
        Vec3 point = target.position();
        Vec3 toward = point.subtract(dragon.position());
        float yaw = turn ? (float) Math.toDegrees(Math.atan2(-toward.x, toward.z)) : dragon.yBodyRot;
        if (Math.abs(Mth.wrapDegrees(yaw - dragon.yBodyRot)) < 8) yaw = dragon.yBodyRot;
        return switch (name) {
            case "ground_double_bite" -> partAttackInRange(name, target, yaw);
            case "ground_flank_press_l", "ground_flank_press_r" -> GraveDragonMythic.flankHits(dragon,
                    GraveDragonMythic.flank(dragon, name, yaw, dragon.position()), target);
            case "ground_tail_stab" -> GraveDragonMythic.areaHits(dragon,
                    GraveDragonMythic.ground(dragon, GraveDragonMythic.part(dragon, name, 1.5, 20, yaw, dragon.bodyPitch(), dragon.position())),
                    GraveDragonMythic.TAIL_SLAM_RADIUS, target);
            case "ground_fan_breath" -> GraveDragonMythic.fanHits(dragon, GraveDragonMythic.fan(dragon, 2, yaw, dragon.position()), target);
            case "ground_rift_fan" -> riftInRange(target, yaw);
            case "air_tail_pierce" -> Math.abs(Mth.wrapDegrees(yaw - (float) Math.toDegrees(Math.atan2(-toward.x, toward.z)))) < 15
                    && clearLine(dragon.position(), target.getEyePosition())
                    && dragon.combatPoseFitsAt(name, 0, yaw, 0, dragon.position());
            case "air_open_fire_ring" -> {
                Vec3 center = GraveDragonMythic.fireRingCenter(dragon, yaw, dragon.position());
                yield GraveDragonMythic.areaHits(dragon, center,
                        GraveDragonMythic.FIRE_RING_RADIUS + GraveDragonMythic.FIRE_RING_HALF_WIDTH, target)
                        && horizontalDistanceSqr(point, center) >= GraveDragonMythic.FIRE_RING_START_RADIUS
                        * GraveDragonMythic.FIRE_RING_START_RADIUS;
            }
            case "air_thunder_trail" -> horizontalDistanceSqr(point, dragon.position()) <= 10000
                    && dragon.combatPoseFits(name, 0, 2, 5.2, 8.4);
            case "ground_claw_front_l", "ground_claw_front_r", "ground_claw_back_l", "ground_claw_back_r" ->
                    partAttackInRange(name, target, yaw);
            case "ground_tailsweep", "ground_tailroll" -> {
                yield partAttackInRange(name, target, yaw)
                        || name.equals("ground_tailroll") && horizontalDistanceSqr(point, dragon.position()) <= 100
                        && Math.abs(point.y - dragon.getY()) < 2.5;
            }
            case "ground_slam" -> horizontalDistanceSqr(point, dragon.position()) <= 100;
            case "ground_turn_sweep", "ground_turn_breath", "ground_sweep_breath" ->
                    GraveDragonBreath.canReach(dragon, name, target, yaw);
            case "ground_firezone" -> GraveDragonFireballEntity.canReach(dragon, name, target, yaw, dragon.position());
            case "air_fireball3" -> {
                var frame = GraveDragonPose.sample(name, .7, false);
                Vec3 mouth = GraveDragonBreath.geometry(frame,
                        GraveDragonPose.modelToEntity(yaw, dragon.bodyPitch(), dragon.getScale()), dragon.position(), name).mouth();
                yield horizontalDistanceSqr(point, mouth) <= 1600
                        && GraveDragonFireballEntity.canReach(dragon, name, target, yaw, dragon.position());
            }
            case "ground_charge" -> {
                Vec3 head = partCenterAtYaw(11, yaw);
                double distance = horizontalDistanceSqr(point, head);
                Vec3 facing = new Vec3(-Math.sin(Math.toRadians(yaw)), 0, Math.cos(Math.toRadians(yaw)));
                Vec3 offset = point.subtract(head).multiply(1, 0, 1);
                double along = offset.dot(facing);
                yield distance >= 144 && distance <= 1600 && along > 0
                        && offset.subtract(facing.scale(along)).lengthSqr() <= 16
                        && clearLine(head, target.getEyePosition())
                        && chargeCorridorClear(point.subtract(head).multiply(1, 0, 1).normalize(), 8);
            }
            case "ground_coil" -> horizontalDistanceSqr(point, dragon.position()) <= 900 && coilSpaceClear();
            case "takeoff" -> dragon.canTakeoffForCombat();
            case "air_dive_slam" -> dragon.combatPoseFits(name, 0, 1.5, 2.5, 3.25, 3.9);
            case "air_coil_lightning" -> horizontalDistanceSqr(point, headCenter()) <= 10000
                    && dragon.combatPoseFits(name, 0, 1.8, 3.2) && lightningPoints(target).size() == 11;
            case "roar", "roar_air" -> horizontalDistanceSqr(point, dragon.position()) <= 6400;
            default -> false;
        };
    }

    private boolean partAttackInRange(String name, ServerPlayer player, float yaw) {
        var transform = GraveDragonPose.modelToEntity(yaw, dragon.bodyPitch(), dragon.getScale());
        for (var window : GraveDragonActions.get(name).windows()) {
            if (window.part().equals("landing")) continue;
            int samples = (int) Math.ceil((window.to() - window.from()) * 80);
            for (int sample = 0; sample <= samples; sample++) {
                double seconds = Mth.lerp((double) sample / samples, window.from(), window.to());
                var frame = GraveDragonPose.sample(name, seconds, false);
                for (int i = 0; i < GraveDragonEntity.PART_LABELS.length; i++) {
                    if (!GraveDragonActions.damagingPart(window.part(), i)) continue;
                    Vec3 origin = name.equals("ground_double_bite") ? dragon.position().add(
                            GraveDragonMythic.forward(yaw).scale(GraveDragonMythic.biteTravel(seconds))) : dragon.position();
                    var box = GraveDragonPose.box(frame, GraveDragonEntity.PART_LABELS[i],
                            GraveDragonEntity.PART_BOUNDS[i], transform, origin);
                    if (box.intersects(player.getBoundingBox())) return true;
                }
            }
        }
        return false;
    }

    private boolean riftInRange(ServerPlayer target, float yaw) {
        List<Vec3> origins = GraveDragonMythic.riftOrigins(dragon, yaw, dragon.position());
        for (int ray = 0; ray < 3; ray++) {
            Vec3 direction = GraveDragonMythic.riftDirection(yaw, ray);
            if (GraveDragonRockEntity.canReach(dragon, origins.get(ray), direction, target)) return true;
        }
        return false;
    }

    private Vec3 partCenterAtYaw(int index, float yaw) {
        return dragon.position().add(dragon.getWorldParts()[index].getOrientedBox().center.subtract(dragon.position())
                .yRot((float) Math.toRadians(dragon.yBodyRot - yaw)));
    }

    private boolean chargeCorridorClear(Vec3 direction, double distance) {
        for (double step = 8; step <= distance; step += 8) {
            Vec3 travel = direction.scale(step);
            for (int index : new int[]{8, 9, 10, 11, 12, 21}) {
                OrientedBoundingBox box = dragon.getWorldParts()[index].getOrientedBox().move(travel);
                for (var shape : dragon.level().getBlockCollisions(dragon, box.enclosingAabb()))
                    for (AABB block : shape.toAabbs()) if (box.intersects(block)) return false;
            }
        }
        return true;
    }

    private boolean coilSpaceClear() {
        Vec3 center = dragon.position().add(0, 2, 0);
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            Vec3 edge = center.add(Math.cos(angle) * 12, 0, Math.sin(angle) * 12);
            if (!clearLine(center, edge) || !supported(ground(edge.subtract(0, 2, 0)))) return false;
        }
        return true;
    }

    private ServerPlayer nearestPlayer(double distance) {
        ServerPlayer nearest = null;
        double best = Math.min(distance, 100) * Math.min(distance, 100) + 1.0E-6;
        for (ServerPlayer player : targetingPlayers()) {
            double d = player.distanceToSqr(dragon);
            if (d < best) { best = d; nearest = player; }
        }
        return nearest;
    }

    private List<ServerPlayer> targetingPlayers() {
        return nearbyPlayers(100).stream().filter(player -> player.distanceToSqr(dragon) <= 10000).toList();
    }

    private List<ServerPlayer> nearbyPlayers(double distance) {
        if (!(dragon.level() instanceof ServerLevel level)) return List.of();
        List<ServerPlayer> players = new ArrayList<>();
        double max = distance * distance;
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator() || player.isCreative()) continue;
            if (player.position().distanceToSqr(dragon.position()) <= max * 4) players.add(player);
        }
        return players;
    }

    private Vec3 headCenter() { return dragon.getWorldParts()[11].getOrientedBox().center; }
    private static boolean airAction(String name) { return name.startsWith("air_") || name.equals("roar_air"); }
    private Vec3 forward() { double yaw = Math.toRadians(dragon.yBodyRot); return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)); }
    private boolean clearLine(Vec3 from, Vec3 to) {
        HitResult hit = dragon.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, dragon));
        return hit.getType() == HitResult.Type.MISS;
    }
    private static double horizontalDistanceSqr(Vec3 a, Vec3 b) {
        double x = a.x - b.x, z = a.z - b.z;
        return x * x + z * z;
    }
}
