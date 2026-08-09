package icu.icuqalt10.panlingre.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.init.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.entity.PartEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Draws an ice block model fitted to every frozen entity collision box. */
@EventBusSubscriber(modid = PanlingRE.MODID, value = Dist.CLIENT)
public final class FreezeEffectRenderer {
    private static final Set<UUID> SYNCED_FROZEN_ENTITIES = new HashSet<>();

    private FreezeEffectRenderer() {
    }

    public static void handleFreezeSync(UUID entityId, boolean frozen) {
        if (frozen) {
            SYNCED_FROZEN_ENTITIES.add(entityId);
        } else {
            SYNCED_FROZEN_ENTITIES.remove(entityId);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Vec3 cameraPosition = event.getCamera().getPosition();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        boolean rendered = false;

        for (Entity candidate : minecraft.level.entitiesForRendering()) {
            if (!(candidate instanceof LivingEntity entity)
                    || (!entity.hasEffect(ModEffects.freeze)
                    && !SYNCED_FROZEN_ENTITIES.contains(entity.getUUID()))) {
                continue;
            }

            PartEntity<?>[] parts = entity.isMultipartEntity() ? entity.getParts() : null;
            if (parts != null && parts.length > 0) {
                for (PartEntity<?> part : parts) {
                    if (part != null) {
                        renderCollisionBox(poseStack, buffers, cameraPosition, partialTick, part);
                        rendered = true;
                    }
                }
            } else {
                renderCollisionBox(poseStack, buffers, cameraPosition, partialTick, entity);
                rendered = true;
            }
        }

        if (rendered) {
            buffers.endBatch(ItemBlockRenderTypes.getRenderType(Blocks.ICE.defaultBlockState(), false));
        }
    }

    private static void renderCollisionBox(
            PoseStack poseStack,
            MultiBufferSource buffers,
            Vec3 cameraPosition,
            float partialTick,
            Entity boxEntity
    ) {
        AABB box = boxEntity.getBoundingBox();
        double width = box.getXsize();
        double height = box.getYsize();
        double depth = box.getZsize();
        if (width <= 0.0 || height <= 0.0 || depth <= 0.0) {
            return;
        }

        double entityX = Mth.lerp(partialTick, boxEntity.xo, boxEntity.getX());
        double entityY = Mth.lerp(partialTick, boxEntity.yo, boxEntity.getY());
        double entityZ = Mth.lerp(partialTick, boxEntity.zo, boxEntity.getZ());

        // Keep each world-axis-aligned block aligned with its interpolated AABB.
        double x = entityX - cameraPosition.x + box.minX - boxEntity.getX();
        double y = entityY - cameraPosition.y + box.minY - boxEntity.getY();
        double z = entityZ - cameraPosition.z + box.minZ - boxEntity.getZ();

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.scale((float) width, (float) height, (float) depth);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                Blocks.ICE.defaultBlockState(),
                poseStack,
                buffers,
                LevelRenderer.getLightColor(boxEntity.level(), boxEntity.blockPosition()),
                OverlayTexture.NO_OVERLAY
        );
        poseStack.popPose();
    }
}
