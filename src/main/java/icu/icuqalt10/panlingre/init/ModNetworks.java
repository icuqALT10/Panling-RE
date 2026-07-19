package icu.icuqalt10.panlingre.init;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.network.*;
import icu.icuqalt10.panlingre.network.particle.GatherBall;
import icu.icuqalt10.panlingre.network.ShakePayload;
import icu.icuqalt10.panlingre.network.particle.ParticleCluster;
import icu.icuqalt10.panlingre.network.particle.ParticleLighting;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworks {

    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PanlingRE.MODID);

        registrar.playToServer(
                SkillPayload.TYPE,
                SkillPayload.STREAM_CODEC,
                SkillPayload::handle
        );

        registrar.playToServer(
                SkillWheelPayload.TYPE,
                SkillWheelPayload.STREAM_CODEC,
                SkillWheelPayload::handle
        );

        registrar.playToClient(
                LingQiSyncPacket.TYPE,
                LingQiSyncPacket.STREAM_CODEC,
                LingQiSyncPacket::handle
        );

        registrar.playToClient(
                SyncBlessPayload.TYPE,
                SyncBlessPayload.STREAM_CODEC,
                SyncBlessPayload::handle
        );

        registrar.playToClient(
                ShockwaveUpdatePayload.TYPE,
                ShockwaveUpdatePayload.STREAM_CODEC,
                ShockwaveUpdatePayload::handle
        );

        registrar.playToClient(
                GroundSmashPayload.TYPE,
                GroundSmashPayload.STREAM_CODEC,
                GroundSmashPayload::handle
        );

        registrar.playToClient(
                ParticleLighting.TYPE,
                ParticleLighting.STREAM_CODEC,
                ParticleLighting::handle
        );

        registrar.playToClient(
                FakeSnowPayload.TYPE,
                FakeSnowPayload.STREAM_CODEC,
                FakeSnowPayload::handle
        );

        registrar.playToClient(
                GatherBall.TYPE,
                GatherBall.STREAM_CODEC,
                GatherBall::handle
        );

        registrar.playToClient(
                ParticleCluster.TYPE,
                ParticleCluster.STREAM_CODEC,
                ParticleCluster::handle
        );

        registrar.playToClient(
                SyncFreezeDataPayload.TYPE,
                SyncFreezeDataPayload.STREAM_CODEC,
                SyncFreezeDataPayload::handle
        );

        registrar.playToClient(
                ShakePayload.TYPE,
                ShakePayload.STREAM_CODEC,
                ShakePayload::handle
        );


    }
}
