package icu.icuqalt10.panlingre.init;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.effect.CommonEffect;
import icu.icuqalt10.panlingre.effect.FreezeEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, PanlingRE.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> jia_yu = EFFECTS.register("jia_yu",
            () -> new CommonEffect(MobEffectCategory.NEUTRAL, 0x99453A)
                    .addAttributeModifier(
                            Attributes.ARMOR,
                            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "jia_yu"),
                            10.0,
                            AttributeModifier.Operation.ADD_VALUE
                    )
    );

    public static final DeferredHolder<MobEffect, MobEffect> po_jia = EFFECTS.register("po_jia",
            () -> new CommonEffect(MobEffectCategory.HARMFUL, 0x5A6C81)
                    .addAttributeModifier(
                            Attributes.ARMOR,
                            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "po_jia"),
                            -10.0,
                            AttributeModifier.Operation.ADD_VALUE
                    )
    );

    public static final DeferredHolder<MobEffect, MobEffect> freeze = EFFECTS.register("freeze", FreezeEffect::new);
}
