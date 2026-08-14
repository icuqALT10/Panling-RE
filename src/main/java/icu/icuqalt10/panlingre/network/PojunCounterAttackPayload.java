package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 让客户端使用当前准星目标执行一次原版左键实体攻击。 */
public record PojunCounterAttackPayload() implements CustomPacketPayload {

    public static final Type<PojunCounterAttackPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "pojun_counter_attack"));
    public static final StreamCodec<ByteBuf, PojunCounterAttackPayload> STREAM_CODEC =
            StreamCodec.unit(new PojunCounterAttackPayload());

    public static void handle(PojunCounterAttackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.gameMode == null) return;

            // 无论准星是否命中实体，都播放一次主手挥击作为格挡成功的演出。
            if (minecraft.hitResult instanceof EntityHitResult entityHit) {
                // 先武装服务端上的这一次反击，再通过原版接口发送实体攻击包。
                PacketDistributor.sendToServer(new PojunCounterAttackReadyPayload());
                minecraft.gameMode.attack(minecraft.player, entityHit.getEntity());
            }
            minecraft.player.swing(InteractionHand.MAIN_HAND);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
