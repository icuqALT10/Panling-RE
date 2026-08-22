package icu.icuqalt10.panlingre.init;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.command.BaFangYiCommand;
import icu.icuqalt10.panlingre.looktip.LookTipNetworkHandler;
import icu.icuqalt10.panlingre.looktip.LookTipRequestPayload;
import icu.icuqalt10.panlingre.looktip.LookTipResponsePayload;
import icu.icuqalt10.panlingre.network.*;
import icu.icuqalt10.panlingre.network.particle.GatherBall;
import icu.icuqalt10.panlingre.network.particle.HuoQiuExplosionParticles;
import icu.icuqalt10.panlingre.network.ShakePayload;
import icu.icuqalt10.panlingre.network.particle.ParticleCluster;
import icu.icuqalt10.panlingre.network.particle.ParticleLighting;
import icu.icuqalt10.panlingre.network.particle.Ys3JinTornadoPayload;
import icu.icuqalt10.panlingre.network.particle.ZhuqueMeteorWarningPayload;
import icu.icuqalt10.panlingre.network.task.TaskEntityCheckPayload;
import icu.icuqalt10.panlingre.network.task.TaskEntityResultPayload;
import icu.icuqalt10.panlingre.network.task.TaskGuideSyncPayload;
import icu.icuqalt10.panlingre.task.TaskGuideService;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworks {

    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PanlingRE.MODID);

        registrar.playToServer(
                LdPayload.TYPE,
                LdPayload.STREAM_CODEC,
                LdPayload::handle
        );

        registrar.playToServer(
                SkillWheelPayload.TYPE,
                SkillWheelPayload.STREAM_CODEC,
                SkillWheelPayload::handle
        );

        registrar.playToServer(
                SkillCastCancelPayload.TYPE,
                SkillCastCancelPayload.STREAM_CODEC,
                SkillCastCancelPayload::handle
        );

        registrar.playToServer(
                TianXingTargetPayload.TYPE,
                TianXingTargetPayload.STREAM_CODEC,
                TianXingTargetPayload::handle
        );

        registrar.playToServer(
                PojunCounterAttackReadyPayload.TYPE,
                PojunCounterAttackReadyPayload.STREAM_CODEC,
                PojunCounterAttackReadyPayload::handle
        );

        registrar.playToClient(
                SkillUseSucceededPayload.TYPE,
                SkillUseSucceededPayload.STREAM_CODEC,
                ClientPayloadHandlers::handleSkillUseSucceeded
        );

        registrar.playToClient(
                SkillCastStatePayload.TYPE,
                SkillCastStatePayload.STREAM_CODEC,
                ClientPayloadHandlers::handleSkillCastState
        );

        registrar.playToClient(
                SkillCastReleasePayload.TYPE,
                SkillCastReleasePayload.STREAM_CODEC,
                ClientPayloadHandlers::handleSkillCastRelease
        );

        registrar.playToClient(
                ItemActivationPayload.TYPE,
                ItemActivationPayload.STREAM_CODEC,
                ClientPayloadHandlers::handleItemActivation
        );

        registrar.playToClient(
                PojunCounterAttackPayload.TYPE,
                PojunCounterAttackPayload.STREAM_CODEC,
                ClientPayloadHandlers::handlePojunCounterAttack
        );

        registrar.playToClient(
                LingQiSyncPacket.TYPE,
                LingQiSyncPacket.STREAM_CODEC,
                ClientPayloadHandlers::handleLingQiSync
        );

        registrar.playToClient(
                SyncBlessPayload.TYPE,
                SyncBlessPayload.STREAM_CODEC,
                ClientPayloadHandlers::handleSyncBless
        );

        registrar.playToClient(
                ShockwaveUpdatePayload.TYPE,
                ShockwaveUpdatePayload.STREAM_CODEC,
                ClientPayloadHandlers::handleShockwaveUpdate
        );

        registrar.playToClient(
                GroundSmashPayload.TYPE,
                GroundSmashPayload.STREAM_CODEC,
                ClientPayloadHandlers::handleGroundSmash
        );

        registrar.playToClient(
                ParticleLighting.TYPE,
                ParticleLighting.STREAM_CODEC,
                ClientPayloadHandlers::handleParticleLighting
        );

        registrar.playToClient(
                HuoQiuExplosionParticles.TYPE,
                HuoQiuExplosionParticles.STREAM_CODEC,
                ClientPayloadHandlers::handleHuoQiuExplosion
        );

        registrar.playToClient(
                FakeSnowPayload.TYPE,
                FakeSnowPayload.STREAM_CODEC,
                ClientPayloadHandlers::handleFakeSnow
        );

        registrar.playToClient(
                SiShouMusicPayload.TYPE,
                SiShouMusicPayload.STREAM_CODEC,
                ClientPayloadHandlers::handleSiShouMusic
        );

        registrar.playToClient(
                GatherBall.TYPE,
                GatherBall.STREAM_CODEC,
                ClientPayloadHandlers::handleGatherBall
        );

        registrar.playToClient(
                ParticleCluster.TYPE,
                ParticleCluster.STREAM_CODEC,
                ClientPayloadHandlers::handleParticleCluster
        );

        registrar.playToClient(
                Ys3JinTornadoPayload.TYPE,
                Ys3JinTornadoPayload.STREAM_CODEC,
                ClientPayloadHandlers::handleYs3JinTornado
        );

        registrar.playToClient(
                ZhuqueMeteorWarningPayload.TYPE,
                ZhuqueMeteorWarningPayload.STREAM_CODEC,
                ClientPayloadHandlers::handleZhuqueMeteorWarning
        );

        registrar.playToClient(
                ShakePayload.TYPE,
                ShakePayload.STREAM_CODEC,
                ClientPayloadHandlers::handleShake
        );

        // Look Tip 网络包
        registrar.playToServer(
                LookTipRequestPayload.TYPE,
                LookTipRequestPayload.STREAM_CODEC,
                LookTipNetworkHandler::handleRequest
        );

        registrar.playToClient(
                LookTipResponsePayload.TYPE,
                LookTipResponsePayload.STREAM_CODEC,
                ClientPayloadHandlers::handleLookTipResponse
        );

        // 八方仪传送
        registrar.playToClient(
                BaFangYiOpenPayload.TYPE,
                BaFangYiOpenPayload.STREAM_CODEC,
                ClientPayloadHandlers::handleBaFangYiOpen
        );

        registrar.playToServer(
                BaFangYiTeleportPayload.TYPE,
                BaFangYiTeleportPayload.STREAM_CODEC,
                BaFangYiCommand::handleTeleportRequest
        );

        registrar.playToClient(
                TaskGuideSyncPayload.TYPE,
                TaskGuideSyncPayload.STREAM_CODEC,
                ClientPayloadHandlers::handleTaskGuideSync
        );

        registrar.playToServer(
                TaskEntityCheckPayload.TYPE,
                TaskEntityCheckPayload.STREAM_CODEC,
                TaskGuideService::handleEntityCheck
        );

        registrar.playToClient(
                TaskEntityResultPayload.TYPE,
                TaskEntityResultPayload.STREAM_CODEC,
                ClientPayloadHandlers::handleTaskEntityResult
        );

    }
}
