package icu.icuqalt10.panlingre.instance;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class InstanceSession {
    private final MinecraftServer server;
    private final ResourceLocation instanceId;
    private final InstanceDefinition definition;
    private final int slot;
    private final UUID playerId;
    private final Set<UUID> participants = new LinkedHashSet<>();
    private final InstanceController controller;
    private boolean ending;

    InstanceSession(MinecraftServer server, ResourceLocation instanceId, InstanceDefinition definition, int slot,
                    ServerPlayer player, InstanceController controller) {
        this(server, instanceId, definition, slot, List.of(player), controller);
    }

    InstanceSession(MinecraftServer server, ResourceLocation instanceId, InstanceDefinition definition, int slot,
                    Collection<ServerPlayer> players, InstanceController controller) {
        this.server = server;
        this.instanceId = instanceId;
        this.definition = definition;
        this.slot = slot;
        players.forEach(player -> participants.add(player.getUUID()));
        this.playerId = participants.iterator().next();
        this.controller = controller;
    }

    public MinecraftServer server() { return server; }
    public ResourceLocation instanceId() { return instanceId; }
    public InstanceDefinition definition() { return definition; }
    public int slot() { return slot; }
    public UUID playerId() { return playerId; }
    public InstanceController controller() { return controller; }
    public Vec3 center() { return definition.centerForSlot(slot); }
    public Vec3 playerSpawn() { return definition.playerSpawnForSlot(slot); }
    public boolean isEnding() { return ending; }
    void markEnding() { ending = true; }

    public ServerPlayer player() {
        return players().stream().findFirst().orElse(null);
    }

    public List<ServerPlayer> players() {
        return participants.stream().map(id -> server.getPlayerList().getPlayer(id))
                .filter(java.util.Objects::nonNull).toList();
    }

    public List<UUID> participantIds() { return List.copyOf(participants); }
    void removeParticipant(UUID id) { participants.remove(id); }

    public ServerLevel level() {
        return server.overworld();
    }

    public void finish(InstanceResult result) {
        InstanceManager.finish(this, result);
    }

}
