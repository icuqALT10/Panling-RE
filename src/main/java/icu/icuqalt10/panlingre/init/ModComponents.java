package icu.icuqalt10.panlingre.init;

import com.mojang.serialization.Codec;
import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModComponents {

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, PanlingRE.MODID);


    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IS_POWERED =
            COMPONENTS.register("is_powered", () ->
                    DataComponentType.<Boolean>builder()
                            .persistent(Codec.BOOL)
                            .networkSynchronized(ByteBufCodecs.BOOL)
                            .build()
            );
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> POWERED_TIMER =
            COMPONENTS.register("powered_timer", () ->
                    DataComponentType.<Long>builder()
                            .persistent(Codec.LONG)
                            .build()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> KEY_TYPE =
            COMPONENTS.register("key_type", () ->
                    DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> KEY_ID =
            COMPONENTS.register("key_id", () ->
                    DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build()
            );
}