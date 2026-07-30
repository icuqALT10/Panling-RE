package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.item.liandan;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.concurrent.atomic.AtomicReference;

public record LdPayload() implements CustomPacketPayload {
    public static final Type<LdPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "skill_packet"));

    public static final StreamCodec<ByteBuf, LdPayload> STREAM_CODEC = StreamCodec.unit(new LdPayload());

    public static void handle(final LdPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();

                AtomicReference<ItemStack> foundStack = new AtomicReference<>(player.getMainHandItem());

                if (!(foundStack.get().getItem() instanceof liandan)) {
                    CuriosApi.getCuriosInventory(player)
                            .flatMap(handler -> handler.findFirstCurio(stack -> stack.getItem() instanceof liandan))
                            .ifPresent(result -> foundStack.set(result.stack()));
                }

                ItemStack stack = foundStack.get();
                if (stack.getItem() instanceof liandan skillItem) {
                    skillItem.liandan_trigger(player.level(), player, stack);
                }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}
