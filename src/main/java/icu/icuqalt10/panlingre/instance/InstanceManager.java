package icu.icuqalt10.panlingre.instance;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.HashMap;
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
    private static int buildCooldown;

    private InstanceManager() {
    }

    public static void startServer(MinecraftServer minecraftServer) {
        server = minecraftServer;
        savedData = minecraftServer.overworld().getDataStorage()
                .computeIfAbsent(InstanceSavedData.FACTORY, "panlingre_instances");
        reloadDefinitions();
    }

    public static void stopServer() {
        for (InstanceSession session : java.util.List.copyOf(SESSIONS.values())) {
            failAndKill(session.player());
            if (!session.isEnding()) finish(session, InstanceResult.FAILURE);
        }
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

        if (buildCooldown > 0) buildCooldown--;
        if (buildCooldown == 0 && !BUILD_QUEUE.isEmpty()) {
            build(BUILD_QUEUE.remove());
            buildCooldown = 10;
        }

        for (InstanceSession session : java.util.List.copyOf(SESSIONS.values())) {
            if (session.isEnding()) continue;
            ServerPlayer player = session.player();
            ServerLevel level = session.level();
            if (player == null || level == null) {
                finish(session, InstanceResult.FAILURE);
                continue;
            }
            double radius = session.definition().spacing() * 0.5;
            if (player.level() != level || player.position().distanceToSqr(session.center()) > radius * radius) {
                failAndKill(player);
                continue;
            }
            session.controller().tick(session);
        }
    }

    public static boolean start(ServerPlayer player, ResourceLocation id) {
        if (SESSIONS.containsKey(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("command.panlingre.instance.already_running"));
            return false;
        }
        RuntimeDefinition runtime = RUNTIMES.get(id);
        if (runtime == null) {
            player.sendSystemMessage(Component.translatable("command.panlingre.instance.unknown", id.toString()));
            return false;
        }
        int slot = runtime.firstReadySlot();
        if (slot < 0) {
            player.sendSystemMessage(Component.translatable("command.panlingre.instance.not_ready"));
            return false;
        }
        Optional<InstanceController> controller = InstanceControllerRegistry.create(runtime.definition.controller());
        if (controller.isEmpty()) {
            player.sendSystemMessage(Component.translatable("command.panlingre.instance.controller_missing", runtime.definition.controller().toString()));
            return false;
        }
        ServerLevel targetLevel = level(runtime.definition);
        if (targetLevel == null) return false;

        runtime.slots[slot] = SlotState.RUNNING;
        savedData.setPreparedVersion(id.toString(), slot, -1);
        InstanceSession session = new InstanceSession(server, id, runtime.definition, slot, player, controller.get());
        SESSIONS.put(player.getUUID(), session);
        player.addTag(ACTIVE_PLAYER_TAG);
        Vec3 spawn = session.playerSpawn();
        player.teleportTo(targetLevel, spawn.x, spawn.y, spawn.z, player.getYRot(), player.getXRot());
        controller.get().start(session);
        return true;
    }

    public static void finish(InstanceSession session, InstanceResult result) {
        finish(session, result, session.player());
    }

    private static void finish(InstanceSession session, InstanceResult result, ServerPlayer callbackPlayer) {
        if (session.isEnding()) return;
        session.markEnding();
        SESSIONS.remove(session.playerId());
        OWNED_ENTITIES.entrySet().removeIf(entry -> entry.getValue() == session);
        session.controller().stop(session, result);

        RuntimeDefinition runtime = RUNTIMES.get(session.instanceId());
        if (runtime != null && session.slot() < runtime.slots.length) {
            runtime.slots[session.slot()] = SlotState.PREPARING;
            BUILD_QUEUE.add(new BuildRequest(session.instanceId(), session.slot()));
        }

        if (callbackPlayer != null) {
            callbackPlayer.removeTag(ACTIVE_PLAYER_TAG);
            Optional<ResourceLocation> callback = result == InstanceResult.SUCCESS
                    ? Optional.of(session.definition().success()) : session.definition().failure();
            executeFunction(callbackPlayer, callback);
        }
    }

    public static void failPlayer(ServerPlayer player) {
        InstanceSession session = SESSIONS.get(player.getUUID());
        if (session != null) finish(session, InstanceResult.FAILURE);
    }

    public static void failAndKill(ServerPlayer player) {
        if (player == null) return;
        InstanceSession session = SESSIONS.get(player.getUUID());
        if (session == null && !player.getTags().contains(ACTIVE_PLAYER_TAG)) return;
        player.kill();
        if (session != null) {
            finish(session, InstanceResult.FAILURE, player);
        } else {
            player.removeTag(ACTIVE_PLAYER_TAG);
        }
    }

    public static void ownEntity(InstanceSession session, LivingEntity entity) {
        OWNED_ENTITIES.put(entity.getUUID(), session);
    }

    public static void releaseEntity(LivingEntity entity) {
        OWNED_ENTITIES.remove(entity.getUUID());
    }

    public static void entityDied(LivingEntity entity) {
        InstanceSession session = OWNED_ENTITIES.get(entity.getUUID());
        if (session != null && !session.isEnding()) session.controller().onEntityDeath(session, entity);
    }

    public static java.util.Collection<ResourceLocation> definitionIds() {
        return java.util.List.copyOf(RUNTIMES.keySet());
    }

    private static void reloadDefinitions() {
        RUNTIMES.clear();
        BUILD_QUEUE.clear();
        for (Map.Entry<ResourceLocation, InstanceDefinition> entry : InstanceDefinitionLoader.entries()) {
            InstanceDefinition definition = entry.getValue();
            RuntimeDefinition runtime = new RuntimeDefinition(definition);
            RUNTIMES.put(entry.getKey(), runtime);
            for (int slot = 0; slot < definition.prewarmSlots(); slot++) {
                if (savedData != null && savedData.preparedVersion(entry.getKey().toString(), slot) == definition.arenaVersion()) {
                    runtime.slots[slot] = SlotState.READY;
                } else {
                    runtime.slots[slot] = SlotState.PREPARING;
                    BUILD_QUEUE.add(new BuildRequest(entry.getKey(), slot));
                }
            }
        }
        loadedGeneration = InstanceDefinitionLoader.generation();
    }

    private static void build(BuildRequest request) {
        RuntimeDefinition runtime = RUNTIMES.get(request.id);
        if (runtime == null) return;
        ServerLevel level = level(runtime.definition);
        if (level == null) return;
        Optional<StructureTemplate> optional = level.getStructureManager().get(runtime.definition.template());
        if (optional.isEmpty()) {
            PanlingRE.LOGGER.error("Missing instance structure template {}", runtime.definition.template());
            return;
        }
        StructureTemplate template = optional.get();
        Vec3 center = runtime.definition.centerForSlot(request.slot);
        BlockPos origin = new BlockPos(
                (int) Math.floor(center.x - (template.getSize().getX() - 1) * 0.5),
                (int) Math.floor(center.y - (template.getSize().getY() - 1) * 0.5),
                (int) Math.floor(center.z - (template.getSize().getZ() - 1) * 0.5)
        );
        BlockPos end = origin.offset(template.getSize().getX() - 1, template.getSize().getY() - 1, template.getSize().getZ() - 1);
        for (int chunkX = SectionPos.blockToSectionCoord(origin.getX()); chunkX <= SectionPos.blockToSectionCoord(end.getX()); chunkX++) {
            for (int chunkZ = SectionPos.blockToSectionCoord(origin.getZ()); chunkZ <= SectionPos.blockToSectionCoord(end.getZ()); chunkZ++) {
                level.getChunk(chunkX, chunkZ);
            }
        }
        AABB bounds = new AABB(
                origin.getX(), origin.getY(), origin.getZ(),
                end.getX() + 1.0, end.getY() + 1.0, end.getZ() + 1.0
        );
        for (Entity entity : java.util.List.copyOf(level.getEntities(null, bounds))) {
            if (entity instanceof ServerPlayer player) {
                if (player.getTags().contains(ACTIVE_PLAYER_TAG) || SESSIONS.containsKey(player.getUUID())) {
                    failAndKill(player);
                } else {
                    player.kill();
                }
            } else {
                entity.discard();
            }
        }
        // Player death may create drops after the first snapshot; discard every non-player remainder before placement.
        for (Entity entity : java.util.List.copyOf(level.getEntities(null, bounds))) {
            if (!(entity instanceof ServerPlayer)) entity.discard();
        }
        boolean placed = template.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.random, 2);
        if (placed) {
            runtime.slots[request.slot] = SlotState.READY;
            savedData.setPreparedVersion(request.id.toString(), request.slot, runtime.definition.arenaVersion());
            PanlingRE.LOGGER.info("Prepared instance {} slot {} at {}", request.id, request.slot, origin);
        } else {
            PanlingRE.LOGGER.error("Failed to place instance {} slot {}", request.id, request.slot);
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

    private enum SlotState { PREPARING, READY, RUNNING }

    private static final class RuntimeDefinition {
        final InstanceDefinition definition;
        final SlotState[] slots;

        RuntimeDefinition(InstanceDefinition definition) {
            this.definition = definition;
            this.slots = new SlotState[definition.prewarmSlots()];
            java.util.Arrays.fill(slots, SlotState.PREPARING);
        }

        int firstReadySlot() {
            for (int i = 0; i < slots.length; i++) if (slots[i] == SlotState.READY) return i;
            return -1;
        }
    }

    private record BuildRequest(ResourceLocation id, int slot) {}
}
