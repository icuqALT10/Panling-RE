package icu.icuqalt10.panlingre.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = PanlingRE.MODID, value = Dist.CLIENT)
public class GroundSmashRenderer {

    // 内部数据类：记录每一次的裂地特效
    private static class SmashInstance {
        final Vec3 center;
        final float radius;
        final int maxTicks;
        int currentTicks;

        SmashInstance(Vec3 center, float radius, int ticks) {
            this.center = center;
            this.radius = radius;
            this.maxTicks = ticks;
            this.currentTicks = ticks;
        }
    }

    private static final List<SmashInstance> ACTIVE_EFFECTS = new ArrayList<>();

    // ==========================================
    // 外部调用接口（重载方法）
    // ==========================================

    /**
     * 触发地面错位特效 (Vec3 传参)
     * @param center 中心点坐标
     * @param radius 扩散半径（格）
     * @param ticks  持续时间（20 ticks = 1秒）
     */
    public static void triggerSmash(Vec3 center, float radius, int ticks) {
        ACTIVE_EFFECTS.add(new SmashInstance(center, radius, ticks));
    }

    /**
     * 触发地面错位特效 (BlockPos 传参)
     */
    public static void triggerSmash(BlockPos center, float radius, int ticks) {
        triggerSmash(center.getBottomCenter(), radius, ticks);
    }

    // ==========================================
    // 内部处理逻辑：Tick递减与动态渲染
    // ==========================================

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (ACTIVE_EFFECTS.isEmpty()) return;
        Iterator<SmashInstance> iterator = ACTIVE_EFFECTS.iterator();
        while (iterator.hasNext()) {
            SmashInstance instance = iterator.next();
            instance.currentTicks--;
            if (instance.currentTicks <= 0) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // 在实体渲染之后绘制，确保不被阻挡，且矩阵完全初始化
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES && !ACTIVE_EFFECTS.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            Level level = mc.level;
            if (level == null) return;

            PoseStack poseStack = event.getPoseStack();
            Vec3 cameraPos = event.getCamera().getPosition();
            BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
            MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

            for (SmashInstance effect : ACTIVE_EFFECTS) {
                // 计算扩散进度 (0.0 -> 1.0)
                float progress = 1.0f - ((float) effect.currentTicks / effect.maxTicks);
                float currentWaveFront = effect.radius * progress; // 当前波纹扩散到的半径

                int r = (int) Math.ceil(effect.radius);
                BlockPos centerPos = BlockPos.containing(effect.center);

                // 遍历半径内的所有X, Z坐标
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        double distSq = x * x + z * z;
                        if (distSq <= effect.radius * effect.radius) {

                            // 寻找该坐标真正的地面方块（向下扫描以防获取到树叶）
                            BlockPos targetPos = null;
                            BlockState targetState = null;
                            for (int y = centerPos.getY() + 3; y >= centerPos.getY() - 5; y--) {
                                BlockPos p = new BlockPos(centerPos.getX() + x, y, centerPos.getZ() + z);
                                BlockState s = level.getBlockState(p);
                                if (s.isSolidRender(level, p)) {
                                    targetPos = p;
                                    targetState = s;
                                    break;
                                }
                            }

                            if (targetPos == null) continue;

                            double distToCenter = Math.sqrt(distSq);
                            float distToWave = (float) Math.abs(distToCenter - currentWaveFront);

                            // 如果该方块处于当前波纹边缘附近，则产生错位动画
                            if (distToWave < 2.5f) {
                                float intensity = (2.5f - distToWave) / 2.5f;

                                // 计算弹起高度和随机倾斜
                                float offsetY = (float) Math.sin(progress * Math.PI) * intensity * 1.2f;

                                // 只有产生了明显位移才渲染（优化性能）
                                if (offsetY > 0.05f) {
                                    poseStack.pushPose();

                                    // 将矩阵平移到相对相机的位置，并加上Y轴错位
                                    poseStack.translate(
                                            targetPos.getX() - cameraPos.x,
                                            targetPos.getY() - cameraPos.y + offsetY,
                                            targetPos.getZ() - cameraPos.z
                                    );

                                    // 加上随机错位旋转（更还原“撕裂”感）
                                    float rotX = intensity * 15f * ((x + z) % 2 == 0 ? 1 : -1);
                                    float rotZ = intensity * 10f * (x % 2 == 0 ? 1 : -1);

                                    // 移动到方块中心旋转
                                    poseStack.translate(0.5, 0.5, 0.5);
                                    poseStack.mulPose(Axis.XP.rotationDegrees(rotX));
                                    poseStack.mulPose(Axis.ZP.rotationDegrees(rotZ));
                                    poseStack.translate(-0.5, -0.5, -0.5);

                                    // 渲染假方块
                                    dispatcher.renderSingleBlock(
                                            targetState, poseStack, bufferSource,
                                            LightTexture.FULL_BRIGHT, // 或者用 level.getLightEmission()
                                            OverlayTexture.NO_OVERLAY,
                                            ModelData.EMPTY, null
                                    );

                                    poseStack.popPose();
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}