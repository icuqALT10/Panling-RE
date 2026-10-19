package icu.icuqalt10.panlingre.instance;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;

public interface InstanceController {
    void start(InstanceSession session);

    void tick(InstanceSession session);

    default void onEntityDeath(InstanceSession session, LivingEntity entity) {
    }

    default void stop(InstanceSession session, InstanceResult result) {
    }

    default void onPlayerLeave(InstanceSession session, ServerPlayer player) {
    }
}
