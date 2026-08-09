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

    public static final DeferredHolder<SoundEvent, SoundEvent> PAN_GU_BGM = SOUND_EVENTS.register(
            "boss.pan_gu",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "boss.pan_gu")
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

    private ModSounds() {
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
