package icu.icuqalt10.panlingre.client;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.init.ModEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class RpgHudOverlay implements LayeredDraw.Layer {
    private static final String HUD_PATH = "textures/gui/hud/";
    private static final String HEALTH_PATH = HUD_PATH + "health/";

    private static final int BAR_WIDTH = 81;
    private static final int BAR_HEIGHT = 9;
    private static final int BAR_TEXTURE_WIDTH = 160;
    private static final int BAR_TEXTURE_HEIGHT = 22;
    private static final int SOURCE_CAP_WIDTH = 8;
    private static final int DISPLAY_CAP_WIDTH = 3;
    private static final int BLINK_INTERVAL_TICKS = 3;
    private static final int HEALTH_BLINK_TICKS = 20;
    private static final int OTHER_BAR_BLINK_TICKS = 10;
    private static final float VALUE_ANIMATION_SPEED = 8.0F;
    private static final float VALUE_SNAP_EPSILON = 0.01F;

    private static final BarTextures HEALTH = healthTextures("health");
    private static final BarTextures POISONED = healthTextures("poisoned");
    private static final BarTextures WITHERED = healthTextures("withered");
    private static final BarTextures FROZEN = healthTextures("frozen");
    private static final BarTextures ABSORBING = healthTextures("absorbing");
    private static final BarTextures HUNGRY = textures(HUD_PATH, "hungry");
    private static final BarTextures LINGQI = textures(HUD_PATH, "lingqi");

    private static final ResourceLocation ARMOR_ICON = loc(HUD_PATH + "armor.png");
    private static final ResourceLocation MANA_ICON = loc(HUD_PATH + "falizhi.png");
    private static final ResourceLocation OXYGEN_ICON = loc(HUD_PATH + "oxygen.png");

    private Player trackedPlayer;
    private float lastHealth;
    private float lastMaxHealth;
    private float lastAbsorption;
    private int lastFood;
    private float lastSaturation;
    private float lastLingQi;
    private float lastMaxLingQi;
    private int healthBlinkUntil;
    private int hungryBlinkUntil;
    private int lingQiBlinkUntil;
    private float displayedHealth;
    private float displayedFood;
    private float displayedLingQi;
    private long lastAnimationMillis;

    private static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, path);
    }

    private static BarTextures healthTextures(String name) {
        return textures(HEALTH_PATH, name);
    }

    private static BarTextures textures(String path, String name) {
        return new BarTextures(
                loc(path + name + "_empty.png"),
                loc(path + name + "_full.png"),
                loc(path + name + "_empty_blinking.png"),
                loc(path + name + "_full_blinking.png")
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) return;

        Player player = minecraft.player;
        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int leftX = centerX - 91;
        int rightX = centerX + 10;
        int rowBottomY = screenHeight - 39;
        int rowTopY = screenHeight - 49;

        float currentLingQi = LingQiData.ClientLingQiData.getCurrent();
        float maxLingQi = LingQiData.ClientLingQiData.getMax();
        updateBlinkTimers(player, currentLingQi, maxLingQi);
        updateDisplayedValues(player.getHealth(), player.getFoodData().getFoodLevel(), currentLingQi);

        float absorption = player.getAbsorptionAmount();
        String healthText = String.format("%.0f/%.0f", displayedHealth, player.getMaxHealth());
        if (absorption > 0) healthText += " \u00a7e+" + (int) absorption;
        drawBar(guiGraphics, leftX, rowBottomY, displayedHealth, player.getMaxHealth(),
                healthTexturesFor(player), isBlinking(player.tickCount, healthBlinkUntil), healthText, 0);

        int foodLevel = player.getFoodData().getFoodLevel();
        float saturation = player.getFoodData().getSaturationLevel();
        String foodText = String.format("%.0f/20", displayedFood);
        if (saturation > 0) foodText += " \u00a7e+" + (int) saturation;
        drawBar(guiGraphics, rightX, rowBottomY, displayedFood, 20.0F,
                HUNGRY, isBlinking(player.tickCount, hungryBlinkUntil), foodText, 0);

        drawBar(guiGraphics, rightX, rowTopY, displayedLingQi, maxLingQi,
                LINGQI, isBlinking(player.tickCount, lingQiBlinkUntil),
                String.format("%.0f/%.0f", displayedLingQi, maxLingQi), 0);

        drawIconValue(guiGraphics, leftX, rowTopY, ARMOR_ICON, String.valueOf(player.getArmorValue()));
        float mana = (float) player.getAttributeValue(ModAttributes.FALIZHI);
        drawIconValue(guiGraphics, leftX + 30, rowTopY, MANA_ICON, String.format("%.2f", mana));
        if (player.getAirSupply() < player.getMaxAirSupply()) {
            drawIconValue(guiGraphics, leftX + 65, rowTopY, OXYGEN_ICON,
                    String.valueOf(player.getAirSupply() / 20));
        }
    }

    private void updateBlinkTimers(Player player, float currentLingQi, float maxLingQi) {
        int tick = player.tickCount;
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float absorption = player.getAbsorptionAmount();
        int food = player.getFoodData().getFoodLevel();
        float saturation = player.getFoodData().getSaturationLevel();

        if (trackedPlayer != player) {
            trackedPlayer = player;
            lastHealth = health;
            lastMaxHealth = maxHealth;
            lastAbsorption = absorption;
            lastFood = food;
            lastSaturation = saturation;
            lastLingQi = currentLingQi;
            lastMaxLingQi = maxLingQi;
            healthBlinkUntil = tick;
            hungryBlinkUntil = tick;
            lingQiBlinkUntil = tick;
            displayedHealth = health;
            displayedFood = food;
            displayedLingQi = currentLingQi;
            lastAnimationMillis = Util.getMillis();
            return;
        }

        if (Float.compare(health, lastHealth) != 0
                || Float.compare(maxHealth, lastMaxHealth) != 0
                || Float.compare(absorption, lastAbsorption) != 0) {
            healthBlinkUntil = tick + HEALTH_BLINK_TICKS;
        }
        if (food != lastFood || Float.compare(saturation, lastSaturation) != 0) {
            hungryBlinkUntil = tick + OTHER_BAR_BLINK_TICKS;
        }
        if (Float.compare(currentLingQi, lastLingQi) != 0 || Float.compare(maxLingQi, lastMaxLingQi) != 0) {
            lingQiBlinkUntil = tick + OTHER_BAR_BLINK_TICKS;
        }

        lastHealth = health;
        lastMaxHealth = maxHealth;
        lastAbsorption = absorption;
        lastFood = food;
        lastSaturation = saturation;
        lastLingQi = currentLingQi;
        lastMaxLingQi = maxLingQi;
    }

    private void updateDisplayedValues(float health, float food, float lingQi) {
        long now = Util.getMillis();
        float deltaSeconds = Math.min((now - lastAnimationMillis) / 1000.0F, 0.1F);
        lastAnimationMillis = now;
        float animationFactor = 1.0F - (float) Math.exp(-VALUE_ANIMATION_SPEED * deltaSeconds);

        displayedHealth = animateValue(displayedHealth, health, animationFactor);
        displayedFood = animateValue(displayedFood, food, animationFactor);
        displayedLingQi = animateValue(displayedLingQi, lingQi, animationFactor);
    }

    private static float animateValue(float displayed, float target, float animationFactor) {
        float animated = displayed + (target - displayed) * animationFactor;
        return Math.abs(target - animated) <= VALUE_SNAP_EPSILON ? target : animated;
    }

    private static BarTextures healthTexturesFor(Player player) {
        if (player.hasEffect(MobEffects.POISON)) return POISONED;
        if (player.hasEffect(MobEffects.WITHER)) return WITHERED;
        if (player.isFullyFrozen() || player.hasEffect(ModEffects.freeze)) return FROZEN;
        if (player.getAbsorptionAmount() > 0) return ABSORBING;
        return HEALTH;
    }

    private static boolean isBlinking(int tick, int blinkUntil) {
        return tick < blinkUntil && (tick / BLINK_INTERVAL_TICKS & 1) == 0;
    }

    private void drawBar(GuiGraphics graphics, int x, int y, float current, float max,
                         BarTextures textures, boolean blinking, String text, int textYOffset) {
        ResourceLocation empty = blinking ? textures.blinkingEmpty() : textures.empty();
        ResourceLocation full = blinking ? textures.blinkingFull() : textures.full();
        drawBarTexture(graphics, empty, x, y);

        float ratio = max > 0.0F ? current / max : 0.0F;
        int width = (int) (Math.clamp(ratio, 0.0F, 1.0F) * BAR_WIDTH);
        if (width > 0) {
            graphics.enableScissor(x, y, x + width, y + BAR_HEIGHT);
            drawBarTexture(graphics, full, x, y);
            graphics.disableScissor();
        }

        renderScaledText(graphics, text, x, y + textYOffset, BAR_WIDTH);
    }

    private void drawBarTexture(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
        int centerSourceWidth = BAR_TEXTURE_WIDTH - SOURCE_CAP_WIDTH * 2;
        int centerDisplayWidth = BAR_WIDTH - DISPLAY_CAP_WIDTH * 2;

        graphics.blit(texture, x, y, DISPLAY_CAP_WIDTH, BAR_HEIGHT,
                0.0F, 0.0F, SOURCE_CAP_WIDTH, BAR_TEXTURE_HEIGHT,
                BAR_TEXTURE_WIDTH, BAR_TEXTURE_HEIGHT);
        graphics.blit(texture, x + DISPLAY_CAP_WIDTH, y, centerDisplayWidth, BAR_HEIGHT,
                SOURCE_CAP_WIDTH, 0.0F, centerSourceWidth, BAR_TEXTURE_HEIGHT,
                BAR_TEXTURE_WIDTH, BAR_TEXTURE_HEIGHT);
        graphics.blit(texture, x + BAR_WIDTH - DISPLAY_CAP_WIDTH, y, DISPLAY_CAP_WIDTH, BAR_HEIGHT,
                BAR_TEXTURE_WIDTH - SOURCE_CAP_WIDTH, 0.0F, SOURCE_CAP_WIDTH, BAR_TEXTURE_HEIGHT,
                BAR_TEXTURE_WIDTH, BAR_TEXTURE_HEIGHT);
    }

    private void drawIconValue(GuiGraphics graphics, int x, int y, ResourceLocation icon, String value) {
        graphics.blit(icon, x, y, 0, 0, 9, 9, 9, 9);
        renderScaledText(graphics, value, x + 11, y + 1, 0);
    }

    private void renderScaledText(GuiGraphics graphics, String text, int x, int y, int barWidth) {
        float scale = 0.7F;
        int textWidth = Minecraft.getInstance().font.width(text);
        float textX = barWidth > 0 ? x + (barWidth - textWidth * scale) / 2.0F : x;
        float textY = y + (BAR_HEIGHT - 8 * scale) / 2.0F;

        graphics.pose().pushPose();
        graphics.pose().translate(textX, textY, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(Minecraft.getInstance().font, text, 0, 0, 0xFFFFFF, true);
        graphics.pose().popPose();
    }

    private record BarTextures(ResourceLocation empty, ResourceLocation full,
                               ResourceLocation blinkingEmpty, ResourceLocation blinkingFull) {
    }
}
