package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.client.ClientModEvents;
import icu.icuqalt10.panlingre.client.ClientSkillCastState;
import icu.icuqalt10.panlingre.client.GroundSmashRenderer;
import icu.icuqalt10.panlingre.client.Ys3JinTornadoParticles;
import icu.icuqalt10.panlingre.client.ZhuqueMeteorWarningParticles;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.item.warrior.other.di_shi_dun;
import icu.icuqalt10.panlingre.looktip.LookTipOverlay;
import icu.icuqalt10.panlingre.looktip.LookTipResponsePayload;
import icu.icuqalt10.panlingre.network.particle.GatherBall;
import icu.icuqalt10.panlingre.network.particle.HuoQiuExplosionParticles;
import icu.icuqalt10.panlingre.network.particle.ParticleCluster;
import icu.icuqalt10.panlingre.network.particle.ParticleLighting;
import icu.icuqalt10.panlingre.network.particle.Ys3JinTornadoPayload;
import icu.icuqalt10.panlingre.network.particle.ZhuqueMeteorWarningPayload;
import icu.icuqalt10.panlingre.network.task.TaskEntityResultPayload;
import icu.icuqalt10.panlingre.network.task.TaskGuideSyncPayload;
import icu.icuqalt10.panlingre.skill.ClientSkillState;
import icu.icuqalt10.panlingre.util.LocalWeatherManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Physical-side-safe entry points for clientbound payload handlers.
 *
 * <p>The public methods in this class contain no references to Minecraft client classes. Dedicated
 * servers can therefore resolve them while registering the shared network protocol. Client-only
 * classes are linked only after the physical-side check succeeds.</p>
 */
public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {
    }

    public static void handleSkillUseSucceeded(SkillUseSucceededPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientOnly.handleSkillUseSucceeded(payload, context);
    }

    public static void handleSkillCastState(SkillCastStatePayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientOnly.handleSkillCastState(payload, context);
    }

    public static void handleSkillCastRelease(SkillCastReleasePayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientOnly.handleSkillCastRelease(payload, context);
    }

    public static void handleItemActivation(ItemActivationPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientOnly.handleItemActivation(payload, context);
    }

    public static void handlePojunCounterAttack(PojunCounterAttackPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientOnly.handlePojunCounterAttack(payload, context);
    }

    public static void handleLingQiSync(LingQiSyncPacket payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientOnly.handleLingQiSync(payload, context);
    }

    public static void handleSyncBless(SyncBlessPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientOnly.handleSyncBless(payload, context);
    }

    public static void handleShockwaveUpdate(ShockwaveUpdatePayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientOnly.handleShockwaveUpdate(payload, context);
    }

    public static void handleGroundSmash(GroundSmashPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientOnly.handleGroundSmash(payload, context);
    }

    public static void handleParticleLighting(ParticleLighting payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientOnly.handleParticleLighting(payload, context);
    }

    public static void handleHuoQiuExplosion(HuoQiuExplosionParticles payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientOnly.handleHuoQiuExplosion(payload, context);
    }

    public static void handleFakeSnow(FakeSnowPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientOnly.handleFakeSnow(payload, context);
    }

    public static void handleSiShouMusic(SiShouMusicPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientOnly.handleSiShouMusic(payload, context);
    }

    public static void handleGatherBall(GatherBall payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientOnly.handleGatherBall(payload, context);
    }

    public static void handleParticleCluster(ParticleCluster payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientOnly.handleParticleCluster(payload, context);
    }

    public static void handleYs3JinTornado(Ys3JinTornadoPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientOnly.handleYs3JinTornado(payload, context);
    }

    public static void handleZhuqueMeteorWarning(ZhuqueMeteorWarningPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientOnly.handleZhuqueMeteorWarning(payload, context);
    }

    public static void handleShake(ShakePayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientOnly.handleShake(payload, context);
    }

    public static void handleLookTipResponse(LookTipResponsePayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientOnly.handleLookTipResponse(payload, context);
    }

    public static void handleBaFangYiOpen(BaFangYiOpenPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientOnly.handleBaFangYiOpen(payload, context);
    }

    public static void handleTaskGuideSync(TaskGuideSyncPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientOnly.handleTaskGuideSync(payload, context);
    }

    public static void handleTaskEntityResult(TaskEntityResultPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientOnly.handleTaskEntityResult(payload, context);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientOnly {
        private ClientOnly() {
        }

        private static void handleSkillUseSucceeded(SkillUseSucceededPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> ClientSkillState.recordCooldown(payload.cooldownKey(), payload.cooldown()));
        }

        private static void handleSkillCastState(SkillCastStatePayload payload, IPayloadContext context) {
            context.enqueueWork(() -> ClientSkillCastState.update(payload));
        }

        private static void handleSkillCastRelease(SkillCastReleasePayload payload, IPayloadContext context) {
            context.enqueueWork(() -> ClientSkillCastState.startRelease(payload));
        }

        private static void handleItemActivation(ItemActivationPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> Minecraft.getInstance().gameRenderer.displayItemActivation(payload.stack()));
        }

        private static void handlePojunCounterAttack(PojunCounterAttackPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player == null || minecraft.gameMode == null) return;
                boolean pojunActive = minecraft.player.getOffhandItem().getItem() instanceof di_shi_dun
                        && di_shi_dun.getForm(minecraft.player.getOffhandItem()) == di_shi_dun.FORM_POJUN;
                if (pojunActive && minecraft.hitResult instanceof EntityHitResult entityHit) {
                    PacketDistributor.sendToServer(new PojunCounterAttackReadyPayload());
                    minecraft.gameMode.attack(minecraft.player, entityHit.getEntity());
                }
                minecraft.player.swing(InteractionHand.MAIN_HAND);
            });
        }

        private static void handleLingQiSync(LingQiSyncPacket payload, IPayloadContext context) {
            context.enqueueWork(() -> LingQiData.ClientLingQiData.set(
                    payload.owner(), payload.current(), payload.max()));
        }

        private static void handleSyncBless(SyncBlessPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                Player player = context.player();
                if (player != null) player.setData(ModAttachments.BLESS.get(), payload.data());
            });
        }

        private static void handleShockwaveUpdate(ShockwaveUpdatePayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                ClientLevel level = Minecraft.getInstance().level;
                if (level == null) return;
                Vec3 center = payload.center();
                double radius = payload.age() * 1.2;
                for (int i = 0; i < 72; i++) {
                    boolean blocked = i < 64
                            ? (payload.blockedMask1() & (1L << i)) != 0
                            : (payload.blockedMask2() & (1L << (i - 64))) != 0;
                    if (blocked) continue;
                    double radians = Math.toRadians(i * 5);
                    level.addParticle(ParticleTypes.GUST, true,
                            center.x + Math.cos(radians) * radius, center.y,
                            center.z + Math.sin(radians) * radius, 0, 1, 0);
                }
            });
        }

        private static void handleGroundSmash(GroundSmashPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> GroundSmashRenderer.triggerSmash(
                    payload.center(), payload.radius(), payload.ticks()));
        }

        private static void handleParticleLighting(ParticleLighting payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player().level() instanceof ClientLevel level)) return;
                Vec3 targetPos = payload.Pos();
                RandomSource random = level.getRandom();
                level.addParticle(ParticleTypes.FLASH, true,
                        targetPos.x, targetPos.y, targetPos.z, 0, 0, 0);
                level.playLocalSound(targetPos.x, targetPos.y, targetPos.z,
                        SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER,
                        1.0F, 1.0F, false);
                for (double y = 0; y < 20; y += 0.5) {
                    for (int i = 0; i < 5; i++) {
                        level.addParticle(ParticleTypes.ELECTRIC_SPARK, true,
                                targetPos.x + random.nextGaussian() * 0.1,
                                targetPos.y + y + random.nextGaussian() * 0.1,
                                targetPos.z + random.nextGaussian() * 0.1,
                                random.nextGaussian() * 0.05,
                                random.nextGaussian() * 0.05,
                                random.nextGaussian() * 0.05);
                    }
                }
            });
        }

        private static void handleHuoQiuExplosion(HuoQiuExplosionParticles payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player().level() instanceof ClientLevel level)) return;
                Vec3 center = payload.pos();
                level.addParticle(ParticleTypes.EXPLOSION, true,
                        center.x, center.y, center.z, 0, 0, 0);
                for (int i = 0; i < 72; i++) {
                    double angle = Math.PI * 2.0 * i / 72;
                    double directionX = Math.cos(angle);
                    double directionZ = Math.sin(angle);
                    level.addParticle(ParticleTypes.FLAME, true,
                            center.x + directionX * 0.75, center.y + 0.1,
                            center.z + directionZ * 0.75,
                            directionX * 0.36, 0.015, directionZ * 0.36);
                }
            });
        }

        private static void handleFakeSnow(FakeSnowPayload payload, IPayloadContext context) {
            context.enqueueWork(() ->
                    LocalWeatherManager.ClientWeatherState.isFakeSnowing = payload.isSnowing());
        }

        private static void handleSiShouMusic(SiShouMusicPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> icu.icuqalt10.panlingre.client.sound.QinglongMusicManager.handle(payload.start()));
        }

        private static void handleGatherBall(GatherBall payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                Level level = context.player().level();
                RandomSource random = level.getRandom();
                for (int i = 0; i < payload.count(); i++) {
                    double theta = random.nextDouble() * 2 * Math.PI;
                    double phi = Math.acos(random.nextDouble() * 2 - 1);
                    double dx = Math.sin(phi) * Math.cos(theta);
                    double dy = Math.sin(phi) * Math.sin(theta);
                    double dz = Math.cos(phi);
                    level.addParticle(payload.particle(), true,
                            payload.center().x + dx * payload.rad(),
                            payload.center().y + dy * payload.rad(),
                            payload.center().z + dz * payload.rad(),
                            -dx * payload.speed(), -dy * payload.speed(), -dz * payload.speed());
                }
            });
        }

        private static void handleParticleCluster(ParticleCluster payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                Level level = context.player().level();
                RandomSource random = level.getRandom();
                Vec3 direction = payload.target().subtract(payload.start());
                double distance = direction.length();
                Vec3 velocity = distance < 0.001 ? Vec3.ZERO : direction.normalize().scale(distance / 2);
                for (int i = 0; i < payload.count(); i++) {
                    double theta = random.nextDouble() * 2 * Math.PI;
                    double phi = Math.acos(2 * random.nextDouble() - 1);
                    double radius = Math.cbrt(random.nextDouble()) * payload.radius();
                    level.addParticle(payload.particle(), true,
                            payload.start().x + radius * Math.sin(phi) * Math.cos(theta),
                            payload.start().y + radius * Math.sin(phi) * Math.sin(theta),
                            payload.start().z + radius * Math.cos(phi),
                            velocity.x, velocity.y, velocity.z);
                }
                PanlingRE.LOGGER.info("ParticleCluster spawned {} particles from {} to {} with velocity {} (distance: {})",
                        payload.count(), payload.start(), payload.target(), velocity, distance);
            });
        }

        private static void handleYs3JinTornado(Ys3JinTornadoPayload payload, IPayloadContext context) {
            context.enqueueWork(() ->
                    Ys3JinTornadoParticles.start(payload.center(), payload.durationTicks()));
        }

        private static void handleZhuqueMeteorWarning(ZhuqueMeteorWarningPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> ZhuqueMeteorWarningParticles.start(
                    payload.center(), payload.radius(), payload.durationTicks()));
        }

        private static void handleShake(ShakePayload payload, IPayloadContext context) {
            context.enqueueWork(() -> ClientModEvents.startShake(
                    payload.center(), payload.radius(), payload.ticks(), payload.intensity()));
        }

        private static void handleLookTipResponse(LookTipResponsePayload payload, IPayloadContext context) {
            context.enqueueWork(() -> LookTipOverlay.handleResponse(payload));
        }

        private static void handleBaFangYiOpen(BaFangYiOpenPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> icu.icuqalt10.panlingre.client.gui.BaFangYiScreen.openWith(payload.majors()));
        }

        private static void handleTaskGuideSync(TaskGuideSyncPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> icu.icuqalt10.panlingre.client.task.ClientTaskGuideState.handleSync(payload));
        }

        private static void handleTaskEntityResult(TaskEntityResultPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> icu.icuqalt10.panlingre.client.task.ClientTaskGuideState.handleEntityResult(payload));
        }
    }
}
