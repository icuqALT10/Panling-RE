package icu.icuqalt10.panlingre.client;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.client.gui.dztScreen;
import icu.icuqalt10.panlingre.client.gui.ldlScreen;
import icu.icuqalt10.panlingre.client.gui.zftScreen;
import icu.icuqalt10.panlingre.init.*;
import icu.icuqalt10.panlingre.renderer.FeiXianJianZhenRenderer;
import icu.icuqalt10.panlingre.renderer.XingHaiRenderer;
import icu.icuqalt10.panlingre.renderer.ldlCurioRenderer;
import icu.icuqalt10.panlingre.renderer.ldlRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

@EventBusSubscriber(modid = "panlingre", value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.ldl_be.get(),
                ldlRenderer::new
        );
        event.registerEntityRenderer(ModEntities.PO_DI_FU.get(), ThrownItemRenderer::new);

        event.registerEntityRenderer(ModEntities.CUSTOM_PELLET.get(), ThrownItemRenderer::new);

        event.registerEntityRenderer(ModEntities.FEI_XIAN_JIAN_ZHEN.get(), FeiXianJianZhenRenderer::new);
        event.registerEntityRenderer(ModEntities.XING_HAI.get(), XingHaiRenderer::new);
    }

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH) ||
                event.getName().equals(VanillaGuiLayers.FOOD_LEVEL) ||
                event.getName().equals(VanillaGuiLayers.ARMOR_LEVEL) ||
                event.getName().equals(VanillaGuiLayers.AIR_LEVEL)) {
            event.setCanceled(true);
        }
    }

    public static void register(RegisterGuiLayersEvent event) {

        event.registerAbove(
                VanillaGuiLayers.FOOD_LEVEL,
                ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "lingqi_hud"),
                new RpgHudOverlay()
        );
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ldl_menu.get(), ldlScreen::new);
        event.register(ModMenus.zft_menu.get(), zftScreen::new);
        event.register(ModMenus.dzt_menu.get(), dztScreen::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        //拉弓动画渲染
        event.enqueueWork(() -> {
            ModItems.ITEMS.getEntries().forEach(entry -> {
                Item item = entry.get();
                if (item instanceof BowItem) {
                    registerBowProperties(item);
                }
            });

            //弩动画渲染
            ModItems.ITEMS.getEntries().forEach(entry -> {
                Item item = entry.get();
                if (item instanceof CrossbowItem) {
                    registerCrossbowProperties(item);
                }
            });

            //竹子渲染
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.bamboo_block.get(), RenderType.cutout());

            //炼丹炉方块
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ldl.get(), RenderType.cutout());

            //定海神针 齐天
            ItemProperties.register(
                    ModItems.ding_hai_shen_zhen.get(),
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "powered"),
                    (stack, level, entity, seed) ->
                            stack.getOrDefault(ModComponents.IS_POWERED.get(), false) ? 1.0F : 0.0F
            );
        });
        //炼丹炉渲染
        CuriosRendererRegistry.register(ModItems.huang_tong_lu.get(), ldlCurioRenderer::new);
        CuriosRendererRegistry.register(ModItems.jing_tie_lu.get(), ldlCurioRenderer::new);
        CuriosRendererRegistry.register(ModItems.chi_tong_lu.get(), ldlCurioRenderer::new);
        CuriosRendererRegistry.register(ModItems.suo_hun_lu.get(), ldlCurioRenderer::new);
        CuriosRendererRegistry.register(ModItems.qi_sha_din.get(), ldlCurioRenderer::new);
        CuriosRendererRegistry.register(ModItems.hun_yuan_shen_din.get(), ldlCurioRenderer::new);
    }
    private static void registerBowProperties(Item item) {
        ItemProperties.register(item, ResourceLocation.withDefaultNamespace("pull"),
                (stack, level, entity, seed) -> {
                    if (entity == null) return 0.0F;
                    if (entity.getUseItem() != stack) return 0.0F;
                    return (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F;
                });

        ItemProperties.register(item, ResourceLocation.withDefaultNamespace("pulling"),
                (stack, level, entity, seed) -> {
                    return entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F;
                });
    }
    public static void registerCrossbowProperties(Item item) {
        ItemProperties.register(item, ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) -> {
            if (entity == null) return 0.0F;
            return CrossbowItem.isCharged(stack) ? 0.0F :
                    (float)(stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / (float)CrossbowItem.getChargeDuration(stack, entity);
        });

        ItemProperties.register(item, ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) -> {
            return entity != null && entity.isUsingItem() && entity.getUseItem() == stack && !CrossbowItem.isCharged(stack) ? 1.0F : 0.0F;
        });

        ItemProperties.register(item, ResourceLocation.withDefaultNamespace("charged"), (stack, level, entity, seed) -> {
            return entity != null && CrossbowItem.isCharged(stack) ? 1.0F : 0.0F;
        });

        ItemProperties.register(item, ResourceLocation.withDefaultNamespace("firework"), (stack, level, entity, seed) -> {
            if (entity == null || !CrossbowItem.isCharged(stack)) {
                return 0.0F;
            }
            var chargedProjectiles = stack.get(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES);
            if (chargedProjectiles != null && !chargedProjectiles.isEmpty()) {
                return chargedProjectiles.contains(Items.FIREWORK_ROCKET) ? 1.0F : 0.0F;
            }
            return 0.0F;
        });
    }
}