package icu.icuqalt10.panlingre.client.sound;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangEntity;
import icu.icuqalt10.panlingre.init.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = PanlingRE.MODID, value = Dist.CLIENT)
public final class ShiHuangMusicManager {
    private static ShiHuangEntity boss;
    private static Music current;

    private ShiHuangMusicManager() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (current != null) mc.getSoundManager().stop(current);
            current = null;
            boss = null;
            return;
        }
        if (mc.isPaused()) return;
        if (boss == null) {
            for (var entity : mc.level.entitiesForRendering()) {
                if (entity instanceof ShiHuangEntity candidate && candidate.entranceTicks(0) >= 0
                        && candidate.isAlive() && !candidate.dying()) {
                    boss = candidate;
                    current = new Music(false);
                    mc.getSoundManager().play(current);
                    break;
                }
            }
        }
        if (current == null) return;
        boolean active = boss.level() == mc.level && boss.isAlive() && !boss.isRemoved() && !boss.dying();
        if (!active) current.fadeOut();
        // Wait for the audio channel to end, not a tick-based estimate of the track duration.
        if (current.isStopped() || !mc.getSoundManager().isActive(current)) {
            if (active && !current.fadingOut) {
                current = new Music(true);
                mc.getSoundManager().play(current);
            } else {
                current = null;
                boss = null;
            }
        }
        if (current != null) mc.getMusicManager().stopPlaying();
    }

    private static final class Music extends AbstractTickableSoundInstance {
        private static final float VOLUME = 0.5F;
        private boolean fadingOut;
        private int fadeTicks = 60;

        private Music(boolean repeat) {
            super((repeat ? ModSounds.SHI_HUANG_BGM_LOOP : ModSounds.SHI_HUANG_BGM).get(),
                    SoundSource.RECORDS, RandomSource.create());
            // The streamed files contain sample-accurate 3s tails; only the loop has a 3s intro fade.
            looping = repeat;
            volume = VOLUME;
            pitch = 1;
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
        }

        private void fadeOut() { fadingOut = true; }

        @Override public void tick() {
            if (fadingOut) {
                volume = VOLUME * Math.max(0, --fadeTicks) / 60F;
                if (fadeTicks <= 0) stop();
            }
        }
    }
}
