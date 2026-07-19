package icu.icuqalt10.panlingre.event;

import com.mojang.blaze3d.platform.InputConstants;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.FreezeData;
import icu.icuqalt10.panlingre.client.SkillWheelOverlay;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.network.SkillPayload;
import icu.icuqalt10.panlingre.network.SkillWheelPayload;
import icu.icuqalt10.panlingre.skill.ClientSkillState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = PanlingRE.MODID, value = Dist.CLIENT)
public class ModKeyBindings {
    protected static final Map<KeyMapping, Integer> SKILL_KEYS = new HashMap<>();

    public static final KeyMapping LIANDAN = register("liandan", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_L, 1);
    public static final KeyMapping SKILL_ACTIVATE = register("skill_1", InputConstants.Type.MOUSE, 3, 13);
    public static final KeyMapping MODE_TOGGLE = register("mode_toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, 14);

    public static final KeyMapping SKILL_WHEEL = new KeyMapping(
            "key.PanlingRE.skill_wheel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "key.categories.PanlingRE");

    private static KeyMapping register(String name, InputConstants.Type type, int code, int id) {
        KeyMapping mapping = new KeyMapping("key.PanlingRE." + name, type, code, "key.categories.PanlingRE");
        SKILL_KEYS.put(mapping, id);
        return mapping;
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        SKILL_KEYS.keySet().forEach(event::register);
        event.register(SKILL_WHEEL);
    }
}

@EventBusSubscriber(modid = PanlingRE.MODID, value = Dist.CLIENT)
class InputHandler {
    private static boolean wheelWasDown;
    private static int rebuildTick;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        FreezeData freezeData = player.getData(ModAttachments.FREEZE_DATA.get());
        if (freezeData.isFrozen()) return;

        if (++rebuildTick >= 2) {
            rebuildTick = 0;
            if (!SkillWheelOverlay.INSTANCE.active) {
                ClientSkillState.rebuild(player);
            }
        }

        while (ModKeyBindings.MODE_TOGGLE.consumeClick()) {
            ClientSkillState.toggleMode();
            String msg = ClientSkillState.isActivateMode() ? "模式：选择并释放" : "模式：仅选择";
            player.displayClientMessage(Component.literal(msg), true);
        }

        handleWheelKey(mc, player);

        if (SkillWheelOverlay.INSTANCE.active) return;

        while (ModKeyBindings.SKILL_ACTIVATE.consumeClick()) {
            var sel = ClientSkillState.getSelectedSkill();
            if (sel != null && ClientSkillState.getCooldownProgress(sel) <= 0) {
                long baseCd = sel.data().cooldown();
                PacketDistributor.sendToServer(
                        new SkillWheelPayload(sel.itemId(), sel.skillIndex(), baseCd));
                ClientSkillState.recordCooldown(sel,
                        ClientSkillState.getReducedCooldown(player, baseCd));
            }
        }

        while (ModKeyBindings.LIANDAN.consumeClick()) {
            PacketDistributor.sendToServer(new SkillPayload(1));
        }
    }

    private static void handleWheelKey(Minecraft mc, Player player) {
        boolean down = ModKeyBindings.SKILL_WHEEL.isDown();

        if (down && !wheelWasDown && !SkillWheelOverlay.INSTANCE.active) {
            ClientSkillState.rebuild(player);
            SkillWheelOverlay.INSTANCE.open();
        }

        if (!down && wheelWasDown && SkillWheelOverlay.INSTANCE.active) {
            SkillWheelOverlay.INSTANCE.close();
        }

        wheelWasDown = down;
    }
}
