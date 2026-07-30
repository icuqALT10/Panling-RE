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
    private static int selectedEntry = -1;
    private static Set<Integer> highlightedEntityIds = Set.of();
    private static int ticksUntilEntityCheck;
    private static boolean guidanceVisible = true;

    private ClientTaskGuideState() {
    }

    public static void handleSync(TaskGuideSyncPayload payload) {
        if (!payload.enabled()) {
            clear();
            return;
        }

        TaskGuideData.parse(JsonParser.parseString(payload.json()))
                .resultOrPartial(error -> PanlingRE.LOGGER.error(
                        "Failed to parse synced task guide {}: {}", payload.taskId(), error))
                .ifPresentOrElse(data -> {
                    if (payload.selectedEntry() < -1 || payload.selectedEntry() >= data.entries().size()) {
                        PanlingRE.LOGGER.error(
                                "Task guide {} selected invalid entry {}",
                                payload.taskId(), payload.selectedEntry()
                        );
                        clear();
                        return;
                    }
                    boolean changedTask = !payload.taskId().equals(taskId);
                    taskId = payload.taskId();
                    task = data;
                    selectedEntry = payload.selectedEntry();
                    highlightedEntityIds = Set.of();
                    ticksUntilEntityCheck = 0;
                    if (changedTask) {
                        guidanceVisible = true;
                    }
                }, ClientTaskGuideState::clear);
    }

    public static void handleEntityResult(TaskEntityResultPayload payload) {
        if (taskId == null
                || !taskId.equals(payload.taskId())
                || selectedEntry != payload.selectedEntry()
                || !guidanceVisible
                || entry()
                        .flatMap(TaskGuideData.Entry::entity)
                        .flatMap(TaskGuideData.EntityTarget::nbt)
                        .isEmpty()) {
            return;
        }
        Set<Integer> result = new HashSet<>();
        Arrays.stream(payload.entityIds()).forEach(result::add);
        highlightedEntityIds = Set.copyOf(result);
    }

    public static Optional<TaskGuideData> task() {
        return Optional.ofNullable(task);
    }

    public static Optional<TaskGuideData.Entry> entry() {
        if (task == null || selectedEntry < 0 || selectedEntry >= task.entries().size()) {
            return Optional.empty();
        }
        return Optional.of(task.entries().get(selectedEntry));
    }

    public static boolean shouldGlow(Entity entity) {
        return highlightedEntityIds.contains(entity.getId());
    }

    public static int outlineColor() {
        return entry().flatMap(TaskGuideData.Entry::entity).isPresent()
                ? entry().flatMap(TaskGuideData.Entry::entity).orElseThrow().outlineColor()
                : 0xE8C96A;
    }

    public static boolean isGuidanceVisible() {
        return guidanceVisible;
    }

    public static void toggleGuidance() {
        Optional<TaskGuideData.Entry> selected = entry();
        if (selected.isPresent()
                && (selected.get().target().isPresent() || selected.get().entity().isPresent())) {
            guidanceVisible = !guidanceVisible;
            highlightedEntityIds = Set.of();
            ticksUntilEntityCheck = 0;
        }
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            clear();
            return;
        }
        if (!guidanceVisible) {
            highlightedEntityIds = Set.of();
            ticksUntilEntityCheck = 0;
            return;
        }
        Optional<TaskGuideData.EntityTarget> entityTarget = entry().flatMap(TaskGuideData.Entry::entity);
        if (taskId == null || entityTarget.isEmpty()) {
            highlightedEntityIds = Set.of();
            return;
        }
        if (--ticksUntilEntityCheck > 0) {
            return;
        }
        ticksUntilEntityCheck = 5;

        TaskGuideData.EntityTarget target = entityTarget.get();
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

        if (target.nbt().isEmpty()) {
            Set<Integer> matches = new HashSet<>();
            Arrays.stream(candidates).forEach(matches::add);
            highlightedEntityIds = Set.copyOf(matches);
            return;
        }

        PacketDistributor.sendToServer(new TaskEntityCheckPayload(taskId, selectedEntry, candidates));
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static void clear() {
        taskId = null;
        task = null;
        selectedEntry = -1;
        highlightedEntityIds = Set.of();
        ticksUntilEntityCheck = 0;
        guidanceVisible = true;
    }
}
