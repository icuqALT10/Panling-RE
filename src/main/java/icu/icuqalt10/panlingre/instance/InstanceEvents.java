package icu.icuqalt10.panlingre.instance;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = PanlingRE.MODID)
public final class InstanceEvents {
    private InstanceEvents() {
    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new InstanceDefinitionLoader());
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        InstanceManager.startServer(event.getServer());
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event) {
        InstanceManager.stopServer();
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        InstanceManager.tick();
    }

    @SubscribeEvent
    public static void livingDeath(LivingDeathEvent event) {
        InstanceManager.entityDied(event.getEntity());
        if (event.getEntity() instanceof ServerPlayer player) InstanceManager.failPlayer(player);
    }

    @SubscribeEvent
    public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) InstanceManager.failAndKill(player);
    }

    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.getTags().contains(InstanceManager.ACTIVE_PLAYER_TAG)) {
            InstanceManager.failAndKill(player);
        }
    }
}
