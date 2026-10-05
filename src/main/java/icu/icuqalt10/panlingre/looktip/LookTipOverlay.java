package icu.icuqalt10.panlingre.looktip;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;

public class LookTipOverlay implements LayeredDraw.Layer {
    public static final LookTipOverlay INSTANCE = new LookTipOverlay();

    private Component currentTip = null;

    // 缓存上一tick的目标信息
    private UUID lastEntityUuid = null;
    private BlockPos lastBlockPos = null;
    private BlockState lastBlockState = null;
    private ClientLevel lastLevel;
    private long lastCheckTick = -1;
    private Map<ResourceLocation, LookTipData> lastTips = Map.of();

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            resetTarget();
            lastLevel = null;
            return;
        }

        Map<ResourceLocation, LookTipData> tips = LookTipLoader.getLookTips();
        if (lastLevel != mc.level || lastTips != tips) {
            resetTarget();
            lastLevel = mc.level;
            lastTips = tips;
        }
        if (lastCheckTick != mc.level.getGameTime()) {
            lastCheckTick = mc.level.getGameTime();
            checkTarget(mc);
        }

        if (currentTip != null) {
            renderTip(guiGraphics, currentTip, mc);
        }
    }

    private void resetTarget() {
        currentTip = null;
        lastEntityUuid = null;
        lastBlockPos = null;
        lastBlockState = null;
        lastCheckTick = -1;
    }

    private void checkTarget(Minecraft mc) {
        HitResult hitResult = getPlayerLookTarget(mc);

        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
            if (lastEntityUuid != null || lastBlockPos != null) {
                lastEntityUuid = null;
                lastBlockPos = null;
                lastBlockState = null;
                doMatch(mc, hitResult);
            }
            return;
        }

        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hitResult;
            Entity entity = entityHit.getEntity();
            UUID entityUuid = entity.getUUID();

            if (!entityUuid.equals(lastEntityUuid)) {
                lastEntityUuid = entityUuid;
                lastBlockPos = null;
                lastBlockState = null;
                doMatch(mc, hitResult);
            }
        } else if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            BlockPos blockPos = blockHit.getBlockPos();
            var blockState = mc.level.getBlockState(blockPos);

            if (!blockPos.equals(lastBlockPos) || blockState != lastBlockState) {
                lastBlockPos = blockPos;
                lastBlockState = blockState;
                lastEntityUuid = null;
                doMatch(mc, hitResult);
            }
        }
    }

    /**
     * 执行匹配：先在客户端匹配 name/pos/block_state，
     * 如果没有 nbt 则直接显示，有 nbt 则发包给服务端
     */
    private void doMatch(Minecraft mc, HitResult hitResult) {
        Map<ResourceLocation, LookTipData> lookTips = LookTipLoader.getLookTips();

        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
            currentTip = null;
            return;
        }

        // 远程客户端没有服务端数据包配置，交由服务器匹配当前目标。
        if (!mc.hasSingleplayerServer()) {
            requestServerMatch(hitResult);
            return;
        }

        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hitResult;
            Entity entity = entityHit.getEntity();

            for (LookTipData data : lookTips.values()) {
                for (LookTipData.EntityCondition condition : data.entries()) {
                    if (!"entity".equals(condition.type())) continue;

                    if (LookTipMatcher.matchesEntityClient(entity, condition)) {
                        // 客户端条件通过，检查是否需要 nbt
                        if (needsNbt(condition)) {
                            // 发包给服务端验证 nbt
                            requestServerMatch(hitResult);
                            return;
                        } else {
                            // 不需要 nbt，直接显示
                            currentTip = data.title();
                            return;
                        }
                    }
                }
            }
        } else if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            BlockPos blockPos = blockHit.getBlockPos();
            var blockState = mc.level.getBlockState(blockPos);

            for (LookTipData data : lookTips.values()) {
                for (LookTipData.EntityCondition condition : data.entries()) {
                    if (!"block".equals(condition.type())) continue;

                    if (LookTipMatcher.matchesBlockClient(blockState, blockPos, condition)) {
                        // 客户端条件通过，检查是否需要 nbt
                        if (needsNbt(condition)) {
                            // 发包给服务端验证 nbt
                            requestServerMatch(hitResult);
                            return;
                        } else {
                            // 不需要 nbt，直接显示
                            currentTip = data.title();
                            return;
                        }
                    }
                }
            }
        }

        // 没有匹配到任何条件
        currentTip = null;
    }

    private boolean needsNbt(LookTipData.EntityCondition condition) {
        return condition.nbt().isPresent() && !condition.nbt().get().isEmpty();
    }

    private void requestServerMatch(HitResult hitResult) {
        currentTip = null;
        if (hitResult instanceof EntityHitResult entityHit) {
            PacketDistributor.sendToServer(LookTipRequestPayload.create(
                    LookTipRequestPayload.TargetType.ENTITY, entityHit.getEntity().getUUID(), BlockPos.ZERO));
        } else if (hitResult instanceof BlockHitResult blockHit) {
            PacketDistributor.sendToServer(LookTipRequestPayload.create(
                    LookTipRequestPayload.TargetType.BLOCK, new UUID(0, 0), blockHit.getBlockPos()));
        }
    }

    // 处理服务端响应
    public static void handleResponse(LookTipResponsePayload payload) {
        if (INSTANCE.lastEntityUuid == null && INSTANCE.lastBlockPos == null) return;
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
                eyePos.distanceToSqr(blockHit.getLocation()) : reachDistance * reachDistance;

        for (Entity entity : mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(reachDistance))) {
            if (entity == mc.player) {
                continue;
            }

            var entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());
            var optionalVec = entityBox.clip(eyePos, endPos);

            if (optionalVec.isPresent()) {
                double distance = eyePos.distanceToSqr(optionalVec.get());
                if (distance < closestDistance) {
                    entityHit = new EntityHitResult(entity, optionalVec.get());
                    closestDistance = distance;
                }
            }
        }

        return entityHit != null ? entityHit : blockHit;
    }
}
