package icu.icuqalt10.panlingre.task;

import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.network.task.TaskEntityCheckPayload;
import icu.icuqalt10.panlingre.network.task.TaskEntityResultPayload;
import icu.icuqalt10.panlingre.network.task.TaskGuideSyncPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Arrays;
import java.util.Optional;

public final class TaskGuideService {
    private static final double ENTITY_RANGE_SQUARED = 50.0 * 50.0;

    private TaskGuideService() {
    }

    public static void activate(ServerPlayer player, ResourceLocation taskId) {
        TaskGuideLoader.LoadedTask loaded = TaskGuideLoader.get(taskId).orElseThrow();
        player.setData(ModAttachments.TASK_GUIDE.get(), new TaskGuideState(Optional.of(taskId)));
        PacketDistributor.sendToPlayer(
                player,
                new TaskGuideSyncPayload(true, taskId, loaded.json())
        );
    }

    public static void deactivate(ServerPlayer player, ResourceLocation taskId) {
        TaskGuideState state = player.getData(ModAttachments.TASK_GUIDE.get());
        if (state.activeTask().filter(taskId::equals).isEmpty()) {
            return;
        }
        player.setData(ModAttachments.TASK_GUIDE.get(), TaskGuideState.EMPTY);
        PacketDistributor.sendToPlayer(player, TaskGuideSyncPayload.disabled(taskId));
    }

    public static boolean isActive(ServerPlayer player, ResourceLocation taskId) {
        return player.getData(ModAttachments.TASK_GUIDE.get()).activeTask()
                .filter(taskId::equals)
                .isPresent();
    }

    public static void syncActive(ServerPlayer player) {
        TaskGuideState state = player.getData(ModAttachments.TASK_GUIDE.get());
        if (state.activeTask().isEmpty()) {
            return;
        }
        ResourceLocation taskId = state.activeTask().get();
        Optional<TaskGuideLoader.LoadedTask> loaded = TaskGuideLoader.get(taskId);
        if (loaded.isEmpty()) {
            player.setData(ModAttachments.TASK_GUIDE.get(), TaskGuideState.EMPTY);
            PacketDistributor.sendToPlayer(player, TaskGuideSyncPayload.disabled(taskId));
            return;
        }
        PacketDistributor.sendToPlayer(
                player,
                new TaskGuideSyncPayload(true, taskId, loaded.get().json())
        );
    }

    public static void handleEntityCheck(TaskEntityCheckPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> checkEntities(payload, context));
    }

    private static void checkEntities(TaskEntityCheckPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !isActive(player, payload.taskId())) {
            return;
        }
        Optional<TaskGuideLoader.LoadedTask> loadedOptional = TaskGuideLoader.get(payload.taskId());
        if (loadedOptional.isEmpty()
                || !loadedOptional.get().data().type().hasEntity()
                || loadedOptional.get().data().entity().isEmpty()) {
            return;
        }
        TaskGuideLoader.LoadedTask loaded = loadedOptional.get();
        TaskGuideData.EntityTarget target = loaded.data().entity().get();

        int[] matches = Arrays.stream(payload.entityIds())
                .distinct()
                .filter(id -> {
                    Entity entity = player.serverLevel().getEntity(id);
                    return entity != null
                            && entity != player
                            && entity.distanceToSqr(player) <= ENTITY_RANGE_SQUARED
                            && TaskGuideMatcher.matchesServer(entity, target, loaded.requiredNbt());
                })
                .limit(TaskEntityCheckPayload.MAX_CANDIDATES)
                .toArray();

        PacketDistributor.sendToPlayer(player, new TaskEntityResultPayload(payload.taskId(), matches));
    }
}
