package icu.icuqalt10.panlingre;

import com.mojang.logging.LogUtils;
import icu.icuqalt10.panlingre.client.ClientModEvents;
import icu.icuqalt10.panlingre.event.ModBusEvents;
import icu.icuqalt10.panlingre.init.*;
import icu.icuqalt10.panlingre.subtool.opengui.OpenGuiSubTool;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(PanlingRE.MODID)
public class PanlingRE {
    public static final String MODID = "panlingre";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PanlingRE(IEventBus modEventBus, ModContainer modContainer) {
        // 注册
        ModComponents.COMPONENTS.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModAttributes.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);
        ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenus.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModSounds.register(modEventBus);
        ModTabs.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);

        modEventBus.addListener(ModBusEvents::onAttributeModification);
        modEventBus.addListener(ModBusEvents::commonSetup);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ClientModEvents::register);
        }

        modEventBus.addListener(ModNetworks::register);

        //子功能
        OpenGuiSubTool.init(modEventBus);
    }




}
