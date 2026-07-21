package icu.icuqalt10.panlingre.looktip;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Objects;
import java.util.UUID;

public class LookTipOverlay implements LayeredDraw.Layer {
    public static final LookTipOverlay INSTANCE = new LookTipOverlay();

    private Component currentTip = null;

    // 缓存上一tick的目标信息
    private UUID lastEntityUuid = null;
    private BlockPos lastBlockPos = null;
    private ResourceLocation lastBlockId = null;
    private String lastBlockState = null;

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        // 每tick检查目标
        checkTarget(mc);

        // 渲染当前提示
        if (currentTip != null) {
            renderTip(guiGraphics, currentTip, mc);
        }
    }

    private void checkTarget(Minecraft mc) {
        HitResult hitResult = getPlayerLookTarget(mc);

        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
            // 没有看向任何东西
            if (lastEntityUuid != null || lastBlockPos != null) {
                // 目标变化了
                lastEntityUuid = null;
                lastBlockPos = null;
                lastBlockId = null;
                lastBlockState = null;

                // 发送请求
                PacketDistributor.sendToServer(LookTipRequestPayload.create(
                        LookTipRequestPayload.TargetType.NONE,
                        new UUID(0, 0),
                        BlockPos.ZERO
                ));
            }
            // 否则目标没变（依然是空），保持上次结果
            return;
        }

        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hitResult;
            Entity entity = entityHit.getEntity();
            UUID entityUuid = entity.getUUID();

            // 检查是否与上一tick相同
            if (!entityUuid.equals(lastEntityUuid)) {
                // 目标变化了
                lastEntityUuid = entityUuid;
                lastBlockPos = null;
                lastBlockId = null;
                lastBlockState = null;

                // 发送请求
                PacketDistributor.sendToServer(LookTipRequestPayload.create(
                        LookTipRequestPayload.TargetType.ENTITY,
                        entityUuid,
                        BlockPos.ZERO
                ));
            }
            // 否则目标没变，保持上次结果
        } else if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            BlockPos blockPos = blockHit.getBlockPos();
            var blockState = mc.level.getBlockState(blockPos);
            Block block = blockState.getBlock();
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
            String blockStateStr = blockState.toString();

            // 检查是否与上一tick相同（BlockPos + BlockId + BlockState）
            if (!blockPos.equals(lastBlockPos) || !blockId.equals(lastBlockId) || !blockStateStr.equals(lastBlockState)) {
                // 目标变化了
                lastBlockPos = blockPos;
                lastBlockId = blockId;
                lastBlockState = blockStateStr;
                lastEntityUuid = null;

                // 发送请求
                PacketDistributor.sendToServer(LookTipRequestPayload.create(
                        LookTipRequestPayload.TargetType.BLOCK,
                        new UUID(0, 0),
                        blockPos
                ));
            }
            // 否则目标没变，保持上次结果
        }
    }

    // 处理服务端响应
    public static void handleResponse(LookTipResponsePayload payload) {
        if (payload.hasResult()) {
            INSTANCE.currentTip = payload.tipText();
        } else {
            INSTANCE.currentTip = null;
        }
    }

    private void renderTip(GuiGraphics guiGraphics, Component tip, Minecraft mc) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        String text = tip.getString();
        String[] lines = text.split("\\\\n");

        int lineHeight = mc.font.lineHeight;
        int x = screenWidth / 2 + 20;
        int y = screenHeight / 2 + 30;

        RenderSystem.enableBlend();
        for (int i = 0; i < lines.length; i++) {
            int lineY = y + i * lineHeight;
            guiGraphics.drawString(mc.font, lines[i], x, lineY, 0xFFFFFF, true);
        }
        RenderSystem.disableBlend();
    }

    private HitResult getPlayerLookTarget(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return null;
        }

        double reachDistance = mc.player.blockInteractionRange();
        Vec3 eyePos = mc.player.getEyePosition(1.0f);
        Vec3 lookVec = mc.player.getViewVector(1.0f);
        Vec3 endPos = eyePos.add(lookVec.scale(reachDistance));

        HitResult blockHit = mc.level.clip(new ClipContext(
                eyePos,
                endPos,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                mc.player
        ));

        EntityHitResult entityHit = null;
        double closestDistance = blockHit.getType() == HitResult.Type.BLOCK ?
                eyePos.distanceTo(blockHit.getLocation()) : reachDistance;

        for (Entity entity : mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(reachDistance))) {
            if (entity == mc.player) {
                continue;
            }

            var entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());
            var optionalVec = entityBox.clip(eyePos, endPos);

            if (optionalVec.isPresent()) {
                double distance = eyePos.distanceTo(optionalVec.get());
                if (distance < closestDistance) {
                    entityHit = new EntityHitResult(entity, optionalVec.get());
                    closestDistance = distance;
                }
            }
        }

        return entityHit != null ? entityHit : blockHit;
    }
}
