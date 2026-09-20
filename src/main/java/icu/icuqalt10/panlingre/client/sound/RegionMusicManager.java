package icu.icuqalt10.panlingre.client.sound;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.init.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

@EventBusSubscriber(modid = PanlingRE.MODID, value = Dist.CLIENT)
public final class RegionMusicManager {
    private static final ResourceLocation MIDDLE_BIOME = ResourceLocation.fromNamespaceAndPath("plre", "middle");
    private static final ResourceLocation PENGLAI_BIOME = ResourceLocation.fromNamespaceAndPath("plre", "peng_lai");

    private static final Region[] BIOME_REGIONS = {
            new Region("middle", 175, 128, -63, 320, ModSounds.BGM_MIDDLE, MIDDLE_BIOME),
            new Region("penglai", 354, 128, -689, 160, ModSounds.BGM_PENGLAI, PENGLAI_BIOME)
    };

    private static final Region[] COORDINATE_REGIONS = {
            // Fallback coordinates keep BGM working if a custom biome is not
            // available in the client's synced biome registry yet.
            new Region("ren", 1711, 128, 159, 208, ModSounds.BGM_REN),
            new Region("xian", 3247, 128, 911, 256, ModSounds.BGM_XIAN),
            new Region("shen", 3247, 128, 287, 208, ModSounds.BGM_SHEN),
            new Region("yao", 2735, 128, 927, 208, ModSounds.BGM_YAO),
            new Region("zhan", 3247, 128, -224, 208, ModSounds.BGM_ZHAN)
    };

    private static RegionMusicInstance current;
    private static Region desired;

    private RegionMusicManager() {}

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (current != null) current.stopMusic();
            current = null;
            desired = null;
            return;
        }

        desired = findBiomeRegion(mc);
        if (desired == null) {
            desired = findCoordinateRegion(mc.player.position());
        }
        if (current != null && current.isStopped()) current = null;

        if (current == null) {
            if (desired != null) {
                current = new RegionMusicInstance(desired);
                mc.getSoundManager().play(current);
            }
        } else if (current.region != desired && !current.isFadingOut()) {
            current.fadeOut();
        }
    }

    private static Region findBiomeRegion(Minecraft mc) {
        Vec3 pos = mc.player.position();
        var biome = mc.level.getBiome(BlockPos.containing(pos)).unwrapKey();
        for (Region region : BIOME_REGIONS) {
            if (biome.isPresent() && biome.get().location().equals(region.biomeId)) {
                return region;
            }
        }
        return null;
    }

    private static Region findCoordinateRegion(Vec3 pos) {
        Region best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Region region : COORDINATE_REGIONS) {
            double distance = pos.distanceToSqr(region.x, region.y, region.z);
            if (distance <= region.radius * region.radius && distance < bestDistance) {
                best = region;
                bestDistance = distance;
            }
        }
        return best;
    }

    private record Region(String name, double x, double y, double z, double radius,
                          DeferredHolder<SoundEvent, SoundEvent> sound,
                          ResourceLocation biomeId) {
        private Region(String name, double x, double y, double z, double radius,
                       DeferredHolder<SoundEvent, SoundEvent> sound) {
            this(name, x, y, z, radius, sound, null);
        }
    }

    private static final class RegionMusicInstance extends AbstractTickableSoundInstance {
        private static final int FADE_IN_TICKS = 20;
        private static final int FADE_OUT_TICKS = 20;
        private final Region region;
        private int fadeTicks;
        private boolean fadingOut;

        private RegionMusicInstance(Region region) {
            super(region.sound.get(), SoundSource.RECORDS, RandomSource.create());
            this.region = region;
            this.looping = true;
            this.volume = 0.0F;
            this.pitch = 1.0F;
            this.x = region.x;
            this.y = region.y;
            this.z = region.z;
            // The manager applies the region radius itself; keep the track at a
            // stable volume instead of applying Minecraft's short linear falloff.
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public boolean canStartSilent() { return true; }

        @Override
        public void tick() {
            if (fadingOut) {
                fadeTicks = Math.max(0, fadeTicks - 1);
                volume = fadeTicks / (float) FADE_OUT_TICKS;
                if (fadeTicks == 0) stop();
            } else {
                fadeTicks = Math.min(FADE_IN_TICKS, fadeTicks + 1);
                volume = fadeTicks / (float) FADE_IN_TICKS;
            }
        }

        private void fadeOut() {
            if (!fadingOut) {
                fadingOut = true;
                fadeTicks = Math.max(1, Math.round(volume * FADE_OUT_TICKS));
            }
        }

        private boolean isFadingOut() { return fadingOut; }
        private void stopMusic() { stop(); }
    }
}
