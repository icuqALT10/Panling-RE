package icu.icuqalt10.panlingre.subtool.opengui.network;

import icu.icuqalt10.panlingre.PanlingRE;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class OpenGuiPackets {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(OpenGuiPackets::onRegister);
    }

    private static void onRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar(PanlingRE.MODID).versioned("1.0");
        reg.playToClient(
                OpenVillagerGuiPacket.TYPE,
                OpenVillagerGuiPacket.STREAM_CODEC,
                OpenVillagerGuiPacket::handle
        );
    }
}