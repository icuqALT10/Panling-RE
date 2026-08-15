package icu.icuqalt10.panlingre.util;

import icu.icuqalt10.panlingre.network.FakeSnowPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class LocalWeatherManager {

    /*
    用法     实例化 开启tick 清理 调用
    private final LocalWeatherManager weatherManager = new LocalWeatherManager(this);

    weatherManager.tick();

    if (!this.level().isClientSide()) {
            weatherManager.cleanup();
        }

    weatherManager.setWeather(LocalWeatherManager.WeatherType.THUNDER, 80.0);

    weatherManager.stop();
    */

    public enum WeatherType {
        CLEAR,
        RAIN,
        THUNDER,
        SNOW
    }

    //内部类 控制雨雪变化
    public static class ClientWeatherState {
        public static boolean isFakeSnowing = false;
    }

    private final Entity centerEntity;
    private final Set<UUID> affectedPlayers = new HashSet<>();

    private boolean isActive = false;
    private WeatherType currentWeather = WeatherType.CLEAR;
    private double currentRadius = 80.0;

    /**
     * 传入中心实体，天气效果会跟随这个实体移动
     */
    public LocalWeatherManager(Entity centerEntity) {
        this.centerEntity = centerEntity;
    }

    /**
     * 开启或切换天气
     * @param type 天气类型
     * @param radius 影响半径
     */
    public void setWeather(WeatherType type, double radius) {
        this.currentWeather = type;
        this.currentRadius = radius;
        this.isActive = (type != WeatherType.CLEAR);

        // 如果切回晴天，或者直接调用stop()，清理所有受影响玩家
        if (!this.isActive) {
            clearAllPlayers();
        } else {
            // 如果是在不同天气间切换（比如雨天切雷暴），需要给当前已经在范围内的玩家刷新状态
            for (UUID uuid : affectedPlayers) {
                ServerPlayer player = getPlayerByUUID(uuid);
                if (player != null) {
                    sendWeatherPackets(player, this.currentWeather);
                }
            }
        }
    }

    /**
     * 关闭局部天气
     */
    public void stop() {
        setWeather(WeatherType.CLEAR, this.currentRadius);
    }

    /**
     * 必须在实体的 tick() 中调用此方法
     */
    public void tick() {
        if (centerEntity.level().isClientSide() || !this.isActive) return;

        // 每 20 tick (1秒) 扫描一次即可
        if (centerEntity.tickCount % 20 != 0) return;

        List<ServerPlayer> nearbyPlayers = centerEntity.level().getEntitiesOfClass(
                ServerPlayer.class,
                centerEntity.getBoundingBox().inflate(this.currentRadius)
        );

        Set<UUID> currentNearby = new HashSet<>();

        // 1. 给新进入范围的玩家发包
        for (ServerPlayer player : nearbyPlayers) {
            currentNearby.add(player.getUUID());
            // 新进入范围的玩家
            if (!affectedPlayers.contains(player.getUUID())) {
                sendWeatherPackets(player, this.currentWeather);
                affectedPlayers.add(player.getUUID());
            }
        }

        // 2. 处理离开范围的玩家
        affectedPlayers.removeIf(uuid -> {
            if (!currentNearby.contains(uuid)) {
                restoreRealWeather(uuid);
                return true;
            }
            return false;
        });
    }

    /**
     * 必须在实体的 remove() 中调用此方法，防止内存泄漏或假天气残留
     */
    public void cleanup() {
        clearAllPlayers();
    }

    private void clearAllPlayers() {
        for (UUID uuid : affectedPlayers) {
            restoreRealWeather(uuid);
        }
        affectedPlayers.clear();
    }

    /**
     * 恢复玩家到当前世界的真实天气
     */
    private void restoreRealWeather(UUID uuid) {
        ServerPlayer player = getPlayerByUUID(uuid);
        if (player != null && centerEntity.level() instanceof ServerLevel serverLevel) {
            boolean isRaining = serverLevel.isRaining();
            float rainLevel = serverLevel.getRainLevel(1.0F);
            float thunderLevel = serverLevel.getThunderLevel(1.0F);

            player.connection.send(new ClientboundGameEventPacket(
                    isRaining ? ClientboundGameEventPacket.START_RAINING : ClientboundGameEventPacket.STOP_RAINING, 0.0F
            ));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, rainLevel));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, thunderLevel));

            // 2. 离开范围时，告诉客户端关闭下雪状态
            PacketDistributor.sendToPlayer(player, new FakeSnowPayload(false));
        }
    }

    private void sendWeatherPackets(ServerPlayer player, WeatherType type) {
        switch (type) {
            case RAIN -> {
                player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0F));
                player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 1.0F));
                player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 0.0F));
                PacketDistributor.sendToPlayer(player, new FakeSnowPayload(false));
            }
            case THUNDER -> {
                player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0F));
                player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 1.5F));
                player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 1.0F));
                PacketDistributor.sendToPlayer(player, new FakeSnowPayload(false));
            }
            case SNOW -> {
                player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 1.0F));
                PacketDistributor.sendToPlayer(player, new FakeSnowPayload(true));
            }
            case CLEAR -> restoreRealWeather(player.getUUID());
        }
    }

    private ServerPlayer getPlayerByUUID(UUID uuid) {
        if (centerEntity.level() instanceof ServerLevel serverLevel) {
            // PlayerList 内部维护按 UUID 的 Map，O(1) 查找，避免每次遍历在线玩家列表
            return serverLevel.getServer().getPlayerList().getPlayer(uuid);
        }
        return null;
    }
}