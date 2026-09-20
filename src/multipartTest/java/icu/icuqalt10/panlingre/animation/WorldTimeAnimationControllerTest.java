package icu.icuqalt10.panlingre.animation;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.keyframe.*;
import software.bernie.geckolib.animation.keyframe.event.data.*;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.math.MathValue;
import software.bernie.geckolib.loading.math.value.Constant;
import software.bernie.geckolib.model.GeoModel;
import java.util.*;

/** Exercise real GeckoLib queues/easing, with unrelated client first-render clocks. */
public final class WorldTimeAnimationControllerTest {
    private static final class Probe implements GeoAnimatable {
        double time;
        WorldTimeAnimationController.Playback playback = WorldTimeAnimationController.Playback.loop("clip", 1000);
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {}
        @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return null; }
        @Override public double getTick(Object object) { return 0; }
    }

    private static Animation clip() {
        Keyframe<MathValue> ramp = new Keyframe<>(64, new Constant(0), new Constant(64));
        KeyframeStack<Keyframe<MathValue>> position = new KeyframeStack<>(List.of(ramp), List.of(ramp), List.of(ramp));
        return new Animation("clip", 64, Animation.LoopType.LOOP,
                new BoneAnimation[]{new BoneAnimation("probe", new KeyframeStack<>(), position, new KeyframeStack<>())},
                new Animation.Keyframes(new SoundKeyframeData[]{new SoundKeyframeData(4d, "old"), new SoundKeyframeData(24d, "current")},
                        new ParticleKeyframeData[0], new CustomInstructionKeyframeData[0]));
    }

    private static final GeoModel<Probe> MODEL = new GeoModel<>() {
        private final Animation clip = clip();
        @Override public ResourceLocation getModelResource(Probe probe) { return ResourceLocation.parse("panlingre:test"); }
        @Override public ResourceLocation getTextureResource(Probe probe) { return getModelResource(probe); }
        @Override public ResourceLocation getAnimationResource(Probe probe) { return getModelResource(probe); }
        @Override public Animation getAnimation(Probe probe, String name) { return clip; }
    };
    private static final Map<String, GeoBone> BONES = Map.of("probe", new GeoBone(null, "probe", false, null, false, false));
    private static int checks;
    private static void check(boolean test, String message) { checks++; if (!test) throw new AssertionError(message); }
    private static void near(double actual, double expected) { check(Math.abs(actual-expected) < 1e-6, actual+" != "+expected); }
    private static WorldTimeAnimationController<Probe> controller(Probe probe) {
        return new WorldTimeAnimationController<>(probe, "test", () -> probe.playback, state -> probe.time + state.getPartialTick());
    }
    private static void process(Probe probe, WorldTimeAnimationController<Probe> controller, double time, double clientTick) {
        probe.time = Math.floor(time);
        var state = new AnimationState<>(probe, 0, 0, (float)(time-Math.floor(time)), false);
        controller.process(MODEL, state, BONES, Map.of(), clientTick, true);
    }
    private static double position(WorldTimeAnimationController<Probe> controller) {
        return EasingType.lerpWithOverride(controller.getBoneAnimationQueues().get("probe").positionXQueue().poll(), null);
    }

    public static void run() {
        Probe original = new Probe(), late = new Probe();
        var a = controller(original); var b = controller(late);
        List<String> sounds = new ArrayList<>();
        b.setSoundKeyframeHandler(event -> sounds.add(event.getKeyframeData().getSound()));
        process(original, a, 1000, 0);
        near(position(a), 0);
        process(original, a, 1084.5, 84.5);
        process(late, b, 1084.5, 0);
        near(position(a), 20.5); near(position(b), 20.5);
        check(sounds.isEmpty(), "Late join replays historical sound");
        process(late, b, 1087, 2.5);
        process(late, b, 1088, 3.5);
        check(sounds.equals(List.of("current")), "Future sound lost or historical sound replayed");
        process(late, b, 1089, 4.5);
        check(sounds.size() == 1, "Event replayed every render");
        var retracked = controller(late);
        process(late, retracked, 1100.25, 0);
        near(position(retracked), 36.25);
        // A second entity rendered through the same GeoModel has an independent phase.
        Probe other = new Probe(); other.playback = WorldTimeAnimationController.Playback.loop("clip", 1090);
        var c = controller(other); process(other, c, 1100.25, 9999); near(position(c), 10.25);
        process(late, b, 1128.25, 0); near(position(b), 0.25);
        late.playback = WorldTimeAnimationController.Playback.loop("clip", 1130);
        process(late, b, 1130, 500); near(position(b), 0);
        late.playback = new WorldTimeAnimationController.Playback("clip", 1130, true, 0.75);
        process(late, b, 1140, 0); near(position(b), 7.5);
        late.playback = WorldTimeAnimationController.Playback.once("clip", 1000);
        process(late, b, 1080, 0);
        check(b.getAnimationState() == AnimationController.State.STOPPED && b.getBoneAnimationQueues().isEmpty(),
                "Expired one-shot replays for new observer");
        late.playback = WorldTimeAnimationController.Playback.once("clip", 1200);
        process(late, b, 1220.5, 0); near(position(b), 20.5);
        late.playback = WorldTimeAnimationController.Playback.once("", -1);
        process(late, b, 1221, 1);
        check(b.getAnimationState() == AnimationController.State.STOPPED && b.getBoneAnimationQueues().isEmpty(), "Stop not applied");
        System.out.println("PASS: " + checks + " GeckoLib late-join/retracking/loop/one-shot/event checks.");
    }
}
