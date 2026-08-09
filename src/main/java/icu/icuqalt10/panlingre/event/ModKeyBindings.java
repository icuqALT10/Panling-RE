package icu.icuqalt10.panlingre.event;

import com.mojang.blaze3d.platform.InputConstants;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.client.SkillWheelOverlay;
import icu.icuqalt10.panlingre.client.task.ClientTaskGuideState;
import icu.icuqalt10.panlingre.init.ModEffects;
import icu.icuqalt10.panlingre.network.LdPayload;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = PanlingRE.MODID, value = Dist.CLIENT)
public class ModKeyBindings {
    protected static final Map<KeyMapping, String> SKILL_KEYS = new HashMap<>();
    public static final List<KeyMapping> SKILL_SHORTCUTS = new ArrayList<>(16);

    public static final KeyMapping LIANDAN =
            register("liandan", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_L, "key.PanlingRE.liandan");
    public static final KeyMapping SKILL_ACTIVATE =
            register("skill_activate", InputConstants.Type.MOUSE, 3, "key.PanlingRE.skill_activate");
    public static final KeyMapping MODE_TOGGLE =
            register("mode_toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.PanlingRE.mode_toggle");
    public static final KeyMapping SKILL_WHEEL =
            register("skill_wheel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.PanlingRE.skill_wheel");
    public static final KeyMapping TASK_GUIDE_TOGGLE =
            register("task_guide_toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.PanlingRE.task_guide_toggle");

    static {
        for (int i = 1; i <= 16; i++) {
            SKILL_SHORTCUTS.add(register("skill_shortcut_" + i, InputConstants.Type.KEYSYM,
                    InputConstants.UNKNOWN.getValue(), "key.PanlingRE.skill_shortcut_" + i));
        }
    }

    private static KeyMapping register(String name, InputConstants.Type type, int code, String id) {
        KeyMapping mapping = new KeyMapping("key.PanlingRE." + name, type, code, "key.categories.PanlingRE");
        SKILL_KEYS.put(mapping, id);
        return mapping;
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        SKILL_KEYS.keySet().forEach(event::register);
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

        while (ModKeyBindings.TASK_GUIDE_TOGGLE.consumeClick()) {
            ClientTaskGuideState.toggleGuidance();
        }

        if (player.hasEffect(ModEffects.freeze)) return;

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
            activateSkill(ClientSkillState.getSelectedSkill());
        }

        for (int i = 0; i < ModKeyBindings.SKILL_SHORTCUTS.size(); i++) {
            KeyMapping shortcut = ModKeyBindings.SKILL_SHORTCUTS.get(i);
            while (shortcut.consumeClick()) {
                var skills = ClientSkillState.getAvailableSkills();
                if (i < skills.size()) activateSkill(skills.get(i));
            }
        }

        while (ModKeyBindings.LIANDAN.consumeClick()) {
            PacketDistributor.sendToServer(new LdPayload());
        }
    }

    private static void activateSkill(ClientSkillState.SkillSlot skill) {
        if (skill != null && ClientSkillState.getCooldownProgress(skill) <= 0) {
            PacketDistributor.sendToServer(
                    new SkillWheelPayload(skill.itemId(), skill.skillIndex()));
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
