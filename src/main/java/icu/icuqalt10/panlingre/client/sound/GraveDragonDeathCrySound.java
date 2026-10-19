package icu.icuqalt10.panlingre.client.sound;

import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import icu.icuqalt10.panlingre.init.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** A single sustained cry follows the head through recoil and fades after landing. */
public final class GraveDragonDeathCrySound extends AbstractTickableSoundInstance {
    private final GraveDragonEntity dragon;
    private final long startedAt;

    public GraveDragonDeathCrySound(GraveDragonEntity dragon) {
        super(ModSounds.GRAVE_DRAGON_DEATH_CRY.get(), SoundSource.HOSTILE, RandomSource.create());
        this.dragon = dragon;
        startedAt = dragon.animationStart();
        looping = false;
        attenuation = SoundInstance.Attenuation.LINEAR;
        volume = .85F;
        pitch = 1F;
        tick();
    }

    @Override public boolean canStartSilent() { return true; }
    @Override public boolean canPlaySound() { return !dragon.isSilent(); }

    @Override public void tick() {
        if (Minecraft.getInstance().level != dragon.level() || dragon.isRemoved() || !dragon.isAlive()
                || !dragon.animation().equals("escape_wall_impact") || dragon.animationStart() != startedAt
                || dragon.animationSeconds(0)>=2.25) {
            stop();
            return;
        }
        var head = dragon.getWorldParts()[11].getOrientedBox().center;
        x = head.x; y = head.y; z = head.z;
    }
}
