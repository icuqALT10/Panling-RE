package icu.icuqalt10.panlingre.review;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.ItemUnlockData;
import icu.icuqalt10.panlingre.compat.jei.PanlingREJeiPlugin;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TotemParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Map;
import java.util.Queue;

/** Runs only via scripts/item-unlock-client-tests.gradle in an isolated singleplayer world. */
@JeiPlugin
public class ItemUnlockClientTest implements IModPlugin {
    private IJeiRuntime runtime;
    private int ticks;
    private int stage;
    private int nextTick;
    private boolean finished;
    private boolean activationSeen;
    private boolean blackSeen;
    private boolean whiteSeen;

    public ItemUnlockClientTest() { NeoForge.EVENT_BUS.addListener(this::tick); }

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "item_unlock_client_test");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) { this.runtime = runtime; }

    private void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (finished) return;
        if (++ticks > 2400) {
            finish(mc, "FAIL: timed out waiting for stage " + stage);
            return;
        }
        if (runtime == null || mc.player == null || mc.getSingleplayerServer() == null || ticks < nextTick) return;
        try {
            switch (stage) {
                case 0 -> {
                    command(mc, "off");
                    runtime.getIngredientFilter().setFilterText("");
                    mc.setScreen(new InventoryScreen(mc.player));
                    advance();
                }
                case 1 -> {
                    checkVisibility(mc, false);
                    screenshot(mc, "jei-locked.png");
                    command(mc, "on");
                    advance();
                }
                case 2 -> {
                    checkVisibility(mc, true);
                    screenshot(mc, "jei-unlocked.png");
                    command(mc, "off");
                    advance();
                }
                case 3 -> {
                    checkVisibility(mc, false);
                    command(mc, "on");
                    advance();
                }
                case 4 -> {
                    checkVisibility(mc, true);
                    mc.setScreen(null);
                    var server = mc.getSingleplayerServer();
                    var id = mc.player.getUUID();
                    server.execute(() -> {
                        var player = server.getPlayerList().getPlayer(id);
                        player.setGameMode(GameType.SURVIVAL);
                        player.setData(ModAttachments.HUAN_HUN_COOLDOWN, 0L);
                        player.getCooldowns().removeCooldown(ModItems.huan_hun_fu.get());
                        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.huan_hun_fu.get(), 2));
                        player.hurt(player.damageSources().generic(), 10000);
                    });
                    stage++;
                }
                case 5 -> {
                    var item = (ItemStack) field(GameRenderer.class, "itemActivationItem").get(mc.gameRenderer);
                    int remaining = field(GameRenderer.class, "itemActivationTicks").getInt(mc.gameRenderer);
                    if (item != null && remaining > 0) {
                        require(item.is(ModItems.huan_hun_fu.get()), "Activation rendered a different item");
                        activationSeen = true;
                        if (remaining >= 5 && remaining <= 9) screenshot(mc, "huan-hun-activation.png");
                    }
                    inspectParticles(mc);
                    if (activationSeen && blackSeen && whiteSeen && remaining <= 0) {
                        require(mc.player.getMainHandItem().getCount() == 1, "Unexpected talisman consumption");
                        finish(mc, "PASS: JEI locked/on/off/on item and recipe visibility; talisman activation model; black and white particles; single consumption");
                    }
                }
            }
        } catch (Throwable error) {
            PanlingRE.LOGGER.error("Item unlock client test failed", error);
            finish(mc, "FAIL: " + error);
        }
    }

    private void checkVisibility(Minecraft mc, boolean expected) {
        require(ItemUnlockData.canUse(mc.player, ModItems.huan_hun_fu.get()) == expected, "Attachment did not sync");
        boolean visible = runtime.getJeiHelpers().getIngredientVisibility()
                .isIngredientVisible(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.huan_hun_fu.get()));
        require(visible == expected, "Wrong JEI ingredient visibility");
        boolean inList = runtime.getIngredientFilter().getFilteredItemStacks().stream()
                .anyMatch(stack -> stack.is(ModItems.huan_hun_fu.get()));
        require(inList == expected, "Wrong JEI sidebar visibility");
        boolean recipe = runtime.getRecipeManager().createRecipeLookup(PanlingREJeiPlugin.ZFT_TYPE).get()
                .anyMatch(value -> value.result().is(ModItems.huan_hun_fu.get()));
        require(recipe == expected, "Wrong JEI recipe visibility");
    }

    private void command(Minecraft mc, String action) {
        var server = mc.getSingleplayerServer();
        var id = mc.player.getUUID();
        server.execute(() -> {
            var player = server.getPlayerList().getPlayer(id);
            server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(2),
                    "plre unlock item panlingre:huan_hun_fu " + action);
        });
    }

    private void advance() { stage++; nextTick = ticks + 30; }

    @SuppressWarnings("unchecked")
    private void inspectParticles(Minecraft mc) throws ReflectiveOperationException {
        var particles = (Map<?, Queue<Particle>>) field(ParticleEngine.class, "particles").get(mc.particleEngine);
        for (var queue : particles.values()) {
            for (Particle particle : queue) {
                if (!(particle instanceof TotemParticle)) continue;
                float r = field(Particle.class, "rCol").getFloat(particle);
                float g = field(Particle.class, "gCol").getFloat(particle);
                float b = field(Particle.class, "bCol").getFloat(particle);
                require(r == g && g == b && (r == 0 || r == 1), "Colored vanilla totem particle leaked");
                blackSeen |= r == 0;
                whiteSeen |= r == 1;
            }
        }
    }

    private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> {});
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private void finish(Minecraft mc, String result) {
        finished = true;
        PanlingRE.LOGGER.info("ITEM_UNLOCK_CLIENT_TEST: {}", result);
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("test-result.txt"), result);
        } catch (java.io.IOException error) {
            throw new RuntimeException(error);
        }
        mc.stop();
    }
}
