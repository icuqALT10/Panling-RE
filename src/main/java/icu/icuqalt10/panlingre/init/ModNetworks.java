package icu.icuqalt10.panlingre.init;

import icu.icuqalt10.panlingre.network.AttackInstructionPayload;
import icu.icuqalt10.panlingre.network.LingQiSyncPacket;
import icu.icuqalt10.panlingre.network.SkillPayload;
import icu.icuqalt10.panlingre.network.SyncBlessPayload;
import icu.icuqalt10.panlingre.server.ServerPayloadHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworks {

    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1.0.0");

        registrar.playToServer(
                SkillPayload.TYPE,
                SkillPayload.STREAM_CODEC,
                (payload, context) -> ServerPayloadHandler.handleSkill(payload, context)
        );

        registrar.playToClient(
                LingQiSyncPacket.TYPE,
                LingQiSyncPacket.STREAM_CODEC,
                LingQiSyncPacket::handle);

        registrar.playToClient(
                SyncBlessPayload.TYPE,
                SyncBlessPayload.STREAM_CODEC,
                SyncBlessPayload::handleClient
        );

        registrar.playToServer(
                AttackInstructionPayload.TYPE,
                AttackInstructionPayload.STREAM_CODEC,
                (payload, context) -> ServerPayloadHandler.handleAttackInstruction(payload, context)
        );

    }
}
