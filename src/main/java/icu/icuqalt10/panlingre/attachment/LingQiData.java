package icu.icuqalt10.panlingre.attachment;

import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.network.LingQiSyncPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class LingQiData {
    private float current;

    public LingQiData(float value) {
        this.current = value;
    }

    public float getCurrent() { return current; }
    public void setCurrent(float value, Player player) {
        float max = (float) player.getAttributeValue(ModAttributes.MAX_LINGQI);
        this.current = Math.min(value, max);
    }

    // 同步
    public void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            float maxLingQi = (float) player.getAttributeValue(ModAttributes.MAX_LINGQI);
            PacketDistributor.sendToPlayer(serverPlayer, new LingQiSyncPacket(serverPlayer.getUUID(), this.current, maxLingQi));
        }
    }

    public boolean consume(Player player, float amount) {
        if (player.level().isClientSide) return true;

        if (this.current >= amount) {
            this.current -= amount;
            sync(player);
            return true;
        }
        player.displayClientMessage(Component.translatable("title.lingqi.cant_use"), true);
        return false;
    }

    //静态方法
    public static Component getCostText(float cost) {
        String value = new BigDecimal(Float.toString(cost)).stripTrailingZeros().toPlainString();
        return Component.literal(value).withStyle(ChatFormatting.AQUA);
    }

    public static class ClientLingQiData {
        private static UUID owner;
        private static float current;
        private static float max;

        public static void set(UUID owner, float c, float m) {
            ClientLingQiData.owner = owner;
            current = c;
            max = m;
        }

        // 读取时校验请求者 UUID，避免多玩家/切换场景下缓存串数据
        public static float getCurrent(UUID requester) {
            return Objects.equals(owner, requester) ? current : 0f;
        }

        public static float getMax(UUID requester) {
            return Objects.equals(owner, requester) ? max : 0f;
        }
    }
}
