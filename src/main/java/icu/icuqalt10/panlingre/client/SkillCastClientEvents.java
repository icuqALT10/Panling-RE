package icu.icuqalt10.panlingre.client;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.network.SkillCastCancelPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Raises the casting hand toward eye level for players rendered in third person. */
@EventBusSubscriber(modid = PanlingRE.MODID, value = Dist.CLIENT)
public final class SkillCastClientEvents {
    private static int renderingPlayerId = Integer.MIN_VALUE;

    private SkillCastClientEvents() {
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) return;

        ClientSkillCastState.clear(minecraft.player.getId());
        PacketDistributor.sendToServer(new SkillCastCancelPayload());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        renderingPlayerId = event.getEntity().getId();
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (renderingPlayerId == event.getEntity().getId()) {
            renderingPlayerId = Integer.MIN_VALUE;
        }
    }

    public static boolean isRenderingThirdPerson(Player player) {
        return renderingPlayerId == player.getId();
    }
}
