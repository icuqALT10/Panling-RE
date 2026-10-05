package icu.icuqalt10.panlingre.client.sound;

import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonActions;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonBreath;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import java.util.HashMap;
import java.util.Map;

/** One continuous fire loop follows the head for the entire breath window. */
public final class GraveDragonBreathSound extends AbstractTickableSoundInstance {
    private static final Map<Integer, GraveDragonBreathSound> PLAYING = new HashMap<>();
    private final GraveDragonEntity dragon;
    private final String action;
    private final long startedAt;

    /** Sound follows the synced entity state even if there is no active particle timeline. */
    public static void update(ClientLevel level) {
        var manager = Minecraft.getInstance().getSoundManager();
        PLAYING.values().removeIf(sound -> {
            if (level != sound.dragon.level() || sound.dragon.isRemoved() || !sound.dragon.isAlive()
                    || !sound.action.equals(sound.dragon.animation())
                    || sound.startedAt != sound.dragon.animationStart() || !isBreathing(sound.dragon)) {
                sound.stopBreath();
                manager.stop(sound);
                return true;
            }
            return false;
        });
        if (level == null) return;
        for (var entity : level.entitiesForRendering()) {
            if (!(entity instanceof GraveDragonEntity dragon) || !isBreathing(dragon)) continue;
            var sound = PLAYING.get(dragon.getId());
            if (sound == null || sound.isStopped() || level.getGameTime() % 20 == 0 && !manager.isActive(sound)) {
                sound = new GraveDragonBreathSound(dragon);
                PLAYING.put(dragon.getId(), sound);
                manager.play(sound);
            }
        }
    }

    public GraveDragonBreathSound(GraveDragonEntity dragon) {
        super(SoundEvents.FIRE_AMBIENT, SoundSource.HOSTILE, RandomSource.create());
        this.dragon = dragon;
        this.action = dragon.animation();
        this.startedAt = dragon.animationStart();
        looping = true;
        delay = 0;
        attenuation = SoundInstance.Attenuation.LINEAR;
        volume = 5F;
        pitch = .7F;
        tick();
    }

    public static boolean isBreathing(GraveDragonEntity dragon) {
        var action = GraveDragonActions.get(dragon.animation());
        if (action == null) return false;
        double seconds = dragon.animationSeconds(0);
        return action.windows().stream().anyMatch(window ->
                (window.part().equals("breath") || window.part().equals("firefield") || window.part().equals("fan_breath")
                        || window.part().equals("fire_ring") && seconds < 2.2)
                        && seconds >= window.from() && seconds < window.to());
    }

    @Override public boolean canStartSilent() { return true; }
    @Override public boolean canPlaySound() { return !dragon.isSilent(); }

    @Override public void tick() {
        if (Minecraft.getInstance().level != dragon.level() || !dragon.isAlive() || dragon.isRemoved()
                || !action.equals(dragon.animation()) || startedAt != dragon.animationStart()
                || !isBreathing(dragon)) {
            stop();
            return;
        }
        var head = GraveDragonBreath.geometry(dragon).mouth();
        x = head.x;
        y = head.y;
        z = head.z;

    }

    public void stopBreath() { stop(); }
}
