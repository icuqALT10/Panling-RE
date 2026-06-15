package icu.icuqalt10.panlingre.event;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.init.ModAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

public class ModBusEvents {

    @SubscribeEvent
    public static void onAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, ModAttributes.MAX_LINGQI);
        event.add(EntityType.PLAYER, ModAttributes.FALIZHI);
        event.add(EntityType.PLAYER, ModAttributes.COOLDOWN_REMOVE);
        event.add(EntityType.PLAYER, ModAttributes.ARROW_DAMAGE);
        event.add(EntityType.PLAYER, ModAttributes.MAGIC_DAMAGE);

        event.add(EntityType.SKELETON, ModAttributes.ARROW_DAMAGE);
        event.add(EntityType.STRAY, ModAttributes.ARROW_DAMAGE);
        event.add(EntityType.WITHER_SKELETON, ModAttributes.ARROW_DAMAGE);
    }
    //修改力量buff
    @SubscribeEvent
    public static void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            MobEffect strength = MobEffects.DAMAGE_BOOST.value();

            strength.addAttributeModifier(
                    ModAttributes.ARROW_DAMAGE,
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "strength_arrow_damage"),
                    3.0,
                    AttributeModifier.Operation.ADD_VALUE
            );

            strength.addAttributeModifier(
                    ModAttributes.MAGIC_DAMAGE,
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "strength_magic_damage"),
                    3.0,
                    AttributeModifier.Operation.ADD_VALUE
            );
        });
    }
}