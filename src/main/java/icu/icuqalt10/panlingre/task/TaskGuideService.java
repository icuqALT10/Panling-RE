package icu.icuqalt10.panlingre.task;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.network.task.TaskEntityCheckPayload;
import icu.icuqalt10.panlingre.network.task.TaskEntityResultPayload;
import icu.icuqalt10.panlingre.network.task.TaskGuideSyncPayload;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = PanlingRE.MODID)
public final class TaskGuideService {
    private static final double ENTITY_RANGE_SQUARED = 50.0 * 50.0;
    private static final int REFRESH_INTERVAL_TICKS = 3;
    private static final Map<UUID, SelectionState> SELECTIONS = new HashMap<>();
    private static final Set<String> REPORTED_COMMAND_ERRORS = new HashSet<>();
    private static int refreshTicks;

    private TaskGuideService() {
    }

    public static void activate(ServerPlayer player, ResourceLocation taskId) {
        player.setData(ModAttachments.TASK_GUIDE.get(), new TaskGuideState(Optional.of(taskId)));
        refresh(player, true);
    }

    public static void deactivate(ServerPlayer player) {
        TaskGuideState state = player.getData(ModAttachments.TASK_GUIDE.get());
        player.setData(ModAttachments.TASK_GUIDE.get(), TaskGuideState.EMPTY);
        SELECTIONS.remove(player.getUUID());
        PacketDistributor.sendToPlayer(
                player,
                state.activeTask()
                        .map(TaskGuideSyncPayload::disabled)
                        .orElseGet(TaskGuideSyncPayload::disabled)
        );
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
            SELECTIONS.remove(player.getUUID());
            PacketDistributor.sendToPlayer(player, TaskGuideSyncPayload.disabled(taskId));
            return;
        }
        refresh(player, true);
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        if (++refreshTicks < REFRESH_INTERVAL_TICKS) {
            return;
        }
        refreshTicks = 0;

        MinecraftServer server = event.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.getData(ModAttachments.TASK_GUIDE.get()).activeTask().isPresent()) {
                refresh(player, false);
            } else {
                SELECTIONS.remove(player.getUUID());
            }
        }
    }

    private static void refresh(ServerPlayer player, boolean forceSync) {
        Optional<ResourceLocation> activeTask = player.getData(ModAttachments.TASK_GUIDE.get()).activeTask();
        if (activeTask.isEmpty()) {
            SELECTIONS.remove(player.getUUID());
            return;
        }

        ResourceLocation taskId = activeTask.get();
        Optional<TaskGuideLoader.LoadedTask> loadedOptional = TaskGuideLoader.get(taskId);
        if (loadedOptional.isEmpty()) {
            player.setData(ModAttachments.TASK_GUIDE.get(), TaskGuideState.EMPTY);
            SELECTIONS.remove(player.getUUID());
            PacketDistributor.sendToPlayer(player, TaskGuideSyncPayload.disabled(taskId));
            return;
        }

        TaskGuideLoader.LoadedTask loaded = loadedOptional.get();
        int selectedEntry = selectFirstExecutable(player, taskId, loaded.data());

        // 条件指令本身可能修改任务状态；此时由对应指令负责同步，不再发送旧任务。
        if (!isActive(player, taskId)) {
            return;
        }

        if (selectedEntry >= 0 && shouldDeactivateWhenLocated(player, loaded, selectedEntry)) {
            deactivate(player);
            return;
        }

        SelectionState previous = SELECTIONS.get(player.getUUID());
        boolean changed = previous == null
                || !previous.taskId.equals(taskId)
                || previous.entryIndex != selectedEntry
                || !previous.json.equals(loaded.json());
        if (forceSync || changed) {
            SELECTIONS.put(player.getUUID(), new SelectionState(taskId, selectedEntry, loaded.json()));
            PacketDistributor.sendToPlayer(
                    player,
                    new TaskGuideSyncPayload(true, taskId, loaded.json(), selectedEntry)
            );
        }
    }

    private static int selectFirstExecutable(
            ServerPlayer player,
            ResourceLocation taskId,
            TaskGuideData data
    ) {
        CommandSourceStack source = player.createCommandSourceStack()
                .withPermission(4)
                .withSuppressedOutput();
        for (int index = 0; index < data.entries().size(); index++) {
            Optional<String> command = data.entries().get(index).command();
            if (command.isEmpty()) {
                return index;
            }
            try {
                int result = player.getServer().getCommands().getDispatcher().execute(command.get(), source);
                if (result > 0) {
                    return index;
                }
            } catch (CommandSyntaxException exception) {
                String errorKey = taskId + "#" + index + "#" + exception.getMessage();
                if (REPORTED_COMMAND_ERRORS.add(errorKey)) {
                    PanlingRE.LOGGER.warn(
                            "Task guide condition failed {} entries[{}]: {}",
                            taskId, index, exception.getMessage()
                    );
                }
            }
        }
        return -1;
    }

    private static boolean shouldDeactivateWhenLocated(
            ServerPlayer player,
            TaskGuideLoader.LoadedTask loaded,
            int selectedEntry
    ) {
        TaskGuideData.Entry entry = loaded.data().entries().get(selectedEntry);
        if (!entry.offWhenLocated()) {
            return false;
        }

        if (entry.target().isPresent()) {
            TaskGuideData.ExactPosition pos = entry.target().get().pos();
            Vec3 waypointPosition = new Vec3(pos.x() + 0.5, pos.y() + 1.5, pos.z() + 0.5);
            if (player.getEyePosition().distanceToSqr(waypointPosition) <= 3.0 * 3.0) {
                return true;
            }
        }

        if (entry.entity().isEmpty()) {
            return false;
        }
        TaskGuideData.EntityTarget target = entry.entity().get();
        return !player.serverLevel().getEntities(
                player,
                player.getBoundingBox().inflate(2.0),
                entity -> entity.distanceToSqr(player) <= 2.0 * 2.0
                        && TaskGuideMatcher.matchesServer(
                                entity,
                                target,
                                loaded.requiredNbts().get(selectedEntry)
                        )
        ).isEmpty();
    }

    public static void handleEntityCheck(TaskEntityCheckPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> checkEntities(payload, context));
    }

    private static void checkEntities(TaskEntityCheckPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !isActive(player, payload.taskId())) {
            return;
        }
        SelectionState selection = SELECTIONS.get(player.getUUID());
        if (selection == null
                || !selection.taskId.equals(payload.taskId())
                || selection.entryIndex != payload.selectedEntry()) {
            return;
        }
        Optional<TaskGuideLoader.LoadedTask> loadedOptional = TaskGuideLoader.get(payload.taskId());
        if (loadedOptional.isEmpty()
                || selection.entryIndex < 0
                || selection.entryIndex >= loadedOptional.get().data().entries().size()) {
            return;
        }
        TaskGuideLoader.LoadedTask loaded = loadedOptional.get();
        TaskGuideData.Entry entry = loaded.data().entries().get(selection.entryIndex);
        if (entry.entity().isEmpty()) {
            return;
        }
        TaskGuideData.EntityTarget target = entry.entity().get();

        int[] matches = Arrays.stream(payload.entityIds())
                .distinct()
                .filter(id -> {
                    Entity entity = player.serverLevel().getEntity(id);
                    return entity != null
                            && entity != player
                            && entity.distanceToSqr(player) <= ENTITY_RANGE_SQUARED
                            && TaskGuideMatcher.matchesServer(
                                    entity,
                                    target,
                                    loaded.requiredNbts().get(selection.entryIndex)
                            );
                })
                .limit(TaskEntityCheckPayload.MAX_CANDIDATES)
                .toArray();

        PacketDistributor.sendToPlayer(
                player,
                new TaskEntityResultPayload(payload.taskId(), selection.entryIndex, matches)
        );
    }

    private record SelectionState(ResourceLocation taskId, int entryIndex, String json) {
    }
}
