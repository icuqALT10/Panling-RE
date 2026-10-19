package icu.icuqalt10.panlingre.init;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, PanlingRE.MODID);

    public static final java.util.Map<String, DeferredHolder<SoundEvent, SoundEvent>> GRAVE_DRAGON_BREATHS = java.util.Map.of(
            "ground_fan_breath", registerBreath("ground_fan_breath"),
            "air_open_fire_ring", registerBreath("air_open_fire_ring"),
            "phase50_firefield", registerBreath("phase50_firefield"),
            "ground_turn_sweep", registerBreath("ground_turn_sweep"),
            "ground_turn_breath", registerBreath("ground_turn_breath"),
            "ground_sweep_breath", registerBreath("ground_sweep_breath"));

    public static final DeferredHolder<SoundEvent, SoundEvent> GRAVE_DRAGON_DEATH_CRY = SOUND_EVENTS.register(
            "boss.grave_dragon.death_cry", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "boss.grave_dragon.death_cry")));

    private static DeferredHolder<SoundEvent, SoundEvent> registerBreath(String action) {
        String name = "boss.grave_dragon.breath." + action;
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, name)));
    }

    public static final DeferredHolder<SoundEvent, SoundEvent> PAN_GU_BGM = SOUND_EVENTS.register(
            "boss.pan_gu",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "boss.pan_gu")
            )
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> SHI_HUANG_BGM = SOUND_EVENTS.register(
            "boss.shihuang", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "boss.shihuang")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SHI_HUANG_BGM_LOOP = SOUND_EVENTS.register(
            "boss.shihuang.loop", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "boss.shihuang.loop")));

    public static final DeferredHolder<SoundEvent, SoundEvent> SISHOU_BGM = SOUND_EVENTS.register(
            "instance.sishou",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "instance.sishou")
            )
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> YS_JIN = SOUND_EVENTS.register(
            "yuansu.jin",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "yuansu.jin")
            )
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> YS_MU = SOUND_EVENTS.register(
            "yuansu.mu",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "yuansu.mu")
            )
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> YS_SHUI = SOUND_EVENTS.register(
            "yuansu.shui",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "yuansu.shui")
            )
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> YS_HUO = SOUND_EVENTS.register(
            "yuansu.huo",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "yuansu.huo")
            )
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> YS_TU = SOUND_EVENTS.register(
            "yuansu.tu",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "yuansu.tu")
            )
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> BGM_REN = registerBgm("bgm_ren");
    public static final DeferredHolder<SoundEvent, SoundEvent> BGM_ZHAN = registerBgm("bgm_zhan");
    public static final DeferredHolder<SoundEvent, SoundEvent> BGM_XIAN = registerBgm("bgm_xian");
    public static final DeferredHolder<SoundEvent, SoundEvent> BGM_YAO = registerBgm("bgm_yao");
    public static final DeferredHolder<SoundEvent, SoundEvent> BGM_SHEN = registerBgm("bgm_shen");
    public static final DeferredHolder<SoundEvent, SoundEvent> BGM_MIDDLE = registerBgm("bgm_middle");
    public static final DeferredHolder<SoundEvent, SoundEvent> BGM_PENGLAI = registerBgm("bgm_penglai");

    private static DeferredHolder<SoundEvent, SoundEvent> registerBgm(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, name)));
    }

    private ModSounds() {
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
