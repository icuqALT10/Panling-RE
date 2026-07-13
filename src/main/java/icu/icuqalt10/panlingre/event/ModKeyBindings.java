package icu.icuqalt10.panlingre.event;

import com.mojang.blaze3d.platform.InputConstants;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.FreezeData;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.network.SkillPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
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

    //注册按键 并 分配ID
    public static final KeyMapping GONGFA = register("gongfa", InputConstants.Type.MOUSE, 3, 1);
    public static final KeyMapping LIANDAN = register("liandan", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_L, 2);
    public static final KeyMapping SKILL_1 = register("skill_1", InputConstants.Type.MOUSE, 4, 11);

    private static KeyMapping register(String name, InputConstants.Type type, int code, int id) {
        KeyMapping mapping = new KeyMapping("key.PanlingRE." + name, type, code, "key.categories.PanlingRE");
        SKILL_KEYS.put(mapping, id);
        return mapping;
    }

    //注册到游戏设置
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        SKILL_KEYS.keySet().forEach(event::register);
    }
}
//监听按键按下
@EventBusSubscriber(modid = PanlingRE.MODID, value = Dist.CLIENT)
class InputHandler {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Player player = Minecraft.getInstance().player;

        if (player == null) return;

        // 检查玩家是否被冻结
        FreezeData freezeData = player.getData(ModAttachments.FREEZE_DATA.get());
        if (freezeData.isFrozen()) {
            // 消耗所有按键输入但不发送到服务器
            for (var entry : ModKeyBindings.SKILL_KEYS.entrySet()) {
                while (entry.getKey().consumeClick()) {
                    // 不做任何事，只是消耗按键
                }
            }
            return;
        }

        for (var entry : ModKeyBindings.SKILL_KEYS.entrySet()) {
            // 如果按键被点击
            while (entry.getKey().consumeClick()) {
                // 发送对应的 ID 给服务器
                PacketDistributor.sendToServer(new SkillPayload(entry.getValue()));
            }
        }
    }
}