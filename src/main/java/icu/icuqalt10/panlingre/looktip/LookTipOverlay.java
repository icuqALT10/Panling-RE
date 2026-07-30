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

import java.util.Map;
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

        checkTarget(mc);

        if (currentTip != null) {
            renderTip(guiGraphics, currentTip, mc);
        }
    }

    private void checkTarget(Minecraft mc) {
        HitResult hitResult = getPlayerLookTarget(mc);

        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
            if (lastEntityUuid != null || lastBlockPos != null) {
                lastEntityUuid = null;
                lastBlockPos = null;
                lastBlockId = null;
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
                lastBlockId = null;
                lastBlockState = null;
                doMatch(mc, hitResult);
            }
        } else if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            BlockPos blockPos = blockHit.getBlockPos();
            var blockState = mc.level.getBlockState(blockPos);
            Block block = blockState.getBlock();
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
            String blockStateStr = blockState.toString();

            if (!blockPos.equals(lastBlockPos) || !blockId.equals(lastBlockId) || !blockStateStr.equals(lastBlockState)) {
                lastBlockPos = blockPos;
                lastBlockId = blockId;
                lastBlockState = blockStateStr;
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
                            PacketDistributor.sendToServer(LookTipRequestPayload.create(
                                    LookTipRequestPayload.TargetType.ENTITY,
                                    entity.getUUID(),
                                    BlockPos.ZERO
                            ));
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
                            PacketDistributor.sendToServer(LookTipRequestPayload.create(
                                    LookTipRequestPayload.TargetType.BLOCK,
                                    new UUID(0, 0),
                                    blockPos
                            ));
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
