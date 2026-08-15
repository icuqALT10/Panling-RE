package icu.icuqalt10.panlingre.instance;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class InstanceSession {
    private final MinecraftServer server;
    private final ResourceLocation instanceId;
    private final InstanceDefinition definition;
    private final int slot;
    private final UUID playerId;
    private final InstanceController controller;
    private boolean ending;

    InstanceSession(MinecraftServer server, ResourceLocation instanceId, InstanceDefinition definition, int slot,
                    ServerPlayer player, InstanceController controller) {
        this.server = server;
        this.instanceId = instanceId;
        this.definition = definition;
        this.slot = slot;
        this.playerId = player.getUUID();
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
        return server.getPlayerList().getPlayer(playerId);
    }

    public ServerLevel level() {
        return server.overworld();
    }

    public void finish(InstanceResult result) {
        InstanceManager.finish(this, result);
    }

}
