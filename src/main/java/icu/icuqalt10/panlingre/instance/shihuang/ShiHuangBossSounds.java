package icu.icuqalt10.panlingre.instance.shihuang;

import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonEntity;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonActions;
import icu.icuqalt10.panlingre.init.ModSounds;
import icu.icuqalt10.panlingre.instance.InstanceManager;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import java.util.HashSet;
import java.util.Set;

/** Arena mix for both tomb bosses. Delivery does not depend on entity tracking distance. */
public final class ShiHuangBossSounds {
    private SoundEvent sustained;
    private long startedAt;
    private final Set<ServerPlayer> listeners = new HashSet<>();
    private long oneShotTick = Long.MIN_VALUE;
    private final Set<SoundEvent> oneShots = new HashSet<>();

    public void playInArena(ShiHuangScene scene, net.minecraft.server.level.ServerLevel level,
                            SoundEvent sound, SoundSource source, float volume, float pitch) {
        if (oneShotTick != level.getGameTime()) { oneShots.clear(); oneShotTick = level.getGameTime(); }
        // Four footfalls or several simultaneous explosions share one arena cue.
        if (!oneShots.add(sound)) return;
        for (var player : level.players()) if (scene.bounds().contains(player.position()))
            player.playNotifySound(sound,source,Math.min(1F,volume / 5F),pitch);
    }

    public static void play(Entity owner, Level level, double x, double y, double z,
                            net.minecraft.core.Holder<SoundEvent> sound, SoundSource source, float volume, float pitch) {
        play(owner,level,x,y,z,sound.value(),source,volume,pitch);
    }

    public static void play(Entity owner, Level level, double x, double y, double z,
                            SoundEvent sound, SoundSource source, float volume, float pitch) {
        var session = owner instanceof LivingEntity living ? InstanceManager.sessionFor(living) : null;
        if (session != null && session.controller() instanceof ShiHuangController tomb) {
            tomb.playArenaSound(sound,source,volume,pitch);
        } else level.playSound(null, x, y, z, sound, source, volume, pitch);
    }

    /** Full recordings are sent once; cancellation stops them for every recipient. */
    public void tick(GraveDragonEntity dragon, ShiHuangScene scene) {
        SoundEvent wanted = null;
        double seconds = dragon.animationSeconds(0);
        var action = GraveDragonActions.get(dragon.animation());
        if (dragon.isAlive() && !dragon.isRemoved() && !dragon.isSilent()) {
            if (action != null && ModSounds.GRAVE_DRAGON_BREATHS.containsKey(dragon.animation())
                    && action.windows().stream().anyMatch(w -> seconds >= w.from() && seconds < w.to()
                    && (w.part().equals("breath") || w.part().equals("firefield") || w.part().equals("fan_breath")
                    || w.part().equals("fire_ring") && seconds < 2.2)))
                wanted = ModSounds.GRAVE_DRAGON_BREATHS.get(dragon.animation()).get();
            if (dragon.animation().equals("escape_wall_impact") && seconds >= .25 && seconds < 2.25)
                wanted = ModSounds.GRAVE_DRAGON_DEATH_CRY.get();
        }
        if (wanted != sustained || startedAt != dragon.animationStart()) {
            stop(); sustained = wanted; startedAt = dragon.animationStart();
        }
        if (sustained == null) return;
        listeners.removeIf(p -> {
            if (p.level() == dragon.level() && scene.bounds().contains(p.position())) return false;
            p.connection.send(new ClientboundStopSoundPacket(sustained.getLocation(), SoundSource.HOSTILE));
            return true;
        });
        for (var player : ((net.minecraft.server.level.ServerLevel)dragon.level()).players())
            if (scene.bounds().contains(player.position()) && listeners.add(player))
                player.playNotifySound(sustained, SoundSource.HOSTILE, .85F, 1F);
    }

    public void stop() {
        if (sustained != null) for (var player : listeners)
            player.connection.send(new ClientboundStopSoundPacket(sustained.getLocation(), SoundSource.HOSTILE));
        listeners.clear(); sustained = null;
    }
}
