package icu.icuqalt10.panlingre.client.sound;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.boss.PanGuEntity;
import icu.icuqalt10.panlingre.init.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = PanlingRE.MODID, value = Dist.CLIENT)
public final class PanGuMusicManager {
    private static PanGuMusicInstance currentMusic;

    private PanGuMusicManager() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            stopCurrentMusic();
            return;
        }

        if (currentMusic != null && currentMusic.isStopped()) {
            currentMusic = null;
        }

        if (currentMusic == null && hasLivingPanGu(minecraft)) {
            currentMusic = new PanGuMusicInstance(minecraft);
            minecraft.getSoundManager().play(currentMusic);
        }
    }

    private static boolean hasLivingPanGu(Minecraft minecraft) {
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof PanGuEntity panGu && isActiveBoss(panGu)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isActiveBoss(PanGuEntity panGu) {
        return panGu.isAlive()
                && !panGu.isRemoved()
                && panGu.getActionState() != PanGuEntity.ActionState.DYING;
    }

    private static void stopCurrentMusic() {
        if (currentMusic != null) {
            currentMusic.stopMusic();
            currentMusic = null;
        }
    }

    private static final class PanGuMusicInstance extends AbstractTickableSoundInstance {
        private static final double HEARING_RADIUS_SQR = 80.0D * 80.0D;
        private static final int DEATH_FADE_TICKS = 56;

        private final Minecraft minecraft;
        private boolean fadingOut;
        private boolean fadeIsAudible;
        private int fadeTicksRemaining = DEATH_FADE_TICKS;

        private PanGuMusicInstance(Minecraft minecraft) {
            super(ModSounds.PAN_GU_BGM.get(), SoundSource.RECORDS, RandomSource.create());
            this.minecraft = minecraft;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.0F;
            this.pitch = 1.0F;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public boolean canStartSilent() {
            // The track may start while the player is outside 80 blocks and become audible later.
            return true;
        }

        @Override
        public void tick() {
            if (this.minecraft.level == null || this.minecraft.player == null) {
                this.stop();
                return;
            }

            boolean hasLivingBoss = false;
            boolean livingBossIsInRange = false;
            boolean hasDyingBoss = false;
            boolean dyingBossIsInRange = false;
            for (Entity entity : this.minecraft.level.entitiesForRendering()) {
                if (!(entity instanceof PanGuEntity panGu) || !panGu.isAlive() || panGu.isRemoved()) {
                    continue;
                }

                boolean isInRange = this.minecraft.player.distanceToSqr(panGu) <= HEARING_RADIUS_SQR;
                if (panGu.getActionState() == PanGuEntity.ActionState.DYING) {
                    hasDyingBoss = true;
                    dyingBossIsInRange |= isInRange;
                } else {
                    hasLivingBoss = true;
                    livingBossIsInRange |= isInRange;
                }
            }

            // Another living Pan Gu in range takes over the single shared track without fading.
            if (livingBossIsInRange) {
                this.fadingOut = false;
                this.fadeTicksRemaining = DEATH_FADE_TICKS;
                this.volume = 1.0F;
                return;
            }

            if (hasDyingBoss) {
                if (!this.fadingOut) {
                    this.fadingOut = true;
                    this.fadeTicksRemaining = DEATH_FADE_TICKS;
                }
                this.fadeIsAudible = dyingBossIsInRange;
            }

            if (this.fadingOut) {
                this.fadeTicksRemaining = Math.max(0, this.fadeTicksRemaining - 1);
                this.volume = this.fadeIsAudible
                        ? (float) this.fadeTicksRemaining / DEATH_FADE_TICKS
                        : 0.0F;

                if (this.fadeTicksRemaining == 0) {
                    if (!hasLivingBoss) {
                        this.stop();
                    } else {
                        this.fadingOut = false;
                        this.fadeTicksRemaining = DEATH_FADE_TICKS;
                    }
                }
                return;
            }

            if (hasLivingBoss) {
                // No positional attenuation: every player inside 80 blocks hears the same full volume.
                this.volume = 0.0F;
            } else {
                this.stop();
            }
        }

        private void stopMusic() {
            this.stop();
        }
    }
}
