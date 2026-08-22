package icu.icuqalt10.panlingre.client;

import icu.icuqalt10.panlingre.network.SkillCastStatePayload;
import icu.icuqalt10.panlingre.network.SkillCastReleasePayload;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/** Client-side view of both vanilla hand casts and synchronized wheel casts. */
public final class ClientSkillCastState {
    private static final long RAISE_DURATION_MILLIS = 140L;
    private static final long RELEASE_DURATION_MILLIS = 320L;
    private static final long RETURN_DURATION_MILLIS = 180L;
    private static final float THROW_FRACTION = 0.28F;

    private static final Map<Integer, TimedCast> SYNCHRONIZED_CASTS = new HashMap<>();
    private static final Map<Integer, EndingAnimation> ENDING_ANIMATIONS = new HashMap<>();

    private ClientSkillCastState() {
    }

    public static void update(SkillCastStatePayload payload) {
        if (payload.durationTicks() <= 0) {
            TimedCast removed = SYNCHRONIZED_CASTS.remove(payload.entityId());
            if (removed != null) {
                long now = Util.getMillis();
                ENDING_ANIMATIONS.put(payload.entityId(), new EndingAnimation(
                        now, RETURN_DURATION_MILLIS, removed.hand(), false,
                        getRaiseProgress(now - removed.startMillis())));
            }
            return;
        }

        long durationMillis = payload.durationTicks() * 50L;
        ENDING_ANIMATIONS.remove(payload.entityId());
        SYNCHRONIZED_CASTS.put(payload.entityId(), new TimedCast(
                Util.getMillis(), durationMillis,
                payload.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND));
    }

    public static void startRelease(SkillCastReleasePayload payload) {
        SYNCHRONIZED_CASTS.remove(payload.entityId());
        ENDING_ANIMATIONS.put(payload.entityId(), new EndingAnimation(
                Util.getMillis(), RELEASE_DURATION_MILLIS,
                payload.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND,
                true, 1.0F));
    }

    public static @Nullable CastView getCastView(Player player, float partialTick) {
        TimedCast cast = SYNCHRONIZED_CASTS.get(player.getId());
        if (cast == null) return null;

        long elapsedMillis = Math.max(0L, Util.getMillis() - cast.startMillis());
        if (elapsedMillis >= cast.durationMillis()) {
            SYNCHRONIZED_CASTS.remove(player.getId());
            return null;
        }

        float progress = elapsedMillis / (float) cast.durationMillis();
        float remainingSeconds = (cast.durationMillis() - elapsedMillis) / 1000.0F;
        return new CastView(Mth.clamp(progress, 0.0F, 1.0F), remainingSeconds, cast.hand());
    }

    public static @Nullable AnimationView getAnimationView(Player player) {
        int entityId = player.getId();
        long now = Util.getMillis();
        TimedCast cast = SYNCHRONIZED_CASTS.get(entityId);
        if (cast != null) {
            long elapsedMillis = Math.max(0L, now - cast.startMillis());
            if (elapsedMillis < cast.durationMillis()) {
                return new AnimationView(
                        getRaiseProgress(elapsedMillis), 0.0F, cast.hand());
            }
            SYNCHRONIZED_CASTS.remove(entityId);
        }

        EndingAnimation ending = ENDING_ANIMATIONS.get(entityId);
        if (ending == null) return null;

        long elapsedMillis = Math.max(0L, now - ending.startMillis());
        if (elapsedMillis >= ending.durationMillis()) {
            ENDING_ANIMATIONS.remove(entityId);
            return null;
        }

        float progress = Mth.clamp(
                elapsedMillis / (float) ending.durationMillis(), 0.0F, 1.0F);
        if (!ending.throwFirst()) {
            return new AnimationView(
                    ending.initialRaise() * (1.0F - smoothstep(progress)),
                    0.0F, ending.hand());
        }

        if (progress < THROW_FRACTION) {
            float throwProgress = easeOutCubic(progress / THROW_FRACTION);
            return new AnimationView(1.0F, throwProgress, ending.hand());
        }

        float returnProgress = (progress - THROW_FRACTION) / (1.0F - THROW_FRACTION);
        float remaining = 1.0F - smoothstep(returnProgress);
        return new AnimationView(remaining, remaining, ending.hand());
    }

    public static void clear(int entityId) {
        SYNCHRONIZED_CASTS.remove(entityId);
        ENDING_ANIMATIONS.remove(entityId);
    }

    private static float getRaiseProgress(long elapsedMillis) {
        return smoothstep(Mth.clamp(
                elapsedMillis / (float) RAISE_DURATION_MILLIS, 0.0F, 1.0F));
    }

    private static float smoothstep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static float easeOutCubic(float value) {
        float inverse = 1.0F - Mth.clamp(value, 0.0F, 1.0F);
        return 1.0F - inverse * inverse * inverse;
    }

    private record TimedCast(long startMillis, long durationMillis, InteractionHand hand) {
    }

    private record EndingAnimation(long startMillis, long durationMillis,
                                   InteractionHand hand, boolean throwFirst,
                                   float initialRaise) {
    }

    public record CastView(float progress, float remainingSeconds, InteractionHand hand) {
    }

    public record AnimationView(float raiseProgress, float throwProgress,
                                InteractionHand hand) {
    }
}
