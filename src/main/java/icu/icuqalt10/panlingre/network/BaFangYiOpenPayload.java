package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端→客户端：携带已解锁的传送数据
 */
public record BaFangYiOpenPayload(List<BaFangYiOpenPayload.BaFangYiMajorPayload> majors) implements CustomPacketPayload {

    public static final Type<BaFangYiOpenPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "ba_fang_yi_open")
    );

    public record BaFangYiMajorPayload(Component title, String id, String texture, List<BaFangYiSubPayload> poses) {}
    public record BaFangYiSubPayload(Component title, String id, String texture, double x, double y, double z) {}

    public static final StreamCodec<RegistryFriendlyByteBuf, BaFangYiOpenPayload> STREAM_CODEC = StreamCodec.of(
            BaFangYiOpenPayload::write,
            BaFangYiOpenPayload::read
    );

    private static void write(RegistryFriendlyByteBuf buf, BaFangYiOpenPayload payload) {
        buf.writeVarInt(payload.majors.size());
        for (BaFangYiMajorPayload major : payload.majors) {
            buf.writeUtf(Component.Serializer.toJson(major.title(), buf.registryAccess()));
            buf.writeUtf(major.id());
            buf.writeUtf(major.texture());
            buf.writeVarInt(major.poses().size());
            for (BaFangYiSubPayload sub : major.poses()) {
                buf.writeUtf(Component.Serializer.toJson(sub.title(), buf.registryAccess()));
                buf.writeUtf(sub.id());
                buf.writeUtf(sub.texture());
                buf.writeDouble(sub.x());
                buf.writeDouble(sub.y());
                buf.writeDouble(sub.z());
            }
        }
    }

    private static BaFangYiOpenPayload read(RegistryFriendlyByteBuf buf) {
        int majorCount = buf.readVarInt();
        List<BaFangYiMajorPayload> majors = new ArrayList<>(majorCount);
        for (int i = 0; i < majorCount; i++) {
            Component title = Component.Serializer.fromJson(buf.readUtf(), buf.registryAccess());
            String id = buf.readUtf();
            String texture = buf.readUtf();
            int subCount = buf.readVarInt();
            List<BaFangYiSubPayload> subs = new ArrayList<>(subCount);
            for (int j = 0; j < subCount; j++) {
                Component subTitle = Component.Serializer.fromJson(buf.readUtf(), buf.registryAccess());
                subs.add(new BaFangYiSubPayload(
                        subTitle, buf.readUtf(), buf.readUtf(),
                        buf.readDouble(), buf.readDouble(), buf.readDouble()
                ));
            }
            majors.add(new BaFangYiMajorPayload(title, id, texture, subs));
        }
        return new BaFangYiOpenPayload(majors);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
