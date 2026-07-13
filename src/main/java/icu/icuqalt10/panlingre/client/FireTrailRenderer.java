package icu.icuqalt10.panlingre.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@EventBusSubscriber(modid = PanlingRE.MODID, value = Dist.CLIENT)
public class FireTrailRenderer {

    private static class TrailBlock {
        final BlockPos pos;
        final BlockState originalState;
        int remainingTicks;

        TrailBlock(BlockPos pos, BlockState originalState, int ticks) {
            this.pos = pos;
            this.originalState = originalState;
            this.remainingTicks = ticks;
        }
    }

    private static final Map<BlockPos, TrailBlock> ACTIVE_TRAILS = new HashMap<>();
    private static final BlockState MAGMA_BLOCK = Blocks.MAGMA_BLOCK.defaultBlockState();

    /**
     * 添加一个火焰轨迹方块
     * @param pos 方块位置
     * @param originalState 原始方块状态
     * @param ticks 持续时间（20 ticks = 1秒）
     */
    public static void addTrailBlock(BlockPos pos, BlockState originalState, int ticks) {
        ACTIVE_TRAILS.put(pos, new TrailBlock(pos, originalState, ticks));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (ACTIVE_TRAILS.isEmpty()) return;

        Iterator<Map.Entry<BlockPos, TrailBlock>> iterator = ACTIVE_TRAILS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, TrailBlock> entry = iterator.next();
            TrailBlock trail = entry.getValue();
            trail.remainingTicks--;
            if (trail.remainingTicks <= 0) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES && !ACTIVE_TRAILS.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            Level level = mc.level;
            if (level == null) return;

            PoseStack poseStack = event.getPoseStack();
            Vec3 cameraPos = event.getCamera().getPosition();
            BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
            MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

            for (TrailBlock trail : ACTIVE_TRAILS.values()) {
                BlockPos pos = trail.pos;

                // 检查方块是否被改动
                BlockState currentState = level.getBlockState(pos);
                if (!currentState.equals(trail.originalState)) continue;

                poseStack.pushPose();

                // 1. 计算从方块中心指向相机的方向向量
                Vec3 blockCenter = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                Vec3 dirToCamera = new Vec3(cameraPos.x, cameraPos.y, cameraPos.z)
                        .subtract(blockCenter)
                        .normalize();

                // 2. 沿视线方向拉近 0.005 格（5毫米，足以避开深度冲突，肉眼看不出来）
                double offset = 0.005;
                poseStack.translate(
                        pos.getX() - cameraPos.x + dirToCamera.x * offset,
                        pos.getY() - cameraPos.y + dirToCamera.y * offset,
                        pos.getZ() - cameraPos.z + dirToCamera.z * offset
                );

                // 3. 禁用背面剔除 —— 即使方块因为偏移出现极小的几何走样，
                //    内侧面也会被渲染，彻底遮住原方块，杜绝闪烁
                GlStateManager._disableCull();

                // 渲染岩浆块
                dispatcher.renderSingleBlock(
                        MAGMA_BLOCK,
                        poseStack,
                        bufferSource,
                        LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY,
                        ModelData.EMPTY,
                        null
                );

                // 4. 恢复背面剔除（避免影响后续渲染）
                GlStateManager._enableCull();

                poseStack.popPose();
            }
        }
    }
}
