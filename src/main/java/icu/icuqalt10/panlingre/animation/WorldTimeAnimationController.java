package icu.icuqalt10.panlingre.animation;

import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

/**
 * GeckoLib 4 adapter for a server-owned single animation clip (loop or once).
 * Synchronize the clip name and start game tick as entity data. A newly tracking
 * client then samples the current phase on its FIRST render; trigger packets and
 * the client entity's tickCount are not used as an animation clock.
 * Multi-stage sequences should publish a new Playback at each server transition.
 */
public final class WorldTimeAnimationController<T extends GeoAnimatable> extends AnimationController<T> {
    public record Playback(String animation, long startedAt, boolean loop, double speed) {
        public static Playback loop(String name, long start) { return new Playback(name, start, true, 1); }
        public static Playback once(String name, long start) { return new Playback(name, start, false, 1); }
        public boolean active() { return !animation.isEmpty() && startedAt >= 0 && speed > 0; }
    }

    private final Supplier<Playback> playback;
    private final ToDoubleFunction<AnimationState<T>> worldTime;
    private Playback previous;
    private long cycle = -1;
    private double sampleTick;

    public WorldTimeAnimationController(T animatable, String name, Supplier<Playback> playback,
                                       ToDoubleFunction<AnimationState<T>> worldTime) {
        super(animatable, name, 0, state -> PlayState.CONTINUE);
        this.playback = playback;
        this.worldTime = worldTime;
    }

    /** Current clip-local tick, useful for keeping other visuals aligned. */
    public double getPlaybackTick() { return sampleTick; }

    @Override
    public void process(GeoModel<T> model, AnimationState<T> state, Map<String, GeoBone> bones,
                        Map<String, BoneSnapshot> snapshots, double seekTime, boolean crashWhenCantFindBone) {
        Playback next = playback.get();
        boneAnimationQueues.clear();
        if (next == null || !next.active()) {
            stop();
            previous = null;
            currentAnimation = null;
            return;
        }
        Animation animation = model.getAnimation(animatable, next.animation());
        if (animation == null || !(animation.length() > 0)) {
            stop();
            currentAnimation = null;
            return;
        }
        double elapsed = Math.max(0, worldTime.applyAsDouble(state) - next.startedAt()) * next.speed();
        if (!next.loop() && elapsed >= animation.length()) {
            stop();
            previous = next;
            currentAnimation = null;
            return; // Joining after a one-shot finished must not replay it.
        }
        long nextCycle = next.loop() ? (long)Math.floor(elapsed / animation.length()) : 0;
        boolean changed = !Objects.equals(previous, next) || needsAnimationReload || currentAnimation == null
                || currentAnimation.animation() != animation;
        boolean newCycle = cycle != nextCycle;
        double oldTick = sampleTick;
        sampleTick = next.loop() ? elapsed % animation.length() : elapsed;
        if (changed || newCycle) {
            // Use GeckoLib's zero-length transition to initialize snapshots and
            // reset its private event set, then evaluate the desired phase in this
            // same render. No reflection or private-field mixin is required.
            double phase = sampleTick;
            sampleTick = 0;
            currentAnimation = null;
            animationQueue.clear();
            animationQueue.add(new AnimationProcessor.QueuedAnimation(animation,
                    next.loop() ? Animation.LoopType.LOOP : Animation.LoopType.PLAY_ONCE));
            animationState = State.TRANSITIONING;
            justStartedTransition = true;
            shouldResetTick = true;
            lastPollTime = Double.NaN;
            super.process(model, state, bones, snapshots, seekTime, crashWhenCantFindBone);
            sampleTick = phase;
        }
        // An observer joining late should not replay old sounds/particles/commands.
        // On an ordinary loop boundary, still allow the new cycle's early events.
        boolean resuming = changed || nextCycle > cycle + 1 || (!newCycle && sampleTick - oldTick > 2);
        final double eventFloor = resuming && sampleTick > 1 ? sampleTick : 0;
        previous = next;
        cycle = nextCycle;
        currentAnimation = new AnimationProcessor.QueuedAnimation(animation,
                next.loop() ? Animation.LoopType.LOOP : Animation.LoopType.PLAY_ONCE);
        animationQueue.clear();
        animationState = State.RUNNING;
        shouldResetTick = false;
        justStartedTransition = false;
        needsAnimationReload = false;
        // Let GeckoLib evaluate its own keyframes/easing, with the server phase.
        var sound = soundKeyframeHandler;
        var particle = particleKeyframeHandler;
        var custom = customKeyframeHandler;
        soundKeyframeHandler = event -> {
            if (sound != null && event.getKeyframeData().getStartTick() >= eventFloor) sound.handle(event);
        };
        particleKeyframeHandler = event -> {
            if (particle != null && event.getKeyframeData().getStartTick() >= eventFloor) particle.handle(event);
        };
        customKeyframeHandler = event -> {
            if (custom != null && event.getKeyframeData().getStartTick() >= eventFloor) custom.handle(event);
        };
        try {
            super.process(model, state, bones, snapshots, seekTime, crashWhenCantFindBone);
        } finally {
            soundKeyframeHandler = sound;
            particleKeyframeHandler = particle;
            customKeyframeHandler = custom;
        }
    }

    @Override protected double adjustTick(double ignoredClientTick) { return sampleTick; }
}
