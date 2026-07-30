package icu.icuqalt10.panlingre.looktip;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class LookTipNetworkHandler {

    public static void handleRequest(LookTipRequestPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ServerLevel level = (ServerLevel) serverPlayer.level();
        Optional<Component> result = Optional.empty();

        try {
            if (payload.getType() == LookTipRequestPayload.TargetType.ENTITY) {
                UUID entityUuid = payload.entityUuid();
                Entity entity = level.getEntity(entityUuid);

                if (entity != null) {
                    result = matchEntity(entity);
                }
            } else if (payload.getType() == LookTipRequestPayload.TargetType.BLOCK) {
                BlockPos pos = payload.blockPos();
                BlockState blockState = level.getBlockState(pos);
                BlockEntity blockEntity = level.getBlockEntity(pos);

                result = matchBlock(blockState, blockEntity);
            }
        } catch (Exception e) {
            PanlingRE.LOGGER.error("Error matching look tip", e);
        }

        // 发送响应给客户端
        if (result.isPresent()) {
            PacketDistributor.sendToPlayer(serverPlayer, LookTipResponsePayload.of(result.get()));
        } else {
            PacketDistributor.sendToPlayer(serverPlayer, LookTipResponsePayload.empty());
        }
    }

    private static Optional<Component> matchEntity(Entity entity) {
        Map<ResourceLocation, LookTipData> lookTips = LookTipLoader.getLookTips();

        for (LookTipData data : lookTips.values()) {
            for (LookTipData.EntityCondition condition : data.entries()) {
                if (LookTipMatcher.matchesEntity(entity, condition)) {
                    return Optional.of(data.title());
                }
            }
        }

        return Optional.empty();
    }

    private static Optional<Component> matchBlock(BlockState blockState, BlockEntity blockEntity) {
        Map<ResourceLocation, LookTipData> lookTips = LookTipLoader.getLookTips();

        for (LookTipData data : lookTips.values()) {
            for (LookTipData.EntityCondition condition : data.entries()) {
                if (LookTipMatcher.matchesBlock(blockState, blockEntity, condition)) {
                    return Optional.of(data.title());
                }
            }
        }

        return Optional.empty();
    }
}
