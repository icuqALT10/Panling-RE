package icu.icuqalt10.panlingre.client.sound;

import icu.icuqalt10.panlingre.PanlingRE;
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
public final class QinglongMusicManager {
    private static QinglongMusicInstance currentMusic;

    private QinglongMusicManager() {
    }

    public static void handle(boolean start) {
        Minecraft minecraft = Minecraft.getInstance();
        if (start) {
            if (currentMusic != null) currentMusic.stopMusic();
            currentMusic = new QinglongMusicInstance();
            minecraft.getSoundManager().play(currentMusic);
        } else if (currentMusic != null) {
            currentMusic.fadeOut();
        }
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            if (currentMusic != null) currentMusic.stopMusic();
            currentMusic = null;
        } else if (currentMusic != null && currentMusic.isStopped()) {
            currentMusic = null;
        }
    }

    private static final class QinglongMusicInstance extends AbstractTickableSoundInstance {
        private static final int HALF_VOLUME_TICKS = 5 * 20;
        private static final int FULL_VOLUME_TICKS = 7 * 20;
        private static final int FADE_OUT_TICKS = 2 * 20;

        private int age;
        private int fadeTicksRemaining;
        private float fadeStartVolume;
        private boolean fadingOut;

        private QinglongMusicInstance() {
            super(ModSounds.SISHOU_BGM.get(), SoundSource.RECORDS, RandomSource.create());
            this.looping = true;
            this.delay = 0;
            this.volume = 0.0F;
            this.pitch = 1.0F;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (fadingOut) {
                fadeTicksRemaining = Math.max(0, fadeTicksRemaining - 1);
                volume = fadeStartVolume * fadeTicksRemaining / (float) FADE_OUT_TICKS;
                if (fadeTicksRemaining == 0) stop();
                return;
            }

            age++;
            if (age <= HALF_VOLUME_TICKS) {
                volume = 0.5F * age / HALF_VOLUME_TICKS;
            } else if (age <= FULL_VOLUME_TICKS) {
                volume = 0.5F + 0.5F * (age - HALF_VOLUME_TICKS)
                        / (FULL_VOLUME_TICKS - HALF_VOLUME_TICKS);
            } else {
                volume = 1.0F;
            }
        }

        private void fadeOut() {
            if (fadingOut) return;
            fadingOut = true;
            fadeStartVolume = volume;
            fadeTicksRemaining = FADE_OUT_TICKS;
        }

        private void stopMusic() {
            stop();
        }
    }
}
