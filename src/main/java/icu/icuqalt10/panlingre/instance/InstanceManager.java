package icu.icuqalt10.panlingre.instance;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;

public final class InstanceManager {
    public static final String ACTIVE_PLAYER_TAG = "panlingre.instance.active";
    private static final Map<ResourceLocation, RuntimeDefinition> RUNTIMES = new LinkedHashMap<>();
    private static final Map<UUID, InstanceSession> SESSIONS = new HashMap<>();
    private static final Map<UUID, InstanceSession> OWNED_ENTITIES = new HashMap<>();
    private static final Queue<BuildRequest> BUILD_QUEUE = new ArrayDeque<>();
    private static MinecraftServer server;
    private static InstanceSavedData savedData;
    private static int loadedGeneration = -1;
    private static InstanceRegionTask activeBuild;
    private static BuildRequest activeRequest;

    private InstanceManager() {
    }

    public static void startServer(MinecraftServer minecraftServer) {
        server = minecraftServer;
        savedData = minecraftServer.overworld().getDataStorage()
                .computeIfAbsent(InstanceSavedData.FACTORY, "panlingre_instances");
        reloadDefinitions();
    }

    public static void stopServer() {
        for (InstanceSession session : new HashSet<>(SESSIONS.values())) {
            for (ServerPlayer player : session.players()) failAndKill(player);
            if (!session.isEnding()) finish(session, InstanceResult.FAILURE);
        }
        if (activeBuild != null) activeBuild.finishIO();
        activeBuild = null;
        activeRequest = null;
        RUNTIMES.clear();
        SESSIONS.clear();
        OWNED_ENTITIES.clear();
        BUILD_QUEUE.clear();
        server = null;
        savedData = null;
    }

    public static void tick() {
        if (server == null) return;
        if (loadedGeneration != InstanceDefinitionLoader.generation() && SESSIONS.isEmpty()) reloadDefinitions();

        if (activeBuild == null) {
            int attempts = Math.min(4, BUILD_QUEUE.size());
            for (int i = 0; i < attempts && activeBuild == null; i++) {
                BuildRequest request = BUILD_QUEUE.remove();
                var runtime = RUNTIMES.get(request.id);
                if (runtime != null && InstanceRegionTask.unloaded(level(runtime.definition), runtime.definition, request.slot))
                    beginBuild(request);
                else BUILD_QUEUE.add(request);
            }
        }
        if (activeBuild != null) {
            try {
                if (activeBuild.tick()) {
                    var runtime = RUNTIMES.get(activeRequest.id);
                    savedData.setPreparedVersion(activeRequest.id.toString(), activeRequest.slot, runtime.definition.arenaVersion());
                    server.overworld().getDataStorage().save();
                    runtime.slots.set(activeRequest.slot, SlotState.READY);
                    PanlingRE.LOGGER.info("Saved baked instance {} slot {}", activeRequest.id, activeRequest.slot);
                    activeBuild = null; activeRequest = null;
                } else if (activeBuild.blocked()) {
                    BUILD_QUEUE.add(activeRequest);
                    activeBuild = null; activeRequest = null;
                }
            } catch (RuntimeException exception) {
                PanlingRE.LOGGER.error("Cannot prepare instance {} slot {}; it remains unavailable", activeRequest.id, activeRequest.slot, exception);
                RUNTIMES.get(activeRequest.id).slots.set(activeRequest.slot, SlotState.FAILED);
                activeBuild = null; activeRequest = null;
            }
        }

        for (InstanceSession session : new HashSet<>(SESSIONS.values())) {
            if (session.isEnding()) continue;
            ServerLevel level = session.level();
            if (level == null) {
                finish(session, InstanceResult.FAILURE);
                continue;
            }
            for (UUID id : session.participantIds()) {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player == null) {
                    SESSIONS.remove(id);
                    session.removeParticipant(id);
                } else if (!player.isAlive()) {
                    failPlayer(player);
                } else if (player.level() != level || !session.definition().contains(session.slot(), player.position())) {
                    failAndKill(player);
                }
            }
            if (session.participantIds().isEmpty()) finish(session, InstanceResult.FAILURE);
            if (!session.isEnding()) session.controller().tick(session);
        }
    }

    public static boolean start(ServerPlayer player, ResourceLocation id) {
        return start(List.of(player), id);
    }

    public static boolean start(Collection<ServerPlayer> targets, ResourceLocation id) {
        return start(targets, id, null);
    }

    static boolean start(Collection<ServerPlayer> targets, ResourceLocation id, CommandSourceStack source) {
        List<ServerPlayer> players = targets.stream().distinct().toList();
        if (players.isEmpty()) return false;
        for (ServerPlayer player : players) {
            if (SESSIONS.containsKey(player.getUUID())) {
                players.forEach(member -> member.sendSystemMessage(Component.translatable("command.panlingre.instance.already_running")));
                return false;
            }
        }
        RuntimeDefinition runtime = RUNTIMES.get(id);
        if (runtime == null) {
            players.forEach(player -> player.sendSystemMessage(Component.translatable("command.panlingre.instance.unknown", id.toString())));
            return false;
        }
        var limit = runtime.definition.playerLimitCount();
        if (players.size() < limit.min() || players.size() > limit.max()) {
            boolean tooFew = players.size() < limit.min();
            Component reason = Component.translatable(tooFew ? "command.panlingre.instance.too_few_players"
                    : "command.panlingre.instance.too_many_players", tooFew ? limit.min() : limit.max(), players.size());
            players.forEach(player -> player.sendSystemMessage(reason));
            if (source != null && !players.contains(source.getEntity())) source.sendFailure(reason);
            return false;
        }
        int slot = runtime.firstReadySlot();
        if (slot < 0) {
            replenish(id, runtime);
            players.forEach(player -> player.sendSystemMessage(Component.translatable("command.panlingre.instance.not_ready")));
            return false;
        }
        Optional<InstanceController> controller = InstanceControllerRegistry.create(runtime.definition.controller());
        if (controller.isEmpty()) {
            players.forEach(player -> player.sendSystemMessage(Component.translatable("command.panlingre.instance.controller_missing", runtime.definition.controller().toString())));
            return false;
        }
        ServerLevel targetLevel = level(runtime.definition);
        if (targetLevel == null) return false;
        List<Vec3> spawns = InstanceSpawnPlacement.find(targetLevel, runtime.definition, slot, players);
        if (spawns.size() != players.size()) {
            players.forEach(player -> player.sendSystemMessage(Component.translatable("command.panlingre.instance.spawn_full")));
            return false;
        }

        runtime.slots.set(slot, SlotState.RUNNING);
        replenish(id, runtime);
        savedData.setPreparedVersion(id.toString(), slot, -1);
        InstanceSession session = new InstanceSession(server, id, runtime.definition, slot, players, controller.get());
        for (int i = 0; i < players.size(); i++) {
            ServerPlayer player = players.get(i);
            Vec3 spawn = spawns.get(i);
            var rotation = runtime.definition.rotation();
            SESSIONS.put(player.getUUID(), session);
            player.addTag(ACTIVE_PLAYER_TAG);
            player.teleportTo(targetLevel, spawn.x, spawn.y, spawn.z, rotation.yaw(), rotation.pitch());
            executeFunction(player, Optional.of(ResourceLocation.fromNamespaceAndPath("plre", "instances/into_load")));
            executeFunction(player, runtime.definition.intoFunction());
        }
        controller.get().start(session);
        return true;
    }

    public static void finish(InstanceSession session, InstanceResult result) {
        if (session.isEnding()) return;
        session.markEnding();
        List<ServerPlayer> players = session.players();
        for (UUID id : session.participantIds()) SESSIONS.remove(id);
        players.forEach(player -> player.removeTag(ACTIVE_PLAYER_TAG));
        OWNED_ENTITIES.entrySet().removeIf(entry -> entry.getValue() == session);
        session.controller().stop(session, result);

        RuntimeDefinition runtime = RUNTIMES.get(session.instanceId());
        if (runtime != null && session.slot() < runtime.slots.size()) {
            runtime.slots.set(session.slot(), SlotState.PREPARING);
            BUILD_QUEUE.add(new BuildRequest(session.instanceId(), session.slot()));
        }

        for (ServerPlayer player : players) {
            Optional<ResourceLocation> callback = result == InstanceResult.SUCCESS
                    ? Optional.of(session.definition().success()) : session.definition().failure();
            executeFunction(player, callback);
        }
    }

    public static void failPlayer(ServerPlayer player) {
        InstanceSession session = SESSIONS.remove(player.getUUID());
        player.removeTag(ACTIVE_PLAYER_TAG);
        if (session == null || session.isEnding()) return;
        session.removeParticipant(player.getUUID());
        session.controller().onPlayerLeave(session, player);
        if (session.participantIds().isEmpty()) finish(session, InstanceResult.FAILURE);
        executeFunction(player, session.definition().failure());
    }

    public static void failAndKill(ServerPlayer player) {
        if (player == null) return;
        if (!SESSIONS.containsKey(player.getUUID()) && !player.getTags().contains(ACTIVE_PLAYER_TAG)) return;
        // Clear membership before death/respawn can copy the player's tags into a replacement entity.
        failPlayer(player);
        player.kill();
    }

    public static void clearOrphanedPlayerTag(ServerPlayer player) {
        if (!SESSIONS.containsKey(player.getUUID())) player.removeTag(ACTIVE_PLAYER_TAG);
    }

    public static void ownEntity(InstanceSession session, LivingEntity entity) {
        OWNED_ENTITIES.put(entity.getUUID(), session);
    }

    public static void releaseEntity(LivingEntity entity) {
        OWNED_ENTITIES.remove(entity.getUUID());
    }

    public static InstanceSession sessionFor(LivingEntity entity) {
        InstanceSession session = OWNED_ENTITIES.get(entity.getUUID());
        return session != null && !session.isEnding() ? session : null;
    }

    public static void entityDied(LivingEntity entity) {
        InstanceSession session = OWNED_ENTITIES.get(entity.getUUID());
        if (session != null && !session.isEnding()) session.controller().onEntityDeath(session, entity);
    }

    public static java.util.Collection<ResourceLocation> definitionIds() {
        return java.util.List.copyOf(RUNTIMES.keySet());
    }

    private static void reloadDefinitions() {
        if (activeBuild != null) activeBuild.finishIO();
        activeBuild = null;
        activeRequest = null;
        RUNTIMES.clear();
        BUILD_QUEUE.clear();
        for (Map.Entry<ResourceLocation, InstanceDefinition> entry : InstanceDefinitionLoader.entries()) {
            InstanceDefinition definition = entry.getValue();
            RuntimeDefinition runtime = new RuntimeDefinition(definition, Math.max(definition.prewarmSlots(),
                    savedData.allocatedSlots(entry.getKey().toString())));
            RUNTIMES.put(entry.getKey(), runtime);
            for (int slot = 0; slot < runtime.slots.size(); slot++) {
                if (savedData != null && savedData.preparedVersion(entry.getKey().toString(), slot) == definition.arenaVersion()) {
                    runtime.slots.set(slot, SlotState.READY);
                } else {
                    runtime.slots.set(slot, SlotState.PREPARING);
                    BUILD_QUEUE.add(new BuildRequest(entry.getKey(), slot));
                }
            }
        }
        loadedGeneration = InstanceDefinitionLoader.generation();
    }

    private static void replenish(ResourceLocation id, RuntimeDefinition runtime) {
        // An IO/resource failure needs a reload, not an endless stream of failed new allocations.
        if (runtime.slots.contains(SlotState.FAILED)) return;
        ServerLevel level = level(runtime.definition);
        int available = 0;
        for (int slot = 0; slot < runtime.slots.size(); slot++) {
            SlotState state = runtime.slots.get(slot);
            if (state == SlotState.READY || state == SlotState.PREPARING
                    && InstanceRegionTask.unloaded(level, runtime.definition, slot)) available++;
        }
        // A reset held resident by a neighbouring player must not impose a new capacity limit.
        while (available < runtime.definition.prewarmSlots()) {
            int slot = runtime.slots.size();
            runtime.slots.add(SlotState.PREPARING);
            savedData.setPreparedVersion(id.toString(), slot, -1);
            BUILD_QUEUE.add(new BuildRequest(id, slot));
            if (InstanceRegionTask.unloaded(level, runtime.definition, slot)) available++;
        }
    }

    private static void beginBuild(BuildRequest request) {
        RuntimeDefinition runtime = RUNTIMES.get(request.id);
        activeRequest = request;
        try {
            activeBuild = new InstanceRegionTask(level(runtime.definition), runtime.definition, request.slot);
        } catch (RuntimeException exception) {
            runtime.slots.set(request.slot, SlotState.FAILED);
            PanlingRE.LOGGER.error("Cannot open baked instance {}", request.id, exception);
            activeRequest = null;
        }
    }

    private static ServerLevel level(InstanceDefinition definition) {
        if (server == null) return null;
        return server.overworld();
    }

    private static void executeFunction(ServerPlayer player, Optional<ResourceLocation> function) {
        function.ifPresent(id -> {
            CommandSourceStack source = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
            player.getServer().getCommands().performPrefixedCommand(source, "function " + id);
        });
    }

    private enum SlotState { PREPARING, READY, RUNNING, FAILED }

    private static final class RuntimeDefinition {
        final InstanceDefinition definition;
        final java.util.List<SlotState> slots;

        RuntimeDefinition(InstanceDefinition definition, int count) {
            this.definition = definition;
            this.slots = new java.util.ArrayList<>(java.util.Collections.nCopies(count, SlotState.PREPARING));
        }

        int firstReadySlot() {
            for (int i = 0; i < slots.size(); i++) if (slots.get(i) == SlotState.READY) return i;
            return -1;
        }
    }

    private record BuildRequest(ResourceLocation id, int slot) {}
}
