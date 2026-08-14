package icu.icuqalt10.panlingre.init;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModAttributes {
    // 1. 创建注册表容器
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(BuiltInRegistries.ATTRIBUTE, PanlingRE.MODID);

    //注册
    public static final DeferredHolder<Attribute, Attribute> MAX_LINGQI = ATTRIBUTES.register("max_lingqi",
            () -> new RangedAttribute(
                    "description.PanlingRE.max_lingqi",
                    20.0,
                    0.0,
                    100000000.0
            ).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> FALIZHI = ATTRIBUTES.register("falizhi",
            () -> new RangedAttribute(
                    "description.PanlingRE.falizhi",
                    0.0,
                    0.0,
                    9999.99
            ).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> ARROW_DAMAGE = ATTRIBUTES.register("arrow_damage",
            () -> new RangedAttribute(
                    "description.PanlingRE.arrow_damage",
                    0.0,
                    0.0,
                    100000000.0
            ).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> MAGIC_DAMAGE = ATTRIBUTES.register("magic_damage",
            () -> new RangedAttribute(
                    "description.PanlingRE.magic_damage",
                    0.0,
                    0.0,
                    100000000.0
            ).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> COOLDOWN_REMOVE = ATTRIBUTES.register("cooldown_remove",
            () -> new RangedAttribute(
                    "description.PanlingRE.cooldown_remove",
                    0.0,
                    -100.0,
                    1.0
            ).setSyncable(true)
    );
    public static final DeferredHolder<Attribute, Attribute> LING_QI_RECOVERY = ATTRIBUTES.register("ling_qi_recovery",
            () -> new RangedAttribute(
                    "description.PanlingRE.ling_qi_recovery",
                    0.05,
                    -1.0,
                    1.0
            ).setSyncable(true)
    );

    public static void register(IEventBus eventBus) {
        ATTRIBUTES.register(eventBus);
    }
}
