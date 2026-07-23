package icu.icuqalt10.panlingre.client.task;

import com.google.gson.JsonParser;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.network.task.TaskEntityCheckPayload;
import icu.icuqalt10.panlingre.network.task.TaskEntityResultPayload;
import icu.icuqalt10.panlingre.network.task.TaskGuideSyncPayload;
import icu.icuqalt10.panlingre.task.TaskGuideData;
import icu.icuqalt10.panlingre.task.TaskGuideMatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@EventBusSubscriber(modid = PanlingRE.MODID, value = Dist.CLIENT)
public final class ClientTaskGuideState {
    private static final double ENTITY_RANGE = 50.0;
    private static ResourceLocation taskId;
    private static TaskGuideData task;
    private static Set<Integer> highlightedEntityIds = Set.of();
    private static int ticksUntilEntityCheck;

    private ClientTaskGuideState() {
    }

    public static void handleSync(TaskGuideSyncPayload payload) {
        if (!payload.enabled()) {
            if (payload.taskId().equals(taskId)) {
                clear();
            }
            return;
        }

        TaskGuideData.parse(JsonParser.parseString(payload.json()))
                .resultOrPartial(error -> PanlingRE.LOGGER.error(
                        "Failed to parse synced task guide {}: {}", payload.taskId(), error))
                .ifPresentOrElse(data -> {
                    taskId = payload.taskId();
                    task = data;
                    highlightedEntityIds = Set.of();
                    ticksUntilEntityCheck = 0;
                }, ClientTaskGuideState::clear);
    }

    public static void handleEntityResult(TaskEntityResultPayload payload) {
        if (taskId == null || !taskId.equals(payload.taskId())) {
            return;
        }
        Set<Integer> result = new HashSet<>();
        Arrays.stream(payload.entityIds()).forEach(result::add);
        highlightedEntityIds = Set.copyOf(result);
    }

    public static Optional<TaskGuideData> task() {
        return Optional.ofNullable(task);
    }

    public static boolean shouldGlow(Entity entity) {
        return highlightedEntityIds.contains(entity.getId());
    }

    public static int outlineColor() {
        return task != null && task.entity().isPresent()
                ? task.entity().get().outlineColor()
                : 0xE8C96A;
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            clear();
            return;
        }
        if (task == null || taskId == null || !task.type().hasEntity() || task.entity().isEmpty()) {
            highlightedEntityIds = Set.of();
            return;
        }
        if (--ticksUntilEntityCheck > 0) {
            return;
        }
        ticksUntilEntityCheck = 5;

        TaskGuideData.EntityTarget target = task.entity().get();
        int[] candidates = minecraft.level
                .getEntities(
                        minecraft.player,
                        minecraft.player.getBoundingBox().inflate(ENTITY_RANGE),
                        entity -> entity.distanceToSqr(minecraft.player) <= ENTITY_RANGE * ENTITY_RANGE
                                && TaskGuideMatcher.matchesClient(entity, target)
                )
                .stream()
                .sorted((first, second) -> Double.compare(
                        first.distanceToSqr(minecraft.player),
                        second.distanceToSqr(minecraft.player)
                ))
                .limit(TaskEntityCheckPayload.MAX_CANDIDATES)
                .mapToInt(Entity::getId)
                .toArray();

        PacketDistributor.sendToServer(new TaskEntityCheckPayload(taskId, candidates));
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static void clear() {
        taskId = null;
        task = null;
        highlightedEntityIds = Set.of();
        ticksUntilEntityCheck = 0;
    }
}
