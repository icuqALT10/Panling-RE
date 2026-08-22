package icu.icuqalt10.panlingre.init;

import com.mojang.serialization.Codec;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.component.FuZhiBagContents;
import icu.icuqalt10.panlingre.component.RightClickComponent;
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

    /** 0 = inactive, 1 = pojun, 2 = jinzhong. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DI_SHI_DUN_FORM =
            COMPONENTS.register("di_shi_dun_form", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .build()
            );

    /** 0 = inactive, 1 = sniper, 2 = ranger. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TIAN_XING_JIAN_FORM =
            COMPONENTS.register("tian_xing_jian_form", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .build()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> HIDDEN_ENCHANTMENTS_INITIALIZED =
            COMPONENTS.register("hidden_enchantments_initialized", () ->
                    DataComponentType.<Boolean>builder()
                            .persistent(Codec.BOOL)
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

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RightClickComponent>> RIGHT_CLICK =
            COMPONENTS.register("right_click", () ->
                    DataComponentType.<RightClickComponent>builder()
                            .persistent(RightClickComponent.CODEC)
                            .networkSynchronized(RightClickComponent.STREAM_CODEC)
                            .build()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FuZhiBagContents>> FU_ZHI_BAG_CONTENTS =
            COMPONENTS.register("fu_zhi_bag_contents", () ->
                    DataComponentType.<FuZhiBagContents>builder()
                            .persistent(FuZhiBagContents.CODEC)
                            .networkSynchronized(FuZhiBagContents.STREAM_CODEC)
                            .cacheEncoding()
                            .build()
            );
}
