package icu.icuqalt10.panlingre.client;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.client.gui.dztScreen;
import icu.icuqalt10.panlingre.client.gui.FuZhiBagScreen;
import icu.icuqalt10.panlingre.client.gui.ldlScreen;
import icu.icuqalt10.panlingre.client.gui.zftScreen;
import icu.icuqalt10.panlingre.client.layer.FireTornadoWindLayer;
import icu.icuqalt10.panlingre.client.models.FireTornadoModel;
import icu.icuqalt10.panlingre.client.renderer.*;
import icu.icuqalt10.panlingre.client.task.TaskGuideOverlay;
import icu.icuqalt10.panlingre.client.renderer.boss.PanGuRenderer;
import icu.icuqalt10.panlingre.init.*;
import icu.icuqalt10.panlingre.item.fuzhi.FuZhiBagItem;
import icu.icuqalt10.panlingre.looktip.LookTipOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@EventBusSubscriber(modid = PanlingRE.MODID, value = Dist.CLIENT)
public class ClientModEvents {

    private static final Pattern ATTRIBUTE_NUMBER =
            Pattern.compile("([+-]?)(\\d+(?:\\.\\d+)?)");

    //视场角抖动
    private static final List<ShakeEffect> shakeEffects = new ArrayList<>();

    private static class ShakeEffect {
        Vec3 center;
        double radius;
        int remainingTicks;
        float intensity;

        ShakeEffect(Vec3 center, double radius, int ticks, float intensity) {
            this.center = center;
            this.radius = radius;
            this.remainingTicks = ticks;
            this.intensity = intensity;
        }
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.ldl_be.get(),
                ldlRenderer::new
        );
        event.registerEntityRenderer(ModEntities.PO_DI_FU.get(), ThrownItemRenderer::new);

        event.registerEntityRenderer(ModEntities.CUSTOM_PELLET.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(
                ModEntities.HUO_QIU_FU.get(),
                HuoQiuFuRenderer::new
        );
        event.registerEntityRenderer(ModEntities.JIN_LI_REN.get(), JinLiRenRenderer::new);

        event.registerEntityRenderer(ModEntities.FEI_XIAN_JIAN_ZHEN.get(), FeiXianJianZhenRenderer::new);
        event.registerEntityRenderer(ModEntities.XING_HAI.get(), XingHaiRenderer::new);

        event.registerEntityRenderer(ModEntities.PAN_GU.get(), PanGuRenderer::new);
        //event.registerEntityRenderer(ModEntities.PAN_GU_LARGE.get(), PanGuLargeRenderer::new);
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

        event.registerAbove(
                VanillaGuiLayers.FOOD_LEVEL,
                ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "skill_icon_overlay"),
                new SkillIconOverlay()
        );

        event.registerAbove(
                VanillaGuiLayers.CROSSHAIR,
                ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "skill_cast_overlay"),
                SkillCastOverlay.INSTANCE
        );

        event.registerAbove(
                VanillaGuiLayers.FOOD_LEVEL,
                ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "skill_wheel_overlay"),
                SkillWheelOverlay.INSTANCE
        );

        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "look_tip_overlay"),
                LookTipOverlay.INSTANCE
        );

        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "task_guide_overlay"),
                TaskGuideOverlay.INSTANCE
        );
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ldl_menu.get(), ldlScreen::new);
        event.register(ModMenus.zft_menu.get(), zftScreen::new);
        event.register(ModMenus.dzt_menu.get(), dztScreen::new);
        event.register(ModMenus.fu_zhi_bag_menu.get(), FuZhiBagScreen::new);
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
            //逐日 长虹
            ItemProperties.register(
                    ModItems.zhu_ri.get(),
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "powered"),
                    (stack, level, entity, seed) ->
                            stack.getOrDefault(ModComponents.IS_POWERED.get(), false) ? 1.0F : 0.0F
            );

            // 地势盾牌：0 = 未激活，1 = 破军，2 = 金钟。
            ItemProperties.register(
                    ModItems.di_shi_dun.get(),
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "form"),
                    (stack, level, entity, seed) -> entity instanceof net.minecraft.world.entity.player.Player player
                            && player.getOffhandItem() == stack
                            ? stack.getOrDefault(ModComponents.DI_SHI_DUN_FORM.get(), 0)
                            : 0.0F
            );
            ItemProperties.register(
                    ModItems.di_shi_dun.get(),
                    ResourceLocation.withDefaultNamespace("blocking"),
                    (stack, level, entity, seed) -> entity != null
                            && entity.isUsingItem()
                            && entity.getUseItem() == stack ? 1.0F : 0.0F
            );

            //逐日 powered状态下拉弓动画10倍速
            ItemProperties.register(
                    ModItems.zhu_ri.get(),
                    ResourceLocation.withDefaultNamespace("pull"),
                    (stack, level, entity, seed) -> {
                        if (entity == null) return 0.0F;
                        if (entity.getUseItem() != stack) return 0.0F;
                        boolean powered = stack.getOrDefault(ModComponents.IS_POWERED.get(), false);
                        float divisor = powered ? 10.0F : 20.0F;
                        return (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / divisor;
                    }
            );

            // loot_key 根据 key_type 切换模型
            ItemProperties.register(
                    ModItems.loot_key.get(),
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "key_type"),
                    (stack, level, entity, seed) -> {
                        String type = stack.getOrDefault(ModComponents.KEY_TYPE.get(), "golden");
                        return switch (type) {
                            case "silver" -> 1.0f;
                            case "copper" -> 2.0f;
                            default -> 0.0f;
                        };
                    }
            );

            // Switch between the empty and filled talisman bag textures.
            ItemProperties.register(
                    ModItems.fu_zhi_bao.get(),
                    ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "filled"),
                    (stack, level, entity, seed) ->
                            FuZhiBagItem.getContents(stack).entries().isEmpty() ? 0.0F : 1.0F
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

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // 注册火龙卷模型
        event.registerLayerDefinition(FireTornadoWindLayer.FIRE_TORNADO_LAYER, FireTornadoModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 注册火龙卷渲染器
        event.registerEntityRenderer(ModEntities.FIRE_TORNADO.get(), FireTornadoRenderer::new);
        // 注册朱日流光箭渲染器
        event.registerEntityRenderer(ModEntities.ZHU_RI_ARROW.get(), ZhuRiArrowRenderer::new);
        event.registerEntityRenderer(ModEntities.YS_MU_HEALING.get(), YsMuHealingRenderer::new);
        event.registerEntityRenderer(ModEntities.TU_BARRIER.get(), TuBarrierRenderer::new);
        event.registerEntityRenderer(ModEntities.YS3_JIN_TORNADO.get(), Ys3JinTornadoRenderer::new);
        event.registerEntityRenderer(ModEntities.YS3_MU_DOMAIN.get(), Ys3MuDomainRenderer::new);
        event.registerEntityRenderer(ModEntities.YS3_SHUI_DOMAIN.get(), Ys3ShuiDomainRenderer::new);
        event.registerEntityRenderer(ModEntities.YS3_HUO_DOMAIN.get(), Ys3HuoDomainRenderer::new);
    }

    /**
     * 处理can_break提示 - Shift显示详细列表，否则显示简略提示
     */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        formatCooldownRemoveTooltip(event.getToolTip());

        ItemStack stack = event.getItemStack();

        // 检查物品是否有can_break组件
        if (!stack.has(DataComponents.CAN_BREAK)) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        boolean isShiftPressed = Screen.hasShiftDown();

        if (isShiftPressed) {
            // Shift按下：保持原样显示详细列表
            return;
        }

        // 未按Shift：用简略提示替换详细列表
        // 查找"能破坏："这一行的索引
        int canBreakIndex = -1;
        for (int i = 0; i < tooltip.size(); i++) {
            String text = tooltip.get(i).getString();
            if (text.contains("能破坏")) {
                canBreakIndex = i;
                break;
            }
        }

        if (canBreakIndex != -1) {
            // 找到了"能破坏："这一行
            // 计算后面有多少个矿石列表项
            int blockCount = 0;
            int removeEndIndex = canBreakIndex;

            // 向后遍历找出所有矿石列表项
            for (int i = canBreakIndex + 1; i < tooltip.size(); i++) {
                String text = tooltip.get(i).getString();
                // 判断是否是矿石列表项（通常会以矿石名称的格式出现）
                if (text.contains("矿") || text.matches(".*[^：]+矿.*")) {
                    blockCount++;
                    removeEndIndex = i;
                } else if (text.startsWith("minecraft:") || text.contains("_can_break")) {
                    // 如果遇到其他组件或非矿石相关的内容，停止
                    break;
                } else if (text.trim().isEmpty()) {
                    // 空行继续
                    continue;
                } else {
                    // 其他文本内容，停止计数
                    break;
                }
            }

            if (blockCount > 0) {
                // 删除从"能破坏："开始到矿石列表结束的所有行
                for (int i = removeEndIndex; i >= canBreakIndex; i--) {
                    tooltip.remove(i);
                }

                // 插入简略提示
                tooltip.add(canBreakIndex, Component.literal("§6[按住 Shift 查看可破坏方块]"));
            }
        }
    }

    private static void formatCooldownRemoveTooltip(List<Component> tooltip) {
        String attributeName = Component.translatable(
                "description.PanlingRE.cooldown_remove"
        ).getString();

        for (int i = 0; i < tooltip.size(); i++) {
            Component line = tooltip.get(i);
            String text = line.getString();
            if (!text.contains(attributeName)) {
                continue;
            }

            Matcher matcher = ATTRIBUTE_NUMBER.matcher(text);
            if (!matcher.find()) {
                continue;
            }

            BigDecimal percent = new BigDecimal(matcher.group(2))
                    .movePointRight(2)
                    .setScale(2, RoundingMode.HALF_UP)
                    .stripTrailingZeros();
            String replacement = text.substring(0, matcher.start())
                    + matcher.group(1) + percent.toPlainString()
                    + "%" + text.substring(matcher.end());
            tooltip.set(i, Component.literal(replacement).withStyle(line.getStyle()));
        }
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



    // 视场角抖动
    public static void startShake(Vec3 center, double radius, int ticks, float intensity) {
        shakeEffects.add(new ShakeEffect(center, radius, ticks, intensity));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        shakeEffects.removeIf(effect -> {
            effect.remainingTicks--;
            return effect.remainingTicks <= 0;
        });
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (shakeEffects.isEmpty()) {
            return;
        }

        Vec3 cameraPos = event.getCamera().getPosition();
        float totalDelta = 0.0f;
        float totalDeltaPitch = 0.0f;

        for (ShakeEffect effect : shakeEffects) {
            double distance = cameraPos.distanceTo(effect.center);
            if (distance <= effect.radius) {
                totalDelta += (float) Math.random() * effect.intensity - (effect.intensity / 2.0f);
                totalDeltaPitch += (float) Math.random() * effect.intensity - (effect.intensity / 2.0f);
            }
        }

        if (totalDelta != 0.0f || totalDeltaPitch != 0.0f) {
            event.setPitch(event.getPitch() + totalDeltaPitch * 1.5f);
            event.setYaw(event.getYaw() + totalDelta);
        }
    }

}
